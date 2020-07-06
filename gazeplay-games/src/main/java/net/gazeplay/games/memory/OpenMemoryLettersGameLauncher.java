package net.gazeplay.games.memory;

import javafx.scene.Scene;
import net.gazeplay.GameLifeCycle;
import net.gazeplay.GameSpec;
import net.gazeplay.IGameContext;
import net.gazeplay.commons.utils.stats.Stats;
import net.gazeplay.games.magiccards.MagicCardsGamesStats;

public class OpenMemoryLettersGameLauncher implements GameSpec.GameLauncher<Stats, GameSpec.DimensionGameVariant> {
    @Override
    public Stats createNewStats(Scene scene) {
        return new MagicCardsGamesStats(scene);
    }

    @Override
    public GameLifeCycle createNewGame(IGameContext gameContext,
                                       GameSpec.DimensionGameVariant gameVariant, Stats stats) {
        return new Memory(Memory.MemoryGameType.LETTERS, gameContext, gameVariant.getWidth(),
            gameVariant.getHeight(), stats, true);
    }

    @Override
    public GameLifeCycle replayGame(IGameContext gameContext,
                                       GameSpec.DimensionGameVariant gameVariant, Stats stats, double gameSeed) {
        return new Memory(Memory.MemoryGameType.LETTERS, gameContext, gameVariant.getWidth(),
            gameVariant.getHeight(), stats, true, gameSeed);
    }
}
