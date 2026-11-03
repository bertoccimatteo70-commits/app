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
| Finanze | entrate e uscite del mese con saldo, budget per categoria (sforamento evidenziato), abbonamenti normalizzati su mese/anno, andamento degli ultimi 6 mesi, export CSV di tutti i movimenti |
| Abitudini | spunta giornaliera, serie attuale e record calcolati dalle date salvate, griglia degli ultimi 7 giorni per recuperare un giorno saltato |
| Lista della spesa | articoli con quantità, completamento, pulizia dei completati |
| Password | gestore locale: ogni segreto è cifrato con AES-256-GCM e chiave non esportabile dell'Android Keystore; generatore di password (12-24 caratteri, senza caratteri ambigui), copia e condivisione |
| Strumenti | calcolatrice con parser reale, convertitori (lunghezza, peso, volume, temperatura, dati), timer con notifica, cronometro con giri, sveglie |
| Launcher | più pagine Home riordinabili, dock configurabile, griglia 3-8 colonne e 3-8 righe, dimensione icone e spaziatura, cartelle con nome/colore/icone, app nascoste con sezione dedicata, ordinamento A-Z/recenti/personalizzato, icon pack (appfilter), icone personalizzate da galleria, rinomina app, ricerca, drawer a elenco o griglia, widget Android, gesture (scorri su per il drawer, scorri giù per le notifiche, doppio tocco per bloccare lo schermo) |
| Notifiche | canali, promemoria e sveglie con AlarmManager, rischedulazione al boot, permessi gestiti, azione "Rinvia di 10 minuti" su promemoria e sveglie |
| Backup | export/import JSON versionato; in più backup cifrato AES-256-GCM con password (PBKDF2), import che rischedula i promemoria |
| Sicurezza | PIN con hash PBKDF2 e salt, blocco biometrico, blocco delle singole app del launcher (PIN o impronta prima dell'apertura), timeout di blocco configurabile, blocco temporaneo dopo 5 tentativi, registro locale eventi, blocco screenshot, recupero PIN tramite reset dichiarato, cancellazione selettiva (documenti, password, tutto) |
| Privacy | centro privacy con stato reale dei permessi e scorciatoie alle impostazioni Android, informativa interna, registro eventi, elenco trasparente dei dati trattati |
| Temi | tema sistema/chiaro/scuro/automatico per orario, 5 accenti, colori HEX personalizzati con verifica contrasto WCAG, angoli 0-32 dp, scala del testo, animazioni, layout compatto, temi salvati con nome, applicazione, eliminazione, export/import JSON, ripristino |
| Ricerca universale | Command Center dalla barra superiore: attività, note, scadenze, documenti, obiettivi, promemoria, abitudini, spese, entrate, abbonamenti e sezioni delle impostazioni; filtri per categoria, cronologia, apertura diretta del risultato, password incluse solo su richiesta esplicita |
| Aspetto | tema sistema/chiaro/scuro/orario, 5 accenti, animazioni on/off, layout compatto, scala del testo, sfondo Home, modalità immersiva, blocco orientamento |

## Sicurezza e privacy

- Il PIN non viene mai salvato: si conserva solo `PBKDF2-HMAC-SHA256` (120.000 iterazioni) con salt casuale e confronto a tempo costante.
- Le password del gestore sono cifrate con una chiave AES-256-GCM generata nell'Android Keystore; la chiave non è esportabile e la password in chiaro non finisce né nello stato né nel DataStore.
- Il backup cifrato deriva la chiave dalla password scelta con PBKDF2 (100.000 iterazioni) e usa AES-256-GCM con IV casuale; un file manomesso o una password errata falliscono in decrittografia.
- Gli allegati dei documenti sono copiati nella memoria privata dell'app; la condivisione avviene solo tramite URI FileProvider con permesso temporaneo.
- Nessuna pubblicità, nessun tracciamento, nessun SDK di analytics. L'app non dichiara il permesso INTERNET: la modalità offline è strutturale.
- L'informativa privacy completa è dentro l'app (scheda Privacy e Impostazioni → Sicurezza e privacy).
- "Cancella tutto" elimina dati, allegati, icone personalizzate, sfondi, promemoria programmati e la chiave di crittografia.
- Il registro eventi è locale, limitato a 200 voci e contiene solo tipo di evento e data: mai PIN, password o contenuti.
- La ricerca globale lavora solo sullo stato in memoria dell'app: non esiste un indice su disco da rubare e nessuna query viene inviata altrove. Le credenziali compaiono nei risultati solo se l'utente attiva l'opzione e comunque non viene mai mostrato il testo cifrato.
- I temi sono semplici preferenze locali; l'export contiene solo colori, modalità e misure, non dati personali.
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
- **Ricerca globale**: copre i dati presenti nell'app e le sezioni delle impostazioni. Non legge il testo dentro PDF/immagini (nessun OCR) e le credenziali compaiono solo attivando l'opzione dedicata, senza mai mostrare il testo cifrato.
- **Tema automatico per orario**: il cambio giorno/notte è valutato quando l'app si ricompone o viene riaperta; non c'è un job che cambia i colori alle 20:00 con l'app in primo piano.
- **Export finanziario**: CSV con separatore `;` e decimali con virgola per i fogli di calcolo italiani; il report PDF è generato con `PdfDocument` (A4) e condiviso via FileProvider, senza carta intestata personalizzabile.
- **File manager, scanner/OCR, automazioni, rubrica e cassaforte file**: non ancora implementati (fasi C-G della roadmap sotto).
- **Widget Android**: l'hosting funziona solo se LifeOS è impostata come app Home; il primo binding passa dalla conferma di sistema `ACTION_APPWIDGET_BIND`, perché un'app normale non può ottenere il permesso `BIND_APPWIDGET`.
- **Sveglie**: mostrano una notifica alta con azione "Rinvia di 10 minuti"; non aprono una schermata a tutto schermo con suoneria prolungata.
- **Blocco app**: protegge solo le app aperte dal launcher di LifeOS; un'app aperta da un altro launcher o dal sistema non passa dal controllo.
- **Sveglie esatte**: dipendono dal permesso `SCHEDULE_EXACT_ALARM`; se negato si usano sveglie non esatte.
- **Backup cifrato su altro dispositivo**: i segreti del gestore password restano cifrati con la chiave del dispositivo di origine, quindi su un telefono nuovo vanno reinseriti; note, documenti e resto dello stato si ripristinano normalmente.
- **APK di release in CI**: viene firmato solo se i quattro secret `RELEASE_*` sono configurati sul repository; senza di essi il job produce un APK non firmato e lo segnala con un warning, invece di ripiegare sulla chiave di debug.

## Test

`./gradlew :app:testDebugUnitTest` esegue 147 test unitari:

- modello dati, transizioni e backup (`LifeOSDataTests`, `UserInteractionsTests`, `WidgetModelTests`, `BackupTests`);
- calcolatrice (`CalculatorTests`);
- documenti, abbonamenti, spese, abitudini, spesa, override del launcher, formattazione importi, convertitori (`PersonalTests`);
- launcher, cartelle, pagine, dock, ordinamenti, parser icon pack, lockout e registro eventi (`LauncherTests`);
- PIN, backup cifrato e rilevamento manomissioni (`SecurityTests`);
- tema: parser HEX, contrasto WCAG, palette con override, orario notturno, preset salvati e import/export temi (`ThemeTests`);
- ricerca: corpo delle note, filtri, ranking, password nascoste, cronologia (`SearchTests`);
- finanze: saldo, budget per categoria, serie mensile, abbonamenti normalizzati, escaping CSV (`FinanceTests`).

In più, `app/src/androidTest` contiene un test strumentale di avvio (`AppLaunchTest`): la pipeline CI avvia un emulatore API 34 e verifica che la `MainActivity` raggiunga lo stato `RESUMED` con contenuto allegato, così un crash all'avvio viene rilevato automaticamente.

## Stato delle fasi

| Fase | Contenuto | Stato |
| --- | --- | --- |
| A | Launcher, icone, cartelle, pagine, dock, personalizzazione, modalità immersiva | **completata** (icon pack limitati ai temi con `appfilter.xml`); tema avanzato aggiunto in 3.0 |
| B | Privacy, sicurezza, crittografia, registro eventi, blocco screenshot, cancellazione selettiva | **completata** per le funzioni elencate; mancano blocco per-app e cifratura degli allegati |
| C | Documenti, scanner, OCR e burocrazia | **non iniziata**: l'archivio documenti esistente resta disponibile, scanner/OCR richiedono una scelta esplicita sulla dipendenza (modello offline) |
| D | File manager, strumenti e gestione quotidiana | **non iniziata** |
| E | Sveglie, notifiche e automazioni | parziale: sveglie con AlarmManager e canali separati già presenti; mancano schermata full-screen, snooze e automazioni |
| F | Dashboard, ricerca globale e produttività | **completata**: Command Center, calendario giorno/settimana con eventi reali, progetti, sottoattività, traguardi degli obiettivi, note con cestino e cartelle |
| G | Accessibilità, backup, ottimizzazione e test finali | **quasi completa**: backup versionato e cifrato, suite unitaria a 147 test e test strumentale di avvio in CI; l'audit di accessibilità completo non è stato eseguito |

Le fasi non ancora iniziate non hanno pulsanti finti: semplicemente non sono presenti nell'interfaccia.

## LifeOS 3.0 — cosa è cambiato in questo blocco

| Modulo | Implementazione reale |
| --- | --- |
| Theme Studio | colori HEX validati (`#RGB`, `#RRGGBB`, `#AARRGGBB`), anteprima, contrasto AA calcolato, angoli, densità, etichette, tema automatico per orario, preset di sistema + personali, export/import JSON, ripristino completo |
| Command Center | ricerca istantanea su tutti i moduli, punteggio, filtri, cronologia persistita, apertura della sezione corretta, password escluse per default |
| Finanze | entrate persistenti, saldo, budget per categoria con sforamento, serie a 6 mesi, abbonamenti mensili/annuali, CSV completo |

La scheda **Finanze** è nuova; il vecchio tool **Spese** resta invariato per chi lo usa dalla sezione Strumenti. Nessun dato dimostrativo: le schermate vuote mostrano messaggi di stato, non numeri finti.

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
2. avvia un emulatore Android API 34 (x86_64, KVM) ed esegue il test strumentale
   di avvio, pubblicando `LifeOS-instrumented-test-results`;
3. compila l'APK di debug (`LifeOS-debug-apk`) e l'APK di release
   (`LifeOS-release-apk`), firmandolo con la chiave reale quando i secret sono
   presenti.

## Permessi

- `POST_NOTIFICATIONS`, `VIBRATE`: promemoria, sveglie e timer.
- `SCHEDULE_EXACT_ALARM`: sveglie all'ora esatta, con fallback se negato.
- `RECEIVE_BOOT_COMPLETED`: rischedulazione dopo il riavvio.
- Nessun permesso signature: il binding dei widget usa la conferma di sistema, non `BIND_APPWIDGET`.
- `USE_BIOMETRIC`: sblocco con impronta o volto.
- Nessuna fotocamera richiesta: le foto passano dall'app fotocamera di sistema.
