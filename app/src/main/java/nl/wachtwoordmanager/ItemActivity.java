package nl.wachtwoordmanager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import java.security.SecureRandom;

public class ItemActivity extends AppCompatActivity {

    private static final String LETTERS  = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String CIJFERS  = "0123456789";
    private static final String SYMBOLEN = "!@#$%^&*()-_=+";
    private static final String AMBIGUE  = "O0lI1";

    private TextInputEditText etNaam, etGebruiker, etWachtwoord, etUrl, etNotities, etTotp;
    private TextView tvStatus, tvLengte, tvSterkte, tvEntropie, tvTitelBalk, tvTotpCode;
    private Slider sliderLengte;
    private CheckBox cbSymbolen, cbAmbigue;
    private ProgressBar pbSterkte;
    private MaterialCardView cardStatus;
    private CircularProgressIndicator totpTimer;
    private ImageButton btnFavoriet;
    private ChipGroup chipGroupCategorie;
    private Kluis kluis;
    private String itemId;
    private boolean isFavoriet = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable verbergStatus;
    private Runnable totpTick;
    private int klembordVerwijderSeconden = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.pasThemaToe(this);
        App.pasSchermBeveiligingToe(this);
        setContentView(R.layout.activity_item);

        kluis  = App.getKluis();
        itemId = getIntent().getStringExtra("id");

        // Klembord verwijder-tijd uit instellingen (index: 0=10s, 1=30s, 2=60s, 3=nooit)
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int klembordIdx = prefs.getInt("klembord_idx", 1);
        int[] klembordSeconden = {10, 30, 60, -1};
        klembordVerwijderSeconden = klembordSeconden[Math.min(klembordIdx, klembordSeconden.length - 1)];

        // Views ophalen
        etNaam         = findViewById(R.id.etNaam);
        etGebruiker    = findViewById(R.id.etGebruikersnaam);
        etWachtwoord   = findViewById(R.id.etWachtwoord);
        etUrl          = findViewById(R.id.etUrl);
        etNotities     = findViewById(R.id.etNotities);
        etTotp         = findViewById(R.id.etTotp);
        tvStatus       = findViewById(R.id.tvStatus);
        tvLengte       = findViewById(R.id.tvLengte);
        tvSterkte      = findViewById(R.id.tvSterkte);
        tvEntropie     = findViewById(R.id.tvEntropie);
        tvTitelBalk    = findViewById(R.id.tvTitelBalk);
        tvTotpCode     = findViewById(R.id.tvTotpCode);
        sliderLengte   = findViewById(R.id.sliderLengte);
        cbSymbolen     = findViewById(R.id.cbSymbolen);
        cbAmbigue      = findViewById(R.id.cbAmbigue);
        pbSterkte      = findViewById(R.id.pbSterkte);
        cardStatus     = findViewById(R.id.cardStatus);
        totpTimer      = findViewById(R.id.totpTimer);
        btnFavoriet    = findViewById(R.id.btnFavoriet);
        chipGroupCategorie = findViewById(R.id.chipGroupCategorie);

        View layoutTotpV = findViewById(R.id.layoutTotpWeergave);
        // layoutTotpWeergave is een LinearLayout, niet MaterialCardView
        final View layoutTotpWeergaveView = layoutTotpV;

        // Laad bestaand item
        if (itemId != null) {
            WachtwoordItem item = kluis.getItem(itemId);
            if (item != null) {
                etNaam.setText(item.naam);
                etGebruiker.setText(item.gebruikersnaam);
                etWachtwoord.setText(item.wachtwoord);
                etUrl.setText(item.url);
                etNotities.setText(item.notities);
                if (item.totpGeheim != null) etTotp.setText(item.totpGeheim);
                isFavoriet = item.favoriet;
                tvTitelBalk.setText(item.naam != null ? item.naam : getString(R.string.item_bewerken));
                selecteerCategorie(item.categorie);
            }
        } else {
            tvTitelBalk.setText(getString(R.string.nieuw_item_titel));
        }

        updateFavorietKnop();

        // Favoriet toggle
        btnFavoriet.setOnClickListener(v -> {
            isFavoriet = !isFavoriet;
            updateFavorietKnop();
        });

        // Slider
        sliderLengte.addOnChangeListener((slider, value, fromUser) ->
            tvLengte.setText(String.valueOf((int) value)));

        // Wachtwoord sterkte live
        etWachtwoord.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            public void onTextChanged(CharSequence s, int i, int b, int c) {
                updateSterkte(s.toString());
            }
            public void afterTextChanged(Editable s) {}
        });
        if (etWachtwoord.getText() != null) updateSterkte(etWachtwoord.getText().toString());

        // Generator
        Button btnGenereer = findViewById(R.id.btnGenereer);
        btnGenereer.setOnClickListener(v -> {
            int lengte = (int) sliderLengte.getValue();
            boolean metSymbolen = cbSymbolen.isChecked();
            boolean zonder = cbAmbigue.isChecked();
            etWachtwoord.setText(genereerWachtwoord(lengte, metSymbolen, zonder));
        });

        // TOTP watcher
        etTotp.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            public void onTextChanged(CharSequence s, int i, int b, int c) {
                if (TotpGenerator.isGeldig(s.toString())) {
                    layoutTotpWeergaveView.setVisibility(View.VISIBLE);
                    startTotpTick(s.toString());
                } else {
                    layoutTotpWeergaveView.setVisibility(View.GONE);
                    stopTotpTick();
                }
            }
            public void afterTextChanged(Editable s) {}
        });

        // Start TOTP ticker als er al een sleutel is
        String totpTekst = etTotp.getText() != null ? etTotp.getText().toString() : "";
        if (TotpGenerator.isGeldig(totpTekst)) {
            layoutTotpWeergaveView.setVisibility(View.VISIBLE);
            startTotpTick(totpTekst);
        }

        // Kopieer wachtwoord
        Button btnKopieer = findViewById(R.id.btnKopieer);
        btnKopieer.setOnClickListener(v -> {
            String pw = tekst(etWachtwoord, "");
            kopieerNaarKlembord(pw);
        });

        // Opslaan
        Button btnOpslaan = findViewById(R.id.btnOpslaan);
        btnOpslaan.setOnClickListener(v -> slaOp());

        // Verwijderen
        Button btnVerwijder = findViewById(R.id.btnVerwijder);
        btnVerwijder.setOnClickListener(v -> {
            if (itemId == null) { finish(); return; }
            new AlertDialog.Builder(this)
                .setMessage(R.string.verwijder_bevestig)
                .setPositiveButton(R.string.ja, (d, w) -> {
                    try {
                        kluis.verwijderItem(itemId);
                        finish();
                    } catch (Exception e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.nee, null)
                .show();
        });

        // Terug
        findViewById(R.id.btnTerug).setOnClickListener(v -> finish());
    }

    private void startTotpTick(String geheim) {
        stopTotpTick();
        totpTick = new Runnable() {
            @Override public void run() {
                try {
                    String code = TotpGenerator.genereer(geheim, System.currentTimeMillis());
                    // Spatie in midden voor leesbaarheid: 123 456
                    tvTotpCode.setText(code.substring(0, 3) + " " + code.substring(3));
                    int secs = TotpGenerator.secondenTotVerval();
                    totpTimer.setProgress(secs);
                } catch (Exception ignored) {}
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(totpTick);
    }

    private void stopTotpTick() {
        if (totpTick != null) { handler.removeCallbacks(totpTick); totpTick = null; }
    }

    @Override protected void onDestroy() { super.onDestroy(); stopTotpTick(); }

    private void updateSterkte(String pw) {
        WachtwoordSterkte s = WachtwoordSterkte.analyseer(pw);
        pbSterkte.setProgress(s.niveau);

        String[] labels = { getString(R.string.sterkte_zeer_zwak), getString(R.string.sterkte_zwak),
                            getString(R.string.sterkte_redelijk), getString(R.string.sterkte_sterk),
                            getString(R.string.sterkte_zeer_sterk) };
        int[] kleuren = { Color.parseColor("#DC2626"), Color.parseColor("#F97316"),
                          Color.parseColor("#EAB308"), Color.parseColor("#22C55E"),
                          Color.parseColor("#16A34A") };

        tvSterkte.setText(pw.isEmpty() ? "" : labels[s.niveau]);
        tvSterkte.setTextColor(kleuren[s.niveau]);
        tvEntropie.setText(pw.isEmpty() ? "" :
            getString(R.string.entropie_bits, (int) s.entropie));
        pbSterkte.getProgressDrawable().setTint(kleuren[s.niveau]);
    }

    private void updateFavorietKnop() {
        btnFavoriet.setImageResource(isFavoriet
            ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
    }

    private void selecteerCategorie(String cat) {
        if (cat == null) return;
        int chipId;
        switch (cat) {
            case "werk":      chipId = R.id.chipWerkCat;      break;
            case "sociaal":   chipId = R.id.chipSociaalCat;   break;
            case "financieel":chipId = R.id.chipFinancieelCat;break;
            case "overig":    chipId = R.id.chipOverigCat;    break;
            default:          chipId = R.id.chipAlgemeen;     break;
        }
        chipGroupCategorie.check(chipId);
    }

    private String getGekozenCategorie() {
        int checked = chipGroupCategorie.getCheckedChipId();
        if (checked == R.id.chipWerkCat)       return "werk";
        if (checked == R.id.chipSociaalCat)    return "sociaal";
        if (checked == R.id.chipFinancieelCat) return "financieel";
        if (checked == R.id.chipOverigCat)     return "overig";
        return "algemeen";
    }

    private void slaOp() {
        String totpStr = tekst(etTotp, "");
        if (!totpStr.isEmpty() && !TotpGenerator.isGeldig(totpStr)) {
            Toast.makeText(this, getString(R.string.totp_ongeldig), Toast.LENGTH_SHORT).show();
            return;
        }

        WachtwoordItem item = new WachtwoordItem(
            tekst(etNaam, "Naamloos"),
            tekst(etGebruiker, ""),
            tekst(etWachtwoord, ""),
            tekst(etUrl, ""),
            tekst(etNotities, ""),
            getGekozenCategorie(),
            totpStr.isEmpty() ? null : totpStr,
            isFavoriet
        );

        if (itemId != null) {
            WachtwoordItem oud = kluis.getItem(itemId);
            if (oud != null) {
                item.aangemaakt = oud.aangemaakt;
            }
        }

        try {
            if (itemId == null) {
                itemId = kluis.voegToe(item);
                tvTitelBalk.setText(item.naam);
            } else {
                kluis.slaItemOp(itemId, item);
            }
            toonStatus(getString(R.string.opgeslagen));
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void kopieerNaarKlembord(String tekst) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("wachtwoord", tekst));
        toonStatus(getString(R.string.gekopieerd));

        // Auto-wis klembord (alleen als instelling niet op "nooit" staat)
        if (klembordVerwijderSeconden > 0) {
            handler.postDelayed(() -> {
                ClipboardManager cm2 = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm2 != null) cm2.setPrimaryClip(ClipData.newPlainText("", ""));
            }, klembordVerwijderSeconden * 1000L);
        }
    }

    private String tekst(TextInputEditText et, String standaard) {
        String s = et.getText() != null ? et.getText().toString().trim() : "";
        return s.isEmpty() ? standaard : s;
    }

    private void toonStatus(String bericht) {
        tvStatus.setText(bericht);
        cardStatus.setVisibility(View.VISIBLE);
        if (verbergStatus != null) handler.removeCallbacks(verbergStatus);
        verbergStatus = () -> cardStatus.setVisibility(View.GONE);
        handler.postDelayed(verbergStatus, 2500);
    }

    private String genereerWachtwoord(int lengte, boolean metSymbolen, boolean uitsluitenAmbigue) {
        String tekens = LETTERS + CIJFERS + (metSymbolen ? SYMBOLEN : "");
        if (uitsluitenAmbigue) {
            for (char c : AMBIGUE.toCharArray()) {
                tekens = tekens.replace(String.valueOf(c), "");
            }
        }
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder(lengte);
        for (int i = 0; i < lengte; i++) {
            sb.append(tekens.charAt(rng.nextInt(tekens.length())));
        }
        return sb.toString();
    }
}
