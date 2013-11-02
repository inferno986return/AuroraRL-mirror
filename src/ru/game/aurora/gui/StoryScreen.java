/**
 * Created with IntelliJ IDEA.
 * User: Egor.Smirnov
 * Date: 24.01.13
 * Time: 17:53
 */
package ru.game.aurora.gui;

import com.google.gson.Gson;
import ru.game.aurora.dialog.DialogListener;
import ru.game.aurora.world.World;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;

/**
 * Screen that contains image, text and left/right buttons
 */
public class StoryScreen implements Serializable {
    private static final long serialVersionUID = 1L;

    public static class StoryElement implements Serializable {
        private static final long serialVersionUID = 3916693966824352564L;

        public final String imageId;

        public final String text;

        public StoryElement(String imageId, String text) {
            this.imageId = imageId;
            this.text = text;
        }
    }

    private static final Gson gson = new Gson();

    private StoryElement[] screens;

    private int currentScreen;

    private DialogListener listener;

    public StoryScreen(String descPath) {
        Reader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(descPath));
        screens = gson.fromJson(reader, StoryElement[].class);
        try {
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setListener(DialogListener listener) {
        this.listener = listener;
    }

    public StoryElement getCurrentElement() {
        return (currentScreen >= 0 && currentScreen < screens.length) ? screens[currentScreen] : null;
    }

    public boolean next(World world) {
        if (currentScreen < screens.length) {
            ++currentScreen;
            if (isOver() && listener != null) {
                listener.onDialogEnded(world, 0);
            }
            return true;
        }
        return false;
    }

    public boolean prev() {
        if (currentScreen > 0) {
            --currentScreen;
            return true;
        }
        return false;
    }

    public void start() {
        currentScreen = 0;
    }


    public boolean isOver() {
        return currentScreen == screens.length;
    }

}
