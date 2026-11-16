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

    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1");
        r.playToServer(CommitCabinet.TYPE, CommitCabinet.CODEC, (p, ctx) -> ctx.enqueueWork(() -> commit(p, ctx)));
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
