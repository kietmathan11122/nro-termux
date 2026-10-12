package nro.models.services;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import nro.models.event.EventManager;
import nro.models.item.Item;
import nro.models.player.Player;

/** Các phần thưởng/nguyên liệu dùng chung của sự kiện Tết Nguyên Đán. */
public class LunarNewYearEventService {

    public static final int CHA_LUA = 1544;
    public static final int MUOI_TIEU = 1545;
    public static final int COM_NEP = 1546;
    public static final int BOT_GAO = 1547;
    public static final int DAU_XANH = 1548;
    public static final int THIT_TUOI = 1549;

    private final java.util.Map<Long, LocalDate> claimDates = new ConcurrentHashMap<>();
    private final java.util.Map<Long, Set<String>> claims = new ConcurrentHashMap<>();
    private static LunarNewYearEventService instance;

    public static LunarNewYearEventService gI() {
        if (instance == null) {
            instance = new LunarNewYearEventService();
        }
        return instance;
    }

    /** Mỗi nhân vật nhận 1 Bột Gạo từ từng NPC, mỗi ngày một lần. */
    public void giveRiceFlourFromNpc(Player player, int npcTemplateId, int mapId) {
        if (!EventManager.LUNNAR_NEW_YEAR || player == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate oldDate = claimDates.put(player.id, today);
        if (!today.equals(oldDate)) {
            claims.remove(player.id);
        }
        Set<String> claimed = claims.computeIfAbsent(player.id,
                ignored -> ConcurrentHashMap.newKeySet());
        // Có thể có cùng một mẫu NPC trên nhiều hành tinh, nên tính theo cả map.
        String claimKey = mapId + ":" + npcTemplateId;
        if (!claimed.add(claimKey)) {
            return;
        }
        Item flour = ItemService.gI().createNewItem((short) BOT_GAO, 1);
        if (InventoryService.gI().addItemBag(player, flour)) {
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player,
                    "NPC tặng bạn 1 Bột Gạo Tết. Mỗi ngày nhận một lần từ NPC này.");
        } else {
            claimed.remove(claimKey);
            Service.gI().sendThongBao(player, "Hành trang đã đầy, chưa thể nhận Bột Gạo.");
        }
    }
}
