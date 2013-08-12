/**
 * User: jedi-philosopher
 * Date: 08.12.12
 * Time: 14:50
 */
package ru.game.aurora.player.research.projects;

import ru.game.aurora.player.research.ResearchProjectDesc;
import ru.game.aurora.world.World;
import ru.game.aurora.world.planet.nature.AnimalSpeciesDesc;

public class AnimalResearch extends ResearchProjectDesc {
    private AnimalSpeciesDesc desc;

    private int progress = 100;

    public AnimalResearch(AnimalSpeciesDesc desc) {
        super("Biology research for '" + desc.getName() + "'", "A set of tasks for researching alien animal. Autopsy, chemical and biological tests etc. ", "autopsy_research");
        this.desc = desc;
    }

    @Override
    public void update(World world, int scientists) {
        progress -= scientists;
    }

    @Override
    public String getStatusString(World world, int scientists) {
        if (progress > 60) {
            return "Poor";
        }

        if (progress > 30) {
            return "Good";
        }

        if (progress > 0) {
            return "Almost done";
        }

        return "Completed";
    }

    @Override
    public boolean isCompleted() {
        return progress <= 0;
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public int getScore() {
        return 50;
    }

    public AnimalSpeciesDesc getDesc() {
        return desc;
    }
}
