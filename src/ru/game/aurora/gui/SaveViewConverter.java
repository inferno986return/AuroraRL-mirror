/**
 * Created with IntelliJ IDEA.
 * User: User
 * Date: 19.08.14
 * Time: 23:25
 */

package ru.game.aurora.gui;

import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.elements.Element;
import ru.game.aurora.application.AuroraGame;
import ru.game.aurora.application.Localization;
import ru.game.aurora.application.SaveGameManager;
import ru.game.aurora.util.EngineUtils;


public class SaveViewConverter implements ListBox.ListBoxViewConverter<SaveGameManager.SaveGameSlot> {

    private String buildString(SaveGameManager.SaveGameSlot item) {
        if (!item.isLoaded()) {
            return item.isAutosave ? Localization.getText("gui", "saveload.autosave") + "\n" : ""
                    + Localization.getText("gui", "saveload.empty_slot");
        }
        return (item.isAutosave ? Localization.getText("gui", "saveload.autosave") + "\n" : "")
                + item.date.toString() + "\n"
                + item.gameLocation + "\n"
                + item.gameDate;
    }

    @Override
    public void display(Element element, SaveGameManager.SaveGameSlot item) {
        EngineUtils.setTextForGUIElement(element.findElementById("#line-text"), buildString(item));
        EngineUtils.setImageForGUIElement(element.findElementById("#line-icon"), item.getScreenshot());
        if (item.isAutosave) {
            element.findElementById("#saveButton").hide();
        } else {
            element.findElementById("#saveButton").show();
        }
        if (!AuroraGame.isGameRunning()) {
            element.findElementById("#saveButton").disable();
        } else {
            element.findElementById("#saveButton").enable();
        }
        if (!item.isLoaded()) {
            element.findElementById("#loadButton").disable();
        } else {
            element.findElementById("#loadButton").enable();
        }
    }

    @Override
    public int getWidth(Element element, SaveGameManager.SaveGameSlot item) {
        return SaveGameManager.SCREEN_SIZE + 400 + 2 * 150 + 20;
    }
}
