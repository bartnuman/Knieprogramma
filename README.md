# Knieprogramma

Persoonlijke Android-app in Kotlin. Eén Activity, portrait-only, minSdk 24, compile/target SDK 36. Alleen INTERNET-permissie (voor YouTube). Geen login, advertenties of analytics.

## APK bouwen zonder Android Studio (via GitHub Actions)

Het project bevat een GitHub Actions-workflow die de APK automatisch bouwt op een tijdelijke Ubuntu-runner. Je hoeft lokaal geen Android Studio, Android SDK of Gradle te installeren.

1. Maak op GitHub een lege repository aan (bijvoorbeeld `Knieprogramma`).
2. Pak `Knieprogramma-source.zip` uit op je computer.
3. Upload de **inhoud** van de map `Knieprogramma` naar de repository, inclusief de verborgen map `.github` en het bestand `.github/workflows/build-apk.yml`. Gebruik bij voorkeur GitHub Desktop of de webinterface; controleer dat `.github/workflows/build-apk.yml` echt is meegestuurd.
4. Open in GitHub de repository en kies **Actions**.
5. Selecteer **Build Knieprogramma APK** en klik **Run workflow** (of wacht op de build na een push naar `main`).
6. Wacht tot de workflow groen is. Open de geslaagde run en download onder **Artifacts** het bestand `Knieprogramma-APK`.
7. Pak de artifact-zip uit. Daarin staat `app-release.apk`.
8. Zet de APK op je Android-telefoon en open hem. Android kan vragen om installatie vanuit deze bron toe te staan.

De release-APK wordt voor persoonlijk gebruik ondertekend met de standaard debug-keystore van de tijdelijke buildmachine. Een volgende build kan een andere sleutel gebruiken; installeer daarom niet over een bestaande installatie heen als Android een handtekeningconflict meldt. Verwijder in dat geval eerst de oude app (de lokaal opgeslagen sessiedata wordt dan gewist).

## Lokaal bouwen (optioneel)

Vereist JDK 17, Gradle 8.13 en Android SDK Platform 36:

```bash
gradle assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk`.

## Opmerkingen

- Fasevoltooiing wordt opgeslagen wanneer je op **Stop – naar hoofdmenu** tikt op het eindscherm.
- YouTube-video's vereisen internet. Als een embed niet laadt, verschijnt een link om de video rechtstreeks op YouTube te openen.
- YouTube kan autoplay of embeds blokkeren op basis van apparaat-, netwerk- of video-instellingen.
- De oefeningen en doseringen zijn overgenomen uit de aangeleverde specificatie; de app beoordeelt niet of een oefening medisch geschikt is.
