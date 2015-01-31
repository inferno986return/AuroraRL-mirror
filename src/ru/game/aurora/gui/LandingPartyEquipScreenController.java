/**
 * Created with IntelliJ IDEA.
 * User: User
 * Date: 07.07.13
 * Time: 20:04
 */

package ru.game.aurora.gui;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.*;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import de.lessvoid.nifty.tools.Color;
import ru.game.aurora.application.Configuration;
import ru.game.aurora.application.Localization;
import ru.game.aurora.application.ResourceManager;
import ru.game.aurora.util.EngineUtils;
import ru.game.aurora.world.Ship;
import ru.game.aurora.world.World;
import ru.game.aurora.world.equip.WeaponDesc;
import ru.game.aurora.world.planet.InventoryItem;
import ru.game.aurora.world.planet.LandingParty;


public class LandingPartyEquipScreenController implements ScreenController {

    // used in rendering warning text
    private final Color redColor = new Color(200, 0, 0, 255);

    private final World world;

    // copy of a players landing party
    // if player closes the screen using 'ok' button, original landing party is replaced by this object,
    // otherwise it is discarded
    private LandingParty localLandingParty;

    private Screen myScreen;

    private Element myWindow;

    private Element weightText;

    private Element statusText;

    private ListBox<Multiset.Entry<InventoryItem>> storageList;

    private ListBox<Multiset.Entry<InventoryItem>> inventoryList;

    private Multiset<InventoryItem> shipStorage;

    public LandingPartyEquipScreenController(World world) {
        this.world = world;
    }

    @Override
    public void bind(Nifty nifty, Screen screen) {
        myScreen = screen;
        myWindow = myScreen.findElementByName("equip_window");
        weightText = myScreen.findElementByName("weight_text");
        statusText = myScreen.findElementByName("status_text");
        storageList = screen.findNiftyControl("storageList", ListBox.class);
        inventoryList = screen.findNiftyControl("inventoryList", ListBox.class);
    }

    @Override
    public void onStartScreen() {
        final LandingParty worldLandingParty = world.getPlayer().getLandingParty();
        if (worldLandingParty == null) {
            localLandingParty = new LandingParty(0, 0, ResourceManager.getInstance().getWeapons().getEntity("assault"), 1, 1, 1, Configuration.getIntProperty("player.landing_party.defaultHP"));
            myScreen.findNiftyControl("cancel_button", Button.class).disable();
        } else {
            localLandingParty = new LandingParty(worldLandingParty);
            if (localLandingParty.canBeLaunched(world)) {
                myScreen.findNiftyControl("cancel_button", Button.class).enable();
            } else {
                myScreen.findNiftyControl("cancel_button", Button.class).disable();
            }
        }

        shipStorage = HashMultiset.create(world.getPlayer().getInventory());

        myWindow.setVisible(true);
        world.setPaused(true);
        Scrollbar scrollbar = myScreen.findNiftyControl("scientists_count", Scrollbar.class);
        scrollbar.setValue(localLandingParty.getScience());
        scrollbar = myScreen.findNiftyControl("engineers_count", Scrollbar.class);
        scrollbar.setValue(localLandingParty.getEngineers());
        scrollbar = myScreen.findNiftyControl("military_count", Scrollbar.class);
        scrollbar.setValue(localLandingParty.getMilitary());

        DropDown weaponSelect = myScreen.findNiftyControl("weapon_select", DropDown.class);
        weaponSelect.clear();
        for (InventoryItem item : world.getPlayer().getInventory().elementSet()) {
            if (item instanceof WeaponDesc) {
                weaponSelect.addItem(item);
            }
        }

        updateLabels();
        refreshLists();
    }

    private void updateLabels() {
        myScreen.findElementByName("scientists_count_text").getRenderer(TextRenderer.class).setText(Localization.getText("gui", "landing_party.scientists") + " " + localLandingParty.getScience());
        myScreen.findElementByName("engineers_count_text").getRenderer(TextRenderer.class).setText(Localization.getText("gui", "landing_party.engineers") + " " + localLandingParty.getEngineers());
        myScreen.findElementByName("military_count_text").getRenderer(TextRenderer.class).setText(Localization.getText("gui", "landing_party.military") + " " + localLandingParty.getMilitary());
        myScreen.findElementByName("total_count").getRenderer(TextRenderer.class).setText(Localization.getText("gui", "landing_party.total") + " " + localLandingParty.getTotalMembers() + " / 10");

        EngineUtils.setTextForGUIElement(weightText, String.format(Localization.getText("gui", "landing_party.weight"), localLandingParty.getInventoryWeight(), localLandingParty.getMaxWeight()));
    }

    @Override
    public void onEndScreen() {
        world.setPaused(false);
    }

    @NiftyEventSubscriber(id = "weapon_select")
    public void onWeaponSelected(final String id, final DropDownSelectionChangedEvent event) {
        if (event.getSelection() == null) {
            return;
        }
        final WeaponDesc weapon = (WeaponDesc) event.getSelection();
        localLandingParty.setWeapon(weapon);

        EngineUtils.setImageForGUIElement(myScreen.findElementByName("selected_weapon_img"), weapon.getImage());
        myScreen.findElementByName("selected_weapon_text").getRenderer(TextRenderer.class).setText(weapon.getName());
    }

    @NiftyEventSubscriber(pattern = ".*_count")
    public void onScrollbarMoved(final String id, final ScrollbarChangedEvent event) {
        String scrollbarId = event.getScrollbar().getId();
        final Ship ship = world.getPlayer().getShip();
        switch (scrollbarId) {
            case "scientists_count": {
                int oldVal = localLandingParty.getScience();
                localLandingParty.setScience((int) event.getValue());
                final int idleScientists = world.getPlayer().getResearchState().getIdleScientists();
                if (localLandingParty.getTotalMembers() > 10 || localLandingParty.getScience() > idleScientists) {
                    if (oldVal > idleScientists) {
                        oldVal = 0;
                    }
                    event.getScrollbar().setValue(oldVal);
                    localLandingParty.setScience(oldVal);
                }
                break;
            }
            case "engineers_count": {
                int oldVal = localLandingParty.getEngineers();
                localLandingParty.setEngineers((int) event.getValue());
                final int idleEngineers = world.getPlayer().getEngineeringState().getIdleEngineers();
                if (localLandingParty.getTotalMembers() > 10 || localLandingParty.getEngineers() > idleEngineers) {
                    if (oldVal > idleEngineers) {
                        oldVal = 0;
                    }
                    event.getScrollbar().setValue(oldVal);
                    localLandingParty.setEngineers(oldVal);
                }
                break;
            }
            default:
                int oldVal = localLandingParty.getMilitary();
                localLandingParty.setMilitary((int) event.getValue());
                if (localLandingParty.getTotalMembers() > 10 || localLandingParty.getMilitary() > ship.getMilitary()) {
                    if (oldVal > ship.getMilitary()) {
                        oldVal = 0;
                    }
                    event.getScrollbar().setValue(oldVal);
                    localLandingParty.setMilitary(oldVal);
                }
                break;
        }

        if (!localLandingParty.canBeLaunched(world)) {
            // disable close buttons, because current party configuration is invalid
            myScreen.findElementByName("ok_button").disable();
            statusText.getRenderer(TextRenderer.class).setColor(redColor);
            EngineUtils.setTextForGUIElement(statusText, Localization.getText("gui", "landing_party.can_not_launch"));
        } else {
            myScreen.findElementByName("ok_button").enable();
            statusText.getRenderer(TextRenderer.class).setColor(new Color("#3C2C41"));
            EngineUtils.setTextForGUIElement(statusText, Localization.getText("gui", "landing_party.can_launch"));
        }
        updateLabels();
    }

    public void ok() {
        GUI.getInstance().popAndSetScreen();
        world.getPlayer().setLandingParty(localLandingParty);
        world.getPlayer().getInventory().clear();
        world.getPlayer().getInventory().addAll(shipStorage);
    }

    public void cancel() {
        GUI.getInstance().popAndSetScreen();
        localLandingParty = null;
    }

    public void onStorageToInventoryClicked() {
        final Multiset.Entry<InventoryItem> inventoryItemEntry = storageList.getSelection().get(0);
        final InventoryItem element = inventoryItemEntry.getElement();
        localLandingParty.pickUp(element, 1);
        shipStorage.remove(element);
        if (inventoryItemEntry.getCount() == 0) {
            storageList.removeItem(inventoryItemEntry);
        }
        refreshLists();
    }

    public void onInventoryToStorageClicked() {
        final Multiset.Entry<InventoryItem> inventoryItemEntry = inventoryList.getSelection().get(0);
        final InventoryItem element = inventoryItemEntry.getElement();
        shipStorage.add(element);
        localLandingParty.getInventory().setCount(element, inventoryItemEntry.getCount() - 1);
        if (inventoryItemEntry.getCount() == 0) {
            inventoryList.removeItem(inventoryItemEntry);
        }
        refreshLists();
    }

    private void refreshLists() {
        storageList.clear();
        for (Multiset.Entry<InventoryItem> entry : shipStorage.entrySet()) {
            storageList.addItem(entry);
        }
        inventoryList.clear();
        for (Multiset.Entry<InventoryItem> entry : localLandingParty.getInventory().entrySet()) {
            inventoryList.addItem(entry);
        }
        updateLabels();
    }

    @NiftyEventSubscriber(pattern = ".*storage_to_inventory")
    public void onReleased(String id, ButtonClickedEvent event) {
        storageList.selectItem((Multiset.Entry<InventoryItem>) event.getButton().getElement().getParent().getUserData());
    }

    //костыль к костылю. YO DAWG
    @NiftyEventSubscriber(pattern = ".*inventory_to_storage")
    public void onPrimaryReleased(String id, ButtonClickedEvent event) {
        inventoryList.selectItem((Multiset.Entry<InventoryItem>) event.getButton().getElement().getParent().getUserData());
    }
}
