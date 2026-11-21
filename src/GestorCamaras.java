import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class GestorCamaras {

    private static final String ARCHIVO_CONFIG = "camaras.properties";

    public static List<CamaraIP> cargarCamarasGuardadas() {
        List<CamaraIP> lista = new ArrayList<>();
        File archivo = new File(ARCHIVO_CONFIG);
        if (!archivo.exists()) {
            return lista;
        }

        Properties prop = new Properties();
        try (FileInputStream in = new FileInputStream(archivo)) {
            prop.load(in);

            String totalStr = prop.getProperty("camaras.total", "0");
            int total = Integer.parseInt(totalStr);

            for (int i = 1; i <= total; i++) {
                String nombre = prop.getProperty("camara." + i + ".nombre");
                String url = prop.getProperty("camara." + i + ".url");
                if (url != null && !url.isBlank()) {
                    lista.add(new CamaraIP(nombre != null ? nombre : ("Cámara " + i), url));
                }
            }
        } catch (Exception e) {
            System.err.println("Advertencia al cargar cámaras guardadas: " + e.getMessage());
        }
        return lista;
    }

    public static void guardarCamaras(List<CamaraIP> camaras) {
        Properties prop = new Properties();
        int indice = 1;
        for (CamaraIP cam : camaras) {
            prop.setProperty("camara." + indice + ".nombre", cam.getNombre());
            prop.setProperty("camara." + indice + ".url", cam.getUrlStream());
            indice++;
        }
        prop.setProperty("camaras.total", String.valueOf(indice - 1));

        try (FileOutputStream out = new FileOutputStream(ARCHIVO_CONFIG)) {
            prop.store(out, "Configuracion de camaras IP / WiFi persistentes");
        } catch (IOException e) {
            System.err.println("Error al guardar archivo de cámaras: " + e.getMessage());
        }
    }

    public static void agregarCamara(CamaraIP nuevaCamara) {
        List<CamaraIP> actuales = cargarCamarasGuardadas();
        actuales.removeIf(c -> c.getUrlStream().equalsIgnoreCase(nuevaCamara.getUrlStream()));
        actuales.add(nuevaCamara);
        guardarCamaras(actuales);
    }

    public static void eliminarCamara(String urlStream) {
        List<CamaraIP> actuales = cargarCamarasGuardadas();
        actuales.removeIf(c -> c.getUrlStream().equalsIgnoreCase(urlStream));
        guardarCamaras(actuales);
    }
}
