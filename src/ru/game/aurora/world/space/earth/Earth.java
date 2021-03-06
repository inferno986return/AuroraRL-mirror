/**
 * Created with IntelliJ IDEA.
 * User: Egor.Smirnov
 * Date: 20.12.12
 * Time: 14:22
 */
package ru.game.aurora.world.space.earth;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import ru.game.aurora.application.*;
import ru.game.aurora.dialog.Dialog;
import ru.game.aurora.dialog.DialogListener;
import ru.game.aurora.gui.StoryScreen;
import ru.game.aurora.player.earth.EarthResearch;
import ru.game.aurora.player.earth.PrivateMessage;
import ru.game.aurora.player.research.ResearchProjectDesc;
import ru.game.aurora.player.research.ResearchState;
import ru.game.aurora.world.World;
import ru.game.aurora.world.planet.BasePlanet;
import ru.game.aurora.world.planet.PlanetAtmosphere;
import ru.game.aurora.world.planet.PlanetCategory;
import ru.game.aurora.world.planet.PlanetSpriteGenerator;
import ru.game.aurora.world.space.StarSystem;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class Earth extends BasePlanet {

    private static final long serialVersionUID = 1L;

    private final Dialog earthDialog;

    private final Dialog progressDialog;
    // flags used to control default earth dialog
    private final Map<String, String> dialogFlags = new HashMap<>();
    private int lastVisitTurn = 0;
    private transient Image earthImage = null;

    public Earth(StarSystem owner, PlanetCategory cat, PlanetAtmosphere atmosphere, int size, int x, int y) {
        super(x, y, size, owner, atmosphere, cat);
        earthDialog = Dialog.loadFromFile("dialogs/earth_dialog.json");
        progressDialog = Dialog.loadFromFile("dialogs/earth_progress_dialog.json");
        progressDialog.addListener(new EarthProgressDialogListener(this));
        earthDialog.addListener(new EarthDialogListener(this));
    }

    public Dialog getProgressDialog() {
        return progressDialog;
    }

    public Map<String, String> getDialogFlags() {
        return dialogFlags;
    }

    @Override
    public void drawOnGlobalMap(GameContainer container, Graphics g, Camera camera, int tileX, int tileY) {
        if ((earthImage == null) || x != oldX || y != oldY) {
            earthImage = ResourceManager.getInstance().getImage("earth");
            double theta = Math.atan2(y, x);
            float shadowXFactor = (float) (0.5 - Math.cos(theta) * 0.25);
            float shadowYFactor = (float) (0.5 - Math.sin(theta) * 0.25);
            earthImage = PlanetSpriteGenerator.shadowPlanet(earthImage, shadowXFactor, shadowYFactor);
            oldX = x;
            oldY = y;
        }
        g.drawImage(earthImage, camera.getXCoord(x) - earthImage.getWidth() / 4, camera.getYCoord(y) - earthImage.getHeight() / 4);
    }

    @Override
    public boolean canBeEntered() {
        return true;
    }

    public Dialog getEarthDialog() {
        return earthDialog;
    }

    @Override
    public void enter(World world) {
        final Queue<Dialog> earthSpecialDialogs = world.getPlayer().getEarthState().getEarthSpecialDialogs();

        if (!earthSpecialDialogs.isEmpty()) {
            Dialog specialDialog = earthSpecialDialogs.remove();
            world.setCurrentRoom(owner);
            world.addOverlayWindow(specialDialog);
            //todo: maybe not return? what if both variables are set?
            return;
        }
        if (world.getGlobalVariables().containsKey("quest.main.show_earth_dialog")) {
            showObliteratorThreatDialog(world);
        } else {
            addQuestFlags(world, dialogFlags);
            world.addOverlayWindow(earthDialog, dialogFlags);
        }
    }

    @Override
    public void returnTo(World world) {
        enter(world);
    }

    private void addQuestFlags(World world, Map<String, String> flags) {
        // check for embassies quest
        if (!world.getGlobalVariables().containsKey("diplomacy.all_done")
            && world.getGlobalVariables().containsKey("diplomacy.klisk_visited")
            && world.getGlobalVariables().containsKey("diplomacy.bork_visited")
            && world.getGlobalVariables().containsKey("diplomacy.zorsan_visited")
            && world.getGlobalVariables().containsKey("diplomacy.rogues_visited")
            && world.getGlobalVariables().containsKey("klisk_trade.quest_result")
            && world.getGlobalVariables().containsKey("bork.diplomacy_test"))
        {
            flags.put("diplomacy.all_done", "0");
        }
    }

    private void showObliteratorThreatDialog(World world) {
        world.addOverlayWindow(Dialog.loadFromFile("dialogs/quest/main/earth_obliterator_warning_1.json"));
        world.addOverlayWindow(new StoryScreen("story/obliterator.json"));
        Dialog last = Dialog.loadFromFile("dialogs/quest/main/earth_obliterator_warning_2.json");
        last.addListener(new DialogListener() {

            private static final long serialVersionUID = -374777902752182404L;

            @Override
            public void onDialogEnded(World world, Dialog dialog, int returnCode, Map<String, String> flags) {
                world.getGlobalVariables().remove("quest.main.show_earth_dialog");
            }
        });
        world.addOverlayWindow(last);
        // starting slow evacuation
        world.getPlayer().getEarthState().getEvacuationState().changeEvacuationSpeed(10);

        // after some time evacuation will go faster
        world.addListener(new EarthResearch("evac_announce", 50) {

            private static final long serialVersionUID = -3699793069471281672L;

            @Override
            protected void onCompleted(World world) {
                world.getPlayer().getEarthState().getEvacuationState().changeEvacuationSpeed(100);
                world.getPlayer().getEarthState().getMessages().add(new PrivateMessage(
                        world,
                        "news_sender",
                        "evac_start",
                        "news"
                ));

                world.getPlayer().getEarthState().getMessages().add(new PrivateMessage(
                        world,
                        "evac_start_2",
                        "message"
                ));
            }
        });

        world.getGlobalVariables().put("quest.main.evacuation_started", null);
    }

    @Override
    public void update(GameContainer container, World world) {

    }

    @Override
    public void draw(GameContainer container, Graphics g, Camera camera, World world) {
        owner.draw(container, g, camera, world);
    }

    public int getLastVisitTurn() {
        return lastVisitTurn;
    }

    public void setLastVisitTurn(int lastVisitTurn) {
        this.lastVisitTurn = lastVisitTurn;
    }

    int dumpResearch(World world) {
        world.onPlayerReturnToEarth();
        int result = 0;
        final ResearchState researchState = world.getPlayer().getResearchState();
        result += researchState.getGeodata().dumpAndGetVictoryPoints(world);
        for (ResearchProjectDesc desc : researchState.getCompletedProjects()) {
            result += desc.getScore();
            if (desc.getEarthProgress() != null) {
                for (String resId : desc.getEarthProgress()) {
                    EarthResearch res = world.getResearchAndDevelopmentProjects().getEarthResearchProjects().get(resId);
                    world.addListener(res);
                    res.onStarted(world);
                }
            }
        }
        researchState.getCompletedProjects().clear();
        result += researchState.dumpAstroData();
        world.getPlayer().getEarthState().updateTechnologyLevel(result);
        // subtracting points for loosing crew members
        result += Configuration.getIntProperty("game.progress.lostCrewScore") * (world.getPlayer().getShip().getLostCrewMembers());
        return result;
    }

    @Override
    public boolean canBeLanded() {
        return false;
    }

    @Override
    public String getScanText() {
        return Localization.getText("races", "Humans.homeworld.description");
    }

    @Override
    public String getName() {
        return "Earth";
    }

    @Override
    public Image getImage() {
        return earthImage;
    }
    
    @Override
    public boolean canBeCommunicated() {
        return true;
    }
}
