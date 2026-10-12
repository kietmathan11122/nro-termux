import java.sql.*;

public class QueryJourneyItems {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        try (Connection con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/yeupich?useUnicode=true&characterEncoding=UTF-8&useSSL=false", "root", "");
             PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM item_template WHERE icon_id BETWEEN 32700 AND 32729 "
                + "OR id IN (547,548,920,1932) "
                + "OR name LIKE '%Ngộ Không%' OR name LIKE '%Bát Giới%' "
                + "OR name LIKE '%Sa Tăng%' OR name LIKE '%Đường Tăng%' OR name LIKE '%Gậy như ý%'");
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData md = rs.getMetaData();
            while (rs.next()) {
                StringBuilder out = new StringBuilder();
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    if (i > 1) out.append(" | ");
                    out.append(md.getColumnLabel(i)).append('=').append(rs.getString(i));
                }
                System.out.println(out);
            }
        }
    }
}
