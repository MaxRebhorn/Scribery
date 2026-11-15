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

        // Beide Zeilen auf einem Bogen, die zweite zuerst falsch (Z statt S)
        setWord(3, "ARCHĒ");
        setWord(16, "LOGOZ");
        shot(mc, "02-set");
        inkPaperCrank(mc, "03-inked-once");
        shot(mc, "04-inked-paper");
        then(0, () -> screen.debugClick("crank"));
        then(8, () -> { });
        shot(mc, "05-crank-moving");
        then(10, () -> screen.debugClick("lever"));
        then(4, () -> { });
        shot(mc, "06-platen-down");
        then(8, () -> { });
        shot(mc, "07-smudged");

        // Nachbessern
        then(0, () -> {
            screen.debugClickSlot(20);       // Z greifen
            screen.debugReleaseOnCase();     // ueber der Box loslassen
            screen.debugClickLetter('S');
            screen.debugReleaseOnSlot(20);
            screen.debugClick("lever");      // ohne Papier: Hinweis
        });
        shot(mc, "08-refusal-hint");
        inkPaperCrank(mc, null);
        then(0, () -> screen.debugClick("crank"));
        then(16, () -> screen.debugClick("lever"));
        then(12, () -> { });
        shot(mc, "09-proof-ok");
        then(40, () -> { });
        shot(mc, "10-complete");

        then(10, () -> {
            LOG.info("[selftest] fertig");
            mc.stop();
        });
    }
}
