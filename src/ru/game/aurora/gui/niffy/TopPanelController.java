/**
 * Created with IntelliJ IDEA.
 * User: Egor.Smirnov
 * Date: 17.09.13
 * Time: 13:21
 */
package ru.game.aurora.gui.niffy;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.Controller;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.xml.xpp3.Attributes;
import ru.game.aurora.util.EngineUtils;

import javax.annotation.Nonnull;
import java.util.Properties;


public class TopPanelController implements Controller {
    private ProgressBarControl progressBarControl;

    private Element sciCountElement;

    private Element engiCountElement;

    private Element milCountElement;

    @Override
    public void bind(@Nonnull Nifty nifty, @Nonnull Screen screen, @Nonnull Element element, @Nonnull Parameters parameters) {
        progressBarControl = element.findControl("progressbar", ProgressBarControl.class);
        sciCountElement = element.findElementByName("sci_count").findElementByName("#count");
        engiCountElement = element.findElementByName("engi_count").findElementByName("#count");
        milCountElement = element.findElementByName("mil_count").findElementByName("#count");
    }

    @Override
    public void init(@Nonnull Parameters parameters) {
    }

    @Override
    public void onStartScreen() {
    }

    @Override
    public void onFocus(boolean getFocus) {
    }

    @Override
    public boolean inputEvent(@Nonnull NiftyInputEvent inputEvent) {
        return false;
    }

    public void setProgress(String text, float value) {
        progressBarControl.setProgress(value);
        progressBarControl.setText(text);
    }

    public void setCrewStats(int sci, int engi, int mil) {
        EngineUtils.setTextForGUIElement(sciCountElement, String.valueOf(sci));
        EngineUtils.setTextForGUIElement(engiCountElement, String.valueOf(engi));
        EngineUtils.setTextForGUIElement(milCountElement, String.valueOf(mil));
    }

    public void setProgressBarEnabled(String text, boolean enabled) {
        if (!enabled) {
            setProgress(text, 1);
        }
        progressBarControl.setEnabled(enabled);
    }

    public boolean isProgressBarEnabled() {
        return progressBarControl.isEnabled();
    }
}
