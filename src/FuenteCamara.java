import java.awt.image.BufferedImage;

public interface FuenteCamara {
    void abrir() throws Exception;

    void cerrar();

    BufferedImage capturarFrame();

    boolean estaAbierta();

    String getNombre();
}
