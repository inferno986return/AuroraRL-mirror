/**
 * User: jedi-philosopher
 * Date: 03.01.13
 * Time: 16:45
 */
package ru.game.aurora.gui;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.CheckBox;
import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.controls.WindowClosedEvent;
import de.lessvoid.nifty.effects.Effect;
import de.lessvoid.nifty.effects.EffectEventId;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import ru.game.aurora.application.GameLogger;
import ru.game.aurora.application.Localization;
import ru.game.aurora.gui.niffy.CustomHint;
import ru.game.aurora.gui.niffy.ImageButtonController;
import ru.game.aurora.gui.niffy.InteractionTargetSelectorController;
import ru.game.aurora.gui.niffy.TopPanelController;
import ru.game.aurora.util.EngineUtils;
import ru.game.aurora.world.*;
import ru.game.aurora.world.equip.WeaponDesc;
import ru.game.aurora.world.planet.BasePlanet;
import ru.game.aurora.world.space.GalaxyMapObject;
import ru.game.aurora.world.space.StarSystem;

import java.util.List;

public class GalaxyMapController extends GameEventListener implements ScreenController, GameLogger.LoggerAppender {

    private static final long serialVersionUID = 6443855197594505098L;

    private World world;


    private transient Screen myScreen;

    private transient ListBox logList;

    private transient TopPanelController topPanelController;


    public GalaxyMapController(World world) {
        this.world = world;
    }

    @Override
    public void bind(Nifty nifty, Screen screen) {
        myScreen = screen;
        GameLogger.getInstance().addAppender(this);

    }

    @Override
    public void onStartScreen() {
        GUI.getInstance().getNifty().setIgnoreKeyboardEvents(true);
        myScreen = GUI.getInstance().getNifty().getCurrentScreen();
        topPanelController = myScreen.findControl("top_panel", TopPanelController.class);
        updateStats();
        updateWeapons();

        logList = myScreen.findNiftyControl("log_list", ListBox.class);
        if (logList != null) {
            logList.clear();
            logList.addAllItems(GameLogger.getInstance().getLogItems());
            logList.setFocusItemByIndex(logList.getItems().size() - 1);
        }
    }

    @Override
    public void onEndScreen() {

    }

    public void openStarMap() {
        GUI.getInstance().pushCurrentScreen();
        GUI.getInstance().getNifty().gotoScreen("star_map_screen");
    }

    public void openResearchScreen() {
        GUI.getInstance().pushCurrentScreen();
        GUI.getInstance().getNifty().gotoScreen("research_screen");
    }

    public void openEngineeringScreen() {
        GUI.getInstance().pushCurrentScreen();
        GUI.getInstance().getNifty().gotoScreen("engineering_screen");
    }

    public void openShipScreen() {
        GUI.getInstance().pushCurrentScreen();
        GUI.getInstance().getNifty().gotoScreen("ship_screen");
    }

    public void openJournal() {
        GUI.getInstance().pushCurrentScreen();
        GUI.getInstance().getNifty().gotoScreen("journal_screen");
    }

    public void openLandingPartyScreen() {
        GUI.getInstance().pushCurrentScreen();
        GUI.getInstance().getNifty().gotoScreen("landing_party_equip_screen");
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public void openMenu() {
        GUI.getInstance().showIngameMenu();
    }

    public void nextTurn() {
        world.setUpdatedNextFrame(true);
    }

    public void updateStats() {

        final Ship ship = world.getPlayer().getShip();
        topPanelController.setProgress(String.format(Localization.getText("gui", "space.hull"), ship.getHull(), ship.getMaxHull()), ship.getHull() / (float) ship.getMaxHull());
        topPanelController.setCrewStats(ship.getScientists(), ship.getEngineers(), ship.getMilitary());

        Element shipCoordinates = myScreen.findElementByName("ship_coordinates");
        if (shipCoordinates != null) {
            String text = String.format(Localization.getText("gui", "space.ship_coords"), ship.getTargetX(), ship.getTargetY());
            GalaxyMapObject mo = world.getGalaxyMap().getObjectAt(ship.getTargetX(), ship.getTargetY());
            if (mo != null) {
                text += "\n" + mo.getName();
            }
            shipCoordinates.getRenderer(TextRenderer.class).setText(text);
        }

        Element ruText = myScreen.findElementByName("resources_text");
        EngineUtils.setTextForGUIElement(ruText, Localization.getText("gui", "resources") + " " + world.getPlayer().getResourceUnits());
        EngineUtils.setTextForGUIElement(myScreen.findElementByName("credits_text"), Localization.getText("gui", "credits") + " " + world.getPlayer().getCredits());

        Element dateText = myScreen.findElementByName("stardate_text");
        EngineUtils.setTextForGUIElement(dateText, world.getCurrentDateString());
    }

    @Override
    public boolean onTurnEnded(World world) {
        if (GUI.getInstance().getNifty().getCurrentScreen().getScreenController().equals(this)) {
            updateStats();
        }
        return false;
    }

    @Override
    public boolean onPlayerShipDamaged(World world) {
        updateStats();
        return false;
    }

    public void updateWeapons() {
        for (int i = 0; i < 4; ++i) {
            final ImageButtonController buttonControl = myScreen.findNiftyControl("weapon_" + i + "_button", ImageButtonController.class);
            if (buttonControl == null) {
                // this is another window controlled by same controller
                return;
            }
            if (i < world.getPlayer().getShip().getWeapons().size()) {
                buttonControl.enable();
                final WeaponDesc weaponDesc = world.getPlayer().getShip().getWeapons().get(i).getWeaponDesc();
                buttonControl.setImage(weaponDesc.getImage());
                // update hint
                List<Effect> effects = buttonControl.getElement().getEffects(EffectEventId.onHover, CustomHint.class);
                final String hintText = String.format(Localization.getText("gui", "space.weapon_tooltip")
                        , Localization.getText("weapons", weaponDesc.getName())
                        , weaponDesc.getDamage()
                        , weaponDesc.getRange()
                        , weaponDesc.getReloadTurns());

                effects.get(0).getParameters().setProperty("hintText", hintText);
                //buttonControl.getElement().getRenderer(ImageRenderer.class).setImage(new NiftyImage(GUI.getInstance().getNifty().getRenderEngine(), new ImageSlickRenderImage(ResourceManager.getInstance().getImage(world.getPlayer().getShip().getWeapons().get(i).getWeaponDesc().image))));
            } else {
                buttonControl.setImage(null);
                buttonControl.disable();
            }
        }
    }

    public void weaponClicked(String weaponIdx) {
        StarSystem ss = world.getCurrentStarSystem();
        if (ss == null) {
            return;
        }

        ss.onWeaponButtonPressed(world, Integer.parseInt(weaponIdx));
    }

    public void nextTargetPressed() {
        world.getCurrentStarSystem().updateShoot(world, true, false, false);
    }

    public void prevTargetPressed() {
        world.getCurrentStarSystem().updateShoot(world, false, true, false);
    }

    public void firePressed() {
        world.getCurrentStarSystem().updateShoot(world, false, false, true);
        cancelPressed(); // close after shooting
    }

    public void cancelPressed() {
        // cancel shoot mode, weapon idx actually means nothing here
        world.getCurrentStarSystem().onWeaponButtonPressed(world, 0);
    }

    @Override
    public void logMessage(String message) {
        if (logList.getItems().size() > GameLogger.MAX_LOG_ENTRIES) {
            logList.removeItemByIndex(0);
        }
        logList.addItem(message);
        logList.setFocusItemByIndex(logList.getItems().size() - 1);
        myScreen.layoutLayers();
    }

    public void closeCurrentPopup() {
        GUI.getInstance().getNifty().setIgnoreKeyboardEvents(true);
        GUI.getInstance().getNifty().closePopup(GUI.getInstance().getNifty().getTopMostPopup().getId());
        world.setPaused(false);
    }

    public void leftButtonPressed() {
        StarSystem currentStarSystem = world.getCurrentStarSystem();
        if (currentStarSystem != null) {
            currentStarSystem.interactWithObjectAtShipPosition(world);
        }
    }

    public void rightButtonPressed() {
        if (world.getCurrentStarSystem() != null) {
            List<GameObject> objects = world.getCurrentStarSystem().getGameObjectsAtPosition(world.getPlayer().getShip());
            if (objects.isEmpty()) {
                return;
            }
            if (objects.size() == 1) {
                if (BasePlanet.class.isAssignableFrom(objects.get(0).getClass())) {
                    scanPlanet((BasePlanet) objects.get(0));
                } else {
                    scanObject(objects.get(0));
                }
                return;
            }

            InteractionTargetSelectorController.open(new IStateChangeListener<GameObject>() {
                private static final long serialVersionUID = -8114467555795780919L;

                @Override
                public void stateChanged(GameObject param) {
                    if (BasePlanet.class.isAssignableFrom(param.getClass())) {
                        scanPlanet((BasePlanet) param);
                    } else {
                        scanObject(param);
                    }
                }
            }, objects);
        }
    }

    private void scanObject(GameObject object) {
        final Nifty nifty = GUI.getInstance().getNifty();
        Element popup = nifty.createPopup("object_scan");
        nifty.showPopup(nifty.getCurrentScreen(), popup.getId(), null);
        GUI.getInstance().getNifty().setIgnoreKeyboardEvents(false);
        world.setPaused(true);
        EngineUtils.setTextForGUIElement(popup.findElementById("scan_text"), object.getScanDescription(world));
    }

    public void scanPlanet(BasePlanet planet) {
        if (planet == null) {
            return;
        }
        GUI.getInstance().pushCurrentScreen();
        PlanetScanController psc = (PlanetScanController) GUI.getInstance().getNifty().findScreenController(PlanetScanController.class.getCanonicalName());
        psc.setPlanetToScan(planet);
        GUI.getInstance().getNifty().gotoScreen("planet_scan_screen");
    }

    public void enterStarsystem() {
        if (world.getCurrentStarSystem() != null) {
            return;
        }

        world.getGalaxyMap().enterStarsystemAtPlayerCoordinates();
    }

    @NiftyEventSubscriber(id = "help_window")
    public void onHelpClose(final String id, final WindowClosedEvent event) {
        if (GUI.getInstance().getNifty().getCurrentScreen().findControl("help_popup", HelpPopupControl.class).isHelpSkipChecked()) {
            GUI.getInstance().getWorldInstance().getGlobalVariables().put("skipHelp", true);
        }
    }

}
