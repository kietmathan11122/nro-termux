import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

public class UnlockAllPlayerSkills {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/yeupich?useUnicode=true&characterEncoding=UTF-8&useSSL=false";
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection con = DriverManager.getConnection(url, "root", "");
            PreparedStatement ps = con.prepareStatement("SELECT id, name, skills FROM player");
            ResultSet rs = ps.executeQuery();

            int[] newSkills = {35, 36, 37, 38, 39, 40, 41, 42, 43};

            while (rs.next()) {
                int pId = rs.getInt("id");
                String name = rs.getString("name");
                String rawSkills = rs.getString("skills");

                JSONArray array = new JSONArray();
                if (rawSkills != null && !rawSkills.isEmpty()) {
                    try {
                        array = (JSONArray) JSONValue.parse(rawSkills);
                    } catch (Exception e) {
                        array = new JSONArray();
                    }
                }

                if (array == null) array = new JSONArray();

                // Add or upgrade all 9 new skills to level 7
                for (int sId : newSkills) {
                    boolean found = false;
                    for (int i = 0; i < array.size(); i++) {
                        String strItem = String.valueOf(array.get(i));
                        if (strItem.contains("[" + sId + ",") || strItem.contains("[" + sId + "]")) {
                            array.set(i, "[" + sId + ",7,0,0]");
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        array.add("[" + sId + ",7,0,0]");
                    }
                }

                String updatedSkills = array.toJSONString();
                String shortcut = "[35,36,37,38,39,40,41,42,43,-1]";

                PreparedStatement updatePs = con.prepareStatement("UPDATE player SET skills = ?, skills_shortcut = ? WHERE id = ?");
                updatePs.setString(1, updatedSkills);
                updatePs.setString(2, shortcut);
                updatePs.setInt(3, pId);
                updatePs.executeUpdate();
                updatePs.close();

                System.out.println("Unlocked and Maxed (Level 7) all 9 new skills for Player: " + name + " (ID: " + pId + ")");
            }

            rs.close();
            ps.close();
            con.close();
            System.out.println("=========================================================");
            System.out.println("ALL PLAYER CHARACTERS HAVE BEEN FULLY UNLOCKED WITH LEVEL 7 MAX SKILLS!");
            System.out.println("=========================================================");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
