/**
 * User: jedi-philosopher
 * Date: 02.12.12
 * Time: 18:46
 */
package ru.game.aurora.world.planet;

import de.matthiasmann.twl.Widget;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.geom.Point;
import ru.game.aurora.application.Camera;
import ru.game.aurora.application.GameLogger;
import ru.game.aurora.application.ResourceManager;
import ru.game.aurora.util.CollectionUtils;
import ru.game.aurora.util.EngineUtils;
import ru.game.aurora.util.ProbabilitySet;
import ru.game.aurora.world.Positionable;
import ru.game.aurora.world.World;
import ru.game.aurora.world.planet.nature.Animal;
import ru.game.aurora.world.planet.nature.AnimalSpeciesDesc;
import ru.game.aurora.world.space.StarSystem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Class for planet.
 * Draws itself on Star System map.
 * Contains planetary data - atmosphere, size, flora and fauna (if any)
 * Contains Room - planet surface, with logic for landing party movement.
 */
public class Planet extends BasePlanet {

    /**
     * Mode for moving. Arrows control landing party movement.
     */
    private static final int MODE_MOVE = 0;

    /**
     * Mode for shooting. Arrows control target selection.
     */
    private static final int MODE_SHOOT = 1;

    /**
     * Current mode
     */
    private int mode = MODE_MOVE;

    /**
     * Tiles with planet surface.
     * Actual contents are encoded by bits
     * <p/>
     * vpm0tttt
     * <p/>
     * v - visibility bit, 1 means tile is not explored, 0 is for explored
     * p - bit shows if tile can be passed on foot (1)
     * m - mountains
     * 0 - reserved
     * tttt - tile type
     */
    private byte[][] surface;

    private int width;

    private int height;

    private static final Random r = new Random();

    private LandingParty landingParty;

    /**
     * Where landing shuttle is located. Launching to orbit and refilling oxygen is available at shuttle
     */
    private Point shuttlePosition;

    /**
     * Available animal species descriptions, if any.
     */
    private AnimalSpeciesDesc[] animalSpecies;

    /**
     * Animals that are located on planet surface.
     */
    private List<PlanetObject> planetObjects = new ArrayList<PlanetObject>();

    /**
     * When in fire mode, this is currently selected target
     */
    private PlanetObject target = null;

    public Planet(StarSystem owner, PlanetCategory cat, PlanetAtmosphere atmosphere, int size, int x, int y, boolean hasLife) {
        super(size, y, owner, atmosphere, x, cat);
        switch (size) {
            case 1:
                this.width = 500;
                this.height = 500;
                break;
            case 2:
                this.width = 300;
                this.height = 300;
                break;
            case 3:
                this.width = 200;
                this.height = 200;
                break;
            case 4:
                this.width = 100;
                this.height = 100;
                break;
            default:
                throw new IllegalArgumentException("Unsupported planet size value");
        }
        // different planets will have different probabilities for tiles
        int mountainProbability = r.nextInt(5) + 2;
        ProbabilitySet<Byte> ps = new ProbabilitySet<Byte>();
        for (byte b : category.availableSurfaces) {
            ps.put(b, r.nextDouble() * (r.nextInt(5) + 1));
        }

        surface = LandscapeGenerator.generateLandscape(cat, width, height);

        final int resourceDeposits = r.nextInt(40 / size);
        for (int i = 0; i < resourceDeposits; ++i) {
            OreDeposit d = new OreDeposit(this, r.nextInt(10), r.nextInt(10), CollectionUtils.selectRandomElement(OreDeposit.OreType.values()), r.nextInt(3) + 1);
            int oreX;
            int oreY;
            do {
                oreX = r.nextInt(10);
                oreY = r.nextInt(10);
            } while (!SurfaceTypes.isPassible(surface[oreY][oreX]));
            d.setPos(oreX, oreY);
            planetObjects.add(d);
        }

        if (hasLife) {
            // generate random species descs. Currently only one
            animalSpecies = new AnimalSpeciesDesc[2];
            animalSpecies[0] = new AnimalSpeciesDesc(this, "Alien mammoth", "mammal_large_1", "mammal_large_1_dead", false, true, 3, 0, 5, AnimalSpeciesDesc.Behaviour.PASSIVE);
            animalSpecies[1] = new AnimalSpeciesDesc(this, "Alien boar", "boar_1", "boar_1_dead", true, true, 5, 1, 2, AnimalSpeciesDesc.Behaviour.AGGRESSIVE);

            final int animalCount = r.nextInt(10) + 5;
            for (int i = 0; i < animalCount; ++i) {
                Animal a = new Animal(this, 0, 0, animalSpecies[r.nextInt(animalSpecies.length)]);
                int animalX;
                int animalY;
                do {
                    animalX = r.nextInt(10);
                    animalY = r.nextInt(10);
                } while (!SurfaceTypes.isPassible(a, surface[animalY][animalX]));
                a.setPos(animalX, animalY);
                planetObjects.add(a);
            }
        }
    }

    public void setNearestFreePoint(Positionable p, int x, int y) {
        while (!SurfaceTypes.isPassible(surface[y][wrapX(x)])) {
            x++;
        }

        p.setPos(x, y);
    }


    public List<PlanetObject> getPlanetObjects() {
        return planetObjects;
    }

    @Override
    public void enter(World world) {
        landingParty = world.getPlayer().getLandingParty();
        landingParty.onLaunch(world);
        landingParty.refillOxygen();
        int x = landingParty.getX();
        int y = landingParty.getY();

        while (!SurfaceTypes.isPassible(landingParty, surface[y][x])) {
            x = wrapX(x + 1);
            y = wrapY(y + r.nextInt(2) - 1);
        }
        landingParty.setPos(x, y);

        world.getCamera().setTarget(landingParty);
        shuttlePosition = new Point(landingParty.getX(), landingParty.getY());
        int openedTiles = updateVisibility(landingParty.getX(), landingParty.getY(), 5);
        landingParty.addCollectedGeodata(openedTiles);
    }

    @Override
    public Widget getGUI() {
        return null;
    }

    public int wrapX(int x) {
        if (x < 0) {
            return width + x;
        } else if (x >= width) {
            return x - width;
        }
        return x;
    }

    public int wrapY(int y) {
        if (y < 0) {
            return height + y;
        } else if (y >= height) {
            return y - height;
        }
        return y;
    }

    /**
     * Updates planet map. Makes tiles visible in given range from given point
     *
     * @return Amount of tiles opened
     */
    private int updateVisibility(int x, int y, int range) {
        int rz = 0;
        for (int i = y - range; i <= y + range; ++i) {
            for (int j = x - range; j <= x + range; ++j) {
                int pointX = wrapX(j);
                int pointY = wrapY(i);
                if (0 == (SurfaceTypes.VISIBILITY_MASK & surface[pointY][pointX])) {
                    surface[pointY][pointX] |= SurfaceTypes.VISIBILITY_MASK;
                    ++rz;
                }
            }
        }
        return rz;
    }

    /**
     * This update is used in MOVE mode. Moving landing party around.
     */
    private void updateMove(GameContainer container, World world) {
        int x = world.getPlayer().getLandingParty().getX();
        int y = world.getPlayer().getLandingParty().getY();

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

        x = wrapX(x);
        y = wrapY(y);

        if (!SurfaceTypes.isPassible(landingParty, surface[y][x])) {
            world.setUpdatedThisFrame(false);
            x = world.getPlayer().getLandingParty().getX();
            y = world.getPlayer().getLandingParty().getY();
        }

        final boolean enterPressed = container.getInput().isKeyPressed(Input.KEY_ENTER);
        if (enterPressed) {
            // check if can pick up smth
            for (Iterator<PlanetObject> iter = planetObjects.iterator(); iter.hasNext();) {
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
        }


        int tilesExplored = updateVisibility(x, y, 1);
        landingParty.addCollectedGeodata(tilesExplored);

        if (x == (int) shuttlePosition.getX() && y == (int) shuttlePosition.getY()) {

            if (world.isUpdatedThisFrame()) {
                GameLogger.getInstance().logMessage("Refilling oxygen");
                world.getPlayer().getLandingParty().refillOxygen();
            }
            if (enterPressed) {
                GameLogger.getInstance().logMessage("Launching shuttle to orbit...");
                world.setCurrentRoom(owner);
                owner.enter(world);
                world.getPlayer().getShip().setPos(globalX, globalY);
                landingParty.onReturnToShip(world);
            }
        }
        world.getPlayer().getLandingParty().setPos(x, y);
    }

    private int getDist(int first, int second, int total) {
        int max = Math.max(first, second);
        int min = Math.min(first, second);

        return Math.min(max - min, total + min - max);

    }

    private int getRange(LandingParty party, Positionable target) {
        int xDist = getDist(party.getX(), target.getX(), width);
        int yDist = getDist(party.getY(), target.getY(), height);
        return xDist + yDist;
    }

    /**
     * This update method is used in FIRE mode. Selecting targets and shooting.
     */
    private void updateShoot(GameContainer container, World world) {
        if (planetObjects.isEmpty()) {
            return;
        }
        int targetIdx = 0;
        List<PlanetObject> availableTargets = new ArrayList<PlanetObject>();

        if (target != null && getRange(landingParty, target) > landingParty.getWeapon().getRange()) {
            // target moved out of range
            target = null;
        }

        for (PlanetObject planetObject : planetObjects) {
            if (!planetObject.canBeShotAt()) {
                continue;
            }
            if ((surface[planetObject.getY()][planetObject.getX()] & SurfaceTypes.VISIBILITY_MASK) == 0) {
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
            target.onShotAt(damage);
            GameLogger.getInstance().logMessage("Bang! Dealt " + damage + " damage to " + target.getName());
            if (!target.isAlive()) {
                GameLogger.getInstance().logMessage(target.getName() + " killed");
                planetObjects.remove(target);
                target = null;
            }
            world.setUpdatedThisFrame(true);
        }

    }

    @Override
    public void update(GameContainer container, World world) {
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

        if (atmosphere != PlanetAtmosphere.BREATHABLE_ATMOSPHERE && world.isUpdatedThisFrame()) {
            world.getPlayer().getLandingParty().consumeOxygen();
        }

        if (world.getPlayer().getLandingParty().getOxygen() < 0 || world.getPlayer().getLandingParty().getTotalMembers() == 0) {
            GameLogger.getInstance().logMessage("Lost connection with landing party");
            world.setCurrentRoom(owner);
            owner.enter(world);
            world.getPlayer().getShip().setPos(globalX, globalY);
        }

        for (PlanetObject a : planetObjects) {
            a.update(container, world);
        }
    }

    public void printPlanetStatus() {
        GameLogger.getInstance().addStatusMessage("Planet info:");
        GameLogger.getInstance().addStatusMessage(String.format("Size: [%d, %d]", width, height));
        GameLogger.getInstance().addStatusMessage("Atmosphere: " + atmosphere);
        GameLogger.getInstance().addStatusMessage("=====================================");
    }

    public void drawLandscape(GameContainer container, Graphics graphics, Camera camera, boolean detailed) {
        for (int i = camera.getTarget().getY() - camera.getNumTilesY() / 2; i <= camera.getTarget().getY() + camera.getNumTilesY() / 2; ++i) {
            for (int j = camera.getTarget().getX() - camera.getNumTilesX() / 2; j <= camera.getTarget().getX() + camera.getNumTilesX() / 2; ++j) {

                final byte type = surface[wrapY(i)][wrapX(j)];
                if ((type & SurfaceTypes.VISIBILITY_MASK) == 0) {
                    continue;
                }
                if (detailed) {
                    SurfaceTypes.drawDetailed(
                            type
                            , camera.getXCoord(j)
                            , camera.getYCoord(i)
                            , camera.getTileWidth()
                            , camera.getTileHeight()
                            , graphics);
                } else {
                    SurfaceTypes.drawSimple(
                            type
                            , camera.getXCoord(j)
                            , camera.getYCoord(i)
                            , camera.getTileWidth()
                            , camera.getTileHeight()
                            , graphics);
                }
            }

        }

        // after all draw mountains
        if (detailed) {
            for (int i = camera.getTarget().getY() - camera.getNumTilesY() / 2; i <= camera.getTarget().getY() + camera.getNumTilesY() / 2; ++i) {
                // first draw outer mountains (that have only one neighbour on X)
                for (int j = camera.getTarget().getX() - camera.getNumTilesX() / 2; j <= camera.getTarget().getX() + camera.getNumTilesX() / 2; j++) {

                    final byte type = surface[wrapY(i)][wrapX(j)];
                    if ((type & SurfaceTypes.VISIBILITY_MASK) == 0) {
                        continue;
                    }
                    if ((type & SurfaceTypes.MOUNTAINS_MASK) != 0) {

                        byte leftNeighbour = surface[wrapY(i)][wrapX(j - 1)];
                        byte rightNeighbour = surface[wrapY(i)][wrapX(j + 1)];

                        if (((leftNeighbour & SurfaceTypes.MOUNTAINS_MASK) == 0) || ((rightNeighbour & SurfaceTypes.MOUNTAINS_MASK) == 0)) {
                            graphics.drawImage(ResourceManager.getInstance().getImage("mountains_tile_1"), camera.getXCoord(j), camera.getYCoord(i));

                        }
                    }
                }

                for (int j = camera.getTarget().getX() - camera.getNumTilesX() / 2; j <= camera.getTarget().getX() + camera.getNumTilesX() / 2; j++) {

                    final byte type = surface[wrapY(i)][wrapX(j)];
                    if ((type & SurfaceTypes.VISIBILITY_MASK) == 0) {
                        continue;
                    }
                    if ((type & SurfaceTypes.MOUNTAINS_MASK) != 0) {

                        byte leftNeighbour = surface[wrapY(i)][wrapX(j - 1)];
                        byte rightNeighbour = surface[wrapY(i)][wrapX(j + 1)];

                        if (((leftNeighbour & SurfaceTypes.MOUNTAINS_MASK) != 0) && ((rightNeighbour & SurfaceTypes.MOUNTAINS_MASK) != 0)) {
                            graphics.drawImage(ResourceManager.getInstance().getImage("mountains_tile_2"), camera.getXCoord(j), camera.getYCoord(i));
                        }
                    }
                }


            }
        }
    }


    public void drawObjects(GameContainer container, Graphics graphics, Camera camera) {
        // this part (monsters, shuttle, landing party) is drawn only when landing party is on surface
        if (landingParty != null) {
            landingParty.draw(container, graphics, camera);


            graphics.drawImage(ResourceManager.getInstance().getImage("shuttle"), camera.getXCoordWrapped((int) shuttlePosition.getX(), width), camera.getYCoordWrapped((int) shuttlePosition.getY(), height));

            if (landingParty.getX() == (int) shuttlePosition.getX() && landingParty.getY() == (int) shuttlePosition.getY()) {
                GameLogger.getInstance().addStatusMessage("Press <enter> to return to orbit");
            }

            graphics.setColor(Color.red);
            for (PlanetObject a : planetObjects) {
                // draw only if tile under this animal is visible
                if ((surface[a.getY()][a.getX()] & SurfaceTypes.VISIBILITY_MASK) != 0) {
                    a.draw(container, graphics, camera);

                    // in shoot mode, all available targets are surrounded with red square
                    if (mode == MODE_SHOOT && a.canBeShotAt() && getRange(landingParty, a) < landingParty.getWeapon().getRange()) {
                        graphics.drawRect(camera.getXCoordWrapped(a.getX(), width), camera.getYCoordWrapped(a.getY(), height), camera.getTileWidth(), camera.getTileHeight());
                    }
                }

                if (a.getX() == landingParty.getX() && a.getY() == landingParty.getY()) {
                    a.printStatusInfo();
                }

            }

            if (mode == MODE_SHOOT && target != null) {
                // draw target mark
                graphics.drawImage(ResourceManager.getInstance().getImage("target"), camera.getXCoordWrapped(target.getX(), width), camera.getYCoordWrapped(target.getY(), height));
            }
            GameLogger.getInstance().addStatusMessage(mode == MODE_MOVE ? "MOVE" : "SHOOT");
        }
    }

    public byte getTileTypeAt(int x, int y) {
        return surface[y][x];
    }

    public void setTileTypeAt(int x, int y, byte val) {
        surface[y][x] = val;
    }

    public void xorMaskAt(int x, int y, byte mask) {
        surface[y][x] ^= mask;
    }

    @Override
    public void draw(GameContainer container, Graphics graphics, Camera camera) {
        printPlanetStatus();
        drawLandscape(container, graphics, camera, true);
        drawObjects(container, graphics, camera);
    }

    @Override
    public void drawOnGlobalMap(GameContainer container, Graphics graphics, Camera camera, int tileX, int tileY) {
        if (!camera.isInViewport(globalX, globalY)) {
            return;
        }
        Color color;
        switch (category) {
            case PLANET_ROCK:
                color = Color.gray;
                break;
            case PLANET_ICE:
                color = Color.white;
                break;
            default:
                color = Color.gray;
        }
        EngineUtils.drawCircleCentered(graphics, camera.getXCoord(globalX) + (camera.getTileWidth() / 2)
                , camera.getYCoord(globalY) + camera.getTileHeight() / 2, StarSystem.PLANET_SCALE_FACTOR * camera.getTileWidth() / (2 * size), color, true);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

}
