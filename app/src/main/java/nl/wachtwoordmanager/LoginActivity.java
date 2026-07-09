package nl.wachtwoordmanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etWachtwoord, etHerhaal;
    private TextInputLayout tilHerhaal;
    private Button btnLogin;
    private LinearLayout layoutBiometrie;
    private ImageButton btnBiometrie;
    private TextView tvOndertitel;
    private Kluis kluis;
    private boolean isNieuw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.pasThemaToe(this);
        App.pasSchermBeveiligingToe(this);
        setContentView(R.layout.activity_login);

        kluis = App.getKluis();
        isNieuw = !kluis.bestaatKluis();

        etWachtwoord   = findViewById(R.id.etWachtwoord);
        etHerhaal      = findViewById(R.id.etHerhaal);
        tilHerhaal     = findViewById(R.id.tilHerhaal);
        btnLogin       = findViewById(R.id.btnLogin);
        tvOndertitel   = findViewById(R.id.tvOndertitel);
        layoutBiometrie = findViewById(R.id.layoutBiometrie);
        btnBiometrie   = findViewById(R.id.btnBiometrie);

        if (isNieuw) {
            tvOndertitel.setText(R.string.nieuwe_kluis);
            tilHerhaal.setVisibility(View.VISIBLE);
            btnLogin.setText(R.string.aanmaken);
        } else {
            controleerBiometrie();
        }

        btnLogin.setOnClickListener(v -> doeLogin());
        btnBiometrie.setOnClickListener(v -> startBiometriePrompt());

        etHerhaal.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) { doeLogin(); return true; }
            return false;
        });
        etWachtwoord.setOnEditorActionListener((v, actionId, event) -> {
            if (!isNieuw && actionId == EditorInfo.IME_ACTION_DONE) { doeLogin(); return true; }
            return false;
        });
    }

    private void controleerBiometrie() {
        BiometricManager bm = BiometricManager.from(this);
        boolean beschikbaar = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                == BiometricManager.BIOMETRIC_SUCCESS;
        if (beschikbaar && kluis.isBiometrieOpgeslagen()) {
            layoutBiometrie.setVisibility(View.VISIBLE);
        }
    }

    private void startBiometriePrompt() {
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometrie_titel))
            .setSubtitle(getString(R.string.biometrie_subtitel))
            .setNegativeButtonText(getString(R.string.annuleren))
            .build();

        BiometricPrompt prompt = new BiometricPrompt(this,
            ContextCompat.getMainExecutor(this),
            new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    String token = kluis.getBiometrieToken();
                    if (token != null && kluis.open(token)) {
                        gaVerder();
                    } else {
                        Toast.makeText(LoginActivity.this,
                            R.string.biometrie_mislukt, Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onAuthenticationError(int code, @NonNull CharSequence msg) {
                    if (code != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        code != BiometricPrompt.ERROR_USER_CANCELED) {
                        Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onAuthenticationFailed() {
                    Toast.makeText(LoginActivity.this,
                        R.string.biometrie_mislukt, Toast.LENGTH_SHORT).show();
                }
            });
        prompt.authenticate(info);
    }

    private void doeLogin() {
        String pw = etWachtwoord.getText() != null ? etWachtwoord.getText().toString() : "";
        if (pw.isEmpty()) {
            Toast.makeText(this, "Vul een wachtwoord in", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (isNieuw) {
                String pw2 = etHerhaal.getText() != null ? etHerhaal.getText().toString() : "";
                if (!pw.equals(pw2)) {
                    Toast.makeText(this, getString(R.string.fout_herhaal), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (pw.length() < 8) {
                    Toast.makeText(this, getString(R.string.fout_kort), Toast.LENGTH_SHORT).show();
                    return;
                }
                kluis.maakNieuw(pw);
            } else {
                if (!kluis.open(pw)) {
                    Toast.makeText(this, getString(R.string.fout_wachtwoord), Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            gaVerder();
        } catch (Exception e) {
            Toast.makeText(this, "Fout: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void gaVerder() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_left);
        finish();
    }
}
