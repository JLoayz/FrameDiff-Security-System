<div align="center">

# FrameDiff Security System

**Real-time motion detection and video surveillance, built from scratch in Java.**

![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/UI-JavaFX-4B8BBE?style=for-the-badge)
![Webcam](https://img.shields.io/badge/Capture-Sarxos%20Webcam-2EA44F?style=for-the-badge)
![SQL Server](https://img.shields.io/badge/Database-SQL%20Server-CC2927?style=for-the-badge&logo=microsoftsqlserver&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux-0A66C2?style=for-the-badge)
![Status](https://img.shields.io/badge/Type-Academic%20Project-6E40C9?style=for-the-badge)

</div>

---

## Overview

FrameDiff Security System watches a webcam feed, detects movement inside a zone you define, and reacts: it sounds an alarm, saves a photo as evidence, writes the event to a live log and records it in a SQL Server database.

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
        +--> Event log          (real-time panel in the UI)
        +--> Evidence snapshot  (saved to disk, at most once every 5 s)
        +--> Database record    (SQL Server, written on a background thread)
```

---

## Features

| Feature | Details |
|---|---|
| **Live video** | Continuous capture and rendering at 640x480, a resolution chosen for stable cross-platform behavior |
| **Detection engine** | Grayscale pixel-matrix comparison with configurable per-pixel threshold and global sensitivity |
| **Region of Interest** | Define X/Y limits to restrict analysis to the area that matters |
| **Alerts** | Acoustic alarm triggered on intrusion events |
| **Evidence capture** | Automatic photo saved to disk when movement is detected |
| **Event log** | Real-time audit panel inside the interface |
| **Database logging** | Each detection (date, movement level and photo path) is stored in SQL Server through JDBC |
| **Non-blocking writes** | Database inserts run on a dedicated thread, so the video never freezes |
| **Cooldown** | Photos and database records are limited to one every 5 seconds, so continuous movement does not flood the system |

---

## Architecture

The project is organized in four classes with clearly separated responsibilities.

| Class | Responsibility |
|---|---|
| `Main` | Application entry point |
| `Detector` | Frame-differencing logic, thresholds and ROI filtering |
| `Ventana` | JavaFX user interface, video rendering, event log and camera loop |
| `DeteccionDAO` | Data access: reads the connection settings and inserts detections into SQL Server |

| Principle | How it is applied |
|---|---|
| **Encapsulation** | Processing, audio/video resources, UI and database access live in separate classes, with protected fields exposed only through accessors |
| **Separation of concerns** | Motion calculation is decoupled from JavaFX rendering and webcam acquisition, and the UI knows nothing about SQL: it only asks the DAO to save a detection |
| **Exception handling** | I/O, audio and database errors are handled explicitly; a database failure never stops the surveillance |
| **Safe queries** | Inserts use `PreparedStatement`, so values are never concatenated into the SQL text |
| **Secrets outside the code** | Database credentials live in `config.properties`, which Git ignores |

---

## Project Structure

```text
FrameDiff-Security-System/
├── src/
│   ├── Main.java                   Entry point
│   ├── Detector.java               Motion detection engine
│   ├── Ventana.java                Graphical user interface
│   └── DeteccionDAO.java           Database access (JDBC)
├── lib/                            External libraries (Sarxos Webcam, SLF4J, BridJ, mssql-jdbc)
├── recursos/
│   ├── alarma.wav                  Alarm sound
│   └── estilos.css                 Interface styles
├── schema.sql                      Database and table creation script
├── config.example.properties       Connection settings template
└── README.md
```

`config.properties` (your real credentials) and the `Evidencias/` folder (captured photos) are created locally and are not part of the repository.

---

## Tech Stack

| Component | Purpose |
|---|---|
| **Java 17+** | Core language |
| **JavaFX** | Graphical user interface |
| **Sarxos Webcam API** | Camera capture |
| **javax.sound.sampled** | Alarm playback |
| **SQL Server** | Storage of detection records |
| **Microsoft JDBC Driver (mssql-jdbc)** | Connection between Java and SQL Server |
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

**Requirements**

- JDK 17 or higher
- [JavaFX SDK](https://openjfx.io/) (not bundled with the JDK; download it separately)
- A connected webcam
- SQL Server with TCP/IP enabled (default port 1433) and a login with SQL Server authentication
- The Sarxos Webcam and `mssql-jdbc` jars are already included in the `lib` folder

**1. Clone the repository**

```bash
git clone https://github.com/JLoayz/FrameDiff-Security-System.git
cd FrameDiff-Security-System
```

**2. Create the database**

Run `schema.sql` in your SQL Server (with DBeaver, SSMS, `sqlcmd` or similar). It creates the `SecuritySystem` database and the `SystemLogs` table:

| Column | Type | Description |
|---|---|---|
| `id_log` | `INT IDENTITY` | Unique id, generated automatically |
| `date_time` | `DATETIME2` | Moment of the detection, filled in by SQL Server |
| `movement_level` | `FLOAT` | Percentage of pixels that changed |
| `image_path` | `NVARCHAR(500)` | Path of the evidence photo on disk |

**3. Configure the connection**

Copy `config.example.properties` to `config.properties` (in the project root) and fill in your data:

```properties
db.url=jdbc:sqlserver://localhost:1433;databaseName=SecuritySystem;encrypt=true;trustServerCertificate=true
db.user=YOUR_USER
db.password=YOUR_PASSWORD
```

Properties in the URL are separated by `;`. For a named instance (for example SQL Server Express), use `jdbc:sqlserver://localhost;instanceName=SQLEXPRESS;databaseName=SecuritySystem;...`. `trustServerCertificate=true` is meant for local development only.

**4. Run**

From the project root, so `config.properties` is found. Replace `/path/to/javafx/lib` with the `lib` folder of your JavaFX SDK.

```bash
# Compile
javac --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.swing \
      -cp "lib/*" -d out src/*.java

# Run (Linux)
java --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.swing \
     -cp "out:recursos:lib/*" Main

# Run (Windows: use ; as the classpath separator)
java --module-path C:\path\to\javafx\lib --add-modules javafx.controls,javafx.swing ^
     -cp "out;recursos;lib/*" Main
```

Alternatively, open the project in your IDE (IntelliJ IDEA, Eclipse or NetBeans), add the jars in `lib` and the JavaFX SDK to the project, mark `recursos` as a resources folder and run `Main`.

---

<details>
<summary><b>Resumen en español</b></summary>

<br>

**FrameDiff Security System** es un sistema de videovigilancia y detección de movimiento en tiempo real, desarrollado en Java como proyecto del curso de Programación Orientada a Objetos (UTP, 2026).

Captura video con la API Sarxos Webcam a 640x480 y analiza los fotogramas con un motor propio de diferencia de fotogramas sobre matrices en escala de grises. Permite delimitar una zona de monitoreo (ROI), dispara una alarma sonora con `javax.sound.sampled`, guarda fotos de evidencia automáticamente, registra cada evento en una bitácora en tiempo real y lo almacena en una base de datos SQL Server.

**Base de datos:** cada detección guarda su fecha, el porcentaje de movimiento y la ruta de la foto en la tabla `SystemLogs`. El guardado se hace en un hilo aparte para no congelar el video, y se limita a un registro cada 5 segundos para que el movimiento continuo no llene la base.

**Arquitectura:** cuatro clases (`Main`, `Detector`, `Ventana` y `DeteccionDAO`) con encapsulamiento, separación de responsabilidades entre detección, captura, interfaz JavaFX y acceso a datos, y manejo de excepciones de I/O, audio y base de datos.

**Compatibilidad:** Windows (probado), Linux (probado), macOS (no soportado por restricciones de la librería Sarxos).

**Ejecución:**

1. Necesitas JDK 17 o superior, el SDK de JavaFX (se descarga aparte) y SQL Server con TCP/IP habilitado.
2. Ejecuta `schema.sql` para crear la base `SecuritySystem` y la tabla `SystemLogs`.
3. Copia `config.example.properties` como `config.properties` y rellena tu usuario y contraseña. Este archivo no se sube a GitHub.
4. Compila y ejecuta `Main` desde la raíz del proyecto, como se muestra en la sección *Getting Started*.

</details>

---

## Author

**Jairo Loayza** | Systems & Computer Science student, UTP, 2026

[LinkedIn](https://www.linkedin.com/in/jairo-loayza)