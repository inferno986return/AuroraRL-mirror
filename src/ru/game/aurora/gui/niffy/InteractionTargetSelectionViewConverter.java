/**
 * Created with IntelliJ IDEA.
 * User: Egor.Smirnov
 * Date: 19.06.14
 * Time: 14:25
 */
package ru.game.aurora.gui.niffy;

import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import ru.game.aurora.util.EngineUtils;
import ru.game.aurora.world.GameObject;


public class InteractionTargetSelectionViewConverter implements ListBox.ListBoxViewConverter<GameObject> {
    @Override
    public void display(Element element, GameObject o) {

        EngineUtils.setTextForGUIElement(element.findElementById("#line-text"), o.getName());
        EngineUtils.setImageForGUIElement(element.findElementById("#line-icon"), o.getImage());
    }

    @Override
    public int getWidth(Element element, GameObject o) {
        final Element text = element.findElementById("#line-text");
        final TextRenderer textRenderer = text.getRenderer(TextRenderer.class);
        return ((textRenderer.getFont() == null) ? 0 : textRenderer.getFont().getWidth(o.getName())
                + ((o.getImage() == null) ? 0 : o.getImage().getWidth()));
    }
}
