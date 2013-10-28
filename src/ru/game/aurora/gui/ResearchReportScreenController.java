/**
 * Created with IntelliJ IDEA.
 * User: Egor.Smirnov
 * Date: 06.06.13
 * Time: 14:57
 */

package ru.game.aurora.gui;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.controls.Window;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.ImageRenderer;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.render.NiftyImage;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import de.lessvoid.nifty.slick2d.render.image.ImageSlickRenderImage;
import ru.game.aurora.application.ResourceManager;
import ru.game.aurora.player.engineering.EngineeringProject;
import ru.game.aurora.player.research.ResearchProjectDesc;
import ru.game.aurora.world.World;


public class ResearchReportScreenController implements ScreenController {
    private World world;

    private ResearchProjectDesc research;

    private Window window;

    private Element icon;

    private Element text;

    private ListBox nextResearch;

    public ResearchReportScreenController(World world) {
        this.world = world;
    }

    public void setResearch(ResearchProjectDesc research) {
        this.research = research;
    }

    @Override
    public void bind(Nifty nifty, Screen screen) {
        window = screen.findNiftyControl("research_report_window", Window.class);
        icon = screen.findElementByName("image_panel");
        text = screen.findElementByName("report_text");
        nextResearch = screen.findNiftyControl("new_research", ListBox.class);
    }

    @Override
    public void onStartScreen() {
        world.setPaused(true);
        window.setTitle(research.getName() + " report");
        icon.getRenderer(ImageRenderer.class).setImage(new NiftyImage(GUI.getInstance().getNifty().getRenderEngine(), new ImageSlickRenderImage(ResourceManager.getInstance().getImage(research.getReport().icon))));
        text.getRenderer(TextRenderer.class).setText(research.getReport().getText());
        nextResearch.clear();
        if (research.getMakesAvailable() != null) {
            for (String researchId : research.getMakesAvailable()) {
                ResearchProjectDesc r = world.getResearchAndDevelopmentProjects().getResearchProjects().get(researchId);
                nextResearch.addItem(r.getName());
            }
        }
        if (research.getMakesAvailableEngineering() != null) {
            for (String engineeringId : research.getMakesAvailableEngineering()) {
                EngineeringProject p = world.getResearchAndDevelopmentProjects().getEngineeringProjects().get(engineeringId);
                nextResearch.addItem(p.getLocalizedName("engineering"));
            }
        }
        GUI.getInstance().getNifty().getCurrentScreen().layoutLayers();
    }

    @Override
    public void onEndScreen() {
        world.setPaused(false);
    }

    public void closeScreen() {
        GUI.getInstance().popAndSetScreen();
    }
}
