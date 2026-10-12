import java.sql.*;

public class RepairJourneyWestEvent {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        String url = "jdbc:mysql://localhost:3306/yeupich?useUnicode=true&characterEncoding=UTF-8&useSSL=false";
        try (Connection con = DriverManager.getConnection(url, "root", "");
             PreparedStatement ps = con.prepareStatement(
                "INSERT INTO item_template (id,TYPE,gender,NAME,description,level,icon_id,part,is_up_to_up,power_require,gold,gem,head,body,leg) "
                + "VALUES (2258,99,3,?,?,0,32729,-1,1,0,0,0,-1,-1,-1) "
                + "ON DUPLICATE KEY UPDATE TYPE=99,gender=3,NAME=VALUES(NAME),description=VALUES(description),icon_id=32729,part=-1,is_up_to_up=1")) {
            ps.setString(1, "Hộp Tây Du");
            ps.setString(2, "Mở ra vật phẩm Tây Du, vàng, ngọc, Ngọc Rồng hoặc vật phẩm tăng sức đánh.");
            ps.executeUpdate();
        }
        try (Connection con = DriverManager.getConnection(url, "root", "");
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id,type,name,description,icon_id,is_up_to_up FROM item_template WHERE id=2258");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) System.out.printf("%d | type=%d | %s | icon=%d | stack=%s%n",
                    rs.getInt("id"), rs.getInt("type"), rs.getString("name"),
                    rs.getInt("icon_id"), rs.getBoolean("is_up_to_up"));
        }
    }
}
