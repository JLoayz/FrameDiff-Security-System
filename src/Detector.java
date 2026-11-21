import java.awt.Color;
import java.awt.image.BufferedImage;

public class Detector {

    private int umbralPixel = 30;
    private double umbralSensibilidad = 1.5;

    public int xInicio = 0, xFin = 640, yInicio = 0, yFin = 480;

    public double calcularPorcentajeMovimiento(BufferedImage imgActual, BufferedImage imgAnterior) {
        if (imgActual == null || imgAnterior == null) {
            return 0.0;
        }

        if (imgActual.getWidth() != imgAnterior.getWidth() || imgActual.getHeight() != imgAnterior.getHeight()) {
            return 0.0;
        }

        int ancho = imgActual.getWidth();
        int alto = imgActual.getHeight();

        int xMin = Math.max(0, Math.min(xInicio, ancho));
        int xMax = Math.max(xMin, Math.min(xFin, ancho));
        int yMin = Math.max(0, Math.min(yInicio, alto));
        int yMax = Math.max(yMin, Math.min(yFin, alto));

        int totalPixeles = (xMax - xMin) * (yMax - yMin);
        if (totalPixeles <= 0) {
            return 0.0;
        }

        int pixelesCambiados = 0;

        for (int y = yMin; y < yMax; y++) {
            for (int x = xMin; x < xMax; x++) {
                Color colorActual = new Color(imgActual.getRGB(x, y));
                Color colorAnterior = new Color(imgAnterior.getRGB(x, y));

                int grisActual = (colorActual.getRed() + colorActual.getGreen() + colorActual.getBlue()) / 3;
                int grisAnterior = (colorAnterior.getRed() + colorAnterior.getGreen() + colorAnterior.getBlue()) / 3;

                if (Math.abs(grisActual - grisAnterior) > umbralPixel) {
                    pixelesCambiados++;
                }
            }
        }

        return ((double) pixelesCambiados / totalPixeles) * 100.0;
    }

    public double getUmbralSensibilidad() {
        return umbralSensibilidad;
    }

    public int getUmbralPixel() {
        return umbralPixel;
    }

    public void setUmbralPixel(int umbralPixel) {
        this.umbralPixel = umbralPixel;
    }

    public void setUmbralSensibilidad(double umbralSensibilidad) {
        this.umbralSensibilidad = umbralSensibilidad;
    }
}
