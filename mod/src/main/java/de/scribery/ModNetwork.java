package de.scribery;

import java.util.List;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Nachrichten vom Client zum Server. Der Server prueft jede: Entfernung, Block und dass der Client nie mehr
 * beansprucht, als wirklich im Block liegt.
 */
public final class ModNetwork {
    private static final double REACH_SQR = 8.0 * 8.0;

    private ModNetwork() {}

    /** Ein fertig gedruckter Bogen, den der Schrank herausgeben soll. */
    public record SheetOut(int fragment, int page, List<String> lines) {
        public static final StreamCodec<ByteBuf, SheetOut> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SheetOut::fragment,
                ByteBufCodecs.VAR_INT, SheetOut::page,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(64)), SheetOut::lines,
                SheetOut::new);
    }

    /** Stand des Schranks nach dem Druck: Letter-Vorrat, Tinte, Papier und neue Boegen. */
    public record CommitCabinet(BlockPos pos, List<Integer> letters, int ink, int paper, List<SheetOut> sheets) implements CustomPacketPayload {
        public static final Type<CommitCabinet> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Scribery.MODID, "commit_cabinet"));
        public static final StreamCodec<ByteBuf, CommitCabinet> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, CommitCabinet::pos,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(64)), CommitCabinet::letters,
                ByteBufCodecs.VAR_INT, CommitCabinet::ink,
                ByteBufCodecs.VAR_INT, CommitCabinet::paper,
                SheetOut.CODEC.apply(ByteBufCodecs.list(16)), CommitCabinet::sheets,
                CommitCabinet::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Stand des Uebersetzungstisches: Tinte, Taefelchen und, wenn fertig, die Uebersetzung (ein Wort pro Eintrag). */
    public record CommitDesk(BlockPos pos, int ink, int tablets, List<String> english) implements CustomPacketPayload {
        public static final Type<CommitDesk> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Scribery.MODID, "commit_desk"));
        public static final StreamCodec<ByteBuf, CommitDesk> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, CommitDesk::pos,
                ByteBufCodecs.VAR_INT, CommitDesk::ink,
                ByteBufCodecs.VAR_INT, CommitDesk::tablets,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(64)), CommitDesk::english,
                CommitDesk::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1");
        r.playToServer(CommitCabinet.TYPE, CommitCabinet.CODEC, (p, ctx) -> ctx.enqueueWork(() -> commit(p, ctx)));
        r.playToServer(CommitDesk.TYPE, CommitDesk.CODEC, (p, ctx) -> ctx.enqueueWork(() -> commitDesk(p, ctx)));
    }

    private static void commitDesk(CommitDesk p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        if (player.distanceToSqr(p.pos().getCenter()) > REACH_SQR) return;
        if (!(player.level().getBlockEntity(p.pos()) instanceof DeskBlockEntity be)) return;
        SheetData sheet = be.sheetData();
        if (!be.commit(p.ink(), p.tablets())) return;
        if (p.english().isEmpty() || sheet == null || !sheet.translatable()) return;
        if (!Translation.isCorrect(sheet, p.english()) || !be.usePaper()) return;
        be.setSheet(net.minecraft.world.item.ItemStack.EMPTY);
        var out = SheetItem.of(ModItems.TRANSLATED_SHEET.get(), new SheetData(sheet.fragment(), sheet.page(), sheet.lines(), p.english()));
        if (!player.getInventory().add(out)) player.drop(out, false);
        player.level().playSound(null, p.pos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 1f);
    }

    private static void commit(CommitCabinet p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        if (player.distanceToSqr(p.pos().getCenter()) > REACH_SQR) return;
        if (!(player.level().getBlockEntity(p.pos()) instanceof CabinetBlockEntity be)) return;
        int paperBefore = be.paper();
        if (!be.commit(p.letters().stream().mapToInt(Integer::intValue).toArray(), p.ink(), p.paper())) return;
        // Jeder Bogen kostet ein Blatt Papier: nie mehr Boegen, als Papier verbraucht wurde.
        int allowed = paperBefore - p.paper();
        for (SheetOut s : p.sheets()) {
            if (allowed-- <= 0) break;
            if (s.lines().size() > 16 || s.lines().stream().anyMatch(l -> l.length() > 64)) continue;
            var stack = SheetItem.of(ModItems.PRINTED_SHEET.get(), new SheetData(s.fragment(), s.page(), s.lines(), List.of()));
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
    }
}
