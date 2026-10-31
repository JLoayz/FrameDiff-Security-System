import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import com.github.sarxos.webcam.Webcam;
import java.io.File;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.border.EmptyBorder;

public class Ventana extends JFrame {
    private JLabel lblVideo;
    private JLabel lblEstado;
    private JButton btnEncender;
    private JTextArea zonaBitacora;
    private JTextField txtInicioX, txtFinX, txtInicioY, txtFinY;

    private Webcam camara;
    private boolean sistemaActivo = false;
    private Detector detector;
    private BufferedImage fotoAnterior;
    private Clip sonidoAlarma;

    public Ventana() {
        setTitle("Sistema de video-vigilancia");
        setSize(1000,700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        detector = new Detector();

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

        lblVideo = new JLabel("[ CÁMARA APAGADA ]", SwingConstants.CENTER);
        lblVideo.setPreferredSize(new Dimension(640,480));
        lblVideo.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        add(lblVideo, BorderLayout.CENTER);

        zonaBitacora = new JTextArea(10, 30);
        zonaBitacora.setEditable(false);
        JScrollPane scroll = new JScrollPane(zonaBitacora);
        add(scroll, BorderLayout.EAST);

        txtInicioX = new JTextField("0");
        txtFinX = new JTextField("640");
        txtInicioY = new JTextField("0");
        txtFinY = new JTextField("480");

        JPanel panelIzq = new JPanel();
        panelIzq.setLayout(new BoxLayout(panelIzq, BoxLayout.Y_AXIS));
        panelIzq.setBorder(new EmptyBorder(10,10,10,10));
        panelIzq.add(new JLabel("X Inicial"));
        panelIzq.add(txtInicioX);
        panelIzq.add(new JLabel("X Final"));
        panelIzq.add(txtFinX);
        panelIzq.add(new JLabel("Y Inicial"));
        panelIzq.add(txtInicioY);
        panelIzq.add(new JLabel("Y Final"));
        panelIzq.add(txtFinY);
        panelIzq.add(Box.createRigidArea(new Dimension(0, 350)));
        add(panelIzq, BorderLayout.WEST);

        JPanel panelInferior = new JPanel(new FlowLayout());
        btnEncender = new JButton("ENCENDER");
        lblEstado = new JLabel("ESTADO: Cámara Apagada");
        panelInferior.add(btnEncender);
        panelInferior.add(lblEstado);
        add(panelInferior, BorderLayout.SOUTH);

        btnEncender.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent alternarSistema) {
                interruptor();
            }
        });
    }

    private void interruptor () {
        if (!sistemaActivo) {
            camara = Webcam.getDefault();
            if (camara != null) {
                camara.setViewSize(new Dimension(640, 480));
                camara.open();
                sistemaActivo = true;
                btnEncender.setText("APAGAR");
                lblEstado.setText("ESTADO: Cámara Encendida");
                zonaBitacora.append("* SISTEMA INICIALIZADO\n");

                Thread hiloCamara = new Thread(new Runnable() {
                    public void run() {
                        bucleCamara();
                    }
                });
                hiloCamara.start();
            } else {
                JOptionPane.showMessageDialog(this, "ERROR. No se detectó ninguna cámara.");
            }
        } else {
            sistemaActivo = false;
            camara.close();
            lblVideo.setIcon(null);
            lblVideo.setText("[ CÁMARA APAGADA ]");
            btnEncender.setText("ENCENDER");
            zonaBitacora.append("* SISTEMA Detenido\n");
        }
    }

    private void bucleCamara() {
        while (sistemaActivo) {
            BufferedImage fotoActual = camara.getImage();

            if (fotoActual != null) {
                try {
                    detector.xInicio = Integer.parseInt(txtInicioX.getText());
                    detector.xFin = Integer.parseInt(txtFinX.getText());
                    detector.yInicio = Integer.parseInt(txtInicioY.getText());
                    detector.yFin = Integer.parseInt(txtFinY.getText());
                } catch (NumberFormatException numeroNoValido) {
                    detector.xInicio = 0;
                    detector.xFin = 640;
                    detector.yInicio = 0;
                    detector.yFin = 480;
                }

                int anchoRectangulo = detector.xFin - detector.xInicio;
                int altoRectangulo = detector.yFin - detector.yInicio;
                Graphics zonaActiva = fotoActual.getGraphics();
                zonaActiva.setColor(Color.green);
                zonaActiva.drawRect(detector.xInicio, detector.yInicio, anchoRectangulo, altoRectangulo);
                zonaActiva.dispose();

                lblVideo.setIcon(new ImageIcon(fotoActual));

                double nivelMovimiento = detector.calcularPorcentajeMovimiento(fotoActual, fotoAnterior);
                if (nivelMovimiento > detector.getUmbralSensibilidad()) {
                    lblEstado.setText("ALERTA: Movmiento detectado (" + String.format("%.2f", nivelMovimiento) + "%)");
                    zonaBitacora.append("Movimiento inusual detectado.\n");

                    sonarAlarma();
                    guardarFoto(fotoActual);
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

    private void guardarFoto(BufferedImage img) {
        try {
            File carpeta = new File("Evidencias");
            if (!carpeta.exists()) carpeta.mkdir();

            String nombreImagen = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss"));

            File salida = new File(carpeta, "evidencia_" + nombreImagen + ".png");
            ImageIO.write(img, "png", salida);

        } catch (Exception e) {
            System.out.println("ERROR. Algo salió al guardar la imagen.");
        }
    }
}