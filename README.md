# 🔐 Wachtwoordmanager v2.0

Een volledig vernieuwde, professionele Android wachtwoordmanager — opgebouwd van de grond af met veiligheid en gebruiksgemak als kern.

---

## 🆕 Nieuw in v2.0 (t.o.v. v1.0)

| Functie | v1.0 | v2.0 |
|---|---|---|
| Versleuteling | Onveilig / geen | AES-256-GCM + PBKDF2 (310k iteraties) |
| Thema | Alleen licht | Licht / Donker / Systeem |
| Wachtwoordgenerator | Simpel | Lengte 8–64, symbolen, ambigue-filter |
| Sterkte-meter | ❌ | ✅ Live entropie in bits |
| 2FA / TOTP | ❌ | ✅ Ingebouwd (RFC 6238) |
| Favorieten | ❌ | ✅ |
| Categorieën | ❌ | ✅ 5 categorieën + filter chips |
| Biometrische login | ❌ | ✅ Vingerafdruk |
| Auto-vergrendeling | ❌ | ✅ Instelbaar (0–15 min / nooit) |
| Klembord auto-wissen | ❌ | ✅ Instelbaar (10–60s / nooit) |
| Screenshot blokkade | ❌ | ✅ FLAG_SECURE |
| Versleutelde back-up | ❌ | ✅ Export/import met eigen wachtwoord |
| Hoofdwachtwoord wijzigen | ❌ | ✅ |
| Design | Basic | Material 3 + donker thema |
| Gekleurde avatars | ❌ | ✅ Deterministisch op naam |

---

## 🛡️ Beveiligingsarchitectuur

```
Hoofdwachtwoord
      │
      ▼
PBKDF2-HMAC-SHA256
(310.000 iteraties, 16-byte salt)
      │
      ▼
 AES-256 sleutel
      │
      ▼
AES-256-GCM versleuteling
(12-byte IV, 128-bit auth-tag)
      │
      ▼
 Opgeslagen in SharedPreferences
```

- Elk opslagmoment genereert een verse IV
- De salt wordt bij aanmaken gegenereerd en nooit opnieuw gebruikt bij wachtwoordwijziging
- GCM-authenticatietag detecteert tampering → onleesbaar bij verkeerd wachtwoord
- TOTP-codes worden volledig lokaal berekend (geen server, geen netwerk)

---

## 📁 Projectstructuur

```
app/src/main/java/nl/wachtwoordmanager/
├── App.java               Application klasse: thema, auto-lock lifecycle, FLAG_SECURE
├── Crypto.java            AES-256-GCM + PBKDF2 versleuteling/ontsleuteling
├── Kluis.java             Kluis-beheer: CRUD, back-up, wachtwoord wijzigen, biometrie-token
├── WachtwoordItem.java    Data model (naam, gebruiker, ww, url, notities, categorie, TOTP, favoriet)
├── WachtwoordSterkte.java Entropie-berekening + 5-niveau sterkte-analyse
├── TotpGenerator.java     RFC 6238 TOTP + RFC 4226 HOTP (puur Java, geen libs)
├── AvatarHulp.java        Gekleurde cirkel-initialen op basis van naam-hash
├── ItemAdapter.java       RecyclerView: filteren, zoeken, sorteren (fav eerst, dan A–Z)
├── LoginActivity.java     Inlogscherm + kluis aanmaken + biometrische ontgrendeling
├── MainActivity.java      Overzicht: zoeken, categorie-chips, FAB, vergrendelen
├── ItemActivity.java      Item aanmaken/bewerken: generator, sterkte, TOTP-ticker
└── SettingsActivity.java  Thema, biometrie, auto-lock, klembord, ww wijzigen, back-up
```

---

## 🚀 Aan de slag

### Vereisten
- Android Studio Hedgehog (2023.1.1) of nieuwer
- Android SDK 34
- JDK 17

### Bouwen
```bash
git clone <repo-url>
cd wachtwoordmanager
./gradlew assembleDebug
# APK staat in: app/build/outputs/apk/debug/
```

### Installeren via ADB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 Schermen

1. **Inlogscherm** — Kluis aanmaken (nieuw) of ontgrendelen (bestaand) + vingerafdruk
2. **Overzicht** — Lijst met avatars, zoekbalk, categorie-chips, FAB
3. **Item bewerken** — Alle velden, wachtwoordgenerator, sterkte-meter, TOTP-ticker
4. **Instellingen** — Thema, biometrie, auto-lock, klembord, wachtwoord wijzigen, back-up

---

## 🔒 Privacybeleid

Alle gegevens worden uitsluitend lokaal op het toestel opgeslagen. Er worden geen gegevens verzonden naar servers of derde partijen. Geen analytics, geen ads, geen cloud-sync.

---

*Gemaakt met ❤️ in Nederland*
