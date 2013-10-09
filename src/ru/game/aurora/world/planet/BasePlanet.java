/**
 * Created with IntelliJ IDEA.
 * User: Egor.Smirnov
 * Date: 24.12.12
 * Time: 14:50
 */
package ru.game.aurora.world.planet;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import ru.game.aurora.application.Camera;
import ru.game.aurora.application.ResourceManager;
import ru.game.aurora.player.Player;
import ru.game.aurora.world.Room;
import ru.game.aurora.world.space.GalaxyMapObject;
import ru.game.aurora.world.space.StarSystem;


public abstract class BasePlanet implements Room, GalaxyMapObject
{
    private static final long serialVersionUID = 1L;
    protected StarSystem owner;
    protected PlanetCategory category;
    protected PlanetAtmosphere atmosphere;
    /**
     * If planet has rings, this value is non-zero index of rings sprite
     */
    protected int rings;
    /**
     * Planet size type. 1 is largest, 4 is smallest.
     * Planet image size on global map and dimensions of planet surface depends on it.
     */
    protected int size;
    /**
     * Position of planet in star system
     */
    protected int globalX;
    protected int globalY;

    private transient Image sprite;

    public BasePlanet(int size, int y, StarSystem owner, PlanetAtmosphere atmosphere, int x, PlanetCategory cat) {
        this.size = size;
        this.globalY = y;
        this.owner = owner;
        this.atmosphere = atmosphere;
        this.globalX = x;
        this.category = cat;
    }

    public void setRings(int ringsId) {
        this.rings = ringsId;
    }

    public int getGlobalX() {
        return globalX;
    }

    public int getGlobalY() {
        return globalY;
    }

    @Override
    public boolean canBeEntered() {
        return true;
    }

    @Override
    public void processCollision(GameContainer container, Player player) {
    }

    public void setGlobalY(int globalY) {
        this.globalY = globalY;
    }

    public void setGlobalX(int globalX) {
        this.globalX = globalX;
    }

    /**
     * True if this is ordinary planet that can be explored with landing party.
     * False if this is some kind of quest location that can not be actually landed on (like alien homeworld)
     */
    public boolean canBeLanded() {
        return true;
    }

    public PlanetCategory getCategory() {
        return category;
    }

    public PlanetAtmosphere getAtmosphere() {
        return atmosphere;
    }

    @Override
    public String getName() {
        return null;
    }

    public StarSystem getOwner() {
        return owner;
    }

    public int getSize() {
        return size;
    }

    public boolean hasLife()
    {
        return false;
    }


    @Override
    public void drawOnGlobalMap(GameContainer container, Graphics graphics, Camera camera, int tileX, int tileY) {
        if (!camera.isInViewport(globalX, globalY)) {
            return;
        }
        if (sprite == null) {
            sprite = PlanetSpriteGenerator.getInstance().createPlanetSprite(camera, category, size, atmosphere != PlanetAtmosphere.NO_ATMOSPHERE);
        }
        // draw planetary rings in 2 steps - first part that is behind planet, then planet itself, then part before planet
        // draw rings shifted on 1/10 of planet diameter - looks nicer
        final int RINGS_SHIFT_FACTOR = 10;
        if (rings != 0) {
            Image backRingsSprite = ResourceManager.getInstance().getImage("ring_back_" + rings);
            graphics.drawImage(backRingsSprite, sprite.getWidth() / RINGS_SHIFT_FACTOR + camera.getXCoord(globalX) + (camera.getTileWidth() - backRingsSprite.getWidth()) / 2, sprite.getWidth() / RINGS_SHIFT_FACTOR + camera.getYCoord(globalY) + (camera.getTileHeight() - backRingsSprite.getHeight()) / 2);
        }
        graphics.drawImage(sprite, camera.getXCoord(globalX) + (camera.getTileWidth() - sprite.getWidth()) / 2, camera.getYCoord(globalY) + (camera.getTileHeight() - sprite.getHeight()) / 2);

        if (rings != 0) {
            Image frontRingsSprite = ResourceManager.getInstance().getImage("ring_front_" + rings);
            graphics.drawImage(frontRingsSprite, sprite.getWidth() / RINGS_SHIFT_FACTOR + camera.getXCoord(globalX) + (camera.getTileWidth() - frontRingsSprite.getWidth()) / 2, sprite.getWidth() / RINGS_SHIFT_FACTOR + camera.getYCoord(globalY) + (camera.getTileHeight() - frontRingsSprite.getHeight()) / 2);
        }
    }

}
