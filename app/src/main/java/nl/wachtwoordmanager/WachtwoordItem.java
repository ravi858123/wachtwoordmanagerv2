package nl.wachtwoordmanager;

public class WachtwoordItem {
    public String naam;
    public String gebruikersnaam;
    public String wachtwoord;
    public String url;
    public String notities;
    public String categorie;   // algemeen / werk / sociaal / financieel / overig
    public String totpGeheim;  // Base32 TOTP secret, nullable
    public boolean favoriet;
    public long aangemaakt;    // epoch ms
    public long gewijzigd;     // epoch ms

    public WachtwoordItem() {}

    public WachtwoordItem(String naam, String gebruikersnaam, String wachtwoord,
                          String url, String notities) {
        this.naam = naam;
        this.gebruikersnaam = gebruikersnaam;
        this.wachtwoord = wachtwoord;
        this.url = url;
        this.notities = notities;
        this.categorie = "algemeen";
        this.favoriet = false;
        this.aangemaakt = System.currentTimeMillis();
        this.gewijzigd  = aangemaakt;
    }

    public WachtwoordItem(String naam, String gebruikersnaam, String wachtwoord,
                          String url, String notities, String categorie,
                          String totpGeheim, boolean favoriet) {
        this(naam, gebruikersnaam, wachtwoord, url, notities);
        this.categorie = categorie != null ? categorie : "algemeen";
        this.totpGeheim = totpGeheim;
        this.favoriet = favoriet;
    }
}
