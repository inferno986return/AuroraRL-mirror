package ru.game.aurora.gui;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.ButtonClickedEvent;
import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import org.newdawn.slick.Input;
import org.newdawn.slick.KeyListener;
import ru.game.aurora.application.Configuration;
import ru.game.aurora.application.InputBinding;
import ru.game.aurora.util.EngineUtils;

import java.util.*;

/**
 * Screen with keyboard layout
 */
public class InputBindingScreenController extends DefaultCloseableScreenController {

    private Input input;

    private ListBox<Map.Entry<InputBinding.Action, Integer>> keyMap;

    private Map<InputBinding.Action, Integer> backupKeys;

    private MyKeyListener listener = new MyKeyListener();

    public InputBindingScreenController(Input input) {
        this.input = input;
    }

    private class MyKeyListener implements KeyListener
    {
        private InputBinding.Action action;

        private Element textElement;

        public void set(InputBinding.Action a, Element e) {
            if (textElement != null) {
                for (Map.Entry<InputBinding.Action, Integer> entry : keyMap.getItems()) {
                    if (entry.getKey() == action) {
                        EngineUtils.setTextForGUIElement(textElement, Input.getKeyName(entry.getValue()));
                        break;
                    }
                }
            }
            this.action = a;
            this.textElement = e;
        }

        @Override
        public void keyPressed(int i, char c) {

        }

        @Override
        public void keyReleased(int i, char c) {
            input.removeKeyListener(this);
            for (Map.Entry<InputBinding.Action, Integer> e : keyMap.getItems()) {
                if (e.getKey() == action) {
                    e.setValue(i);
                    break;
                }
            }
            EngineUtils.setTextForGUIElement(textElement, Input.getKeyName(i));
            action = null;
            textElement = null;
        }

        @Override
        public void setInput(Input input) {

        }

        @Override
        public boolean isAcceptingInput() {
            return true;
        }

        @Override
        public void inputEnded() {

        }

        @Override
        public void inputStarted() {

        }
    }

    @Override
    public void bind(Nifty nifty, Screen screen) {
        keyMap = screen.findNiftyControl("items", ListBox.class);
    }

    @Override
    public void onStartScreen() {
        keyMap.clear();
        setupKeyMap();
        backupKeys = new EnumMap<InputBinding.Action, Integer>(InputBinding.Action.class);
        backupKeys.putAll(InputBinding.keyBinding);
    }

    @Override
    public void onEndScreen() {

    }

    public void applySettings() {
        for (Map.Entry<InputBinding.Action, Integer> entry : keyMap.getItems()) {
            InputBinding.keyBinding.put(entry.getKey(), entry.getValue());
        }
        saveConfiguration();
        GUI.getInstance().getNifty().gotoScreen("settings_screen");
    }

    public void cancelSettings() {
        InputBinding.keyBinding.putAll(backupKeys);
        saveConfiguration();
        GUI.getInstance().getNifty().gotoScreen("settings_screen");
    }

    public void restoreDefault(){
        InputBinding.useDefaultBinding();
        saveConfiguration();
        keyMap.clear();
        setupKeyMap();
    }

    private void setupKeyMap() {
        // show actual key binds
        List<Map.Entry<InputBinding.Action, Integer>> listToAdd = new ArrayList<>();
        listToAdd.addAll(InputBinding.keyBinding.entrySet());
        Collections.sort(listToAdd, new Comparator<Map.Entry<InputBinding.Action, Integer>>() {
            @Override
            public int compare(Map.Entry<InputBinding.Action, Integer> o1, Map.Entry<InputBinding.Action, Integer> o2) {
                return Integer.compare(o1.getKey().ordinal(), o2.getKey().ordinal());
            }
        });

        keyMap.addAllItems(listToAdd);
    }

    private void saveConfiguration(){
        Configuration.getSystemProperties().put(InputBinding.key, InputBinding.saveToString());
        Configuration.saveSystemProperties();
    }

    @NiftyEventSubscriber(pattern = ".*redefine.+")
    public void redefine(String id, ButtonClickedEvent event) {
        final Element parent = event.getButton().getElement().getParent();
        final InputBinding.Action action = (InputBinding.Action) parent.getUserData();
        if (listener.action == action) {
            return; // already pressed same button
        }
        input.removeKeyListener(listener);

        final Element textElement = parent.findElementByName("#value-text");
        EngineUtils.setTextForGUIElement(textElement, "???");
        listener.set(action, textElement);
        input.addKeyListener(listener);
    }

    @Override
    public void inputUpdate(Input input) {
        if(input.isKeyPressed(Input.KEY_ENTER)){
            applySettings();
            return;
        }
        else if(input.isKeyPressed(Input.KEY_ESCAPE)){
            cancelSettings();
            return;
        }
    }
}
