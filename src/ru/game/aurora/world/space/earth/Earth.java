/**
 * Created with IntelliJ IDEA.
 * User: Egor.Smirnov
 * Date: 20.12.12
 * Time: 14:22
 */
package ru.game.aurora.world.space.earth;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import ru.game.aurora.application.Camera;
import ru.game.aurora.application.ResourceManager;
import ru.game.aurora.npc.Dialog;
import ru.game.aurora.player.research.ResearchProjectDesc;
import ru.game.aurora.player.research.ResearchState;
import ru.game.aurora.world.World;
import ru.game.aurora.world.planet.Planet;
import ru.game.aurora.world.planet.PlanetAtmosphere;
import ru.game.aurora.world.planet.PlanetCategory;
import ru.game.aurora.world.space.StarSystem;

public class Earth extends Planet {

    private Dialog earthDialog;

    private Dialog progressDialog;

    private int lastVisitTurn = 0;

    public Earth(StarSystem owner, PlanetCategory cat, PlanetAtmosphere atmosphere, int size, int x, int y, boolean hasLife) {
        super(owner, cat, atmosphere, size, x, y, hasLife);
        earthDialog = Dialog.loadFromFile(Earth.class.getClassLoader().getResourceAsStream("dialogs/earth_dialog.json"));
        progressDialog = Dialog.loadFromFile(Earth.class.getClassLoader().getResourceAsStream("dialogs/earth_progress_dialog.json"));
    }

    @Override
    public void drawOnGlobalMap(GameContainer container, Graphics g, Camera camera, int tileX, int tileY) {
        if (!camera.isInViewport(globalX, globalY)) {
            return;
        }

        g.drawImage(ResourceManager.getInstance().getImage("earth"), camera.getXCoord(globalX), camera.getYCoord(globalY));
    }

    @Override
    public boolean canBeEntered() {
        return true;
    }

    @Override
    public void enter(World world) {
        world.setCurrentDialog(earthDialog);
    }

    @Override
    public void update(GameContainer container, World world) {
        if (earthDialog.isOver()) {
            if (earthDialog.getReturnValue() == 1) {
                // player has chosen to dump research info

                int daysPassed = world.getTurnCount() - lastVisitTurn;
                Dialog.Statement stmt;

                if (daysPassed > 50) {
                    int totalScore = dumpResearch(world);
                    double scorePerTurn = (double) totalScore / (daysPassed);
                    stmt = new Dialog.Statement(0, String.format("Let us see. You have brought us new %d points of data, giving %f points/day", totalScore, scorePerTurn), new Dialog.Reply(0, 0, ""));

                    if (scorePerTurn < 0.001) {
                        // unsatisfactory
                        stmt.replies[0] = new Dialog.Reply(0, 3, "=continue=");
                    } else if (scorePerTurn < 0.01) {
                        // poor
                        stmt.replies[0] = new Dialog.Reply(0, 2, "=continue=");
                    } else {
                        // ok
                        stmt.replies[0] = new Dialog.Reply(0, 1, "=continue=");
                    }
                    lastVisitTurn = world.getTurnCount();
                } else {
                    stmt = new Dialog.Statement(0, "We are pleased to see you come back, but your flight was too short to judge your perfomance. Come back later after you have acquired more data", new Dialog.Reply(0, -1, "Ok"));
                }
                progressDialog.putStatement(stmt);
                world.setCurrentDialog(progressDialog);
            }
            // just reset state
            earthDialog.enter(world);
        }
        if (progressDialog.isOver()) {
            if (progressDialog.getReturnValue() == -1) {
                container.exit();
            }
            world.setCurrentRoom(owner);
        }

    }

    @Override
    public void draw(GameContainer container, Graphics g, Camera camera) {
        owner.draw(container, g, camera);
    }

    private int dumpResearch(World world) {
        int result = 0;
        final ResearchState researchState = world.getPlayer().getResearchState();
        result += researchState.getGeodata().dumpAndGetVictoryPoints();
        for (ResearchProjectDesc desc : researchState.getCompletedProjects()) {
            result += desc.getScore();
        }
        researchState.getCompletedProjects().clear();
        result += researchState.dumpAstroData();
        return result;
    }
}
