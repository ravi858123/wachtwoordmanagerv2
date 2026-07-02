package nl.wachtwoordmanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.VH> {

    public interface OnItemClick { void onClick(String id); }
    public interface OnFavorietToggle { void onToggle(String id, WachtwoordItem item); }

    private final Kluis kluis;
    private final OnItemClick clickListener;
    private final OnFavorietToggle favorietListener;
    private List<Map.Entry<String, WachtwoordItem>> gefilterd = new ArrayList<>();
    private String zoekterm = "";
    private String categorieFilter = "alle";

    public ItemAdapter(Kluis kluis, OnItemClick clickListener, OnFavorietToggle favorietListener) {
        this.kluis = kluis;
        this.clickListener = clickListener;
        this.favorietListener = favorietListener;
        herlaad();
    }

    public void herlaad() {
        filter(zoekterm, categorieFilter);
    }

    public void filter(String term, String cat) {
        zoekterm = term.toLowerCase();
        categorieFilter = cat;
        gefilterd = new ArrayList<>();
        for (Map.Entry<String, WachtwoordItem> e : kluis.getItems().entrySet()) {
            WachtwoordItem item = e.getValue();
            if (!matchesZoek(item)) continue;
            if (!matchesCategorie(item)) continue;
            gefilterd.add(e);
        }
        // Favorieten bovenaan, daarna alfabetisch op naam
        gefilterd.sort((a, b) -> {
            if (a.getValue().favoriet != b.getValue().favoriet)
                return a.getValue().favoriet ? -1 : 1;
            String nA = a.getValue().naam != null ? a.getValue().naam : "";
            String nB = b.getValue().naam != null ? b.getValue().naam : "";
            return nA.compareToIgnoreCase(nB);
        });
        notifyDataSetChanged();
    }

    private boolean matchesZoek(WachtwoordItem item) {
        if (zoekterm.isEmpty()) return true;
        return (item.naam != null && item.naam.toLowerCase().contains(zoekterm))
            || (item.gebruikersnaam != null && item.gebruikersnaam.toLowerCase().contains(zoekterm))
            || (item.url != null && item.url.toLowerCase().contains(zoekterm));
    }

    private boolean matchesCategorie(WachtwoordItem item) {
        switch (categorieFilter) {
            case "alle": return true;
            case "favorieten": return item.favoriet;
            default:
                String cat = item.categorie != null ? item.categorie : "algemeen";
                return cat.equals(categorieFilter);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wachtwoord, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Map.Entry<String, WachtwoordItem> entry = gefilterd.get(position);
        String id = entry.getKey();
        WachtwoordItem item = entry.getValue();

        holder.tvNaam.setText(item.naam != null ? item.naam : "Naamloos");
        holder.tvGebruiker.setText(item.gebruikersnaam != null ? item.gebruikersnaam : "");
        AvatarHulp.stelIn(holder.tvInitiaal, item.naam);

        holder.ivFavoriet.setImageResource(item.favoriet
            ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);

        holder.itemView.setOnClickListener(v -> clickListener.onClick(id));
        holder.ivFavoriet.setOnClickListener(v -> {
            item.favoriet = !item.favoriet;
            try { kluis.slaItemOp(id, item); } catch (Exception ignored) {}
            holder.ivFavoriet.setImageResource(item.favoriet
                ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
            if (favorietListener != null) favorietListener.onToggle(id, item);
            herlaad();
        });
    }

    @Override
    public int getItemCount() { return gefilterd.size(); }

    public int getTotaalAantal() { return gefilterd.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvNaam, tvGebruiker, tvInitiaal;
        ImageView ivFavoriet;
        VH(View v) {
            super(v);
            tvNaam      = v.findViewById(R.id.tvNaam);
            tvGebruiker = v.findViewById(R.id.tvGebruikersnaam);
            tvInitiaal  = v.findViewById(R.id.tvInitiaal);
            ivFavoriet  = v.findViewById(R.id.ivFavoriet);
        }
    }
}
