package net.gazeplay;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.gazeplay.commons.configuration.ConfigurationBuilder;

/**
 * Created by schwab on 17/12/2016.
 */
@Slf4j
public class GazePlay extends Application {

    @Getter
    private static GazePlay instance;

    @Getter
    private HomeMenuScreen homeMenuScreen;

    @Getter
    private Stage primaryStage;

    public GazePlay() {
        instance = this;
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        Screen screen = Screen.getPrimary();

        Rectangle2D screenBounds = screen.getBounds();

        primaryStage.setWidth(screenBounds.getWidth() * 0.95);
        primaryStage.setHeight(screenBounds.getHeight() * 0.90);

        primaryStage.setMaximized(false);

        homeMenuScreen = HomeMenuScreen.newInstance(this, ConfigurationBuilder.createFromPropertiesResource().build());
        homeMenuScreen.setUpOnStage(primaryStage);

        primaryStage.centerOnScreen();

        primaryStage.setFullScreen(true);
    }

    public void onGameLaunch(GameContext gameContext) {
        gameContext.setUpOnStage(primaryStage);
    }

    public void onReturnToMenu() {
        homeMenuScreen.setUpOnStage(primaryStage);
    }

    public void onDisplayStats(StatsContext statsContext) {
        statsContext.setUpOnStage(primaryStage);
    }

    public void onDisplayConfigurationManagement(ConfigurationContext configurationContext) {
        configurationContext.setUpOnStage(primaryStage);
    }

    public void toggleFullScreen() {
        boolean fullScreen = !primaryStage.isFullScreen();
        log.info("fullScreen = {}", fullScreen);
        primaryStage.setFullScreen(fullScreen);
        primaryStage.show();
    }

}
