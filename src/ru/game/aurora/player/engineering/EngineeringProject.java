/**
 * Created with IntelliJ IDEA.
 * User: Egor.Smirnov
 * Date: 16.07.13
 * Time: 14:38
 */
package ru.game.aurora.player.engineering;

import ru.game.aurora.application.Configuration;
import ru.game.aurora.application.GameLogger;
import ru.game.aurora.application.Localization;
import ru.game.aurora.common.Drawable;
import ru.game.aurora.common.ItemWithTextAndImage;
import ru.game.aurora.player.EarthCountry;
import ru.game.aurora.world.World;

public abstract class EngineeringProject extends ItemWithTextAndImage {
    private static final long serialVersionUID = -7083477407612327469L;

    protected int engineersAssigned;

    protected int remainingProgress;
    private boolean projectStarted = false;

    public EngineeringProject(String id, String icon, int length) {
        super(id, new Drawable(icon));
        remainingProgress = length;
    }

    public boolean update(World world) {
        int realSpeed = getRealSpeed(world);
        remainingProgress -= realSpeed;
        if (remainingProgress <= 0) {
            GameLogger.getInstance().logMessage(String.format(Localization.getText("gui", "logging.engineering_project_completed"), getLocalizedName("engineering")));
            onCompleted(world);
            return false;
        }
        return true;
    }

    public String getRemainingDays(World world) {
        final int realSpeed = getRealSpeed(world);
        if (realSpeed == 0) {
            return "INF";
        }
        return String.valueOf(remainingProgress / realSpeed);
    }

    private int getRealSpeed(World world) {
        int realSpeed = engineersAssigned;
        if (world.getPlayer().getMainCountry() == EarthCountry.EUROPE) {
            realSpeed = (int) Math.ceil(realSpeed * Configuration.getDoubleProperty("player.europe.engineeringSpeedMultiplier"));
        }
        return realSpeed;
    }

    public void changeEngineers(int amount, World world) {
        if (!projectStarted) {
            world.getPlayer().setResourceUnits(world.getPlayer().getResourceUnits() - getCost());
            projectStarted = true;
        }
        engineersAssigned += amount;
    }

    public int getEngineersAssigned() {
        return engineersAssigned;
    }

    public void onCompleted(World world) {
        projectStarted = false;
        world.getPlayer().getEngineeringState().addIdleEngineers(engineersAssigned);
        engineersAssigned = 0;
    }

    @Override
    public String toString() {
        return getLocalizedName("engineering");
    }

    public boolean isRepeatable() {
        return false;
    }

    /**
     * @return project cost (0 if project have no cost)
     */
    public abstract int getCost();

    public boolean isProjectStarted() {
        return projectStarted;
    }
}
