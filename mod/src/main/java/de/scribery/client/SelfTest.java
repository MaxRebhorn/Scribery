package de.scribery.client;

import java.util.ArrayDeque;
import java.util.Deque;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

/**
 * Selbsttest fuer die Entwicklung (gradlew :mod:runSelftest): oeffnet die Presse ohne Welt,
 * spielt einen festen Ablauf ueber dieselben Klick-Wege wie die Maus, speichert Screenshots
 * nach run/screenshots und beendet das Spiel. Aktiv nur mit -Dscribery.selftest=true.
 */
final class SelfTest {
    private static final Logger LOG = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("scribery.selftest");

    private record Step(int waitTicks, Runnable action) {}

    private static final Deque<Step> steps = new ArrayDeque<>();
    private static boolean started;
    private static int wait = 40;
    private static PressScreen screen;

    private SelfTest() {}

    static void tick() {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (!started) {
            if (mc.getOverlay() != null || mc.screen == null) return;
            if (wait-- > 0) return;
            started = true;
            screen = new PressScreen();
            mc.setScreen(screen);
            script(mc);
            wait = steps.peek().waitTicks();
            return;
        }
        if (wait-- > 0) return;
        Step step = steps.poll();
        if (step == null) return;
        try {
            step.action().run();
        } catch (RuntimeException e) {
            LOG.error("[selftest] Schritt fehlgeschlagen", e);
            mc.stop();
            return;
        }
        wait = steps.isEmpty() ? 0 : steps.peek().waitTicks();
    }

    private static void then(int waitTicks, Runnable action) {
        steps.add(new Step(waitTicks, action));
    }

    private static void shot(Minecraft mc, String name) {
        then(4, () -> {
            Screenshot.grab(mc.gameDirectory, "selftest-" + name + ".png", mc.getMainRenderTarget(), msg -> { });
            LOG.info("[selftest] Screenshot {}", name);
        });
    }

    private static void setWord(int firstSlot, String letters) {
        then(0, () -> {
            for (int i = 0; i < letters.length(); i++) {
                screen.debugClickLetter(letters.charAt(i));   // druecken ...
                screen.debugReleaseOnSlot(firstSlot + i);     // ... ziehen und loslassen
            }
        });
    }

    private static void inkPaperCrank(Minecraft mc, String shotAfterFirstPass) {
        then(0, () -> {
            screen.debugClick("barrel");
            screen.debugRollAcross(true);
        });
        if (shotAfterFirstPass != null) shot(mc, shotAfterFirstPass);
        then(0, () -> {
            screen.debugRollAcross(false);
            screen.debugClick("barrel");
            screen.debugClick("paper_stock");
            screen.debugClick("paper_tray");
        });
    }

    private static void script(Minecraft mc) {
        shot(mc, "01-start");

        // Presse. Ein Bogen: Zeile 1 EN ARCHĒ ĒN, Zeile 2 HO LOGOS. LOGOS zuerst falsch (Z statt S).
        setWord(3, "ARCHĒ");
        setWord(19, "LOGOZ");
        shot(mc, "02-set");
        inkPaperCrank(mc, "03-inked-once");
        shot(mc, "04-inked-paper");
        then(0, () -> screen.debugClick("crank"));
        then(16, () -> screen.debugClick("lever"));
        then(4, () -> { });
        shot(mc, "05-platen-down");
        then(8, () -> { });
        shot(mc, "06-smudged");
        then(0, () -> {
            screen.debugClickSlot(23);       // Z greifen
            screen.debugReleaseOnCase();     // ueber der Box loslassen
            screen.debugClickLetter('S');
            screen.debugReleaseOnSlot(23);
        });
        inkPaperCrank(mc, null);
        then(0, () -> screen.debugClick("crank"));
        then(16, () -> screen.debugClick("lever"));
        then(12, () -> { });
        shot(mc, "07-proof-ok");
        then(40, () -> { });
        shot(mc, "08-press-complete");

        // Uebersetzen. Das Blatt ist leer; ein Klick auf ein Wort schreibt es hin und sein Slot entsteht.
        then(0, () -> mc.setScreen(new TileScreen(screen.bench)));
        shot(mc, "09-tiles-start");
        then(0, () -> {
            TileScreen t = (TileScreen) mc.screen;
            t.debugPick(0, true);    // im Manuskript angeklickt
            t.debugPick(1, true);
            t.debugPick(2, false);   // auf dem gedruckten Bogen angeklickt
        });
        shot(mc, "10-tiles-three-picked");
        then(0, () -> {
            TileScreen t = (TileScreen) mc.screen;
            t.debugPick(3, false);
            t.debugPick(4, true);
            t.debugPlace("in", 0);
            t.debugPlace("beginning", 1);
            t.debugPlace("was", 2);
            t.debugPlace("the", 3);
            t.debugPlace("words", 4);    // falsch: words statt word
        });
        shot(mc, "11-tiles-placed");
        then(0, () -> ((TileScreen) mc.screen).debugClickQuill());
        then(12, () -> { });
        shot(mc, "12-tiles-writing");
        then(24, () -> { });
        shot(mc, "13-tiles-smudged");
        then(0, () -> {
            TileScreen t = (TileScreen) mc.screen;
            t.debugType(4, "Word");   // von Hand getippt, Gross-/Kleinschreibung egal
            t.debugClickQuill();
        });
        then(14, () -> { });
        shot(mc, "14-tiles-fair-copy");
        then(40, () -> { });
        shot(mc, "15-tiles-complete");

        // Die echten Manuskripte: laden, in Boegen aufteilen, Plaettchen bauen
        then(0, () -> {
            var real = de.scribery.Manuscripts.loadReal();
            int sheets = 0;
            for (var f : real) {
                var press = new de.scribery.core.PressSession(f.words(), 16, 4, 3, 16, 8, 0, new java.util.Random(1));
                sheets += press.pageCount();
                for (var page : press.sheets()) {
                    de.scribery.core.TileSession.tilesFor(f.tileWords(), page, 1L, 3);
                }
            }
            LOG.info("[selftest] echte Manuskripte: {} mit {} Boegen", real.size(), sheets);
            if (real.size() != 21) throw new IllegalStateException("erwartet 21 Manuskripte, geladen " + real.size());
        });

        // Ein langes echtes Manuskript (P6, Joh 1,5) in der Presse und auf dem Pult
        then(0, () -> {
            de.scribery.Manuscripts.useReal();
            mc.setScreen(new PressScreen(new Workbench(5, Stations.DEBUG)));
        });
        shot(mc, "17-long-manuscript");
        then(0, () -> mc.setScreen(new LecternScreen(new Workbench(5, new Stations(true, false, false, false)))));
        shot(mc, "18-lectern-screen");

        // Das Buch: Titelseite, ein langer Abschnitt, eine Tabelle
        // Jedes Buch, das eine Textdatei hat, muss sich setzen lassen
        then(0, () -> {
            for (String id : new String[] { "greek_alphabet_primer", "greek_grammar_book", "greek_lexicon_a_c", "greek_lexicon_d_f",
                    "thematic_greek_lexicon", "greek_forms_grammar_lexicon", "complete_greek_lexicon" }) {
                if (!BookText.exists(id)) continue;
                int pages = BookText.paginate(mc.font, BookText.read(id)).size();
                LOG.info("[selftest] Buch {}: {} Seiten", id, pages);
            }
        });
        then(0, () -> mc.setScreen(new BookScreen(BookText.read("greek_alphabet_primer"))));
        shot(mc, "19-book-cover");
        then(0, () -> {
            BookScreen b = (BookScreen) mc.screen;
            LOG.info("[selftest] Buch: {} Seiten", b.pageCount());
            String[] names = { "Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota", "Kappa", "Lambda", "Mu",
                    "Nu", "Xi", "Omicron", "Pi", "Rho", "Sigma", "Tau", "Upsilon", "Phi", "Chi", "Psi", "Omega" };
            StringBuilder spill = new StringBuilder();
            for (int i = 0; i + 1 < names.length; i++) {
                int a0 = b.findPage((i + 1) + ". " + names[i]), a1 = b.findPage((i + 2) + ". " + names[i + 1]);
                if (a1 - a0 != 2) spill.append(names[i]).append('=').append(a1 - a0).append(' ');
            }
            LOG.info("[selftest] Buchstaben, die nicht genau eine Doppelseite haben: {}", spill.length() == 0 ? "keine" : spill);
            b.goTo(b.findPage("1. Alpha"));
        });
        shot(mc, "20-book-letters");
        then(0, () -> ((BookScreen) mc.screen).findPage("20. Upsilon"));
        shot(mc, "21-book-table");

        then(10, () -> {
            LOG.info("[selftest] fertig");
            mc.stop();
        });
    }
}
