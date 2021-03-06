/**
 * User: jedi-philosopher
 * Date: 29.11.12
 * Time: 20:44
 */
package ru.game.aurora.world.space;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.util.pathfinding.AStarPathFinder;
import org.newdawn.slick.util.pathfinding.PathFindingContext;
import ru.game.aurora.application.Camera;
import ru.game.aurora.application.InputBinding;
import ru.game.aurora.gui.GUI;
import ru.game.aurora.player.Player;
import ru.game.aurora.world.BasePositionable;
import ru.game.aurora.world.ITileMap;
import ru.game.aurora.world.Room;
import ru.game.aurora.world.World;
import ru.game.aurora.world.dungeon.IVictoryCondition;
import ru.game.aurora.world.planet.LandingParty;

import java.lang.ref.SoftReference;
import java.util.Collection;

public abstract class BaseSpaceRoom implements Room, ITileMap {
    private static final long serialVersionUID = 1L;

    protected Player player;

    protected transient SoftReference<AStarPathFinder> pathfinder;

    private int xClick = 0, yClick = 0;

    @Override
    public void enter(World world) {
        world.getCamera().resetViewPort();
        this.player = world.getPlayer();
    }

    @Override
    public void update(GameContainer container, World world) {
        Camera myCamera = world.getCamera();
        Input input = container.getInput();

        if (!world.isPaused() && GUI.getInstance().getNifty().getTopMostPopup() == null) {
            // do not move camera by mouse if some other window is open
            if (input.isMousePressed(Input.MOUSE_LEFT_BUTTON)) {
                xClick = container.getInput().getMouseX() - myCamera.getViewportX();
                yClick = container.getInput().getMouseY() - myCamera.getViewportY();
            }
            if (input.isMouseButtonDown(Input.MOUSE_LEFT_BUTTON)) {
                myCamera.setViewportX(container.getInput().getMouseX() - xClick);
                myCamera.setViewportY(container.getInput().getMouseY() - yClick);
            }
            if (input.isKeyPressed(Input.KEY_HOME)) {
                myCamera.resetViewPort();
            }
        }

        if (!player.getShip().nowMoving()) {
            if (input.isKeyPressed(InputBinding.keyBinding.get(InputBinding.Action.UP)) || input.isKeyPressed(InputBinding.keyBinding.get(InputBinding.Action.UP_SECONDARY))) {
                player.getShip().moveUp();
                shipMove(world);
            }
            if (input.isKeyPressed(InputBinding.keyBinding.get(InputBinding.Action.DOWN)) || input.isKeyPressed(InputBinding.keyBinding.get(InputBinding.Action.DOWN_SECONDARY))) {
                player.getShip().moveDown();
                shipMove(world);
            }
            if (input.isKeyPressed(InputBinding.keyBinding.get(InputBinding.Action.LEFT)) || input.isKeyPressed(InputBinding.keyBinding.get(InputBinding.Action.LEFT_SECONDARY))) {
                player.getShip().moveLeft();
                shipMove(world);
            }
            if (input.isKeyPressed(InputBinding.keyBinding.get(InputBinding.Action.RIGHT)) || input.isKeyPressed(InputBinding.keyBinding.get(InputBinding.Action.RIGHT_SECONDARY))) {
                player.getShip().moveRight();
                shipMove(world);
            }
        }
        player.getShip().update(container, world);
    }

    private void shipMove(World world) {
        world.getCamera().resetViewPort();
        world.setUpdatedThisFrame(true);
    }

    @Override
    public void draw(GameContainer container, Graphics g, Camera camera, World world) {
        player.getShip().draw(container, g, camera, world);
    }

    @Override
    public void draw(GameContainer container, Graphics graphics, Camera camera) {

    }

    @Override
    public boolean isTilePassable(int x, int y) {
        return true;
    }

    @Override
    public boolean isTilePassable(LandingParty landingParty, int x, int y) {
        return true;
    }

    @Override
    public boolean isTileVisible(int x, int y) {
        return true;
    }

    @Override
    public boolean lineOfSightExists(int x1, int y1, int x2, int y2) {
        return true;
    }

    @Override
    public boolean isWrapped() {
        return false;
    }

    @Override
    public int updateVisibility(int x, int y, int range) {
        return 0;
    }

    @Override
    public void setTilePassable(int x, int y, boolean isPassable) {

    }

    @Override
    public BasePositionable getEntryPoint() {
        return null;
    }

    @Override
    public Collection<IVictoryCondition> getVictoryConditions() {
        return null;
    }

    @Override
    public boolean contains(int i, int i2) {
        return true;
    }

    @Override
    public boolean isObstacle(int i, int i2) {
        return false;
    }

    @Override
    public void visit(int i, int i2) {

    }

    @Override
    public void pathFinderVisited(int i, int i2) {

    }

    @Override
    public boolean blocked(PathFindingContext pathFindingContext, int i, int i2) {
        return false;
    }

    @Override
    public float getCost(PathFindingContext pathFindingContext, int i, int i2) {
        return 1;
    }

    @Override
    public AStarPathFinder getPathFinder() {
        AStarPathFinder pf = pathfinder != null ? pathfinder.get() : null;
        if (pf == null) {
            pf = new AStarPathFinder(this, getWidthInTiles(), false);
            pathfinder = new SoftReference<>(pf);
        }
        return pf;
    }
}
