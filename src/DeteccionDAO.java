import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

public class DeteccionDAO {
    private final String url;
    private final String user;
    private final String password;

    public DeteccionDAO() {
        Properties config = new Properties();
        try (FileInputStream in = new FileInputStream("config.properties")) {
            config.load(in);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo leer config.properties. Copia config.example.properties, renómbralo a config.properties y rellena tus datos.", e);
        }
        url = config.getProperty("db.url");
        user = config.getProperty("db.user");
        password = config.getProperty("db.password");
    }

    public boolean guardarDeteccion(double nivelMovimiento, String rutaImagen) {
        String sql = "INSERT INTO SystemLogs (movement_level, image_path) VALUES (?, ?)";

        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDouble(1, nivelMovimiento);
            ps.setString(2, rutaImagen);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("ERROR AL GUARDAR EN LA BD: " + e.getMessage());
            return false;
        }
    }
}