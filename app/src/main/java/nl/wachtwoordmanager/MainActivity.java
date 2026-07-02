package nl.wachtwoordmanager;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {

    private ItemAdapter adapter;
    private Kluis kluis;
    private String huidigCatFilter = "alle";
    private String verwijderdId;
    private WachtwoordItem verwijderdItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.pasThemaToe(this);
        App.pasSchermBeveiligingToe(this);
        setContentView(R.layout.activity_main);

        kluis = App.getKluis();

        RecyclerView rv = findViewById(R.id.rvItems);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ItemAdapter(kluis,
            id -> {
                Intent i = new Intent(this, ItemActivity.class);
                i.putExtra("id", id);
                startActivity(i);
            },
            (id, item) -> updateLeegStaat()
        );
        rv.setAdapter(adapter);

        // Zoeken
        TextInputEditText etZoeken = findViewById(R.id.etZoeken);
        etZoeken.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                adapter.filter(s.toString(), huidigCatFilter);
                updateLeegStaat();
            }
            public void afterTextChanged(Editable s) {}
        });

        // Filter chips
        ChipGroup chips = findViewById(R.id.chipGroupFilter);
        chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if      (id == R.id.chipAlle)       huidigCatFilter = "alle";
            else if (id == R.id.chipFavorieten) huidigCatFilter = "favorieten";
            else if (id == R.id.chipWerk)       huidigCatFilter = "werk";
            else if (id == R.id.chipSociaal)    huidigCatFilter = "sociaal";
            else if (id == R.id.chipFinancieel) huidigCatFilter = "financieel";
            String zoek = etZoeken.getText() != null ? etZoeken.getText().toString() : "";
            adapter.filter(zoek, huidigCatFilter);
            updateLeegStaat();
        });

        // Vergrendelen
        findViewById(R.id.btnVergrendel).setOnClickListener(v -> {
            kluis.vergrendel();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // Instellingen
        findViewById(R.id.btnInstellingen).setOnClickListener(v ->
            startActivity(new Intent(this, SettingsActivity.class)));

        // Nieuw item
        ExtendedFloatingActionButton fab = findViewById(R.id.fabNieuw);
        fab.setOnClickListener(v -> {
            Intent i = new Intent(this, ItemActivity.class);
            i.putExtra("id", (String) null);
            startActivity(i);
        });

        // FAB krimpen bij scrollen
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@androidx.annotation.NonNull RecyclerView r, int dx, int dy) {
                if (dy > 8) fab.shrink(); else if (dy < -8) fab.extend();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.herlaad();
        updateLeegStaat();
    }

    private void updateLeegStaat() {
        LinearLayout layoutLeeg = findViewById(R.id.layoutLeeg);
        RecyclerView rv = findViewById(R.id.rvItems);
        boolean leeg = adapter.getTotaalAantal() == 0;
        layoutLeeg.setVisibility(leeg ? View.VISIBLE : View.GONE);
        rv.setVisibility(leeg ? View.GONE : View.VISIBLE);
    }
}
