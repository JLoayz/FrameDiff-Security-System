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

        int pixelesCambiados = 0;
        int totalPixeles = (xFin - xInicio)*(yFin - yInicio);
        if (totalPixeles <= 0){
            totalPixeles =1;
        }
        for (int y = yInicio; y < yFin; y++) {
            for (int x = xInicio; x < xFin; x++) {

                if (x >= 0 && x < imgActual.getWidth() && y >= 0 && y < imgActual.getHeight()) {
                    Color colorActual = new Color(imgActual.getRGB(x, y));
                    Color colorAnterior = new Color(imgAnterior.getRGB(x, y));

                    int grisActual = (colorActual.getRed() + colorActual.getGreen() + colorActual.getBlue()) / 3;
                    int grisAnterior = (colorAnterior.getRed() + colorAnterior.getGreen() + colorAnterior.getBlue()) / 3;

                    if (Math.abs(grisActual - grisAnterior) > umbralPixel) {
                        pixelesCambiados++;
                    }
                }
            }
        }
        return ((double) pixelesCambiados / totalPixeles) * 100.0;
    }
    public double getUmbralSensibilidad() {
        return umbralSensibilidad;
    }
}