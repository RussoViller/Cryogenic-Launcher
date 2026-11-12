#!/usr/bin/env python3
"""
Busca os patrocinadores do afdian e grava sponsors/sponsors.json para o Cryogenic Launcher.

O token fica só no Secret AFDIAN_TOKEN do repositório; o JSON publicado tem apenas
nome, foto e datas de quem está dentro dos últimos 30 dias (sem valores nem mensagens).
"""
import hashlib
import json
import os
import sys
import time
import urllib.request

API = "https://afdian.com/api/open/"
WINDOW = 30 * 86400  # cada patrocínio fica 30 dias na lista


def call(token, user_id, path, params):
    last = None
    for attempt in range(4):
        p = json.dumps(params, separators=(",", ":"))
        ts = int(time.time())
        sign = hashlib.md5(f"{token}params{p}ts{ts}user_id{user_id}".encode()).hexdigest()
        body = json.dumps({"user_id": user_id, "params": p, "ts": ts, "sign": sign}).encode()
        req = urllib.request.Request(
            API + path, data=body,
            headers={"Content-Type": "application/json", "User-Agent": "CryogenicLauncher-sponsors"},
        )
        try:
            with urllib.request.urlopen(req, timeout=30) as r:
                data = json.load(r)
            if data.get("ec") != 200:
                # erro de token/user_id não melhora tentando de novo
                raise SystemExit(f"afdian respondeu ec={data.get('ec')} em={data.get('em')} {data.get('data')}")
            return data["data"]
        except SystemExit:
            raise
        except Exception as e:  # rede
            last = e
            time.sleep(5 * (attempt + 1))
    raise SystemExit(f"falha de rede ao falar com o afdian: {last}")


def all_sponsors(token, user_id):
    page, out = 1, []
    while True:
        data = call(token, user_id, "query-sponsor", {"page": page, "per_page": 100})
        out += data.get("list") or []
        if page >= int(data.get("total_page") or 1):
            return out
        page += 1


def main():
    token = os.environ.get("AFDIAN_TOKEN", "").strip()
    user_id = os.environ.get("AFDIAN_USER_ID", "").strip()
    out_path = sys.argv[1] if len(sys.argv) > 1 else "sponsors/sponsors.json"
    if not token or not user_id:
        raise SystemExit("Falta o Secret AFDIAN_TOKEN (Settings > Secrets and variables > Actions).")

    now = int(time.time())
    sponsors = []
    for item in all_sponsors(token, user_id):
        user = item.get("user") or {}
        paid = int(item.get("last_pay_time") or item.get("first_pay_time") or item.get("create_time") or 0)
        if paid <= 0 or paid + WINDOW <= now:
            continue
        uid = str(user.get("user_id") or "")
        sponsors.append({
            "id": hashlib.sha1(uid.encode()).hexdigest()[:12] if uid else "",
            "name": str(user.get("name") or "").strip()[:40],
            "avatar": str(user.get("avatar") or ""),
            "since": paid,
            "until": paid + WINDOW,
        })
    sponsors.sort(key=lambda s: s["since"])  # do primeiro ao último

    new = {
        "version": 1,
        "updated": now,
        "latest": max((s["since"] for s in sponsors), default=0),
        "count": len(sponsors),
        "sponsors": sponsors,
    }

    # Só reescreve quando a lista muda (evita commit a cada 8 horas sem novidade)
    try:
        with open(out_path, encoding="utf-8") as f:
            old = json.load(f)
        if old.get("sponsors") == new["sponsors"]:
            print(f"Sem mudanças ({len(sponsors)} patrocinadores ativos).")
            return
        # "latest" nunca diminui: assim o Launcher não reabre a tela quando alguém sai da lista
        new["latest"] = max(new["latest"], int(old.get("latest") or 0))
    except (FileNotFoundError, ValueError):
        pass

    os.makedirs(os.path.dirname(out_path) or ".", exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(new, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(f"Atualizado: {len(sponsors)} patrocinadores ativos.")


if __name__ == "__main__":
    main()
