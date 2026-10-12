package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.shop.ShopService;
import nro.models.services_func.Input;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.services.LunarNewYearEventService;

public class Santa extends Npc {

    public Santa(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {

            Item pGG = InventoryService.gI().findItem(player.inventory.itemsBag, 459);
            int soLuong = 0;
            if (pGG != null) {
                soLuong = pGG.quantity;
            }
            List<String> menu = new ArrayList<>(Arrays.asList(
                    "Cửa hàng",
                    "Mở rộng\nHành trang\nRương đồ",
                    "Nhập mã\nquà tặng",
                    "Cửa hàng\nHạn sử dụng",
                    "Tiệm\nHớt tóc",
                    "Danh\nhiệu",
                    "Đổi đệ\ncấp 4"));

            if (soLuong >= 1) {
                menu.add(1, "Giảm giá\n80%");
            }
            if (nro.models.event.EventManager.LUNNAR_NEW_YEAR) {
                menu.add("Mua 10\nMuối Tiêu");
            }

            String[] menus = menu.toArray(new String[0]);

            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Xin chào, ta có một số vật phẩm đặc biệt cậu có muốn xem không?", menus);
        }

    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            Item pGG = InventoryService.gI().findItem(player.inventory.itemsBag, 459);
            int soLuong = 0;
            if (pGG != null) {
                soLuong = pGG.quantity;
            }

            if (this.mapId == 5 || this.mapId == 13 || this.mapId == 20) {
                if (player.idMark.isBaseMenu()) {
                    int buySaltIndex = soLuong >= 1 ? 8 : 7;
                    if (nro.models.event.EventManager.LUNNAR_NEW_YEAR && select == buySaltIndex) {
                        buyTetSaltPepper(player);
                        return;
                    }
                    switch (select) {
                        case 0:
                            ShopService.gI().opendShop(player, "SANTA", false);
                            break;
                        case 1:
                            if (soLuong >= 1) {
                                ShopService.gI().opendShop(player, "SANTA_GIAM_GIA_1", false);
                            } else {
                                ShopService.gI().opendShop(player, "SANTA_MO_RONG_HANH_TRANG", false);
                            }
                            break;
                        case 2:
                            if (soLuong >= 1) {
                                ShopService.gI().opendShop(player, "SANTA_MO_RONG_HANH_TRANG", false);
                            } else {
                                Input.gI().createFormGiftCode(player);
                            }
                            break;
                        case 3:
                            if (soLuong >= 1) {
                                Input.gI().createFormGiftCode(player);
                            } else {
                                ShopService.gI().opendShop(player, "SANTA_HAN_SU_DUNG", false);
                            }
                            break;
                        case 4:
                            if (soLuong >= 1) {
                                ShopService.gI().opendShop(player, "SANTA_HAN_SU_DUNG", false);
                            } else {
                                ShopService.gI().opendShop(player, "SANTA_HEAD", false);
                            }
                            break;
                        case 5:
                            if (soLuong >= 1) {
                                ShopService.gI().opendShop(player, "SANTA_HEAD", false);
                            } else {
                                ShopService.gI().opendShop(player, "SANTA_DANH_HIEU", false);
                            }
                            break;
                        case 6:
                            if (soLuong >= 1) {
                                ShopService.gI().opendShop(player, "SANTA_DANH_HIEU", false);
                            } else {
                                doiDeCap4(player);
                            }
                            break;
                        case 7:
                            if (soLuong >= 1) {
                                doiDeCap4(player);
                            }
                            break;
                    }
                }
            }
        }
    }

    private void buyTetSaltPepper(Player player) {
        final long price = 1_000_000L;
        if (player.inventory.gold < price) {
            Service.gI().sendThongBao(player, "Cần 1 triệu vàng để mua 10 Muối Tiêu.");
            return;
        }
        Item salt = ItemService.gI().createNewItem((short) LunarNewYearEventService.MUOI_TIEU, 10);
        if (!InventoryService.gI().addItemBag(player, salt)) {
            Service.gI().sendThongBao(player, "Hành trang đã đầy.");
            return;
        }
        player.inventory.gold -= price;
        Service.gI().sendMoney(player);
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendThongBao(player, "Bạn đã mua 10 Muối Tiêu với giá 1 triệu vàng.");
    }

    private void doiDeCap4(Player player) {
        if (player.pet == null || player.pet.typePet < 2 || player.pet.typePet > 4) {
            nro.models.services.Service.gI().sendThongBao(player, "Yêu cầu có đệ tử cấp 3 để đổi!");
            return;
        }
        Item bossPiece = InventoryService.gI().findItem(player.inventory.itemsBag, 2002);
        Item mobBadge = InventoryService.gI().findItem(player.inventory.itemsBag, 2003);
        if (bossPiece == null || mobBadge == null || player.inventory.gem < 1000) {
            nro.models.services.Service.gI().sendThongBao(player, "Yêu cầu: 1 Bình kill 2, 1 Đổi đệ tử cấp 4 và 1000 Ngọc!");
            return;
        }
        InventoryService.gI().subQuantityItemsBag(player, bossPiece, 1);
        InventoryService.gI().subQuantityItemsBag(player, mobBadge, 1);
        player.inventory.gem -= 1000;
        nro.models.services.Service.gI().sendMoney(player);
        InventoryService.gI().sendItemBags(player);
        
        nro.models.services.PetService.gI().changePetLevel4(player);
        nro.models.services.Service.gI().sendThongBao(player, "Đổi đệ tử cấp 4 thành công!");
    }
}
