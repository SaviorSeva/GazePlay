package net.gazeplay.games.bera;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.gazeplay.IGameContext;
import net.gazeplay.commons.configuration.Configuration;
import net.gazeplay.commons.gaze.devicemanager.GazeEvent;
import net.gazeplay.commons.utils.stats.Stats;

@Slf4j
@ToString
@Getter
class PictureCard extends Group {

    private final double minTime;
    private final IGameContext gameContext;
    private final boolean winner;

    private final ImageView imageRectangle;
    private final Rectangle errorImageRectangle;

    private final double initialWidth;
    private final double initialHeight;

    private final double initialPositionX;
    private final double initialPositionY;

    private final Stats stats;
    private final String imagePath;
    private final PictureCard.CustomInputEventHandlerMouse customInputEventHandlerMouse;
    private final Bera gameInstance;
    private ProgressIndicator progressIndicator;
    private Timeline progressIndicatorAnimationTimeLine;
    private boolean selected;
    private boolean alreadySee;

    PictureCard(double posX, double posY, double width, double height, @NonNull IGameContext gameContext,
                boolean winner, @NonNull String imagePath, @NonNull Stats stats, Bera gameInstance) {

        log.info("imagePath = {}", imagePath);

        final Configuration config = gameContext.getConfiguration();

        this.minTime = config.getFixationLength();
        this.initialPositionX = posX;
        this.initialPositionY = posY;
        this.initialWidth = width;
        this.initialHeight = height;
        this.selected = false;
        this.alreadySee = false;
        this.winner = winner;
        this.gameContext = gameContext;
        this.stats = stats;
        this.gameInstance = gameInstance;

        this.imagePath = imagePath;

        this.imageRectangle = createImageView(posX, posY, width, height, imagePath);

        this.progressIndicator = buildProgressIndicator(width, height);

        this.errorImageRectangle = createErrorImageRectangle();

        this.getChildren().add(imageRectangle);
        this.getChildren().add(progressIndicator);
        this.getChildren().add(errorImageRectangle);

        customInputEventHandlerMouse = new PictureCard.CustomInputEventHandlerMouse();

        gameContext.getGazeDeviceManager().addEventFilter(imageRectangle);

        this.addEventFilter(MouseEvent.ANY, customInputEventHandlerMouse);
        this.addEventFilter(GazeEvent.ANY, customInputEventHandlerMouse);

    }

    private Timeline createProgressIndicatorTimeLine(Bera gameInstance) {
        Timeline result = new Timeline();

        result.getKeyFrames()
            .add(new KeyFrame(new Duration(2000), new KeyValue(progressIndicator.progressProperty(), 1)));

        EventHandler<ActionEvent> progressIndicatorAnimationTimeLineOnFinished = createProgressIndicatorAnimationTimeLineOnFinished(
            gameInstance);

        result.setOnFinished(progressIndicatorAnimationTimeLineOnFinished);

        return result;
    }

    private EventHandler<ActionEvent> createProgressIndicatorAnimationTimeLineOnFinished(Bera gameInstance) {
        return actionEvent -> {

            log.debug("FINISHED");

            if (this.alreadySee) {
                selected = true;
                imageRectangle.removeEventFilter(MouseEvent.ANY, customInputEventHandlerMouse);
                imageRectangle.removeEventFilter(GazeEvent.ANY, customInputEventHandlerMouse);
                gameContext.getGazeDeviceManager().removeEventFilter(imageRectangle);
                if (winner) {
                    onCorrectCardSelected();
                } else {
                    // bad card
                    onWrongCardSelected();
                }
            } else {
                this.alreadySee = true;
                customInputEventHandlerMouse.ignoreAnyInput = true;
                this.newProgressIndicator();
                gameInstance.checkAllPictureCardChecked();
            }
        };
    }

    public void setVisibleProgressIndicator() {
        customInputEventHandlerMouse.ignoreAnyInput = false;
    }

    public void newProgressIndicator() {
        this.getChildren().remove(progressIndicator);
        this.progressIndicator = buildProgressIndicator(initialWidth, initialHeight);
        this.getChildren().add(progressIndicator);
    }

    public void onCorrectCardSelected() {

        if (gameInstance.indexFileImage == 19) {
            gameInstance.increaseIndexFileImage(true);
            this.endGame();
        } else {
            gameInstance.increaseIndexFileImage(true);

            stats.incrementNumberOfGoalsReached();

            customInputEventHandlerMouse.ignoreAnyInput = true;
            progressIndicator.setVisible(false);

            gameContext.updateScore(stats, gameInstance);

            gameInstance.dispose();
            gameContext.clear();
            gameInstance.launch();
        }

    }

    public void onWrongCardSelected() {

        if (gameInstance.indexFileImage == 19) {
            this.endGame();
        } else {
            gameInstance.increaseIndexFileImage(false);

            stats.incrementNumberOfGoalsReached();

            customInputEventHandlerMouse.ignoreAnyInput = true;
            progressIndicator.setVisible(false);

            gameContext.updateScore(stats, gameInstance);

            gameInstance.dispose();
            gameContext.clear();
            gameInstance.launch();
        }
    }

    private ImageView createImageView(double posX, double posY, double width, double height,
                                      @NonNull String imagePath) {
        final Image image = new Image(imagePath);

        ImageView result = new ImageView(image);

        result.setFitWidth(width);
        result.setFitHeight(height);

        double ratioX = result.getFitWidth() / image.getWidth();
        double ratioY = result.getFitHeight() / image.getHeight();

        double reducCoeff = Math.min(ratioX, ratioY);

        double w = image.getWidth() * reducCoeff;
        double h = image.getHeight() * reducCoeff;

        result.setX(posX);
        result.setY(posY);
        result.setTranslateX((result.getFitWidth() - w) / 2);
        result.setTranslateY((result.getFitHeight() - h) / 2);
        result.setPreserveRatio(true);

        return result;
    }

    private ImageView createStretchedImageView(double posX, double posY, double width, double height,
                                               @NonNull String imagePath) {
        final Image image = new Image(imagePath);

        ImageView result = new ImageView(image);

        result.setFitWidth(width);
        result.setFitHeight(height);

        result.setX(posX);
        result.setY(posY);
        result.setPreserveRatio(false);

        return result;
    }

    private Rectangle createErrorImageRectangle() {
        final Image image = new Image("data/common/images/error.png");

        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();
        double imageHeightToWidthRatio = imageHeight / imageWidth;

        double rectangleWidth = imageRectangle.getFitWidth() / 3;
        double rectangleHeight = imageHeightToWidthRatio * rectangleWidth;

        double positionX = imageRectangle.getX() + (imageRectangle.getFitWidth() - rectangleWidth) / 2;
        double positionY = imageRectangle.getY() + (imageRectangle.getFitHeight() - rectangleHeight) / 2;

        Rectangle errorImageRectangle = new Rectangle(rectangleWidth, rectangleHeight);
        errorImageRectangle.setFill(new ImagePattern(image));
        errorImageRectangle.setX(positionX);
        errorImageRectangle.setY(positionY);
        errorImageRectangle.setOpacity(0);
        errorImageRectangle.setVisible(false);
        return errorImageRectangle;
    }

    private ProgressIndicator buildProgressIndicator(double parentWidth, double parentHeight) {
        double minWidth = parentWidth / 2;
        double minHeight = parentHeight / 2;

        double positionX = imageRectangle.getX() + (parentWidth - minWidth) / 2;
        double positionY = imageRectangle.getY() + (parentHeight - minHeight) / 2;

        ProgressIndicator result = new ProgressIndicator(0);
        result.setTranslateX(positionX);
        result.setTranslateY(positionY);
        result.setMinWidth(minWidth);
        result.setMinHeight(minHeight);
        result.setOpacity(0.5);
        result.setVisible(false);
        return result;
    }

    public void endGame() {

        progressIndicator.setVisible(false);
        gameInstance.finalStats();
        gameContext.updateScore(stats, gameInstance);

        gameContext.playWinTransition(0, event -> {
            gameInstance.dispose();

            gameContext.clear();

            gameContext.showRoundStats(stats, gameInstance);
        });
    }

    private class CustomInputEventHandlerMouse implements EventHandler<Event> {

        /**
         * this is used to temporarily indicate to ignore input for instance, when an animation is in progress, we
         * do not want the game to continue to process input, as the user input is irrelevant while the animation is
         * in progress
         */
        private boolean ignoreAnyInput = false;

        @Override
        public void handle(Event e) {
            if (ignoreAnyInput) {
                return;
            }

            if (selected) {
                return;
            }

            if (e.getEventType() == MouseEvent.MOUSE_ENTERED || e.getEventType() == GazeEvent.GAZE_ENTERED) {
                onEntered();
            } else if (e.getEventType() == MouseEvent.MOUSE_EXITED || e.getEventType() == GazeEvent.GAZE_EXITED) {
                onExited();
            }
        }

        private void onEntered() {
            log.info("ENTERED {}", imagePath);

            progressIndicatorAnimationTimeLine = createProgressIndicatorTimeLine(gameInstance);

            progressIndicator.setProgress(0);
            progressIndicator.setVisible(true);

            progressIndicatorAnimationTimeLine.playFromStart();
        }

        private void onExited() {
            log.info("EXITED {}", imagePath);

            progressIndicatorAnimationTimeLine.stop();

            progressIndicator.setVisible(false);
            progressIndicator.setProgress(0);
        }

    }

}

