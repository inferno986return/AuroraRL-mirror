package ru.game.aurora.gui;

import com.google.common.collect.Multiset;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.ButtonClickedEvent;
import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.controls.WindowClosedEvent;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import ru.game.aurora.application.Localization;
import ru.game.aurora.util.EngineUtils;
import ru.game.aurora.world.World;
import ru.game.aurora.world.planet.InventoryItem;
import ru.game.aurora.world.planet.LandingParty;
import ru.game.aurora.world.planet.UsableItem;

/**
 * Created with IntelliJ IDEA.
 * User: User
 * Date: 17.12.13
 * Time: 22:27
 */
public class InventoryController implements ScreenController {
    private Element myWindow;

    private ListBox<Multiset.Entry<InventoryItem>> items;

    private World world;

    private Element weightText;

    private LandingParty landingParty;

    public InventoryController(World world) {
        this.world = world;
        landingParty = world.getPlayer().getLandingParty();
    }

    @Override
    public void bind(Nifty nifty, Screen screen) {
        items = screen.findNiftyControl("items", ListBox.class);
        myWindow = screen.findElementByName("inventory_window");
        weightText = screen.findElementByName("weight_text");
    }

    @Override
    public void onStartScreen() {
        myWindow.setVisible(true);
        items.clear();
        for (Multiset.Entry<InventoryItem> entry : landingParty.getInventory().entrySet()) {
            items.addItem(entry);
        }
        updateWeight();
    }

    @Override
    public void onEndScreen() {

    }

    public void closeScreen() {
        GUI.getInstance().popAndSetScreen();
    }

    @NiftyEventSubscriber(id = "inventory_window")
    public void onClose(final String id, final WindowClosedEvent event) {
        closeScreen();
    }

    private void updateWeight()
    {
        EngineUtils.setTextForGUIElement(weightText, String.format(Localization.getText("gui", "landing_party.weight"), landingParty.getInventoryWeight(), landingParty.getMaxWeight()));
    }

    public void usePressed() {
        if (items.getFocusItem().getElement().isUsable()) {
            UsableItem u = (UsableItem) items.getFocusItem().getElement();
            u.useIt(world, items.getFocusItem().getCount());
            updateWeight();
        }
    }

    public void dropPressed() {
        landingParty.getInventory().setCount(items.getFocusItem().getElement(), items.getFocusItem().getCount() - 1);
        onStartScreen();
    }

    //magic
    @NiftyEventSubscriber(pattern = ".*Button")
    public void onClicked(String id, ButtonClickedEvent event) {
        int numericId = Integer.parseInt(id.split("#")[0]);
        numericId -= Integer.parseInt(items.getElement().findElementByName("#child-root").getElements().get(0).getId());
        items.setFocusItemByIndex(numericId);
    }
}
