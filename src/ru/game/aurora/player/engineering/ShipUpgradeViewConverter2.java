package ru.game.aurora.player.engineering;

import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import ru.game.aurora.util.EngineUtils;

/**
 */
public class ShipUpgradeViewConverter2 implements ListBox.ListBoxViewConverter<ShipUpgrade> {
    @Override
    public void display(Element element, ShipUpgrade item) {
        EngineUtils.setTextForGUIElement(element.findElementById("#line-text"), item.getLocalizedDescription());
        EngineUtils.setImageForGUIElement(element.findElementById("#line-icon"), item.getDrawable().getImage());
    }

    @Override
    public int getWidth(Element element, ShipUpgrade item) {
        final Element text = element.findElementById("#line-text");
        final TextRenderer textRenderer = text.getRenderer(TextRenderer.class);
        return ((textRenderer.getFont() == null) ? 0 : textRenderer.getFont().getWidth(item.getLocalizedDescription())
                + item.getDrawable().getImage().getWidth());
    }
}
