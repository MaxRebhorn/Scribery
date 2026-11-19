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

    /** Im Archiv abgelegte Fragmente (IDs wie "P7", in der Reihenfolge des Ablegens); man kann sie wieder herausnehmen. */
    public static final Supplier<AttachmentType<java.util.List<String>>> STORED = ATTACHMENTS.register("stored_fragments",
            () -> AttachmentType.<java.util.List<String>>builder(() -> java.util.List.of())
                    .serialize(com.mojang.serialization.Codec.STRING.listOf()).copyOnDeath().build());

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }

    private ModAttachments() {}
}
