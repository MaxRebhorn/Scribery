package de.scribery;

import java.util.function.Supplier;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Spielerdaten, die der Mod speichert. */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Scribery.MODID);

    /** Die Sammlung im Manuscript Archive. Bleibt nach dem Tod erhalten. */
    public static final Supplier<AttachmentType<ArchiveData>> ARCHIVE = ATTACHMENTS.register("archive",
            () -> AttachmentType.builder(() -> ArchiveData.EMPTY).serialize(ArchiveData.CODEC).copyOnDeath().build());

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }

    private ModAttachments() {}
}
