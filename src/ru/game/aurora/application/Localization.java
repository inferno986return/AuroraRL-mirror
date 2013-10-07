/**
 * Created with IntelliJ IDEA.
 * User: Egor.Smirnov
 * Date: 20.09.13
 * Time: 17:29
 */
package ru.game.aurora.application;

import de.lessvoid.nifty.Nifty;

import java.util.Locale;
import java.util.ResourceBundle;


public class Localization {
    private static Locale currentLocale;

    private static String[] supportedLocales = {"ru", "en"};

    public static void init(Locale locale) {
        for (String s : supportedLocales) {
            if (locale.getLanguage().contains(s)) {
                currentLocale = locale;
                System.out.println("Using locale " + locale.getLanguage());
                return;
            }
        }

        System.out.println("Locale " + locale.getLanguage() + " is not supported, defaulting to en");
        currentLocale = Locale.ENGLISH;
    }

    private static String getBundleName(String key) {
        return "localization/" + currentLocale.getLanguage() + "/" + key;
    }

    public static String getText(String bundleId, String textId) {
        ResourceBundle bundle = ResourceBundle.getBundle(getBundleName(bundleId), currentLocale, new UTF8Control());
        if (!bundle.containsKey(textId)) {
            return "<" + bundleId + "/" + textId + ">";
        }
        return bundle.getString(textId);
    }

    public static void registerGUIBungles(Nifty nifty) {
        nifty.setLocale(currentLocale);
        //nifty.addResourceBundle("gui", getBundleName("gui"));
        nifty.getResourceBundles().put("gui", ResourceBundle.getBundle(getBundleName("gui"), currentLocale, new UTF8Control()));
    }
}
