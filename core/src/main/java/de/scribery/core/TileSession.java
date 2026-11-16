package de.scribery.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Zustand und Regeln der Plaettchen-Station fuer ein Fragment. Kennt weder Maus noch Minecraft.
 *
 * Jedes Wort hat einen Platz auf dem Interlinear-Blatt, in der Reihenfolge des griechischen Textes.
 * Das Blatt ist zuerst leer: Der Spieler waehlt ein Wort aus dem Manuskript oder vom gedruckten Bogen,
 * die Feder schreibt es an seinen Platz, und darunter entsteht der Slot fuer die Uebersetzung.
 * In den Kaesten liegen Plaettchen mit je einem englischen Wort. Der Spieler legt je eines in jeden Slot;
 * die Feder schreibt dann alle gelegten Woerter. Ein richtiges Wort bleibt
 * stehen und sein Plaettchen ist verbraucht, ein falsches verwischt und das Plaettchen geht zurueck.
 * Ein Blatt fasst nur begrenzt viele Woerter, laengere Fragmente haben mehrere Blaetter.
 */
public final class TileSession {
    /**
     * Ein Wort mit seinen Glossen.
     *
     * @param ok    akzeptierte Glossen, die erste ist die kanonische
     * @param wrong falsche Glossen fuer die Plaettchen
     * @param box   Kasten, in dem die Plaettchen dieses Wortes liegen
     */
    public record Word(String greek, String latin, List<String> ok, List<String> wrong, int box) {
        public boolean accepts(String face) {
            return ok.stream().anyMatch(o -> o.equalsIgnoreCase(face));
        }
    }

    /** Ein Plaettchen mit einem englischen Wort. */
    public static final class Tile {
        private final int id;
        private final int box;
        private final String face;
        private int slot = -1;
        private boolean used;

        private Tile(int id, int box, String face) {
            this.id = id;
            this.box = box;
            this.face = face;
        }

        public int id() { return id; }
        public int box() { return box; }
        /** Das Wort auf dem Plaettchen. */
        public String face() { return face; }
        /** Slot auf dem Blatt, oder -1, wenn das Plaettchen im Kasten liegt. */
        public int slot() { return slot; }
        /** Verbraucht: Das Wort ist mit diesem Plaettchen richtig geschrieben. */
        public boolean used() { return used; }
    }

    /** So viele falsche Plaettchen liegen pro Wort hoechstens im Kasten. */
    public static final int WRONG_PER_WORD = 3;

    /** Vorrat, wenn die Station keinen echten Vorrat hat (Debug). */
    public static final int DEFAULT_TABLETS = 999;

    public enum Refusal { EMPTY_SLOTS, NO_INK, NO_TABLETS, BUSY }

    public enum Hint { PICK_WORDS, PLACE_TILES, WRITE, DONE }

    /**
     * Ergebnis eines Schreibversuchs.
     *
     * @param refusal   Grund, warum nicht geschrieben wurde, sonst null
     * @param attempted pro Slot: wurde in diesem Versuch geschrieben
     * @param wrong     pro Slot: verwischt
     * @param allCorrect das ganze Blatt ist jetzt fehlerfrei
     */
    public record WriteResult(Refusal refusal, boolean[] attempted, boolean[] wrong, boolean allCorrect) {
        static WriteResult refused(Refusal r) {
            return new WriteResult(r, null, null, false);
        }
    }

    private final List<Word> words;
    private final List<List<Integer>> pages;
    private final long seed;
    private final int[] wrongPerPage;
    private int ink;
    /** Holztaefelchen: jedes geschriebene Wort verbraucht eines, auch ein falsches. */
    private int tablets = DEFAULT_TABLETS;
    private int pageIndex;

    // Zustand des aktuellen Blatts
    private List<Tile> tiles = List.of();
    private int[] slotTile = new int[0];
    private boolean[] done = new boolean[0];
    private boolean[] revealed = new boolean[0];
    private String[] typed = new String[0];
    private String[] written = new String[0];
    private String[] smudged = new String[0];

    /**
     * @param pages pro Blatt die Indizes der Woerter, in Textreihenfolge
     * @param seed  bestimmt die Reihenfolge der Plaettchen in den Kaesten
     */
    public TileSession(List<Word> words, List<List<Integer>> pages, int ink, long seed) {
        this(words, pages, null, ink, seed);
    }

    /**
     * @param wrongPerPage pro Blatt, wie viele falsche Plaettchen jedes Wort bekommt; null = ueberall
     *                     {@link #WRONG_PER_WORD}. Weniger braucht ein Blatt, dessen Kaesten sonst ueberlaufen.
     */
    public TileSession(List<Word> words, List<List<Integer>> pages, int[] wrongPerPage, int ink, long seed) {
        this.words = List.copyOf(words);
        this.pages = pages.stream().map(List::copyOf).toList();
        this.wrongPerPage = wrongPerPage == null ? null : wrongPerPage.clone();
        this.ink = ink;
        this.seed = seed;
        loadPage();
    }

    /** Teilt die Woerter gleichmaessig auf: {@code perPage} Woerter pro Blatt. */
    public static List<List<Integer>> pagesOf(int wordCount, int perPage) {
        List<List<Integer>> out = new ArrayList<>();
        for (int i = 0; i < wordCount; i += perPage) {
            List<Integer> page = new ArrayList<>();
            for (int k = i; k < Math.min(wordCount, i + perPage); k++) page.add(k);
            out.add(page);
        }
        return out;
    }

    /**
     * Die Plaettchen fuer ein Blatt. Pro Wort je ein Plaettchen fuer die kanonische Glosse und fuer
     * bis zu drei falsche. Die Reihenfolge verraet nicht, zu welchem Wort ein Plaettchen gehoert.
     * Gleiche Eingaben ergeben immer dieselben Plaettchen.
     */
    public static List<Tile> tilesFor(List<Word> words, List<Integer> page, long seed) {
        return tilesFor(words, page, seed, WRONG_PER_WORD);
    }

    /** Wie {@link #tilesFor(List, List, long)}, aber mit hoechstens {@code wrongCount} falschen pro Wort. */
    public static List<Tile> tilesFor(List<Word> words, List<Integer> page, long seed, int wrongCount) {
        record Keyed(long key, int box, String face) {}
        List<Keyed> keyed = new ArrayList<>();
        for (int wi : page) {
            Word w = words.get(wi);
            Random r = new Random(seed * 1_000_003L + wi);
            keyed.add(new Keyed(r.nextLong(), w.box(), w.ok().get(0)));
            for (int k = 0; k < Math.min(wrongCount, w.wrong().size()); k++) {
                keyed.add(new Keyed(r.nextLong(), w.box(), w.wrong().get(k)));
            }
        }
        keyed.sort(Comparator.comparingLong(Keyed::key));
        List<Tile> out = new ArrayList<>();
        for (Keyed k : keyed) out.add(new Tile(out.size(), k.box(), k.face()));
        return out;
    }

    private void loadPage() {
        if (isComplete()) {
            tiles = List.of();
            slotTile = new int[0];
            done = new boolean[0];
            revealed = new boolean[0];
            written = new String[0];
            typed = new String[0];
            smudged = new String[0];
            return;
        }
        List<Integer> page = pages.get(pageIndex);
        tiles = tilesFor(words, page, seed, wrongPerPage == null ? WRONG_PER_WORD : wrongPerPage[pageIndex]);
        slotTile = new int[page.size()];
        java.util.Arrays.fill(slotTile, -1);
        done = new boolean[page.size()];
        revealed = new boolean[page.size()];
        written = new String[page.size()];
        typed = new String[page.size()];
        smudged = new String[page.size()];
    }

    // ---- Abfragen ----

    public int ink() { return ink; }
    public int tablets() { return tablets; }
    public void setTablets(int n) { tablets = Math.max(0, n); }
    public int pageIndex() { return pageIndex; }
    public int pageCount() { return pages.size(); }
    public boolean isComplete() { return pageIndex >= pages.size(); }
    /** Anzahl der Slots auf dem aktuellen Blatt. */
    public int slotCount() { return slotTile.length; }
    public Word word(int slot) { return words.get(pages.get(pageIndex).get(slot)); }
    public List<Tile> tiles() { return tiles; }
    /** Plaettchen im Slot, oder null. */
    public Tile tileInSlot(int slot) { return slotTile[slot] < 0 ? null : tiles.get(slotTile[slot]); }
    /** Das Wort steht schon auf dem Blatt, sein Slot ist da. */
    public boolean isRevealed(int slot) { return revealed[slot]; }
    /** Das Wort in diesem Slot ist richtig geschrieben. */
    public boolean isDone(int slot) { return done[slot]; }
    /** Das von Hand eingetippte Wort in diesem Slot, oder null. */
    public String typed(int slot) { return typed[slot]; }

    /**
     * Legt ein von Hand getipptes Wort in einen Slot, statt ein Plaettchen zu nehmen. Ein leeres Wort leert den
     * Slot. Ein dort liegendes Plaettchen geht zurueck in den Kasten.
     */
    public boolean setTyped(int slot, String text) {
        if (isComplete() || slot < 0 || slot >= typed.length || done[slot] || !revealed[slot]) return false;
        if (slotTile[slot] >= 0) {
            tiles.get(slotTile[slot]).slot = -1;
            slotTile[slot] = -1;
        }
        typed[slot] = text == null || text.isBlank() ? null : text;
        smudged[slot] = null;
        return true;
    }

    /** Der geschriebene Text, wenn der Slot fertig ist, sonst null. */
    public String written(int slot) { return written[slot]; }
    /** Der verwischte Text des letzten Fehlversuchs, sonst null. */
    public String smudged(int slot) { return smudged[slot]; }

    public boolean pageDone() {
        if (isComplete()) return false;
        for (boolean d : done) if (!d) return false;
        return true;
    }

    private boolean hasEmptySlot() {
        for (int i = 0; i < slotTile.length; i++) if (!done[i] && slotTile[i] < 0 && typed[i] == null) return true;
        return false;
    }

    public Hint hint() {
        if (isComplete() || pageDone()) return Hint.DONE;
        for (boolean r : revealed) if (!r) return Hint.PICK_WORDS;
        return hasEmptySlot() ? Hint.PLACE_TILES : Hint.WRITE;
    }

    // ---- Plaettchen ----

    /** Schreibt ein Wort auf das Blatt: Sein Slot entsteht. Liefert false, wenn es schon dasteht. */
    public boolean reveal(int slot) {
        if (isComplete() || slot < 0 || slot >= revealed.length || revealed[slot]) return false;
        revealed[slot] = true;
        return true;
    }

    /** Legt ein Plaettchen in einen Slot. Liegt dort schon eines, geht das zurueck in den Kasten. */
    public boolean place(int tileId, int slot) {
        if (isComplete() || slot < 0 || slot >= slotTile.length || done[slot] || !revealed[slot]) return false;
        Tile t = tiles.get(tileId);
        if (t.used) return false;
        if (t.slot == slot) return true;
        if (t.slot >= 0) slotTile[t.slot] = -1;
        if (slotTile[slot] >= 0) tiles.get(slotTile[slot]).slot = -1;
        slotTile[slot] = tileId;
        t.slot = slot;
        typed[slot] = null;
        smudged[slot] = null;
        return true;
    }

    /** Nimmt ein Plaettchen vom Blatt zurueck in den Kasten. */
    public void unplace(int tileId) {
        Tile t = tiles.get(tileId);
        if (t.slot >= 0) {
            slotTile[t.slot] = -1;
            t.slot = -1;
        }
    }

    // ---- Feder ----

    /** Schreibt alle gelegten Woerter. Verbraucht eine Einheit Tinte. */
    public WriteResult write() {
        if (isComplete() || pageDone()) return WriteResult.refused(Refusal.BUSY);
        if (hasEmptySlot()) return WriteResult.refused(Refusal.EMPTY_SLOTS);
        if (ink <= 0) return WriteResult.refused(Refusal.NO_INK);
        int need = 0;
        for (boolean d : done) if (!d) need++;
        if (tablets < need) return WriteResult.refused(Refusal.NO_TABLETS);
        ink--;
        tablets -= need;
        boolean[] attempted = new boolean[slotTile.length];
        boolean[] wrong = new boolean[slotTile.length];
        for (int i = 0; i < slotTile.length; i++) {
            if (done[i]) continue;
            attempted[i] = true;
            Tile t = slotTile[i] < 0 ? null : tiles.get(slotTile[i]);
            String face = t != null ? t.face() : typed[i];
            if (word(i).accepts(face)) {
                done[i] = true;
                written[i] = face;
                if (t != null) t.used = true;
            } else {
                wrong[i] = true;
                smudged[i] = face;
            }
            typed[i] = null;
            if (t != null) {
                t.slot = -1;
                slotTile[i] = -1;
            }
        }
        return new WriteResult(null, attempted, wrong, pageDone());
    }

    /** Schliesst ein fehlerfreies Blatt ab und laedt das naechste. */
    public boolean finishPage() {
        if (!pageDone()) return false;
        pageIndex++;
        loadPage();
        return true;
    }
}
