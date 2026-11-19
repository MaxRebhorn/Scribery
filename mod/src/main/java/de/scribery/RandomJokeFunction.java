package de.scribery;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/** Loot-Funktion {@code scribery:random_joke}: schreibt einen zufaelligen Witz auf das Ostrakon. */
public class RandomJokeFunction extends LootItemConditionalFunction {
    public static final MapCodec<RandomJokeFunction> CODEC = RecordCodecBuilder.mapCodec(i ->
            commonFields(i).apply(i, RandomJokeFunction::new));

    protected RandomJokeFunction(List<LootItemCondition> conditions) {
        super(conditions);
    }

    @Override
    public LootItemFunctionType<RandomJokeFunction> getType() {
        return ModLoot.RANDOM_JOKE.get();
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        stack.set(ModComponents.JOKE.get(), Ostraka.ids().get(context.getRandom().nextInt(Ostraka.ids().size())));
        return stack;
    }
}
