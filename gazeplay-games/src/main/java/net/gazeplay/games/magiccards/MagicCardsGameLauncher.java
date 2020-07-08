package net.gazeplay.games.magiccards;

import javafx.scene.Scene;
import net.gazeplay.GameLifeCycle;
import net.gazeplay.GameSpec;
import net.gazeplay.IGameContext;
import net.gazeplay.commons.gamevariants.DimensionGameVariant;
import net.gazeplay.commons.utils.stats.Stats;

public class MagicCardsGameLauncher implements GameSpec.GameLauncher<Stats, DimensionGameVariant> {
    @Override
    public Stats createNewStats(Scene scene) {
        return new MagicCardsGamesStats(scene);
    }

    @Override
    public GameLifeCycle createNewGame(IGameContext gameContext,
                                       DimensionGameVariant gameVariant, Stats stats) {
        return new MagicCards(gameContext, gameVariant.getWidth(), gameVariant.getHeight(), stats);
    }
}
