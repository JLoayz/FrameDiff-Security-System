# FrameDiff-Security-System

## Descripción del Proyecto / Project Description

### Español
FrameDiff-Security-System es un sistema de videovigilancia y detección de movimiento en tiempo real desarrollado como proyecto académico para el curso de Programación Orientada a Objetos (POO).

El sistema captura video a través de una cámara web utilizando la librería Sarxos Webcam API (configurada a una resolución estable de 640x480 para garantizar compatibilidad multiplataforma) y procesa los fotogramas mediante un motor personalizado de análisis matricial basado en la técnica de diferencia de fotogramas (Frame-Differencing). El software incluye la delimitación dinámica de zonas de monitoreo, alertas acústicas mediante la API `javax.sound.sampled` y la captura automatizada de evidencias fotográficas ante eventos de intrusión.

### English
FrameDiff-Security-System is a real-time video surveillance and motion detection system developed as an academic project for the Object-Oriented Programming (OOP) course.

The system captures video from a webcam using the Sarxos Webcam API (configured at a stable 640x480 resolution to ensure cross-platform compatibility) and processes frames through a custom matrix analysis engine based on the Frame-Differencing technique. Features include dynamic monitoring zone delimitation, acoustic alerts via the `javax.sound.sampled` API, and automated photographic evidence logging during intrusion events.

---

## Arquitectura y Principios de POO / Architecture and OOP Principles

### Español
El proyecto fue diseñado aplicando los principios fundamentales de la Programación Orientada a Objetos:

- **Encapsulamiento / Encapsulation:** Las clases delimitan estrictamente la lógica de procesamiento (`Detector`), la gestión de recursos de audio y video, y la interfaz de usuario (`Ventana`), manteniendo los atributos protegidos y expuestos únicamente mediante métodos accesores.
- **Modularidad y Separación de Responsabilidades / Modularity and Separation of Concerns:** La lógica de detección de movimiento se encuentra aislada de los eventos de la interfaz gráfica Swing y de la captura de hardware.
- **Manejo de Excepciones y Recursos / Exception and Resource Management:** Implementación de control de excepciones I/O y de audio para garantizar la estabilidad del sistema en entornos Linux y Windows.

### English
The project was designed adhering to fundamental Object-Oriented Programming principles:

- **Encapsulation:** Classes strictly delineate frame processing logic (`Detector`), audio/video resource management, and user interface controls (`Ventana`), maintaining protected fields accessed only via explicit getter/setter methods.
- **Modularity and Separation of Concerns:** Motion detection calculation is completely decoupled from Swing GUI rendering and hardware acquisition tasks.
- **Exception and Resource Management:** Robust handling of stream I/O and audio device exceptions ensuring software stability in Linux and Windows environments.

---

## Características Técnicas / Technical Features

### Español
- **Procesamiento de Video / Video Processing:** Captura y renderizado continuo a resolución 640x480 (optimizada para compatibilidad multiplataforma).
- **Algoritmo de Detección / Detection Algorithm:** Comparación de matrices de píxeles en escala de grises con umbrales configurables de diferencia por píxel y sensibilidad global.
- **Región de Interés (ROI) / Region of Interest:** Definición dinámica de límites de coordenadas (X, Y) para acotar el área de análisis.
- **Gestión de Eventos / Event Management:** Disparo de alarmas sonoras y registro automático de imágenes de evidencia en disco.
- **Bitácora de Eventos / Event Log:** Panel de texto de auditoría en tiempo real dentro de la interfaz gráfica.

### English
- **Video Processing:** Continuous capture and rendering at 640x480 resolution (optimized for cross-platform compatibility).
- **Detection Algorithm:** Grayscale pixel matrix comparison with configurable per-pixel difference thresholds and global sensitivity.
- **Region of Interest (ROI):** Dynamic definition of coordinate limits (X, Y) to restrict the analysis area.
- **Event Management:** Audio alarm triggering and automatic saving of evidence images to disk.
- **Event Log:** Real-time auditing text panel within the graphical user interface.

---

## Compatibilidad de Sistemas Operativos / Operating System Compatibility

### Español
- **Windows:** Probado y totalmente funcional.
- **Linux:** Totalmente compatible y probado en entornos de desarrollo.
- **macOS:** No soportado debido a restricciones de acceso al hardware de captura de video mediante la librería Sarxos en este sistema operativo.

### English
- **Windows:** Tested and fully functional.
- **Linux:** Fully compatible and tested in development environments.
- **macOS:** Not supported due to video capture hardware access restrictions imposed by the Sarxos library on this operating system.

---

## Requisitos del Sistema / System Requirements

### Español
- Kit de Desarrollo de Java (JDK) 17 o superior.
- Sistemas Operativos compatibles: Windows / Linux.
- Librerías externas:
  - Sarxos Webcam API (`webcam-capture-*.jar`)
  - SLF4J API
  - BridJ Native Interface

### English
- Java Development Kit (JDK) 17 or higher.
- Compatible Operating Systems: Windows / Linux.
- External Libraries:
  - Sarxos Webcam API (`webcam-capture-*.jar`)
  - SLF4J API
  - BridJ Native Interface

---

## Instrucciones de Ejecución / Execution Instructions

#### Clonar el repositorio / Clone the repository
```bash
git clone [https://github.com/JLoayz/FrameDiff-Security-System.git](https://github.com/JLoayz/FrameDiff-Security-System.git)
cd FrameDiff-Security-System
```
---
## Autor / Author

### Español
Proyecto desarrollado para el curso de Programación Orientada a Objetos.  
UTP — 2026.

### English
Project developed for the Object-Oriented Programming course.  
UTP — 2026.
