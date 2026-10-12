package nro.models.services;

import nro.models.player.Player;
import nro.models.player.Pet;
import nro.models.item.Item;
import nro.models.server.Manager;
import nro.models.services_func.EffectMapService;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;

/**
 * Service quản lý hiệu ứng bước chân hoa hồng nở cho nhân vật và đệ tử khi di chuyển.
 */
public class FootstepService {

    private static FootstepService instance;

    // Effect IDs (DataEffect_125 / DataEffect_126)
    public static final int EFFECT_ROSE_BLUE = 125;
    public static final int EFFECT_ROSE_RED = 126;

    // Item IDs
    public static final int ITEM_ROSE_RED = 2439;
    public static final int ITEM_ROSE_BLUE = 2440;
    public static final int ITEM_WATER = 2441;

    // Khoảng cách tối thiểu giữa 2 lần nở hoa (pixels)
    private static final int MIN_DISTANCE_SQ = 28 * 28;
    // Thời gian hồi tối thiểu giữa 2 bước chân (ms)
    private static final long MIN_INTERVAL_MS = 260L;

    public static FootstepService gI() {
        if (instance == null) {
            instance = new FootstepService();
        }
        return instance;
    }

    /**
     * Xác định ID hiệu ứng bước chân đang kích hoạt cho nhân vật hoặc đệ tử.
     * Đệ tử sẽ tự động thừa hưởng bước chân của sư phụ.
     */
    public int getFootstepEffectId(Player player) {
        if (player == null) {
            return 0;
        }

        // Nếu là đệ tử (Pet), lấy hiệu ứng bước chân từ sư phụ
        if (player instanceof Pet) {
            Pet pet = (Pet) player;
            if (pet.master != null) {
                return getFootstepEffectId(pet.master);
            }
            return 0;
        }

        // 1. Nếu người chơi đã kích hoạt thủ công qua lệnh / vật phẩm
        if (player.activeFootstepEffect > 0) {
            return player.activeFootstepEffect;
        }

        // 2. Tự động kiểm tra vật phẩm trong hành trang (itemsBag) hoặc trên người (itemsBody)
        if (player.inventory != null) {
            if (hasItem(player, ITEM_ROSE_BLUE)) {
                return EFFECT_ROSE_BLUE;
            }
            if (hasItem(player, ITEM_ROSE_RED)) {
                return EFFECT_ROSE_RED;
            }
        }

        return 0;
    }

    private boolean hasItem(Player player, int itemId) {
        if (player.inventory == null) {
            return false;
        }
        if (player.inventory.itemsBody != null) {
            for (Item item : player.inventory.itemsBody) {
                if (item != null && item.isNotNullItem() && item.template != null && item.template.id == itemId) {
                    return true;
                }
            }
        }
        if (player.inventory.itemsBag != null) {
            for (Item item : player.inventory.itemsBag) {
                if (item != null && item.isNotNullItem() && item.template != null && item.template.id == itemId) {
                    return true;
                }
            }
        }
        return false;
    }

    // Độ lệch trục Y (càng lớn càng hạ thấp xuống mặt đất)
    public static int offsetY = 2;

    /**
     * Xử lý tạo hoa nở theo từng bước chân khi nhân vật hoặc đệ tử di chuyển trên mặt đất.
     */
    public void handleMove(Player player, int x, int y, boolean isFlying) {
        if (player == null || player.zone == null || player.isDie()) {
            return;
        }

        // Chỉ tạo bước chân khi tiếp đất (không bay)
        if (isFlying || player.isFly) {
            return;
        }

        int effId = getFootstepEffectId(player);
        if (effId <= 0) {
            return;
        }

        long now = System.currentTimeMillis();
        int dx = x - player.lastFootstepX;
        int dy = y - player.lastFootstepY;
        int distSq = dx * dx + dy * dy;

        // Kích hoạt khi đủ bước đi hoặc khi đã dừng một nhịp rồi bước tiếp
        if (now - player.lastFootstepTime >= MIN_INTERVAL_MS && (distSq >= MIN_DISTANCE_SQ || now - player.lastFootstepTime >= 600L)) {
            player.lastFootstepTime = now;
            player.lastFootstepX = x;
            player.lastFootstepY = y;

            // Gửi hiệu ứng map (CMD 113) cho toàn bộ người chơi trong khu vực cùng thấy
            // loop = 1: chạy hiệu ứng 1 lần từ nụ -> hoa nở -> rơi cánh
            // layer = 2: vẽ ở lớp mặt đất (dưới chân nhân vật)
            // delay = -1: xuất hiện ngay tức thì
            EffectMapService.gI().sendEffectMapToAllInMap(player.zone, effId, 2, 1, x, y + offsetY, -1);
        }
    }

    /**
     * Bật / Tắt hiệu ứng bước chân
     */
    public void toggleFootstep(Player player, int effectId, String name) {
        if (player == null) {
            return;
        }
        if (player.activeFootstepEffect == effectId) {
            player.activeFootstepEffect = 0;
            Service.gI().sendThongBao(player, "Đã tắt bước chân " + name + ".");
        } else {
            player.activeFootstepEffect = effectId;
            Service.gI().sendThongBao(player, "Đã kích hoạt bước chân " + name + " cho bạn và đệ tử!");
        }
    }

    /**
     * Tặng vật phẩm bước chân cho người chơi (dùng cho lệnh test hoặc admin/sự kiện)
     */
    public void giveFootstepItem(Player player, int itemId) {
        if (player == null || player.inventory == null) {
            return;
        }
        Item item = ItemService.gI().createNewItem((short) itemId);
        if (item != null) {
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được: " + item.template.name);
        }
    }
}
