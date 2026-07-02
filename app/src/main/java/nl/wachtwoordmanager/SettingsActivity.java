package nl.wachtwoordmanager;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class SettingsActivity extends AppCompatActivity {

    private Kluis kluis;
    private SharedPreferences prefs;
    private SwitchMaterial switchBiometrie, switchScherm;
    private RadioGroup rgThema;
    private Spinner spinnerAutoVergrendel, spinnerKlembord;
    private TextInputEditText etHuidig, etNieuw, etNieuwHerhaal;

    private final ActivityResultLauncher<Intent> backupOpslaan =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                vraagBackupWachtwoord(uri, true);
            }
        });

    private final ActivityResultLauncher<Intent> backupOpenen =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                vraagBackupWachtwoord(uri, false);
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.pasThemaToe(this);
        App.pasSchermBeveiligingToe(this);
        setContentView(R.layout.activity_settings);

        kluis = App.getKluis();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Terug
        ((ImageButton) findViewById(R.id.btnTerug)).setOnClickListener(v -> finish());

        // Thema
        rgThema = findViewById(R.id.rgThema);
        String thema = prefs.getString("thema", "systeem");
        if ("licht".equals(thema))       rgThema.check(R.id.rbThemaLicht);
        else if ("donker".equals(thema)) rgThema.check(R.id.rbThemaDonker);
        else                             rgThema.check(R.id.rbThemaSysteem);

        rgThema.setOnCheckedChangeListener((g, id) -> {
            String t = (id == R.id.rbThemaLicht) ? "licht"
                     : (id == R.id.rbThemaDonker) ? "donker" : "systeem";
            prefs.edit().putString("thema", t).apply();
            App.pasThemaToe(this);
        App.pasSchermBeveiligingToe(this);
            recreate();
        });

        // Biometrie switch
        switchBiometrie = findViewById(R.id.switchBiometrie);
        switchBiometrie.setChecked(kluis.isBiometrieOpgeslagen());
        controleerBiometrieBeschikbaarheid();
        switchBiometrie.setOnCheckedChangeListener((btn, aan) -> {
            if (aan) registreerBiometrie();
            else {
                kluis.verwijderBiometrieToken();
                switchBiometrie.setChecked(false);
            }
        });

        // Scherm beveiligen
        switchScherm = findViewById(R.id.switchScherm);
        switchScherm.setChecked(prefs.getBoolean("scherm_beveiligen", true));
        switchScherm.setOnCheckedChangeListener((b, aan) -> {
            prefs.edit().putBoolean("scherm_beveiligen", aan).apply();
            pasSchermBeveiligingToe(aan);
        });
        pasSchermBeveiligingToe(switchScherm.isChecked());

        // Auto-vergrendel spinner
        spinnerAutoVergrendel = findViewById(R.id.spinnerAutoVergrendel);
        String[] vergrendelOpties = {
            getString(R.string.auto_vergrendel_direct),
            getString(R.string.auto_vergrendel_1),
            getString(R.string.auto_vergrendel_5),
            getString(R.string.auto_vergrendel_15),
            getString(R.string.auto_vergrendel_nooit)
        };
        int[] vergrendelWaarden = {0, 60, 300, 900, -1};
        ArrayAdapter<String> vAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, vergrendelOpties);
        vAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAutoVergrendel.setAdapter(vAdapter);
        int geslagenV = prefs.getInt("auto_vergrendel_idx", 2);
        spinnerAutoVergrendel.setSelection(geslagenV);
        spinnerAutoVergrendel.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                prefs.edit().putInt("auto_vergrendel_idx", pos).putString("auto_vergrendel", String.valueOf(vergrendelWaarden[pos])).apply();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });

        // Klembord wissen spinner
        spinnerKlembord = findViewById(R.id.spinnerKlembord);
        String[] klembordOpties = {
            getString(R.string.klembord_10),
            getString(R.string.klembord_30),
            getString(R.string.klembord_60),
            getString(R.string.klembord_nooit)
        };
        ArrayAdapter<String> kAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, klembordOpties);
        kAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerKlembord.setAdapter(kAdapter);
        spinnerKlembord.setSelection(prefs.getInt("klembord_idx", 1));
        spinnerKlembord.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                prefs.edit().putInt("klembord_idx", pos).apply();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });

        // Wachtwoord wijzigen
        etHuidig       = findViewById(R.id.etHuidig);
        etNieuw        = findViewById(R.id.etNieuw);
        etNieuwHerhaal = findViewById(R.id.etNieuwHerhaal);
        ((Button) findViewById(R.id.btnWijzig)).setOnClickListener(v -> wijzigWachtwoord());

        // Backup / herstel
        ((LinearLayout) findViewById(R.id.rowBackup)).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/octet-stream");
            i.putExtra(Intent.EXTRA_TITLE, "wachtwoordmanager-backup.kwb");
            backupOpslaan.launch(i);
        });

        ((LinearLayout) findViewById(R.id.rowHerstel)).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            backupOpenen.launch(i);
        });
    }

    private void pasSchermBeveiligingToe(boolean aan) {
        if (aan) getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        else     getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
    }

    private void controleerBiometrieBeschikbaarheid() {
        BiometricManager bm = BiometricManager.from(this);
        boolean ok = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            == BiometricManager.BIOMETRIC_SUCCESS;
        switchBiometrie.setEnabled(ok);
        if (!ok) switchBiometrie.setChecked(false);
    }

    private void registreerBiometrie() {
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometrie_titel))
            .setSubtitle("Registreer je vingerafdruk voor snelle toegang")
            .setNegativeButtonText(getString(R.string.annuleren))
            .build();

        BiometricPrompt prompt = new BiometricPrompt(this,
            ContextCompat.getMainExecutor(this),
            new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    // Sla het hoofdwachtwoord op als biometrie-token zodat we het
                    // bij volgende ontgrendeling direct kunnen gebruiken.
                    String pw = kluis.getHoofdWachtwoord();
                    if (pw != null) {
                        kluis.slaaBiometrieToken(pw);
                        Toast.makeText(SettingsActivity.this, "Vingerafdruk geregistreerd", Toast.LENGTH_SHORT).show();
                    } else {
                        switchBiometrie.setChecked(false);
                    }
                }
                @Override
                public void onAuthenticationError(int code, @NonNull CharSequence msg) {
                    if (code != BiometricPrompt.ERROR_NEGATIVE_BUTTON)
                        switchBiometrie.setChecked(false);
                    else switchBiometrie.setChecked(false);
                }
                @Override
                public void onAuthenticationFailed() { switchBiometrie.setChecked(false); }
            });
        prompt.authenticate(info);
    }

    private void wijzigWachtwoord() {
        String huidig  = tekst(etHuidig);
        String nieuw   = tekst(etNieuw);
        String herhaal = tekst(etNieuwHerhaal);

        if (huidig.isEmpty() || nieuw.isEmpty()) {
            Toast.makeText(this, "Vul alle velden in", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!nieuw.equals(herhaal)) {
            Toast.makeText(this, getString(R.string.fout_herhaal), Toast.LENGTH_SHORT).show();
            return;
        }
        if (nieuw.length() < 8) {
            Toast.makeText(this, getString(R.string.fout_kort), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            kluis.wijzigWachtwoord(huidig, nieuw);
            Toast.makeText(this, getString(R.string.wachtwoord_gewijzigd), Toast.LENGTH_SHORT).show();
            etHuidig.setText("");
            etNieuw.setText("");
            etNieuwHerhaal.setText("");
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void vraagBackupWachtwoord(Uri uri, boolean isExport) {
        TextInputEditText etPw = new TextInputEditText(this);
        etPw.setHint(getString(R.string.backup_wachtwoord_hint));
        etPw.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        int pad = (int)(16 * getResources().getDisplayMetrics().density);
        etPw.setPadding(pad, pad, pad, pad);

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(isExport ? getString(R.string.backup_maken) : getString(R.string.backup_herstellen))
            .setMessage(getString(R.string.backup_wachtwoord_uitleg))
            .setView(etPw)
            .setPositiveButton(getString(R.string.bevestigen), (d, w) -> {
                String pw = etPw.getText() != null ? etPw.getText().toString() : "";
                if (pw.isEmpty()) return;
                if (isExport) doeExport(uri, pw);
                else doeImport(uri, pw);
            })
            .setNegativeButton(getString(R.string.annuleren), null)
            .show();
    }

    private void doeExport(Uri uri, String pw) {
        try {
            String data = kluis.exporteerVersleuteld(pw);
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                if (os != null) os.write(data.getBytes("UTF-8"));
            }
            Toast.makeText(this, getString(R.string.backup_gelukt), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.backup_mislukt) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void doeImport(Uri uri, String pw) {
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(getContentResolver().openInputStream(uri), "UTF-8"))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
            int n = kluis.herstelVanExport(sb.toString(), pw);
            Toast.makeText(this, getString(R.string.herstel_gelukt, n), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.herstel_mislukt), Toast.LENGTH_LONG).show();
        }
    }

    private String tekst(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
