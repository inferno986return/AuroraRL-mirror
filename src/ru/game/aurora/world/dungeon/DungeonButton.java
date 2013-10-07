package ru.game.aurora.world.dungeon;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.tiled.TiledMap;
import ru.game.aurora.application.Camera;
import ru.game.aurora.application.GameLogger;
import ru.game.aurora.application.ResourceManager;
import ru.game.aurora.world.World;
import ru.game.aurora.world.planet.PlanetObject;

/**
 * Date: 06.10.13
 * Time: 13:12
 */
public class DungeonButton extends DungeonObject {
    private static final long serialVersionUID = 6393069794553306363L;

    private String imageNormalName;
    private int imageNormalTileX = -1;
    private int imageNormalTileY = -1;
    private transient Image imageNormal = null;

    private String imagePressName;
    private int imagePressTileX = -1;
    private int imagePressTileY = -1;
    private transient Image imagePress = null;

    //Normal - false, pressed - true
    private boolean state;

    //name of associated door object
    private String door;

    @Override
    public void onPickedUp(World world) {
        if (isPressed()) {
           setState(false);
        } else {
            setState(true);
        }

        GameLogger.getInstance().logMessage("Button pressed"); //todo: localization

        //search for associated door and change state for it
        for (PlanetObject po : world.getCurrentDungeon().getMap().getObjects()) {
            if (DungeonDoor.class.isAssignableFrom(po.getClass())) {
                DungeonDoor dd = (DungeonDoor) po;
                if (dd.getDoorName().equals(door)) {
                    dd.setState(state);
                }
            }
        }
    }

    public DungeonButton(TiledMap map, int groupId, int objectId) {
        super(map, groupId, objectId);
        state = false;

        this.imageNormalName = map.getObjectProperty(groupId, objectId, "imageNormal", null);
        this.imageNormalTileX = Integer.parseInt(map.getObjectProperty(groupId, objectId, "imageNormalTileX", "-1"));
        this.imageNormalTileY = Integer.parseInt(map.getObjectProperty(groupId, objectId, "imageNormalTileY", "-1"));

        this.imagePressName = map.getObjectProperty(groupId, objectId, "imagePress", null);
        this.imagePressTileX = Integer.parseInt(map.getObjectProperty(groupId, objectId, "imagePressTileX", "-1"));
        this.imagePressTileY = Integer.parseInt(map.getObjectProperty(groupId, objectId, "imagePressTileY", "-1"));

        this.door = map.getObjectProperty(groupId, objectId, "door", null);
    }

    @Override
    public boolean canBePickedUp() {
        return true;
    }

    private Image getImageNormal() {
        if (imageNormalTileX < 0) {
            //this is a standalone image
            return ResourceManager.getInstance().getImage(imageNormalName);
        } else {
            return ResourceManager.getInstance().getSpriteSheet(imageNormalName).getSprite(imageNormalTileX, imageNormalTileY);
        }
    }

    private Image getImagePress() {
        if (imagePressTileX < 0) {
            //this is a standalone image
            return ResourceManager.getInstance().getImage(imagePressName);
        } else {
            return ResourceManager.getInstance().getSpriteSheet(imagePressName).getSprite(imagePressTileX, imagePressTileY);
        }
    }

    @Override
    public void draw(GameContainer container, Graphics graphics, Camera camera) {
        if (imageNormal == null) {
            imageNormal = getImageNormal();
        }
        if (imagePress == null) {
            imagePress = getImagePress();
        }
        if (state) {
            graphics.drawImage(imagePress, camera.getXCoord(x), camera.getYCoord(y));
        } else {
            graphics.drawImage(imageNormal, camera.getXCoord(x), camera.getYCoord(y));
        }
    }

    public boolean isPressed() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }
}
