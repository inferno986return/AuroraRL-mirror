package ru.game.aurora.world.planet;

import ru.game.aurora.gui.GUI;
import ru.game.aurora.gui.InventoryController;
import ru.game.aurora.npc.Faction;
import ru.game.aurora.world.World;

/**
 * Date: 21.12.13
 * Time: 10:55
 */
public abstract class UsableItem implements InventoryItem {
    private static final long serialVersionUID = 6868775045575998500L;

    private final String id;

    public UsableItem(String id) {
        this.id = id;
    }

    public void useIt(World world, int amount) {
        world.getPlayer().getLandingParty().getInventory().setCount(this, amount - 1);
        world.getPlayer().getLandingParty().overWeightTest();
        GUI.getInstance().getNifty().findScreenController(InventoryController.class.getCanonicalName()).onStartScreen();
    }

    @Override
    public int getWeight() {
        return 1;
    }

    @Override
    public boolean equals(Object o) {
        return getClass().equals(o.getClass());
    }

    @Override
    public int hashCode() {
        return getClass().getCanonicalName().hashCode();
    }

    @Override
    public boolean canBeSoldTo(World world, Faction faction) {
        return false;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public void onReceived(World world, int amount) {
        world.getPlayer().getInventory().add(this, amount);
        world.onItemAmountChanged(this, amount);
    }

    @Override
    public void onLost(World world, int amount) {
        world.getPlayer().getInventory().remove(this, amount);
        world.onItemAmountChanged(this, amount);
    }

    @Override
    public boolean isUnique() {
        return false;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isVisibleInInventory() {
        return true;
    }

}
