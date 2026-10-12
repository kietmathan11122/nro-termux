import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class InstallGeneratedMounts {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        String url = "jdbc:mysql://localhost:3306/yeupich?useUnicode=true&characterEncoding=UTF-8&useSSL=false";
        String migration = Files.readString(Path.of("sql", "generated_mounts_82_91.sql"), StandardCharsets.UTF_8);

        try (Connection connection = DriverManager.getConnection(url, "root", "")) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                for (String rawStatement : migration.split(";")) {
                    String sql = rawStatement.trim();
                    if (!sql.isEmpty()
                            && !sql.equalsIgnoreCase("START TRANSACTION")
                            && !sql.equalsIgnoreCase("COMMIT")) {
                        statement.execute(sql);
                    }
                }

                int itemCount = count(statement,
                        "SELECT COUNT(*) FROM item_template WHERE id BETWEEN 2259 AND 2268 "
                        + "AND TYPE = 23 AND part BETWEEN 82 AND 91");
                int imageCount = count(statement,
                        "SELECT COUNT(*) FROM img_by_name WHERE NAME REGEXP '^mount_(8[2-9]|9[01])_[01]$' "
                        + "AND n_frame = 4");
                if (itemCount != 10 || imageCount != 20) {
                    throw new IllegalStateException(
                            "Dữ liệu chưa đủ: item_template=" + itemCount + ", img_by_name=" + imageCount);
                }
                connection.commit();
                System.out.println("Đã thêm 10 item thú cưỡi và xác nhận 20 sprite 4-frame trong SQL yeupich.");
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private static int count(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }
}
