import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import javax.imageio.ImageIO;

public class CamaraIP implements FuenteCamara {

    private final String nombre;
    private final String urlStream;
    private static final int MAX_ANCHO_PROCESAMIENTO = 1280;

    private volatile boolean activa = false;
    private volatile BufferedImage ultimoFrame = null;
    private Thread hiloLector;
    private HttpURLConnection conexionActiva;

    public CamaraIP(String nombre, String urlStream) {
        this.nombre = (nombre == null || nombre.isBlank()) ? "Cámara WiFi" : nombre;
        this.urlStream = urlStream.trim();
    }

    @Override
    public synchronized void abrir() throws Exception {
        if (activa) {
            return;
        }

        if (urlStream.isBlank()) {
            throw new IllegalArgumentException("La URL de la cámara IP no puede estar vacía.");
        }

        activa = true;
        ultimoFrame = null;

        hiloLector = new Thread(this::bucleLectura, "HiloLector-" + nombre);
        hiloLector.setDaemon(true);
        hiloLector.start();

        long inicio = System.currentTimeMillis();
        while (activa && ultimoFrame == null && (System.currentTimeMillis() - inicio < 4000)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }

        if (ultimoFrame == null && activa) {
            cerrar();
            throw new RuntimeException("Tiempo de espera agotado: no se recibieron datos de " + urlStream);
        }
    }

    @Override
    public synchronized void cerrar() {
        activa = false;
        if (conexionActiva != null) {
            try {
                conexionActiva.disconnect();
            } catch (Exception ignorada) {
            }
        }
        if (hiloLector != null && hiloLector.isAlive()) {
            hiloLector.interrupt();
        }
        ultimoFrame = null;
    }

    @Override
    public BufferedImage capturarFrame() {
        return ultimoFrame;
    }

    @Override
    public boolean estaAbierta() {
        return activa;
    }

    @Override
    public String getNombre() {
        return nombre;
    }

    public String getUrlStream() {
        return urlStream;
    }

    @Override
    public String toString() {
        return "[WiFi/IP] " + nombre + " (" + urlStream + ")";
    }

    private BufferedImage ajustarResolucion(BufferedImage img) {
        if (img == null) {
            return null;
        }
        int w = img.getWidth();
        int h = img.getHeight();

        if (w > MAX_ANCHO_PROCESAMIENTO) {
            int nuevoAncho = MAX_ANCHO_PROCESAMIENTO;
            int nuevoAlto = (int) Math.round((double) h * MAX_ANCHO_PROCESAMIENTO / w);
            BufferedImage escalada = new BufferedImage(nuevoAncho, nuevoAlto, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = escalada.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(img, 0, 0, nuevoAncho, nuevoAlto, null);
            g2d.dispose();
            return escalada;
        }
        return img;
    }

    private void bucleLectura() {
        while (activa) {
            InputStream in = null;
            try {
                URL url = new URI(urlStream).toURL();
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(4000);
                con.setReadTimeout(5000);
                con.setRequestProperty("User-Agent", "Mozilla/5.0 (ProyectoPOO-Monitor)");
                con.connect();

                conexionActiva = con;
                String contentType = con.getContentType();

                if (contentType != null && !contentType.toLowerCase().contains("multipart") && contentType.toLowerCase().contains("image")) {
                    in = new BufferedInputStream(con.getInputStream());
                    BufferedImage img = ImageIO.read(in);
                    if (img != null) {
                        ultimoFrame = ajustarResolucion(img);
                    }
                    in.close();
                    con.disconnect();
                    Thread.sleep(35);
                    continue;
                }

                in = new BufferedInputStream(con.getInputStream());
                ByteArrayOutputStream buffer = new ByteArrayOutputStream(64 * 1024);
                int prev = -1;
                int cur;
                boolean grabando = false;

                while (activa && (cur = in.read()) != -1) {
                    if (!grabando) {
                        if (prev == 0xFF && cur == 0xD8) {
                            grabando = true;
                            buffer.reset();
                            buffer.write(0xFF);
                            buffer.write(0xD8);
                        }
                    } else {
                        buffer.write(cur);
                        if (prev == 0xFF && cur == 0xD9) {
                            byte[] bytes = buffer.toByteArray();
                            try {
                                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                                if (img != null) {
                                    ultimoFrame = ajustarResolucion(img);
                                }
                            } catch (Exception e) {
                            }
                            grabando = false;
                            buffer.reset();
                        }
                    }
                    prev = cur;
                }

            } catch (Exception e) {
                if (activa) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            } finally {
                if (in != null) {
                    try { in.close(); } catch (Exception ignorada) {}
                }
                if (conexionActiva != null) {
                    try { conexionActiva.disconnect(); } catch (Exception ignorada) {}
                }
            }
        }
    }
}
