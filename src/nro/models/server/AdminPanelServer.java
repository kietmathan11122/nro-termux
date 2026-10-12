package nro.models.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import nro.models.data.LocalManager;
import nro.models.event.EventManager;
import nro.models.utils.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/** Local-only web control panel bundled with the game server. */
public final class AdminPanelServer {

    private static final AdminPanelServer INSTANCE = new AdminPanelServer();
    private static final String BASE = "/sv1/admin";
    private static final Set<String> EDITABLE_TABLES = Set.of(
            "account", "player", "giftcode", "item_template", "shop", "tab_shop",
            "item_shop", "power_limit", "map_template", "notify");
    private static final Map<String, Set<String>> EDITABLE_COLUMNS = Map.ofEntries(
            Map.entry("account", Set.of("ban", "is_admin", "active", "vnd", "tongnap", "vip")),
            Map.entry("player", Set.of("name", "head", "gender", "clan_id", "rank", "event_point", "point_sukien")),
            Map.entry("giftcode", Set.of("code", "count_left", "detail", "expired", "type")),
            Map.entry("item_template", Set.of("TYPE", "gender", "NAME", "description", "level", "icon_id", "power_require", "gold", "gem")),
            Map.entry("shop", Set.of("npc_id", "tag_name", "type_shop")),
            Map.entry("tab_shop", Set.of("shop_id", "NAME")),
            Map.entry("item_shop", Set.of("tab_id", "temp_id", "is_new", "is_sell", "type_sell", "cost", "sort_order")),
            Map.entry("power_limit", Set.of("power", "hp", "mp", "damage", "defense", "critical")),
            Map.entry("map_template", Set.of("NAME", "zones", "max_player", "type", "planet_id", "bg_type", "tile_id", "bg_id", "is_map_double")),
            Map.entry("notify", Set.of("name", "text")));
    private static final Map<String, QuerySpec> DATA_QUERIES = createQueries();

    private HttpServer server;
    private int port;

    public static AdminPanelServer gI() {
        return INSTANCE;
    }

    public synchronized void start() {
        if (server != null) {
            return;
        }
        Properties config = loadConfig();
        if (!Boolean.parseBoolean(config.getProperty("panel.enabled", "true"))) {
            return;
        }
        String externalUrl = config.getProperty("panel.url", "").trim();
        if (!externalUrl.isEmpty()) {
            Logger.success("Website panel: " + externalUrl + "\n");
            if (Boolean.parseBoolean(config.getProperty("panel.autoOpen", "true"))) {
                openBrowserLater(externalUrl);
            }
            return;
        }
        int requestedPort = parseInt(config.getProperty("panel.port"), 80);
        try {
            server = create(requestedPort);
            port = requestedPort;
        } catch (IOException firstError) {
            try {
                server = create(8080);
                port = 8080;
                Logger.warning("Port panel " + requestedPort + " dang ban, chuyen sang 8080.\n");
            } catch (IOException secondError) {
                Logger.logException(AdminPanelServer.class, secondError);
                return;
            }
        }
        server.createContext(BASE, this::handle);
        server.setExecutor(Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "Admin panel HTTP");
            thread.setDaemon(true);
            return thread;
        }));
        server.start();
        String url = "http://localhost" + (port == 80 ? "" : ":" + port) + BASE + "/panel";
        Logger.success("Admin panel: " + url + "\n");
        if (Boolean.parseBoolean(config.getProperty("panel.autoOpen", "true"))) {
            openBrowserLater(url);
        }
    }

    private HttpServer create(int listenPort) throws IOException {
        return HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), listenPort), 0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            addSecurityHeaders(exchange);
            String path = exchange.getRequestURI().getPath();
            if (path.equals(BASE) || path.equals(BASE + "/") || path.equals(BASE + "/panel")) {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    sendJson(exchange, 405, error("Chỉ hỗ trợ GET"));
                    return;
                }
                send(exchange, 200, "text/html; charset=UTF-8", loadPanelHtml());
                return;
            }
            if (path.equals(BASE + "/api/status")) {
                sendJson(exchange, 200, buildStatus());
                return;
            }
            if (path.equals(BASE + "/api/data")) {
                Map<String, String> query = parseForm(exchange.getRequestURI().getRawQuery());
                sendJson(exchange, 200, queryData(query.getOrDefault("section", "accounts"),
                        query.getOrDefault("q", ""), parseInt(query.get("page"), 1)));
                return;
            }
            if (path.equals(BASE + "/api/icon")) {
                serveIcon(exchange, parseInt(parseForm(exchange.getRequestURI().getRawQuery()).get("id"), -1));
                return;
            }
            if (path.equals(BASE + "/api/action") && "POST".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 200, performAction(readJson(exchange)));
                return;
            }
            sendJson(exchange, 404, error("Không tìm thấy đường dẫn"));
        } catch (Exception e) {
            Logger.logException(AdminPanelServer.class, e);
            sendJson(exchange, 500, error(e.getMessage() == null ? "Lỗi máy chủ" : e.getMessage()));
        } finally {
            exchange.close();
        }
    }

    @SuppressWarnings("unchecked")
    private JSONObject buildStatus() {
        JSONObject json = ok("Đã kết nối server");
        json.put("serverName", ServerManager.NAME_SERVER);
        json.put("startedAt", ServerManager.timeStart);
        json.put("online", Client.gI().getPlayers().size());
        json.put("exp", Manager.RATE_EXP_SERVER);
        json.put("drop", Manager.RATE_DROP_SERVER);
        json.put("adminMode", PanelCommandService.isAdminModeOnly());
        json.put("botMode", PanelCommandService.isBotModeEnabled());
        json.put("bossPersist", PanelCommandService.isBossPersistMode());
        json.put("eventId", EventManager.gI().getCurrentEventId());
        json.put("eventName", EventManager.gI().getEventName(EventManager.gI().getCurrentEventId()));
        JSONArray players = new JSONArray();
        Client.gI().getPlayers().stream().limit(100).forEach(player -> {
            JSONObject row = new JSONObject();
            row.put("id", player.id);
            row.put("name", player.name);
            row.put("map", player.zone == null ? "Đang tải" : player.zone.map.mapName + " - khu " + player.zone.zoneId);
            players.add(row);
        });
        json.put("players", players);
        json.put("bosses", readBosses());
        return json;
    }

    @SuppressWarnings("unchecked")
    private JSONObject queryData(String section, String search, int page) throws Exception {
        QuerySpec spec = DATA_QUERIES.get(section);
        if (spec == null) {
            return error("Mục dữ liệu không hợp lệ");
        }
        int safePage = Math.max(1, page);
        int limit = 50;
        int offset = (safePage - 1) * limit;
        String keyword = "%" + search.trim() + "%";
        JSONArray rows = new JSONArray();
        JSONArray columns = new JSONArray();
        try (Connection connection = LocalManager.getConnection();
                PreparedStatement statement = connection.prepareStatement(spec.sql + " LIMIT ? OFFSET ?")) {
            int param = 1;
            for (int i = 0; i < spec.searchParams; i++) {
                statement.setString(param++, keyword);
            }
            statement.setInt(param++, limit);
            statement.setInt(param, offset);
            try (ResultSet rs = statement.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    columns.add(meta.getColumnLabel(i));
                }
                while (rs.next()) {
                    JSONObject row = new JSONObject();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        Object value = rs.getObject(i);
                        String text = value == null ? "" : String.valueOf(value);
                        row.put(meta.getColumnLabel(i), text.length() > 500 ? text.substring(0, 500) + "…" : text);
                    }
                    rows.add(row);
                }
            }
        }
        JSONObject json = ok("Đã tải dữ liệu");
        json.put("section", section);
        json.put("table", spec.table);
        json.put("editable", EDITABLE_TABLES.contains(spec.table));
        json.put("columns", columns);
        json.put("rows", rows);
        json.put("page", safePage);
        return json;
    }

    private JSONObject performAction(JSONObject body) throws Exception {
        String action = string(body.get("action"));
        return switch (action) {
            case "buffVnd" -> runCommand("BUFF_VND|" + cleanPipe(body.get("player")) + "|" + positive(body.get("amount")), "Đã gửi lệnh tặng Cá");
            case "buffItem" -> buffItem(body);
            case "maintenance" -> runCommand("MAINTENANCE_NOW", "Bảo trì sẽ bắt đầu sau 30 giây");
            case "setExp" -> runCommand("SET_EXP|" + positive(body.get("value")), "Đã đổi tỉ lệ EXP");
            case "setDrop" -> runCommand("SET_DROP|" + positive(body.get("value")), "Đã đổi tỉ lệ rơi đồ");
            case "spawnBoss" -> runCommand("SPAWN_BOSS|" + integer(body.get("bossId")), "Đã gọi boss");
            case "editBoss" -> runCommand("EDIT_BOSS|" + integer(body.get("bossId"))
                    + "|" + integer(body.get("level"))
                    + "|" + positive(body.get("hp"))
                    + "|" + positive(body.get("dame"))
                    + "|" + Math.max(0, integer(body.get("armor")))
                    + "|" + java.net.URLEncoder.encode(string(body.get("name")), StandardCharsets.UTF_8)
                    + "|" + java.net.URLEncoder.encode(string(body.get("skills")), StandardCharsets.UTF_8),
                    "Đã cập nhật tên, HP, dame, giáp và kỹ năng boss");
            case "kick" -> runCommand("KICK_PLAYER|" + cleanPipe(body.get("player")) + "|" + positiveOr(body.get("seconds"), 300), "Đã kick nhân vật");
            case "adminMode" -> runCommand("ADMIN_MODE|" + boolFlag(body.get("enabled")), "Đã đổi chế độ admin");
            case "botMode" -> runCommand("BOT_PEM|" + boolFlag(body.get("enabled")), "Đã đổi chế độ bot");
            case "bossPersist" -> runCommand("BOSS_PERSIST_MODE|" + boolFlag(body.get("enabled")), "Đã đổi chế độ lưu boss");
            case "setEvent" -> runCommand("SET_EVENT|" + integer(body.get("eventId")), "Đã đổi sự kiện");
            case "reset" -> runCommand("RESET_PANEL", "Đã đặt lại cấu hình vận hành");
            case "reloadGiftcode" -> runCommand("LOAD_GIFTCODE", "Đã nạp lại giftcode");
            case "reloadPower" -> runCommand("LOAD_POWER_LIMIT", "Đã nạp lại chỉ số");
            case "updateRow" -> updateRow(body);
            case "createGiftcode" -> createGiftcode(body);
            case "createNotify" -> createNotify(body);
            case "deleteRow" -> deleteRow(body);
            case "approveTopup" -> approveTopup(body);
            default -> error("Chức năng không hợp lệ");
        };
    }

    private JSONObject buffItem(JSONObject body) {
        JSONArray items = new JSONArray();
        JSONObject item = new JSONObject();
        item.put("temp_id", integer(body.get("itemId")));
        item.put("quantity", positive(body.get("quantity")));
        item.put("options", body.get("options") instanceof JSONArray ? body.get("options") : new JSONArray());
        items.add(item);
        return runCommand("BUFF_ITEM|" + cleanPipe(body.get("player")) + "|" + items.toJSONString(), "Đã gửi vật phẩm");
    }

    private JSONObject runCommand(String command, String message) {
        PanelCommandService.gI().executeNow(command);
        return ok(message);
    }

    private JSONObject updateRow(JSONObject body) throws Exception {
        String table = string(body.get("table"));
        String field = string(body.get("field"));
        int id = integer(body.get("id"));
        if (!EDITABLE_TABLES.contains(table) || !EDITABLE_COLUMNS.getOrDefault(table, Set.of()).contains(field) || id < 0) {
            return error("Bảng hoặc cột không được phép sửa");
        }
        try (Connection connection = LocalManager.getConnection();
                PreparedStatement statement = connection.prepareStatement("UPDATE `" + table + "` SET `" + field + "`=? WHERE id=? LIMIT 1")) {
            statement.setString(1, string(body.get("value")));
            statement.setInt(2, id);
            int changed = statement.executeUpdate();
            reloadIfNeeded(table);
            return changed == 1 ? ok("Đã lưu thay đổi") : error("Không tìm thấy bản ghi");
        }
    }

    private JSONObject createGiftcode(JSONObject body) throws Exception {
        try (Connection connection = LocalManager.getConnection();
                PreparedStatement ps = connection.prepareStatement("INSERT INTO giftcode(code,count_left,detail,expired,type) VALUES(?,?,?,?,?)")) {
            ps.setString(1, string(body.get("code")));
            ps.setInt(2, positiveOr(body.get("count"), 1));
            ps.setString(3, string(body.get("detail")));
            ps.setString(4, string(body.get("expired")));
            ps.setInt(5, integer(body.get("type")));
            ps.executeUpdate();
        }
        PanelCommandService.gI().executeNow("LOAD_GIFTCODE");
        return ok("Đã tạo giftcode");
    }

    private JSONObject createNotify(JSONObject body) throws Exception {
        LocalManager.executeUpdate("INSERT INTO notify(name,text) VALUES(?,?)", string(body.get("name")), string(body.get("text")));
        return ok("Đã thêm nội dung");
    }

    private JSONObject deleteRow(JSONObject body) throws Exception {
        String table = string(body.get("table"));
        int id = integer(body.get("id"));
        if (!Set.of("giftcode", "notify").contains(table) || id < 0) {
            return error("Không được phép xóa bản ghi này");
        }
        LocalManager.executeUpdate("DELETE FROM `" + table + "` WHERE id=? LIMIT 1", id);
        reloadIfNeeded(table);
        return ok("Đã xóa bản ghi");
    }

    private JSONObject approveTopup(JSONObject body) throws Exception {
        String table = string(body.get("table"));
        int id = integer(body.get("id"));
        try (Connection connection = LocalManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int accountId;
                int amount;
                String update;
                if ("nap_the".equals(table)) {
                    try (PreparedStatement ps = connection.prepareStatement("SELECT account_id,card_amount,status FROM nap_the WHERE id=? FOR UPDATE")) {
                        ps.setInt(1, id);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next() || "success".equalsIgnoreCase(rs.getString("status"))) return error("Giao dịch đã duyệt hoặc không tồn tại");
                            accountId = rs.getInt("account_id"); amount = rs.getInt("card_amount");
                        }
                    }
                    update = "UPDATE nap_the SET status='success',updated_at=NOW() WHERE id=?";
                } else if ("napthe".equals(table)) {
                    String username;
                    try (PreparedStatement ps = connection.prepareStatement("SELECT user_nap,amount,status FROM napthe WHERE id=? FOR UPDATE")) {
                        ps.setInt(1, id);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next() || rs.getInt("status") == 1) return error("Giao dịch đã duyệt hoặc không tồn tại");
                            username = rs.getString("user_nap"); amount = rs.getInt("amount");
                        }
                    }
                    try (PreparedStatement ps = connection.prepareStatement("SELECT id FROM account WHERE username=?")) {
                        ps.setString(1, username);
                        try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) return error("Không tìm thấy tài khoản"); accountId = rs.getInt(1); }
                    }
                    update = "UPDATE napthe SET status=1 WHERE id=?";
                } else {
                    return error("Loại giao dịch không hỗ trợ duyệt thủ công");
                }
                try (PreparedStatement ps = connection.prepareStatement("UPDATE account SET vnd=vnd+?,tongnap=tongnap+? WHERE id=?")) {
                    ps.setInt(1, amount); ps.setInt(2, amount); ps.setInt(3, accountId); ps.executeUpdate();
                }
                try (PreparedStatement ps = connection.prepareStatement(update)) { ps.setInt(1, id); ps.executeUpdate(); }
                connection.commit();
                return ok("Đã duyệt và cộng " + amount + " Cá");
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void reloadIfNeeded(String table) {
        if ("giftcode".equals(table)) PanelCommandService.gI().executeNow("LOAD_GIFTCODE");
        if ("power_limit".equals(table)) PanelCommandService.gI().executeNow("LOAD_POWER_LIMIT");
    }

    private void serveIcon(HttpExchange exchange, int iconId) throws IOException {
        if (iconId < 0) { sendJson(exchange, 404, error("Icon không hợp lệ")); return; }
        Path icon = Path.of("data", "icon", "x1", iconId + ".png").normalize();
        if (!Files.isRegularFile(icon)) { sendJson(exchange, 404, error("Không tìm thấy icon")); return; }
        sendBytes(exchange, 200, "image/png", Files.readAllBytes(icon));
    }

    @SuppressWarnings("unchecked")
    private JSONArray readBosses() {
        try {
            Object parsed = JSONValue.parse(Files.readString(Path.of("boss_status.json"), StandardCharsets.UTF_8));
            if (parsed instanceof JSONObject object && object.get("bosses") instanceof JSONArray array) return array;
        } catch (Exception ignored) { }
        return new JSONArray();
    }

    private String loadPanelHtml() throws IOException {
        try (InputStream input = AdminPanelServer.class.getResourceAsStream("/nro/models/server/admin-panel.html")) {
            if (input != null) return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        Path source = Path.of("src", "nro", "models", "server", "admin-panel.html");
        if (Files.isRegularFile(source)) return Files.readString(source, StandardCharsets.UTF_8);
        throw new IOException("Thiếu giao diện admin-panel.html");
    }

    private JSONObject readJson(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Object parsed = JSONValue.parse(body);
        return parsed instanceof JSONObject object ? object : new JSONObject();
    }

    private Map<String, String> parseForm(String raw) {
        Map<String, String> values = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return values;
        for (String pair : raw.split("&")) {
            String[] parts = pair.split("=", 2);
            values.put(decode(parts[0]), parts.length == 2 ? decode(parts[1]) : "");
        }
        return values;
    }

    private static Map<String, QuerySpec> createQueries() {
        Map<String, QuerySpec> map = new LinkedHashMap<>();
        map.put("topups", new QuerySpec("nap_the", "SELECT id,'nap_the' source,account_id,card_type,card_amount amount,status,serial,created_at FROM nap_the WHERE CAST(account_id AS CHAR) LIKE ? OR card_type LIKE ? OR serial LIKE ? ORDER BY id DESC", 3));
        map.put("accounts", new QuerySpec("account", "SELECT id,username,email,ban,is_admin,active,vnd,tongnap,vip,last_time_login,ip_address FROM account WHERE username LIKE ? OR email LIKE ? OR CAST(id AS CHAR) LIKE ? ORDER BY id DESC", 3));
        map.put("players", new QuerySpec("player", "SELECT id,account_id,name,gender,head,clan_id,rank,event_point,point_sukien,create_time FROM player WHERE name LIKE ? OR CAST(id AS CHAR) LIKE ? ORDER BY id DESC", 2));
        map.put("giftcodes", new QuerySpec("giftcode", "SELECT id,code,count_left,detail,datecreate,expired,type FROM giftcode WHERE code LIKE ? OR detail LIKE ? ORDER BY id DESC", 2));
        map.put("items", new QuerySpec("item_template", "SELECT id,TYPE,gender,NAME,description,level,icon_id,power_require,gold,gem FROM item_template WHERE NAME LIKE ? OR CAST(id AS CHAR) LIKE ? OR CAST(icon_id AS CHAR) LIKE ? ORDER BY id DESC", 3));
        map.put("shops", new QuerySpec("item_shop", "SELECT i.id,s.tag_name shop,t.NAME tab_name,i.tab_id,i.temp_id,p.NAME item_name,i.is_sell,i.type_sell,i.cost,i.sort_order FROM item_shop i JOIN tab_shop t ON t.id=i.tab_id JOIN shop s ON s.id=t.shop_id LEFT JOIN item_template p ON p.id=i.temp_id WHERE s.tag_name LIKE ? OR t.NAME LIKE ? OR p.NAME LIKE ? ORDER BY i.id DESC", 3));
        map.put("power", new QuerySpec("power_limit", "SELECT id,power,hp,mp,damage,defense,critical FROM power_limit WHERE CAST(id AS CHAR) LIKE ? ORDER BY id", 1));
        map.put("maps", new QuerySpec("map_template", "SELECT id,NAME,zones,max_player,type,planet_id,bg_type,tile_id,bg_id,is_map_double FROM map_template WHERE NAME LIKE ? OR CAST(id AS CHAR) LIKE ? ORDER BY id", 2));
        map.put("trades", new QuerySpec("history_transaction", "SELECT id,player_1,player_2,item_player_1,item_player_2,time_tran FROM history_transaction WHERE player_1 LIKE ? OR player_2 LIKE ? ORDER BY id DESC", 2));
        map.put("content", new QuerySpec("notify", "SELECT id,name,text FROM notify WHERE name LIKE ? OR text LIKE ? ORDER BY id DESC", 2));
        map.put("icons", new QuerySpec("item_template", "SELECT id,NAME,icon_id,description FROM item_template WHERE NAME LIKE ? OR CAST(icon_id AS CHAR) LIKE ? ORDER BY icon_id DESC", 2));
        return map;
    }

    private Properties loadConfig() {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(Path.of("Config.properties"))) { properties.load(input); } catch (Exception ignored) { }
        return properties;
    }

    private void openBrowserLater(String url) {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(1200L);
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(url));
            } catch (Exception ignored) { }
        }, "Open admin panel");
        thread.setDaemon(true);
        thread.start();
    }

    private void addSecurityHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.getResponseHeaders().set("Content-Security-Policy", "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'");
    }

    private void sendJson(HttpExchange exchange, int status, JSONObject value) throws IOException { send(exchange, status, "application/json; charset=UTF-8", value.toJSONString()); }
    private void send(HttpExchange exchange, int status, String type, String text) throws IOException { sendBytes(exchange, status, type, text.getBytes(StandardCharsets.UTF_8)); }
    private void sendBytes(HttpExchange exchange, int status, String type, byte[] data) throws IOException { exchange.getResponseHeaders().set("Content-Type", type); exchange.sendResponseHeaders(status, data.length); exchange.getResponseBody().write(data); }
    private String decode(String value) { return URLDecoder.decode(value, StandardCharsets.UTF_8); }
    private String string(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    private String cleanPipe(Object value) { return string(value).replace("|", ""); }
    private int integer(Object value) { return parseInt(string(value), 0); }
    private int positive(Object value) { return Math.max(1, integer(value)); }
    private int positiveOr(Object value, int fallback) { int number = integer(value); return number > 0 ? number : fallback; }
    private String boolFlag(Object value) { return Boolean.parseBoolean(string(value)) ? "1" : "0"; }
    private int parseInt(String value, int fallback) { try { return Integer.parseInt(value == null ? "" : value.trim()); } catch (Exception ignored) { return fallback; } }

    @SuppressWarnings("unchecked")
    private JSONObject ok(String message) { JSONObject json = new JSONObject(); json.put("ok", true); json.put("message", message); return json; }
    @SuppressWarnings("unchecked")
    private JSONObject error(String message) { JSONObject json = new JSONObject(); json.put("ok", false); json.put("message", message); return json; }

    private record QuerySpec(String table, String sql, int searchParams) { }
}
