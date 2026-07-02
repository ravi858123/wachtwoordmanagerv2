package nl.wachtwoordmanager;

import android.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;

/**
 * RFC 6238 TOTP / RFC 4226 HOTP implementatie.
 * Geen externe afhankelijkheden.
 */
public class TotpGenerator {
    private static final int DIGITS   = 6;
    private static final int PERIODE  = 30;
    private static final long[] MACHT = {1,10,100,1_000,10_000,100_000,1_000_000};

    /** Genereer TOTP-code voor het opgegeven tijdstip (ms). */
    public static String genereer(String base32Geheim, long tijdstipMs) throws Exception {
        byte[] sleutel = base32Decodeer(base32Geheim.trim().toUpperCase());
        long teller = tijdstipMs / 1000 / PERIODE;
        byte[] bericht = ByteBuffer.allocate(8).putLong(teller).array();

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(sleutel, "RAW"));
        byte[] hmac = mac.doFinal(bericht);

        int offset = hmac[hmac.length - 1] & 0x0F;
        long code = ((hmac[offset]     & 0x7F) << 24)
                  | ((hmac[offset + 1] & 0xFF) << 16)
                  | ((hmac[offset + 2] & 0xFF) <<  8)
                  |  (hmac[offset + 3] & 0xFF);

        long otp = code % MACHT[DIGITS];
        return String.format("%0" + DIGITS + "d", otp);
    }

    /** Seconden tot vervaldatum van de huidige code. */
    public static int secondenTotVerval() {
        long nu = System.currentTimeMillis() / 1000;
        return PERIODE - (int)(nu % PERIODE);
    }

    /** Eenvoudige Base32-decoder (A–Z, 2–7). */
    private static byte[] base32Decodeer(String invoer) {
        // Verwijder padding
        invoer = invoer.replaceAll("=", "");
        int n = invoer.length();
        byte[] resultaat = new byte[n * 5 / 8];
        long buffer = 0;
        int bits = 0;
        int idx = 0;
        for (char c : invoer.toCharArray()) {
            int waarde;
            if (c >= 'A' && c <= 'Z') waarde = c - 'A';
            else if (c >= '2' && c <= '7') waarde = c - '2' + 26;
            else throw new IllegalArgumentException("Ongeldig Base32-teken: " + c);
            buffer = (buffer << 5) | waarde;
            bits += 5;
            if (bits >= 8) {
                bits -= 8;
                resultaat[idx++] = (byte)((buffer >> bits) & 0xFF);
            }
        }
        return resultaat;
    }

    /** Controleert of een Base32-string geldig is. */
    public static boolean isGeldig(String geheim) {
        if (geheim == null || geheim.trim().isEmpty()) return false;
        try { base32Decodeer(geheim.trim().toUpperCase()); return true; }
        catch (Exception e) { return false; }
    }
}
