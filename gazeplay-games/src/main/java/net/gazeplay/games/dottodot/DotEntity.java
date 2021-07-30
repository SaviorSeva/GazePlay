package net.gazeplay.games.dottodot;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.gazeplay.IGameContext;
import net.gazeplay.commons.gaze.devicemanager.GazeEvent;
import net.gazeplay.commons.utils.stats.Stats;

import javafx.scene.image.ImageView;

import java.util.ArrayList;

@Slf4j
public class DotEntity extends Parent {
    private final IGameContext gameContext;
    private final ProgressIndicator progressIndicator;
    private Timeline progressTimeline;
    private final Stats stats;
    private final DotToDotGameVariant gameVariant;
    private static DotToDot gameObject;
    final private int index;
    private boolean isFirst = false;

    @Setter @Getter
    private int previous;

    public DotEntity (final ImageView imageView, final Stats stats,
                      final ProgressIndicator progressIndicator, final Text number, final IGameContext gameContext, final DotToDotGameVariant gameVariant, DotToDot gameInstance, int index) {

        this.gameContext = gameContext;
        this.progressIndicator = progressIndicator;
        this.stats = stats;
        this.gameObject = gameInstance;
        this.index = index;
        this.gameVariant = gameVariant;

        if (this.index == 1)
            isFirst = true;
        else
            previous = index - 1;

        this.getChildren().addAll(number, imageView, this.progressIndicator);

        final EventHandler<Event> enterHandler = (Event event) -> {
            progressTimeline = new Timeline(
                new KeyFrame(new Duration(gameContext.getConfiguration().getFixationLength()), new KeyValue(this.progressIndicator.progressProperty(), 1)));

            progressTimeline.setOnFinished(e -> drawTheLine());

            this.progressIndicator.setOpacity(1);
            progressTimeline.playFromStart();
        };

        this.addEventFilter(MouseEvent.MOUSE_ENTERED, enterHandler);
        this.addEventFilter(GazeEvent.GAZE_ENTERED, enterHandler);

        final EventHandler<Event> exitHandler = (Event event) -> {
            this.progressIndicator.setOpacity(0);
            this.progressIndicator.setProgress(0);
            progressTimeline.stop();
        };

        this.addEventFilter(MouseEvent.MOUSE_EXITED, exitHandler);
        this.addEventFilter(GazeEvent.GAZE_EXITED, exitHandler);
    }

    public void drawTheLine() {
        if (previous == gameObject.getPrevious()) {
            if (gameVariant.getLabel().contains("Order")
                && index != gameObject.getTargetAOIList().size()
                && !gameContext.getChildren().contains(gameObject.getDotList().get(index))) {
                gameObject.positioningDot(gameObject.getDotList().get(index));

            }

            nextDot(gameObject.getTargetAOIList().get(index - 2).getXValue(), gameObject.getTargetAOIList().get(index - 2).getYValue(),
                gameObject.getTargetAOIList().get(index - 1).getXValue(), gameObject.getTargetAOIList().get(index - 1).getYValue());
            gameObject.setPrevious(index);

        } else if (isFirst && gameObject.getPrevious() == gameObject.getTargetAOIList().size()) {
            nextDot(gameObject.getTargetAOIList().get(gameObject.getTargetAOIList().size() - 1).getXValue(), gameObject.getTargetAOIList().get(gameObject.getTargetAOIList().size() - 1).getYValue(),
                gameObject.getTargetAOIList().get(0).getXValue(), gameObject.getTargetAOIList().get(0).getYValue());

            stats.incrementNumberOfGoalsReached();
            log.debug("level = {}, nbGoalsReached = {}, fails = {}", gameObject.getLevel(), stats.nbGoalsReached, gameObject.getFails());
            gameObject.getListOfFails().add(gameObject.getFails());
            gameObject.setFails(0);

            if(gameVariant.getLabel().contains("Dynamic")) {
                if (stats.nbGoalsReached > 0 && stats.nbGoalsReached % 3 == 0) {
                    if (nextLevelDecision() && gameObject.getLevel() < 7)
                        gameObject.setLevel(gameObject.getLevel() + 1);

                    if (!nextLevelDecision() && gameObject.getLevel() > 1)
                        gameObject.setLevel(gameObject.getLevel() - 1);
                }
            }

            gameObject.setPrevious(1);

            gameContext.updateScore(stats, gameObject);
            gameObject.getTargetAOIList().clear();
            gameObject.getDotList().clear();

            gameContext.playWinTransition(500, actionEvent -> {
                gameObject.dispose();
                gameContext.clear();
                gameObject.launch();
            });

        } else {
            gameObject.catchFail();
            gameContext.getChildren().removeAll(gameObject.getLineList());

            /*if(gameVariant.getLabel().contains("Order")) {
                gameObject.getGameContext().getChildren().removeAll(gameObject.getDotList());
                //why duplicated if it xas removed ???
                gameObject.getGameContext().getChildren().addAll(gameObject.getDotList().get(0), gameObject.getDotList().get(0));
            }*/

            gameObject.setPrevious(1);
        }
    }

    public void nextDot(double startX, double startY, double endX, double endY) {
        Line line = new Line(startX + 20, startY, endX + 20, endY);
        line.setStyle("-fx-stroke: red;");
        line.setStrokeWidth(5);

        gameObject.getLineList().add(line);
        gameContext.getChildren().add(line);
    }

    public boolean nextLevelDecision() {
        int nbOfGoalsReached = stats.nbGoalsReached;
        int compare = 3;
        for (int i = 0; i < 3; i++) {
            if (gameObject.getListOfFails().get(nbOfGoalsReached - i - 1) > 2)
                compare--;
        }

        return (compare == 3);
    }
}
