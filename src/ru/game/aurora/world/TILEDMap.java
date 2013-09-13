package ru.game.aurora.world;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.tiled.TiledMap;
import ru.game.aurora.application.AuroraGame;
import ru.game.aurora.application.Camera;
import ru.game.aurora.world.dungeon.DungeonObject;
import ru.game.aurora.world.dungeon.IVictoryCondition;
import ru.game.aurora.world.planet.LandingParty;
import ru.game.aurora.world.planet.PlanetObject;
import ru.game.aurora.world.planet.SurfaceTypes;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Map in a format of a TILed EDitor
 */
public class TILEDMap implements ITileMap
{
    private static final long serialVersionUID = -8605255474835067962L;

    private List<PlanetObject> objects = new LinkedList<>();

    private List<IVictoryCondition> victoryConditions = new ArrayList<>();

    private final String mapRef;

    private transient byte[][] flags;

    private transient TiledMap map;

    private transient BasePositionable entryPoint;

    private transient List<BasePositionable> exitPoints;

    public TILEDMap(String mapRef)
    {
        this.mapRef = mapRef;
    }

    @Override
    public List<PlanetObject> getObjects() {
        return objects;
    }

    private int getXCoord(int x)
    {
        return x / AuroraGame.tileSize;
    }

    private int getYCoord(int y)
    {
        return (y - 1) / AuroraGame.tileSize; // somehow, y in editor starts from 1
    }

    private void loadObject(int groupId, int objectId)
    {
        final String typeName = map.getObjectType(groupId, objectId);
        switch (typeName) {
            case "entryPoint": {
                entryPoint = new BasePositionable(getXCoord(map.getObjectX(groupId, objectId)), getYCoord(map.getObjectY(groupId, objectId)));
                break;
            }
            case "exitPoint": {
                exitPoints.add(new BasePositionable(getXCoord(map.getObjectX(groupId, objectId)), getYCoord(map.getObjectY(groupId, objectId))));
                break;
            }
            default:
                try {
                    Class clazz = Class.forName(typeName);

                    Constructor ctor = clazz.getConstructor(TiledMap.class, int.class, int.class);
                    Object obj = ctor.newInstance(map, groupId, objectId);
                    if (DungeonObject.class.isAssignableFrom(clazz)) {
                        objects.add((PlanetObject) obj);
                    } else if (IVictoryCondition.class.isAssignableFrom(clazz)) {
                        victoryConditions.add((IVictoryCondition) obj);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to load object " + groupId + ", " + objectId, e);
                }
        }
    }


    private void setObstacles(int layerIdx)
    {
        for (int x = 0; x < map.getWidth(); ++x) {
            for (int y = 0; y < map.getHeight(); ++y) {
                if (map.getTileImage(x, y, layerIdx) != null) {
                    flags[y][x] |= SurfaceTypes.OBSTACLE_MASK;
                }
            }
        }
    }

    // must be called from main thread with OpenGL context
    private void loadMap()
    {
        try {
            map = new TiledMap(mapRef, "resources/maps");
            flags = new byte[map.getHeight()][map.getWidth()];
            exitPoints = new ArrayList<>();

            for (int i = 0; i < map.getObjectGroupCount(); ++i) {
                for (int j = 0; j < map.getObjectCount(i); ++j) {
                    loadObject(i, j);
                }
            }

            for (int layerIdx = 0; layerIdx < map.getLayerCount(); ++layerIdx) {
                if (map.getLayerProperty(layerIdx, "isObstacle", "false").equals("true")) {
                    setObstacles(layerIdx);
                }
            }
        } catch (SlickException e) {
            throw new RuntimeException("Failed to load TILED map", e);
        }
    }

    @Override
    public void draw(GameContainer container, Graphics graphics, Camera camera) {
        if (map == null) {
            loadMap();
        }
        for (int i = camera.getTarget().getY() - camera.getNumTilesY() / 2; i <= camera.getTarget().getY() + camera.getNumTilesY() / 2; ++i) {
            for (int j = camera.getTarget().getX() - camera.getNumTilesX() / 2; j <= camera.getTarget().getX() + camera.getNumTilesX() / 2; ++j) {
                if (i < 0 || j < 0 || i >= map.getHeight() || j >= map.getHeight()) {
                    continue;
                }

                for (int layer = 0; layer < map.getLayerCount(); ++layer) {
                    final Image image = map.getTileImage(j, i, layer);
                    if (image == null) {
                        continue;
                    }
                    graphics.drawImage(image, camera.getXCoord(j), camera.getYCoord(i));
                }

            }
        }
    }

    @Override
    public boolean isTilePassable(int x, int y) {
        return SurfaceTypes.isPassible(flags[y][x]);
    }

    @Override
    public boolean isTilePassable(LandingParty landingParty, int x, int y) {
        return isTilePassable(x, y);
    }

    @Override
    public boolean isTileVisible(int x, int y) {
        return true;
    }

    @Override
    public int getWidth() {
        return map.getWidth();
    }

    @Override
    public int getHeight() {
        return map.getHeight();
    }

    @Override
    public int updateVisibility(int x, int y, int range) {
        int rz = 0;
        for (int i = y - range; i <= y + range; ++i) {
            for (int j = x - range; j <= x + range; ++j) {
                if (i < 0 || j < 0 || i >= map.getHeight() || j >= map.getHeight()) {
                    continue;
                }
                if (0 == (SurfaceTypes.VISIBILITY_MASK & flags[i][j])) {
                    flags[i][j] |= SurfaceTypes.VISIBILITY_MASK;
                    ++rz;
                }
            }
        }
        return rz;
    }

    @Override
    public void setTilePassable(int x, int y, boolean isPassable) {
        if (!isPassable) {
            flags[y][x] |= SurfaceTypes.OBSTACLE_MASK;
        } else {
            flags[y][x] &= ~SurfaceTypes.OBSTACLE_MASK;
        }
    }

    @Override
    public BasePositionable getEntryPoint() {
        if (map == null) {
            loadMap();
        }
        return entryPoint;
    }

    @Override
    public Collection<BasePositionable> getExitPoints() {
        return exitPoints;
    }

    @Override
    public List<IVictoryCondition> getVictoryConditions() {
        return victoryConditions;
    }
}
