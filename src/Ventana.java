import javafx.application.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import com.github.sarxos.webcam.Webcam;

public class Ventana extends Application {
    private ImageView vistaVideo;
    private Label lblVideo;
    private Label lblEstado;
    private Button btnEncender;
    private TextArea zonaBitacora;
    private TextField txtInicioX, txtFinX, txtInicioY, txtFinY;

    private volatile String valInicioX = "0";
    private volatile String valFinX = "640";
    private volatile String valInicioY = "0";
    private volatile String valFinY = "480";

    private Webcam camara;
    private volatile boolean sistemaActivo = false;
    private Detector detector;
    private BufferedImage fotoAnterior;
    private Clip sonidoAlarma;
    private final DeteccionDAO dao = new DeteccionDAO();
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private long ultimoRegistro = 0;
    private static final long ENFRIAMIENTO_MS = 5000;

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage escenario) {
        escenario.setTitle("Sistema de video-vigilancia");

        detector = new Detector();
        cargarAudio();

        Label titulo = new Label("VIDEO-VIGILANCIA");
        titulo.setId("titulo");
        HBox panelTitulo = new HBox(titulo);
        panelTitulo.setAlignment(Pos.CENTER);
        panelTitulo.setPadding(new Insets(15, 0, 5, 0));

        lblVideo = new Label("[ CÁMARA APAGADA ]");
        lblVideo.setId("lbl-video");
        vistaVideo = new ImageView();
        vistaVideo.setFitWidth(640);
        vistaVideo.setFitHeight(480);
        vistaVideo.setPreserveRatio(true);

        StackPane panelVideo = new StackPane(lblVideo, vistaVideo);
        panelVideo.setPrefSize(640, 480);
        panelVideo.setId("panel-video");
        panelVideo.setMaxSize(640, 480);

        zonaBitacora = new TextArea();
        zonaBitacora.setEditable(false);
        zonaBitacora.setPrefWidth(260);
        zonaBitacora.setMinWidth(260);
        zonaBitacora.setWrapText(true);
        zonaBitacora.setPrefRowCount(10);
        zonaBitacora.setId("bitacora");

        txtInicioX = new TextField("0");
        txtFinX = new TextField("640");
        txtInicioY = new TextField("0");
        txtFinY = new TextField("480");

        txtInicioX.textProperty().addListener((obs, a, n) -> valInicioX = n);
        txtFinX.textProperty().addListener((obs, a, n) -> valFinX = n);
        txtInicioY.textProperty().addListener((obs, a, n) -> valInicioY = n);
        txtFinY.textProperty().addListener((obs, a, n) -> valFinY = n);

        VBox panelIzq = new VBox(6,
                etiqueta("X Inicial"), txtInicioX,
                etiqueta("X Final"), txtFinX,
                etiqueta("Y Inicial"), txtInicioY,
                etiqueta("Y Final"), txtFinY);
        panelIzq.setPadding(new Insets(14));
        panelIzq.setPrefWidth(130);
        panelIzq.getStyleClass().add("panel-lateral");
        BorderPane.setMargin(panelIzq, new Insets(10));

        btnEncender = new Button("ENCENDER");
        btnEncender.setId("btn-encender");
        lblEstado = new Label("ESTADO: Cámara Apagada");
        lblEstado.setId("lbl-estado");
        HBox panelInferior = new HBox(20, btnEncender, lblEstado);
        panelInferior.setAlignment(Pos.CENTER);
        panelInferior.setPadding(new Insets(15));

        btnEncender.setOnAction(e -> interruptor());

        BorderPane.setMargin(panelVideo, new Insets(10));
        BorderPane.setMargin(zonaBitacora, new Insets(10));

        BorderPane raiz = new BorderPane();
        raiz.setId("raiz");
        raiz.setTop(panelTitulo);
        raiz.setCenter(panelVideo);
        raiz.setRight(zonaBitacora);
        raiz.setLeft(panelIzq);
        raiz.setBottom(panelInferior);

        Scene escena = new Scene(raiz);
        java.net.URL urlCss = getClass().getResource("/estilos.css");
        if (urlCss != null) {
            escena.getStylesheets().add(urlCss.toExternalForm());
        } else {
            System.out.println("ERROR. No se encontró estilos.css en la carpeta de recursos.");
        }

        escenario.setScene(escena);
        escenario.show();
        escenario.setMinWidth(escenario.getWidth());
        escenario.setMinHeight(escenario.getHeight());
        escenario.centerOnScreen();
    }

    private Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("etiqueta");
        return l;
    }

    private void ponerAlerta(boolean activa) {
        lblEstado.getStyleClass().remove("alerta");
        if (activa) {
            lblEstado.getStyleClass().add("alerta");
        }
    }

    @Override
    public void stop() {
        sistemaActivo = false;
        if (camara != null && camara.isOpen()) {
            camara.close();
        }
        if (sonidoAlarma != null) {
            sonidoAlarma.close();
        }

        dbExecutor.shutdown();
        try {
            dbExecutor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignorada) {
        }

        System.exit(0);
    }

    private void cargarAudio() {
        try {
            java.net.URL urlAudio = getClass().getResource("/alarma.wav");

            if (urlAudio != null) {
                AudioInputStream stream = AudioSystem.getAudioInputStream(urlAudio);
                sonidoAlarma = AudioSystem.getClip();
                sonidoAlarma.open(stream);
            } else {
                System.out.println("ERROR. No se encontró el archivo alarma.wav en la carpeta de recursos.");
            }
        } catch (Exception error) {
            System.out.println("ERROR al procesar el audio: " + error.getMessage());
        }
    }

    private void interruptor() {
        if (!sistemaActivo) {
            camara = Webcam.getDefault();
            if (camara != null) {
                camara.setViewSize(new Dimension(640, 480));
                camara.open();
                sistemaActivo = true;
                fotoAnterior = null;
                btnEncender.setText("APAGAR");
                btnEncender.getStyleClass().add("activo");
                ponerAlerta(false);
                lblEstado.setText("ESTADO: Cámara Encendida");
                lblVideo.setVisible(false);
                zonaBitacora.appendText("* SISTEMA INICIALIZADO\n");

                Thread hiloCamara = new Thread(this::bucleCamara);
                hiloCamara.setDaemon(true);
                hiloCamara.start();
            } else {
                Alert alerta = new Alert(Alert.AlertType.ERROR, "ERROR. No se detectó ninguna cámara.");
                alerta.showAndWait();
            }
        } else {
            sistemaActivo = false;
            camara.close();
            vistaVideo.setImage(null);
            lblVideo.setVisible(true);
            btnEncender.setText("ENCENDER");
            btnEncender.getStyleClass().remove("activo");
            ponerAlerta(false);
            lblEstado.setText("ESTADO: Cámara Apagada");
            zonaBitacora.appendText("* SISTEMA Detenido\n");
        }
    }

    private void bucleCamara() {
        while (sistemaActivo) {
            BufferedImage fotoActual = camara.getImage();

            if (fotoActual != null) {
                try {
                    detector.xInicio = Integer.parseInt(valInicioX);
                    detector.xFin = Integer.parseInt(valFinX);
                    detector.yInicio = Integer.parseInt(valInicioY);
                    detector.yFin = Integer.parseInt(valFinY);
                } catch (NumberFormatException numeroNoValido) {
                    detector.xInicio = 0;
                    detector.xFin = 640;
                    detector.yInicio = 0;
                    detector.yFin = 480;
                }

                int anchoRectangulo = detector.xFin - detector.xInicio;
                int altoRectangulo = detector.yFin - detector.yInicio;
                Graphics zonaActiva = fotoActual.getGraphics();
                zonaActiva.setColor(java.awt.Color.green);
                zonaActiva.drawRect(detector.xInicio, detector.yInicio, anchoRectangulo, altoRectangulo);
                zonaActiva.dispose();

                Image imagenFx = SwingFXUtils.toFXImage(fotoActual, null);
                Platform.runLater(() -> {
                    if (sistemaActivo) {
                        vistaVideo.setImage(imagenFx);
                    }
                });

                double nivelMovimiento = detector.calcularPorcentajeMovimiento(fotoActual, fotoAnterior);
                if (nivelMovimiento > detector.getUmbralSensibilidad()) {
                    String texto = "ALERTA: Movmiento detectado (" + String.format("%.2f", nivelMovimiento) + "%)";
                    Platform.runLater(() -> {
                        ponerAlerta(true);
                        lblEstado.setText(texto);
                        zonaBitacora.appendText("Movimiento inusual detectado.\n");
                    });

                    sonarAlarma();
                    long ahora = System.currentTimeMillis();
                    if (ahora - ultimoRegistro >= ENFRIAMIENTO_MS) {
                        ultimoRegistro = ahora;
                        String ruta = guardarFoto(fotoActual);
                        dbExecutor.submit(() -> dao.guardarDeteccion(nivelMovimiento, ruta));
                    }
                }
                fotoAnterior = fotoActual;
            }
            try {
                Thread.sleep(33);
            } catch (InterruptedException problemaHilo) {
                System.out.println("ERROR. Algo salió mal.");
            }
        }
    }

    private void sonarAlarma() {
        if (sonidoAlarma == null) return;

        if (sonidoAlarma.isRunning()) {
            sonidoAlarma.stop();
        }
        sonidoAlarma.setFramePosition(0);
        sonidoAlarma.start();
    }

    private String guardarFoto(BufferedImage img) {
        try {
            File carpeta = new File("Evidencias");
            if (!carpeta.exists()) carpeta.mkdir();

            String nombreImagen = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss"));

            File salida = new File(carpeta, "evidencia_" + nombreImagen + ".png");
            ImageIO.write(img, "png", salida);
            return salida.getAbsolutePath();

        } catch (Exception e) {
            System.out.println("ERROR. Algo salió al guardar la imagen.");
            return null;
        }
    }
}