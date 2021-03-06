/**
 * Created with IntelliJ IDEA.
 * User: Egor.Smirnov
 * Date: 18.01.13
 * Time: 16:24
 */
package ru.game.aurora.world.quest;


import ru.game.aurora.application.GameLogger;
import ru.game.aurora.application.Localization;
import ru.game.aurora.dialog.Dialog;
import ru.game.aurora.world.GameObject;
import ru.game.aurora.world.World;
import ru.game.aurora.world.space.NPCShip;
import ru.game.aurora.world.space.StarSystem;

/**
 * Special quest probe. Appears in fourth visited system. Contains all astrodata for that system as well as records from Aurora
 * about their course and intentions.
 */
public class AuroraProbe extends NPCShip {
    private static final long serialVersionUID = -579871001131587178L;

    private final Dialog dialog;

    public AuroraProbe(int x, int y) {
        super("probe", x, y);
        setFaction(null);
        setAi(null);
        dialog = Dialog.loadFromFile("dialogs/quest/aurora_probe_1_found.json");
    }

    @Override
    public boolean isHostile(World world, GameObject object) {
        return false;
    }

    @Override
    public boolean interact(World world) {
        world.addOverlayWindow(dialog);
        isAlive = false;

        world.getPlayer().getResearchState().addNewAvailableProject(world.getResearchAndDevelopmentProjects().getResearchProjects().get("probe"));

        StarSystem curSystem = (StarSystem) world.getCurrentRoom();
        int remainingAstroData = curSystem.getAstronomyData();
        GameLogger.getInstance().logMessage(String.format(Localization.getText("research", "probe.astro_data_retrieved"), curSystem.getAstronomyData()));
        curSystem.setAstronomyData(0);
        world.getPlayer().getResearchState().addProcessedAstroData(remainingAstroData);
        
        return true;
    }

    @Override
    public String getScanDescription(World world) {
        return Localization.getText("research", "probe.scan_description");
    }
}
