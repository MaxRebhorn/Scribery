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

    /**
     * Was in der Presse verbraucht wurde: Verschleiss je Letter-Typ, Tinte, Papier, dazu die neue Setzform
     * und die neu gedruckten Boegen.
     */
    public record CommitCabinet(BlockPos pos, List<Integer> lost, int inkUsed, int paperUsed, String forme,
            List<SheetOut> sheets) implements CustomPacketPayload {
        public static final Type<CommitCabinet> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Scribery.MODID, "commit_cabinet"));
        public static final StreamCodec<ByteBuf, CommitCabinet> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, CommitCabinet::pos,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(64)), CommitCabinet::lost,
                ByteBufCodecs.VAR_INT, CommitCabinet::inkUsed,
                ByteBufCodecs.VAR_INT, CommitCabinet::paperUsed,
                ByteBufCodecs.STRING_UTF8, CommitCabinet::forme,
                SheetOut.CODEC.apply(ByteBufCodecs.list(16)), CommitCabinet::sheets,
                CommitCabinet::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Der Spieler nimmt das aufgelegte Fragment vom Pult oder den Bogen vom Uebersetzungstisch zurueck. */
    public record TakeItem(BlockPos pos) implements CustomPacketPayload {
        public static final Type<TakeItem> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Scribery.MODID, "take_item"));
        public static final StreamCodec<ByteBuf, TakeItem> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, TakeItem::pos, TakeItem::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Der Spieler will Vorraete aus seinem Inventar in den Schrank fuellen. mode 0 = Letter Types, 1 = Tinte und Papier. */
    public record Refill(BlockPos pos, int mode) implements CustomPacketPayload {
        public static final Type<Refill> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Scribery.MODID, "refill"));
        public static final StreamCodec<ByteBuf, Refill> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Refill::pos, ByteBufCodecs.VAR_INT, Refill::mode, Refill::new);

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

    /** Vom Server zum Client: die ganze Sammlung des Spielers im Manuscript Archive (siehe ArchiveService). */
    public record SyncArchive(List<ArchiveData.Entry> entries) implements CustomPacketPayload {
        public static final Type<SyncArchive> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Scribery.MODID, "sync_archive"));
        public static final StreamCodec<ByteBuf, SyncArchive> CODEC = StreamCodec.composite(
                ArchiveData.Entry.STREAM_CODEC.apply(ByteBufCodecs.list(4096)), SyncArchive::entries, SyncArchive::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1");
        r.playToClient(SyncArchive.TYPE, SyncArchive.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> de.scribery.client.ClientArchive.set(ArchiveData.fromEntries(p.entries()))));
        r.playToServer(CommitCabinet.TYPE, CommitCabinet.CODEC, (p, ctx) -> ctx.enqueueWork(() -> commit(p, ctx)));
        r.playToServer(TakeItem.TYPE, TakeItem.CODEC, (p, ctx) -> ctx.enqueueWork(() -> take(p, ctx)));
        r.playToServer(Refill.TYPE, Refill.CODEC, (p, ctx) -> ctx.enqueueWork(() -> refill(p, ctx)));
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

    private static void take(TakeItem p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        if (player.distanceToSqr(p.pos().getCenter()) > REACH_SQR) return;
        var be = player.level().getBlockEntity(p.pos());
        net.minecraft.world.item.ItemStack back = net.minecraft.world.item.ItemStack.EMPTY;
        if (be instanceof LecternBlockEntity l) {
            back = l.fragment();
            l.setFragment(net.minecraft.world.item.ItemStack.EMPTY);
        } else if (be instanceof DeskBlockEntity d) {
            back = d.sheet();
            d.setSheet(net.minecraft.world.item.ItemStack.EMPTY);
        }
        if (back.isEmpty()) return;
        if (!player.getInventory().add(back)) player.drop(back, false);
        player.level().playSound(null, p.pos(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1f, 1f);
    }

    private static void refill(Refill p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        if (player.distanceToSqr(p.pos().getCenter()) > REACH_SQR) return;
        if (!(player.level().getBlockEntity(p.pos()) instanceof CabinetBlockEntity be)) return;
        var inv = player.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            var stack = inv.items.get(i);
            if (stack.isEmpty()) continue;
            if (p.mode() == 0) {
                int letter = ModItems.letterIndex(stack.getItem());
                if (letter < 0) continue;
                be.addLetters(letter, stack.getCount());
            } else if (stack.is(Items.INK_SAC)) {
                be.addInk(stack.getCount() * CabinetBlockEntity.INK_PER_SAC);
            } else if (stack.is(Items.PAPER)) {
                be.addPaper(stack.getCount());
            } else {
                continue;
            }
            inv.items.set(i, net.minecraft.world.item.ItemStack.EMPTY);
        }
        player.level().playSound(null, p.pos(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.2f);
    }

    private static void commit(CommitCabinet p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        if (player.distanceToSqr(p.pos().getCenter()) > REACH_SQR) return;
        if (!(player.level().getBlockEntity(p.pos()) instanceof CabinetBlockEntity be)) return;
        if (!be.commit(p.lost().stream().mapToInt(Integer::intValue).toArray(), p.inkUsed(), p.paperUsed(), p.forme())) return;
        // Jeder Bogen kostet ein Blatt Papier: nie mehr Boegen, als Papier verbraucht wurde.
        int allowed = p.paperUsed();
        for (SheetOut s : p.sheets()) {
            if (allowed-- <= 0) break;
            if (s.lines().size() > 16 || s.lines().stream().anyMatch(l -> l.length() > 64)) continue;
            var stack = SheetItem.of(ModItems.PRINTED_SHEET.get(), new SheetData(s.fragment(), s.page(), s.lines(), List.of()));
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
    }
}
