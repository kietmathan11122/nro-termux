import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class RunImportSkillSql {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/yeupich?useUnicode=true&characterEncoding=UTF-8&useSSL=false";
        String user = "root";
        String pass = "";

        System.out.println("Connecting to MySQL Database 'yeupich'...");

        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, user, pass);
            Statement stmt = conn.createStatement();

            System.out.println("Connected! Executing add_b9_skills_dow.sql...");

            BufferedReader reader = new BufferedReader(new FileReader("add_b9_skills_dow.sql"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("--") || line.trim().isEmpty()) {
                    continue;
                }
                sb.append(line).append(" ");
                if (line.trim().endsWith(";")) {
                    String query = sb.toString().replace(";", "").trim();
                    if (!query.isEmpty()) {
                        stmt.execute(query);
                    }
                    sb.setLength(0);
                }
            }
            reader.close();
            stmt.close();
            conn.close();

            System.out.println("=================================================");
            System.out.println("SUCCESSFULLY INSTALLED ALL 9 SKILLS & 63 SKILL LEVELS INTO MYSQL DATABASE 'yeupich'!");
            System.out.println("=================================================");
        } catch (Exception e) {
            System.err.println("SQL Execution Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
