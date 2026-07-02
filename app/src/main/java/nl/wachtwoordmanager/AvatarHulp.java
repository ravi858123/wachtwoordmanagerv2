package nl.wachtwoordmanager;

import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.widget.TextView;

/** Genereert een gekleurde cirkel met een initiaal op basis van de naam. */
public class AvatarHulp {
    private static final int[] KLEUREN = {
        0xFF4F46E5, 0xFF0EA5A4, 0xFFDB2777, 0xFFD97706,
        0xFF16A34A, 0xFF7C3AED, 0xFF0284C7, 0xFFE11D48
    };

    public static void stelIn(TextView tv, String naam) {
        String initiaal = (naam != null && !naam.isEmpty())
            ? String.valueOf(naam.charAt(0)).toUpperCase() : "?";
        int kleur = KLEUREN[Math.abs(naam != null ? naam.hashCode() : 0) % KLEUREN.length];
        ShapeDrawable cirkel = new ShapeDrawable(new OvalShape());
        cirkel.getPaint().setColor(kleur);
        tv.setBackground(cirkel);
        tv.setText(initiaal);
    }
}
