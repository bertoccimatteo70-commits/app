# LifeOS

Launcher, dashboard e sistema di organizzazione personale per Android.
Kotlin + Jetpack Compose + Material 3, Android 8+ (minSdk 26, target 35).
Tutto è locale: nessun account, nessun server, nessun permesso INTERNET.

## Funzioni

| Area | Cosa funziona davvero |
| --- | --- |
| Attività | aggiunta, completamento, eliminazione, checklist, ricorrenza (giorno/settimana/mese), rollover automatico |
| Note | testo, tag, pin, ricerca, eliminazione |
| Scadenze | aggiunta, scadenza predefinita a 7 giorni, filtro in scadenza, completamento |
| Calendario | griglia mese, giorni evidenziati, eventi da scadenze e promemoria, promemoria con data/ora/ripetizione |
| Obiettivi | avanzamento 0-100 con barra animata |
| Dashboard / Widget | orologio live, statistiche, widget riordinabili e nascondibili, persistiti |
| Documenti | archivio con categorie, tag, preferiti, ricerca e ordinamento; scadenza con promemoria automatico 24h prima; allegati copiati nella memoria privata; condivisione via FileProvider; foto del documento tramite fotocamera di sistema |
| Abbonamenti | importo, prossima scadenza, ricorrenza, attivo/disattivo, evidenza di quelli in scadenza |
| Spese | inserimento in centesimi, totale del mese, budget mensile con percentuale di consumo |
| Abitudini | spunta giornaliera, serie attuale e record calcolati dalle date salvate |
| Lista della spesa | articoli con quantità, completamento, pulizia dei completati |
| Password | gestore locale: ogni segreto è cifrato con AES-256-GCM e chiave non esportabile dell'Android Keystore |
| Strumenti | calcolatrice con parser reale, convertitori (lunghezza, peso, volume, temperatura, dati), timer con notifica, cronometro con giri, sveglie |
| Launcher | più pagine Home riordinabili, dock configurabile, griglia 3-8 colonne e 3-8 righe, dimensione icone e spaziatura, cartelle con nome/colore/icone, app nascoste con sezione dedicata, ordinamento A-Z/recenti/personalizzato, icon pack (appfilter), icone personalizzate da galleria, rinomina app, ricerca, drawer a elenco o griglia, widget Android, gesture (scorri su per il drawer, scorri giù per le notifiche, doppio tocco per bloccare lo schermo) |
| Notifiche | canali, promemoria e sveglie con AlarmManager, rischedulazione al boot, permessi gestiti |
| Backup | export/import JSON versionato; in più backup cifrato AES-256-GCM con password (PBKDF2), import che rischedula i promemoria |
| Sicurezza | PIN con hash PBKDF2 e salt, blocco biometrico, timeout di blocco configurabile, blocco temporaneo dopo 5 tentativi, registro locale eventi, blocco screenshot, recupero PIN tramite reset dichiarato, cancellazione selettiva (documenti, password, tutto) |
| Privacy | centro privacy con stato reale dei permessi e scorciatoie alle impostazioni Android, informativa interna, registro eventi, elenco trasparente dei dati trattati |
| Aspetto | tema sistema/chiaro/scuro, 5 accenti, animazioni on/off, layout compatto, scala del testo, sfondo Home, modalità immersiva, blocco orientamento |

## Sicurezza e privacy

- Il PIN non viene mai salvato: si conserva solo `PBKDF2-HMAC-SHA256` (120.000 iterazioni) con salt casuale e confronto a tempo costante.
- Le password del gestore sono cifrate con una chiave AES-256-GCM generata nell'Android Keystore; la chiave non è esportabile e la password in chiaro non finisce né nello stato né nel DataStore.
- Il backup cifrato deriva la chiave dalla password scelta con PBKDF2 (100.000 iterazioni) e usa AES-256-GCM con IV casuale; un file manomesso o una password errata falliscono in decrittografia.
- Gli allegati dei documenti sono copiati nella memoria privata dell'app; la condivisione avviene solo tramite URI FileProvider con permesso temporaneo.
- Nessuna pubblicità, nessun tracciamento, nessun SDK di analytics. L'app non dichiara il permesso INTERNET: la modalità offline è strutturale.
- L'informativa privacy completa è dentro l'app (scheda Privacy e Impostazioni → Sicurezza e privacy).
- "Cancella tutto" elimina dati, allegati, icone personalizzate, sfondi, promemoria programmati e la chiave di crittografia.
- Il registro eventi è locale, limitato a 200 voci e contiene solo tipo di evento e data: mai PIN, password o contenuti.
- Il blocco screenshot usa `FLAG_SECURE` quando l'opzione è attiva; non protegge da foto scattate con un altro dispositivo.
- Il recupero del PIN è volutamente un reset completo: LifeOS non conserva il PIN né una copia leggibile dei dati, quindi non esiste un recupero che non distrugga i dati.

## Limiti noti (dichiarati, non nascosti)

- **Icon pack di terze parti**: LifeOS legge gli icon pack che espongono `appfilter.xml` (ADW/Nova/Go e derivati). Gli altri temi non sono supportati e per le app senza mappatura restano le icone di sistema; non c'è modo di garantire copertura totale.
- **Applicazioni protette e cartelle cifrate**: non implementate. Le app restano avviabili dal drawer e le cartelle sono solo visive; la cifratura è riservata a password e backup.
- **OCR / scanner PDF**: non incluso. Servirebbe ML Kit (modello scaricato dalla rete, quindi in contrasto con l'offline richiesto). Alternativa implementata: foto del documento allegata al record e condivisibile. Il riconoscimento testo può essere aggiunto come opzione online separata.
- **Cartella privata cifrata dei file**: gli allegati sono nella sandbox privata dell'app ma non cifrati singolarmente. Solo i segreti del gestore password sono cifrati con Keystore, perché richiedono lettura casuale. Cifratura dei file può essere aggiunta con lo stesso schema, con costo di decrittografia all'apertura.
- **Gestione file / download**: non implementata. Su Android 11+ richiederebbe `MANAGE_EXTERNAL_STORAGE`, che Google Play limita a file manager dichiarati; alternativa concreta: browser via Storage Access Framework su cartella scelta dall'utente.
- **Rubrica e contatti**: non implementata (avrebbe richiesto READ_CONTACTS e una UI dedicata).
- **Automazioni locali**: non implementate. Android non consente di cambiare modalità notte o avviare azioni di sistema da un'app senza permessi speciali (WRITE_SECURE_SETTINGS / accessibilità): restano fuori per non chiedere permessi che promettono più di quanto possano fare.
- **Ricerca globale, produttività avanzata e file manager**: non ancora implementati (fasi C-G della roadmap sotto).
- **Widget Android**: l'hosting funziona solo se LifeOS è impostata come app Home; senza quel ruolo il sistema nega il binding e l'app lo spiega.
- **Sveglie**: mostrano una notifica, non una schermata a tutto schermo con suoneria prolungata.
- **Sveglie esatte**: dipendono dal permesso `SCHEDULE_EXACT_ALARM`; se negato si usano sveglie non esatte.
- **Backup cifrato su altro dispositivo**: i segreti del gestore password restano cifrati con la chiave del dispositivo di origine, quindi su un telefono nuovo vanno reinseriti; note, documenti e resto dello stato si ripristinano normalmente.
- **APK di release in CI**: viene firmato solo se i quattro secret `RELEASE_*` sono configurati sul repository; senza di essi il job produce un APK non firmato e lo segnala con un warning, invece di ripiegare sulla chiave di debug.

## Test

`./gradlew :app:testDebugUnitTest` esegue 82 test unitari:

- modello dati, transizioni e backup (`LifeOSDataTests`, `UserInteractionsTests`, `WidgetModelTests`, `BackupTests`);
- calcolatrice (`CalculatorTests`);
- documenti, abbonamenti, spese, abitudini, spesa, override del launcher, formattazione importi, convertitori (`PersonalTests`);
- launcher, cartelle, pagine, dock, ordinamenti, parser icon pack, lockout e registro eventi (`LauncherTests`);
- PIN, backup cifrato e rilevamento manomissioni (`SecurityTests`).

I test UI strumentali non sono inclusi: richiedono un emulatore, non disponibile nell'ambiente di build usato.

## Stato delle fasi

| Fase | Contenuto | Stato |
| --- | --- | --- |
| A | Launcher, icone, cartelle, pagine, dock, personalizzazione, modalità immersiva | **completata** (icon pack limitati ai temi con `appfilter.xml`) |
| B | Privacy, sicurezza, crittografia, registro eventi, blocco screenshot, cancellazione selettiva | **completata** per le funzioni elencate; mancano blocco per-app e cifratura degli allegati |
| C | Documenti, scanner, OCR e burocrazia | **non iniziata**: l'archivio documenti esistente resta disponibile, scanner/OCR richiedono una scelta esplicita sulla dipendenza (modello offline) |
| D | File manager, strumenti e gestione quotidiana | **non iniziata** |
| E | Sveglie, notifiche e automazioni | parziale: sveglie con AlarmManager e canali separati già presenti; mancano schermata full-screen, snooze e automazioni |
| F | Dashboard, ricerca globale e produttività | **non iniziata** |
| G | Accessibilità, backup, ottimizzazione e test finali | parziale: backup versionato e cifrato già presenti; audit accessibilità e test UI strumentali mancano |

Le fasi non ancora iniziate non hanno pulsanti finti: semplicemente non sono presenti nell'interfaccia.

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
./gradlew :app:assembleRelease
```

Gli APK vengono generati in:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

## Firma di release

La chiave di firma **non è nel repository**: `.gitignore` esclude `keystore/`,
`keystore.properties` e ogni `*.jks`. In locale Gradle legge
`keystore.properties` nella radice del progetto:

```properties
storeFile=keystore/lifeos-release.jks
storePassword=...
keyAlias=lifeos
keyPassword=...
```

Se il file esiste, `:app:assembleRelease` produce `app-release.apk` firmato con
la chiave di release; se manca, l'APK esce **non firmato** (nessun fallback
silenzioso alla chiave di debug).

Certificato attuale:

```
CN=LifeOS, OU=Mobile, O=LifeOS, L=Roma, ST=Italia, C=IT
SHA-256: a4f0b90324ca1e48d6e0f6ab20ce30d39d7bf1a63ef9f7142670e0139f5ddcba
SHA-1:   c52c1e9df2e0fba1afb90cc4aeab2b90a80afe59
RSA 4096, SHA384withRSA, validità fino al 2054
```

Verifica della firma:

```
$ANDROID_HOME/build-tools/35.0.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

**Backup obbligatorio**: senza `keystore/lifeos-release.jks` e la sua password
non sarà più possibile aggiornare l'app installata, perché Android rifiuta un
APK con una firma diversa. Conserva entrambi in un posto sicuro.

### Firma in CI

Carica i quattro secret in GitHub → Settings → Secrets and variables → Actions:

| Secret | Valore |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | `base64 -w0 keystore/lifeos-release.jks` |
| `RELEASE_STORE_PASSWORD` | password dello store |
| `RELEASE_KEY_ALIAS` | `lifeos` |
| `RELEASE_KEY_PASSWORD` | password della chiave |

Il workflow ricostruisce il keystore solo su runner, verifica la firma con
`apksigner` e poi elimina i file temporanei insieme al runner. Senza questi
secret l'APK di release viene caricato non firmato, con un warning nel log.

## GitHub Actions

Il workflow `.github/workflows/android.yml`, su push alle branch `main` o
`freebuff/e374e51647df726e31db2253` oppure con `workflow_dispatch`:

1. esegue i test unitari e pubblica `LifeOS-unit-test-results`;
2. compila l'APK di debug (`LifeOS-debug-apk`) e l'APK di release
   (`LifeOS-release-apk`), firmandolo con la chiave reale quando i secret sono
   presenti.

## Permessi

- `POST_NOTIFICATIONS`, `VIBRATE`: promemoria, sveglie e timer.
- `SCHEDULE_EXACT_ALARM`: sveglie all'ora esatta, con fallback se negato.
- `RECEIVE_BOOT_COMPLETED`: rischedulazione dopo il riavvio.
- `BIND_APPWIDGET`: hosting dei widget quando LifeOS è la Home.
- `USE_BIOMETRIC`: sblocco con impronta o volto.
- Nessuna fotocamera richiesta: le foto passano dall'app fotocamera di sistema.
