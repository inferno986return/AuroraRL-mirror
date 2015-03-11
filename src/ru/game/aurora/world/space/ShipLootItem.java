package ru.game.aurora.world.space;

import ru.game.aurora.application.Configuration;
import ru.game.aurora.common.Drawable;
import ru.game.aurora.npc.AlienRace;
import ru.game.aurora.player.SellOnlyInventoryItem;
import ru.game.aurora.player.earth.PrivateMessage;
import ru.game.aurora.player.research.ResearchProjectDesc;
import ru.game.aurora.world.World;
import ru.game.aurora.world.generation.aliens.KliskGenerator;
import ru.game.aurora.world.generation.aliens.RoguesGenerator;
import ru.game.aurora.world.generation.aliens.bork.BorkGenerator;
import ru.game.aurora.world.generation.aliens.zorsan.ZorsanGenerator;

/**
 * Items that are dropped from destroyed alien ship
 */
public class ShipLootItem extends SellOnlyInventoryItem
{

    public static enum Type
    {
        COMPUTERS ("loot.computers"),
        WEAPONS ("loot.weaponry"),
        ENERGY ("loot.energy"),
        MATERIALS ("loot.materials"),
        GOODS ("loot.goods");

        public final String id;

        Type(String id) {
            this.id = id;
        }
    }

    private static int getRaceIdx(String raceName)
    {
        switch (raceName) {
            case KliskGenerator.NAME:
                return 0;
            case RoguesGenerator.NAME:
                return 1;
            case ZorsanGenerator.NAME:
                return 2;
            case BorkGenerator.NAME:
                return 3;
            default:
                throw new IllegalArgumentException("Unsupported race " + raceName);
        }
    }

    public ShipLootItem(Type type, String race) {
        super("research", type.id, new Drawable("alien_space_drop_items", getRaceIdx(race), type.ordinal()), Configuration.getIntProperty("trade.loot_price"), false);
    }

    public ShipLootItem(Type type, AlienRace race) {
        this(type, race.getName());
    }

    @Override
    public void onReceived(World world, int amount) {
        world.getPlayer().getInventory().add(this, amount);
        ResearchProjectDesc r = world.getResearchAndDevelopmentProjects().getResearchProjects().remove(id);
        if (r != null) {
            world.getPlayer().getResearchState().addNewAvailableProject(r);
        }

        if (id.contains("goods")) {
            world.getPlayer().getEarthState().getMessages().add(new PrivateMessage("loot.goods", "news"));
        }
    }

    @Override
    public void onLost(World world, int amount) {
        world.getPlayer().getInventory().remove(this, amount);
    }
}