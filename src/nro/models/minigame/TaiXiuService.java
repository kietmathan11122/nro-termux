package nro.models.minigame;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import nro.models.data.LocalManager;
import nro.models.database.PlayerEconomyDAO;
import nro.models.network.Message;
import nro.models.player.Inventory;
import nro.models.player.Player;
import nro.models.server.Client;
import nro.models.services.Service;
import org.json.simple.JSONObject;

/** Gold bets and payouts share the persisted player wallet. Dice are kept in a durable round journal. */
public final class TaiXiuService implements Runnable {
    public static final int PACKET_TYPE_TAIXIU = 3;
    public static final int BET_TAI = 0, BET_XIU = 1, BET_CHAN = 2, BET_LE = 3;
    public static final double RATIO = 1.8;
    public static final int COST = 1_000;
    public long betSeconds = 50;
    private static final String[] NAMES = {"Tài", "Xỉu", "Chẵn", "Lẻ"};
    private static final TaiXiuService INSTANCE = new TaiXiuService();
    private final SecureRandom random = new SecureRandom();
    private final Set<Long> viewers = new HashSet<>();
    private Round round;
    private boolean schemaReady;
    private java.util.List<Round> history = new java.util.ArrayList<>();
    private long historyRound = -1;
    private TaiXiuService() { }
    public static TaiXiuService gI() { return INSTANCE; }

    @Override public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                synchronized (this) {
                    advance();
                    for (Iterator<Long> it = viewers.iterator(); it.hasNext();) {
                        Player player = Client.gI().getPlayer(it.next());
                        if (!eligible(player)) { it.remove(); continue; }
                        synchronized (player) {
                            if (!eligible(player)) { it.remove(); continue; }
                            settle(player);
                            if (eligible(player)) sendState(player, "");
                        }
                    }
                }
                Thread.sleep(1000);
            } catch (InterruptedException stopped) { Thread.currentThread().interrupt(); }
            catch (Exception error) {
                System.err.println("TaiXiu: round journal unavailable: " + error.getClass().getSimpleName());
                try { Thread.sleep(5000); } catch (InterruptedException stopped) { Thread.currentThread().interrupt(); }
            }
        }
    }

    public void handleClientMessage(Player player, Message msg) {
        try {
            int action = msg.reader().readUnsignedByte();
            if (action == 0) sendTaiXiuInfo(player);
            else if (action == 1) placeBet(player, msg.reader().readUnsignedByte(), msg.reader().readLong());
            else if (action == 2 && player != null) { synchronized (this) { viewers.remove(player.id); } }
        } catch (Exception malformed) { notifyPlayer(player, "Dữ liệu Tài xỉu không hợp lệ."); }
    }

    public synchronized void sendTaiXiuInfo(Player player) {
        if (!eligible(player)) return;
        try {
            advance();
            viewers.add(player.id);
            synchronized (player) { if (!eligible(player)) return; settle(player); if (eligible(player)) sendState(player, ""); }
        } catch (Exception unavailable) { notifyPlayer(player, "Tài xỉu đang mất kết nối dữ liệu. Vui lòng thử lại."); }
    }

    public synchronized void placeBet(Player player, int type, long amount) {
        if (!eligible(player)) return;
        try {
            advance();
            viewers.add(player.id);
            synchronized (player) {
                if (!eligible(player)) return;
                String failure = null;
                if (player.isDie()) failure = "Hãy hồi sinh trước khi đặt cược.";
                else if (!TaiXiuRules.validBet(type, amount)) failure = "Cược từ 10 triệu đến 100 tỷ vàng, mỗi ván một cửa.";
                else if (!settle(player)) failure = "Tiền thưởng đang chờ nhận. Hãy giảm số vàng đang giữ rồi thử lại.";
                else if (round.a != 0 || System.currentTimeMillis() >= round.closesAt) failure = "Đã hết giờ đặt cược.";
                else if (player.inventory.gold < amount) failure = "Bạn không đủ vàng.";
                if (!eligible(player)) return;
                JSONObject gameData = PlayerEconomyDAO.copyGameData(player);
                JSONObject bet = object(gameData.get("taiXiuBet"));
                if (failure == null && bet != null && number(bet, "round") == round.id) {
                    if (number(bet, "type") != type) failure = "Mỗi ván chỉ được chọn một cửa.";
                    else if (number(bet, "amount") + amount > TaiXiuRules.MAX_BET) failure = "Tổng cược tối đa 100 tỷ vàng mỗi ván.";
                }
                if (failure != null) { notifyPlayer(player, failure); sendState(player, failure); return; }
                long previous = bet != null && number(bet, "round") == round.id ? number(bet, "amount") : 0;
                JSONObject updated = new JSONObject();
                updated.put("round", round.id); updated.put("type", type); updated.put("amount", previous + amount);
                gameData.put("taiXiuBet", updated);
                if (!PlayerEconomyDAO.commit(player, player.inventory.gold - amount, gameData)) return;
                Service.gI().sendMoney(player);
                sendState(player, "");
            }
        } catch (Exception unavailable) { notifyPlayer(player, "Chưa đặt cược: không thể lưu dữ liệu. Vui lòng thử lại."); }
    }

    /** Idempotent claim: the bet and wallet are changed by the same durable write. */
    private boolean settle(Player player) throws Exception {
        JSONObject data = PlayerEconomyDAO.copyGameData(player);
        JSONObject bet = object(data.get("taiXiuBet"));
        if (bet == null) return true;
        long id = number(bet, "round");
        Round result = id == round.id ? round : findRound(id);
        if (result == null) throw new IllegalStateException("Missing round journal");
        if (result.a == 0) return true;
        long amount = number(bet, "amount"); int type = (int)number(bet, "type");
        long reward = TaiXiuRules.payout(type, amount, result.a, result.b, result.c);
        if (player.inventory.gold > Inventory.LIMIT_GOLD - reward) return false;
        JSONObject last = new JSONObject();
        last.putAll(bet);
        last.put("payout", reward); last.put("a", result.a); last.put("b", result.b); last.put("c", result.c);
        data.put("taiXiuLast", last); data.remove("taiXiuBet");
        if (!PlayerEconomyDAO.commit(player, player.inventory.gold + reward, data)) return false;
        Service.gI().sendMoney(player);
        if (reward > 0) notifyPlayer(player, "Tài xỉu: đã nhận " + reward + " vàng của ván " + id + ".");
        return true;
    }

    private void sendState(Player player, String notice) throws Exception {
        JSONObject data = PlayerEconomyDAO.copyGameData(player);
        JSONObject bet = object(data.get("taiXiuBet"));
        JSONObject last = object(data.get("taiXiuLast"));
        JSONObject visible = bet != null && number(bet,"round") == round.id ? bet
                : last != null && number(last,"round") == round.id ? last : null;
        boolean betting = round.a == 0;
        Message out = new Message(-126);
        try {
            out.writer().writeByte(PACKET_TYPE_TAIXIU);
            out.writer().writeByte(betting ? 0 : 1);
            out.writer().writeByte((int)Math.min(127, Math.max(0, ((betting ? round.closesAt : round.revealedAt + 10000) - System.currentTimeMillis() + 999) / 1000)));
            out.writer().writeByte(betting ? 0 : round.a);
            out.writer().writeByte(betting ? 0 : round.b);
            out.writer().writeByte(betting ? 0 : round.c);
            out.writer().writeByte(visible == null ? -1 : (int)number(visible,"type"));
            out.writer().writeLong(visible == null ? 0 : number(visible,"amount"));
            String resultText = notice;
            if (bet != null && number(bet,"round") != round.id)
                resultText = "Thưởng đang chờ; hãy bớt vàng trong ví.";
            if (!betting) {
                int sum = round.a + round.b + round.c;
                resultText = "Kết quả: " + round.a + " + " + round.b + " + " + round.c + " = " + sum;
                if (bet != null && number(bet,"round") != round.id)
                    resultText += "\nThưởng ván cũ chờ nhận: ví vàng đầy.";
                if (visible != null) {
                    long payout = TaiXiuRules.payout((int)number(visible,"type"), number(visible,"amount"), round.a, round.b, round.c);
                    resultText += "\n" + (payout > 0 ? "Nhận " + payout + " vàng" : "Không trúng thưởng");
                    if (bet != null) resultText += "\nTiền thưởng chờ nhận: ví vàng đã đầy.";
                }
            }
            out.writer().writeUTF(resultText);
            // Keep the old optional jackpot fields for client packet compatibility.
            out.writer().writeInt(0); out.writer().writeByte(0); out.writer().writeInt(0); out.writer().writeUTF("");
            out.writer().writeByte(history.size());
            for (Round past : history) { out.writer().writeLong(past.id); out.writer().writeByte(past.a); out.writer().writeByte(past.b); out.writer().writeByte(past.c); }
            player.sendMessage(out);
        } finally { out.cleanup(); }
    }

    private void advance() throws Exception {
        if (!schemaReady) {
            try (Connection connection = LocalManager.getConnection(); Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS hactinh_taixiu_round (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, closes_at BIGINT NOT NULL, revealed_at BIGINT NOT NULL DEFAULT 0, dice1 TINYINT NOT NULL DEFAULT 0, dice2 TINYINT NOT NULL DEFAULT 0, dice3 TINYINT NOT NULL DEFAULT 0) ENGINE=InnoDB");
            }
            schemaReady = true;
        }
        if (round == null) {
            try (Connection c = LocalManager.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT * FROM hactinh_taixiu_round ORDER BY id DESC LIMIT 1"); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) round = readRound(rs);
            }
        }
        long now = System.currentTimeMillis();
        if (round != null && round.a == 0 && now >= round.closesAt) {
            int a = random.nextInt(6) + 1, b = random.nextInt(6) + 1, d = random.nextInt(6) + 1;
            try (Connection c = LocalManager.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE hactinh_taixiu_round SET dice1=?,dice2=?,dice3=?,revealed_at=? WHERE id=? AND dice1=0")) {
                ps.setInt(1,a); ps.setInt(2,b); ps.setInt(3,d); ps.setLong(4,now); ps.setLong(5,round.id); ps.executeUpdate();
            }
            round = findRound(round.id); historyRound = -1;
        }
        if (round == null || (round.a != 0 && now >= round.revealedAt + 10000)) {
            long closes = now + Math.max(10, Math.min(120, betSeconds)) * 1000;
            try (Connection c = LocalManager.getConnection(); PreparedStatement ps = c.prepareStatement("INSERT INTO hactinh_taixiu_round(closes_at) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1,closes); ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) throw new IllegalStateException("Missing new round id");
                    round = new Round(rs.getLong(1), closes, 0, 0, 0, 0);
                }
            }
        }
        refreshHistory();
    }
    private void refreshHistory() throws Exception {
        if (historyRound == round.id) return;
        java.util.List<Round> latest = new java.util.ArrayList<>();
        try (Connection c = LocalManager.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT * FROM hactinh_taixiu_round WHERE dice1<>0 ORDER BY id DESC LIMIT 20"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) latest.add(readRound(rs));
        }
        history = latest; historyRound = round.id;
    }
    private Round findRound(long id) throws Exception {
        try (Connection c = LocalManager.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT * FROM hactinh_taixiu_round WHERE id=?")) {
            ps.setLong(1,id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? readRound(rs) : null; }
        }
    }
    private static Round readRound(ResultSet rs) throws Exception {
        return new Round(rs.getLong("id"),rs.getLong("closes_at"),rs.getLong("revealed_at"),rs.getInt("dice1"),rs.getInt("dice2"),rs.getInt("dice3"));
    }
    private static JSONObject object(Object value) { return value instanceof JSONObject ? (JSONObject)value : null; }
    private static long number(JSONObject obj, String key) { Object value = obj.get(key); return value instanceof Number ? ((Number)value).longValue() : 0; }
    private static boolean eligible(Player player) {
        return player != null && player.isPl() && !player.beforeDispose && player.inventory != null
                && !player.inventory.gameDataPersistenceBlocked && player.idMark != null
                && player.idMark.isLoadedAllDataPlayer() && player.getSession() != null
                && player.getSession().player == player && player.getSession().isConnected();
    }
    private static void notifyPlayer(Player player, String message) { if (player != null && player.getSession() != null) Service.gI().sendThongBao(player,message); }
    public synchronized boolean isInBettingPhase() { return round != null && round.a == 0 && System.currentTimeMillis() < round.closesAt; }
    public synchronized long getSecond() { return round == null ? 0 : Math.max(0,(round.closesAt-System.currentTimeMillis()+999)/1000); }
    private static final class Round {
        final long id, closesAt, revealedAt; final int a,b,c;
        Round(long id,long closesAt,long revealedAt,int a,int b,int c) { this.id=id;this.closesAt=closesAt;this.revealedAt=revealedAt;this.a=a;this.b=b;this.c=c; }
    }
}
