/**
 * Created with IntelliJ IDEA.
 * User: Egor.Smirnov
 * Date: 06.02.13
 * Time: 15:12
 */
package ru.game.aurora.world.generation.aliens;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.newdawn.slick.Color;
import ru.game.aurora.application.CommonRandom;
import ru.game.aurora.application.Configuration;
import ru.game.aurora.application.ResourceManager;
import ru.game.aurora.dialog.Dialog;
import ru.game.aurora.dialog.DialogListener;
import ru.game.aurora.dialog.NextDialogListener;
import ru.game.aurora.gui.TradeScreenController;
import ru.game.aurora.npc.*;
import ru.game.aurora.npc.shipai.LeaveSystemAI;
import ru.game.aurora.player.earth.PrivateMessage;
import ru.game.aurora.player.research.ResearchProjectDesc;
import ru.game.aurora.player.research.ResearchReport;
import ru.game.aurora.player.research.projects.AlienRaceResearch;
import ru.game.aurora.player.research.projects.ArtifactResearch;
import ru.game.aurora.steam.AchievementManager;
import ru.game.aurora.steam.AchievementNames;
import ru.game.aurora.world.GameObject;
import ru.game.aurora.world.Positionable;
import ru.game.aurora.world.World;
import ru.game.aurora.world.generation.WorldGenerator;
import ru.game.aurora.world.generation.WorldGeneratorPart;
import ru.game.aurora.world.generation.aliens.bork.BorkGenerator;
import ru.game.aurora.world.generation.humanity.HumanityGenerator;
import ru.game.aurora.world.generation.quest.EmbassiesQuest;
import ru.game.aurora.world.generation.quest.heritage.HeritageKliskDialogListener;
import ru.game.aurora.world.planet.*;
import ru.game.aurora.world.quest.JournalEntry;
import ru.game.aurora.world.quest.SentientStonesQuestGenerator;
import ru.game.aurora.world.space.*;

import java.util.Iterator;
import java.util.Map;

/**
 * Creates Klisk alien race
 */
public class KliskGenerator implements WorldGeneratorPart {
    private static final long serialVersionUID = -6983386879381885934L;

    public static final String NAME = "Klisk";

    // ship IDs used in factory generation
    public static final int DEFAULT_SHIP = 0;

    public static final int TRADE_PROBE = 1;

    public static final int STATION = 2;

    private Dialog createDefaultKliskPlanetDialog(World world) {
        Dialog d = Dialog.loadFromFile("dialogs/klisk/klisk_planet_default.json");
        d.addListener(new DialogListener() {
            private static final long serialVersionUID = 4082728827280648178L;

            @Override
            public void onDialogEnded(World world, Dialog dialog, int returnCode, Map<String, String> flags) {
                if (returnCode == 100) {
                    //trade
                    TradeScreenController.openTrade("klisk_vip_dialog", KliskMainDialogListener.getDefaultTradeInventory(world));
                    return;
                } else if (returnCode == 131) {
                    SentientStonesQuestGenerator.processKliskDialogResult(world);
                } else if (returnCode == 129) {
                    // heritage quest
                    Dialog heritageEndDialog = Dialog.loadFromFile("dialogs/encounters/heritage/heritage_klisk_final.json");
                    heritageEndDialog.addListener(new HeritageKliskDialogListener());
                    world.addOverlayWindow(heritageEndDialog);
                    return;
                }
                if (flags.containsKey("klisk.war_help")) {
                    world.getGlobalVariables().put("klisk.war_help", true);
                }

                if (flags.containsKey("klisk_trader_drone.withdraw")) {
                    // remove trader drone
                    for (Iterator<GameObject> iter = ((StarSystem) world.getGlobalVariables().get("solar_system")).getShips().iterator(); iter.hasNext(); ) {
                        GameObject so = iter.next();
                        if (so.getName().equals("Klisk trade probe")) {
                            iter.remove();
                            break;
                        }
                    }

                    world.getGlobalVariables().put("klisk_trader_drone.result", "withdraw");
                    world.getPlayer().getJournal().addQuestEntries("klisk_trader_drone", "withdrawed");
                }

                if (world.getGlobalVariables().containsKey("klisk_trade.result")) {
                    int repDelta = 0;
                    final String tradeResult = (String) world.getGlobalVariables().get("klisk_trade.result");
                    switch (tradeResult) {
                        case "perfect":
                            repDelta = 2;
                            AchievementManager.getInstance().achievementUnlocked(AchievementNames.kliskCodex);
                            break;
                        case "good":
                            repDelta = 1;
                            break;
                        case "bad":
                            repDelta = -1;
                            break;
                    }
                    world.getPlayer().getJournal().questCompleted("klisk_trade");
                    EmbassiesQuest.updateJournal(world, "klisk_" + tradeResult);
                    world.getReputation().updateReputation(KliskGenerator.NAME, HumanityGenerator.NAME, repDelta);
                    world.getGlobalVariables().remove("klisk_trade.result");
                    world.getGlobalVariables().put("klisk_trade.quest_result", tradeResult);
                }
            }
        });

        return d;
    }

    private void beginTradeQuest(World world, AlienHomeworld kliskPlanet, final StarSystem targetSystem) {
        NPCShip ship = ((AlienRace) world.getFactions().get(KliskGenerator.NAME)).getDefaultFactory().createShip(world, 0);
        ship.setCaptain(new NPC(Dialog.loadFromFile("dialogs/klisk/klisk_trade_quest_ship_default.json")));

        ship.setAi(new LeaveSystemAI());
        ship.setPos(kliskPlanet.getX() - 1, kliskPlanet.getY() + 1);
        world.getCurrentStarSystem().getShips().add(ship);
        world.getPlayer().getJournal().addQuestEntries("klisk_trade", "start");
        world.addListener(new KliskTradequestDialogListener(targetSystem));
    }

    static Dialog loadTradeStationDefaultDialog(final Faction race) {
        final Dialog stationDialog = Dialog.loadFromFile("dialogs/klisk/klisk_trade_quest_station_default.json");
        stationDialog.addListener(new DialogListener() {
            @Override
            public void onDialogEnded(World world, Dialog dialog, int returnCode, Map<String, String> flags) {
                if (returnCode == 1) {
                    Multiset<InventoryItem> defaultTradeInventory = HashMultiset.create();
                    defaultTradeInventory.add(new KliskTradeItems.AdvancedRadarsSellItem());
                    defaultTradeInventory.add(new KliskTradeItems.AlienAlloysSellItem());
                    defaultTradeInventory.add(new KliskTradeItems.ResourceSellItem());
                    defaultTradeInventory.add(new KliskTradeItems.ScienceTheorySellItem("math"));
                    defaultTradeInventory.add(new KliskTradeItems.ScienceTheorySellItem("physics"));
                    defaultTradeInventory.add(new ShipLootItem(ShipLootItem.Type.GOODS, world.getFactions().get(KliskGenerator.NAME)));
                    defaultTradeInventory.add(new ShipLootItem(ShipLootItem.Type.COMPUTERS, world.getFactions().get(RoguesGenerator.NAME)));
                    defaultTradeInventory.add(new ShipLootItem(ShipLootItem.Type.ENERGY, world.getFactions().get(RoguesGenerator.NAME)));
                    defaultTradeInventory.add(new ShipLootItem(ShipLootItem.Type.MATERIALS, world.getFactions().get(KliskGenerator.NAME)));
                    defaultTradeInventory.add(new ShipLootItem(ShipLootItem.Type.WEAPONS, world.getFactions().get(BorkGenerator.NAME)));
                    TradeScreenController.openTrade("rogues_dialog", defaultTradeInventory, race);
                }
            }
        });
        return stationDialog;
    }

    private StarSystem generateTargetStarsystemForTradeQuest(World world, final AlienRace race) {
        StarSystem ss = WorldGenerator.generateRandomStarSystem(world, 12, 15);
        world.getGalaxyMap().addObjectAtDistance(ss, (Positionable) world.getGlobalVariables().get("solar_system"), 20);
        world.getGlobalVariables().put("klisk_trade.coords", ss.getCoordsString());

        NPCShip spaceStation = race.getDefaultFactory().createShip(world, STATION);
        ss.setRandomEmptyPosition(spaceStation);
        ss.getShips().add(spaceStation);

        spaceStation.setCaptain(new NPC(loadTradeStationDefaultDialog(race)));
        ss.setQuestLocation(true);
        return ss;
    }

    private Dialog createPlanetDialogAndQuests(final AlienHomeworld kliskPlanet, final StarSystem targetSystemForQuest) {
        Dialog startDialog = Dialog.loadFromFile("dialogs/klisk/klisk_station_start.json");

        Dialog ambassadorDialog = Dialog.loadFromFile("dialogs/klisk/klisk_station_main.json");
        startDialog.addListener(new NextDialogListener(ambassadorDialog));
        ambassadorDialog.addListener(new DialogListener() {
            private static final long serialVersionUID = 4082728827280648178L;

            @Override
            public void onDialogEnded(World world, Dialog dialog, int returnCode, Map<String, String> flags) {
                world.getGlobalVariables().put("diplomacy.klisk_visited", 0);
                if (returnCode == 0) {
                    // no quest
                    world.getReputation().updateReputation(KliskGenerator.NAME, HumanityGenerator.NAME, -1);
                    world.getPlayer().getJournal().addQuestEntries("embassies", "klisk_quest_refused");
                    world.getGlobalVariables().put("klisk_trade.quest_result", "refused");
                } else {
                    // accepts a quest
                    world.addOverlayWindow(Dialog.loadFromFile("dialogs/klisk/klisk_trade_quest_start.json"));
                    world.getGlobalVariables().put("klisk_trade.started", 1);
                    beginTradeQuest(world, kliskPlanet, targetSystemForQuest);
                }
                world.getPlayer().getEarthState().getMessages().add(new PrivateMessage(world, "news_sender", "klisk.ambassador_visit", "news"));
                kliskPlanet.setDialog(createDefaultKliskPlanetDialog(world));
            }
        });

        return startDialog;
    }


    private StarSystem generateKliskHomeworld(World world, int x, int y, AlienRace kliskRace) {
        BasePlanet[] planets = new BasePlanet[3];
        StarSystem ss = new StarSystem(world.getStarSystemNamesCollection().popName(), new Star(2, Color.yellow), x, y);

        planets[0] = new Planet(world, ss, PlanetCategory.PLANET_ROCK, PlanetAtmosphere.NO_ATMOSPHERE, 4, 0, 0);
        HomeworldGenerator.setCoord(planets[0], 2);

        planets[1] = new AlienHomeworld("klisk_homeworld", kliskRace, null, 3, 0, ss, PlanetAtmosphere.PASSIVE_ATMOSPHERE, 0, PlanetCategory.PLANET_ROCK);
        ((AlienHomeworld) planets[1]).setDialog(createPlanetDialogAndQuests((AlienHomeworld) planets[1], generateTargetStarsystemForTradeQuest(world, kliskRace)));
        HomeworldGenerator.setCoord(planets[1], 3);

        planets[2] = new Planet(world, ss, PlanetCategory.PLANET_ICE, PlanetAtmosphere.PASSIVE_ATMOSPHERE, 3, 0, 0);
        HomeworldGenerator.setCoord(planets[2], 5);

        AlienArtifact a = new AlienArtifact(3, 4, "small_artifact", new ArtifactResearch(new ResearchReport("small_artifact", "klisk_banner.report")));
        final Planet planet = (Planet) planets[2];
        planet.setNearestFreePoint(a, CommonRandom.getRandom().nextInt(planet.getWidth()), CommonRandom.getRandom().nextInt(planet.getHeight()));
        planet.getPlanetObjects().add(a);

        ss.setPlanets(planets);
        ss.setQuestLocation(true);
        ss.setRadius(Math.max((int) (6 * 1.5), 10));
        return ss;
    }

    @Override
    public void updateWorld(World world) {
        Dialog mainDialog = Dialog.loadFromFile("dialogs/klisk_1.json");
        final AlienRace kliskRace = new AlienRace(NAME, "klisk_ship", mainDialog);

        kliskRace.getDefaultLootTable().put(new SpaceDebris.ItemDebris(new ShipLootItem(ShipLootItem.Type.COMPUTERS, kliskRace)), 0.5);
        kliskRace.getDefaultLootTable().put(new SpaceDebris.ItemDebris(new ShipLootItem(ShipLootItem.Type.ENERGY, kliskRace)), 0.3);
        kliskRace.getDefaultLootTable().put(new SpaceDebris.ItemDebris(new ShipLootItem(ShipLootItem.Type.GOODS, kliskRace)), 0.3);
        kliskRace.getDefaultLootTable().put(new SpaceDebris.ItemDebris(new ShipLootItem(ShipLootItem.Type.MATERIALS, kliskRace)), 0.6);
        kliskRace.getDefaultLootTable().put(new SpaceDebris.ItemDebris(new ShipLootItem(ShipLootItem.Type.WEAPONS, kliskRace)), 0.15);

        kliskRace.setMusic(ResourceManager.getInstance().getPlaylist("Klisk"));
        mainDialog.addListener(new DialogListener() {

            private static final long serialVersionUID = 8770464358766507288L;

            @Override
            public void onDialogEnded(World world, Dialog dialog, int returnCode, Map<String, String> flags) {
                world.getGlobalVariables().put("klisk.first_contact", true);
                switch (returnCode) {
                    case 3:
                        // free info about klisk race
                        world.getGlobalVariables().put("klisk.klisk_info", true);
                        ResearchProjectDesc research = new AlienRaceResearch("klisk", (AlienRace) world.getFactions().get(KliskGenerator.NAME), new JournalEntry("klisk", "main"));
                        world.getPlayer().getResearchState().addNewAvailableProject(research);
                        break;
                    case 4:
                        // free info about colony planet
                        world.getGlobalVariables().put("klisk.planet_info", "0");
                        break;
                    case 10:
                        // decided to take time
                        return;
                }

                if (flags.containsKey("klisk.knows_about_path_philosophy")) {
                    world.getGlobalVariables().put("klisk.knows_about_path_philosophy", true);
                }

                Dialog newDefaultDialog = Dialog.loadFromFile("dialogs/klisk_main.json");
                newDefaultDialog.addListener(new KliskMainDialogListener());
                kliskRace.setDefaultDialog(newDefaultDialog);
            }
        });
        kliskRace.setDefaultFactory(new NPCShipFactory() {
            private static final long serialVersionUID = 5473066320214324094L;

            @Override
            public NPCShip createShip(World world, int shipType) {

                NPCShip ship;
                switch (shipType) {
                    case DEFAULT_SHIP:
                        ship = new NPCShip("klisk_ship");
                        ship.setWeapons(ResourceManager.getInstance().getWeapons().getEntity("klisk_small_laser"), ResourceManager.getInstance().getWeapons().getEntity("klisk_large_laser"));
                        ship.setSpeed(2);
                        break;

                    case TRADE_PROBE:
                        ship = new NPCShip("klisk_drone");
                        ship.setWeapons(ResourceManager.getInstance().getWeapons().getEntity("klisk_small_laser"));
                        ship.setCanBeHailed(false);
                        ship.setSpeed(2);
                        break;
                    case STATION:
                        ship = new NPCShip("klisk_station");
                        ship.setWeapons(ResourceManager.getInstance().getWeapons().getEntity("klisk_small_laser"), ResourceManager.getInstance().getWeapons().getEntity("klisk_large_laser"));
                        ship.enableRepairs(3);
                        ship.setSpeed(2);
                        break;
                    default:
                        throw new IllegalArgumentException("Klisk race does not define ship of type " + shipType);
                }
                ship.setLoot(kliskRace.getDefaultLootTable());
                return ship;
            }
        });
        StarSystem kliskHomeworld = generateKliskHomeworld(world, 15, 15, kliskRace);
        kliskRace.setHomeworld(kliskHomeworld);

        world.addListener(new StandardAlienShipEvent(kliskRace));
        final GalaxyMapObject solar_system = (GalaxyMapObject) world.getGlobalVariables().get("solar_system");
        world.getGalaxyMap().addObjectAtDistance(kliskHomeworld, solar_system, 20 + CommonRandom.getRandom().nextInt(Configuration.getIntProperty("world.galaxy.klisk_homeworld_distance")));
        world.getFactions().put(kliskRace.getName(), kliskRace);
        world.getGlobalVariables().put("klisk.homeworld", kliskHomeworld.getCoordsString());

    }
}
