package de.scribery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import de.scribery.core.ArchiveProgress;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Die Sammlung eines Spielers im Manuscript Archive: pro Manuskript-ID, was eingetragen ist (Fragment gefunden, Boegen
 * gedruckt, Boegen uebersetzt). Unveraenderlich; jede Aenderung liefert ein neues Objekt, das als Spielerdaten gesetzt wird.
 */
public record ArchiveData(Map<String, ArchiveProgress> entries) {
    public static final ArchiveData EMPTY = new ArchiveData(Map.of());

    private static final Codec<ArchiveProgress> PROGRESS_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("found").forGetter(ArchiveProgress::found),
            Codec.LONG.fieldOf("printed").forGetter(ArchiveProgress::printed),
            Codec.LONG.fieldOf("translated").forGetter(ArchiveProgress::translated)
    ).apply(i, ArchiveProgress::new));

    /** Speicherformat (Spielerdaten); Eintraege ohne Inhalt werden nicht gespeichert. */
    public static final Codec<ArchiveData> CODEC = Codec.unboundedMap(Codec.STRING, PROGRESS_CODEC)
            .xmap(m -> new ArchiveData(Map.copyOf(m)), ArchiveData::entries);

    /** Ein Eintrag fuer die Uebertragung zum Client. */
    public record Entry(String id, boolean found, long printed, long translated) {
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Entry::id,
                ByteBufCodecs.BOOL, Entry::found,
                ByteBufCodecs.VAR_LONG, Entry::printed,
                ByteBufCodecs.VAR_LONG, Entry::translated,
                Entry::new);
    }

    public ArchiveProgress get(String id) {
        return entries.getOrDefault(id, ArchiveProgress.NONE);
    }

    /** Neue Sammlung mit geaendertem Fortschritt dieses Manuskripts. */
    public ArchiveData with(String id, ArchiveProgress progress) {
        Map<String, ArchiveProgress> copy = new HashMap<>(entries);
        if (progress.isEmpty()) copy.remove(id);
        else copy.put(id, progress);
        return new ArchiveData(Map.copyOf(copy));
    }

    public List<Entry> toEntries() {
        return entries.entrySet().stream()
                .map(e -> new Entry(e.getKey(), e.getValue().found(), e.getValue().printed(), e.getValue().translated())).toList();
    }

    public static ArchiveData fromEntries(List<Entry> list) {
        Map<String, ArchiveProgress> m = new HashMap<>();
        for (Entry e : list) m.put(e.id(), new ArchiveProgress(e.found(), e.printed(), e.translated()));
        return new ArchiveData(Map.copyOf(m));
    }
}
