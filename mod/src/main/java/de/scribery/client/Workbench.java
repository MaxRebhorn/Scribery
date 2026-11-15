package de.scribery.client;

import java.util.List;
import java.util.Random;

import de.scribery.core.PressSession;
import de.scribery.core.TileSession;

/**
 * Der Arbeitsstand an einem Fragment, den sich die Stationen teilen: Was an der Presse gedruckt wurde,
 * liegt danach in der Plaettchen-Station auf dem Pult. Lebt nur, solange /scribery debug offen ist;
 * spaeter traegt der Abzug als Item diesen Stand von Block zu Block.
 */
final class Workbench {
    private static final int START_STOCK = 16;
    private static final int START_INK = 8;
    private static final int START_IRON = 24;

    final int fragmentIndex;
    final DebugFragments.Fragment fragment;
    final PressSession press;
    /** Wird von der Plaettchen-Station angelegt, sobald sie zum ersten Mal offen ist. */
    TileSession tiles;

    Workbench(int index) {
        List<DebugFragments.Fragment> all = DebugFragments.load();
        UiLayout ui = UiLayout.get();
        this.fragmentIndex = Math.floorMod(index, all.size());
        this.fragment = all.get(fragmentIndex);
        this.press = new PressSession(fragment.words(), ui.lineSlots, ui.rows, ui.maxWordsPerLine,
                START_STOCK, START_INK, START_IRON, new Random());
    }

    int startInk() {
        return START_INK;
    }
}
