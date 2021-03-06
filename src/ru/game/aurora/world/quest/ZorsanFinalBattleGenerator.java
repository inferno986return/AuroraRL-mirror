package ru.game.aurora.world.quest;

import org.newdawn.slick.GameContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.game.aurora.application.CommonRandom;
import ru.game.aurora.application.Configuration;
import ru.game.aurora.application.Localization;
import ru.game.aurora.application.ResourceManager;
import ru.game.aurora.dialog.Dialog;
import ru.game.aurora.dialog.DialogListener;
import ru.game.aurora.gui.FailScreenController;
import ru.game.aurora.gui.GUI;
import ru.game.aurora.gui.StoryScreen;
import ru.game.aurora.npc.AlienRace;
import ru.game.aurora.npc.CrewMember;
import ru.game.aurora.npc.NPC;
import ru.game.aurora.npc.shipai.LandAI;
import ru.game.aurora.player.earth.PrivateMessage;
import ru.game.aurora.steam.AchievementManager;
import ru.game.aurora.steam.AchievementNames;
import ru.game.aurora.util.GameTimer;
import ru.game.aurora.world.*;
import ru.game.aurora.world.equip.WeaponDesc;
import ru.game.aurora.world.generation.aliens.KliskGenerator;
import ru.game.aurora.world.generation.aliens.RoguesGenerator;
import ru.game.aurora.world.generation.aliens.bork.BorkGenerator;
import ru.game.aurora.world.generation.aliens.zorsan.ZorsanGenerator;
import ru.game.aurora.world.generation.humanity.HumanityGenerator;
import ru.game.aurora.world.quest.act2.SecondPartStarter;
import ru.game.aurora.world.space.GalaxyMapObject;
import ru.game.aurora.world.space.NPCShip;
import ru.game.aurora.world.space.StarSystem;
import ru.game.aurora.world.space.earth.Earth;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: User
 * Date: 20.02.14
 * Time: 23:06
 */
public class ZorsanFinalBattleGenerator extends GameEventListener implements DialogListener {
    private static final Logger logger = LoggerFactory.getLogger(ZorsanFinalBattleGenerator.class);
    private static final long serialVersionUID = 2198763272604170716L;
    private final List<NPCShip> currentWave = new LinkedList<>();
    private final List<NPCShip> allyShips = new LinkedList<>();
    private final int shotsDone = 0;
    private State state = State.CREATED;
    private int turnNumber;
    private StarSystem solarSystem;
    private AlienRace humanity;
    private AlienRace zorsan;
    private Earth earth;
    private int dropShipsLanded = 0;
    private StarSystem closestStarSystem = null;
    // if it is not null - space station is being boarded
    // if it becomes signalled - station is captured and destroyed
    private GameTimer spaceStationBoardingTimer = null;
    private NPCShip spaceStation = null;
    private Dungeon spaceStationDungeon = null;

    // when first troop transport reaches earth, it also starts an attack on a space station
    private void attackSpaceStation(World world) {
        if (spaceStation == null || !spaceStation.isAlive()) {
            // already destroyed
            return;
        }

        Dialog boardingDialog = Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_before_station_boarding.json");
        boardingDialog.addListener(this);
        spaceStation.setCaptain(new NPC(boardingDialog));
        spaceStationBoardingTimer = new GameTimer(15);
        spaceStationDungeon = new Dungeon(world, new AuroraTiledMap("maps/hum_station.tmx"), solarSystem);
        spaceStationDungeon.getController().addListener(new IStateChangeListener<World>() {
            private static final long serialVersionUID = 7759568719556795246L;

            @Override
            public void stateChanged(World world) {
                world.addOverlayWindow(Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_station_saved.json"));
                spaceStationBoardingTimer = null;
                spaceStation.setCaptain(new NPC(Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_humanity_default.json")));
            }
        });
    }

    @Override
    public void onDialogEnded(World world, Dialog dialog, int returnCode, Map<String, String> flags) {
        if (dialog.getId().equals("zorsan_battle_before_station_boarding")) {
            spaceStationDungeon.enter(world);
        } else if (dialog.getId().equals("zorsan_battle_before_start")) {
            updateWorld(world);
            if (returnCode == 1) {
                world.setCurrentRoom(earth.getOwner());
                GUI.getInstance().getNifty().gotoScreen("earth_screen");
            }
        }
    }

    private void summonFirstWaveOfReinforcements(World world) {
        String val = (String) world.getGlobalVariables().get("klisk_trader_drone.result");
        if ("withdraw".equals(val)) {
            world.addOverlayWindow(Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_klisk_arrive.json"));
            final int ships = Configuration.getIntProperty("quest.zorsan_final_battle.klisk_ships");

            for (int i = 0; i < ships; ++i) {
                NPCShip probe = ((AlienRace) world.getFactions().get(KliskGenerator.NAME)).getDefaultFactory().createShip(world, KliskGenerator.TRADE_PROBE);
                probe.setCanBeHailed(false);
                probe.setStationary(false);
                probe.setSpeed(2);
                probe.setPos(earth.getX() + 1 + i, earth.getY() + CommonRandom.getRandom().nextInt(3) - 1);
                solarSystem.getShips().add(probe);
                allyShips.add(probe);
            }
        }

        if (world.getGlobalVariables().containsKey("bork.war_help")) {
            world.addOverlayWindow(Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_bork_arrive.json"));
            final int ships = Configuration.getIntProperty("quest.zorsan_final_battle.bork_ships");

            for (int i = 0; i < ships; ++i) {
                NPCShip ship = ((AlienRace) world.getFactions().get(BorkGenerator.NAME)).getDefaultFactory().createShip(world, 0);
                ship.setCanBeHailed(false);
                ship.setPos(2 + i, i);
                solarSystem.getShips().add(ship);
                allyShips.add(ship);
            }
        }
    }

    private void summonSecondWaveOfReinforcements(World world) {

        if ("help".equals(world.getGlobalVariables().get("rogues_altar.result"))) {
            world.addOverlayWindow(Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_rogues_arrive.json"));

            final int ships = Configuration.getIntProperty("quest.zorsan_final_battle.rogues_ships");
            AlienRace rogues = (AlienRace) world.getFactions().get(RoguesGenerator.NAME);
            for (int i = 0; i < ships; ++i) {
                NPCShip ship = rogues.getDefaultFactory().createShip(world, RoguesGenerator.SCOUT_SHIP);
                NPCShip probe = rogues.getDefaultFactory().createShip(world, RoguesGenerator.PROBE_SHIP);
                ship.setCanBeHailed(false);
                probe.setPos(earth.getX() + i, earth.getY() + i - 1);
                ship.setPos(earth.getX() - 2 + i, earth.getY() + i);
                solarSystem.getShips().add(ship);
                solarSystem.getShips().add(probe);
                allyShips.add(ship);
                allyShips.add(probe);
            }
        }

        if ("pay".equals(world.getGlobalVariables().get("bork_blockade.result"))) {
            world.addOverlayWindow(Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_bork_event_arrive.json"));
            final int ships = Configuration.getIntProperty("quest.zorsan_final_battle.bork_event_ships");

            for (int i = 0; i < ships; ++i) {
                NPCShip ship = ((AlienRace) world.getFactions().get(BorkGenerator.NAME)).getDefaultFactory().createShip(world, 0);
                ship.setCanBeHailed(false);
                ship.setPos(2 + i, i);
                solarSystem.getShips().add(ship);
                allyShips.add(ship);
            }
        }
    }

    private void summonFirstAttackWave(World world) {
        // first wave consists of some zorsan ships
        int count = Configuration.getIntProperty("quest.zorsan_final_battle.first_wave_ships");
        for (int i = 0; i < count; ++i) {
            // spawn in 2 lines, one straight and one broken
            NPCShip ship = zorsan.getDefaultFactory().createShip(world, 0);
            ship.setPos(-solarSystem.getRadius() - (i < (count / 2) ? 4 : 0) - CommonRandom.getRandom().nextInt(6), i * 2);
            currentWave.add(ship);
            solarSystem.getShips().add(ship);
        }

        world.addOverlayWindow(Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_first_wave.json"));
    }

    private void summonFirstAttackWaveReinforcements(World world) {
        world.addOverlayWindow(Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_first_wave_reinforcements.json"));
        int count = Configuration.getIntProperty("quest.zorsan_final_battle.first_wave_ships");
        for (int i = 0; i < count / 2; ++i) {
            NPCShip ship = zorsan.getDefaultFactory().createShip(world, ZorsanGenerator.CRUISER_SHIP);
            ship.setPos(solarSystem.getRadius(), i);
            currentWave.add(ship);
            solarSystem.getShips().add(ship);
        }
    }

    private Dialog preprocessDialog(Dialog dialog) {
        if (dropShipsLanded >= 5) {
            // put america leader portrait instead of martan
            dialog.setIconName("usa_leader");
        }
        return dialog;
    }

    @Override
    public boolean onTurnEnded(World world) {
        if (state == State.CREATED && world.getDayCount() - turnNumber > 5) {
            summonFirstWaveOfReinforcements(world);
            state = State.REINFORCEMENTS_ARRIVED;
            return true;
        }

        if (state == State.REINFORCEMENTS_ARRIVED && world.getDayCount() - turnNumber > 5) {
            summonFirstAttackWave(world);
            turnNumber = world.getDayCount();
            state = State.FIRST_WAVE_COMBAT;
            return true;
        }

        if (state == State.FIRST_WAVE_COMBAT && world.getDayCount() - turnNumber > 30) {
            // suddenly some ships appear from another side
            summonFirstAttackWaveReinforcements(world);
            state = State.FIRST_WAVE_REINFORCEMENTS;
            return true;
        }

        if (world.getCurrentDungeon() == null && spaceStationBoardingTimer != null && spaceStationBoardingTimer.update()) {
            // player did not help space station, destroy it
            if (spaceStation.isAlive()) {
                spaceStation.onAttack(world, null, spaceStation.getHp());
                world.addOverlayWindow(preprocessDialog(Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_station_captured.json")));
            }
            spaceStationBoardingTimer = null;
        }

        // clean up dead attacking ships
        for (Iterator<NPCShip> iter = currentWave.iterator(); iter.hasNext(); ) {
            NPCShip ship = iter.next();
            if (!ship.isAlive()) {
                iter.remove();
            }
        }

        if (state == State.FIRST_WAVE_REINFORCEMENTS && currentWave.isEmpty()) {
            state = State.FIRST_WAVE_INTERMISSION;
            turnNumber = world.getDayCount();
            repairAllies();
            world.addOverlayWindow(Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_first_wave_ended.json"));
            return true;
        }

        if (state == State.FIRST_WAVE_INTERMISSION && world.getDayCount() - turnNumber > 5) {
            state = State.ALL_REINFORCEMENTS_ARRIVED;
            turnNumber = world.getDayCount();
            summonSecondWaveOfReinforcements(world);
            return true;
        }

        if (state == State.ALL_REINFORCEMENTS_ARRIVED && world.getDayCount() - turnNumber > 5) {
            world.addOverlayWindow(Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_second_wave.json"));
            summonSecondAttackWave(world);
            state = State.SECOND_WAVE_COMBAT;
            return true;
        }

        if (state == State.SECOND_WAVE_COMBAT && currentWave.size() <= 1) {
            world.addOverlayWindow(preprocessDialog(Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_third_wave.json")));
            summonThirdAttackWave(world);
            repairAllies();
            state = State.THIRD_WAVE_COMBAT;
            return true;
        }

        if (state == State.THIRD_WAVE_COMBAT && currentWave.size() == 0) {

            boolean kliskAlive = false;

            boolean borkAlive = false;

            boolean roguesAlive = false;

            boolean voyagerAlive = false;

            solarSystem.setCanBeLeft(true);
            for (NPCShip s : allyShips) {
                if (s.isAlive()) {
                    solarSystem.getShips().remove(s);
                } else {
                    continue;
                }

                if (s.getFaction().getName().equals(BorkGenerator.NAME) && "pay".equals(world.getGlobalVariables().get("bork_blockade.result"))) {
                    world.getGlobalVariables().put("bork.hrrraka_alive", true);
                    borkAlive = true;
                } else if (s.getFaction().getName().equals(RoguesGenerator.NAME)) {
                    roguesAlive = true;
                    world.getGlobalVariables().put("rogues.fuko_alive", true);
                } else if (s.getName().equals("Voyager")) {
                    voyagerAlive = true;
                } else if (s.getFaction().getName().equals(KliskGenerator.NAME)) {
                    kliskAlive = true;
                }
            }
            world.getGlobalVariables().remove("zorsan.war_preparations");

            closestStarSystem = world.getGalaxyMap().getClosestStarSystem(solarSystem);
            Map<String, String> flags = new HashMap<>();
            flags.put("closest_coords", closestStarSystem.getCoordsString());
            flags.put("earth_damage", Integer.toString(dropShipsLanded + shotsDone));
            world.getPlayer().getJournal().addQuestEntries("zorsan_battle_after_recon", "start");


            if (borkAlive) {
                world.addOverlayWindow(Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_after_hrraka.json"), flags);
            }

            if (roguesAlive) {
                world.addOverlayWindow(Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_after_rogues.json"), flags);
            }

            if (voyagerAlive) {
                world.addOverlayWindow(Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_after_voyager.json"), flags);
            } else {
                world.getGlobalVariables().remove("voyager");
            }

            if (kliskAlive) {
                world.addOverlayWindow(Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_after_klisk.json"), flags);
            }

            world.addOverlayWindow(Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_after_earth.json"), flags);

            world.getPlayer().getEarthState().getEarthSpecialDialogs().clear();

            world.getPlayer().getEarthState().getMessages().add(new PrivateMessage(world, "news_sender", "zorsan_attack_victory", "news"));


            state = State.OVER;
            world.getPlayer().getShip().setDefaultCrewDialogs(world);

            // reset earth counters so that player does not get fired after saving Earth, lol
            earth.setLastVisitTurn(world.getDayCount());
            world.getPlayer().resetFailCount();
        }

        if (state == State.RECON_DONE && world.getDayCount() - turnNumber > 5) {

            final StoryScreen storyScreen = new StoryScreen("story/obliterator.json");
            world.addOverlayWindow(storyScreen);
            storyScreen.setListener(new IStateChangeListener<World>() {

                private static final long serialVersionUID = -7376617022370202993L;

                @Override
                public void stateChanged(World world) {
                    final SecondPartStarter secondPartStarter = new SecondPartStarter();
                    secondPartStarter.start(world);
                }
            });
            world.addOverlayWindow(Dialog.loadFromFile("dialogs/quest/main/obliterator_arrival.json"));
            world.getListeners().remove(this);
        }


        return false;
    }

    @Override
    public String getLocalizedMessageForStarSystem(World world, GalaxyMapObject galaxyMapObject) {
        if (galaxyMapObject.equals(closestStarSystem)) {
            return Localization.getText("journal", "zorsan_battle_after_recon.title");
        }
        return null;
    }

    @Override
    public boolean onPlayerEnterStarSystem(World world, StarSystem ss) {
        if (ss.equals(closestStarSystem)) {
            world.addOverlayWindow(Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_after_recon.json"));
            world.getGlobalVariables().put("zorsan_recon.completed", true);
            world.getPlayer().getJournal().questCompleted("zorsan_battle_after_recon", "end");
            closestStarSystem = null;
        } else if (ss.equals(solarSystem) && world.getGlobalVariables().containsKey("zorsan_recon.completed")) {
            state = State.RECON_DONE;
            solarSystem.setBackgroundSprite("obliterator_background");
            Object oldLocation = world.getGlobalVariables().get("quest.main.obliterator");
            if (oldLocation != null) {
                ((StarSystem) oldLocation).setBackgroundSprite(null);
            }
            turnNumber = world.getDayCount();
        }

        return false;
    }

    private void repairAllies() {
        for (NPCShip s : allyShips) {
            s.setHp(s.getMaxHP());
        }
        for (GameObject s : solarSystem.getShips()) {
            if (s instanceof NPCShip && s.getFaction().getName().equals(HumanityGenerator.NAME)) {
                ((NPCShip) s).setHp(((NPCShip) s).getMaxHP());
            }
        }
    }

    private void summonThirdAttackWave(World world) {
        int ships = Configuration.getIntProperty("quest.zorsan_final_battle.third_wave_ships");
        for (int i = 0; i < ships; ++i) {

            NPCShip warship = zorsan.getDefaultFactory().createShip(world, CommonRandom.getRandom().nextBoolean() ? ZorsanGenerator.CRUISER_SHIP : ZorsanGenerator.SCOUT_SHIP);
            warship.setPos(1 + 2 * i, -solarSystem.getRadius() - CommonRandom.getRandom().nextInt(5));
            currentWave.add(warship);
            solarSystem.getShips().add(warship);
        }

        NPCShip bigBoss = new NPCShip("zorsan_boss");
        currentWave.add(bigBoss);
        solarSystem.getShips().add(bigBoss);
        bigBoss.setWeapons(ResourceManager.getInstance().getWeapons().getEntity("zorsan_cannon"), ResourceManager.getInstance().getWeapons().getEntity("zorsan_boss_cannon"));
        // todo: add areal damage
        bigBoss.setPos(-1, -solarSystem.getRadius());
    }

    private void summonSecondAttackWave(World world) {
        int ships = Configuration.getIntProperty("quest.zorsan_final_battle.second_wave_ships");
        for (int i = 0; i < ships; ++i) {

            NPCShip warship = zorsan.getDefaultFactory().createShip(world, CommonRandom.getRandom().nextBoolean() ? ZorsanGenerator.CRUISER_SHIP : ZorsanGenerator.SCOUT_SHIP);
            warship.setPos(2 * i, solarSystem.getRadius());
            solarSystem.getShips().add(warship);
            currentWave.add(warship);

            ZorsanTroopTransport transport = new ZorsanTroopTransport(warship.getX(), warship.getY() + 8, earth);
            solarSystem.getShips().add(transport);
            currentWave.add(transport);
        }
    }

    private NPCShip createVoyager(World world) {
        List<WeaponDesc> weapons = new LinkedList<>();
        //default weapons
        weapons.add(ResourceManager.getInstance().getWeapons().getEntity("laser_cannon2"));
        weapons.add(ResourceManager.getInstance().getWeapons().getEntity("humanity_missiles"));

        if (world.getGlobalVariables().containsKey("klisk.war_help")) {
            weapons.add(ResourceManager.getInstance().getWeapons().getEntity("klisk_large_laser"));
        }

        int hp = 15;
        if (world.getGlobalVariables().containsKey("rogues.war_help")) {
            hp *= 2;
            weapons.add(ResourceManager.getInstance().getWeapons().getEntity("plasma_cannon"));
        }

        NPCShip ship = new NPCShip("voyager");
        ship.setCaptain(new NPC(Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_voyager.json")));
        ship.setHp(hp);
        ship.setSpeed(2);
        ship.setPos(earth.getX() + CommonRandom.getRandom().nextInt(8) - 2, earth.getY() + CommonRandom.getRandom().nextInt(6) - 3);
        ship.enableRepairs(4);
        WeaponDesc[] descs = weapons.toArray(new WeaponDesc[weapons.size()]);
        ship.setWeapons(descs);
        allyShips.add(ship);
        return ship;
    }

    public void updateWorld(World world) {
        humanity = ((AlienRace) world.getFactions().get(HumanityGenerator.NAME));
        zorsan = ((AlienRace) world.getFactions().get(ZorsanGenerator.NAME));
        solarSystem = humanity.getHomeworld();
        earth = (Earth) solarSystem.getPlanets()[2];
        solarSystem.setCanBeLeft(false);

        world.getReputation().setHostile(HumanityGenerator.NAME, ZorsanGenerator.NAME);
        world.getReputation().setHostile(ZorsanGenerator.NAME, HumanityGenerator.NAME);

        solarSystem.getReputation().setHostile(HumanityGenerator.NAME, ZorsanGenerator.NAME);
        solarSystem.getReputation().setHostile(ZorsanGenerator.NAME, HumanityGenerator.NAME);

        solarSystem.getReputation().setHostile(BorkGenerator.NAME, ZorsanGenerator.NAME);
        solarSystem.getReputation().setHostile(ZorsanGenerator.NAME, BorkGenerator.NAME);

        solarSystem.getReputation().setHostile(RoguesGenerator.NAME, ZorsanGenerator.NAME);
        solarSystem.getReputation().setHostile(ZorsanGenerator.NAME, RoguesGenerator.NAME);

        solarSystem.getReputation().setHostile(KliskGenerator.NAME, ZorsanGenerator.NAME);
        solarSystem.getReputation().setHostile(ZorsanGenerator.NAME, KliskGenerator.NAME);

        // create humanity ships
        int humanityShips = Configuration.getIntProperty("quest.zorsan_final_battle.humanity_ships");
        for (int i = 0; i < humanityShips; ++i) {
            NPCShip defender = humanity.getDefaultFactory().createShip(world, 0);
            defender.setPos(earth.getX() + CommonRandom.getRandom().nextInt(6) - 3, earth.getY() + CommonRandom.getRandom().nextInt(6) - 3);
            solarSystem.getShips().add(defender);
        }

        for (GameObject so : solarSystem.getShips()) {
            if (so.getName().equals("Orbital Scaffold")) {
                spaceStation = (NPCShip) so;
                break;
            }
        }

        if (spaceStation == null) {
            logger.error("Failed to find space station in solar system");
        }

        NPCShip voyager = createVoyager(world);
        voyager.setPos(earth.getX() + 5, earth.getY() - 3);
        solarSystem.getShips().add(voyager);
        world.getGlobalVariables().put("voyager", voyager);

        final CrewMember henry = world.getPlayer().getShip().getCrewMembers().get("henry");
        henry.setDialog(null);

        final CrewMember gordon = world.getPlayer().getShip().getCrewMembers().get("gordon");
        gordon.setDialog(null);

        final CrewMember sarah = world.getPlayer().getShip().getCrewMembers().get("sarah");
        sarah.setDialog(null);

        final Dialog crewBeforeAttackDialog = Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_crew_before_attack.json");
        Map<String, String> flags = new HashMap<>();
        flags.put("crew.engineer", String.valueOf(sarah.getReputation()));
        world.addOverlayWindow(crewBeforeAttackDialog, flags);

        //todo: set earth dialog
        world.getPlayer().setResourceUnits(world.getPlayer().getResourceUnits() + 20);
        final Dialog earthDialog = Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_humanity_default.json");
        // hack: show only this earth dialog
        earthDialog.addListener(new DialogListener() {
            private static final long serialVersionUID = -3608024860992598068L;

            @Override
            public void onDialogEnded(World world, Dialog dialog, int returnCode, Map<String, String> flags) {
                world.getPlayer().getEarthState().getEarthSpecialDialogs().add(earthDialog);
            }
        });
        world.getPlayer().getEarthState().getEarthSpecialDialogs().add(earthDialog);

        turnNumber = world.getDayCount();
        ZorsanGenerator.removeWarDataDrop(world);

        world.addListener(this);

        world.getPlayer().getEarthState().getMessages().add(new PrivateMessage(world, "news_sender", "zorsan_attack_2", "news"));



        repairAllies();

        boolean kliskHelp1 = "withdraw".equals(world.getGlobalVariables().get("klisk_trader_drone.result"));
        boolean borkHelp1 = world.getGlobalVariables().containsKey("bork.war_help");

        // playe may have missed these quests as they are random. So treat them as passed by default
        boolean roguesHelp = "help".equals(world.getGlobalVariable("rogues_altar.result", "help"));
        boolean borkHelp2 = "pay".equals(world.getGlobalVariable("bork_blockade.result", "pay"));

        boolean kliskHelp2 = world.getGlobalVariables().containsKey("klisk.war_help");
        boolean roguesHelp2 = world.getGlobalVariables().containsKey("rogues.war_help");

        if (kliskHelp1 && borkHelp1 && roguesHelp && borkHelp2 && kliskHelp2 && roguesHelp2) {
            AchievementManager.getInstance().achievementUnlocked(AchievementNames.unitedGalaxy);
        }
    }

    private enum State {
        CREATED,
        REINFORCEMENTS_ARRIVED,
        FIRST_WAVE_COMBAT,
        FIRST_WAVE_REINFORCEMENTS,
        FIRST_WAVE_INTERMISSION,
        ALL_REINFORCEMENTS_ARRIVED,
        SECOND_WAVE_COMBAT,
        THIRD_WAVE_COMBAT,
        OVER,
        RECON_DONE
    }

    class ZorsanTroopTransport extends NPCShip {

        private static final long serialVersionUID = 4933360150674508485L;


        public ZorsanTroopTransport(int x, int y, Positionable target) {
            super("zorsan_transport", x, y);
            setSpeed(1);
            setAi(new LandAI(target));
            setLoot(zorsan.getDefaultLootTable());
        }

        @Override
        public void update(GameContainer container, World world) {
            if (!isAlive()) {
                return;
            }

            doMove(container);

            if (world.isUpdatedThisFrame()) {
                curSpeed--;
            }
            if (curSpeed > 0) {
                return;
            }
            curSpeed = speed;
            ai.update(this, world, solarSystem);
            if (!ai.isAlive()) {
                dropShipsLanded++;
                if (dropShipsLanded == 1) {
                    world.addOverlayWindow(Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_first_dropship.json"));
                    world.getPlayer().getEarthState().getMessages().add(new PrivateMessage(world, "news_sender", "zorsan_attack_invasion", "news"));
                    attackSpaceStation(world);
                }

                if (dropShipsLanded == 3) {
                    world.addOverlayWindow(Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_half_dropships.json"));
                }

                if (dropShipsLanded == 5) {
                    world.addOverlayWindow(Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_almost_all_dropships.json"));
                }

                if (dropShipsLanded == 6) {
                    Dialog endDialog = Dialog.loadFromFile("dialogs/zorsan/final_battle/zorsan_battle_earth_captured.json");
                    endDialog.addListener(new DialogListener() {
                        private static final long serialVersionUID = -426755352470714542L;

                        @Override
                        public void onDialogEnded(World world, Dialog dialog, int returnCode, Map<String, String> flags) {
                            GUI.getInstance().getNifty().gotoScreen("fail_screen");
                            FailScreenController controller = (FailScreenController) GUI.getInstance().getNifty().findScreenController(FailScreenController.class.getCanonicalName());
                            controller.set("crew_lost_gameover", "zorsan_captured_earth");
                        }
                    });
                    world.addOverlayWindow(endDialog);
                }
            }
        }
    }
}
