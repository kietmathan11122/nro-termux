package nro.models.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import nro.models.data.LocalManager;
import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.player.Player;
import nro.models.utils.Logger;

/** Các mốc nạp tích lũy, nhận một lần cho mỗi tài khoản và mỗi mốc. */
public final class FirstTopupService {
    private static final FirstTopupService INSTANCE = new FirstTopupService();
    public static final int[] MILESTONES = {
        10_000, 20_000, 50_000, 100_000, 150_000,
        200_000, 500_000, 700_000, 1_000_000, 1_500_000
    };

    // Thêm ID item và số lượng cho từng mốc tại đây. Mảng rỗng = chưa mở nhận quà.
    private static final short[][] REWARD_ITEM_IDS = {
        {2269, 2270, 2271, 2272, 1150, 1152, 1153, 1145, 595}, {457, 1538},
        {1145, 457}, {2370, 457, 2273, 2371}, {933, 934, 457},
        {1145, 457, 1150, 2372}, {1795, 457, 2373, 16},
        {14, 15, 16, 17, 18, 19, 20, 457, 1869, 2374},
        {2375, 2376, 2377, 457, 2272, 2002, 1819},
        {1145, 457, 2000, 2375, 2376, 2377, 2107, 2275}
    };
    private static final int[][] REWARD_QUANTITIES = {
        {10, 10, 10, 10, 1, 1, 1, 1, 100}, {100, 5},
        {1, 200}, {1, 200, 1, 1}, {9_999, 999, 100},
        {1, 200, 99, 1}, {1, 200, 1, 50},
        {100, 100, 100, 100, 100, 100, 100, 200, 1, 20},
        {20, 20, 20, 100, 10, 1, 1},
        {1, 200, 1, 50, 50, 50, 1, 1}
    };
    private static final int[] REWARD_GEMS = {0, 20_000, 10_000, 10_000, 0, 0, 0, 0, 0, 0};
    private static volatile boolean tableReady;

    public static FirstTopupService gI() {
        return INSTANCE;
    }

    public void sendInfo(Player player) {
        if (player == null || player.getSession() == null) return;
        try {
            ensureTable();
            sendState(player, readTotalTopup(player));
        } catch (Exception e) {
            Logger.logException(FirstTopupService.class, e);
            Service.gI().sendThongBao(player, "Không thể tải thông tin nạp đầu");
        }
    }

    public synchronized void claim(Player player, int tierIndex) {
        if (player == null || player.getSession() == null) return;
        if (tierIndex < 0 || tierIndex >= MILESTONES.length) return;
        try {
            ensureTable();
            int total = readTotalTopup(player);
            if (total < MILESTONES[tierIndex]) {
                Service.gI().sendThongBao(player, "Chưa đủ tổng nạp cho mốc này");
            } else if (!hasConfiguredReward(tierIndex)) {
                Service.gI().sendThongBao(player, "Mốc này chưa có phần quà");
            } else if (isClaimed(player.getSession().userId, tierIndex)) {
                Service.gI().sendThongBao(player, "Đã nhận quà mốc này");
            } else if (!rewardTemplatesLoaded(tierIndex)) {
                Service.gI().sendThongBao(player, "Vật phẩm mốc quà chưa được nạp, hãy khởi động lại server");
            } else if (player.inventory.gem > 2_000_000_000 - REWARD_GEMS[tierIndex]) {
                Service.gI().sendThongBao(player, "Ngọc đã gần giới hạn, hãy tiêu bớt trước khi nhận quà");
            } else if (InventoryService.gI().getCountEmptyBag(player) < REWARD_ITEM_IDS[tierIndex].length) {
                Service.gI().sendThongBao(player, "Hành trang không đủ ô trống");
            } else {
                // Khóa duy nhất trong database chặn nhận trùng kể cả khi gửi yêu cầu liên tục.
                int inserted = LocalManager.executeUpdate(
                    "INSERT IGNORE INTO first_topup_milestone_reward(account_id,milestone) VALUES (?,?)",
                    player.getSession().userId, MILESTONES[tierIndex]);
                if (inserted == 1) {
                    for (int i = 0; i < REWARD_ITEM_IDS[tierIndex].length; i++) {
                        Item item = ItemService.gI().createNewItem(
                            REWARD_ITEM_IDS[tierIndex][i], REWARD_QUANTITIES[tierIndex][i]);
                        if (REWARD_ITEM_IDS[tierIndex][i] == 1145) {
                            if (tierIndex == 5) {
                                item.itemOptions.add(new Item.ItemOption(77, 30));
                                item.itemOptions.add(new Item.ItemOption(103, 30));
                                item.itemOptions.add(new Item.ItemOption(147, 30));
                                item.itemOptions.add(new Item.ItemOption(5, 30));
                            } else if (tierIndex == 9) {
                                item.itemOptions.add(new Item.ItemOption(77, 100));
                                item.itemOptions.add(new Item.ItemOption(103, 100));
                                item.itemOptions.add(new Item.ItemOption(147, 100));
                                item.itemOptions.add(new Item.ItemOption(5, 100));
                            } else {
                                item.itemOptions.add(new Item.ItemOption(101, tierIndex == 2 ? 150 : 100));
                                if (tierIndex == 2) item.itemOptions.add(new Item.ItemOption(95, 5));
                            }
                            item.content = item.getContent();
                            item.info = item.getInfo();
                        }
                        if (tierIndex == 4 && (REWARD_ITEM_IDS[tierIndex][i] == 933
                                || REWARD_ITEM_IDS[tierIndex][i] == 934)) {
                            // Nâng bông tai đọc số mảnh từ option 31.
                            item.itemOptions.add(new Item.ItemOption(31, REWARD_QUANTITIES[tierIndex][i]));
                            item.content = item.getContent();
                            item.info = item.getInfo();
                        }
                        if (tierIndex == 8 && REWARD_ITEM_IDS[tierIndex][i] == 1819) {
                            item.itemOptions.add(new Item.ItemOption(72, 3));
                            item.content = item.getContent();
                            item.info = item.getInfo();
                        }
                        if (tierIndex == 9 && REWARD_ITEM_IDS[tierIndex][i] == 2000) {
                            item.itemOptions.add(new Item.ItemOption(72, 4));
                            item.content = item.getContent();
                            item.info = item.getInfo();
                        }
                        if (tierIndex == 9 && (REWARD_ITEM_IDS[tierIndex][i] == 2107
                                || REWARD_ITEM_IDS[tierIndex][i] == 2275)) {
                            item.itemOptions.add(new Item.ItemOption(73,
                                    REWARD_ITEM_IDS[tierIndex][i] == 2107 ? 20 : 30));
                            item.content = item.getContent();
                            item.info = item.getInfo();
                        }
                        InventoryService.gI().addItemBag(player, item);
                    }
                    if (REWARD_GEMS[tierIndex] > 0) {
                        player.inventory.gem += REWARD_GEMS[tierIndex];
                        Service.gI().sendMoney(player);
                    }
                    InventoryService.gI().sendItemBags(player);
                    Service.gI().sendThongBao(player, "Đã nhận quà mốc " + MILESTONES[tierIndex] + "đ");
                }
            }
            sendState(player, total);
        } catch (Exception e) {
            Logger.logException(FirstTopupService.class, e);
            Service.gI().sendThongBao(player, "Nhận quà thất bại, vui lòng thử lại");
        }
    }

    private boolean hasConfiguredReward(int tierIndex) {
        short[] ids = REWARD_ITEM_IDS[tierIndex];
        int[] quantities = REWARD_QUANTITIES[tierIndex];
        if (ids.length == 0 || ids.length != quantities.length) return false;
        for (int quantity : quantities) if (quantity <= 0) return false;
        return true;
    }

    private boolean rewardTemplatesLoaded(int tierIndex) {
        for (short itemId : REWARD_ITEM_IDS[tierIndex]) {
            nro.models.player_system.Template.ItemTemplate template = ItemService.gI().getTemplate(itemId);
            if (template == null || template.id != itemId
                    || template.name == null || template.name.isEmpty()) return false;
        }
        return true;
    }

    private int readTotalTopup(Player player) throws Exception {
        int total = 0;
        try (Connection con = LocalManager.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT tongnap FROM account WHERE id=? LIMIT 1")) {
            ps.setInt(1, player.getSession().userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) total = Math.max(0, rs.getInt(1));
            }
        }
        player.getSession().tongnap = total;
        return total;
    }

    private boolean isClaimed(int accountId, int index) throws Exception {
        try (Connection con = LocalManager.getConnection();
             PreparedStatement ps = con.prepareStatement(
                "SELECT 1 FROM first_topup_milestone_reward WHERE account_id=? AND milestone=? LIMIT 1")) {
            ps.setInt(1, accountId);
            ps.setInt(2, MILESTONES[index]);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void sendState(Player player, int total) throws Exception {
        Message msg = null;
        try {
            msg = new Message(-76);
            msg.writer().writeByte(7);
            msg.writer().writeInt(total);
            msg.writer().writeByte(MILESTONES.length);
            for (int i = 0; i < MILESTONES.length; i++) {
                msg.writer().writeInt(MILESTONES[i]);
                msg.writer().writeBoolean(isClaimed(player.getSession().userId, i));
                msg.writer().writeBoolean(hasConfiguredReward(i));
                int rewardCount = hasConfiguredReward(i) ? REWARD_ITEM_IDS[i].length + (REWARD_GEMS[i] > 0 ? 1 : 0) : 0;
                msg.writer().writeByte(rewardCount);
                for (int reward = 0; reward < REWARD_ITEM_IDS[i].length && rewardCount > 0; reward++) {
                    short itemId = REWARD_ITEM_IDS[i][reward];
                    msg.writer().writeShort(itemId);
                    msg.writer().writeInt(REWARD_QUANTITIES[i][reward]);
                    String note = "";
                    if (i == 0 && itemId == 1145) note = "+100% tiềm năng, sức mạnh";
                    else if (i == 1 && itemId == 1538) note = "Mở ngẫu nhiên một món set kích hoạt theo hành tinh";
                    else if (i == 2 && itemId == 1145) note = "+150% tiềm năng, sức mạnh; hút HP 5%";
                    else if (i == 3 && itemId == 2370) note = "Mở ra pet ngẫu nhiên, HP/KI/SĐ +10-15%";
                    else if (i == 3 && itemId == 2371) note = "Chọn một món đồ thần linh có set kích hoạt";
                    else if (i == 3 && itemId == 2273) note = "Sách kỹ năng dùng tại Lính Canh";
                    else if (i == 5 && itemId == 1145) note = "HP, KI, sức đánh, sát thương chí mạng +30%";
                    else if (i == 5 && itemId == 2372) note = "Pet ngẫu nhiên, HP/KI/sức đánh +15-20%";
                    else if (i == 6 && itemId == 1795) note = "Tích 3000 Kilis để đổi đệ Mabư 60 tỷ sức mạnh";
                    else if (i == 6 && itemId == 2373) note = "Chọn 1 bộ gồm 5 món thần linh cùng set kích hoạt, mỗi món 5 sao";
                    else if (i == 6 && itemId == 16) note = "50 Ngọc Rồng 3 sao";
                    else if (i == 7 && itemId >= 14 && itemId <= 20) note = "100 bộ Ngọc Rồng (mỗi loại 100 viên)";
                    else if (i == 7 && itemId == 1869) note = "Giáp luyện tập cấp 5";
                    else if (i == 7 && itemId == 2374) note = "Mở danh sách Boss, dịch chuyển đến Boss còn sống; tiêu hao 1 radar/lần";
                    else if (i == 8 && itemId == 2375) note = "Ép vào trang bị có lỗ sao: +10% sức đánh";
                    else if (i == 8 && itemId == 2376) note = "Ép vào trang bị có lỗ sao: +10% KI";
                    else if (i == 8 && itemId == 2377) note = "Ép vào trang bị có lỗ sao: +10% HP";
                    else if (i == 8 && itemId == 2272) note = "Lọ tiềm năng x10, 30 phút";
                    else if (i == 8 && itemId == 2002) note = "Bình hút năng lượng 2";
                    else if (i == 8 && itemId == 1819) note = "Bông tai Porata cấp 3";
                    else if (i == 9 && itemId == 1145) note = "HP, KI, sức đánh và sức đánh chí mạng +100%";
                    else if (i == 9 && itemId >= 2375 && itemId <= 2377) note = "50 viên đá ép sao, mỗi viên +10% chỉ số tương ứng";
                    else if (i == 9 && itemId == 2000) note = "Bông tai Porata cấp 4";
                    else if (i == 9 && itemId == 2107) note = "Kỳ Lân Thần: +20% mọi chỉ số";
                    else if (i == 9 && itemId == 2275) note = "Pet Ma Nhà Giàu: +30% mọi chỉ số";
                    msg.writer().writeUTF(note);
                }
                if (rewardCount > REWARD_ITEM_IDS[i].length) {
                    msg.writer().writeShort(-1); // Ngọc cộng thẳng, chỉ dùng ID âm để xem trước.
                    msg.writer().writeInt(REWARD_GEMS[i]);
                    msg.writer().writeUTF("Cộng trực tiếp vào số ngọc của nhân vật");
                }
            }
            player.sendMessage(msg);
        } finally {
            if (msg != null) msg.cleanup();
        }
    }

    private void ensureTable() throws Exception {
        if (tableReady) return;
        synchronized (FirstTopupService.class) {
            if (tableReady) return;
            LocalManager.executeUpdate("CREATE TABLE IF NOT EXISTS first_topup_milestone_reward ("
                    + "account_id INT NOT NULL, milestone INT NOT NULL, "
                    + "claimed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "PRIMARY KEY(account_id,milestone))");
            tableReady = true;
        }
    }
}
