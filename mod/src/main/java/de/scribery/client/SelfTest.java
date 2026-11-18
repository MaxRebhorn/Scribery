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
        // Zeile 2 beginnt bei Slot lineSlots (nicht fest 16), das Wort steht ab dem dritten Slot der Zeile
        final int row2 = UiLayout.get().lineSlots + 3;
        setWord(3, "ARCHĒ");
        setWord(row2, "LOGOZ");
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
            screen.debugClickSlot(row2 + 4); // Z greifen
            screen.debugReleaseOnCase();     // ueber der Box loslassen
            screen.debugClickLetter('S');
            screen.debugReleaseOnSlot(row2 + 4);
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
            var ui = UiLayout.get();
            int sheets = 0;
            for (var f : real) {
                var press = new de.scribery.core.PressSession(f.words(), ui.lineSlots, ui.rows, ui.maxWordsPerLine, 16, 8, 0, new java.util.Random(1));
                sheets += press.pageCount();
                for (var page : press.sheets()) {
                    de.scribery.core.TileSession.tilesFor(f.tileWords(), page, 1L, 3);
                }
            }
            LOG.info("[selftest] echte Manuskripte: {} mit {} Boegen", real.size(), sheets);
            if (real.size() != 1540) throw new IllegalStateException("erwartet 1540 Manuskripte, geladen " + real.size());
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

        // Fragment-Varianten (custom_model_data 0 bis 4) und die Fundbloecke als Item gross gezeichnet
        then(0, () -> mc.setScreen(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("preview")) {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {
                g.fill(0, 0, width, height, 0xFF3A3640);
                int x = 20;
                for (int i = 0; i <= 4; i++) {
                    var st = de.scribery.ManuscriptFragmentItem.of("P1");
                    if (i > 0) st.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
                            new net.minecraft.world.item.component.CustomModelData(i));
                    g.pose().pushPose();
                    g.pose().translate(x, 40, 0);
                    g.pose().scale(6, 6, 1);
                    g.renderItem(st, 0, 0);
                    g.pose().popPose();
                    x += 100;
                }
                // Fundbloecke: Sand, Schlamm, Asche, Eis mit Fragment, Scherbenhaufen, Amphore
                var blocks = java.util.List.of(de.scribery.ModBlocks.SUSPICIOUS_SAND, de.scribery.ModBlocks.SUSPICIOUS_MUD,
                        de.scribery.ModBlocks.SUSPICIOUS_ASHES, de.scribery.ModBlocks.FROZEN_FRAGMENT_BLOCK,
                        de.scribery.ModBlocks.SHARD_PILE, de.scribery.ModBlocks.AMPHORA);
                int bx = 20;
                for (var b : blocks) {
                    g.pose().pushPose();
                    g.pose().translate(bx, 170, 0);
                    g.pose().scale(5, 5, 1);
                    g.renderItem(new net.minecraft.world.item.ItemStack(b.get()), 0, 0);
                    g.pose().popPose();
                    bx += 85;
                }
            }
        }));
        shot(mc, "22-variants");

        // Kapitel-Tabs wie im Creative-Menue: Icon, Geschichte und Versbereich (eine Liste, damit sie ins Fenster passt)
        then(0, () -> mc.setScreen(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("tabs")) {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {
                g.fill(0, 0, width, height, 0xFF3A3640);
                float s = Math.min(1f, (height - 6) / (21f * 11f));
                g.pose().pushPose();
                g.pose().scale(s, s, 1);
                for (int c = 1; c <= 21; c++) {
                    var info = de.scribery.Chapters.of(c);
                    int y = 3 + (c - 1) * 11;
                    var icon = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                            net.minecraft.resources.ResourceLocation.parse(info.icon()));
                    g.pose().pushPose();
                    g.pose().translate(8, y, 0);
                    g.pose().scale(0.65f, 0.65f, 1);
                    g.renderItem(new net.minecraft.world.item.ItemStack(icon), 0, 0);
                    g.pose().popPose();
                    g.drawString(font, info.title() + " · John " + info.range(), 24, y + 1, 0xFFFFFFFF);
                }
                g.pose().popPose();
            }
        }));
        shot(mc, "23-chapter-tabs");

        // Nahansicht: dasselbe Fragment in allen Zustaenden (normal, verkohlt, wasser, verblasst, gefroren) wie auf dem Pult
        then(0, () -> mc.setScreen(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("closeups")) {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {
                g.fill(0, 0, width, height, 0xFF3A3640);
                var ui = UiLayout.get();
                int[] f = ui.rect("fragment");
                var words = de.scribery.Manuscripts.load().get(5).words().stream().map(w -> w.greek()).toList();
                float s = Math.min(2f, (width - 10) / (5f * (f[2] + 6)));
                String[] names = { "normal", "charred", "water", "faded", "frozen" };
                for (int look = 0; look < 5; look++) {
                    g.pose().pushPose();
                    g.pose().translate(5 + look * (f[2] + 6) * s, (height - f[3] * s) / 2f, 0);
                    g.pose().scale(s, s, 1f);
                    var fit = FragmentLayout.fit(font, words, new int[] { 0, 0, f[2], f[3] });
                    RaggedPaper.draw(g, ui, fit.paper()[0], fit.paper()[1], fit.paper()[2], fit.paper()[3], 2, look);
                    for (int i = 0; i < fit.words().length; i++) {
                        g.drawString(font, words.get(i), fit.words()[i][0], fit.words()[i][1], RaggedPaper.ink(ui, look), false);
                    }
                    g.drawString(font, names[look], 2, f[3] + 4, 0xFFFFFFFF, false);
                    g.pose().popPose();
                }
            }
        }));
        shot(mc, "24-fragment-closeups");

        // Die Amphoren-Varianten als Item (liegende Varianten zeigen im Item die stehende Form; gedreht wird erst im Blockstate)
        then(0, () -> mc.setScreen(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("amphorae")) {
            @Override
            public void render(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float pt) {
                g.fill(0, 0, width, height, 0xFF3A3640);
                var all = new java.util.ArrayList<net.neoforged.neoforge.registries.DeferredBlock<net.minecraft.world.level.block.Block>>();
                all.add(de.scribery.ModBlocks.AMPHORA);
                all.addAll(de.scribery.ModBlocks.AMPHORA_VARIANTS);
                float s = Math.min(6f, (width - 10) / (all.size() * 20f));
                for (int i = 0; i < all.size(); i++) {
                    g.pose().pushPose();
                    g.pose().translate(5 + i * 20 * s, height / 2f - 8 * s, 0);
                    g.pose().scale(s, s, 1);
                    g.renderItem(new net.minecraft.world.item.ItemStack(all.get(i).get()), 0, 0);
                    g.pose().popPose();
                }
            }
        }));
        shot(mc, "25-amphora-variants");

        then(10, () -> {
            LOG.info("[selftest] fertig");
            mc.stop();
        });
    }
}
