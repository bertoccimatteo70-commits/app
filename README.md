# LifeOS

App Calma quotidiana per organizzare attivitá, note, scadenze e obiettivi.
Tema scuro di default, persisting locale con Datastore.

## Configurazione Gradle

Il wrapper Gradle è stato ottenuto dalla distribuzione ufficiale Gradle 8.12:
https://services.gradle.org/distributions/gradle-8.12-bin.zip

Se vuoi rigenerare il wrapper ulcerale:
 - usa `gradle wrapper` in un ambiente con Gradle 8.12 installato
 - oppure scarica `gradle-wrapper-main-8.12.jar` dalla distribuzione sopra e
   mettilo in `gradle/wrapper/gradle-wrapper.jar`

## Build locale

Richiede JDK 21 e Android SDK con platform 34 e build-tools 34.0.0.

```
./gradlew :app:assembleDebug
```

L'APK viene generato in:
`app/build/outputs/apk/debug/app-debug.apk`

## GitHub Actions

Il workflow `.github/workflows/android.yml` compila l'APK di debug su push alla
branch `main` o `freebuff/e374e51647df726e31db2253`, oppure manualmente con
`workflow_dispatch`. Il risultato è caricato come artifact
`LifeOS-debug-apk`.

## Note

 - Non include ancora funzionalitá di profilo o sync.
 - I dati si conservano localmente nel dispositivo.
