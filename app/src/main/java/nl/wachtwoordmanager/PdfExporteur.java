package nl.wachtwoordmanager;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfDocument.Page;
import android.graphics.pdf.PdfDocument.PageInfo;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.pdf.PrintedPdfDocument;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Genereert een opgemaakte PDF van alle wachtwoorden.
 * Ondersteunt zowel opslaan als direct printen via PrintManager.
 */
public class PdfExporteur extends PrintDocumentAdapter {

    // A4 bij 72 DPI (standaard PDF-punten)
    private static final int PAGINA_BREEDTE  = 595;
    private static final int PAGINA_HOOGTE   = 842;
    private static final int MARGE           = 48;
    private static final int KOLOM_BREEDTE   = PAGINA_BREEDTE - 2 * MARGE;
    private static final int REGEL_HOOGTE    = 18;
    private static final int ITEM_PADDING    = 12;
    private static final int ITEM_HOOGTE_MIN = 80;

    // Kleuren
    private static final int KLEUR_ACCENT     = 0xFF5B4FCF;
    private static final int KLEUR_DONKER     = 0xFF16132E;
    private static final int KLEUR_SECUNDAIR  = 0xFF6E6B85;
    private static final int KLEUR_RAND       = 0xFFE8E6F0;
    private static final int KLEUR_ACHTERGROND = 0xFFF6F5FB;
    private static final int KLEUR_RIJ_EVEN   = 0xFFFFFFFF;
    private static final int KLEUR_RIJ_ONEVEN = 0xFFF8F7FD;
    private static final int KLEUR_ROOD_BG    = 0xFFFFF0F0;
    private static final int KLEUR_ROOD_RAND  = 0xFFFFCDD2;
    private static final int KLEUR_WIT        = Color.WHITE;

    private final Context context;
    private final Kluis kluis;
    private PrintedPdfDocument pdfDocument;
    private int aantalPaginas;

    // Paints
    private final Paint pAccent   = makePaint(KLEUR_ACCENT, 22, true);
    private final Paint pTitel    = makePaint(KLEUR_DONKER, 10, true);
    private final Paint pLabel    = makePaint(KLEUR_SECUNDAIR, 8, false);
    private final Paint pWaarde   = makePaint(KLEUR_DONKER, 9, false);
    private final Paint pWachtwoord = makeMonoPaint(KLEUR_DONKER, 8.5f);
    private final Paint pPaginaNr = makePaint(KLEUR_SECUNDAIR, 7.5f, false);
    private final Paint pVulling  = new Paint();
    private final Paint pRand     = new Paint();
    private final Paint pWaarschuwing = makePaint(Color.parseColor("#B91C1C"), 7.5f, false);

    public PdfExporteur(Context context, Kluis kluis) {
        this.context = context;
        this.kluis   = kluis;
    }

    // ── PrintDocumentAdapter overrides ──────────────────────────────────────

    @Override
    public void onLayout(PrintAttributes oldAttr, PrintAttributes newAttr,
                         CancellationSignal cancel, LayoutResultCallback callback,
                         Bundle metadata) {
        pdfDocument = new PrintedPdfDocument(context, newAttr);
        if (cancel.isCanceled()) { callback.onLayoutCancelled(); return; }

        aantalPaginas = berekenAantalPaginas(newAttr);
        PrintDocumentInfo info = new PrintDocumentInfo.Builder("wachtwoorden.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(aantalPaginas)
            .build();
        callback.onLayoutFinished(info, !newAttr.equals(oldAttr));
    }

    @Override
    public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                        CancellationSignal cancel, WriteResultCallback callback) {
        List<Map.Entry<String, WachtwoordItem>> items = new ArrayList<>(kluis.getItems().entrySet());
        int paginaIndex = 0;
        int itemIndex = 0;
        List<PageRange> geschrevenPaginas = new ArrayList<>();

        while (itemIndex < items.size() || paginaIndex == 0) {
            if (cancel.isCanceled()) { callback.onWriteCancelled(); return; }

            Page pagina = pdfDocument.startPage(
                new PageInfo.Builder(PAGINA_BREEDTE, PAGINA_HOOGTE, paginaIndex).create());
            Canvas canvas = pagina.getCanvas();

            tekenPaginaAchtergrond(canvas);
            int y = tekenKoptekst(canvas, paginaIndex == 0);
            if (paginaIndex == 0) y = tekenWaarschuwing(canvas, y);

            while (itemIndex < items.size()) {
                Map.Entry<String, WachtwoordItem> entry = items.get(itemIndex);
                int benodigdHoogte = berekenItemHoogte(entry.getValue());
                if (y + benodigdHoogte > PAGINA_HOOGTE - MARGE - 30) break;
                y = tekenItem(canvas, entry.getValue(), y, itemIndex);
                itemIndex++;
            }

            tekenVoettekst(canvas, paginaIndex + 1, aantalPaginas);
            pdfDocument.finishPage(pagina);
            geschrevenPaginas.add(new PageRange(paginaIndex, paginaIndex));
            paginaIndex++;

            if (itemIndex >= items.size()) break;
        }

        try {
            pdfDocument.writeTo(new FileOutputStream(destination.getFileDescriptor()));
            callback.onWriteFinished(geschrevenPaginas.toArray(new PageRange[0]));
        } catch (IOException e) {
            callback.onWriteFailed(e.getMessage());
        } finally {
            pdfDocument.close();
        }
    }

    // ── Directe PDF-naar-bestand export ─────────────────────────────────────

    public void schrijfNaarBestand(FileOutputStream fos) throws IOException {
        PdfDocument doc = new PdfDocument();
        List<Map.Entry<String, WachtwoordItem>> items = new ArrayList<>(kluis.getItems().entrySet());

        int paginaIndex = 0;
        int itemIndex = 0;

        do {
            Page pagina = doc.startPage(
                new PageInfo.Builder(PAGINA_BREEDTE, PAGINA_HOOGTE, paginaIndex).create());
            Canvas canvas = pagina.getCanvas();

            tekenPaginaAchtergrond(canvas);
            int y = tekenKoptekst(canvas, paginaIndex == 0);
            if (paginaIndex == 0) y = tekenWaarschuwing(canvas, y);

            while (itemIndex < items.size()) {
                int benodigdHoogte = berekenItemHoogte(items.get(itemIndex).getValue());
                if (y + benodigdHoogte > PAGINA_HOOGTE - MARGE - 30) break;
                y = tekenItem(canvas, items.get(itemIndex).getValue(), y, itemIndex);
                itemIndex++;
            }

            tekenVoettekst(canvas, paginaIndex + 1, -1);
            doc.finishPage(pagina);
            paginaIndex++;
        } while (itemIndex < items.size());

        doc.writeTo(fos);
        doc.close();
    }

    // ── Teken-methoden ───────────────────────────────────────────────────────

    private void tekenPaginaAchtergrond(Canvas c) {
        pVulling.setColor(KLEUR_WIT);
        c.drawRect(0, 0, PAGINA_BREEDTE, PAGINA_HOOGTE, pVulling);
    }

    private int tekenKoptekst(Canvas c, boolean eersteP) {
        // Paars koptekst-blok
        pVulling.setColor(KLEUR_ACCENT);
        c.drawRect(0, 0, PAGINA_BREEDTE, eersteP ? 72 : 48, pVulling);

        Paint pWit = makePaint(KLEUR_WIT, eersteP ? 18 : 12, true);
        c.drawText("Wachtwoordmanager", MARGE, eersteP ? 34 : 28, pWit);

        if (eersteP) {
            Paint pSub = makePaint(0xCCFFFFFF, 9, false);
            String datum = new SimpleDateFormat("d MMMM yyyy 'om' HH:mm", new Locale("nl"))
                .format(new Date());
            c.drawText("Geëxporteerd op " + datum, MARGE, 52, pSub);
            c.drawText(kluis.getItems().size() + " items", MARGE, 64, pSub);
        }

        // Paginanummer rechts
        Paint pWitKlein = makePaint(0xAAFFFFFF, 8, false);
        pWitKlein.setTextAlign(Align.RIGHT);
        c.drawText("Vertrouwelijk", PAGINA_BREEDTE - MARGE, eersteP ? 34 : 28, pWitKlein);

        return (eersteP ? 72 : 48) + 16;
    }

    private int tekenWaarschuwing(Canvas c, int y) {
        // Rode waarschuwingsbox
        pVulling.setColor(KLEUR_ROOD_BG);
        pRand.setColor(KLEUR_ROOD_RAND);
        pRand.setStyle(Style.STROKE);
        pRand.setStrokeWidth(1f);
        RectF box = new RectF(MARGE, y, MARGE + KOLOM_BREEDTE, y + 36);
        c.drawRoundRect(box, 6, 6, pVulling);
        c.drawRoundRect(box, 6, 6, pRand);

        Paint pRoodBold = makePaint(Color.parseColor("#B91C1C"), 8.5f, true);
        c.drawText("⚠  VERTROUWELIJK DOCUMENT", MARGE + 10, y + 13, pRoodBold);
        c.drawText("Bewaar dit bestand veilig. Vernietig het na gebruik. Deel het nooit via e-mail of cloud.",
            MARGE + 10, y + 26, pWaarschuwing);

        return y + 36 + 12;
    }

    private int tekenItem(Canvas c, WachtwoordItem item, int y, int index) {
        int hoogte = berekenItemHoogte(item);

        // Achtergrond (zebra)
        pVulling.setColor(index % 2 == 0 ? KLEUR_RIJ_EVEN : KLEUR_RIJ_ONEVEN);
        RectF bg = new RectF(MARGE, y, MARGE + KOLOM_BREEDTE, y + hoogte);
        c.drawRoundRect(bg, 8, 8, pVulling);

        // Linker kleur-balk gebaseerd op categorie
        pVulling.setColor(categorieKleur(item.categorie));
        c.drawRoundRect(new RectF(MARGE, y, MARGE + 4, y + hoogte), 2, 2, pVulling);

        // Rand
        pRand.setColor(KLEUR_RAND);
        pRand.setStyle(Style.STROKE);
        pRand.setStrokeWidth(0.75f);
        c.drawRoundRect(bg, 8, 8, pRand);

        int ix = MARGE + 14;
        int iy = y + ITEM_PADDING + 2;

        // Naam (groot en vet)
        Paint pNaam = makePaint(KLEUR_DONKER, 11, true);
        c.drawText(veiligeTekst(item.naam, "Naamloos"), ix, iy + 9, pNaam);

        // Categorie-badge rechts
        if (item.categorie != null && !item.categorie.isEmpty()) {
            Paint pBadge = makePaint(categorieKleur(item.categorie), 7, false);
            pBadge.setTextAlign(Align.RIGHT);
            c.drawText(item.categorie.toUpperCase(), MARGE + KOLOM_BREEDTE - 10, iy + 9, pBadge);
        }

        iy += REGEL_HOOGTE;

        // Velden
        iy = tekenVeld(c, "Gebruikersnaam", item.gebruikersnaam, ix, iy, false);
        iy = tekenVeld(c, "Wachtwoord",     item.wachtwoord,     ix, iy, true);
        if (item.url != null && !item.url.isEmpty())
            iy = tekenVeld(c, "Website", item.url, ix, iy, false);
        if (item.notities != null && !item.notities.isEmpty())
            iy = tekenVeld(c, "Notities", item.notities, ix, iy, false);
        if (item.totpGeheim != null && !item.totpGeheim.isEmpty())
            iy = tekenVeld(c, "2FA sleutel", item.totpGeheim, ix, iy, true);

        return y + hoogte + 6;
    }

    private int tekenVeld(Canvas c, String label, String waarde, int x, int y, boolean mono) {
        if (waarde == null || waarde.isEmpty()) return y;
        c.drawText(label + ":", x, y, pLabel);
        Paint p = mono ? pWachtwoord : pWaarde;
        c.drawText(veiligeTekst(waarde, ""), x + 90, y, p);
        return y + REGEL_HOOGTE;
    }

    private void tekenVoettekst(Canvas c, int pagina, int totaal) {
        String tekst = totaal > 0
            ? "Wachtwoordmanager Export  •  Pagina " + pagina + " van " + totaal
            : "Wachtwoordmanager Export  •  Pagina " + pagina;
        pPaginaNr.setTextAlign(Align.CENTER);
        c.drawText(tekst, PAGINA_BREEDTE / 2f, PAGINA_HOOGTE - 20, pPaginaNr);

        // Scheidingslijn
        pRand.setColor(KLEUR_RAND);
        pRand.setStyle(Style.STROKE);
        pRand.setStrokeWidth(0.5f);
        c.drawLine(MARGE, PAGINA_HOOGTE - 30, PAGINA_BREEDTE - MARGE, PAGINA_HOOGTE - 30, pRand);
    }

    // ── Hulpmethoden ────────────────────────────────────────────────────────

    private int berekenItemHoogte(WachtwoordItem item) {
        int regels = 3; // naam + gebruiker + wachtwoord
        if (item.url != null && !item.url.isEmpty()) regels++;
        if (item.notities != null && !item.notities.isEmpty()) regels++;
        if (item.totpGeheim != null && !item.totpGeheim.isEmpty()) regels++;
        return Math.max(ITEM_HOOGTE_MIN, ITEM_PADDING * 2 + regels * REGEL_HOOGTE + 4);
    }

    private int berekenAantalPaginas(PrintAttributes attrs) {
        // Ruwe schatting
        List<Map.Entry<String, WachtwoordItem>> items = new ArrayList<>(kluis.getItems().entrySet());
        int beschikbaar = PAGINA_HOOGTE - MARGE - 72 - 50 - 30; // koptekst + waarschuwing + voet
        int paginas = 1;
        int y = 0;
        for (Map.Entry<String, WachtwoordItem> e : items) {
            int h = berekenItemHoogte(e.getValue()) + 6;
            if (y + h > beschikbaar) { paginas++; y = 0; beschikbaar = PAGINA_HOOGTE - 48 - 50; }
            y += h;
        }
        return paginas;
    }

    private int categorieKleur(String cat) {
        if (cat == null) return KLEUR_ACCENT;
        switch (cat) {
            case "werk":      return 0xFF1D4ED8;
            case "sociaal":   return 0xFFBE185D;
            case "financieel":return 0xFF15803D;
            case "overig":    return 0xFF6B7280;
            default:          return KLEUR_ACCENT;
        }
    }

    private String veiligeTekst(String s, String standaard) {
        return (s != null && !s.isEmpty()) ? s : standaard;
    }

    private static Paint makePaint(int kleur, float textSizeSp, boolean vet) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(kleur);
        p.setTextSize(textSizeSp * 1.33f); // sp → punten (72 DPI)
        if (vet) p.setTypeface(Typeface.DEFAULT_BOLD);
        return p;
    }

    private static Paint makeMonoPaint(int kleur, float textSizeSp) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(kleur);
        p.setTextSize(textSizeSp * 1.33f);
        p.setTypeface(Typeface.MONOSPACE);
        return p;
    }
}
