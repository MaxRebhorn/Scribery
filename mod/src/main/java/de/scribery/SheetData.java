package de.scribery;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Inhalt eines bedruckten oder uebersetzten Bogens.
 *
 * @param fragment Index des Platzhalter-Fragments, -1 beim freien Druck
 * @param page     Bogen im Fragment, -1 beim freien Druck
 * @param lines    die gedruckten Zeilen (lateinische Umschrift)
 * @param english  die Uebersetzung, ein Eintrag pro Wort; leer bei einem unuebersetzten Bogen
 */
public record SheetData(int fragment, int page, List<String> lines, List<String> english) {
    public static final Codec<SheetData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("fragment").forGetter(SheetData::fragment),
            Codec.INT.fieldOf("page").forGetter(SheetData::page),
            Codec.STRING.listOf().fieldOf("lines").forGetter(SheetData::lines),
            Codec.STRING.listOf().optionalFieldOf("english", List.of()).forGetter(SheetData::english)
    ).apply(i, SheetData::new));

    public static final StreamCodec<ByteBuf, SheetData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SheetData::fragment,
            ByteBufCodecs.VAR_INT, SheetData::page,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(64)), SheetData::lines,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(64)), SheetData::english,
            SheetData::new);

    /** Nur ein Bogen mit Fragment-Bezug laesst sich uebersetzen. */
    public boolean translatable() {
        return fragment >= 0 && page >= 0 && english.isEmpty();
    }
}
