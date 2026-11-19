package de.scribery;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Loot-Funktion {@code scribery:random_manuscript}: macht aus dem Eintrag ein zufaelliges Manuskript der Fundart
 * ({@code "find": "sand" | "mud" | "ashes" | "ice" | "shards"}) und setzt das Aussehen passend zum Fundblock.
 * Ob ueberhaupt etwas herauskommt, regelt die Loot-Tabelle (Bedingung random_chance).
 */
public class RandomManuscriptFunction extends LootItemConditionalFunction {
    public static final MapCodec<RandomManuscriptFunction> CODEC = RecordCodecBuilder.mapCodec(i ->
            commonFields(i).and(Codec.STRING.fieldOf("find").forGetter(f -> f.find)).apply(i, RandomManuscriptFunction::new));

    private final String find;

    protected RandomManuscriptFunction(List<LootItemCondition> conditions, String find) {
        super(conditions);
        this.find = find;
    }

    @Override
    public LootItemFunctionType<RandomManuscriptFunction> getType() {
        return ModLoot.RANDOM_MANUSCRIPT.get();
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        String id = FindPools.pick(find, context.getRandom());
        if (id == null) return stack;
        return ManuscriptFragmentItem.of(id);   // das Aussehen setzt of() nach dem Manuskript, siehe FindPools.variantOf
    }
}
