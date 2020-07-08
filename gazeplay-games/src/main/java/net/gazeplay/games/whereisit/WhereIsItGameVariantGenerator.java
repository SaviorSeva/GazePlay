package net.gazeplay.games.whereisit;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.gazeplay.GameSpec;
import net.gazeplay.commons.gamevariants.DimensionGameVariant;
import net.gazeplay.commons.gamevariants.IGameVariant;
import net.gazeplay.commons.gamevariants.generators.IGameVariantGenerator;

import java.util.Set;

public class WhereIsItGameVariantGenerator implements IGameVariantGenerator {
    @Override
    public Set<IGameVariant> getVariants() {
        return Sets.newLinkedHashSet(Lists.newArrayList(
            new DimensionGameVariant(2, 2),
            new DimensionGameVariant(2, 3),
            new DimensionGameVariant(3, 2),
            new DimensionGameVariant(3, 3),
            new GameSpec.DimensionDifficultyGameVariant(2, 2, "easy"),
            new GameSpec.DimensionDifficultyGameVariant(2, 3, "easy"),
            new GameSpec.DimensionDifficultyGameVariant(3, 2, "easy")
        ));
    }
}
