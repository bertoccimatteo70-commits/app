# LifeOS

App Calma quotidiana per organizzare attivitá, note, scadenze e obiettivi.
Tema scuro di default, persisting locale con Datastore.

## Configurazione Gradle

Il wrapper Gradle 8.12 è stato generato con il task `wrapper` della
 distribuzione ufficiale:
https://services.gradle.org/distributions/gradle-8.12-bin.zip

Per rigenerarlo:

```
 gradle wrapper --gradle-version 8.12 --distribution-type bin
```

## Build locale

Richiede JDK 21 e Android SDK con platform 35 e build-tools 35.0.0.

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
