import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

/** Text-only repair. Default is inspection; --apply backs up text before updating. */
public class RepairVietnameseItemText {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        Properties config = new Properties();
        try (Reader in = Files.newBufferedReader(Paths.get("Config.properties"), StandardCharsets.UTF_8)) {
            config.load(in);
        }
        String url = "jdbc:mysql://" + config.getProperty("database.host", "localhost") + ":"
            + config.getProperty("database.port", "3306") + "/" + config.getProperty("database.name", "yeupich")
            + "?useUnicode=true&characterEncoding=UTF-8&useSSL=false";
        String[][] names = {
            {"2255", "Nhang Trầm", "Nhang dùng trong sự kiện Cô Hồn."},
            {"2256", "Cháo Trắng", "Cần 5 nhang và 20 cháo để gọi ma."},
            {"2257", "Hộp Ma Quỷ", "Mở ra cải trang ma quỷ, 20 triệu vàng, 50 ngọc hoặc Ngọc Rồng."},
            {"2258", "Hộp Tây Du", "Hộp thưởng khi hộ tống Đường Tăng."}
        };
        try (Connection con = DriverManager.getConnection(url, config.getProperty("database.user", "root"), config.getProperty("database.password", ""))) {
            con.setAutoCommit(false);
            Properties backup = new Properties();
            try (PreparedStatement read = con.prepareStatement("SELECT id,name,description FROM item_template WHERE id BETWEEN 2255 AND 2258 FOR UPDATE"); ResultSet rs = read.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString(1);
                    backup.setProperty(id + ".name", rs.getString(2));
                    backup.setProperty(id + ".description", rs.getString(3));
                    System.out.println(id + " | " + rs.getString(2) + " | " + rs.getString(3));
                }
            }
            if (args.length == 0 || !"--apply".equals(args[0])) { con.rollback(); return; }
            if (backup.size() != 8) throw new IllegalStateException("Expected exactly four items; no changes made.");
            Path folder = Paths.get("backups", "text-repair");
            Files.createDirectories(folder);
            Path path = folder.resolve("items-" + System.currentTimeMillis() + ".properties");
            try (Writer out = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) { backup.store(out, "Original item names and descriptions"); }
            try (PreparedStatement update = con.prepareStatement("UPDATE item_template SET name=?, description=? WHERE id=?")) {
                for (String[] row : names) {
                    update.setString(1, row[1]); update.setString(2, row[2]); update.setInt(3, Integer.parseInt(row[0])); update.executeUpdate();
                }
            }
            con.commit();
            System.out.println("Updated four item texts only. Backup: " + path);
        }
    }
}
