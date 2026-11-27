package com.russoviller.cryogenic.game.version.mod.conflict

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.moandjiezana.toml.Toml
import com.russoviller.cryogenic.game.addons.modloader.ModLoader
import com.russoviller.cryogenic.game.version.installed.Version
import com.russoviller.cryogenic.game.version.installed.VersionFolders
import com.russoviller.cryogenic.game.version.mod.isEnabled
import com.russoviller.cryogenic.game.version.mod.reader.checkOptiFine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.Attributes
import java.util.jar.Manifest
import java.util.zip.ZipFile

/**
 * Antes de abrir uma versão, procura mods que não funcionam juntos:
 * - o próprio mod avisa que é incompatível com outro ("breaks"/"conflicts" do Fabric e Quilt,
 *   type = "incompatible"/"discouraged" do Forge/NeoForge);
 * - o mesmo mod instalado duas vezes (mesmo ID, mesmo que em versões diferentes);
 * - mods conhecidos por substituir a mesma parte do jogo (motor de renderização, shaders).
 *
 * Mods que conflitam sem declarar isso em lugar nenhum só aparecem quando o jogo roda.
 */
object ModConflictChecker {

    /** Um mod encontrado na pasta. [ids] inclui o ID principal e os que ele "fornece". */
    data class ScannedMod(
        val file: File,
        val id: String,
        val name: String,
        val version: String,
        val ids: Set<String>,
        val rules: List<Rule>,
        /** IDs de mods que este mod precisa para funcionar (dependências obrigatórias). */
        val requires: Set<String> = emptySet(),
        /** IDs de mods embutidos no jar (jar-in-jar). Só contam para dependências, não para duplicados. */
        val nestedIds: Set<String> = emptySet()
    )

    /** Regra de incompatibilidade declarada pelo mod: [hard] = o jogo não abre; senão, só pode dar problema. */
    data class Rule(val target: String, val range: String, val maven: Boolean, val hard: Boolean)

    enum class Kind { DECLARED, DECLARED_SOFT, DUPLICATE, KNOWN }

    /** Uma ligação entre dois mods (ou mais, no caso de duplicados). */
    data class Link(val kind: Kind, val mods: List<ScannedMod>, val note: String? = null)

    /** Um grupo de mods em conflito entre si (pode ter 2, 3 ou mais). */
    data class Group(
        val mods: List<ScannedMod>,
        val links: List<Link>,
        /** Para cada mod do grupo, os mods que precisam dele (param de funcionar se ele sair). */
        val dependents: Map<File, List<ScannedMod>> = emptyMap()
    ) {
        /** Grupo em que nenhum mod impede o jogo de abrir (só avisos). */
        val softOnly: Boolean get() = links.all { it.kind == Kind.DECLARED_SOFT }
    }

    /** IDs que são o próprio jogo ou o carregador (regras contra eles não são conflito entre mods). */
    private val platformIds = setOf(
        "minecraft", "java", "fabricloader", "fabric-loader", "quilt_loader", "forge", "neoforge",
        "fml", "javafml", "lowcodefml", "mixinextras"
    )

    /**
     * Mods que substituem a mesma parte do jogo: dentro de cada grupo só pode ficar um "membro".
     * O valor é o membro (OptiFabric e o OptiFine são o mesmo membro, um precisa do outro).
     */
    private val knownGroups: List<Pair<String, Map<String, String>>> = listOf(
        "renderer" to mapOf(
            "sodium" to "sodium", "embeddium" to "embeddium", "rubidium" to "rubidium",
            "magnesium" to "magnesium", "optifine" to "optifine", "optifabric" to "optifine"
        ),
        "shaders" to mapOf(
            "iris" to "iris", "oculus" to "oculus", "optifine" to "optifine", "optifabric" to "optifine"
        )
    )

    private data class CacheKey(val path: String, val size: Long, val modified: Long, val loader: ModLoader?)
    private val cache = ConcurrentHashMap<CacheKey, List<ScannedMod>>()

    /** Procura conflitos nos mods ativos da versão. Lista vazia = tudo certo. */
    suspend fun check(version: Version): List<Group> = withContext(Dispatchers.IO) {
        val info = version.getVersionInfo() ?: return@withContext emptyList()
        val loader = info.primaryLoader?.loader
        if (loader == null || loader == ModLoader.OPTIFINE || loader == ModLoader.LITE_LOADER) return@withContext emptyList()
        val dir = File(version.getGameDir(), VersionFolders.MOD.folderName)
        val files = dir.listFiles()
            ?.filter { it.isFile && it.isEnabled() && it.name.endsWith(".jar", ignoreCase = true) }
            .orEmpty()
        checkFiles(files, loader)
    }

    /** Verifica uma lista de arquivos .jar para um carregador (usado também nos testes). */
    internal fun checkFiles(files: List<File>, loader: ModLoader): List<Group> {
        if (files.size < 2) return emptyList()
        val mods = files.flatMap { scan(it, loader) }
        return findGroups(mods).map { g -> g.copy(dependents = dependentsOf(g.mods.map { it.file }, mods)) }
    }

    /** Lê os mods ativos de uma pasta (para avisos de dependência). */
    suspend fun scanDir(modsDir: File, loader: ModLoader?): List<ScannedMod> = withContext(Dispatchers.IO) {
        if (loader == null || loader == ModLoader.OPTIFINE || loader == ModLoader.LITE_LOADER) return@withContext emptyList()
        modsDir.listFiles()
            ?.filter { it.isFile && it.isEnabled() && it.name.endsWith(".jar", ignoreCase = true) }
            .orEmpty()
            .flatMap { scan(it, loader) }
    }

    /**
     * Para cada arquivo em [removed], os mods (que continuam instalados) que precisam dele.
     * Se outro mod que fica instalado também fornece o mesmo ID, não conta como quebra.
     */
    fun dependentsOf(removed: Collection<File>, mods: List<ScannedMod>): Map<File, List<ScannedMod>> {
        val removedSet = removed.toSet()
        val staying = mods.filter { it.file !in removedSet }
        val stayingIds = staying.flatMap { it.ids + it.nestedIds }.toSet()
        val result = LinkedHashMap<File, List<ScannedMod>>()
        mods.filter { it.file in removedSet }.forEach { gone ->
            val lost = (gone.ids + gone.nestedIds) - stayingIds
            if (lost.isEmpty()) return@forEach
            val needers = staying.filter { s -> s.requires.any { it in lost } }.distinctBy { it.file }
            if (needers.isNotEmpty()) result[gone.file] = needers
        }
        return result
    }

    private fun scan(file: File, loader: ModLoader): List<ScannedMod> {
        val key = CacheKey(file.absolutePath, file.length(), file.lastModified(), loader)
        cache[key]?.let { return it }
        val result = runCatching { readJar(file, loader) }.getOrDefault(emptyList())
        cache[key] = result
        return result
    }

    private fun readJar(file: File, loader: ModLoader): List<ScannedMod> = ZipFile(file).use { zip ->
        val fabricLike = loader == ModLoader.FABRIC || loader == ModLoader.QUILT ||
                loader == ModLoader.LEGACY_FABRIC || loader == ModLoader.BABRIC
        val forgeLike = loader == ModLoader.FORGE || loader == ModLoader.NEOFORGE

        if (zip.checkOptiFine()) {
            return@use listOf(ScannedMod(file, "optifine", "OptiFine", "", setOf("optifine"), emptyList()))
        }
        if (fabricLike) {
            if (loader == ModLoader.QUILT) zip.text("quilt.mod.json")?.let { readQuilt(file, it) }?.let { return@use listOf(it) }
            zip.text("fabric.mod.json")?.let { readFabric(file, it) }?.let { mod ->
                return@use listOf(mod.copy(nestedIds = zip.nestedFabricIds()))
            }
        }
        if (forgeLike) {
            val toml = (if (loader == ModLoader.NEOFORGE) zip.text("META-INF/neoforge.mods.toml") else null)
                ?: zip.text("META-INF/mods.toml")
            if (toml != null) {
                val jarVersion = zip.getEntry("META-INF/MANIFEST.MF")?.let { e ->
                    runCatching {
                        zip.getInputStream(e).use { Manifest(it).mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION) }
                    }.getOrNull()
                }
                return@use readForge(file, toml, jarVersion.orEmpty())
            }
        }
        emptyList()
    }

    /** IDs dos mods embutidos em META-INF/jars (Fabric/Quilt jar-in-jar), um nível. */
    private fun ZipFile.nestedFabricIds(): Set<String> {
        val out = HashSet<String>()
        entries().asSequence()
            .filter { !it.isDirectory && it.name.startsWith("META-INF/jars/") && it.name.endsWith(".jar") }
            .forEach { entry ->
                runCatching {
                    java.util.zip.ZipInputStream(getInputStream(entry)).use { zin ->
                        var e = zin.nextEntry
                        while (e != null) {
                            if (e.name == "fabric.mod.json") {
                                val json = JsonParser.parseString(zin.readBytes().toString(Charsets.UTF_8)).asJsonObject
                                json.str("id")?.lowercase()?.let(out::add)
                                json.get("provides")?.takeIf { it.isJsonArray }?.asJsonArray
                                    ?.forEach { p -> runCatching { out += p.asString.lowercase() } }
                                break
                            }
                            e = zin.nextEntry
                        }
                    }
                }
            }
        return out
    }

    private fun ZipFile.text(path: String): String? =
        getEntry(path)?.let { e -> getInputStream(e).bufferedReader().use { it.readText() } }

    // ---------------- Fabric ----------------

    private fun readFabric(file: File, text: String): ScannedMod? {
        val json = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull() ?: return null
        val id = json.str("id")?.lowercase() ?: return null
        val provides = json.get("provides")?.takeIf { it.isJsonArray }?.asJsonArray
            ?.mapNotNull { runCatching { it.asString.lowercase() }.getOrNull() }.orEmpty()
        val rules = fabricRules(json.getAsJsonObjectOrNull("breaks"), hard = true) +
                fabricRules(json.getAsJsonObjectOrNull("conflicts"), hard = false)
        val requires = json.getAsJsonObjectOrNull("depends")?.keySet()?.map { it.lowercase() }?.toSet().orEmpty()
        return ScannedMod(
            file = file,
            id = id,
            name = json.str("name") ?: id,
            version = json.str("version").orEmpty(),
            ids = (provides + id).toSet(),
            rules = rules,
            requires = requires - platformIds
        )
    }

    private fun fabricRules(obj: JsonObject?, hard: Boolean): List<Rule> {
        obj ?: return emptyList()
        return obj.entrySet().flatMap { (target, value) ->
            val ranges = when {
                value.isJsonArray -> value.asJsonArray.mapNotNull { runCatching { it.asString }.getOrNull() }
                value.isJsonPrimitive -> listOf(value.asString)
                else -> listOf("*")
            }
            // lista = qualquer uma das faixas
            listOf(Rule(target.lowercase(), ranges.joinToString("||"), maven = false, hard = hard))
        }
    }

    // ---------------- Quilt ----------------

    private fun readQuilt(file: File, text: String): ScannedMod? {
        val root = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull() ?: return null
        val ql = root.getAsJsonObjectOrNull("quilt_loader") ?: return null
        val id = ql.str("id")?.lowercase() ?: return null
        val provides = ql.get("provides")?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { p ->
            when {
                p.isJsonPrimitive -> p.asString.lowercase()
                p.isJsonObject -> p.asJsonObject.str("id")?.lowercase()
                else -> null
            }
        }.orEmpty()
        val rules = mutableListOf<Rule>()
        ql.get("breaks")?.let { b ->
            val list = if (b.isJsonArray) b.asJsonArray.toList() else listOf(b)
            list.forEach { d ->
                when {
                    d.isJsonPrimitive -> rules += Rule(d.asString.substringAfter(':').lowercase(), "*", maven = false, hard = true)
                    d.isJsonObject -> {
                        val o = d.asJsonObject
                        val target = o.str("id")?.substringAfter(':')?.lowercase() ?: return@forEach
                        val versions = o.get("versions")
                        val range = when {
                            versions == null -> "*"
                            versions.isJsonArray -> versions.asJsonArray.mapNotNull { runCatching { it.asString }.getOrNull() }.joinToString("||")
                            versions.isJsonPrimitive -> versions.asString
                            else -> "*"
                        }
                        rules += Rule(target, range, maven = false, hard = true)
                    }
                }
            }
        }
        val requires = ql.get("depends")?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { d ->
            when {
                d.isJsonPrimitive -> d.asString.substringAfter(':').lowercase()
                d.isJsonObject -> {
                    val o = d.asJsonObject
                    val optional = o.get("optional")?.takeIf { it.isJsonPrimitive }?.asBoolean == true
                    if (optional) null else o.str("id")?.substringAfter(':')?.lowercase()
                }
                else -> null
            }
        }?.toSet().orEmpty()
        return ScannedMod(
            file = file,
            id = id,
            name = ql.getAsJsonObjectOrNull("metadata")?.str("name") ?: id,
            version = ql.str("version").orEmpty(),
            ids = (provides + id).toSet(),
            rules = rules,
            requires = requires - platformIds
        )
    }

    // ---------------- Forge / NeoForge ----------------

    @Suppress("UNCHECKED_CAST")
    private fun readForge(file: File, text: String, jarVersion: String): List<ScannedMod> {
        val toml = runCatching { Toml().read(text) }.getOrNull() ?: return emptyList()
        val map = toml.toMap()
        val mods = (map["mods"] as? List<Map<String, Any?>>).orEmpty()
        if (mods.isEmpty()) return emptyList()
        val deps = map["dependencies"] as? Map<String, Any?>
        val allIds = mods.mapNotNull { (it["modId"] as? String)?.lowercase() }.toSet()
        val first = mods.first()
        val id = (first["modId"] as? String)?.lowercase() ?: return emptyList()
        val version = (first["version"] as? String).orEmpty().replace("\${file.jarVersion}", jarVersion)
        val rules = mutableListOf<Rule>()
        val requires = mutableSetOf<String>()
        deps?.values?.forEach { list ->
            (list as? List<Map<String, Any?>>)?.forEach { d ->
                val target = (d["modId"] as? String)?.lowercase() ?: return@forEach
                val type = (d["type"] as? String)?.lowercase()
                if (type == "required" || (type == null && d["mandatory"] == true)) requires += target
                val hard = when (type) {
                    "incompatible" -> true
                    "discouraged" -> false
                    else -> return@forEach
                }
                rules += Rule(target, (d["versionRange"] as? String).orEmpty().ifBlank { "*" }, maven = true, hard = hard)
            }
        }
        return listOf(
            ScannedMod(
                file = file,
                id = id,
                name = (first["displayName"] as? String) ?: id,
                version = version,
                ids = allIds + id,
                rules = rules,
                requires = requires - platformIds - allIds
            )
        )
    }

    // ---------------- Conflitos ----------------

    private fun findGroups(mods: List<ScannedMod>): List<Group> {
        val links = mutableListOf<Link>()

        // 1) o mesmo mod instalado mais de uma vez
        val byId = HashMap<String, MutableList<ScannedMod>>()
        mods.forEach { m -> m.ids.forEach { byId.getOrPut(it) { mutableListOf() }.add(m) } }
        val seenDup = HashSet<Set<File>>()
        byId.forEach { (id, list) ->
            val distinct = list.distinctBy { it.file }
            if (distinct.size > 1 && id !in platformIds && seenDup.add(distinct.map { it.file }.toSet())) {
                links += Link(Kind.DUPLICATE, distinct, id)
            }
        }

        // 2) regras declaradas pelos próprios mods
        mods.forEach { source ->
            source.rules.forEach { rule ->
                if (rule.target in platformIds) return@forEach
                byId[rule.target]?.forEach { target ->
                    if (target.file == source.file) return@forEach
                    if (matches(target.version, rule)) {
                        links += Link(if (rule.hard) Kind.DECLARED else Kind.DECLARED_SOFT, listOf(source, target))
                    }
                }
            }
        }

        // 3) mods que substituem a mesma parte do jogo
        knownGroups.forEach { (group, members) ->
            val present = mods.mapNotNull { m -> m.ids.firstNotNullOfOrNull { members[it] }?.let { it to m } }
            val byMember = present.groupBy({ it.first }, { it.second })
            if (byMember.size > 1) {
                val list = byMember.values.flatten().distinctBy { it.file }
                for (i in list.indices) for (j in i + 1 until list.size) {
                    val a = list[i]
                    val b = list[j]
                    val ma = a.ids.firstNotNullOfOrNull { members[it] }
                    val mb = b.ids.firstNotNullOfOrNull { members[it] }
                    if (ma != mb) links += Link(Kind.KNOWN, listOf(a, b), group)
                }
            }
        }

        if (links.isEmpty()) return emptyList()
        // um par já avisado pelo próprio mod não precisa aparecer de novo
        val declaredPairs = links.filter { it.kind == Kind.DECLARED || it.kind == Kind.DUPLICATE }
            .map { l -> l.mods.map { it.file }.toSet() }
        val clean = links.filter { l ->
            l.kind == Kind.DECLARED || l.kind == Kind.DUPLICATE || declaredPairs.none { it.containsAll(l.mods.map { m -> m.file }) }
        }.distinctBy { l -> l.kind to l.mods.map { it.file }.toSet() }

        // junta em grupos (A com B e B com C = um grupo só)
        val parent = HashMap<File, File>()
        fun find(f: File): File {
            var x = f
            while (parent[x] != null && parent[x] != x) x = parent[x]!!
            return x
        }
        clean.forEach { l ->
            val root = find(l.mods.first().file)
            parent.putIfAbsent(root, root)
            l.mods.drop(1).forEach { m ->
                val r = find(m.file)
                parent.putIfAbsent(r, r)
                if (r != root) parent[r] = root
            }
        }
        val modByFile = mods.associateBy { it.file }
        return clean.groupBy { find(it.mods.first().file) }.map { (_, ls) ->
            val files = ls.flatMap { l -> l.mods.map { it.file } }.distinct()
            Group(files.mapNotNull { modByFile[it] }, ls)
        }.sortedBy { it.softOnly }
    }

    // ---------------- Versões ----------------

    private fun matches(version: String, rule: Rule): Boolean {
        val range = rule.range.trim()
        if (range.isEmpty() || range == "*") return true
        if (version.isBlank() || version.contains("\${")) return false
        return runCatching {
            if (rule.maven) mavenMatches(version, range)
            else range.split("||").any { alt -> semverMatches(version, alt.trim()) }
        }.getOrDefault(false)
    }

    /** Faixas do Fabric/Quilt: "*", ">=1.2 <2", "~1.2", "^1.2", "1.2.x", "=1.2". Todas precisam valer. */
    private fun semverMatches(version: String, range: String): Boolean {
        if (range.isEmpty() || range == "*") return true
        return range.split(Regex("\\s+")).filter { it.isNotEmpty() }.all { p ->
            when {
                p == "*" -> true
                p.startsWith(">=") -> compare(version, p.drop(2)) >= 0
                p.startsWith("<=") -> compare(version, p.drop(2)) <= 0
                p.startsWith(">") -> compare(version, p.drop(1)) > 0
                p.startsWith("<") -> compare(version, p.drop(1)) < 0
                p.startsWith("=") -> compare(version, p.drop(1)) == 0
                p.startsWith("~") -> {
                    val base = p.drop(1)
                    val parts = numbers(base)
                    val upper = if (parts.size >= 2) "${parts[0]}.${parts[1] + 1}" else "${parts.getOrElse(0) { 0 } + 1}"
                    compare(version, base) >= 0 && compare(version, upper) < 0
                }
                p.startsWith("^") -> {
                    val base = p.drop(1)
                    val major = numbers(base).getOrElse(0) { 0 }
                    compare(version, base) >= 0 && compare(version, "${major + 1}") < 0
                }
                p.endsWith(".x") || p.endsWith(".X") || p.endsWith(".*") -> {
                    val prefix = p.dropLast(2)
                    version == prefix || version.startsWith("$prefix.")
                }
                else -> compare(version, p) == 0
            }
        }
    }

    /** Faixas do Forge (Maven): "[1.0,2.0)", "[1.0,)", "(,1.0]", "[1.0]", "1.0" (= 1.0 ou maior). */
    private fun mavenMatches(version: String, range: String): Boolean {
        if (!range.startsWith("[") && !range.startsWith("(")) return compare(version, range) >= 0
        val parts = Regex("[\\[(][^\\])]*[\\])]").findAll(range).map { it.value }.toList()
        return parts.any { part ->
            val lowInc = part.first() == '['
            val highInc = part.last() == ']'
            val inner = part.substring(1, part.length - 1)
            if (!inner.contains(',')) return@any compare(version, inner.trim()) == 0
            val lo = inner.substringBefore(',').trim()
            val hi = inner.substringAfter(',').trim()
            val okLo = lo.isEmpty() || compare(version, lo).let { if (lowInc) it >= 0 else it > 0 }
            val okHi = hi.isEmpty() || compare(version, hi).let { if (highInc) it <= 0 else it < 0 }
            okLo && okHi
        }
    }

    private fun numbers(v: String): List<Int> =
        v.substringBefore('-').substringBefore('+').split('.').map { it.toIntOrNull() ?: 0 }

    /** Compara versões no estilo SemVer (pré-lançamento fica abaixo do lançamento). */
    private fun compare(a: String, b: String): Int {
        val ma = a.trim().substringBefore('+')
        val mb = b.trim().substringBefore('+')
        val ca = ma.substringBefore('-').split('.')
        val cb = mb.substringBefore('-').split('.')
        for (i in 0 until maxOf(ca.size, cb.size)) {
            val x = ca.getOrElse(i) { "0" }
            val y = cb.getOrElse(i) { "0" }
            val nx = x.toIntOrNull()
            val ny = y.toIntOrNull()
            val c = if (nx != null && ny != null) nx.compareTo(ny) else x.compareTo(y)
            if (c != 0) return c
        }
        val pa = ma.substringAfter('-', "")
        val pb = mb.substringAfter('-', "")
        return when {
            pa.isEmpty() && pb.isEmpty() -> 0
            pa.isEmpty() -> 1
            pb.isEmpty() -> -1
            else -> pa.compareTo(pb)
        }
    }

    private fun JsonObject.str(key: String): String? =
        get(key)?.takeIf { it.isJsonPrimitive }?.asString

    private fun JsonObject.getAsJsonObjectOrNull(key: String): JsonObject? =
        get(key)?.takeIf(JsonElement::isJsonObject)?.asJsonObject
}
