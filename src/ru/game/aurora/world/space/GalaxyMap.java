/**
 * User: jedi-philosopher
 * Date: 29.11.12
 * Time: 20:12
 */
package ru.game.aurora.world.space;

import jgame.JGColor;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import ru.game.aurora.application.Camera;
import ru.game.aurora.application.GameLogger;
import ru.game.aurora.npc.AlienRace;
import ru.game.aurora.npc.Dialog;
import ru.game.aurora.npc.SingleShipEvent;
import ru.game.aurora.npc.StandartAlienShipEvent;
import ru.game.aurora.util.CollectionUtils;
import ru.game.aurora.world.Room;
import ru.game.aurora.world.World;
import ru.game.aurora.world.planet.Planet;
import ru.game.aurora.world.planet.PlanetAtmosphere;
import ru.game.aurora.world.planet.PlanetCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Galaxy map.
 * Map is represented as a list of all static objects (like star systems), and a 2d array of cells, where each cell is either -1
 * (empty sector of space) or index of an element in this array.
 */
public class GalaxyMap extends BaseSpaceRoom {

    private ParallaxBackground background;

    private List<GalaxyMapObject> objects = new ArrayList<GalaxyMapObject>();

    private GalaxyMapScreen fullMapScreen = new GalaxyMapScreen();

    private int[][] map;

    private int tilesX;

    private int tilesY;

    public static final int maxStars = 15;

    private static final Random r = new Random();

    public GalaxyMap(World world, Camera cam, int tilesX, int tilesY, int systemSizeX, int systemSizeY) {
        this.tilesX = tilesX;
        this.tilesY = tilesY;
        background = new ParallaxBackground(tilesX * cam.getTileWidth(), tilesY * cam.getTileHeight(), tilesX / 2, tilesY / 2, 1);
        background.setBaseWidth(4);
        map = new int[tilesY][tilesX];
        for (int i = 0; i < tilesY; ++i) {
            for (int j = 0; j < tilesX; ++j) {
                map[i][j] = -1;
            }
        }

        // adding special objects that exist only in single instance, but are used frequently.
        // 0 is Nebula
        objects.add(new Nebula());


        AlienRace gardenerRace = null;
        AlienRace kliskRace = null;

        gardenerRace = new AlienRace("Gardeners", "gardener_ship", 8, Dialog.loadFromFile(getClass().getClassLoader().getResourceAsStream("dialogs/gardener_default_dialog.json")));
        kliskRace = new AlienRace("Klisk", "klisk_ship", 8, Dialog.loadFromFile(getClass().getClassLoader().getResourceAsStream("dialogs/klisk_default_dialog.json")));
        StarSystem kliskHomeworld = HomeworldGenerator.generateKliskHomeworld(5, 5, kliskRace);
        kliskRace.setHomeworld(kliskHomeworld);

        world.addListener(new SingleShipEvent(0.9, new NPCShip(0, 0, gardenerRace.getShipSprite(), gardenerRace, null, null)));
        world.addListener(new StandartAlienShipEvent(kliskRace));

        objects.add(kliskHomeworld);
        map[5][5] = objects.size() - 1;

        // earth
        StarSystem solarSystem = HomeworldGenerator.createSolarSystem();
        objects.add(solarSystem);
        map[9][9] = objects.size() - 1;


        // now generate random star systems
        for (int i = 0; i < maxStars; ++i) {
            int x;
            int y;
            do {
                x = r.nextInt(tilesX);
                y = r.nextInt(tilesY);
            } while (map[y][x] != -1);
            final int idx = objects.size();
            objects.add(generateRandomStarSystem(x, y, systemSizeX, systemSizeY));
            map[y][x] = idx;
        }
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
    }

    public static StarSystem generateRandomStarSystem(int x, int y, int maxSizeX, int maxSizeY) {
        int size = StarSystem.possibleSizes[r.nextInt(StarSystem.possibleSizes.length)];
        JGColor starColor = StarSystem.possibleColors[r.nextInt(StarSystem.possibleColors.length)];
        final int planetCount = r.nextInt(5);
        Planet[] planets = new Planet[planetCount];
        int maxRadius = 0;
        StarSystem ss = new StarSystem(new StarSystem.Star(size, starColor), x, y);

        int astroData = 20 * size;

        for (int i = 0; i < planetCount; ++i) {
            int radius = r.nextInt(planetCount * StarSystem.PLANET_SCALE_FACTOR) + StarSystem.STAR_SCALE_FACTOR;
            maxRadius = Math.max(radius, maxRadius);
            int planetX = r.nextInt(2 * radius) - radius;

            int planetY = (int) (Math.sqrt(radius * radius - planetX * planetX) * (r.nextBoolean() ? -1 : 1));
            PlanetAtmosphere atmosphere = CollectionUtils.selectRandomElement(PlanetAtmosphere.values());
            final int planetSize = r.nextInt(3) + 1;
            planets[i] = new Planet(
                    ss
                    , CollectionUtils.selectRandomElement(PlanetCategory.values())
                    , atmosphere
                    , planetSize
                    , planetX
                    , planetY
                    , atmosphere != PlanetAtmosphere.NO_ATMOSPHERE);
            astroData += 10 * planetSize;
        }
        ss.setPlanets(planets);
        astroData += r.nextInt(30);
        ss.setAstronomyData(astroData);
        ss.setRadius(Math.max((int) (maxRadius * 1.5), 10));
        return ss;
    }

    public GalaxyMapObject getObjectAt(int x, int y) {
        final int idx = map[y][x];
        if (idx != -1) {
            return objects.get(idx);
        }
        return null;
    }


    @Override
    public void update(GameContainer container, World world) {
        if (container.getInput().isKeyDown(Input.KEY_M)) {
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
            if (objects.get(idx).canBeEntered() && container.getInput().isKeyDown(Input.KEY_ENTER)) {
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
        background.draw(graphics, camera);
        for (int i = 0; i < tilesY; ++i) {
            for (int j = 0; j < tilesX; ++j) {
                if (map[i][j] != -1) {
                    GalaxyMapObject obj = objects.get(map[i][j]);
                    if (j == player.getShip().getX() && i == player.getShip().getY() && obj.canBeEntered()) {
                        GameLogger.getInstance().addStatusMessage("Press <enter> to enter location");
                    }
                    obj.drawOnGlobalMap(container, graphics, camera, j, i);
                }
            }
        }
        player.addGlobalStatus();
    }

    public static double getDistance(StarSystem first, StarSystem second) {
        return Math.sqrt(Math.pow(first.getGlobalMapX() - second.getGlobalMapX(), 2) + Math.pow(first.getGlobalMapY() - second.getGlobalMapY(), 2));
    }
}
