# 🤖 AI Interview Trainer – Sisteme Mobile și Aplicații (SMA)

<p align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Kotlin-100%25%20Native-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Gemini%20AI-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white" alt="Gemini AI" />
  <img src="https://img.shields.io/badge/MediaPipe%20%26%20TFLite-FF6F00?style=for-the-badge&logo=tensorflow&logoColor=white" alt="MediaPipe & TFLite" />
  <img src="https://img.shields.io/badge/Room%20DB-4285F4?style=for-the-badge&logo=sqlite&logoColor=white" alt="Room DB" />
</p>

> **Proiect de Semestru – Sisteme Mobile și Aplicații (SMA)**  
> **Autor**: Roșca Albert-Paul  
> **Instituție**: Universitatea Politehnica Timișoara – Facultatea de Automatică și Calculatoare  
> **Specializare**: Calculatoare (Anul III)  
> **Arhitectură**: Nativ Android (100% Kotlin — 29 fișiere `.kt`) | MVVM | Jetpack Compose | Room DB | Gemini AI | MediaPipe  

---

## 📌 Cuprins
1. [Despre Proiect](#-despre-proiect)
2. [Evoluția Proiectului: Inițial vs. Implementat](#-evoluția-proiectului-inițial-vs-implementat)
3. [Arhitectură & Stack Tehnologic](#-arhitectură--stack-tehnologic)
4. [Funcționalități Cheie Implementate](#-funcționalități-cheie-implementate)
5. [Structura Detaliată a Codului](#-structura-detaliată-a-codului)
6. [Securitate și Stocare Date](#-securitate-și-stocare-date)

---

## 🎯 Despre Proiect

**AI Interview Trainer** este o aplicație mobilă nativă Android dezvoltată în cadrul disciplinei **Sisteme Mobile și Aplicații (SMA)**. Aplicația oferă un simulator multimodal de interviuri profesionale și tehnice, combinând:
* **Interviu conversațional (Scris + Audio)** cu indicii în timp real, scratchpad pentru cod și întrebări de tip *follow-up* dinamice;
* **Apel Video interactiv cu recruiterul AI „Amalia”** care analizează în timp real contactul vizual, postura umerilor și gesticulația prin modele **MediaPipe / TensorFlow Lite**;
* **Generare personalizată din CV sau Job Description** via **Google Gemini API** (cu fallback inteligent offline);
* **Evaluare hibridă (Rubrică CtCI + Evaluare Semantică Gemini)** și antrenament de **Repetiție Spațiată** pe baza lacunelor detectate în istoric.

---

## 🔄 Evoluția Proiectului: Inițial vs. Implementat

| Modul / Criteriu | 🟡 Concept Inițial | 🟢 Versiune Implementată |
| :--- | :--- | :--- |
| **Sursă Întrebări** | Set static local | **Hibrid**: Cloud JSON via **Retrofit + GitHub Gist**, **Generator AI din CV/Job (Gemini 2.5 Flash)** și **Bancă Personală CRUD (Room DB)** |
| **Moduri de Simulare** | Doar chat text simplu | **Interviu Scris + Audio (STT/TTS + Whiteboard Cod)** și **Apel Video AI (Amalia + MediaPipe Vision + Hands-Free Voice)** |
| **Configurare Profil** | Selecție simplă | **Meniuri în Cascadă pe 3 Niveluri** (*Domeniu ➔ Specializare ➔ Rol*), nivel experiență, stil intervievator (*Friendly HR*, *Tech Lead*) și mod cronometrat |
| **Dinamica Conversației** | Întrebări liniare fixe | **Follow-up automat** (pentru răspunsuri scurte sau lipsa analizei Big O) + **Sistem Indicii CtCI (💡 Hint)** |
| **Analiză Multimodală** | Fără senzori | **CameraX + MediaPipe Tasks Vision** (`face_landmarker`, `pose_landmarker_lite`, `hand_landmarker`) + **Analiză Vocală** (încredere, inflexiune, ritm cuv/min, cuvinte de umplutură) |
| **Evaluare & Feedback** | Potrivire simplă de cuvinte | **Motor Hibrid**: Rubrică **CtCI** (cuvinte-cheie, structură, penalizare indicii) + **Evaluare Semantică Gemini** + răspuns ideal de referință per întrebare |
| **Istoric & Învățare** | Listă simplă de scoruri | **Room DB complet**: detalii per întrebare, grafic de progres, bookmark-uri și **Spaced Repetition** pe întrebările slabe |
| **Suport Lingvistic & Temă** | Doar RO, temă fixă | **Bilingv (Română / Engleză)** + suport **Light / Dark Mode** |

---

## 🛠️ Arhitectură & Stack Tehnologic

Aplicația respectă principiile **Clean Architecture** și modelul **MVVM (Model-View-ViewModel)** cu flux de date unidirecțional prin `StateFlow`.

```text
 ┌─────────────────────────────────────────────────────────────────────────┐
 │                           UI Layer (Jetpack Compose)                    │
 │  [SetupScreen] ─> [CvJobGeneratorScreen] ─> [ChatScreen / VideoCall]    │
 │  [QuestionBankScreen] ────────────────────> [FeedbackScreen]            │
 │  [HistoryScreen (Progres & Spaced Repetition)]                          │
 └────────────────────────────────────┬────────────────────────────────────┘
                                      │ StateFlow / Events
 ┌────────────────────────────────────▼────────────────────────────────────┐
 │                            ViewModel Layer                              │
 │                          InterviewViewModel                             │
 └───────────┬────────────────────────┼────────────────────────┬───────────┘
             │                        │                        │
 ┌───────────▼──────────┐  ┌──────────▼──────────┐  ┌──────────▼──────────┐
 │  Repository & Cloud  │  │    AI & Evaluare    │  │   Senzori & Media   │
 │ InterviewRepository  │  │   GeminiAiService   │  │ BodyLanguageAnalyzer│
 │ QuestionApiService   │  │ CtciRubricEvaluator │  │ HandsFreeVoiceMgr   │
 │  (Retrofit + Gist)   │  │                     │  │    VoiceHelper      │
 └───────────┬──────────┘  └─────────────────────┘  └─────────────────────┘
             │
 ┌───────────▼──────────┐
 │  Local Persistence   │
 │ AppDatabase (Room DB)│
 │     InterviewDao     │
 └──────────────────────┘
```

### Tehnologii și Librării Principale
* **Limbaj**: Kotlin (100% Native — 29 fișiere `.kt`)
* **UI Framework**: Jetpack Compose + Material 3 (Navigation Compose, ExposedDropdownMenu, FlowRow, Canvas Charts)
* **AI & NLP**: Google Gemini REST API (`gemini-2.5-flash`) + `CtciRubricEvaluator`
* **Computer Vision (On-Device)**: CameraX + MediaPipe Tasks Vision (`face_landmarker.task`, `pose_landmarker_lite.task`, `hand_landmarker.task`)
* **Audio & Speech**: Android `SpeechRecognizer` (STT), `TextToSpeech` (TTS) & analiză acustică în timp real
* **Networking**: Retrofit + Gson Converter
* **Persistență Locală**: Room Database (SQLite cu entități pentru interviuri, întrebări personalizate și bookmark-uri)

---

## 🚀 Funcționalități Cheie Implementate

### 1. Configurare Avansată & Generator CV / Job Description (`SetupScreen`, `CvJobGeneratorScreen`)
* Filtrare în cascadă pe 3 niveluri (*Domeniu ➔ Specializare ➔ Rol*), selecție nivel (*Junior / Mid / Senior*), limbă (*Română / Engleză*) și stil intervievator (*Friendly HR* / *Tech Lead*).
* **Generare AI din CV sau Job Description**: utilizatorul poate lipi textul unui CV sau anunț de angajare, iar `GeminiAiService` generează automat 5 întrebări tehnice adaptate (cu fallback local inteligent în lipsa conexiunii/cheii API).
* **Mod Cronometrat (Timed Mode)**: cronometru per întrebare cu alertă vizuală în ultimele 20 de secunde.

### 2. Interviu Scris + Audio (`ChatScreen`, `CodeScratchpadSheet`)
* Interfață conversațională optimizată pentru text și dictare vocală (`VoiceHelper`), fără elemente video inutile.
* **Whiteboard / Code Scratchpad**: editor dedicat pentru scrierea și atașarea blocurilor de cod în răspuns.
* **Sistem de Indicii CtCI (`💡 Hint`)** și **Editare răspunsuri transcrise** înainte de finalizare.
* **Întrebări Follow-up Automate**: dacă un răspuns este prea scurt sau omite complexitatea algoritmică (*Big O*), intervievatorul cere automat clarificări.

### 3. Simulare Apel Video cu Recruiter AI „Amalia” (`VideoCallScreen`, `BodyLanguageAnalyzer`, `HandsFreeVoiceManager`)
* Mod hands-free cu recunoaștere vocală continuă și sinteză vocală (TTS).
* Analiză în timp real pe dispozitiv a limbajului nonverbal: **contact vizual**, **stabilitatea posturii umerilor**, **gesticulația mâinilor** și **expresivitatea**, alături de metrici vocale (**încredere** și **inflexiune**).

### 4. Raport de Evaluare Detaliat (`FeedbackScreen`)
* **Scor General (0–100%)** calculat instant pe rubrica CtCI și rafinat semantic prin Gemini AI.
* Secțiuni dedicate pentru **✓ Puncte tari**, **⚡ Ce poți îmbunătăți**, **Metrici de ritm** (secunde/răspuns, cuvinte/minut, cuvinte de umplutură), **Analiză Vocală** și **Limbaj Nonverbal**.
* **Analiză per întrebare (`QuestionDetailScreen`)**: afișează întrebarea, răspunsul candidatului, cuvintele-cheie bifate/omise, răspunsul ideal de referință și criteriile urmărite de intervievator, plus opțiune de **Share** a raportului.

### 5. Bancă de Întrebări & Istoric cu Repetiție Spațiată (`QuestionBankScreen`, `HistoryScreen`)
* **Banca de Întrebări**: gestionare completă (adăugare, editare, ștergere) pentru întrebări proprii (`CustomQuestionEntity`) și vizualizarea întrebărilor salvate la favorite (`BookmarkedQuestionEntity`).
* **Istoric & Progres**: stocare completă în Room DB, grafic de evoluție a scorurilor, calcul scor mediu/record și modul **Repetiție Spațiată**, care extrage automat întrebările din sesiunile anterioare unde au fost omise concepte cheie.

---

## 📁 Structura Detaliată a Codului (29 fișiere Kotlin)

```text
app/src/
├── main/
│   ├── assets/
│   │   ├── face_landmarker.task          # Model MediaPipe pentru detecție facială & contact vizual
│   │   ├── hand_landmarker.task          # Model MediaPipe pentru detecția gesticulației
│   │   └── pose_landmarker_lite.task     # Model MediaPipe pentru postura umerilor
│   └── java/com/example/interviewtrainer/
│       ├── data/
│       │   ├── model/
│       │   │   ├── BookmarkedQuestionEntity.kt  # Entitate Room pentru întrebări favorite
│       │   │   ├── ChatMessage.kt               # Model mesaje chat (USER, INTERVIEWER, HINT)
│       │   │   ├── CustomQuestionEntity.kt      # Entitate Room pentru întrebări custom
│       │   │   ├── InterviewConfig.kt           # Configurația sesiunii de interviu
│       │   │   ├── InterviewEntity.kt           # Entitate Room pentru istoricul sesiunilor + serializare JSON
│       │   │   ├── InterviewReport.kt           # Modele pentru raportul final și recenzia per întrebare
│       │   │   └── QuestionDto.kt               # DTO-uri pentru întrebările din Cloud JSON / AI
│       │   ├── remote/
│       │   │   ├── AppDatabase.kt               # Baza de date locală Room (SQLite)
│       │   │   ├── InterviewDao.kt              # Interfață DAO (CRUD interviuri, întrebări custom, bookmarks)
│       │   │   └── QuestionApiService.kt        # Serviciu Retrofit pentru preluarea băncii de întrebări
│       │   └── repository/
│       │       └── InterviewRepository.kt       # Gestionarea fluxului de întrebări și evaluării de bază
│       ├── ui/
│       │   ├── bank/
│       │   │   └── QuestionBankScreen.kt        # Ecran bancă de întrebări (Custom CRUD & Bookmarks)
│       │   ├── chat/
│       │   │   ├── ChatScreen.kt                # Ecran interviu scris + audio (STT/TTS, Hint, Whiteboard)
│       │   │   ├── CodeScratchpadSheet.kt       # BottomSheet pentru scrierea blocurilor de cod
│       │   │   └── VideoCallScreen.kt           # Ecran apel video hands-free cu analiza posturii
│       │   ├── feedback/
│       │   │   └── FeedbackScreen.kt            # Ecran raport final + ecran detaliu per întrebare
│       │   ├── history/
│       │   │   └── HistoryScreen.kt             # Ecran istoric, grafic evoluție și Repetiție Spațiată
│       │   ├── setup/
│       │   │   ├── CvJobGeneratorScreen.kt      # Ecran generare interviu din CV / Job Description
│       │   │   └── SetupScreen.kt               # Ecran configurare profil, mod interviu și temă
│       │   └── theme/
│       │       ├── Color.kt                     # Paleta de culori Light / Dark
│       │       ├── Theme.kt                     # Tema Material 3 a aplicației
│       │       └── Type.kt                      # Tipografia aplicației
│       ├── utils/
│       │   ├── BodyLanguageAnalyzer.kt          # Procesare cadre CameraX cu MediaPipe Tasks Vision
│       │   ├── CtciRubricEvaluator.kt           # Evaluator local bazat pe rubrica CtCI și cuvinte-cheie
│       │   ├── GeminiAiService.kt               # Client REST pentru generare întrebări și evaluare semantică
│       │   ├── HandsFreeVoiceManager.kt         # Gestionare conversație vocală continuă și metrici voce
│       │   └── VoiceHelper.kt                   # Wrapper pentru SpeechRecognizer și TextToSpeech
│       ├── viewmodel/
│       │   └── InterviewViewModel.kt            # ViewModel central (stare sesiune, evaluare, persistență)
│       └── MainActivity.kt                      # Punct de intrare și configurare Navigation Compose
├── androidTest/java/com/example/interviewtrainer/
│   └── ExampleInstrumentedTest.kt               # Teste instrumentate Android
└── test/java/com/example/interviewtrainer/
    └── ExampleUnitTest.kt                       # Teste unitare JVM
```

---

## 🔒 Securitate și Stocare Date

* **Procesare On-Device**: Cadrele video de la cameră și semnalul audio pentru analiza posturii și a vocii sunt procesate **exclusiv local pe dispozitiv** prin modelele MediaPipe/TFLite, fără a fi trimise către servere externe.
* **Persistență Privată**: Istoricul sesiunilor, răspunsurile, scorurile și întrebările personalizate sunt stocate local în baza de date privată **Room (SQLite)**.
* **Integrare Cloud Opțională**: Cheia API Gemini este gestionată prin `BuildConfig`, iar aplicația dispune de mecanisme complete de fallback local în cazul lipsei conexiunii la internet.