import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class TestLoadDatabase {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/yeupich?useUnicode=true&characterEncoding=UTF-8&useSSL=false";
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection con = DriverManager.getConnection(url, "root", "");
            PreparedStatement ps = con.prepareStatement("select * from skill_template where id >= 35");
            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next()) {
                byte id = rs.getByte("id");
                String name = rs.getString("name");
                String rawSkills = rs.getString("skills");
                JSONArray dataArray = (JSONArray) JSONValue.parse(
                        rawSkills
                                .replaceAll("\\[\"", "[")
                                .replaceAll("\"\\[", "[")
                                .replaceAll("\"\\]", "]")
                                .replaceAll("\\]\"", "]")
                                .replaceAll("\\}\",\"\\{", "},{"));
                if (dataArray != null) {
                    for (int j = 0; j < dataArray.size(); j++) {
                        JSONObject dts = (JSONObject) JSONValue.parse(String.valueOf(dataArray.get(j)));
                        if (dts == null) {
                            System.err.println("NULL ERROR FOR SKILL ID: " + id + " (" + name + ")");
                        } else {
                            count++;
                        }
                    }
                } else {
                    System.err.println("NULL DATA ARRAY FOR SKILL ID: " + id + " (" + name + ")");
                }
            }
            rs.close();
            ps.close();
            con.close();
            System.out.println("SUCCESSFULLY VERIFIED " + count + " SKILL LEVELS IN DATABASE WITHOUT ANY NULL POINTER EXCEPTION!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
