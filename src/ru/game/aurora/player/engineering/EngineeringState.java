/**
 * Created with IntelliJ IDEA.
 * User: Egor.Smirnov
 * Date: 29.01.13
 * Time: 16:13
 */
package ru.game.aurora.player.engineering;


import ru.game.aurora.application.GameLogger;
import ru.game.aurora.application.Localization;
import ru.game.aurora.world.World;

import java.io.Serializable;
import java.util.*;

public class EngineeringState implements Serializable {
    private static final long serialVersionUID = 1L;

    private int idleEngineers;

    private final HullRepairs hullRepairs;

    private final List<EngineeringProject> projects = new LinkedList<>();

    private final Set<String> completedProjectNames = new HashSet<>();

    public EngineeringState(int idleEngineers) {
        this.idleEngineers = idleEngineers;
        hullRepairs = new HullRepairs();
    }

    public void update(World world) {
        hullRepairs.update(world);

        for (Iterator<EngineeringProject> iter = projects.iterator(); iter.hasNext(); ) {
            EngineeringProject ep = iter.next();
            if (!ep.update(world)) {
                if (!ep.isRepeatable()) {
                    iter.remove();
                }

            }
        }
    }

    public HullRepairs getHullRepairs() {
        return hullRepairs;
    }

    public int getIdleEngineers() {
        return idleEngineers;
    }

    public void setIdleEngineers(int idleEngineers) {
        this.idleEngineers = idleEngineers;
    }

    public void addIdleEngineers(int amount) {
        idleEngineers += amount;
    }

    public List<EngineeringProject> getProjects() {
        return projects;
    }

    public void addNewEngineeringProject(EngineeringProject project) {
        if (!completedProjectNames.contains(project.getId())) {
            projects.add(project);
            GameLogger.getInstance().logMessage(String.format(Localization.getText("gui", "logging.new_engineering_added"), project.getLocalizedName("engineering")));
            // every project can be added only once
            completedProjectNames.add(project.getId());
        }
    }
    
    public void removeEngineers(int amount) {
        if (idleEngineers > 0) {
            int idleToRemove = Math.min(amount, idleEngineers);
            idleEngineers = idleEngineers - idleToRemove;
            amount -= idleToRemove;
        }

        for (EngineeringProject epr : projects) {
            if (amount <= 0) {
                break;
            }
            int projectScientistsToRemove = Math.min(amount, epr.getEngineersAssigned());
            epr.changeEngineers(-projectScientistsToRemove, World.getWorld());
            amount -= projectScientistsToRemove;
        }
        
        if(amount > 0 && hullRepairs.engineersAssigned > 0) {
            int toRemove = Math.min(amount, hullRepairs.engineersAssigned);
            hullRepairs.engineersAssigned -= Math.min(amount, toRemove);
            amount -= toRemove;
        }
    }
    
    public int getBusyEngineers(boolean recalc) {
        if(!recalc) {
            return World.getWorld().getPlayer().getShip().getEngineers() - idleEngineers;
        }
        
        int busyEngineers = 0;
        for(EngineeringProject project : projects) {
            busyEngineers += project.getEngineersAssigned();
        }
        
        return busyEngineers + hullRepairs.engineersAssigned;
    }
}
