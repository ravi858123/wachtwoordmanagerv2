package nl.wachtwoordmanager;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.PreferenceManager;

import android.view.WindowManager;
import androidx.appcompat.app.AppCompatDelegate;

public class App extends Application {

    private static Kluis kluis;
    private static long achtergrondSinds = 0L;

    @Override
    public void onCreate() {
        super.onCreate();
        kluis = new Kluis(this);
        pasThemaToe(this);
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            private int actief = 0;

            @Override public void onActivityStarted(Activity a) {
                if (actief == 0) {
                    long nu = System.currentTimeMillis();
                    int vergrendelSeconden = getVergrendelSeconden();
                    if (achtergrondSinds > 0
                            && vergrendelSeconden >= 0
                            && (nu - achtergrondSinds) > vergrendelSeconden * 1000L
                            && kluis.isOntgrendeld()) {
                        kluis.vergrendel();
                    }
                }
                actief++;
            }

            @Override public void onActivityStopped(Activity a) {
                actief--;
                if (actief == 0) achtergrondSinds = System.currentTimeMillis();
            }

            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityResumed(Activity a) {}
            @Override public void onActivityPaused(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        });
    }

    private int getVergrendelSeconden() {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
        String v = p.getString("auto_vergrendel", "300");
        try { return Integer.parseInt(v); } catch (Exception e) { return 300; }
    }

    public static void pasThemaToe(android.content.Context ctx) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(ctx);
        String thema = p.getString("thema", "systeem");
        switch (thema) {
            case "licht":  AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);  break;
            case "donker": AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
            default:       AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    public static Kluis getKluis() { return kluis; }

    /** Past FLAG_SECURE toe op het venster van een activiteit op basis van de instelling. */
    public static void pasSchermBeveiligingToe(android.app.Activity activity) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean beveiligd = p.getBoolean("scherm_beveiligen", true);
        if (beveiligd) {
            activity.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            activity.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        }
    }
}
