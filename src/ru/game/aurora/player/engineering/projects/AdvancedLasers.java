/**
 * Created with IntelliJ IDEA.
 * User: Egor.Smirnov
 * Date: 23.07.13
 * Time: 16:07
 */
package ru.game.aurora.player.engineering.projects;

import ru.game.aurora.application.GameLogger;
import ru.game.aurora.application.Localization;
import ru.game.aurora.application.ResourceManager;
import ru.game.aurora.player.engineering.EngineeringProject;
import ru.game.aurora.player.engineering.ShipUpgrade;
import ru.game.aurora.player.engineering.upgrades.WeaponUpgrade;
import ru.game.aurora.world.Ship;
import ru.game.aurora.world.World;
import ru.game.aurora.world.equip.WeaponDesc;
import ru.game.aurora.world.equip.WeaponInstance;

import java.util.Collections;
import java.util.Iterator;

public class AdvancedLasers extends EngineeringProject {
    private static final long serialVersionUID = -6929677248004188022L;

    public AdvancedLasers() {
        super("advanced_lasers", "ship_laser2", 10);
    }

    @Override
    public void onCompleted(World world) {
        super.onCompleted(world);
        Ship ship = world.getPlayer().getShip();
        final WeaponDesc upgraded_cannon = ResourceManager.getInstance().getWeapons().getEntity("laser_cannon2");
        GameLogger.getInstance().logMessage(Localization.getText("engineering", "advanced_lasers.end_message"));
        for (int idx = 0; idx < ship.getWeapons().size(); ++idx) {
            WeaponInstance sw = ship.getWeapons().get(idx);
            if (sw.getWeaponDesc().getId().equals("laser_cannon")) {
                ship.getWeapons().set(idx, new WeaponInstance(upgraded_cannon));
            }
        }

        int count = 0;
        for (Iterator<ShipUpgrade> iterator = ship.getUpgrades().iterator(); iterator.hasNext(); ) {
            ShipUpgrade upgrade = iterator.next();
            if (upgrade instanceof WeaponUpgrade && ((WeaponUpgrade) upgrade).getWeaponDesc().getId().equals("laser_cannon")) {
                iterator.remove();
                ++count;
            }
        }

        for (int i = 0; i < count; ++i) {
            ship.getUpgrades().add(new WeaponUpgrade(upgraded_cannon));
        }
        world.getPlayer().getEarthState().getAvailableUpgrades().add(new WeaponUpgrade(upgraded_cannon));
        world.getGlobalVariables().put("earth.advanced_lasers", true);
    }

    @Override
    public java.util.Map<ru.game.aurora.world.planet.InventoryItem, Integer> getCost() {
        return Collections.emptyMap();
    }
}
