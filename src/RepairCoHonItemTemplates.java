import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

public class RepairCoHonItemTemplates {

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        Properties config = new Properties();
        try (FileInputStream input = new FileInputStream("Config.properties")) {
            config.load(new java.io.InputStreamReader(input, StandardCharsets.UTF_8));
        }
        String host = config.getProperty("database.host", "localhost");
        String port = config.getProperty("database.port", "3306");
        String database = config.getProperty("database.name", "yeupich");
        String user = config.getProperty("database.user", "root");
        String password = config.getProperty("database.password", "");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=UTC";

        String[][] items = {
            {"2255", "Nhang Trầm", "Nhang dùng trong sự kiện Cô Hồn.", "31268"},
            {"2256", "Cháo Trắng", "Cần 5 nhang và 20 cháo để gọi ma.", "31269"},
            {"2257", "Hộp Ma Quỷ", "Mở ra cải trang ma quỷ, 20 triệu vàng, 50 ngọc hoặc Ngọc Rồng.", "31271"}
        };

        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE item_template SET type=99, gender=3, name=?, description=?, "
                     + "icon_id=?, part=-1, is_up_to_up=1 WHERE id=?")) {
            for (String[] item : items) {
                update.setString(1, item[1]);
                update.setString(2, item[2]);
                update.setInt(3, Integer.parseInt(item[3]));
                update.setInt(4, Integer.parseInt(item[0]));
                update.addBatch();
            }
            update.executeBatch();

            try (PreparedStatement createProgress = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS co_hon_escort_progress ("
                    + "player_id BIGINT NOT NULL PRIMARY KEY, escort_count INT NOT NULL DEFAULT 0, "
                    + "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP) "
                    + "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")) {
                createProgress.executeUpdate();
            }
            try (PreparedStatement repairBadge = connection.prepareStatement(
                    "UPDATE data_badges SET NAME = ?, Options = ? WHERE idEffect = 265 AND idItem = 1925")) {
                repairBadge.setString(1, "Người Dẫn Hồn");
                repairBadge.setString(2, "[{\"param\":5,\"id\":77},{\"param\":5,\"id\":103}]");
                repairBadge.executeUpdate();
            }

            try (PreparedStatement verify = connection.prepareStatement(
                    "SELECT id, type, name, description, icon_id FROM item_template WHERE id BETWEEN 2255 AND 2257 ORDER BY id");
                 ResultSet rs = verify.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("%d | type=%d | %s | %s | icon=%d%n",
                            rs.getInt("id"), rs.getInt("type"), rs.getString("name"),
                            rs.getString("description"), rs.getInt("icon_id"));
                }
            }
            try (PreparedStatement badge = connection.prepareStatement(
                    "SELECT id, idEffect, idItem, NAME, Options FROM data_badges WHERE idItem = 1925 OR idEffect = 265");
                 ResultSet rs = badge.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("BADGE %d | effect=%d | item=%d | %s | %s%n",
                            rs.getInt("id"), rs.getInt("idEffect"), rs.getInt("idItem"),
                            rs.getString("NAME"), rs.getString("Options"));
                }
            }
        }
    }
}
