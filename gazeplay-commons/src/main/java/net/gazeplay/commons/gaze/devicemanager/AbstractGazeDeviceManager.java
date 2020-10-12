package net.gazeplay.commons.gaze.devicemanager;

import javafx.application.Platform;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.gazeplay.commons.configuration.ActiveConfigurationContext;
import net.gazeplay.commons.configuration.Configuration;
import net.gazeplay.commons.gaze.GazeMotionListener;
import net.gazeplay.commons.utils.ImmutableCachingSupplier;
import net.gazeplay.commons.utils.RobotSupplier;
import net.gazeplay.commons.utils.stats.Stats;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Created by schwab on 04/10/2017.
 */
@Slf4j
public abstract class AbstractGazeDeviceManager implements GazeDeviceManager {

    private final List<GazeMotionListener> gazeMotionListeners = new CopyOnWriteArrayList<>();

    @Getter
    private final Map<IdentityKey<Node>, GazeInfos> shapesEventFilter = Collections.synchronizedMap(new HashMap<>());

    @Getter
    private final Map<IdentityKey<Node>, GazeInfos> shapesEventHandler = Collections.synchronizedMap(new HashMap<>());

    private final List<Node> toRemove = new LinkedList<>();
    private final List<Node> toAdd = new LinkedList<>();

    private final Supplier<Robot> robotSupplier = new ImmutableCachingSupplier<>(new RobotSupplier());

    public AbstractGazeDeviceManager() {

    }

    @Override
    public abstract void init(Supplier<Dimension2D> currentScreenDimensionSupplier, Supplier<Point2D> currentScreenPositionSupplier);

    @Override
    public abstract void destroy();

    @Override
    public void addGazeMotionListener(GazeMotionListener listener) {
        this.gazeMotionListeners.add(listener);
    }

    @Override
    public void removeGazeMotionListener(GazeMotionListener listener) {
        this.gazeMotionListeners.remove(listener);
    }

    private void notifyAllGazeMotionListeners(Point2D position) {
        for (GazeMotionListener l : this.gazeMotionListeners) {
            l.gazeMoved(position);
        }
    }

    @Override
    public void addEventFilter(Node gs) {
        toAdd.add(gs);
    }

    public void add() {
        synchronized (shapesEventFilter) {
            List<Node> temp = new LinkedList<>(toAdd);
            for (Node node : temp) {
                shapesEventFilter.put(new IdentityKey<>(node), new GazeInfos(node));
                toAdd.remove(node);
            }
        }
    }

    @Override
    public void addEventHandler(Node gs) {
        synchronized (shapesEventFilter) {
            shapesEventHandler.put(new IdentityKey<>(gs), new GazeInfos(gs));
        }
    }

    @Override
    public void removeEventFilter(Node gs) {
        toRemove.add(gs);
        if (gs instanceof Pane) {
            for (Node child : ((Pane) gs).getChildren()) {
                removeEventFilter(child);
            }
        }
    }


    public GazeInfos gameScene = null;

    public void addStats(Stats stats) {
        gameScene = new GazeInfos(stats.gameContextScene.getRoot());
    }

    public void delete() {
        synchronized (shapesEventFilter) {
            List<Node> temp = new LinkedList<>(toRemove);
            for (Node node : temp) {
                GazeInfos removed = shapesEventFilter.remove(new IdentityKey<>(node));
                if (removed == null) {
                    log.warn("EventFilter to remove not found");
                } else {
                    if (removed.isOnGaze() || removed.isOnMouse()) {
                        Platform.runLater(
                            () ->
                                removed.getNode().fireEvent(new GazeEvent(GazeEvent.GAZE_EXITED, System.currentTimeMillis(), 0, 0))
                        );
                    }
                }
                toRemove.remove(node);
            }
        }
    }

    @Override
    public void removeEventHandler(Node gs) {
        synchronized (shapesEventFilter) {
            GazeInfos removed = shapesEventHandler.remove(new IdentityKey<>(gs));
            if (removed == null) {
                log.warn("EventHandler to remove not found");
            }
        }
    }

    /**
     * Clear all Nodes in both EventFilter and EventHandler. There is no more gaze event after this function is called
     */
    @Override
    public void clear() {
        synchronized (shapesEventFilter) {
            shapesEventFilter.clear();
            shapesEventHandler.clear();
            gazeMotionListeners.clear();
        }
    }

    synchronized void onGazeUpdate(Point2D gazePositionOnScreen, String event) {
        // notifyAllGazeMotionListeners(gazePositionOnScreen);
        final double positionX = gazePositionOnScreen.getX();
        final double positionY = gazePositionOnScreen.getY();
        updatePosition(positionX, positionY, event);
    }

    @Override
    synchronized public void onSavedMovementsUpdate(Point2D gazePositionOnScene, String event) {
            if (gameScene != null) {
                Point2D gazePositionOnScreen = gameScene.getNode().localToScreen(gazePositionOnScene);
                if(gazePositionOnScreen!=null) {
                    final double positionX = gazePositionOnScreen.getX();
                    final double positionY = gazePositionOnScreen.getY();
                    updatePosition(positionX, positionY, event);
                }
            }
    }

    void updatePosition(double positionX, double positionY, String event) {

        Configuration config = ActiveConfigurationContext.getInstance();

        if (config.isGazeMouseEnable() && !config.isMouseFree()) {
            Platform.runLater(
                () -> robotSupplier.get().mouseMove((int) positionX, (int) positionY)
            );
        }

        add();
        delete();

        synchronized (shapesEventFilter) {
            Collection<GazeInfos> c = shapesEventFilter.values();
            for (GazeInfos gi : c) {
                if (gameScene != null && gi.getNode() != gameScene.getNode()) {
                    if(eventFire(positionX, positionY, gi, event, c)){
                        break;
                    }
                }
            }

            if (gameScene != null) {
                eventFire(positionX, positionY, gameScene, event);
            }

        }
    }

    public boolean isOnTopOfContains(Node node, double positionX, double positionY, Collection<GazeInfos> c){
        if(contains(node,positionX,positionY)){
            if (c != null) {
                for (GazeInfos gi : c) {
                    Node giNode = gi.getNode();
                    if (giNode != gameScene.getNode() && contains(giNode, positionX, positionY)) {
                        log.info("is false {} > {} ", giNode.getViewOrder(), node.getViewOrder());
                        if ( giNode.getViewOrder() > node.getViewOrder() ){
                             return false;
                        }
                    }
                }
            }
            return true;
        }

        return false;

    };

    public boolean contains(Node node, double positionX, double positionY) {
        Point2D localPosition = node.screenToLocal(positionX, positionY);
        int offset = 5;
        if(node!=null && localPosition!=null) {
            try {
                return (
                    node.contains(localPosition.getX(), localPosition.getY()) /*||
                    node.contains(localPosition.getX() + offset, localPosition.getY()) ||
                    node.contains(localPosition.getX() + offset, localPosition.getY() + offset) ||
                    node.contains(localPosition.getX() + offset, localPosition.getY() - offset) ||
                    node.contains(localPosition.getX() - offset, localPosition.getY()) ||
                    node.contains(localPosition.getX() - offset, localPosition.getY() + offset) ||
                    node.contains(localPosition.getX() - offset, localPosition.getY() - offset) ||
                    node.contains(localPosition.getX(), localPosition.getY() + offset) ||
                    node.contains(localPosition.getX(), localPosition.getY() - offset)*/
                );
            } catch (IndexOutOfBoundsException e) {
                return false;
            }
        } return false;
    }


    public void eventFire(double positionX, double positionY, GazeInfos gi, String event) {
        eventFire(positionX,positionY,gi,event,null);
    }

    public boolean eventFire(double positionX, double positionY, GazeInfos gi, String event, Collection<GazeInfos> c) {
        Node node = gi.getNode();
        if (!node.isDisable()) {

            Point2D localPosition = node.screenToLocal(positionX, positionY);

            if (localPosition != null && isOnTopOfContains(node, positionX, positionY, c)) {
                if (event.equals("gaze")) {
                    if (gi.isOnGaze()) {
                        Platform.runLater(
                            () ->
                                node.fireEvent(new GazeEvent(GazeEvent.GAZE_MOVED, gi.getTime(), localPosition.getX(), localPosition.getY()))
                        );
                        return true;
                    } else {

                        gi.setOnGaze(true);
                        gi.setTime(System.currentTimeMillis());
                        Platform.runLater(
                            () ->
                                node.fireEvent(new GazeEvent(GazeEvent.GAZE_ENTERED, gi.getTime(), localPosition.getX(), localPosition.getY()))
                        );
                        return true;
                    }
                } else {
                    if (gi.isOnMouse()) {
                        Platform.runLater(
                            () ->
                                 node.fireEvent(new GazeEvent(GazeEvent.GAZE_MOVED, gi.getTime(), localPosition.getX(), localPosition.getY()))
//                                node.fireEvent(new MouseEvent(MouseEvent.MOUSE_MOVED,
//                                    localPosition.getX(), localPosition.getY(), positionX, positionY, MouseButton.PRIMARY, 1,
//                                    true, true, true, true, true, true, true, true, true, true, null))

                        );
                        return true;
                    } else {

                        gi.setOnMouse(true);
                        gi.setTime(System.currentTimeMillis());
                        Platform.runLater(
                            () ->
                                 node.fireEvent(new GazeEvent(GazeEvent.GAZE_ENTERED, gi.getTime(), localPosition.getX(), localPosition.getY()))
//                                node.fireEvent(new MouseEvent(MouseEvent.MOUSE_ENTERED,
//                                    localPosition.getX(), localPosition.getY(), positionX, positionY, MouseButton.PRIMARY, 1,
//                                    true, true, true, true, true, true, true, true, true, true, null))

                        );
                        return true;
                    }
                }
            } else {// gaze is not on the shape
                if (event.equals("gaze")) {
                    if (gi.isOnGaze()) {// gaze was on the shape previously
                        gi.setOnGaze(false);
                        gi.setTime(-1);
                        if (localPosition != null) {
                            Platform.runLater(
                                () ->
                                    node.fireEvent(new GazeEvent(GazeEvent.GAZE_EXITED, gi.getTime(), localPosition.getX(), localPosition.getY()))
                            );
                        } else {
                            // nothing to do
                        }
                    } else {// gaze was not on the shape previously
                        // nothing to do

                    }
                } else {
                    if (gi.isOnMouse()) {// gaze was on the shape previously
                        gi.setOnMouse(false);
                        gi.setTime(-1);
                        if (localPosition != null) {
                            Platform.runLater(
                                () ->
                                     node.fireEvent(new GazeEvent(GazeEvent.GAZE_EXITED, gi.getTime(), localPosition.getX(), localPosition.getY()))
//                                    node.fireEvent(new MouseEvent(MouseEvent.MOUSE_EXITED,
//                                        localPosition.getX(), localPosition.getY(), positionX, positionY, MouseButton.PRIMARY, 1,
//                                        true, true, true, true, true, true, true, true, true, true, null))

                            );
                        } else {
                            // nothing to do
                        }
                    } else {// gaze was not on the shape previously
                        // nothing to do

                    }
                }

            }
        }
        return false;
    }

}
