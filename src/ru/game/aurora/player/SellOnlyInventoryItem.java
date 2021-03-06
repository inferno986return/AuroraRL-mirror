package ru.game.aurora.player;

import org.newdawn.slick.Image;
import ru.game.aurora.common.Drawable;
import ru.game.aurora.common.ItemWithTextAndImage;
import ru.game.aurora.npc.Faction;
import ru.game.aurora.world.World;
import ru.game.aurora.world.planet.InventoryItem;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Special items that can only be sold to aliens
 */
public class SellOnlyInventoryItem extends ItemWithTextAndImage implements InventoryItem
{
    private static final long serialVersionUID = 1L;

    private final double price;

    private final String localizationGroup;

    private final boolean isUnique;

    // if set, can not be sold to these factions
    private Set<String> factionFilter;

    public SellOnlyInventoryItem(String localizationGroup, String id, String image, double price, boolean isUnique, String... factionFilter) {
        this(localizationGroup, id, new Drawable(image), price, isUnique, factionFilter);
    }

    public SellOnlyInventoryItem(String localizationGroup, String id, Drawable drawable, double price, boolean isUnique, String... factionFilter) {
        super(id, drawable);
        this.price = price;
        this.localizationGroup = localizationGroup;
        this.isUnique = isUnique;
        if (factionFilter.length > 0) {
            this.factionFilter = new HashSet<>();
            Collections.addAll(this.factionFilter, factionFilter);
        }
    }

    @Override
    public String getName() {
        return super.getLocalizedName(localizationGroup);
    }

    @Override
    public String getDescription() {
        return getLocalizedText(localizationGroup);
    }

    @Override
    public Image getImage() {
        return super.getDrawable().getImage();
    }

    @Override
    public double getPrice() {
        return price;
    }

    @Override
    public void onReceived(World world, int amount) {
        world.onItemAmountChanged(this, amount);
    }

    @Override
    public void onLost(World world, int amount) {
        world.getPlayer().getInventory().remove(this, amount);
        world.onItemAmountChanged(this, amount);
    }

    @Override
    public boolean isDumpable() {
        return false;
    }

    @Override
    public boolean isUsable() {
        return false;
    }

    @Override
    public boolean isUnique() {
        return isUnique;
    }

    @Override
    public boolean isVisibleInInventory() {
        return true;
    }

    @Override
    public int getWeight() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean canBeSoldTo(World world, Faction faction) {
        return faction == null || factionFilter == null || !factionFilter.contains(faction.getName());
    }
}
