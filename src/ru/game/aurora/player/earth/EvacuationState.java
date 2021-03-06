/**
 * Created with IntelliJ IDEA.
 * User: Egor.Smirnov
 * Date: 04.04.13
 * Time: 13:56
 */
package ru.game.aurora.player.earth;


import ru.game.aurora.gui.StoryScreen;
import ru.game.aurora.npc.AlienRace;
import ru.game.aurora.world.IStateChangeListener;
import ru.game.aurora.world.World;
import ru.game.aurora.world.planet.BasePlanet;
import ru.game.aurora.world.planet.Planet;
import ru.game.aurora.world.planet.PlanetAtmosphere;
import ru.game.aurora.world.space.GalaxyMap;
import ru.game.aurora.world.space.GalaxyMapObject;
import ru.game.aurora.world.space.StarSystem;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * State of humanity evacuation from Earth
 */
public class EvacuationState implements Serializable {
    // Constants that are used to select ending based on progress

    // If evacuated count is less than this, show 'bad' ending - humanity extinction
    private static final long E1 = 4 * 1000000000L;

    // Obliterator will visit earth in 10 years after game start
    private static final int OBLITERATOR_TURNS = 365 * 10;

    private static final long serialVersionUID = -5249504974370213017L;

    private final int turnObliteratorArrives;

    private long evacuated = 0;

    private long evacuationSpeed;

    private StarSystem targetSystem;

    public EvacuationState(World world) {
        this.turnObliteratorArrives = world.getDayCount() + OBLITERATOR_TURNS;
        findSuitableStarSystem(world.getGalaxyMap(), ((AlienRace) world.getFactions().get("Humanity")).getHomeworld());
    }

    public void update(World world) {
        evacuated += evacuationSpeed;
    }

    public int getTurnObliteratorArrives() {
        return turnObliteratorArrives;
    }

    public long getEvacuated() {
        return evacuated;
    }

    public void changeEvacuationSpeed(int delta) {
        this.evacuationSpeed += delta;
    }

    public long getEvacuationSpeed() {
        return evacuationSpeed;
    }

    public StarSystem getTargetSystem() {
        return targetSystem;
    }

    public boolean isGameOver(World world) {
        //todo: this is for Alpha, obliterator never arrives
        return false;//world.getDayCount() >= turnObliteratorArrives;
    }

    public void showEndGameScreen(World world) {

        StoryScreen ss;

        if (evacuated < E1) {
            ss = new StoryScreen("story/evacuation_ending_bad.json");
        } else {
            ss = new StoryScreen("story/evacuation_ending_normal.json");
        }

        ss.setListener(new IStateChangeListener<World>() {

            private static final long serialVersionUID = 2069156686330555730L;

            @Override
            public void stateChanged(World world) {
                world.setGameOver(true);
            }
        });

        world.addOverlayWindow(ss);
    }

    /**
     * Find a suitable star system for humanity migration
     * It must contain an earth-like planet with breathable atmosphere and life
     */
    private void findSuitableStarSystem(GalaxyMap map, final StarSystem solarSystem) {
        List<StarSystem> suitableStarSystems = new LinkedList<>();
        for (GalaxyMapObject o : map.getGalaxyMapObjects()) {
            if (!(o instanceof StarSystem)) {
                continue;
            }

            if (((StarSystem) o).isQuestLocation()) {
                continue;
            }

            boolean found = false;
            for (BasePlanet p : ((StarSystem) o).getPlanets()) {
                if (!(p instanceof Planet)) {
                    // this star system has special planets in it, so it is already occupied
                    break;
                }
                if (p.getAtmosphere() == PlanetAtmosphere.BREATHABLE_ATMOSPHERE) {
                    found = true;
                    break;
                }
            }

            if (found) {
                suitableStarSystems.add((StarSystem) o);
            }
        }

        if (suitableStarSystems.isEmpty()) {
            throw new IllegalStateException("No suitable star system for humanity evacuation");
        }
        // now sort suitable systems based on their distance to Earth and select the closest one
        Collections.sort(suitableStarSystems, new Comparator<StarSystem>() {
            @Override
            public int compare(StarSystem o1, StarSystem o2) {
                return (int) (GalaxyMap.getDistance(solarSystem, o1) - GalaxyMap.getDistance(solarSystem, o2));
            }
        });

        targetSystem = suitableStarSystems.get(0);
    }
}
