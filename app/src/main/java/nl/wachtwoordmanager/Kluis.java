package nl.wachtwoordmanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class Kluis {
    private static final String PREFS_NAAM  = "kluis_prefs";
    private static final String KEY_SALT    = "salt";
    private static final String KEY_DATA    = "data";
    private static final String KEY_BESTAAT = "bestaat";
    private static final String KEY_BIOMETRIE_TOKEN = "bio_token";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    private String hoofd;
    private byte[] salt;
    private Map<String, WachtwoordItem> items = new LinkedHashMap<>();

    public Kluis(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAAM, Context.MODE_PRIVATE);
    }

    public boolean bestaatKluis() {
        return prefs.getBoolean(KEY_BESTAAT, false);
    }

    public void maakNieuw(String wachtwoord) throws Exception {
        salt = Crypto.nieuweSalt();
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putBoolean(KEY_BESTAAT, true)
            .apply();
        hoofd = wachtwoord;
        items = new LinkedHashMap<>();
        slaOp();
    }

    public boolean open(String wachtwoord) {
        try {
            String saltStr = prefs.getString(KEY_SALT, null);
            if (saltStr == null) return false;
            salt = Base64.decode(saltStr, Base64.NO_WRAP);

            String versleuteld = prefs.getString(KEY_DATA, null);
            if (versleuteld == null) {
                hoofd = wachtwoord;
                items = new LinkedHashMap<>();
                return true;
            }

            String json = Crypto.ontsleutel(versleuteld, wachtwoord, salt);
            Type type = new TypeToken<LinkedHashMap<String, WachtwoordItem>>() {}.getType();
            items = gson.fromJson(json, type);
            if (items == null) items = new LinkedHashMap<>();
            hoofd = wachtwoord;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void slaOp() throws Exception {
        String json = gson.toJson(items);
        String versleuteld = Crypto.versleutel(json, hoofd, salt);
        prefs.edit().putString(KEY_DATA, versleuteld).apply();
    }

    /** Wijzig het hoofdwachtwoord. Gooit uitzondering als huidig onjuist is. */
    public void wijzigWachtwoord(String huidig, String nieuw) throws Exception {
        if (!huidig.equals(hoofd)) throw new Exception("Huidig wachtwoord is onjuist");
        hoofd = nieuw;
        salt = Crypto.nieuweSalt();
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .apply();
        slaOp();
    }

    /** Geeft de Base64-versleutelde JSON terug als export-string voor back-ups. */
    public String exporteerVersleuteld(String backupWachtwoord) throws Exception {
        String json = gson.toJson(items);
        byte[] backupSalt = Crypto.nieuweSalt();
        String versleuteld = Crypto.versleutel(json, backupWachtwoord, backupSalt);
        // Formaat: Base64(salt) + "." + versleuteld
        return Base64.encodeToString(backupSalt, Base64.NO_WRAP) + "." + versleuteld;
    }

    /** Herstelt items uit een export-string. Retourneert aantal herstelde items. */
    public int herstelVanExport(String exportString, String backupWachtwoord) throws Exception {
        int dot = exportString.indexOf('.');
        if (dot < 0) throw new Exception("Ongeldig back-upformaat");
        byte[] backupSalt = Base64.decode(exportString.substring(0, dot), Base64.NO_WRAP);
        String versleuteld = exportString.substring(dot + 1);
        String json = Crypto.ontsleutel(versleuteld, backupWachtwoord, backupSalt);
        Type type = new TypeToken<LinkedHashMap<String, WachtwoordItem>>() {}.getType();
        Map<String, WachtwoordItem> hersteld = gson.fromJson(json, type);
        if (hersteld == null) throw new Exception("Lege of ongeldige back-up");
        for (Map.Entry<String, WachtwoordItem> e : hersteld.entrySet()) {
            items.put(e.getKey(), e.getValue());
        }
        slaOp();
        return hersteld.size();
    }

    public boolean isBiometrieOpgeslagen() {
        return prefs.contains(KEY_BIOMETRIE_TOKEN);
    }

    public void slaaBiometrieToken(String token) {
        prefs.edit().putString(KEY_BIOMETRIE_TOKEN, token).apply();
    }

    public String getBiometrieToken() {
        return prefs.getString(KEY_BIOMETRIE_TOKEN, null);
    }

    public void verwijderBiometrieToken() {
        prefs.edit().remove(KEY_BIOMETRIE_TOKEN).apply();
    }

    public Map<String, WachtwoordItem> getItems() { return items; }
    public WachtwoordItem getItem(String id) { return items.get(id); }

    public String voegToe(WachtwoordItem item) throws Exception {
        String id = UUID.randomUUID().toString();
        item.aangemaakt = System.currentTimeMillis();
        item.gewijzigd = item.aangemaakt;
        items.put(id, item);
        slaOp();
        return id;
    }

    public void slaItemOp(String id, WachtwoordItem item) throws Exception {
        item.gewijzigd = System.currentTimeMillis();
        items.put(id, item);
        slaOp();
    }

    public void verwijderItem(String id) throws Exception {
        items.remove(id);
        slaOp();
    }

    public boolean isOntgrendeld() { return hoofd != null; }

    /** Retourneert het huidige hoofdwachtwoord (alleen zichtbaar als kluis open is). */
    public String getHoofdWachtwoord() { return hoofd; }

    public void vergrendel() {
        hoofd = null;
        salt = null;
        items = new LinkedHashMap<>();
    }
}
