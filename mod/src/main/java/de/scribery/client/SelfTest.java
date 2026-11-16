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
        then(0, () -> screen.switchStation(1));
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
            t.debugPlace("word", 4);
            t.debugClickQuill();
        });
        then(14, () -> { });
        shot(mc, "14-tiles-fair-copy");
        then(40, () -> { });
        shot(mc, "15-tiles-complete");

        // Freier Druck: von Hand tippen, auch Buchstaben ohne Taste (Ē, Ō)
        then(0, () -> {
            PressScreen free = new PressScreen(Workbench.free(new Stations(false, true, true, false)));
            mc.setScreen(free);
            for (char c : "HELLO".toCharArray()) free.charTyped(c, 0);
            free.charTyped(' ', 0);
            free.typeLetter('Ē');
            free.typeLetter('Ō');
            free.charTyped('q', 0);   // gibt es nicht
        });
        shot(mc, "16-free-typing");

        then(10, () -> {
            LOG.info("[selftest] fertig");
            mc.stop();
        });
    }
}
