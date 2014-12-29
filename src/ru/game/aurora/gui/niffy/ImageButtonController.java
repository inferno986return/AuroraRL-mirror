/**
 * Created with IntelliJ IDEA.
 * User: Egor.Smirnov
 * Date: 08.07.13
 * Time: 17:22
 */

package ru.game.aurora.gui.niffy;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.Parameters;
import de.lessvoid.nifty.controls.button.ButtonControl;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.ImageRenderer;
import de.lessvoid.nifty.render.NiftyImage;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.slick2d.render.image.ImageSlickRenderImage;
import org.newdawn.slick.Image;

import javax.annotation.Nonnull;
import java.util.Properties;

public class ImageButtonController extends ButtonControl
{
    private ImageRenderer imageRenderer;

    private Nifty nifty;

    @Override
    public void bind(@Nonnull Nifty niftyParam, @Nonnull Screen screenParam, @Nonnull Element newElement, @Nonnull Parameters parameters) {
        super.bind(niftyParam, screenParam, newElement, parameters);
        this.imageRenderer = newElement.findElementByName("#image").getRenderer(ImageRenderer.class);
        this.nifty = niftyParam;
    }

    public void setImage(Image image)
    {
        if (image == null) {
            imageRenderer.setImage(null);
        } else {
            imageRenderer.setImage(new NiftyImage(nifty.getRenderEngine(), new ImageSlickRenderImage(image)));
        }
    }
}
