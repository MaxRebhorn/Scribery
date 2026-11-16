package de.scribery.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Zustand und Regeln der Presse fuer ein Fragment. Kennt weder Maus noch Minecraft.
 *
 * Die Setzform hat eine feste Zahl von Zeilen, und alle Slots sind immer da. Ein Bogen fuellt so viele,
 * wie der Text braucht. Gesperrt sind nur die Leerstellen zwischen den Woertern einer Zeile; in alle
 * anderen freien Slots kann man setzen, aber eine Letter, die dort nicht hingehoert, verwischt im Abzug. Hat ein Fragment mehr Zeilen als die Setzform, folgen weitere Boegen.
 *
 * Ablauf pro Bogen: Lettern setzen, einfaerben, Papier auflegen, kurbeln, Hebel ziehen.
 * Ist der Abzug fehlerfrei, schliesst {@link #finishPage()} den Bogen ab.
 * Slots sind durchnummeriert: Zeile * lineSlots + Spalte.
 */
public final class PressSession {
    /** So oft muss die Walze ueber eine Letter laufen. */
    public static final int INK_PASSES = 2;
    /** Eine gesetzte Letter nutzt sich beim Ablegen mit Wahrscheinlichkeit 1/WEAR_ODDS ab. */
    public static final int WEAR_ODDS = 25;

    public enum Paper { NONE, ON_TRAY, FED }

    public enum Refusal { NOT_INKED, NO_PAPER, NOT_FED, BUSY }

    public enum Hint { SET_TYPE, DIP_ROLLER, ROLL_INK, LAY_PAPER, TURN_CRANK, PULL_LEVER, DONE }

    /**
     * Ergebnis eines Hebelzugs.
     *
     * @param refusal  Grund, warum nicht gedruckt wurde, sonst null
     * @param printed  gedrucktes Zeichen pro Slot, 0 = nichts
     * @param smudged  pro Slot: gehoert zu einem falschen Wort
     * @param allCorrect alle Woerter des Bogens stimmen
     */
    public record PullResult(Refusal refusal, char[] printed, boolean[] smudged, boolean allCorrect) {
        static PullResult refused(Refusal r) {
            return new PullResult(r, null, null, false);
        }
    }

    private final List<PressWord> words;
    /** Freier Druck ohne Vorlage: jeder Slot ist offen, jeder Abzug gelingt. */
    private final boolean free;
    /** Bogen -> Zeilen -> Wortindizes. */
    private final List<List<List<Integer>>> pages = new ArrayList<>();
    private final int lineSlots;
    private final int rows;
    private final int slotCount;
    private final Map<Character, Integer> stock = new LinkedHashMap<>();
    private final Random rng;
    private final List<String> finishedLines = new ArrayList<>();
    private final List<char[]> printedSheets = new ArrayList<>();

    /** So viele Bogen Papier liegen im Vorrat. */
    public static final int START_PAPER = 16;

    private int ink;
    private int paperStock = START_PAPER;
    private int iron;
    private int pageIndex;

    // Zustand des aktuellen Bogens
    private char[] slots;
    private boolean[] fixed;
    private int[] slotWord;
    private boolean[] spacer;
    private int[] inkLevel;
    private boolean rollerCharged;
    private Paper paper = Paper.NONE;
    private boolean awaitingFinish;
    private int lastWorn;

    /** @param maxWordsPerLine hoechstens so viele Woerter in einer Zeile */
    public PressSession(List<PressWord> words, int lineSlots, int rows, int maxWordsPerLine, int stockPerLetter,
            int ink, int iron, Random rng) {
        this(words, false, lineSlots, rows, maxWordsPerLine, stockPerLetter, ink, iron, rng);
    }

    /** Presse ohne Fragment: der Spieler setzt und druckt beliebige Lettern. */
    public static PressSession free(int lineSlots, int rows, int stockPerLetter, int ink, int iron, Random rng) {
        return new PressSession(List.of(), true, lineSlots, rows, 1, stockPerLetter, ink, iron, rng);
    }

    private PressSession(List<PressWord> words, boolean free, int lineSlots, int rows, int maxWordsPerLine,
            int stockPerLetter, int ink, int iron, Random rng) {
        this.free = free;
        this.words = List.copyOf(words);
        this.lineSlots = lineSlots;
        this.rows = rows;
        this.slotCount = rows * lineSlots;
        this.ink = ink;
        this.iron = iron;
        this.rng = rng;
        for (char c : Translit.ALPHABET.toCharArray()) stock.put(c, stockPerLetter);
        List<List<Integer>> lines = LineBreaker.breakLines(
                this.words.stream().map(w -> w.latin().length()).toList(), lineSlots, maxWordsPerLine);
        for (int i = 0; i < lines.size(); i += rows) {
            pages.add(lines.subList(i, Math.min(lines.size(), i + rows)));
        }
        loadPage();
    }

    private void loadPage() {
        slots = new char[slotCount];
        fixed = new boolean[slotCount];
        slotWord = new int[slotCount];
        spacer = new boolean[slotCount];
        inkLevel = new int[slotCount];
        java.util.Arrays.fill(slotWord, -1);
        paper = Paper.NONE;
        awaitingFinish = false;
        if (isComplete() || free) return;
        List<List<Integer>> page = pages.get(pageIndex);
        for (int row = 0; row < page.size(); row++) {
            int pos = row * lineSlots;
            for (int wi : page.get(row)) {
                PressWord w = words.get(wi);
                for (int k = 0; k < w.latin().length(); k++) {
                    slotWord[pos] = wi;
                    if (w.preset()) {
                        slots[pos] = w.latin().charAt(k);
                        fixed[pos] = true;
                    }
                    pos++;
                }
                List<Integer> line = page.get(row);
                if (wi != line.get(line.size() - 1)) spacer[pos] = true; // Leerstelle zwischen zwei Woertern
                pos++;
            }
        }
    }

    // ---- Abfragen ----

    public int lineSlots() { return lineSlots; }
    public int rows() { return rows; }
    public int slotCount() { return slotCount; }
    public int pageCount() { return pages.size(); }
    public int pageIndex() { return pageIndex; }
    public boolean isComplete() { return !free && pageIndex >= pages.size(); }
    /** Freier Druck ohne Fragment. */
    public boolean isFree() { return free; }
    public int ink() { return ink; }
    public int iron() { return iron; }
    public int paperStock() { return paperStock; }
    public void setPaperStock(int n) { paperStock = Math.max(0, n); }
    public void setInk(int n) { ink = Math.max(0, n); }
    public void setStock(char letter, int n) { if (stock.containsKey(letter)) stock.put(letter, Math.max(0, n)); }
    public int stock(char letter) { return stock.getOrDefault(letter, 0); }
    public Paper paper() { return paper; }
    public boolean rollerCharged() { return rollerCharged; }
    public boolean awaitingFinish() { return awaitingFinish; }
    public List<String> finishedLines() { return List.copyOf(finishedLines); }
    /** Anzahl der Lettern, die beim letzten Abschluss eines Bogens verschlissen sind. */
    public int lastWorn() { return lastWorn; }

    /** Pro Bogen die Indizes seiner Woerter, in Textreihenfolge. */
    public List<List<Integer>> sheets() {
        List<List<Integer>> out = new ArrayList<>();
        for (List<List<Integer>> page : pages) {
            List<Integer> flat = new ArrayList<>();
            page.forEach(flat::addAll);
            out.add(flat);
        }
        return out;
    }

    /** Anzahl der fertig gedruckten Boegen. */
    public int printedCount() { return printedSheets.size(); }

    /** Das Druckbild eines fertigen Bogens: ein Zeichen pro Slot, 0 = nichts. */
    public char[] printedSheet(int page) { return printedSheets.get(page).clone(); }

    public char slot(int i) { return slots[i]; }
    public boolean isFixed(int i) { return fixed[i]; }
    /** Slot gehoert zu einem Wort des Bogens. */
    public boolean isWordSlot(int i) { return slotWord[i] >= 0; }
    /** Gesperrte Leerstelle zwischen zwei Woertern einer Zeile. */
    public boolean isSpacer(int i) { return spacer[i]; }
    public int inkLevel(int i) { return inkLevel[i]; }

    /** Griechischer Text des aktuellen Bogens, eine Zeile pro Eintrag. */
    public List<String> currentGreekLines() {
        List<String> out = new ArrayList<>();
        if (isComplete()) return out;
        for (List<Integer> line : pages.get(pageIndex)) {
            StringBuilder sb = new StringBuilder();
            for (int wi : line) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(words.get(wi).greek());
            }
            out.add(sb.toString());
        }
        return out;
    }

    /** Alle belegten Slots sind fertig eingefaerbt, und es gibt mindestens einen. */
    public boolean isInked() {
        boolean any = false;
        for (int i = 0; i < slotCount; i++) {
            if (slots[i] == 0) continue;
            any = true;
            if (inkLevel[i] < INK_PASSES) return false;
        }
        return any;
    }

    private boolean anyLetter() {
        for (char c : slots) if (c != 0) return true;
        return false;
    }

    private boolean hasEmptyGap() {
        for (int i = 0; i < slotCount; i++) {
            if (slotWord[i] >= 0 && slots[i] == 0) return true;
        }
        return false;
    }

    /** Der naechste sinnvolle Schritt, fuer die Hinweiszeile. */
    public Hint hint() {
        if (isComplete()) return Hint.DONE;
        if (paper == Paper.FED) return Hint.PULL_LEVER;
        if (hasEmptyGap() || (free && !anyLetter())) return Hint.SET_TYPE;
        if (!isInked()) return rollerCharged ? Hint.ROLL_INK : Hint.DIP_ROLLER;
        if (paper == Paper.NONE) return Hint.LAY_PAPER;
        return Hint.TURN_CRANK;
    }

    /** Die Setzform ist frei zugaenglich (nicht von Papier bedeckt, kein Abschluss offen). */
    private boolean formeOpen() {
        return !isComplete() && paper != Paper.FED && !awaitingFinish;
    }

    /**
     * Raeumt die Presse auf, wenn der Spieler geht: ein offener Abzug wird abgeschlossen, alles Gesetzte
     * geht zurueck in die Box, unbedrucktes Papier zurueck in den Vorrat, die Walze wird abgelegt.
     */
    public void closeOut() {
        if (awaitingFinish) finishPage();
        if (paper != Paper.NONE) {
            paperStock++;
            paper = Paper.NONE;
        }
        for (int i = 0; i < slotCount; i++) {
            if (fixed[i] || slots[i] == 0) continue;
            returnToCase(slots[i]);
            slots[i] = 0;
            inkLevel[i] = 0;
        }
        rollerCharged = false;
    }

    // ---- Buchstabenbox ----

    public boolean takeFromCase(char letter) {
        int n = stock(letter);
        if (n <= 0) return false;
        stock.put(letter, n - 1);
        return true;
    }

    public void returnToCase(char letter) {
        stock.merge(letter, 1, Integer::sum);
    }

    /** Giesst eine neue Letter aus einem Eisen-Nugget. */
    public boolean cast(char letter) {
        if (iron <= 0 || !stock.containsKey(letter)) return false;
        iron--;
        returnToCase(letter);
        return true;
    }

    // ---- Setzform ----

    /** Setzt eine Letter in einen leeren Slot. Nur die Leerstellen zwischen Woertern sind gesperrt. */
    public boolean place(int slot, char letter) {
        if (!formeOpen() || spacer[slot] || fixed[slot] || slots[slot] != 0) return false;
        slots[slot] = letter;
        inkLevel[slot] = 0;
        return true;
    }

    /** Nimmt eine gesetzte Letter wieder heraus. Liefert 0, wenn dort keine vom Spieler gesetzte liegt. */
    public char pickUp(int slot) {
        if (!formeOpen() || fixed[slot] || slots[slot] == 0) return 0;
        char c = slots[slot];
        slots[slot] = 0;
        inkLevel[slot] = 0;
        return c;
    }

    // ---- Einfaerben ----

    /** Taucht die Walze ein. Verbraucht eine Einheit Tinte. */
    public boolean dipRoller() {
        if (rollerCharged || ink <= 0) return false;
        ink--;
        rollerCharged = true;
        return true;
    }

    public void returnRoller() {
        rollerCharged = false;
    }

    /** Die Walze laeuft einmal ueber einen Slot. */
    public boolean inkSlot(int slot) {
        if (!formeOpen() || !rollerCharged || slots[slot] == 0 || inkLevel[slot] >= INK_PASSES) return false;
        inkLevel[slot]++;
        return true;
    }

    // ---- Papier, Kurbel, Hebel ----

    public boolean layPaper() {
        if (isComplete() || awaitingFinish || paper != Paper.NONE || paperStock <= 0) return false;
        paperStock--;
        paper = Paper.ON_TRAY;
        return true;
    }

    /** Zieht das Papier in die Presse. Liefert null bei Erfolg. */
    public Refusal crank() {
        if (isComplete() || awaitingFinish) return Refusal.BUSY;
        if (paper == Paper.NONE) return Refusal.NO_PAPER;
        if (paper == Paper.FED) return Refusal.BUSY;
        if (!isInked()) return Refusal.NOT_INKED;
        paper = Paper.FED;
        return null;
    }

    /** Zieht den Hebel. Gedruckt wird nur, wenn Papier in der Presse liegt. */
    public PullResult pull() {
        if (isComplete() || awaitingFinish) return PullResult.refused(Refusal.BUSY);
        if (paper == Paper.NONE) return PullResult.refused(Refusal.NO_PAPER);
        if (paper == Paper.ON_TRAY) return PullResult.refused(Refusal.NOT_FED);

        char[] printed = slots.clone();
        boolean[] smudged = new boolean[slotCount];
        if (free) {
            paper = Paper.NONE;
            java.util.Arrays.fill(inkLevel, 0);
            awaitingFinish = true;
            return new PullResult(null, printed, smudged, true);
        }
        boolean allCorrect = true;
        List<List<Integer>> page = pages.get(pageIndex);
        for (int row = 0; row < page.size(); row++) {
            int pos = row * lineSlots;
            for (int wi : page.get(row)) {
                String target = words.get(wi).latin();
                boolean ok = true;
                for (int k = 0; k < target.length(); k++) {
                    if (slots[pos + k] != target.charAt(k)) ok = false;
                }
                if (!ok) {
                    allCorrect = false;
                    for (int k = 0; k < target.length(); k++) smudged[pos + k] = true;
                }
                pos += target.length() + 1;
            }
        }
        // Eine Letter, wo kein Wort hingehoert, wird mitgedruckt und ist falsch.
        for (int i = 0; i < slotCount; i++) {
            if (slotWord[i] < 0 && slots[i] != 0) {
                smudged[i] = true;
                allCorrect = false;
            }
        }
        paper = Paper.NONE;
        java.util.Arrays.fill(inkLevel, 0);
        awaitingFinish = allCorrect;
        return new PullResult(null, printed, smudged, allCorrect);
    }

    /**
     * Schliesst einen fehlerfrei gedruckten Bogen ab: Lettern gehen zurueck in die Box
     * (abzueglich Verschleiss), der naechste Bogen wird geladen.
     */
    public boolean finishPage() {
        if (!awaitingFinish) return false;
        printedSheets.add(slots.clone());
        if (free) {
            for (int row = 0; row < rows; row++) {
                StringBuilder text = new StringBuilder();
                for (int c = 0; c < lineSlots; c++) {
                    char ch = slots[row * lineSlots + c];
                    text.append(ch == 0 ? ' ' : ch);
                }
                String t = text.toString().stripTrailing();
                if (!t.isBlank()) finishedLines.add(t);
            }
        }
        for (List<Integer> line : free ? List.<List<Integer>>of() : pages.get(pageIndex)) {
            StringBuilder text = new StringBuilder();
            for (int wi : line) {
                if (text.length() > 0) text.append(' ');
                text.append(words.get(wi).latin());
            }
            finishedLines.add(text.toString());
        }
        lastWorn = 0;
        for (int i = 0; i < slotCount; i++) {
            if (slots[i] == 0 || fixed[i]) continue;
            if (rng.nextInt(WEAR_ODDS) == 0) lastWorn++;
            else returnToCase(slots[i]);
        }
        pageIndex++;
        loadPage();
        return true;
    }
}
