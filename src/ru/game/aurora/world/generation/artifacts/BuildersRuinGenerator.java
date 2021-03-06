/**
 * User: jedi-philosopher
 * Date: 10.02.13
 * Time: 18:11
 */
package ru.game.aurora.world.generation.artifacts;

import ru.game.aurora.application.GlobalThreadPool;
import ru.game.aurora.player.research.ResearchReport;
import ru.game.aurora.player.research.projects.ArtifactResearch;
import ru.game.aurora.util.CollectionUtils;
import ru.game.aurora.world.World;
import ru.game.aurora.world.generation.WorldGeneratorPart;
import ru.game.aurora.world.planet.AlienArtifact;
import ru.game.aurora.world.planet.BasePlanet;
import ru.game.aurora.world.planet.Planet;
import ru.game.aurora.world.space.GalaxyMapObject;
import ru.game.aurora.world.space.StarSystem;

import java.util.HashSet;
import java.util.Set;

public class BuildersRuinGenerator implements WorldGeneratorPart {
    private static final int SYSTEMS = 5;

    private static final long serialVersionUID = 1848518979450823837L;

    private void updateStarSystem(StarSystem ss) {
        for (BasePlanet p : ss.getPlanets()) {
            // add random artifact on every planet
            if (p instanceof Planet) {
                AlienArtifact artifact = new AlienArtifact(15, 15, "builders_ruins", new ArtifactResearch(new ResearchReport("builders_ruins", "builder_ruins.report")));

                ((Planet) p).setNearestFreePoint(artifact, 15, 15);
                ((Planet) p).getPlanetObjects().add(artifact);
                break;
            }
        }
    }

    @Override
    public void updateWorld(World world) {
        // select 10 random star systems
        Set<GalaxyMapObject> updatedSystems = new HashSet<>();
        for (int i = 0; i < Math.min(SYSTEMS, world.getGalaxyMap().getGalaxyMapObjects().size()); ++i) {
            final GalaxyMapObject obj = CollectionUtils.selectRandomElement(world.getGalaxyMap().getGalaxyMapObjects());
            if (StarSystem.class.isAssignableFrom(obj.getClass()) && !updatedSystems.contains(obj)) {
                GlobalThreadPool.getExecutor().submit(new Runnable() {
                    @Override
                    public void run() {
                        updateStarSystem((StarSystem) obj);
                    }
                });
                updatedSystems.add(obj);
            }
        }

        while (GlobalThreadPool.getExecutor().getQueue().size() + GlobalThreadPool.getExecutor().getActiveCount() > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {

            }
        }
    }
}
