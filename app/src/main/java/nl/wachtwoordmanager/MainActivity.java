package nl.wachtwoordmanager;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final List<String[]> FILTERS = Arrays.asList(
        new String[]{"alle",       "Alle"},
        new String[]{"favorieten", "Favorieten"},
        new String[]{"werk",       "Werk"},
        new String[]{"sociaal",    "Sociaal"},
        new String[]{"financieel", "Financieel"}
    );

    private ItemAdapter adapter;
    private Kluis kluis;
    private String huidigFilter = "alle";
    private LinearLayout layoutFilterTabs;
    private MaterialCardView cardLijst;
    private RecyclerView rvItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.pasThemaToe(this);
        App.pasSchermBeveiligingToe(this);
        setContentView(R.layout.activity_main);

        kluis = App.getKluis();

        rvItems = findViewById(R.id.rvItems);
        cardLijst = findViewById(R.id.cardLijst);
        layoutFilterTabs = findViewById(R.id.layoutFilterTabs);

        // Gebruik RecyclerView (eenvoudigst en meest robuust)
        rvItems.setVisibility(View.VISIBLE);
        rvItems.setNestedScrollingEnabled(false);
        rvItems.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ItemAdapter(kluis,
            id -> {
                Intent i = new Intent(this, ItemActivity.class);
                i.putExtra("id", id);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                startActivity(i);
            },
            (id, item) -> updateLeegStaat()
        );
        rvItems.setAdapter(adapter);

        // Animatie op lijst-items
        rvItems.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());

        // Filter-tabs bouwen
        bouwFilterTabs();

        // Zoeken
        TextInputEditText etZoeken = findViewById(R.id.etZoeken);
        etZoeken.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                adapter.filter(s.toString(), huidigFilter);
                updateLeegStaat();
            }
            public void afterTextChanged(Editable s) {}
        });

        // Vergrendelen
        findViewById(R.id.btnVergrendel).setOnClickListener(v -> {
            kluis.vergrendel();
            Intent i = new Intent(this, LoginActivity.class);
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
            finish();
        });

        // Instellingen
        findViewById(R.id.btnInstellingen).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // FAB
        ExtendedFloatingActionButton fab = findViewById(R.id.fabNieuw);
        fab.setOnClickListener(v -> {
            Intent i = new Intent(this, ItemActivity.class);
            i.putExtra("id", (String) null);
            startActivity(i);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // FAB shrink on scroll
        rvItems.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(RecyclerView r, int dx, int dy) {
                if (dy > 8) fab.shrink(); else if (dy < -8) fab.extend();
            }
        });
    }

    private void bouwFilterTabs() {
        layoutFilterTabs.removeAllViews();
        for (String[] filter : FILTERS) {
            String id = filter[0], label = filter[1];
            TextView tab = new TextView(this);
            int horizontaal = dpToPx(16);
            int verticaal = dpToPx(8);
            tab.setPadding(horizontaal, verticaal, horizontaal, verticaal);
            tab.setText(label);
            tab.setTextSize(13f);
            tab.setSingleLine(true);
            tab.setTypeface(null, android.graphics.Typeface.BOLD);
            tab.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
            ((LinearLayout.LayoutParams) tab.getLayoutParams())
                .setMarginEnd(dpToPx(6));

            updateTabStijl(tab, id.equals(huidigFilter));

            tab.setOnClickListener(v -> {
                huidigFilter = id;
                updateAlleTabStijlen();
                String zoek = ((TextInputEditText) findViewById(R.id.etZoeken))
                    .getText() != null ? ((TextInputEditText) findViewById(R.id.etZoeken))
                    .getText().toString() : "";
                adapter.filter(zoek, huidigFilter);
                updateLeegStaat();
            });

            tab.setTag(id);
            layoutFilterTabs.addView(tab);
        }
    }

    private void updateAlleTabStijlen() {
        for (int i = 0; i < layoutFilterTabs.getChildCount(); i++) {
            View child = layoutFilterTabs.getChildAt(i);
            if (child instanceof TextView) {
                String tag = (String) child.getTag();
                updateTabStijl((TextView) child, tag.equals(huidigFilter));
            }
        }
    }

    private void updateTabStijl(TextView tab, boolean actief) {
        if (actief) {
            tab.setBackgroundResource(R.drawable.bg_chip_actief);
            tab.setTextColor(getResources().getColor(R.color.accent, getTheme()));
        } else {
            tab.setBackgroundResource(R.drawable.bg_chip_inactief);
            tab.setTextColor(getResources().getColor(R.color.tekst_secundair, getTheme()));
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!kluis.isOntgrendeld()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        adapter.herlaad();
        updateLeegStaat();
    }

    private void updateLeegStaat() {
        LinearLayout layoutLeeg = findViewById(R.id.layoutLeeg);
        boolean leeg = adapter.getTotaalAantal() == 0;
        layoutLeeg.setVisibility(leeg ? View.VISIBLE : View.GONE);
        rvItems.setVisibility(leeg ? View.GONE : View.VISIBLE);
    }
}
