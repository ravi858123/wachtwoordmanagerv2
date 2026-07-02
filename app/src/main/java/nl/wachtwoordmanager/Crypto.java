package nl.wachtwoordmanager;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;

/**
 * AES-256-GCM versleuteling met PBKDF2-HMAC-SHA256 sleutelafleiding.
 * Formaat: Base64(IV [12 bytes] || ciphertekst+tag)
 */
public class Crypto {
    private static final int ITERATIES  = 310_000; // OWASP 2023 aanbeveling
    private static final int SLEUTEL_BITS = 256;
    private static final int IV_BYTES   = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final String ALGORITME = "AES/GCM/NoPadding";
    private static final String PBE_ALGO  = "PBKDF2WithHmacSHA256";

    public static byte[] nieuweSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    private static SecretKey leidSleutelAf(String wachtwoord, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(wachtwoord.toCharArray(), salt, ITERATIES, SLEUTEL_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBE_ALGO);
        byte[] sleutelBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(sleutelBytes, "AES");
    }

    public static String versleutel(String tekst, String wachtwoord, byte[] salt) throws Exception {
        byte[] iv = new byte[IV_BYTES];
        new SecureRandom().nextBytes(iv);

        SecretKey sleutel = leidSleutelAf(wachtwoord, salt);
        Cipher cipher = Cipher.getInstance(ALGORITME);
        cipher.init(Cipher.ENCRYPT_MODE, sleutel, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] versleuteld = cipher.doFinal(tekst.getBytes(StandardCharsets.UTF_8));

        // Combineer IV + ciphertekst
        byte[] resultaat = new byte[IV_BYTES + versleuteld.length];
        System.arraycopy(iv, 0, resultaat, 0, IV_BYTES);
        System.arraycopy(versleuteld, 0, resultaat, IV_BYTES, versleuteld.length);
        return Base64.encodeToString(resultaat, Base64.NO_WRAP);
    }

    public static String ontsleutel(String base64Data, String wachtwoord, byte[] salt) throws Exception {
        byte[] data = Base64.decode(base64Data, Base64.NO_WRAP);
        if (data.length < IV_BYTES) throw new Exception("Corrupte data");

        byte[] iv = new byte[IV_BYTES];
        byte[] cipherTekst = new byte[data.length - IV_BYTES];
        System.arraycopy(data, 0, iv, 0, IV_BYTES);
        System.arraycopy(data, IV_BYTES, cipherTekst, 0, cipherTekst.length);

        SecretKey sleutel = leidSleutelAf(wachtwoord, salt);
        Cipher cipher = Cipher.getInstance(ALGORITME);
        cipher.init(Cipher.DECRYPT_MODE, sleutel, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] klaarTekst = cipher.doFinal(cipherTekst);
        return new String(klaarTekst, StandardCharsets.UTF_8);
    }
}
