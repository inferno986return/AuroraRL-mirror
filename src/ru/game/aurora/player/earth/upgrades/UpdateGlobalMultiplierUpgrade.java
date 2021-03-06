package ru.game.aurora.player.earth.upgrades;

import ru.game.aurora.common.Drawable;
import ru.game.aurora.player.earth.EarthUpgrade;
import ru.game.aurora.world.World;

/**
 * Created by on 27.08.2015.
 * Upgrade that sets global variable multiplier. If such variable already exists it is treated as double and multiplied
 * by this value
 */
public class UpdateGlobalMultiplierUpgrade extends EarthUpgrade {
    private String variableName;

    private double variableValue;

    private boolean add = false;

    public UpdateGlobalMultiplierUpgrade(String id, Drawable drawable, int value) {
        super(id, drawable, value);
    }

    @Override
    public void unlock(World world) {
        super.unlock(world);
        if (!add) {
            double val = Double.parseDouble((String) world.getGlobalVariable(variableName, "1.0"));
            world.getGlobalVariables().put(variableName, String.valueOf(val * variableValue));
        } else {
            double val = ((Number) world.getGlobalVariable(variableName, 0)).doubleValue();
            world.getGlobalVariables().put(variableName, val + variableValue);
        }
    }
}
