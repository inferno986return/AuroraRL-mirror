/**
 * User: jedi-philosopher
 * Date: 11.12.12
 * Time: 20:11
 */
package ru.game.aurora.world.space;

import org.newdawn.slick.Color;
import ru.game.aurora.application.CommonRandom;
import ru.game.aurora.application.ResourceManager;
import ru.game.aurora.dialog.Dialog;
import ru.game.aurora.npc.AlienRace;
import ru.game.aurora.npc.NPC;
import ru.game.aurora.npc.shipai.LeaveSystemAI;
import ru.game.aurora.player.research.ResearchReport;
import ru.game.aurora.player.research.projects.ArtifactResearch;
import ru.game.aurora.world.World;
import ru.game.aurora.world.equip.StarshipWeapon;
import ru.game.aurora.world.planet.*;
import ru.game.aurora.world.space.earth.Earth;

public class HomeworldGenerator {

    public static void setCoord(BasePlanet p, int radius) {
        int planetX = CommonRandom.getRandom().nextInt(2 * radius) - radius;

        int planetY = (int) (Math.sqrt(radius * radius - planetX * planetX) * (CommonRandom.getRandom().nextBoolean() ? -1 : 1));

        p.setGlobalX(planetX);
        p.setGlobalY(planetY);
    }

    public static StarSystem createSolarSystem(AlienRace humanRace) {
        // todo: gas giants and other planets

        Planet[] planets = new Planet[4];
        StarSystem ss = new StarSystem("Solar system", new Star(2, Color.yellow), 9, 9);

        // mercury
        planets[0] = new Planet(ss, PlanetCategory.PLANET_ROCK, PlanetAtmosphere.NO_ATMOSPHERE, 4, 0, 0);
        setCoord(planets[0], 2);

        // venus
        planets[1] = new Planet(ss, PlanetCategory.PLANET_ROCK, PlanetAtmosphere.PASSIVE_ATMOSPHERE, 3, 0, 0);
        setCoord(planets[1], 3);

        planets[2] = new Earth(ss, PlanetCategory.PLANET_ROCK, PlanetAtmosphere.BREATHABLE_ATMOSPHERE, 3, 0, 0);
        setCoord(planets[2], 5);

        // mars
        planets[3] = new Planet(ss, PlanetCategory.PLANET_ROCK, PlanetAtmosphere.PASSIVE_ATMOSPHERE, 3, 0, 0);
        setCoord(planets[3], 5);


        ss.setPlanets(planets);
        ss.setRadius(Math.max((int) (6 * 1.5), 10));

        NPCShip spaceStation = new NPCShip(planets[2].getGlobalX() + 1, planets[2].getGlobalY() - 1, "earth_station", humanRace, null, "Orbital Scaffold");
        spaceStation.setStationary(true);
        spaceStation.setAi(null);
        ss.getShips().add(spaceStation);

        return ss;
    }


    public static StarSystem generateKliskHomeworld(World world, int x, int y, AlienRace kliskRace) {
        BasePlanet[] planets = new BasePlanet[3];
        StarSystem ss = new StarSystem(world.getStarSystemNamesCollection().popName(), new Star(2, Color.yellow), x, y);

        planets[0] = new Planet(ss, PlanetCategory.PLANET_ROCK, PlanetAtmosphere.NO_ATMOSPHERE, 4, 0, 0);
        setCoord(planets[0], 2);

        planets[1] = new AlienHomeworld("klisk_homeworld", kliskRace, kliskRace.getDefaultDialog(), 3, 0, ss, PlanetAtmosphere.PASSIVE_ATMOSPHERE, 0, PlanetCategory.PLANET_ROCK);
        setCoord(planets[1], 3);

        planets[2] = new Planet(ss, PlanetCategory.PLANET_ICE, PlanetAtmosphere.PASSIVE_ATMOSPHERE, 3, 0, 0);
        setCoord(planets[2], 5);

        AlienArtifact a = new AlienArtifact(3, 4, "small_artifact", new ArtifactResearch(new ResearchReport("small_artifact", "This artifact represents a platinum plate with engraved message on it. Based on our knowlege of klisk language, we could decipher it partially." +
                " It seems to tell about the nation that was first to reach another planet in their home system. Similar plates were sent by humnanity to Moon and Mars with first probes that reached these celestial bodies." +
                " \n It is purely a memorial and posess little scientific interest. However perhaps it will help learn Klisk language and culture better, when Earth linguists and xenopshycologists will decipher it completely.")));
        ((Planet) planets[2]).setNearestFreePoint(a, 2, 2);
        ((Planet) planets[2]).getPlanetObjects().add(a);

        ss.setPlanets(planets);

        ss.setRadius(Math.max((int) (6 * 1.5), 10));
        return ss;
    }

    public static StarSystem generateRoguesWorld(World world, int x, int y, AlienRace roguesRace) {
        BasePlanet[] planets = new BasePlanet[2];
        StarSystem ss = new StarSystem(world.getStarSystemNamesCollection().popName(), new Star(2, Color.red), x, y);

        planets[0] = new Planet(ss, PlanetCategory.PLANET_ROCK, PlanetAtmosphere.NO_ATMOSPHERE, 4, 0, 0);
        setCoord(planets[0], 2);

        planets[1] = new Planet(ss, PlanetCategory.PLANET_ICE, PlanetAtmosphere.PASSIVE_ATMOSPHERE, 3, 0, 0);
        setCoord(planets[1], 5);

        ss.setPlanets(planets);
        ss.setRadius(8);

        NPCShip frame = new NPCShip(2, 2, "rogues_frame", roguesRace, new NPC(Dialog.loadFromFile("dialogs/rogues_frame_dialog.json")), "Rogues Frame");
        frame.setAi(null);
        ss.getShips().add(frame);

        for (int i = 0; i < 3; ++i) {
            NPCShip probe = new NPCShip(CommonRandom.getRandom().nextInt(6) - 3, CommonRandom.getRandom().nextInt(6) - 3, "rogues_probe", roguesRace, null, "Defence drone");
            probe.setStationary(true);
            probe.setCanBeHailed(false);
            probe.setAi(new LeaveSystemAI());
            probe.setWeapons(new StarshipWeapon(ResourceManager.getInstance().getWeapons().getEntity("plasma_cannon"), StarshipWeapon.MOUNT_ALL));
            ss.getShips().add(probe);
        }

        return ss;
    }
}
