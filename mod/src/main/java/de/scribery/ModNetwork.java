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

    /** Ein Buchstabe soll am Minting Table gegossen werden. */
    public record CastLetter(BlockPos pos, int letter) implements CustomPacketPayload {
        public static final Type<CastLetter> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Scribery.MODID, "cast_letter"));
        public static final StreamCodec<ByteBuf, CastLetter> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, CastLetter::pos, ByteBufCodecs.VAR_INT, CastLetter::letter, CastLetter::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Stand des Schranks nach dem Druck: Letter-Vorrat, Tinte, Papier. */
    public record CommitCabinet(BlockPos pos, List<Integer> letters, int ink, int paper) implements CustomPacketPayload {
        public static final Type<CommitCabinet> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Scribery.MODID, "commit_cabinet"));
        public static final StreamCodec<ByteBuf, CommitCabinet> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, CommitCabinet::pos,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(64)), CommitCabinet::letters,
                ByteBufCodecs.VAR_INT, CommitCabinet::ink,
                ByteBufCodecs.VAR_INT, CommitCabinet::paper,
                CommitCabinet::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1");
        r.playToServer(CastLetter.TYPE, CastLetter.CODEC, (p, ctx) -> ctx.enqueueWork(() -> cast(p, ctx)));
        r.playToServer(CommitCabinet.TYPE, CommitCabinet.CODEC, (p, ctx) -> ctx.enqueueWork(() -> commit(p, ctx)));
    }

    private static void cast(CastLetter p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        if (player.distanceToSqr(p.pos().getCenter()) > REACH_SQR) return;
        if (p.letter() < 0 || p.letter() >= ModItems.LETTER_ITEMS.size()) return;
        if (!(player.level().getBlockEntity(p.pos()) instanceof MintBlockEntity be)) return;
        if (!be.use(1)) return;
        Give.items(player, ModItems.LETTER_ITEMS.get(p.letter()).get(), MintBlockEntity.YIELD);
        player.level().playSound(null, p.pos(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5f, 1.5f);
    }

    private static void commit(CommitCabinet p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        if (player.distanceToSqr(p.pos().getCenter()) > REACH_SQR) return;
        if (!(player.level().getBlockEntity(p.pos()) instanceof CabinetBlockEntity be)) return;
        be.commit(p.letters().stream().mapToInt(Integer::intValue).toArray(), p.ink(), p.paper());
    }
}
