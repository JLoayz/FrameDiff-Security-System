import com.github.sarxos.webcam.Webcam;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class CamaraLocal implements FuenteCamara {

    private final Webcam webcam;
    private final String nombrePersonalizado;

    public CamaraLocal(Webcam webcam) {
        this(webcam, null);
    }

    public CamaraLocal(Webcam webcam, String nombrePersonalizado) {
        this.webcam = webcam;
        this.nombrePersonalizado = nombrePersonalizado;
    }

    @Override
    public void abrir() throws Exception {
        if (webcam == null) {
            throw new IllegalStateException("No se especificó ninguna webcam local.");
        }
        if (!webcam.isOpen()) {
            webcam.setViewSize(new Dimension(640, 480));
            boolean exito = webcam.open();
            if (!exito) {
                throw new RuntimeException("No se pudo inicializar la cámara local: " + getNombre());
            }
        }
    }

    @Override
    public void cerrar() {
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
    }

    @Override
    public BufferedImage capturarFrame() {
        if (webcam != null && webcam.isOpen()) {
            return webcam.getImage();
        }
        return null;
    }

    @Override
    public boolean estaAbierta() {
        return webcam != null && webcam.isOpen();
    }

    @Override
    public String getNombre() {
        if (nombrePersonalizado != null && !nombrePersonalizado.isBlank()) {
            return nombrePersonalizado;
        }
        return (webcam != null) ? webcam.getName() : "Webcam Local";
    }

    @Override
    public String toString() {
        return "[Local] " + getNombre();
    }

    public Webcam getWebcam() {
        return webcam;
    }

    public static List<CamaraLocal> obtenerCamarasDisponibles() {
        List<CamaraLocal> lista = new ArrayList<>();
        try {
            List<Webcam> webcams = Webcam.getWebcams();
            for (Webcam w : webcams) {
                lista.add(new CamaraLocal(w));
            }
        } catch (Exception e) {
            System.err.println("Advertencia al escanear webcams locales: " + e.getMessage());
        }
        return lista;
    }
}
