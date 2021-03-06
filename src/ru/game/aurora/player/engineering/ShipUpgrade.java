package ru.game.aurora.player.engineering;

import ru.game.aurora.application.Localization;
import ru.game.aurora.common.Drawable;
import ru.game.aurora.common.ItemWithTextAndImage;
import ru.game.aurora.world.Ship;
import ru.game.aurora.world.World;

/**
 * Created with IntelliJ IDEA.
 * User: Egor.Smirnov
 * Date: 30.04.14
 * Time: 16:45
 */
public abstract class ShipUpgrade extends ItemWithTextAndImage {
    private final String localizationGroup;

    private static final long serialVersionUID = 5654574535197600236L;

    public ShipUpgrade(String id, String icon, String localizationGroup) {
        super(id, new Drawable(icon));
        this.localizationGroup = localizationGroup;
    }

    public ShipUpgrade(String id, Drawable d, String localizationGroup) {
        super(id, d);
        this.localizationGroup = localizationGroup;
    }

    public abstract void onInstalled(World world, Ship ship);

    public abstract void onRemoved(World world, Ship ship);

    public boolean isUsable() {
        return false;
    }

    public boolean canBeUsedNow(World world) {
        return false;
    }

    public void onUse(World world) {
        // nothing in the base class
    }

    public abstract int getSpace();

    public String getLocalizationGroup() {
        return localizationGroup;
    }

    public String getLocalizedDescription() {
        StringBuilder sb = new StringBuilder(super.getLocalizedText(localizationGroup));
        sb.append("\n");
        sb.append(Localization.getText("gui", "shipyard.upgrade_space"));
        sb.append(" ").append(getSpace());
        return sb.toString();
    }
}
