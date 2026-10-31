<div align="center">

# FrameDiff Security System

**Real-time motion detection and video surveillance, built from scratch in Java.**

![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Swing](https://img.shields.io/badge/UI-Swing-4B8BBE?style=for-the-badge)
![Webcam](https://img.shields.io/badge/Capture-Sarxos%20Webcam-2EA44F?style=for-the-badge)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux-0A66C2?style=for-the-badge)
![Status](https://img.shields.io/badge/Type-Academic%20Project-6E40C9?style=for-the-badge)

</div>

---

## Overview

FrameDiff Security System watches a webcam feed, detects movement inside a zone you define, and reacts: it sounds an alarm, saves a photo as evidence, and writes the event to a live log.

Detection is powered by a **custom frame-differencing engine** that compares consecutive grayscale frames pixel by pixel. The matrix analysis is written from scratch rather than delegated to a computer vision library. The project was developed for the Object-Oriented Programming course at UTP.

---

## How It Works

```text
  Webcam (640x480)
        |
        v
  Grayscale conversion
        |
        v
  Frame differencing  <--  previous frame
        |
        v
  ROI filter (X, Y limits)
        |
        v
  Threshold check (per-pixel difference + global sensitivity)
        |
        +--> Audio alarm        (javax.sound.sampled)
        +--> Evidence snapshot  (saved to disk)
        +--> Event log          (real-time panel in the UI)
```

---

## Features

| Feature | Details |
|---|---|
| **Live video** | Continuous capture and rendering at 640x480, a resolution chosen for stable cross-platform behavior |
| **Detection engine** | Grayscale pixel-matrix comparison with configurable per-pixel threshold and global sensitivity |
| **Region of Interest** | Define X/Y limits to restrict analysis to the area that matters |
| **Alerts** | Acoustic alarm triggered on intrusion events |
| **Evidence capture** | Automatic photo saved to disk for every detected event |
| **Event log** | Real-time audit panel inside the interface |

---

## Architecture

The project is organized in three classes with clearly separated responsibilities.

| Class | Responsibility |
|---|---|
| `Main` | Application entry point |
| `Detector` | Frame-differencing logic, thresholds and ROI filtering |
| `Ventana` | Swing user interface, video rendering and event log |

| Principle | How it is applied |
|---|---|
| **Encapsulation** | Processing, audio/video resources and UI live in separate classes, with protected fields exposed only through accessors |
| **Separation of concerns** | Motion calculation is fully decoupled from Swing rendering and webcam acquisition |
| **Exception handling** | I/O and audio device errors are handled explicitly to keep the system stable on Windows and Linux |

---

## Project Structure

```text
FrameDiff-Security-System/
├── src/
│   ├── Main.java        Entry point
│   ├── Detector.java    Motion detection engine
│   └── Ventana.java     Graphical user interface
├── lib/                 External libraries (Sarxos Webcam, SLF4J, BridJ)
├── recursos/
│   └── alarma.wav       Alarm sound
└── README.md
```

---

## Tech Stack

| Component | Purpose |
|---|---|
| **Java 17+** | Core language |
| **Swing** | Graphical user interface |
| **Sarxos Webcam API** | Camera capture |
| **javax.sound.sampled** | Alarm playback |
| **SLF4J API** | Logging dependency of the webcam library |
| **BridJ** | Native interface dependency of the webcam library |

---

## Platform Support

| OS | Status |
|---|---|
| Windows | Tested and fully functional |
| Linux | Tested in development environments |
| macOS | Not supported (camera access restricted by the Sarxos library on this OS) |

---

## Getting Started

**Requirements:** JDK 17 or higher and a connected webcam. The required `.jar` files are already included in the `lib` folder.

**1. Clone the repository**

```bash
git clone https://github.com/JLoayz/FrameDiff-Security-System.git
cd FrameDiff-Security-System
```

**2. Compile**

```bash
javac -cp "lib/*" -d out src/*.java
```

**3. Run** (from the project root, so `recursos/alarma.wav` is found)

```bash
# Linux
java -cp "out:lib/*" Main

# Windows
java -cp "out;lib/*" Main
```

Alternatively, open the project in your IDE (IntelliJ IDEA, Eclipse or NetBeans), add the jars in `lib` to the classpath and run `Main`.

---

<details>
<summary><b>Resumen en español</b></summary>

<br>

**FrameDiff Security System** es un sistema de videovigilancia y detección de movimiento en tiempo real, desarrollado en Java como proyecto del curso de Programación Orientada a Objetos (UTP, 2026).

Captura video con la API Sarxos Webcam a 640x480 y analiza los fotogramas con un motor propio de diferencia de fotogramas sobre matrices en escala de grises. Permite delimitar una zona de monitoreo (ROI), dispara una alarma sonora con `javax.sound.sampled`, guarda fotos de evidencia automáticamente y registra cada evento en una bitácora en tiempo real.

**Arquitectura:** tres clases (`Main`, `Detector` y `Ventana`) con encapsulamiento, separación de responsabilidades entre detección, captura e interfaz Swing, y manejo de excepciones de I/O y audio.

**Compatibilidad:** Windows (probado), Linux (probado), macOS (no soportado por restricciones de la librería Sarxos).

**Ejecución:** JDK 17 o superior. Las librerías ya están en la carpeta `lib`. Compila con `javac -cp "lib/*" -d out src/*.java` y ejecuta `Main` desde la raíz del proyecto.

</details>

---

## Author

**Jairo Loayza** | Systems & Computer Science student, UTP, 2026

[LinkedIn](https://www.linkedin.com/in/jairo-loayza)
