package de.scribery.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Rollgeraeusch der Farbwalze: das Fahrgeraeusch der Lore, solange die Walze bewegt wird. */
final class RollerSound extends AbstractTickableSoundInstance {
    private static final int LINGER_TICKS = 4;

    private int remaining = LINGER_TICKS;

    RollerSound() {
        super(SoundEvents.MINECART_RIDING, SoundSource.MASTER, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.35f;
        this.pitch = 1.3f;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    /** Die Walze hat sich gerade bewegt. */
    void keepRolling() {
        remaining = LINGER_TICKS;
    }

    void end() {
        stop();
    }

    @Override
    public void tick() {
        if (--remaining <= 0) stop();
    }
}
