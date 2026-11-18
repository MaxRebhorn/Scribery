package de.scribery.client;

import de.scribery.Manuscripts;
import java.util.List;
import java.util.Random;

import de.scribery.core.PressSession;
import de.scribery.core.TileSession;

/**
 * Der Arbeitsstand an einem Fragment, den sich die Stationen teilen: Was an der Presse gedruckt wurde,
 * liegt danach in der Plaettchen-Station auf dem Pult. Lebt nur auf dem Client; spaeter traegt der
 * Abzug als Item diesen Stand von Block zu Block.
 */
final class Workbench {
    private static final int START_STOCK = 16;
    private static final int START_INK = 8;
    private static final int START_IRON = 24;

    final int fragmentIndex;
    /** Null, wenn kein Fragment gewaehlt ist (freier Druck). */
    final Manuscripts.Fragment fragment;
    /** Zustand des Fragments auf dem Pult (custom_model_data: 0 normal, 1 verkohlt, 2 wasser, 3 verblasst, 4 gefroren). */
    int variant;
    final PressSession press;
    final Stations stations;
    /** Wird von der Plaettchen-Station angelegt, sobald sie zum ersten Mal offen ist. */
    TileSession tiles;
    /** Der Lettern-Schrank, aus dem diese Presse ihre Vorraete bezieht; null im Debug. */
    net.minecraft.core.BlockPos cabinetPos;
    /** Das Pult, auf dem das Fragment liegt; null, wenn keins da ist. */
    net.minecraft.core.BlockPos lecternPos;
    /** Wie viele fertige Boegen der Presse schon an den Server gemeldet sind. */
    int sheetsSent;
    /** Uebersetzungstisch, an dem dieser Stand arbeitet; null im Debug. */
    net.minecraft.core.BlockPos deskPos;
    /** Beim Uebersetzungstisch: nur dieser Bogen des Fragments wird uebersetzt, sonst -1. */
    int sheetPage = -1;
    int deskInk, deskTablets;
    /** Am Uebersetzungstisch: das Original liegt auf einem Pult daneben. Sonst ist nur der Druck da. */
    boolean manuscript = true;

    Workbench(int index) {
        this(index, Stations.DEBUG);
    }

    Workbench(int index, Stations stations) {
        List<Manuscripts.Fragment> all = Manuscripts.load();
        UiLayout ui = UiLayout.get();
        this.stations = stations;
        this.fragmentIndex = Math.floorMod(index, all.size());
        this.fragment = all.get(fragmentIndex);
        this.press = new PressSession(fragment.words(), ui.lineSlots, ui.rows, ui.maxWordsPerLine,
                START_STOCK, START_INK, START_IRON, new Random());
    }

    /** Fuer den Uebersetzungstisch: ein einzelner gedruckter Bogen mit den Vorraeten des Tisches. */
    static Workbench forSheet(int fragment, int page, net.minecraft.core.BlockPos desk, int ink, int tablets, boolean manuscript) {
        Workbench w = new Workbench(fragment, Stations.NONE);
        w.sheetPage = page;
        w.deskPos = desk;
        w.deskInk = ink;
        w.deskTablets = tablets;
        w.manuscript = manuscript;
        return w;
    }

    /** Ohne Fragment: freier Druck, die Presse setzt beliebige Lettern. */
    private Workbench(Stations stations) {
        UiLayout ui = UiLayout.get();
        this.stations = stations;
        this.fragmentIndex = 0;
        this.fragment = null;
        this.press = PressSession.free(ui.lineSlots, ui.rows, START_STOCK, START_INK, START_IRON, new Random());
    }

    static Workbench free(Stations stations) {
        return new Workbench(stations);
    }

    /** Schluessel fuer den Zwischenspeicher: das Fragment oder -1 ohne. */
    int key() {
        return fragment == null ? -1 : fragmentIndex;
    }

    // Stand beim letzten Abgleich mit dem Schrank. Gemeldet wird nur, was seitdem verbraucht wurde.
    final int[] baseTotal = new int[22];
    final int[] seenStock = new int[22];
    int baseInk, basePaper, seenInk, seenPaper;

    private static int count(String forme, char c) {
        int n = 0;
        for (int i = 0; i < forme.length(); i++) if (forme.charAt(i) == c) n++;
        return n;
    }

    /** Merkt sich den jetzigen Stand als Ausgangspunkt fuer die naechste Meldung. */
    void snapshot() {
        String forme = press.formeString();
        for (int i = 0; i < 22; i++) {
            char c = de.scribery.core.Translit.ALPHABET.charAt(i);
            baseTotal[i] = press.stock(c) + count(forme, c);
            seenStock[i] = press.stock(c);
        }
        baseInk = seenInk = press.ink();
        basePaper = seenPaper = press.paperStock();
    }

    /** Uebernimmt Letter-Vorrat, Tinte, Papier und gesetzte Lettern aus dem Schrank. */
    void applyResources(int[] letters, int ink, int paper, String forme) {
        for (int i = 0; i < letters.length && i < 22; i++) {
            press.setStock(de.scribery.core.Translit.ALPHABET.charAt(i), letters[i]);
        }
        press.setInk(ink);
        press.setPaperStock(paper);
        press.replaceForme(forme);
        snapshot();
    }

    /** Der Schrank hat sich von aussen geaendert (Auffuellen, Entnehmen): die Differenz geht in die Presse. */
    void syncFrom(int[] letters, int ink, int paper) {
        for (int i = 0; i < letters.length && i < 22; i++) {
            int d = letters[i] - seenStock[i];
            if (d == 0) continue;
            char c = de.scribery.core.Translit.ALPHABET.charAt(i);
            press.setStock(c, press.stock(c) + d);
            baseTotal[i] += d;
            seenStock[i] = letters[i];
        }
        int di = ink - seenInk;
        if (di != 0) { press.setInk(press.ink() + di); baseInk += di; seenInk = ink; }
        int dp = paper - seenPaper;
        if (dp != 0) { press.setPaperStock(press.paperStock() + dp); basePaper += dp; seenPaper = paper; }
    }

    /** Uebernimmt Schrank und Vorraete einer vorherigen Arbeit, damit ein Neustart nichts neu erfindet. */
    void inherit(Workbench old) {
        cabinetPos = old.cabinetPos;
        if (old.cabinetPos == null) return;
        for (char c : de.scribery.core.Translit.ALPHABET.toCharArray()) press.setStock(c, old.press.stock(c));
        press.setInk(old.press.ink());
        press.setPaperStock(old.press.paperStock());
        System.arraycopy(old.baseTotal, 0, baseTotal, 0, 22);
        System.arraycopy(old.seenStock, 0, seenStock, 0, 22);
        baseInk = old.baseInk; basePaper = old.basePaper; seenInk = old.seenInk; seenPaper = old.seenPaper;
    }

    int startInk() {
        return START_INK;
    }
}
