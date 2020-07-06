package net.gazeplay.games.race;

import javafx.scene.Scene;
import net.gazeplay.GameLifeCycle;
import net.gazeplay.GameSpec;
import net.gazeplay.IGameContext;
import net.gazeplay.commons.utils.stats.Stats;

public class FrogsRaceGameLauncher implements GameSpec.GameLauncher {
    @Override
    public Stats createNewStats(Scene scene) {
        return new RaceGamesStats(scene, "race");
    }

    @Override
    public GameLifeCycle createNewGame(IGameContext gameContext, GameSpec.GameVariant gameVariant,
                                       Stats stats) {
        return new Race(gameContext, stats, "race");
    }

    @Override
    public GameLifeCycle replayGame(IGameContext gameContext, GameSpec.GameVariant gameVariant,
                                       Stats stats, double gameSeed) {
        return new Race(gameContext, stats, "race", gameSeed);
    }
}
