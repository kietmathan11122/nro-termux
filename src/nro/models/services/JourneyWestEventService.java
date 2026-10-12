package nro.models.services;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.boss.event_tet.DuongTangEscort;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstNpc;
import nro.models.consts.ConstPlayer;
import nro.models.event.EventManager;
import nro.models.item.Item;
import nro.models.map.service.ChangeMapService;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.server.Manager;
import nro.models.utils.Util;

/** Hộ tống Đường Tăng và phần thưởng Hộp Tây Du của sự kiện Tết. */
public class JourneyWestEventService {

    public static final int BOX_ITEM_ID = 2258;
    public static final int[] COLLECTION_IDS = {547, 548, 1932, 920};
    private final List<DuongTangEscort> escorts = new CopyOnWriteArrayList<>();
    private final java.util.Map<Long, DuongTangEscort> playerEscorts = new ConcurrentHashMap<>();
    private static JourneyWestEventService instance;

    public static JourneyWestEventService gI() {
        if (instance == null) instance = new JourneyWestEventService();
        return instance;
    }

    public synchronized void startEvent() {
        stopEvent();
        if (!EventManager.LUNNAR_NEW_YEAR) return;
        for (nro.models.map.Map map : Manager.MAPS) {
            if (map == null || map.mapId == 0 || map.planetId != ConstPlayer.TRAI_DAT
                    || map.zones == null || map.zones.isEmpty()) continue;
            try {
                DuongTangEscort escort = new DuongTangEscort(map.mapId);
                escorts.add(escort);
            } catch (Exception ignored) {
            }
        }
    }

    public synchronized void stopEvent() {
        for (DuongTangEscort escort : escorts) {
            if (escort == null) continue;
            escort.cancelEscort();
            if (escort.zone != null) ChangeMapService.gI().exitMap(escort);
            escort.changeStatus(BossStatus.REST);
            BossManager.gI().removeBoss(escort);
        }
        escorts.clear();
        playerEscorts.clear();
    }

    public boolean register(Player player, DuongTangEscort escort) {
        return playerEscorts.putIfAbsent(player.id, escort) == null;
    }

    public void unregister(Player player, DuongTangEscort escort) {
        if (player != null) playerEscorts.remove(player.id, escort);
    }

    public void followAfterMapChange(Player player) {
        DuongTangEscort escort = playerEscorts.get(player.id);
        if (escort != null) escort.followTo(player);
    }

    public void deliver(Player player, Npc npc) {
        DuongTangEscort escort = playerEscorts.get(player.id);
        if (escort == null) {
            Service.gI().sendThongBao(player, "Bạn chưa dẫn theo Đường Tăng bị lạc.");
            return;
        }
        escort.deliverAtNpc(player, npc);
    }

    public boolean rewardEscort(Player player) {
        if (!EventManager.LUNNAR_NEW_YEAR) return false;
        Item box = ItemService.gI().createNewItem((short) BOX_ITEM_ID, 1);
        if (!InventoryService.gI().addItemBag(player, box)) {
            Service.gI().sendThongBao(player, "Hành trang đã đầy, chưa thể nhận Hộp Tây Du.");
            return false;
        }
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendThongBao(player, "Hộ tống thành công, nhận 1 Hộp Tây Du!");
        return true;
    }

    public void exchangeCake(Player player, int cakeItemId, int boxQuantity, String cakeName) {
        if (!EventManager.LUNNAR_NEW_YEAR) {
            Service.gI().sendThongBao(player, "Sự kiện Tết Nguyên Đán hiện chưa được bật.");
            return;
        }
        Item cake = InventoryService.gI().findItemBag(player, cakeItemId);
        if (cake == null || cake.quantity < 1) {
            Service.gI().sendThongBao(player, "Bạn không có " + cakeName + ".");
            return;
        }
        Item boxes = ItemService.gI().createNewItem((short) BOX_ITEM_ID, boxQuantity);
        if (!InventoryService.gI().addItemBag(player, boxes)) {
            Service.gI().sendThongBao(player, "Hành trang đã đầy, chưa thể nhận Hộp Tây Du.");
            return;
        }
        InventoryService.gI().subQuantityItemsBag(player, cake, 1);
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendThongBao(player,
                "Đã giao 1 " + cakeName + " và nhận " + boxQuantity + " Hộp Tây Du.");
    }

    public void openBox(Player player) {
        Item box = InventoryService.gI().findItemBag(player, BOX_ITEM_ID);
        if (box == null || box.quantity < 1) return;
        int roll = Util.nextInt(1, 100);
        String text;
        if (roll <= 45) {
            if (InventoryService.gI().getCountEmptyBag(player) < 1) return;
            int[] costumes = {547, 548, 1932};
            Item reward = ItemService.gI().createNewItem((short) costumes[Util.nextInt(costumes.length)]);
            reward.itemOptions.clear();
            reward.itemOptions.add(new Item.ItemOption(50, Util.nextInt(10, 25)));
            reward.itemOptions.add(new Item.ItemOption(77, Util.nextInt(10, 25)));
            reward.itemOptions.add(new Item.ItemOption(103, Util.nextInt(10, 25)));
            reward.itemOptions.add(new Item.ItemOption(30, 0));
            addDuration(reward);
            InventoryService.gI().addItemBag(player, reward);
            text = "Bạn nhận được " + reward.template.name + ".";
        } else if (roll <= 60) {
            if (InventoryService.gI().getCountEmptyBag(player) < 1) return;
            Item staff = ItemService.gI().createNewItem((short) 920);
            staff.itemOptions.clear();
            int[] options = {50, 77, 103};
            staff.itemOptions.add(new Item.ItemOption(options[Util.nextInt(options.length)], Util.nextInt(5, 7)));
            staff.itemOptions.add(new Item.ItemOption(30, 0));
            addDuration(staff);
            InventoryService.gI().addItemBag(player, staff);
            text = "Bạn nhận được Gậy Như Ý.";
        } else if (roll <= 75) {
            int gold = Util.nextInt(10_000_000, 100_000_000);
            player.inventory.addGold(gold);
            Service.gI().sendMoney(player);
            text = "Bạn nhận được " + Util.numberToMoney(gold) + " vàng.";
        } else if (roll <= 85) {
            int gem = Util.nextInt(100, 1_000);
            player.inventory.gem = Math.min(2_000_000_000, player.inventory.gem + gem);
            Service.gI().sendMoney(player);
            text = "Bạn nhận được " + gem + " ngọc.";
        } else if (roll <= 95) {
            if (InventoryService.gI().getCountEmptyBag(player) < 1) return;
            Item ball = ItemService.gI().createNewItem((short) Util.nextInt(14, 20), 1);
            InventoryService.gI().addItemBag(player, ball);
            text = "Bạn nhận được " + ball.template.name + ".";
        } else {
            if (InventoryService.gI().getCountEmptyBag(player) < 1) return;
            Item power = ItemService.gI().createNewItem((short) 381, Util.nextInt(1, 3));
            InventoryService.gI().addItemBag(player, power);
            text = "Bạn nhận được vật phẩm tăng sức đánh " + power.template.name + ".";
        }
        InventoryService.gI().subQuantityItemsBag(player, box, 1);
        InventoryService.gI().sendItemBags(player);
        Service.gI().point(player);
        Service.gI().sendThongBao(player, text);
    }

    private void addDuration(Item item) {
        if (!Util.isTrue(5, 100)) {
            item.itemOptions.add(new Item.ItemOption(93, Util.nextInt(7, 30)));
        }
    }
}
