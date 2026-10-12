import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class InstallChibiSystem {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        String sql = Files.readString(Path.of("sql", "add_chibi_system.sql"), StandardCharsets.UTF_8);
        try (Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3306/yeupich?useUnicode=true&characterEncoding=UTF-8&useSSL=false", "root", "");
             Statement st = c.createStatement()) {
            for (String raw : sql.split(";")) {
                String q = raw.replaceAll("(?m)^\\s*--.*$", "").trim();
                if (!q.isEmpty()) st.execute(q);
            }
        }
        System.out.println("Installed Chibi templates 2290-2297.");
    }
}
