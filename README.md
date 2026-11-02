# LifeOS

Ecosistema Android per organizzare attivitá, note, scadenze, obiettivi e
promemoria, con launcher, dashboard configurabile, mini-app e backup locale.
Kotlin + Jetpack Compose + Material 3, Android 8+ (minSdk 26, target 35).

## Funzioni

Tutto ció che segue è operativo e salvato su Datastore come JSON; nessuna
schermata è dimostrativa.

| Area | Cosa funziona |
| --- | --- |
| Attivitá | aggiunta, completamento, eliminazione, checklist, ricorrenza (giorno/settimana/mese), rollover automatico |
| Note | testo, tag, pin, ricerca, eliminazione |
| Scadenze | aggiunta, scadenza a 7 giorni di default, filtro in scadenza, completamento |
| Calendario | griglia mese, giorni evidenziati, eventi di scadenze e promemoria, promemoria con data/ora/ripetizione |
| Obiettivi | avanzamento 0-100 con barra animata |
| Dashboard | orologio live, statistiche, widget riordinabili e nascondibili (persistiti) |
| Launcher | elenco app installate, ricerca, preferiti, avvio app, filtro Home opzionale |
| Strumenti | calcolatrice (parser reale), timer con notifica, cronometro con giri, sveglie |
| Notifiche | canali, promemoria e sveglie con AlarmManager, rischedulazione al boot, permessi gestiti |
| Dati | export/import backup JSON versionato via SAF, import che rischedula i promemoria |
| Impostazioni | tema sistema/chiaro/scuro, 5 accenti, animazioni on/off, layout compatto, notifiche, automazioni, ruolo Home |

## Limiti noti

 - L'hosting dei widget Android nella schermata Launcher richiede che LifeOS sia
   impostata come app Home; senza quel ruolo il sistema rifiuta il binding e
   l'app lo segnala esplicitamente.
 - Le sveglie mostrano una notifica: non c'é ancora una schermata a tutto schermo
   con suoneria prolungata.
 - Le sveglie esatte dipendono dal permesso di sistema `SCHEDULE_EXACT_ALARM`;
   se negato si usano sveglie non esatte (ritardo possibile in Doze).
 - Gli "icon pack" esterni non sono supportati: la personalizzazione copre tema,
   accento, animazioni e layout.
 - L'app non dichiara il permesso INTERNET e non ha sync né account: la modalitá
   offline é strutturale, non un interruttore.

## Test

`./gradlew :app:testDebugUnitTest` esegue 44 test unitari su modello dati,
calcolatrice, backup, ricorrenze e widget della dashboard.

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

Il workflow `.github/workflows/android.yml`, su push alle branch `main` o
`freebuff/e374e51647df726e31db2253` oppure con `workflow_dispatch`:

 1. esegue i test unitari e pubblica `LifeOS-unit-test-results`;
 2. compila l'APK di debug (`LifeOS-debug-apk`) e l'APK di release
    (`LifeOS-release-apk`).

L'APK di release usato dalla CI é firmato con la chiave di debug perché nel
repository non esiste un keystore: per la distribuzione va sostituito con una
firma di release reale.

## Note

 - Non include ancora funzionalitá di profilo o sync (per scelta: tutto locale).
 - I dati si conservano localmente nel dispositivo.
