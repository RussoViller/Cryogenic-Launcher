package com.russoviller.cryogenic.cmod.api;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Peças de interface prontas no estilo do Launcher. As cores seguem o tema de cores escolhido no
 * Launcher (são lidas na hora em que a peça é criada). Tudo aqui é View comum do Android: dá para
 * mudar tamanho, margem, cor, fonte etc. depois de criar. Use só na thread principal.
 *
 * <p>Exemplo:
 * <pre>
 * CryoModUi ui = screen.getUi();
 * LinearLayout col = ui.column();
 * col.addView(ui.title("Meu mod"));
 * col.addView(ui.button("Escolher arquivo", v -&gt; ...));
 * screen.setContent(ui.scroll(col));
 * </pre>
 */
public interface CryoModUi {

    /** Mudou o valor de um controle (slider). */
    interface ValueListener {
        void onValueChanged(int value);
    }

    // ---------------------------------------------------------------- cores do tema (ARGB)

    /** Cor principal do tema (destaques, bordas, ícones). */
    int getAccentColor();

    /** Cor clara do tema (começo do degradê). */
    int getAccentSoftColor();

    /** Cor secundária do tema (fim do degradê). */
    int getAuroraColor();

    /** Cor de fundo escura do tema. */
    int getBackgroundColor();

    /** Cor de fundo dos cartões (com transparência, efeito vidro). */
    int getSurfaceColor();

    /** Cor do texto principal. */
    int getTextColor();

    /** Cor do texto secundário. */
    int getSecondaryTextColor();

    /** Cores do degradê do tema, na ordem. */
    int[] getGradientColors();

    // ---------------------------------------------------------------- medidas e fundos

    /** Converte dp em pixels. */
    int dp(float value);

    /** Fundo de vidro (escuro translúcido com borda do tema), cantos com raio em dp. */
    Drawable glassBackground(float radiusDp);

    /** Fundo em degradê do tema, cantos com raio em dp. */
    Drawable gradientBackground(float radiusDp);

    // ---------------------------------------------------------------- layout

    /** Coluna com espaço de 8dp entre os itens. */
    LinearLayout column();

    /** Linha (itens lado a lado, centralizados na vertical) com espaço de 8dp. */
    LinearLayout row();

    /** Cartão de vidro (coluna com fundo, borda e espaçamento interno). */
    LinearLayout card();

    /** Rolagem vertical para um conteúdo grande. */
    ScrollView scroll(View content);

    /** Espaço vazio de N dp. */
    View space(float dp);

    /** Linha divisória fina. */
    View divider();

    // ---------------------------------------------------------------- textos

    /** Título grande (negrito). */
    TextView title(CharSequence text);

    /** Texto normal. */
    TextView text(CharSequence text);

    /** Texto pequeno e mais apagado. */
    TextView caption(CharSequence text);

    // ---------------------------------------------------------------- controles

    /** Botão principal (degradê do tema). */
    TextView button(CharSequence text, View.OnClickListener onClick);

    /** Botão secundário (vidro com borda do tema). */
    TextView secondaryButton(CharSequence text, View.OnClickListener onClick);

    /** Campo de texto de uma linha. */
    EditText input(CharSequence hint);

    /** Interruptor com texto. */
    CompoundButton switchButton(CharSequence text, boolean checked, CompoundButton.OnCheckedChangeListener onChange);

    /** Slider de 0 até max. */
    SeekBar slider(int max, int value, ValueListener onChange);

    /** Barra de progresso (indeterminate = animação sem porcentagem). */
    ProgressBar progress(boolean indeterminate);

    // ---------------------------------------------------------------- API 2

    /** API 2: an option was chosen ({@link #choice}). */
    interface ChoiceListener {
        void onChoice(int index, String option);
    }

    /** API 2: answer of {@link #confirm} (true = confirmed). */
    interface ConfirmListener {
        void onResult(boolean confirmed);
    }

    /** API 2: answer of {@link #prompt} (null = cancelled). */
    interface TextListener {
        void onText(String text);
    }

    /** API 2: image with rounded corners (size in dp, 0 = wrap content). */
    ImageView image(Bitmap bitmap, float widthDp, float heightDp);

    /** API 2: small pill (tag/filter). selected = filled with the theme color. */
    TextView chip(CharSequence text, boolean selected, View.OnClickListener onClick);

    /** API 2: check box with text. */
    CompoundButton checkbox(CharSequence text, boolean checked, CompoundButton.OnCheckedChangeListener onChange);

    /** API 2: dropdown with options (shows the selected one; tap to choose another). */
    TextView choice(String[] options, int selected, ChoiceListener onChoice);

    /** API 2: list row with icon (may be null), title, subtitle (may be null) and tap action. */
    LinearLayout listItem(Bitmap icon, CharSequence title, CharSequence subtitle, View.OnClickListener onClick);

    /** API 2: grid with this number of columns (children fill the columns in order). */
    GridLayout grid(int columns);

    /** API 2: themed dialog with Confirm/Cancel. Main thread. */
    void confirm(CharSequence title, CharSequence message, ConfirmListener onResult);

    /** API 2: themed dialog with a text field. Main thread. */
    void prompt(CharSequence title, CharSequence hint, CharSequence initial, TextListener onResult);

    /** API 2: themed dialog that shows any View (null title = no title). Returns a handle to close it. */
    Runnable dialog(CharSequence title, View content);
}
