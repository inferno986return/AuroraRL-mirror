/**
 * User: jedi-philosopher
 * Date: 29.11.12
 * Time: 20:12
 */
package ru.game.aurora.world.space;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.screen.Screen;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import ru.game.aurora.application.Camera;
import ru.game.aurora.gui.GUI;
import ru.game.aurora.world.Room;
import ru.game.aurora.world.World;

import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Galaxy map.
 * Map is represented as a list of all static objects (like star systems), and a 2d array of cells, where each cell is either -1
 * (empty sector of space) or index of an element in this array.
 */
public class GalaxyMap extends BaseSpaceRoom {

    private static final long serialVersionUID = -2138368991952958011L;

    private transient ParallaxBackground background;

    private List<GalaxyMapObject> objects = new ArrayList<GalaxyMapObject>();

    private transient GalaxyMapScreen fullMapScreen = new GalaxyMapScreen();

    private int[][] map;

    private int tilesX;

    private int tilesY;

    private World world;

    public GalaxyMap() {
    }

    public GalaxyMap(World world, int tilesX, int tilesY) {
        this.world = world;
        //this.myGui = new GalaxyMapController(world);
        this.tilesX = tilesX;
        this.tilesY = tilesY;
        map = new int[tilesY][tilesX];
        for (int i = 0; i < tilesY; ++i) {
            for (int j = 0; j < tilesX; ++j) {
                map[i][j] = -1;
            }
        }
    }

    private void createBackground(Camera cam, int tilesX, int tilesY) {
        background = new ParallaxBackground(tilesX * cam.getTileWidth(), tilesY * cam.getTileHeight(), cam.getTileWidth() * tilesX / 2, cam.getTileHeight() * tilesY / 2, 8);
        background.setBaseWidth(2); // smaller size of background stars so that they are not messed with real stars
    }

    @Override
    public Screen getGUI() {
        //myGui = new GalaxyMapController(world);
        // return myGui;
        return null;
    }

    public int[][] getMap() {
        return map;
    }

    public int getTilesX() {
        return tilesX;
    }

    public int getTilesY() {
        return tilesY;
    }

    @Override
    public void enter(World world) {
        super.enter(world);
        world.getCamera().setTarget(player.getShip());
        world.setCurrentStarSystem(null);
        final Nifty nifty = GUI.getInstance().getNifty();
        nifty.gotoScreen("galaxy_map_gui");
        nifty.setIgnoreKeyboardEvents(true);
    }


    public GalaxyMapObject getObjectAt(int x, int y) {
        final int idx = map[y][x];
        if (idx != -1) {
            return objects.get(idx);
        }
        return null;
    }

    private void readObject(ObjectInputStream is) {
        try {
            is.defaultReadObject();
            fullMapScreen = new GalaxyMapScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void update(GameContainer container, World world) {
        if (container.getInput().isKeyPressed(Input.KEY_M)) {
            world.setCurrentRoom(fullMapScreen);
            fullMapScreen.enter(world);
            return;
        }
        super.update(container, world);
        final int y = player.getShip().getY();
        final int x = player.getShip().getX();

        int idx;
        if (y >= 0 && x >= 0 && y < tilesY && x < tilesX) {
            idx = map[y][x];
        } else {
            idx = -1;
        }
        if (idx != -1) {
            if (world.isUpdatedThisFrame()) {
                objects.get(idx).processCollision(container, player);
            }
            if (objects.get(idx).canBeEntered() && container.getInput().isKeyPressed(Input.KEY_ENTER)) {
                Room r = (Room) objects.get(idx);
                world.setCurrentRoom(r);
                r.enter(world);
                world.setUpdatedThisFrame(true);
            }
        }
    }

    @Override
    public void draw(GameContainer container, Graphics graphics, Camera camera) {
        super.draw(container, graphics, camera);
        if (background == null) {
            createBackground(world.getCamera(), tilesX, tilesY);
        }
        background.draw(graphics, camera);
        for (int i = 0; i < tilesY; ++i) {
            for (int j = 0; j < tilesX; ++j) {
                if (map[i][j] != -1) {
                    GalaxyMapObject obj = objects.get(map[i][j]);
                    obj.drawOnGlobalMap(container, graphics, camera, j, i);
                }
            }
        }
    }

    public static double getDistance(StarSystem first, StarSystem second) {
        return Math.sqrt(Math.pow(first.getGlobalMapX() - second.getGlobalMapX(), 2) + Math.pow(first.getGlobalMapY() - second.getGlobalMapY(), 2));
    }

    public List<GalaxyMapObject> getObjects() {
        return objects;
    }

    public void setTileAt(int x, int y, int val) {
        map[y][x] = val;
    }

    public void addObjectAndSetTile(GalaxyMapObject object, int x, int y) {
        objects.add(object);
        map[y][x] = objects.size() - 1;
    }
}
