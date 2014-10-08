package ru.game.aurora.world.space;

import ru.game.aurora.application.GameLogger;
import ru.game.aurora.application.Localization;
import ru.game.aurora.common.Drawable;
import ru.game.aurora.util.ProbabilitySet;
import ru.game.aurora.world.BaseGameObject;
import ru.game.aurora.world.GameObject;
import ru.game.aurora.world.World;

/**
 * Space debris are left after a ship is destroyed
 */
public class SpaceDebris extends BaseGameObject {
    public static class ResourceDebris extends BaseGameObject {

        private static final long serialVersionUID = 1L;

        private final int amount;

        public ResourceDebris(int amount) {
            super(0, 0);
            this.amount = amount;
        }

        @Override
        public void interact(World world) {
            world.getPlayer().setResourceUnits(world.getPlayer().getResourceUnits() + amount);
            GameLogger.getInstance().logMessage(String.format(Localization.getText("gui", "space.debris.message"), amount));
        }
    }

    private static final long serialVersionUID = 7779753986433190967L;

    private final GameObject dropItem;

    public SpaceDebris(int x, int y, ProbabilitySet<GameObject> dropList) {
        this(x, y, "debris", dropList);
    }

    public SpaceDebris(int x, int y, String imageId, ProbabilitySet<GameObject> dropList) {
        super(x, y, new Drawable(imageId));
        this.dropItem = dropList.getRandom();
    }

    public SpaceDebris(int x, int y, String imageId, GameObject dropItem) {
        super(x, y, imageId);
        this.dropItem = dropItem;
    }

    @Override
    public void interact(World world) {
        isAlive = false;
        dropItem.interact(world);
    }

    @Override
    public String getName() {
        return Localization.getText("gui", "space.debris.name");
    }

    @Override
    public String getScanDescription(World world) {
        return Localization.getText("gui", "space.debris.desc");
    }
}
