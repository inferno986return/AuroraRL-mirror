/**
 * Created with IntelliJ IDEA.
 * User: Egor.Smirnov
 * Date: 06.12.12
 * Time: 16:15
 */
package ru.game.aurora.player.research;

import ru.game.aurora.application.Configuration;
import ru.game.aurora.application.GameLogger;
import ru.game.aurora.application.Localization;
import ru.game.aurora.player.EarthCountry;
import ru.game.aurora.player.engineering.EngineeringProject;
import ru.game.aurora.player.research.projects.AnimalResearch;
import ru.game.aurora.player.research.projects.AstronomyResearch;
import ru.game.aurora.world.World;
import ru.game.aurora.world.planet.nature.AnimalSpeciesDesc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Contains all data about science and research done by player
 */
public class ResearchState implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<ResearchProjectDesc> completedProjects = new ArrayList<>();
    private final List<ResearchProjectState> currentProjects = new ArrayList<>();
    private final Geodata geodata = new Geodata();
    private int idleScientists;
    private int processedAstroData;

    public ResearchState(int idleScientists) {
        this.idleScientists = idleScientists;
        currentProjects.add(new ResearchProjectState(new AstronomyResearch()));
    }

    public Geodata getGeodata() {
        return geodata;
    }

    public List<ResearchProjectDesc> getCompletedProjects() {
        return completedProjects;
    }

    public List<ResearchProjectState> getCurrentProjects() {
        return currentProjects;
    }

    public int getIdleScientists() {
        return idleScientists;
    }

    public void setIdleScientists(int idleScientists) {
        this.idleScientists = idleScientists;
    }

    public void addNewAvailableProject(ResearchProjectDesc desc) {
        if (desc == null) {
            return;
        }
        this.currentProjects.add(new ResearchProjectState(desc));
        GameLogger.getInstance().logMessage(String.format(Localization.getText("gui", "logging.added_new_research"), desc.getName()));
    }

    /**
     * Called when turn passes.
     * Updates research progress for current projects
     */
    public void update(World world) {
        List<ResearchProjectState> toAdd = new LinkedList<>();
        List<ResearchProjectState> toRemove = new LinkedList<>();
        List<ResearchProjectState> list = new ArrayList<>(currentProjects);
        for (ResearchProjectState state : list) {
            int realScientists = state.scientists;
            if (world.getPlayer().getMainCountry() == EarthCountry.ASIA) {
                // asia has a research bonus
                realScientists = (int) Math.ceil(world.getPlayer().getResearchMultiplier() * realScientists * Configuration.getDoubleProperty("player.asia.researchMultiplier"));
            }
            state.desc.update(world, realScientists);
            if (state.desc.isCompleted()) {
                toRemove.add(state);
                if (!state.desc.isRepeatable()) {
                    completedProjects.add(state.desc);
                } else {
                    toAdd.add(new ResearchProjectState(state.desc));
                }
                idleScientists += state.scientists;
                if (state.desc.getReport() != null) {
                    world.addOverlayWindow(state.desc);
                }
                if (state.desc.getMakesAvailable() != null) {
                    for (String str : state.desc.getMakesAvailable()) {
                        ResearchProjectDesc projectDesc = world.getResearchAndDevelopmentProjects().getResearchProjects().get(str);
                        if (projectDesc == null) {
                            // possibly already researched
                            continue;
                        }
                        toAdd.add(new ResearchProjectState(projectDesc));
                        GameLogger.getInstance().logMessage(String.format(Localization.getText("gui", "logging.added_new_research"), projectDesc.getName()));
                    }
                }
                if (state.desc.getMakesAvailableEngineering() != null) {
                    for (String str : state.desc.getMakesAvailableEngineering()) {
                        EngineeringProject projectDesc = world.getResearchAndDevelopmentProjects().getEngineeringProjects().get(str);
                        if (projectDesc == null) {
                            // possibly it is already done
                            continue;
                        }
                        world.getPlayer().getEngineeringState().addNewEngineeringProject(projectDesc);
                        // engineering projects are removed after they are explored, so that they will not be re-added later
                        world.getResearchAndDevelopmentProjects().getEngineeringProjects().remove(str);
                    }
                }
                GameLogger.getInstance().logMessage(String.format(Localization.getText("gui", "logging.research_project_completed"), state.desc.getName()));
                state.desc.onCompleted(world);
            }
        }
        // to prevent CME
        currentProjects.addAll(toAdd);
        currentProjects.removeAll(toRemove);
    }

    public boolean containsResearchFor(AnimalSpeciesDesc animalSpeciesDesc) {
        for (ResearchProjectState c : currentProjects) {
            if (c.desc instanceof AnimalResearch && ((AnimalResearch) c.desc).getDesc() == animalSpeciesDesc) {
                return true;
            }
        }

        for (ResearchProjectDesc d : completedProjects) {
            if (d instanceof AnimalResearch && ((AnimalResearch) d).getDesc() == animalSpeciesDesc) {
                return true;
            }
        }
        return false;
    }

    public void addProcessedAstroData(int value) {
        processedAstroData += value;
    }

    public int dumpAstroData() {
        int rz = processedAstroData;
        processedAstroData = 0;
        return rz;
    }

    public int getProcessedAstroData() {
        return processedAstroData;
    }
    
    public void addIdleScientists(int amount) {
        idleScientists += amount;
    }
    
    public void removeScientists(int amount) {
        if (idleScientists > 0) {
            int idleToRemove = Math.min(amount, idleScientists);
            idleScientists = (idleScientists - idleToRemove);
            amount -= idleToRemove;
        }

        for (ResearchProjectState rps : currentProjects) {
            if (amount <= 0) {
                break;
            }
            int projectScientistsToRemove = Math.min(amount, rps.scientists);
            rps.scientists -= projectScientistsToRemove;
            amount -= projectScientistsToRemove;
        }
    }
    
    public int getBusyScientists(boolean recalc) {
        if(!recalc) {
            return World.getWorld().getPlayer().getShip().getScientists() - idleScientists;
        }
        
        int busyScientists = 0;
        for(ResearchProjectState project : currentProjects) {
            busyScientists += project.scientists;
        }
        
        return busyScientists;
    }
}
