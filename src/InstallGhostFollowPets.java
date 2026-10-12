import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/** Installs and verifies the four cosmetic ghost-pet item templates. */
public class InstallGhostFollowPets {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        String url = "jdbc:mysql://localhost:3306/yeupich?useUnicode=true&characterEncoding=UTF-8&useSSL=false";
        String itemMigration = Files.readString(Path.of("sql", "add_ghost_follow_pets.sql"), StandardCharsets.UTF_8);
        String partMigration = Files.readString(Path.of("sql", "add_ghost_pet_parts.sql"), StandardCharsets.UTF_8);

        try (Connection connection = DriverManager.getConnection(url, "root", "")) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                executeMigration(statement, partMigration);
                executeMigration(statement, itemMigration);
                try (ResultSet result = statement.executeQuery(
                        "SELECT COUNT(*) FROM item_template WHERE id BETWEEN 2274 AND 2277 "
                                + "AND TYPE = 27 AND part > 0 AND head > 0 AND body > 0 AND leg > 0")) {
                    result.next();
                    if (result.getInt(1) != 4) {
                        throw new IllegalStateException("Thiếu part/head/body/leg của pet ma trong item_template.");
                    }
                }
                try (ResultSet result = statement.executeQuery(
                        "SELECT COUNT(*) FROM part WHERE id BETWEEN 3294 AND 3305")) {
                    result.next();
                    if (result.getInt(1) != 12) {
                        throw new IllegalStateException("Thiếu part đầu/thân/chân của pet ma.");
                    }
                }
                connection.commit();
                try (ResultSet result = statement.executeQuery(
                        "SELECT id, part, head, body, leg FROM item_template WHERE id BETWEEN 2274 AND 2277 ORDER BY id")) {
                    while (result.next()) {
                        System.out.printf("item %d: part=%d head=%d body=%d leg=%d%n",
                                result.getInt("id"), result.getInt("part"), result.getInt("head"),
                                result.getInt("body"), result.getInt("leg"));
                    }
                }
                System.out.println("Đã cài 4 pet ma và xác nhận đủ 12 part.");
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private static void executeMigration(Statement statement, String migration) throws Exception {
        for (String rawStatement : migration.split(";")) {
            String sql = rawStatement.replaceAll("(?m)^\\s*--.*$", "").trim();
            if (!sql.isEmpty()
                    && !sql.equalsIgnoreCase("START TRANSACTION")
                    && !sql.equalsIgnoreCase("COMMIT")) {
                statement.execute(sql);
            }
        }
    }
}
