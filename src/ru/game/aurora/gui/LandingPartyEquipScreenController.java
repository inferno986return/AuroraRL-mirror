/**
 * Created with IntelliJ IDEA.
 * User: User
 * Date: 07.07.13
 * Time: 20:04
 */

package ru.game.aurora.gui;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.DropDown;
import de.lessvoid.nifty.controls.DropDownSelectionChangedEvent;
import de.lessvoid.nifty.controls.Scrollbar;
import de.lessvoid.nifty.controls.ScrollbarChangedEvent;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import ru.game.aurora.application.ResourceManager;
import ru.game.aurora.util.EngineUtils;
import ru.game.aurora.world.World;
import ru.game.aurora.world.equip.LandingPartyWeapon;
import ru.game.aurora.world.planet.InventoryItem;
import ru.game.aurora.world.planet.LandingParty;


public class LandingPartyEquipScreenController implements ScreenController {
    private World world;

    private LandingParty landingParty;

    private Screen myScreen;

    public LandingPartyEquipScreenController(World world) {
        this.world = world;
        landingParty = world.getPlayer().getLandingParty();
        if (landingParty == null) {
            landingParty = new LandingParty(0, 0, ResourceManager.getInstance().getLandingPartyWeapons().getEntity("assault"), 1, 1, 1);
            world.getPlayer().setLandingParty(landingParty);
        }
    }

    @Override
    public void bind(Nifty nifty, Screen screen) {
        myScreen = screen;
    }

    @Override
    public void onStartScreen() {
        Scrollbar scrollbar = myScreen.findNiftyControl("scientists_count", Scrollbar.class);
        scrollbar.setValue(landingParty.getScience());
        scrollbar = myScreen.findNiftyControl("engineers_count", Scrollbar.class);
        scrollbar.setValue(landingParty.getEngineers());
        scrollbar = myScreen.findNiftyControl("military_count", Scrollbar.class);
        scrollbar.setValue(landingParty.getMilitary());

        DropDown weaponSelect = myScreen.findNiftyControl("weapon_select", DropDown.class);
        for (InventoryItem item : world.getPlayer().getInventory().keySet()) {
            if (item instanceof LandingPartyWeapon) {
                weaponSelect.addItem(item);
            }
        }

        myScreen.findElementByName("scientists_count_text").getRenderer(TextRenderer.class).setText("Scientists: " + landingParty.getScience());
        myScreen.findElementByName("engineers_count_text").getRenderer(TextRenderer.class).setText("Engineers: " + landingParty.getEngineers());
        myScreen.findElementByName("military_count_text").getRenderer(TextRenderer.class).setText("Military: " + landingParty.getMilitary());
        myScreen.findElementByName("total_count").getRenderer(TextRenderer.class).setText("Total: " + landingParty.getTotalMembers() + " / 10");

    }

    @Override
    public void onEndScreen() {
    }

    @NiftyEventSubscriber(id = "weapon_select")
    public void onWeaponSelected(final String id, final DropDownSelectionChangedEvent event)
    {
        if (event.getSelection() == null) {
            return;
        }
        final LandingPartyWeapon weapon = (LandingPartyWeapon) event.getSelection();
        landingParty.setWeapon(weapon);

        EngineUtils.setImageForGUIElement(myScreen.findElementByName("selected_weapon_img"), weapon.getImage());
        myScreen.findElementByName("selected_weapon_text").getRenderer(TextRenderer.class).setText(weapon.getName());
    }

    @NiftyEventSubscriber(pattern = ".*_count")
    public void onScrollbarMoved(final String id, final ScrollbarChangedEvent event) {
        String scrollbarId = event.getScrollbar().getId();
        switch (scrollbarId) {
            case "scientists_count": {
                int oldVal = landingParty.getScience();
                landingParty.setScience((int) event.getValue());
                if (landingParty.getTotalMembers() > 10) {
                    event.getScrollbar().setValue(oldVal);
                    landingParty.setScience(oldVal);
                }
                myScreen.findElementByName("scientists_count_text").getRenderer(TextRenderer.class).setText("Scientists: " + landingParty.getScience());
                break;
            }
            case "engineers_count": {
                int oldVal = landingParty.getEngineers();
                landingParty.setEngineers((int) event.getValue());
                if (landingParty.getTotalMembers() > 10) {
                    event.getScrollbar().setValue(oldVal);
                    landingParty.setEngineers(oldVal);
                }
                myScreen.findElementByName("engineers_count_text").getRenderer(TextRenderer.class).setText("Engineers: " + landingParty.getEngineers());
                break;
            }
            default:
                int oldVal = landingParty.getMilitary();
                landingParty.setMilitary((int) event.getValue());
                if (landingParty.getTotalMembers() > 10) {
                    event.getScrollbar().setValue(oldVal);
                    landingParty.setMilitary(oldVal);
                }
                myScreen.findElementByName("military_count_text").getRenderer(TextRenderer.class).setText("Military: " + landingParty.getMilitary());
                break;
        }

        myScreen.findElementByName("total_count").getRenderer(TextRenderer.class).setText("Total: " + landingParty.getTotalMembers() + " / 10");
    }

    public void closeScreen() {
        GUI.getInstance().popAndSetScreen();
    }
}
