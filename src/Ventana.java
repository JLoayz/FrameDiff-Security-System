import javafx.application.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;

public class Ventana extends Application {
    private ImageView vistaVideo;
    private Label lblVideo;
    private Label lblEstado;
    private Button btnEncender;
    private TextArea zonaBitacora;
    private TextField txtInicioX, txtFinX, txtInicioY, txtFinY;

    private ComboBox<FuenteCamara> comboCamaras;
    private Button btnAgregarIP;
    private Button btnEliminarIP;
    private FuenteCamara camaraActual;

    private volatile String valInicioX = "0";
    private volatile String valFinX = "640";
    private volatile String valInicioY = "0";
    private volatile String valFinY = "480";

    private volatile boolean sistemaActivo = false;
    private volatile boolean resolucionDetectada = false;
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

    @Override
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

        comboCamaras = new ComboBox<>();
        comboCamaras.setMaxWidth(Double.MAX_VALUE);

        btnAgregarIP = new Button("+ WiFi");
        btnAgregarIP.getStyleClass().add("button-secundario");
        btnAgregarIP.setTooltip(new Tooltip("Añadir y guardar una nueva cámara WiFi / IP"));
        btnAgregarIP.setOnAction(e -> mostrarDialogoAgregarIP());

        btnEliminarIP = new Button("- Borrar");
        btnEliminarIP.getStyleClass().add("button-peligro");
        btnEliminarIP.setTooltip(new Tooltip("Eliminar la cámara WiFi seleccionada"));
        btnEliminarIP.setOnAction(e -> eliminarCamaraSeleccionada());

        HBox panelBotonesCamara = new HBox(6, btnAgregarIP, btnEliminarIP);
        HBox.setHgrow(btnAgregarIP, Priority.ALWAYS);
        HBox.setHgrow(btnEliminarIP, Priority.ALWAYS);
        btnAgregarIP.setMaxWidth(Double.MAX_VALUE);
        btnEliminarIP.setMaxWidth(Double.MAX_VALUE);

        comboCamaras.valueProperty().addListener((obs, anterior, nueva) -> {
            boolean esIP = (nueva instanceof CamaraIP);
            btnEliminarIP.setDisable(!esIP || sistemaActivo);
        });

        cargarCamarasDisponibles();

        txtInicioX = new TextField("0");
        txtFinX = new TextField("640");
        txtInicioY = new TextField("0");
        txtFinY = new TextField("480");

        txtInicioX.textProperty().addListener((obs, a, n) -> valInicioX = n);
        txtFinX.textProperty().addListener((obs, a, n) -> valFinX = n);
        txtInicioY.textProperty().addListener((obs, a, n) -> valInicioY = n);
        txtFinY.textProperty().addListener((obs, a, n) -> valFinY = n);

        VBox panelIzq = new VBox(6,
                etiqueta("Fuente de Video"), comboCamaras,
                panelBotonesCamara,
                new Separator(),
                etiqueta("X Inicial"), txtInicioX,
                etiqueta("X Final"), txtFinX,
                etiqueta("Y Inicial"), txtInicioY,
                etiqueta("Y Final"), txtFinY);
        panelIzq.setPadding(new Insets(14));
        panelIzq.setPrefWidth(165);
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

    private void cargarCamarasDisponibles() {
        comboCamaras.getItems().clear();

        List<CamaraLocal> locales = CamaraLocal.obtenerCamarasDisponibles();
        comboCamaras.getItems().addAll(locales);

        List<CamaraIP> guardadas = GestorCamaras.cargarCamarasGuardadas();
        comboCamaras.getItems().addAll(guardadas);

        if (!comboCamaras.getItems().isEmpty()) {
            comboCamaras.setValue(comboCamaras.getItems().get(0));
        }
    }

    private void mostrarDialogoAgregarIP() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Agregar Cámara WiFi / IP");
        dialog.setHeaderText("Introduce los datos de la cámara de red\n(Se guardará automáticamente para futuras sesiones)");

        TextField txtNombre = new TextField("Cámara Celular");
        TextField txtUrl = new TextField("http://192.168.1.35:8080/video");
        txtUrl.setPromptText("http://IP:PUERTO/video");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(txtNombre, 1, 0);
        grid.add(new Label("URL Stream:"), 0, 1);
        grid.add(txtUrl, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String nom = txtNombre.getText().trim();
                String url = txtUrl.getText().trim();
                if (!url.isEmpty()) {
                    CamaraIP nueva = new CamaraIP(nom, url);
                    GestorCamaras.agregarCamara(nueva);
                    comboCamaras.getItems().add(nueva);
                    comboCamaras.setValue(nueva);
                    zonaBitacora.appendText("+ Cámara IP guardada: " + nueva.getNombre() + "\n");
                }
            }
        });
    }

    private void eliminarCamaraSeleccionada() {
        FuenteCamara actual = comboCamaras.getValue();
        if (actual instanceof CamaraIP camaraIP) {
            Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                    "¿Deseas eliminar la cámara guardada '" + camaraIP.getNombre() + "'?",
                    ButtonType.YES, ButtonType.NO);
            confirmacion.setHeaderText("Confirmar eliminación");
            Optional<ButtonType> res = confirmacion.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.YES) {
                GestorCamaras.eliminarCamara(camaraIP.getUrlStream());
                comboCamaras.getItems().remove(camaraIP);
                if (!comboCamaras.getItems().isEmpty()) {
                    comboCamaras.setValue(comboCamaras.getItems().get(0));
                }
                zonaBitacora.appendText("- Cámara eliminada: " + camaraIP.getNombre() + "\n");
            }
        }
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
        if (camaraActual != null && camaraActual.estaAbierta()) {
            camaraActual.cerrar();
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
            camaraActual = comboCamaras.getValue();
            if (camaraActual == null) {
                Alert alerta = new Alert(Alert.AlertType.WARNING, "No hay ninguna cámara seleccionada.");
                alerta.showAndWait();
                return;
            }

            try {
                lblEstado.setText("Conectando a " + camaraActual.getNombre() + "...");
                camaraActual.abrir();
                sistemaActivo = true;
                fotoAnterior = null;
                resolucionDetectada = false;

                btnEncender.setText("APAGAR");
                btnEncender.getStyleClass().add("activo");
                comboCamaras.setDisable(true);
                btnAgregarIP.setDisable(true);
                btnEliminarIP.setDisable(true);
                ponerAlerta(false);
                lblEstado.setText("ESTADO: Vigilando [" + camaraActual.getNombre() + "]");
                lblVideo.setVisible(false);
                zonaBitacora.appendText("* SISTEMA INICIALIZADO con " + camaraActual.getNombre() + "\n");

                Thread hiloCamara = new Thread(this::bucleCamara);
                hiloCamara.setDaemon(true);
                hiloCamara.start();
            } catch (Exception errorConexion) {
                if (camaraActual != null) {
                    camaraActual.cerrar();
                }
                lblEstado.setText("ESTADO: Error de conexión");
                Alert alerta = new Alert(Alert.AlertType.ERROR, "ERROR al conectar la cámara:\n" + errorConexion.getMessage());
                alerta.showAndWait();
            }
        } else {
            sistemaActivo = false;
            if (camaraActual != null) {
                camaraActual.cerrar();
            }
            comboCamaras.setDisable(false);
            btnAgregarIP.setDisable(false);
            btnEliminarIP.setDisable(!(comboCamaras.getValue() instanceof CamaraIP));
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
            BufferedImage fotoActual = (camaraActual != null) ? camaraActual.capturarFrame() : null;

            if (fotoActual != null) {
                int anchoReal = fotoActual.getWidth();
                int altoReal = fotoActual.getHeight();

                if (!resolucionDetectada) {
                    resolucionDetectada = true;
                    Platform.runLater(() -> {
                        txtInicioX.setText("0");
                        txtInicioY.setText("0");
                        txtFinX.setText(String.valueOf(anchoReal));
                        txtFinY.setText(String.valueOf(altoReal));
                    });
                    valInicioX = "0";
                    valInicioY = "0";
                    valFinX = String.valueOf(anchoReal);
                    valFinY = String.valueOf(altoReal);
                }

                try {
                    detector.xInicio = Integer.parseInt(valInicioX);
                    detector.xFin = Integer.parseInt(valFinX);
                    detector.yInicio = Integer.parseInt(valInicioY);
                    detector.yFin = Integer.parseInt(valFinY);
                } catch (NumberFormatException numeroNoValido) {
                    detector.xInicio = 0;
                    detector.xFin = anchoReal;
                    detector.yInicio = 0;
                    detector.yFin = altoReal;
                }

                int x1 = Math.max(0, Math.min(detector.xInicio, anchoReal - 2));
                int y1 = Math.max(0, Math.min(detector.yInicio, altoReal - 2));
                int x2 = Math.max(x1 + 1, Math.min(detector.xFin, anchoReal));
                int y2 = Math.max(y1 + 1, Math.min(detector.yFin, altoReal));
                int anchoR = Math.max(1, (x2 - x1) - 2);
                int altoR = Math.max(1, (y2 - y1) - 2);

                Graphics2D g2d = (Graphics2D) fotoActual.getGraphics();
                g2d.setColor(Color.GREEN);
                g2d.setStroke(new BasicStroke(2.0f));
                g2d.drawRect(x1, y1, anchoR, altoR);
                g2d.dispose();

                Image imagenFx = SwingFXUtils.toFXImage(fotoActual, null);
                Platform.runLater(() -> {
                    if (sistemaActivo) {
                        vistaVideo.setImage(imagenFx);
                    }
                });

                double nivelMovimiento = detector.calcularPorcentajeMovimiento(fotoActual, fotoAnterior);
                if (nivelMovimiento > detector.getUmbralSensibilidad()) {
                    String texto = "ALERTA: Movimiento detectado (" + String.format("%.2f", nivelMovimiento) + "%)";
                    Platform.runLater(() -> {
                        ponerAlerta(true);
                        lblEstado.setText(texto);
                        zonaBitacora.appendText("Movimiento inusual en " + camaraActual.getNombre() + " (" + String.format("%.2f", nivelMovimiento) + "%)\n");
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
