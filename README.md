# 🤖 AI Interview Trainer – Sistem Mobil Afectiv (SMA)

<p align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Retrofit-000000?style=for-the-badge&logo=squareup&logoColor=white" alt="Retrofit" />
  <img src="https://img.shields.io/badge/Room%20DB-4285F4?style=for-the-badge&logo=sqlite&logoColor=white" alt="Room DB" />
</p>

> **Proiect de Semestru – Sistem Mobil Afectiv (SMA)**  
> **Autor**: Roșca Albert-Paul  
> **Instituție**: Universitatea Politehnica Timișoara – Facultatea de Automatică și Calculatoare  
> **Specializare**: Calculatoare (Anul III)  
> **Arhitectură**: Nativ Android (100% Kotlin) | MVVM | Jetpack Compose | Retrofit | Room DB  

---

## 📌 Cuprins
1. [Despre Proiect](#-despre-proiect)
2. [Evoluția Proiectului: Inițial vs. Final](#-evoluția-proiectului-inițial-vs-final)
3. [Arhitectură & Stack Tehnologic](#-arhitectură--stack-tehnologic)
4. [Functionalități Cheie ale Proiectului Final](#-funcționalități-cheie-ale-proiectului-final)
5. [Structura Detaliată a Codului](#-structura-detaliată-a-codului)
6. [Ghid de Instalare și Rulare](#-ghid-de-instalare-și-rulare)
7. [Securitate și Stocare Date](#-securitate-și-stocare-date)

---

## 🎯 Despre Proiect

**AI Interview Trainer** este o aplicație mobilă nativă Android concepută ca un **Sistem Mobil Afectiv (SMA)**. Aplicația simulează un mediu interactiv și realist de interviu de angajare, pregătind candidații pentru întrebări tehnice și de cunoștințe generale pe o varietate mare de domenii profesionale.

Sistemul permite selecția facilă a profilului dorit, descarcă dinamic întrebări relevante din cloud, oferă asistență contextuală prin indicii în timp real (💡 **Hint**), evaluează răspunsurile prin algoritmi de potrivire a termenilor cheie (NLP Keyword Matching) și stochează progresul local în baza de date.

---

## 🔄 Evoluția Proiectului: Inițial vs. Final

Aplicația a evoluat de la o etapă conceptuală bazată pe un motor local LLM/static la un sistem mobil hibrid, stabil, eficient din punct de vedere al resurselor și complet adaptat cerințelor de performanță mobilă.

| Modul / Criteriu | 🟡 Concept Inițial (Propus) | 🟢 Proiect Final (Implementat) |
| :--- | :--- | :--- |
| **Sursă Întrebări** | Set static local sau instanță locală Ollama/LLM | **Cloud Remote JSON via Retrofit & GitHub Gist** (Preluare dinamică asincronă, ușor extensibilă fără recompilare) |
| **Domenii Acoperite** | 2–3 domenii de bază (IT, Vânzări) | **10 Domenii Profesionale Esențiale** (*IT, Biologie, HR, Finanțe, Marketing, Inginerie, Educație, Sales, Drept, Logistică*) |
| **Ierarhie Selecție** | Meniu simplu (Domeniu ➔ Rol) | **Meniuri în Cascadă pe 3 Niveluri**: *Domeniu ➔ Specializare ➔ Rol / Poziție* |
| **Format Sesiune** | Întrebări fixe secvențiale | **Sesiuni Dinamice**: Extragere aleatorie a **7 întrebări random** per sesiune din bancul de date ales |
| **Asistență Candidat** | Fără ajutor pe parcursul interviului | **Sistem de Indicii în Timp Real (💡 Hint System)**: Oferă îndrumare contextuală fără a penaliza scorul |
| **Evaluare & Feedback** | Punctaj simplist sau generare brută | **Engine Evaluativ NLP (Ground-Truth & Keyword Matching)**: Calcul exact al scorului (0-100%), generare *Puncte Tare* și *Arii de Îmbunătățire* |
| **Suport Lingvistic** | Doar Limba Română | **Sistem Bilingv Complet (RO / EN)**: Întrebări, indicii, interfață și rapoarte traduse |
| **Persistență Date** | Stocare temporară în memorie | **Room Database**: Salvarea completă a sesiunilor pe dispozitiv și calcularea **Mediei Generale a Scorurilor** |

---

## 🛠️ Arhitectură & Stack Tehnologic

Aplicația respectă principiile **Clean Architecture** și modelul de proiectare **MVVM (Model-View-ViewModel)** recomandat de Google.

```text
 ┌─────────────────────────────────────────────────────────────┐
 │                       UI Layer (Compose)                   │
 │   [SetupScreen] ───> [ChatScreen] ───> [FeedbackScreen]     │
 └──────────────────────────────┬──────────────────────────────┘
                                │ StateFlow / Events
 ┌──────────────────────────────▼──────────────────────────────┐
 │                     ViewModel Layer                         │
 │                   InterviewViewModel                        │
 └──────────────────────────────┬──────────────────────────────┘
                                │ Coroutines
 ┌──────────────────────────────▼──────────────────────────────┐
 │                     Repository Layer                        │
 │                    InterviewRepository                      │
 └───────────────┬──────────────────────────────┬──────────────┘
                 │                              │
 ┌───────────────▼──────────────┐  ┌────────────▼──────────────┐
 │ Remote Data (Retrofit + Gist)│  │ Local Data (Room DB)      │
 │   QuestionApiService / JSON  │  │   InterviewDao / SQLite   │
 └──────────────────────────────┘  └───────────────────────────┘# 🤖 Mobile Affective System (SMA) – AI Interview Coach (Local LLM Edition)

A native Android application built as a **Sistem Mobil Afectiv (SMA)**. It simulates realistic, domain-specific job interviews in real time using a **fully local LLM** (e.g., Ollama or on-device inference via MediaPipe LLM Inference / llama.cpp).

The app includes advanced evaluation capabilities: instead of generating arbitrary feedback, it evaluates candidate answers against verifiable reference criteria and ground-truth answer specs, providing precise scoring, persistent progression tracking, and optional voice interaction.

---

## 🎯 Overview

The **SMA AI Interview Coach** provides a structured, multi-domain environment where users select detailed job profiles via dynamic UI controls. The app guides the user through extended question sequences, evaluates each response against established domain criteria, saves past sessions locally, and provides vocal capabilities for hands-free practice.

---

## 🛠️ Tech Stack

* **Language:** Kotlin (100% Native)
* **UI Framework:** Jetpack Compose + Material 3 (Dropdown Menus, Custom Cards, Dark/Light Theme)
* **Architecture:** MVVM / Clean Architecture + Navigation Compose
* **Networking & Local AI:**
* *Local Server Mode:* Retrofit + OkHttp connected to a local **Ollama** / **LocalAI** instance over LAN (`[http://10.0.2.2:11434](http://10.0.2.2:11434)`)
* *On-Device Mode:* MediaPipe LLM Inference API / llama.cpp (GGUF model execution)


* **Local Storage:** Room Database (Interview history, session transcripts, and score metrics)
* **Voice Capabilities:** Android Native `SpeechRecognizer` (STT) & `TextToSpeech` (TTS)

---

## 📋 Execution Roadmap

### Step 1: Extended Multi-Domain & Dynamic UI Setup

* **Expanded Domain Directory:** Add support for a wide variety of industries (IT/Software, Sales, Marketing, Customer Support, Finance, HR, Project Management, Healthcare, etc.).
* **Dropdown Selection (Exposed Dropdown Menu):** Implement Compose dynamic dropdown components (cascading selection menus) allowing users to easily choose Domain, Specific Role, and Experience Level (Junior, Mid, Senior).
* **Extended Question Pipeline:** Configure the system to support longer, multi-stage interviews (higher question volume per session rather than brief 2–3 question samples).

### Step 2: Rigorous Ground-Truth & Answer Comparison Engine

* **Reference Answer Matching:** Establish an evaluation mechanism where the local LLM compares the user's submitted response directly against dynamic ground-truth key requirements (ideal answer specifications, domain concepts, and technical accuracy).
* **Objective Scoring System:** Eliminate automatic/random score generation by forcing the local evaluation prompt to break down scores based on specific criteria:
* Technical accuracy vs. ideal response traits.
* Completeness of arguments and key domain terms present.
* Structure, clarity, and communication quality.


* **Detailed Delta Analysis:** Highlight missing key points by explicitly showing what the ideal answer contained versus what the candidate answered.

### Step 3: Local Data Persistence (Room Database)

* **Database Architecture:** Build a Room database with entities for completed interview sessions, question-by-question logs, user responses, reference specifications, and scores.
* **Progress Tracking:** Store score histories per domain so users can track their performance trends over time on their device.
* **History Dashboard:** Create a history screen where users can re-read full past transcripts, target specific weak areas, and inspect past feedback reports.

### Step 4: Voice Capabilities (STT & TTS)

* **Text-to-Speech (TTS):** Integrate native Android `TextToSpeech` to vocalize interviewer questions aloud in real time.
* **Speech-to-Text (STT):** Integrate Android `SpeechRecognizer` allowing users to dictate their answers using the device microphone.

### Step 5: UI/UX Refinement & Polishing

* **Interactive UI Elements:** Design distinct chat bubbles, dynamic score progress indicators, and visual feedback cards (green for strong points, orange for areas needing improvement).
* **Theming & Transitions:** Support Dark/Light theme switching with seamless Compose animations across transitions.
* **Edge-Case Handling & Robustness:** Add handling for network interruptions (when using local LAN servers), high memory consumption during inference, long text payloads, and incomplete voice recordings.

---

## 💡 Local LLM Advantages

| Feature | Local Model (Ollama / On-Device) | Cloud AI APIs |
| --- | --- | --- |
| **Cost** | 100% Free | Pay-per-token / Monthly subscription |
| **Privacy** | High (Data never leaves device/network) | Medium (Data processed on remote servers) |
| **Internet Dependency** | Works offline / Local LAN | Requires stable internet |
| **Latency** | Depends on hardware (GPU/NPU) | Depends on cloud network connection |

---
