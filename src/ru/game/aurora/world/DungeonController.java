/**
 * Created with IntelliJ IDEA.
 * User: User
 * Date: 10.09.13
 * Time: 22:02
 */

package ru.game.aurora.world;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.elements.Element;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import ru.game.aurora.application.Camera;
import ru.game.aurora.application.GameLogger;
import ru.game.aurora.application.ResourceManager;
import ru.game.aurora.effects.BlasterShotEffect;
import ru.game.aurora.effects.Effect;
import ru.game.aurora.gui.GUI;
import ru.game.aurora.util.EngineUtils;
import ru.game.aurora.world.dungeon.IVictoryCondition;
import ru.game.aurora.world.planet.LandingParty;
import ru.game.aurora.world.planet.PlanetObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Contains logic for player landing party movement and combat
 */
public class DungeonController implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Mode for moving. Arrows control landing party movement.
     */
    private static final int MODE_MOVE = 0;

    /**
     * Mode for shooting. Arrows control target selection.
     */
    private static final int MODE_SHOOT = 1;

    private Room prevRoom;
    /**
     * Current mode
     */
    private int mode = MODE_MOVE;

    private ITileMap map;

    private boolean isWrap;

    private World world;

    private LandingParty landingParty;

    private transient Effect currentEffect = null;

    /**
     * When in fire mode, this is currently selected target
     */
    private transient PlanetObject target = null;

    public DungeonController(World world, Room prevRoom, ITileMap map, boolean wrap) {
        this.isWrap = wrap;
        this.prevRoom = prevRoom;
        this.map = map;
        this.world = world;
        this.landingParty = world.getPlayer().getLandingParty();
    }

    /**
     * This update is used in MOVE mode. Moving landing party around.
     */
    private void updateMove(GameContainer container, World world) {
        int x = landingParty.getX();
        int y = landingParty.getY();

        if (container.getInput().isKeyPressed(Input.KEY_UP)) {
            y--;
            world.setUpdatedThisFrame(true);
        }
        if (container.getInput().isKeyPressed(Input.KEY_DOWN)) {
            y++;
            world.setUpdatedThisFrame(true);
        }

        if (container.getInput().isKeyPressed(Input.KEY_LEFT)) {
            x--;
            world.setUpdatedThisFrame(true);
        }
        if (container.getInput().isKeyPressed(Input.KEY_RIGHT)) {
            x++;
            world.setUpdatedThisFrame(true);
        }
        if (isWrap) {
            x = EngineUtils.wrap(x, map.getWidth());
            y = EngineUtils.wrap(y, map.getHeight());
        } else {
            if (x < 0 || x >= map.getWidth() || y < 0 || y >= map.getHeight()) {
                x = landingParty.getX();
                y = landingParty.getY();
            }
        }

        if (!map.isTilePassable(landingParty, x, y)) {
            world.setUpdatedThisFrame(false);
            x = world.getPlayer().getLandingParty().getX();
            y = world.getPlayer().getLandingParty().getY();
        }

        final boolean enterPressed = container.getInput().isKeyPressed(Input.KEY_ENTER);
        if (enterPressed) {
            interactWithObject(world);
        }

        int tilesExplored = map.updateVisibility(x, y, 1);
        landingParty.addCollectedGeodata(tilesExplored);


        world.getPlayer().getLandingParty().setPos(x, y);
    }


    public void interactWithObject(World world) {
        int x = world.getPlayer().getLandingParty().getX();
        int y = world.getPlayer().getLandingParty().getY();
        // check if can pick up smth
        for (Iterator<PlanetObject> iter = map.getObjects().iterator(); iter.hasNext(); ) {
            PlanetObject p = iter.next();

            if (!p.canBePickedUp()) {
                continue;
            }

            if (p.getX() != x || p.getY() != y) {
                continue;
            }
            p.onPickedUp(world);
            world.setUpdatedThisFrame(true);
            // some items (like ore deposits) can be picked up more than once, do not remove them in this case
            if (!p.isAlive()) {
                iter.remove();
            }
        }

        for (BasePositionable exitPoint : map.getExitPoints()) {
            if (exitPoint.getDistance(landingParty) == 0) {
                returnToPrevRoom();
            }
        }
    }

    private int getDist(int first, int second, int total) {
        int max = Math.max(first, second);
        int min = Math.min(first, second);

        return Math.min(max - min, total + min - max);

    }

    private int getRange(LandingParty party, Positionable target) {
        int xDist = getDist(party.getX(), target.getX(), map.getWidth());
        int yDist = getDist(party.getY(), target.getY(), map.getHeight());
        return xDist + yDist;
    }

    /**
     * This update method is used in FIRE mode. Selecting targets and shooting.
     */
    private void updateShoot(GameContainer container, World world) {
        if (map.getObjects().isEmpty()) {
            return;
        }
        int targetIdx = 0;
        List<PlanetObject> availableTargets = new ArrayList<>();

        if (target != null && getRange(landingParty, target) > landingParty.getWeapon().getRange()) {
            // target moved out of range
            target = null;
        }

        for (PlanetObject planetObject : map.getObjects()) {
            if (!planetObject.canBeShotAt()) {
                continue;
            }
            if (!map.isTileVisible(planetObject.getX(), planetObject.getY())) {
                // do not target animals on unexplored tiles
                continue;
            }
            if (landingParty.getWeapon().getRange() >= getRange(landingParty, planetObject)) {
                availableTargets.add(planetObject);
                if (target == null) {
                    target = planetObject;
                }

                if (target == planetObject) {
                    targetIdx = availableTargets.size() - 1;
                }
            }

        }

        if (availableTargets.isEmpty()) {
            // no target available in weapon range
            return;
        }

        if (container.getInput().isKeyPressed(Input.KEY_UP) || container.getInput().isKeyPressed(Input.KEY_RIGHT)) {
            targetIdx++;
            if (targetIdx >= availableTargets.size()) {
                targetIdx = 0;
            }
        } else if (container.getInput().isKeyPressed(Input.KEY_DOWN) || container.getInput().isKeyPressed(Input.KEY_LEFT)) {
            targetIdx--;
            if (targetIdx < 0) {
                targetIdx = availableTargets.size() - 1;
            }
        }

        target = availableTargets.get(targetIdx);

        if (container.getInput().isKeyPressed(Input.KEY_F) || container.getInput().isKeyPressed(Input.KEY_ENTER)) {
            // firing
            final int damage = landingParty.calcDamage();

            currentEffect = new BlasterShotEffect(landingParty, world.getCamera().getXCoordWrapped(target.getX(), map.getWidth()), world.getCamera().getYCoordWrapped(target.getY(), map.getHeight()), world.getCamera(), 800, landingParty.getWeapon().getShotImage());

            target.onShotAt(damage);
            GameLogger.getInstance().logMessage("Bang! Dealt " + damage + " damage to " + target.getName());
            if (!target.isAlive()) {
                GameLogger.getInstance().logMessage(target.getName() + " killed");
                map.getObjects().remove(target);
                target = null;
            }
            world.setUpdatedThisFrame(true);
        }

    }

    public void changeMode() {
        mode = (mode == MODE_MOVE) ? MODE_SHOOT : MODE_MOVE;
    }

    public void update(GameContainer container, World world) {
        if (currentEffect != null) {
            currentEffect.update(container, world);
            if (currentEffect.isOver()) {
                currentEffect = null;
            }
            return;
        }

        switch (mode) {
            case MODE_MOVE:
                if (container.getInput().isKeyPressed(Input.KEY_F)) {
                    mode = MODE_SHOOT;
                    return;
                }
                updateMove(container, world);
                break;
            case MODE_SHOOT:
                if (container.getInput().isKeyPressed(Input.KEY_ESCAPE)) {
                    mode = MODE_MOVE;
                    return;
                }
                updateShoot(container, world);
                break;
            default:
                throw new IllegalStateException("Unknown planet update type " + mode);

        }
        // should always be done after player update, so that world.isUpdatedThisFrame() flag is set
        for (PlanetObject a : map.getObjects()) {
            a.update(container, world);
        }

        if (world.isUpdatedThisFrame() && !map.getVictoryConditions().isEmpty()) {
            boolean allConditionsSatisfied = true;
            for (IVictoryCondition cond : map.getVictoryConditions()) {
                if (!cond.isSatisfied(world)) {
                    allConditionsSatisfied = false;
                    break;
                }
            }
            if (allConditionsSatisfied) {
                GameLogger.getInstance().logMessage("All objectives completed");
                returnToPrevRoom();
            }
        }
        if (landingParty.getTotalMembers() == 0) {
            onLandingPartyDestroyed(world);
        }
    }

    public void draw(GameContainer container, Graphics graphics, Camera camera) {
        if (landingParty != null) {
            landingParty.draw(container, graphics, camera);

            graphics.setColor(Color.red);
            for (PlanetObject a : map.getObjects()) {
                // draw only if tile under this animal is visible
                if (map.isTileVisible(a.getX(), a.getY())) {
                    a.draw(container, graphics, camera);

                    // in shoot mode, all available targets are surrounded with red square
                    if (mode == MODE_SHOOT && a.canBeShotAt() && landingParty.getDistance(a) < landingParty.getWeapon().getRange()) {
                        graphics.drawRect(camera.getXCoordWrapped(a.getX(), map.getWidth()), camera.getYCoordWrapped(a.getY(), map.getHeight()), camera.getTileWidth(), camera.getTileHeight());
                    }
                }

                if (a.getX() == landingParty.getX() && a.getY() == landingParty.getY()) {
                    a.printStatusInfo();
                }

            }

            if (mode == MODE_SHOOT && target != null) {
                // draw target mark
                graphics.drawImage(ResourceManager.getInstance().getImage("target"), camera.getXCoordWrapped(target.getX(), map.getWidth()), camera.getYCoordWrapped(target.getY(), map.getHeight()));
            }
        }
        if (currentEffect != null) {
            currentEffect.draw(container, graphics, camera);
        }
    }

    public void onLandingPartyDestroyed(World world) {
        GameLogger.getInstance().logMessage("Lost connection with landing party");

        // do not call returnToPrevRoom()
        world.setCurrentRoom(prevRoom);
        prevRoom.enter(world);

        final Nifty nifty = GUI.getInstance().getNifty();
        Element popup = nifty.createPopup("landing_party_lost");
        nifty.setIgnoreKeyboardEvents(false);
        nifty.showPopup(nifty.getCurrentScreen(), popup.getId(), null);
    }

    public void returnToPrevRoom() {
        world.setCurrentRoom(prevRoom);
        prevRoom.enter(world);
        landingParty.onReturnToShip(world);
    }

    public void setCurrentEffect(Effect currentEffect) {
        this.currentEffect = currentEffect;
    }
}