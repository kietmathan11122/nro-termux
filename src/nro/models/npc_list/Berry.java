package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.TaskService;
import nro.models.shop.ShopService;
import nro.models.item.Item;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;

public class Berry extends Npc {

    public Berry(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                if (player.playerTask.taskMain.id >= 33) {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Cậu cần gì ở ta?", "Cửa\nhàng", "Đổi 100\nLinh hồn", "Đổi 99 Băng\nbuộc đầu", "Mua\nTúi giữ nhiệt\n100 ngọc");
                } else {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Cậu cần gì ở ta?", "Cửa\nhàng", "Đổi 100\nLinh hồn", "Đổi 99 Băng\nbuộc đầu");
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.isBaseMenu()) {
                switch (select) {
                    case 0 -> ShopService.gI().opendShop(player, "BERRY", true);
                    case 1 -> {
                        Item linhHon = InventoryService.gI().findItemBag(player, 2007);
                        if (linhHon != null && linhHon.quantity >= 100) {
                            if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                InventoryService.gI().subQuantityItemsBag(player, linhHon, 100);
                                Item banDo = ItemService.gI().createNewItem((short) 2008);
                                InventoryService.gI().addItemBag(player, banDo);
                                InventoryService.gI().sendItemBags(player);
                                Service.gI().sendThongBao(player, "Bạn nhận được " + banDo.template.name);
                            } else {
                                Service.gI().sendThongBao(player, "Hành trang không đủ chỗ trống");
                            }
                        } else {
                            Service.gI().sendThongBao(player, "Bạn không đủ 100 Linh hồn để đổi");
                        }
                    }
                    case 2 -> {
                        Item bangBuocDau = InventoryService.gI().findItemBag(player, 2009);
                        if (bangBuocDau != null && bangBuocDau.quantity >= 99) {
                            if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                InventoryService.gI().subQuantityItemsBag(player, bangBuocDau, 99);
                                Item ngocNangCap = ItemService.gI().createNewItem((short) 2030); // ID của Ngọc Nâng Cấp
                                InventoryService.gI().addItemBag(player, ngocNangCap);
                                InventoryService.gI().sendItemBags(player);
                                Service.gI().sendThongBao(player, "Bạn nhận được " + ngocNangCap.template.name);
                            } else {
                                Service.gI().sendThongBao(player, "Hành trang không đủ chỗ trống");
                            }
                        } else {
                            Service.gI().sendThongBao(player, "Bạn không đủ 99 Băng buộc đầu để đổi");
                        }
                    }
                    case 3 -> {
                        if (player.playerTask.taskMain.id >= 33) {
                            if (player.inventory.getGem() >= 100) {
                                if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                                    player.inventory.subGem(100);
                                    Item tuiGiuNhiet = ItemService.gI().createNewItem((short) 2012);
                                    tuiGiuNhiet.itemOptions.add(new Item.ItemOption(106, 1));
                                    InventoryService.gI().addItemBag(player, tuiGiuNhiet);
                                    InventoryService.gI().sendItemBags(player);
                                    Service.gI().sendMoney(player);
                                    Service.gI().sendThongBao(player, "Bạn nhận được " + tuiGiuNhiet.template.name);
                                } else {
                                    Service.gI().sendThongBao(player, "Hành trang không đủ chỗ trống");
                                }
                            } else {
                                Service.gI().sendThongBao(player, "Bạn không đủ 100 ngọc");
                            }
                        }
                    }
                }
            }
        }
    }
}
