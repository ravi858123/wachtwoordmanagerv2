package nl.wachtwoordmanager;

public class WachtwoordSterkte {
    public static final int ZEER_ZWAK  = 0;
    public static final int ZWAK       = 1;
    public static final int REDELIJK   = 2;
    public static final int STERK      = 3;
    public static final int ZEER_STERK = 4;

    public final int niveau;
    public final double entropie;

    public WachtwoordSterkte(int niveau, double entropie) {
        this.niveau = niveau;
        this.entropie = entropie;
    }

    public static WachtwoordSterkte analyseer(String pw) {
        if (pw == null || pw.isEmpty()) return new WachtwoordSterkte(ZEER_ZWAK, 0);

        boolean heeftKlein  = pw.matches(".*[a-z].*");
        boolean heeftGroot  = pw.matches(".*[A-Z].*");
        boolean heeftCijfer = pw.matches(".*\\d.*");
        boolean heeftSym    = pw.matches(".*[^a-zA-Z0-9].*");

        int poolGrootte = 0;
        if (heeftKlein)  poolGrootte += 26;
        if (heeftGroot)  poolGrootte += 26;
        if (heeftCijfer) poolGrootte += 10;
        if (heeftSym)    poolGrootte += 32;

        if (poolGrootte == 0) poolGrootte = 26;

        double entropie = pw.length() * (Math.log(poolGrootte) / Math.log(2));

        int niveau;
        if      (entropie < 28) niveau = ZEER_ZWAK;
        else if (entropie < 40) niveau = ZWAK;
        else if (entropie < 60) niveau = REDELIJK;
        else if (entropie < 80) niveau = STERK;
        else                    niveau = ZEER_STERK;

        return new WachtwoordSterkte(niveau, entropie);
    }
}
