package de.scribery;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Das Manuscript Archive. Rechtsklick mit einem Fragment, einem gedruckten oder einem uebersetzten Bogen traegt es in die
 * Sammlung des Spielers ein (das Item wandert ins Archiv; Schleichen + leere Hand nimmt das zuletzt abgelegte Fragment wieder heraus, der Fortschritt bleibt). Ein Duplikat bleibt beim Spieler und ist fuer den Scribe Villager. Rechtsklick mit leerer
 * Hand oeffnet den Text des Evangeliums mit seinen drei Spalten.
 */
public class ArchiveBlock extends FacingBlock {
    public static final MapCodec<ArchiveBlock> CODEC = simpleCodec(ArchiveBlock::new);

    public ArchiveBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        boolean fragment = held.getItem() instanceof ManuscriptFragmentItem;
        boolean printed = held.is(ModItems.PRINTED_SHEET.get()), translated = held.is(ModItems.TRANSLATED_SHEET.get());
        if (!fragment && !printed && !translated) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return ItemInteractionResult.CONSUME;

        ArchiveService.Result result;
        if (fragment) {
            result = ArchiveService.registerFragment(sp, held);
        } else {
            SheetData sheet = held.get(ModComponents.SHEET.get());
            result = sheet == null ? ArchiveService.Result.NOT_ACCEPTED : ArchiveService.registerSheet(sp, sheet, translated);
        }
        switch (result) {
            case ADDED -> {
                if (fragment) {
                    ArchiveService.storeFragment(sp, held.get(ModComponents.MANUSCRIPT.get()));   // Item wandert ins Archiv
                }
                if (!player.getAbilities().instabuild) held.shrink(1);
                level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 1f);
                ArchiveService.message(player, fragment ? "Filed in the archive: the Greek text is now in your collection. Sneak and click with an empty hand to take it out again."
                        : translated ? "Filed in the archive: the translation is now in your collection"
                        : "Filed in the archive: the transliteration is now in your collection");
            }
            case ALREADY_THERE -> ArchiveService.message(player, fragment
                    ? "The archive already has this one. Keep it as a duplicate: the Scribe takes loose fragments."
                    : "The archive already has this one. Keep it as a duplicate.");
            case NOT_ACCEPTED -> ArchiveService.message(player, "The archive cannot file this.");
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {   // Schleichen + leere Hand: zuletzt abgelegtes Fragment herausnehmen
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                ItemStack out = ArchiveService.takeOutFragment(sp);
                if (out == null) {
                    ArchiveService.message(player, "Nothing stored in the archive.");
                } else {
                    if (!player.getInventory().add(out)) player.drop(out, false);
                    level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 0.8f);
                    ArchiveService.message(player, "Took a fragment out of the archive (" + ArchiveService.storedCount(sp) + " left).");
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) {
            de.scribery.client.ClientHooks.openArchive();
        } else if (player instanceof ServerPlayer sp) {
            ArchiveService.sync(sp);   // aktueller Stand, falls sich seit der Anmeldung etwas geaendert hat
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
