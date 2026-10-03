package com.omarkent.ruota;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(6, 17, 31);
    private static final int PANEL = Color.rgb(13, 31, 51);
    private static final int GOLD = Color.rgb(255, 202, 40);
    private static final int CYAN = Color.rgb(38, 198, 218);
    private static final int GREEN = Color.rgb(76, 175, 80);
    private static final int WHITE = Color.rgb(245, 247, 250);

    private static class Puzzle {
        final String category;
        final String phrase;
        Puzzle(String category, String phrase) {
            this.category = category;
            this.phrase = phrase;
        }
    }

    private static class Player {
        String name;
        int roundScore;
        int totalScore;
        Player(String name) { this.name = name; }
    }

    private final List<Puzzle> puzzles = Arrays.asList(
            new Puzzle("PROVERBIO", "CHI DORME NON PIGLIA PESCI"),
            new Puzzle("PROVERBIO", "IL BUON GIORNO SI VEDE DAL MATTINO"),
            new Puzzle("MODO DI DIRE", "AVERE UN ASSO NELLA MANICA"),
            new Puzzle("MODO DI DIRE", "PRENDERE DUE PICCIONI CON UNA FAVA"),
            new Puzzle("CINEMA", "RITORNO AL FUTURO"),
            new Puzzle("CINEMA", "IL SIGNORE DEGLI ANELLI"),
            new Puzzle("SERIE TV", "CACCIARE MOSTRI E SALVARE PERSONE"),
            new Puzzle("GEOGRAFIA", "LE DOLOMITI AL TRAMONTO"),
            new Puzzle("GEOGRAFIA", "IL LAGO DI GARDA"),
            new Puzzle("ANIMALI", "IL GATTO TIGRATO SUL DIVANO"),
            new Puzzle("CUCINA", "PASTA ALLA CARBONARA"),
            new Puzzle("MUSICA", "UNA CANZONE ALLA RADIO"),
            new Puzzle("OGGETTO", "TELECOMANDO DEL TELEVISORE"),
            new Puzzle("VITA QUOTIDIANA", "IL CAFFE DEL MATTINO"),
            new Puzzle("VIAGGI", "UN WEEKEND AL MARE"),
            new Puzzle("TECNOLOGIA", "CONNESSIONE SENZA FILI"),
            new Puzzle("SPORT", "LA FINALE DI CAMPIONATO"),
            new Puzzle("NATURA", "UN CIELO PIENO DI STELLE"),
            new Puzzle("MODO DI DIRE", "NON TUTTO IL MALE VIENE PER NUOCERE"),
            new Puzzle("PROVERBIO", "TRA IL DIRE E IL FARE C'E DI MEZZO IL MARE")
    );

    private final List<Player> players = new ArrayList<>();
    private final Set<Character> usedLetters = new HashSet<>();
    private final Random random = new Random();

    private Puzzle currentPuzzle;
    private int currentPlayer = 0;
    private int spinValue = 0;
    private boolean canGuessConsonant = false;
    private boolean tvMode = false;

    private LinearLayout root;
    private LinearLayout playerRow;
    private TextView categoryView;
    private TextView boardView;
    private TextView statusView;
    private GridLayout lettersGrid;
    private WheelView wheelView;
    private Button spinButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        players.add(new Player("GIOCATORE 1"));
        players.add(new Player("GIOCATORE 2"));
        newPuzzle(false);
        buildUi();
        showWelcome();
    }

    private void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(12), dp(10), dp(12), dp(10));
        setContentView(root);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        TextView title = label("RUOTA FORTUNA", 22, GOLD, true);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        Button playersBtn = button("GIOCATORI", PANEL);
        playersBtn.setOnClickListener(v -> showPlayersDialog());
        top.addView(playersBtn, new LinearLayout.LayoutParams(dp(112), dp(44)));

        Button tvBtn = button("TV", PANEL);
        tvBtn.setOnClickListener(v -> toggleTvMode());
        top.addView(tvBtn, new LinearLayout.LayoutParams(dp(64), dp(44)));

        Button castBtn = button("CAST", CYAN);
        castBtn.setTextColor(BG);
        castBtn.setOnClickListener(v -> showCastDialog());
        top.addView(castBtn, new LinearLayout.LayoutParams(dp(78), dp(44)));

        categoryView = label("", 16, CYAN, true);
        categoryView.setGravity(Gravity.CENTER);
        root.addView(categoryView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));

        boardView = label("", 30, WHITE, true);
        boardView.setGravity(Gravity.CENTER);
        boardView.setBackgroundColor(Color.rgb(11, 72, 70));
        boardView.setPadding(dp(12), dp(10), dp(12), dp(10));
        root.addView(boardView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(122)));

        playerRow = new LinearLayout(this);
        playerRow.setOrientation(LinearLayout.HORIZONTAL);
        playerRow.setGravity(Gravity.CENTER);
        root.addView(playerRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(66)));

        LinearLayout main = new LinearLayout(this);
        boolean landscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        main.setOrientation(landscape ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        root.addView(main, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        wheelView = new WheelView(this);
        wheelView.setOnSpinEndListener(this::onSpinResult);
        FrameLayout wheelFrame = new FrameLayout(this);
        wheelFrame.addView(wheelView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        main.addView(wheelFrame, landscape
                ? new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.05f)
                : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        ScrollView controlsScroll = new ScrollView(this);
        controlsScroll.setFillViewport(true);
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(dp(10), dp(6), dp(10), dp(6));
        controlsScroll.addView(controls);
        main.addView(controlsScroll, landscape
                ? new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.15f)
                : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.1f));

        statusView = label("", 16, GOLD, true);
        statusView.setGravity(Gravity.CENTER);
        controls.addView(statusView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER);
        controls.addView(actionRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));

        spinButton = button("GIRA LA RUOTA", GOLD);
        spinButton.setTextColor(BG);
        spinButton.setOnClickListener(v -> spin());
        actionRow.addView(spinButton, new LinearLayout.LayoutParams(0, dp(52), 1.4f));

        Button solve = button("RISOLVI", GREEN);
        solve.setOnClickListener(v -> showSolveDialog());
        actionRow.addView(solve, new LinearLayout.LayoutParams(0, dp(52), 1f));

        Button next = button("NUOVA", PANEL);
        next.setOnClickListener(v -> confirmNewPuzzle());
        actionRow.addView(next, new LinearLayout.LayoutParams(0, dp(52), 0.85f));

        TextView hint = label("Consonanti: gira prima • Vocali: 250 punti", 13, Color.LTGRAY, false);
        hint.setGravity(Gravity.CENTER);
        controls.addView(hint, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(30)));

        lettersGrid = new GridLayout(this);
        lettersGrid.setColumnCount(9);
        lettersGrid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        lettersGrid.setUseDefaultMargins(false);
        controls.addView(lettersGrid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        buildAlphabet();
        refreshAll();
    }

    private void buildAlphabet() {
        lettersGrid.removeAllViews();
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int i = 0; i < alphabet.length(); i++) {
            final char letter = alphabet.charAt(i);
            Button b = button(String.valueOf(letter), isVowel(letter) ? Color.rgb(0, 121, 107) : Color.rgb(32, 55, 78));
            b.setTag(Character.valueOf(letter));
            b.setOnClickListener(v -> chooseLetter(letter));
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 0;
            p.height = dp(45);
            p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            p.setMargins(dp(2), dp(2), dp(2), dp(2));
            lettersGrid.addView(b, p);
        }
    }

    private void spin() {
        if (wheelView.isSpinning()) return;
        spinButton.setEnabled(false);
        canGuessConsonant = false;
        spinValue = 0;
        status("La ruota gira…");
        wheelView.spin();
    }

    private void onSpinResult(String result) {
        spinButton.setEnabled(true);
        if ("BANCA ROTTA".equals(result)) {
            players.get(currentPlayer).roundScore = 0;
            status("BANCA ROTTA! " + players.get(currentPlayer).name + " perde i punti del round.");
            nextPlayer();
        } else if ("PASSA".equals(result)) {
            status("PASSA! Tocca al prossimo giocatore.");
            nextPlayer();
        } else if ("JACKPOT".equals(result)) {
            spinValue = 1500;
            canGuessConsonant = true;
            status("JACKPOT: 1500 per ogni consonante!");
        } else {
            spinValue = Integer.parseInt(result);
            canGuessConsonant = true;
            status("Vale " + spinValue + " punti. Scegli una consonante.");
        }
        refreshAll();
    }

    private void chooseLetter(char raw) {
        char letter = Character.toUpperCase(raw);
        if (usedLetters.contains(letter)) {
            toast("Lettera già usata");
            return;
        }
        Player p = players.get(currentPlayer);
        boolean vowel = isVowel(letter);
        if (vowel) {
            if (p.roundScore < 250) {
                toast("Servono 250 punti per comprare una vocale");
                return;
            }
            p.roundScore -= 250;
        } else if (!canGuessConsonant) {
            toast("Prima gira la ruota");
            return;
        }

        usedLetters.add(letter);
        int count = countOccurrences(letter);
        if (count > 0) {
            if (!vowel) p.roundScore += count * spinValue;
            status(count == 1 ? "C'è una " + letter + "!" : "Ci sono " + count + " " + letter + "!");
            if (isPuzzleFullyRevealed()) winRound();
        } else {
            status("Nessuna " + letter + ". Tocca al prossimo.");
            nextPlayer();
        }
        canGuessConsonant = false;
        spinValue = 0;
        refreshAll();
    }

    private int countOccurrences(char letter) {
        int count = 0;
        String normalized = normalizeText(currentPuzzle.phrase);
        for (int i = 0; i < normalized.length(); i++) {
            if (normalized.charAt(i) == letter) count++;
        }
        return count;
    }

    private void showSolveDialog() {
        final EditText input = new EditText(this);
        input.setTextColor(Color.BLACK);
        input.setSingleLine(false);
        input.setHint("Scrivi la frase completa");
        int pad = dp(18);
        FrameLayout box = new FrameLayout(this);
        box.setPadding(pad, dp(6), pad, 0);
        box.addView(input);

        new AlertDialog.Builder(this)
                .setTitle(players.get(currentPlayer).name + " prova a risolvere")
                .setView(box)
                .setNegativeButton("ANNULLA", null)
                .setPositiveButton("CONFERMA", (d, w) -> {
                    String guess = normalizeText(input.getText().toString()).replaceAll("\\s+", " ").trim();
                    String answer = normalizeText(currentPuzzle.phrase).replaceAll("\\s+", " ").trim();
                    if (guess.equals(answer)) {
                        for (char c = 'A'; c <= 'Z'; c++) usedLetters.add(c);
                        refreshAll();
                        winRound();
                    } else {
                        status("Soluzione errata. Tocca al prossimo.");
                        nextPlayer();
                        refreshAll();
                    }
                }).show();
    }

    private void winRound() {
        Player winner = players.get(currentPlayer);
        int won = winner.roundScore;
        winner.totalScore += won;
        refreshAll();
        new AlertDialog.Builder(this)
                .setTitle("ROUND VINTO!")
                .setMessage(winner.name + " ha risolto:\n\n" + currentPuzzle.phrase + "\n\n+" + won + " punti • Totale " + winner.totalScore)
                .setCancelable(false)
                .setPositiveButton("PROSSIMA FRASE", (d, w) -> newPuzzle(true))
                .show();
    }

    private void newPuzzle(boolean keepPlayer) {
        Puzzle next;
        do {
            next = puzzles.get(random.nextInt(puzzles.size()));
        } while (puzzles.size() > 1 && next == currentPuzzle);
        currentPuzzle = next;
        usedLetters.clear();
        spinValue = 0;
        canGuessConsonant = false;
        for (Player p : players) p.roundScore = 0;
        if (!keepPlayer) currentPlayer = 0;
        if (root != null) {
            buildAlphabet();
            status("Tocca a " + players.get(currentPlayer).name + ". Gira la ruota!");
            refreshAll();
        }
    }

    private void confirmNewPuzzle() {
        new AlertDialog.Builder(this)
                .setTitle("Nuova frase?")
                .setMessage("Il round corrente verrà azzerato.")
                .setNegativeButton("NO", null)
                .setPositiveButton("SÌ", (d, w) -> newPuzzle(false))
                .show();
    }

    private void nextPlayer() {
        currentPlayer = (currentPlayer + 1) % players.size();
        spinValue = 0;
        canGuessConsonant = false;
    }

    private void refreshAll() {
        if (categoryView == null) return;
        categoryView.setText("CATEGORIA: " + currentPuzzle.category);
        boardView.setText(buildBoardText());
        rebuildPlayers();
        refreshLetterButtons();
        if (statusView.getText().length() == 0) {
            status("Tocca a " + players.get(currentPlayer).name + ". Gira la ruota!");
        }
    }

    private void rebuildPlayers() {
        playerRow.removeAllViews();
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            TextView card = label(p.name + "\nROUND " + p.roundScore + "  •  TOT " + p.totalScore, 13,
                    i == currentPlayer ? BG : WHITE, true);
            card.setGravity(Gravity.CENTER);
            card.setBackgroundColor(i == currentPlayer ? GOLD : PANEL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            lp.setMargins(dp(3), dp(4), dp(3), dp(4));
            playerRow.addView(card, lp);
        }
    }

    private String buildBoardText() {
        String phrase = currentPuzzle.phrase.toUpperCase(Locale.ITALIAN);
        String normalized = normalizeText(phrase);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < phrase.length(); i++) {
            char original = phrase.charAt(i);
            char base = normalized.charAt(i);
            if (base >= 'A' && base <= 'Z') {
                out.append(usedLetters.contains(base) ? original : '□');
                if (i < phrase.length() - 1 && phrase.charAt(i + 1) != ' ') out.append(' ');
            } else if (original == ' ') {
                out.append("   ");
            } else {
                out.append(original);
            }
        }
        return out.toString();
    }

    private void refreshLetterButtons() {
        for (int i = 0; i < lettersGrid.getChildCount(); i++) {
            View v = lettersGrid.getChildAt(i);
            if (v instanceof Button) {
                Button b = (Button) v;
                Object tag = b.getTag();
                if (tag instanceof Character) {
                    char c = (Character) tag;
                    boolean used = usedLetters.contains(c);
                    b.setEnabled(!used);
                    b.setAlpha(used ? 0.28f : 1f);
                }
            }
        }
    }

    private boolean isPuzzleFullyRevealed() {
        String n = normalizeText(currentPuzzle.phrase);
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (c >= 'A' && c <= 'Z' && !usedLetters.contains(c)) return false;
        }
        return true;
    }

    private void showPlayersDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(6), dp(20), 0);
        final EditText[] inputs = new EditText[4];
        for (int i = 0; i < 4; i++) {
            EditText e = new EditText(this);
            e.setHint("Giocatore " + (i + 1) + (i >= 2 ? " (opzionale)" : ""));
            if (i < players.size()) e.setText(players.get(i).name);
            inputs[i] = e;
            box.addView(e, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
        }

        new AlertDialog.Builder(this)
                .setTitle("Giocatori (2-4)")
                .setView(box)
                .setNegativeButton("ANNULLA", null)
                .setPositiveButton("SALVA", (d, w) -> {
                    List<String> names = new ArrayList<>();
                    for (EditText e : inputs) {
                        String s = e.getText().toString().trim();
                        if (!s.isEmpty()) names.add(s.toUpperCase(Locale.ITALIAN));
                    }
                    if (names.size() < 2) {
                        toast("Servono almeno 2 giocatori");
                        return;
                    }
                    players.clear();
                    for (String n : names) players.add(new Player(n));
                    currentPlayer = 0;
                    newPuzzle(false);
                }).show();
    }

    private void showCastDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Chromecast sulla TV")
                .setMessage("Per vedere tutta la partita sulla TV dal Samsung:\n\n1. Telefono e Chromecast sulla stessa Wi-Fi.\n2. Apri Google Home.\n3. Tocca il Chromecast.\n4. Scegli 'Trasmetti schermo'.\n5. Torna qui e premi TV per il fullscreen.\n\nPuoi anche provare il menu Cast di sistema.")
                .setNegativeButton("CHIUDI", null)
                .setNeutralButton("CAST SISTEMA", (d, w) -> openSystemCast())
                .setPositiveButton("APRI GOOGLE HOME", (d, w) -> openGoogleHome())
                .show();
    }

    private void openSystemCast() {
        try {
            startActivity(new Intent("android.settings.CAST_SETTINGS"));
        } catch (ActivityNotFoundException ex) {
            openGoogleHome();
        }
    }

    private void openGoogleHome() {
        String pkg = "com.google.android.apps.chromecast.app";
        Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
        if (launch != null) {
            startActivity(launch);
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg)));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)));
        }
    }

    private void toggleTvMode() {
        tvMode = !tvMode;
        if (tvMode) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            enterImmersive();
            toast("Modalità TV attiva");
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            exitImmersive();
            toast("Modalità TV disattivata");
        }
    }

    private void enterImmersive() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void exitImmersive() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) c.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private void showWelcome() {
        new AlertDialog.Builder(this)
                .setTitle("Ruota Fortuna")
                .setMessage("Versione 1.0 pronta per 2-4 giocatori.\n\nGira la ruota, scegli le consonanti, compra le vocali e prova a risolvere la frase. Per la TV usa CAST e poi TV.")
                .setPositiveButton("GIOCA", null)
                .show();
    }

    private String normalizeText(String in) {
        String n = Normalizer.normalize(in.toUpperCase(Locale.ITALIAN), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "");
    }

    private boolean isVowel(char c) {
        return c == 'A' || c == 'E' || c == 'I' || c == 'O' || c == 'U';
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private Button button(String text, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(12);
        b.setTextColor(WHITE);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackgroundColor(color);
        b.setPadding(dp(4), 0, dp(4), 0);
        return b;
    }

    private void status(String text) {
        if (statusView != null) statusView.setText(text);
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(value * d);
    }
}
