/**
 * User: jedi-philosopher
 * Date: 29.11.12
 * Time: 20:09
 */
package ru.game.aurora.world;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import ru.game.aurora.application.Camera;
import ru.game.aurora.npc.Dialog;
import ru.game.aurora.player.Player;
import ru.game.aurora.player.research.ResearchScreen;
import ru.game.aurora.world.planet.Planet;
import ru.game.aurora.world.space.GalaxyMap;
import ru.game.aurora.world.space.StarSystem;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class World {
    private Camera camera;

    private Room currentRoom;

    private GalaxyMap galaxyMap;

    private Player player;

    private boolean updatedThisFrame;

    private Dialog currentDialog;

    private int turnCount = 0;

    private List<GameEventListener> listeners = new LinkedList<GameEventListener>();

    public World(Camera camera, int sizeX, int sizeY) {
        player = new Player();
        this.camera = camera;
        camera.setTarget(player.getShip());
        currentRoom = galaxyMap = new GalaxyMap(this, camera, sizeX, sizeY, camera.getNumTilesX(), camera.getNumTilesY());
        currentRoom.enter(this);
        updatedThisFrame = false;
    }

    public void update(GameContainer container) {
        updatedThisFrame = false;

        if (currentDialog != null) {
            currentDialog.update(container, this);
            if (currentDialog.isOver()) {
                currentDialog = null;
            }
            return;
        }
        if (container.getInput().isKeyPressed(Input.KEY_R)) {
            // open research screen
            ResearchScreen researchScreen = new ResearchScreen();
            researchScreen.enter(this);
            currentRoom = researchScreen;
            return;
        }
        currentRoom.update(container, this);
        if (isUpdatedThisFrame()) {
            player.getResearchState().update(this);
            turnCount++;
        }

        for (Iterator<GameEventListener> listenerIterator = listeners.iterator(); listenerIterator.hasNext();) {
            GameEventListener l = listenerIterator.next();
            if (!l.isAlive()) {
                listenerIterator.remove();
            }
        }

        if (player.getShip().getTotalCrew() <= 0) {
            if (currentRoom instanceof Planet && player.getLandingParty().getTotalMembers() <= 0) {
                container.exit();
            }
        }
    }

    public void draw(GameContainer container, Graphics graphics) {
        currentRoom.draw(container, graphics, camera);
        if (currentDialog != null) {
            currentDialog.draw(container, graphics, camera);
        }
    }

    public Camera getCamera() {
        return camera;
    }

    public boolean isUpdatedThisFrame() {
        return updatedThisFrame;
    }

    public void setUpdatedThisFrame(boolean updatedThisFrame) {
        this.updatedThisFrame = updatedThisFrame;
    }

    public Room getCurrentRoom() {
        return currentRoom;
    }

    public GalaxyMap getGalaxyMap() {
        return galaxyMap;
    }

    public Player getPlayer() {
        return player;
    }

    public void setCurrentRoom(Room currentRoom) {
        this.currentRoom = currentRoom;
    }

    public void setCurrentDialog(Dialog currentDialog) {
        this.currentDialog = currentDialog;
        currentDialog.enter(this);
    }

    public int getTurnCount() {
        return turnCount;
    }

    public void onPlayerEnteredSystem(StarSystem ss) {
        for (GameEventListener l : listeners) {
            l.onPlayerEnterStarSystem(this, ss);
        }
    }

    public void addListener(GameEventListener listener) {
        listeners.add(listener);
    }
}
