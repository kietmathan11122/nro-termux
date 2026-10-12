package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.consts.ConstPlayer;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services_dungeon.NgocRongNamecService;
import nro.models.map.service.NpcService;
import nro.models.services.TaskService;
import nro.models.services.Service;
import nro.models.services.PlayerService;
import nro.models.services.shenron.SummonDragonNamek;
import nro.models.shop.ShopService;
import nro.models.utils.TimeUtil;
import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 * 
 */

public class Dende extends Npc {

    public Dende(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (player.playerTask.taskMain.id == 34) {
                if (player.playerTask.taskMain.index == 0) {
                    this.createOtherMenu(player, 330, "Lũ Robot Cyclopian đang xâm chiếm Namek, cậu hãy đến ngăn chặn chúng!", "Đồng ý", "Từ chối");
                    return;
                }
                if (player.playerTask.taskMain.index == 5) {
                    this.createOtherMenu(player, 999, "Bạn đã mang Chìa khóa cùm tay tới rồi sao?", "Giao\nChìa khóa", "Từ chối");
                    return;
                }
                if (player.playerTask.taskMain.index == 9) {
                    this.createOtherMenu(player, 349, "Để tìm được ngọc rồng dễ dàng hơn, bạn hãy đến Trái Đất, tìm Bulma ở Làng Aru để mượn Máy Dò Ngọc nhé!", "Đồng ý", "Từ chối");
                    return;
                }
                if (player.playerTask.taskMain.index == 10 || player.playerTask.taskMain.index == 11) {
                    int ballsInBag = 0;
                    for (int i = 353; i <= 359; i++) {
                        nro.models.item.Item item = nro.models.services.InventoryService.gI().findItemBag(player, i);
                        if (item != null) {
                            ballsInBag += item.quantity;
                        }
                    }
                    if (player.idNRNM >= 353 && player.idNRNM <= 359) {
                        ballsInBag += 1;
                    }
                    int currentCount = player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).count;
                    if (currentCount >= 7) {
                        this.createOtherMenu(player, 350, "Cậu đã thu thập đủ 7 viên ngọc rồng rồi! Hãy giao chúng cho ta để gọi Rồng Thần nhé!", "Giao\nngọc rồng", "Từ chối");
                    } else if (ballsInBag > 0) {
                        this.createOtherMenu(player, 350, "Ta thấy cậu đang mang theo ngọc rồng Namek! Cậu có muốn giao cho ta luôn không?\n(Đã giao: " + currentCount + "/7)", "Giao\nngọc rồng", "Từ chối");
                    } else {
                        this.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Cậu mới giao được " + currentCount + "/7 viên ngọc rồng. Hãy tìm tiếp rồi mang về đây cho ta nhé!", "Đóng");
                    }
                    return;
                }
            }
            if (player.playerTask.taskMain.id == 35) {
                String btnText = (player.zone.map.mapId == 119 || player.zone.map.mapId == 191 || player.zone.map.mapId == 206 || player.zone.map.mapId == 207) ? "Về\nnhà" : "Đến\nVùng Đất\nHắc Ám";
                if (player.gender != 1 && player.zone.map.mapId != 206) {
                    this.createOtherMenu(player, 332, "Chào cậu bé, cậu cần giúp gì?", "Nhiệm vụ", "Nâng cấp\nCánh Thiên Thần", btnText, "Từ chối");
                } else {
                    this.createOtherMenu(player, 332, "Anh cần trang bị gì cứ đến chỗ em nhé, hoặc muốn làm nhiệm vụ?", "Nhiệm vụ", "Cửa\nhàng", "Nâng cấp\nCánh Thiên Thần", btnText, "Từ chối");
                }
                return;
            }
            if (player.playerTask.taskMain.id >= 36) {
                String btnNamekMoi = player.zone.map.mapId == 206 ? "Về nhà" : "Đến Namek\nMới";
                String btnHacAm = player.zone.map.mapId == 207 ? "Về nhà" : "Đến\nVùng Đất\nHắc Ám";
                if (player.gender != 1 && player.zone.map.mapId != 206) {
                    this.createOtherMenu(player, 333, "Chào cậu bé, cậu cần giúp gì?", btnNamekMoi, btnHacAm, "Gọi Rồng\nTà Ác", "Nâng cấp\nCánh Thiên Thần", "Từ chối");
                } else {
                    this.createOtherMenu(player, 333, "Anh cần trang bị gì cứ đến chỗ em nhé, hoặc muốn đi đâu?", "Cửa\nhàng", btnNamekMoi, btnHacAm, "Gọi Rồng\nTà Ác", "Nâng cấp\nCánh Thiên Thần", "Từ chối");
                }
                return;
            }
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                if (player.idNRNM != -1) {
                    if (player.zone.map.mapId == 7 || player.zone.map.mapId == 206 || (player.playerTask != null && player.playerTask.taskMain != null && player.playerTask.taskMain.id >= 43)) {
                        if (player.playerTask != null && player.playerTask.taskMain != null && player.playerTask.taskMain.id >= 43) {
                            this.createOtherMenu(player, 1, "Ồ, ngọc rồng namếc, bạn thật là may mắn\nnếu tìm đủ 7 viên sẽ được Rồng Thiêng Namếc ban cho điều ước", "Hướng\ndẫn\nGọi Rồng", "Gọi rồng", "Giao\nNgọc rồng", "Từ chối");
                        } else {
                            this.createOtherMenu(player, 1, "Ồ, ngọc rồng namếc, bạn thật là may mắn\nnếu tìm đủ 7 viên sẽ được Rồng Thiêng Namếc ban cho điều ước", "Hướng\ndẫn\nGọi Rồng", "Gọi rồng", "Từ chối");
                        }
                    }
                } else {
                    if (player.zone.map.mapId == 206) {
                        this.createOtherMenu(player, 331, "Anh cần trang bị gì cứ đến chỗ em nhé, hoặc muốn về nhà?", "Cửa\nhàng", "Về nhà", "Nâng cấp\nCánh Thiên Thần", "Từ chối");
                    } else if (player.playerTask.taskMain.id >= 33) {
                        if (player.gender != 1) {
                            this.createOtherMenu(player, 331, "Cậu muốn đến Hành Tinh Namek Mới không?", "Đến đó", "Nâng cấp\nCánh Thiên Thần", "Từ chối");
                        } else {
                            this.createOtherMenu(player, 331, "Anh cần trang bị gì cứ đến chỗ em nhé, hoặc cậu muốn đến Hành Tinh Namek Mới?", "Cửa\nhàng", "Đến Namek\nMới", "Nâng cấp\nCánh Thiên Thần");
                        }
                    } else if (player.gender != 1) {
                        NpcService.gI().createTutorial(player, tempId, this.avartar, "Xin lỗi anh, em chỉ bán đồ cho dân tộc Namếc");
                    } else if (!player.inventory.itemsDaBan.isEmpty()) {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                "Anh cần trang bị gì cứ đến chỗ em nhé", "Cửa\nhàng"
                            );
                    } else {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                                "Anh cần trang bị gì cứ đến chỗ em nhé", "Cửa\nhàng");
                    }
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.getIndexMenu() == nro.models.consts.ConstNpc.MENU_START_COMBINE) {
                if (select == 0) {
                    nro.models.combine.CombineService.gI().startCombine(player);
                }
                return;
            }
            if (player.idMark.getIndexMenu() == 330) {
                if (select == 0) {
                    TaskService.gI().doneTask(player, nro.models.consts.ConstTask.TASK_34_0);
                    nro.models.map.service.ChangeMapService.gI().changeMapInYard(player, 206, -1, -1);
                }
                return;
            }
            if (player.idMark.getIndexMenu() == 333) {
                if (player.gender != 1 && player.zone.map.mapId != 206) {
                    if (select == 0) { // btnNamekMoi
                        if (player.zone.map.mapId == 206) {
                            nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, -1, 250);
                        } else {
                            nro.models.map.service.ChangeMapService.gI().changeMapInYard(player, 206, -1, -1);
                        }
                    } else if (select == 1) { // btnHacAm
                        if (player.zone.map.mapId == 207) {
                            nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, -1, 250);
                        } else {
                            nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, 207, -1, 250);
                        }
                    } else if (select == 2) { // Gọi Rồng Tà Ác
                        showCallBossMenu(player);
                    } else if (select == 3) { // Nâng cấp Cánh
                        player.idMark.setNpcChose(this);
                        nro.models.combine.CombineService.gI().openTabCombine(player, nro.models.combine.CombineService.NANG_CAP_CANH);
                    }
                } else {
                    if (select == 0) { // Cửa hàng
                        ShopService.gI().opendShop(player, "DENDE", true);
                    } else if (select == 1) { // btnNamekMoi
                        if (player.zone.map.mapId == 206) {
                            nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, -1, 250);
                        } else {
                            nro.models.map.service.ChangeMapService.gI().changeMapInYard(player, 206, -1, -1);
                        }
                    } else if (select == 2) { // btnHacAm
                        if (player.zone.map.mapId == 207) {
                            nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, -1, 250);
                        } else {
                            nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, 207, -1, 250);
                        }
                    } else if (select == 3) { // Gọi Rồng Tà Ác
                        showCallBossMenu(player);
                    } else if (select == 4) { // Nâng cấp Cánh
                        player.idMark.setNpcChose(this);
                        nro.models.combine.CombineService.gI().openTabCombine(player, nro.models.combine.CombineService.NANG_CAP_CANH);
                    }
                }
                return;
            }
            if (player.idMark.getIndexMenu() == 335) {
                handleCallBossMenu(player, select);
                return;
            }
            if (player.idMark.getIndexMenu() == 331) {
                if (player.zone.map.mapId == 206 || player.gender == 1) {
                    if (select == 0) {
                        ShopService.gI().opendShop(player, "DENDE", true);
                    } else if (select == 1) {
                        if (player.zone.map.mapId == 206) {
                            nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, -1, 250);
                        } else {
                            nro.models.map.service.ChangeMapService.gI().changeMapInYard(player, 206, -1, -1);
                        }
                    } else if (select == 2) {
                        player.idMark.setNpcChose(this);
                        nro.models.combine.CombineService.gI().openTabCombine(player, nro.models.combine.CombineService.NANG_CAP_CANH);
                    }
                } else {
                    if (select == 0) {
                        if (player.zone.map.mapId == 206) {
                            nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, -1, 250);
                        } else {
                            nro.models.map.service.ChangeMapService.gI().changeMapInYard(player, 206, -1, -1);
                        }
                    } else if (select == 1) {
                        player.idMark.setNpcChose(this);
                        nro.models.combine.CombineService.gI().openTabCombine(player, nro.models.combine.CombineService.NANG_CAP_CANH);
                    }
                }
                return;
            }

            if (player.idMark.getIndexMenu() == 332) {
                if (player.gender != 1 && player.zone.map.mapId != 206) {
                    if (select == 0) {
                        showTask35Menu(player);
                    } else if (select == 1) {
                        player.idMark.setNpcChose(this);
                        nro.models.combine.CombineService.gI().openTabCombine(player, nro.models.combine.CombineService.NANG_CAP_CANH);
                    } else if (select == 2) {
                        if (player.zone.map.mapId == 119 || player.zone.map.mapId == 191 || player.zone.map.mapId == 206 || player.zone.map.mapId == 207) {
                            nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, -1, 250);
                        } else {
                            nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, 207, -1, 250);
                        }
                    }
                } else {
                    if (select == 0) {
                        showTask35Menu(player);
                    } else if (select == 1) {
                        ShopService.gI().opendShop(player, "DENDE", true);
                    } else if (select == 2) {
                        player.idMark.setNpcChose(this);
                        nro.models.combine.CombineService.gI().openTabCombine(player, nro.models.combine.CombineService.NANG_CAP_CANH);
                    } else if (select == 3) {
                        if (player.zone.map.mapId == 119 || player.zone.map.mapId == 191 || player.zone.map.mapId == 206 || player.zone.map.mapId == 207) {
                            nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, -1, 250);
                        } else {
                            nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, 207, -1, 250);
                        }
                    }
                }
                return;
            }

            if (player.idMark.getIndexMenu() == 999) {
                if (select == 0) {
                    nro.models.item.Item key = nro.models.services.InventoryService.gI().findItemBag(player, 2015);
                    if (key != null) {
                        nro.models.services.InventoryService.gI().subQuantityItemsBag(player, key, 1);
                        nro.models.services.InventoryService.gI().sendItemBags(player);
                    }
                    TaskService.gI().doneTask(player, nro.models.consts.ConstTask.TASK_34_5);
                    try {
                        nro.models.player_system.Template.NpcTemplate quocVuong = nro.models.server.Manager.NPC_TEMPLATES.get(42);
                        NpcService.gI().createTutorial(player, 42, quocVuong.avatar, "Cảm ơn cậu bé, nhờ có cậu mà ta đã được giải thoát khỏi tay bọn người máy! Cậu thật sự là vị cứu tinh của hành tinh Namek này.");
                    } catch (Exception e) {
                        NpcService.gI().createTutorial(player, 42, 42, "Cảm ơn cậu bé, nhờ có cậu mà ta đã được giải thoát khỏi tay bọn người máy! Cậu thật sự là vị cứu tinh của hành tinh Namek này.");
                    }
                }
                return;
            }
            if (player.idMark.getIndexMenu() == 349) {
                if (select == 0) {
                    TaskService.gI().doneTask(player, nro.models.consts.ConstTask.TASK_34_9);
                    Service.gI().sendThongBao(player, "Hãy đến Làng Aru gặp Bulma để lấy Máy dò ngọc.");
                }
                return;
            }
            if (player.idMark.getIndexMenu() == 350) {
                if (select == 0) {
                    if (player.playerTask.taskMain.index == 10 || player.playerTask.taskMain.index == 11) {
                        int currentCount = player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).count;
                        int ballsInBag = 0;
                        for (int i = 353; i <= 359; i++) {
                            nro.models.item.Item ball = nro.models.services.InventoryService.gI().findItemBag(player, i);
                            if (ball != null) {
                                int amount = ball.quantity;
                                if (currentCount + amount > 7) amount = 7 - currentCount;
                                nro.models.services.InventoryService.gI().subQuantityItemsBag(player, ball, amount);
                                currentCount += amount;
                                ballsInBag += amount;
                                if (currentCount >= 7) break;
                            }
                        }
                        
                        if (currentCount < 7 && player.idNRNM >= 353 && player.idNRNM <= 359) {
                            int nrId = player.idNRNM;
                            player.idNRNM = -1;
                            nro.models.services_dungeon.NgocRongNamecService.gI().pNrNamec[nrId - 353] = "";
                            nro.models.services_dungeon.NgocRongNamecService.gI().idpNrNamec[nrId - 353] = -1;
                            nro.models.services.PlayerService.gI().changeAndSendTypePK(player, nro.models.consts.ConstPlayer.NON_PK);
                            if (player.pet != null) {
                                nro.models.services.PlayerService.gI().changeAndSendTypePK(player.pet, nro.models.consts.ConstPlayer.NON_PK);
                            }
                            nro.models.services.Service.gI().sendFlagBag(player);
                            ballsInBag += 1;
                            currentCount += 1;
                        }
                        
                        if (ballsInBag > 0) {
                            player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).count = (short) currentCount;
                            TaskService.gI().sendUpdateCountSubTask(player);
                            nro.models.services.InventoryService.gI().sendItemBags(player);
                        }
                        
                        if (currentCount >= 7) {
                            nro.models.services.shenron.SummonDragonNamek.gI().summonNamecTask(player);
                        } else if (ballsInBag > 0) {
                            Service.gI().sendThongBao(player, "Tuyệt quá! Cậu vừa giao " + ballsInBag + " viên ngọc rồng! Cố gắng tìm thêm nhé (Đã giao: " + currentCount + "/7).");
                        } else {
                            Service.gI().sendThongBao(player, "Bạn không có viên ngọc rồng Namek nào!");
                        }
                    }
                }
                return;
            }
            if (player.idMark.getIndexMenu() == 357) {
                if (select == 0) {
                    if (player.zone.map.mapId == 119 || player.zone.map.mapId == 191 || player.zone.map.mapId == 206 || player.zone.map.mapId == 207) {
                        nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, -1, 250);
                    } else {
                        nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, 207, -1, 250);
                    }
                } else if (select == 1 && player.zone.map.mapId != 206) {
                    nro.models.map.service.ChangeMapService.gI().changeMapInYard(player, 206, -1, -1);
                }
                return;
            }
            if (player.idMark.getIndexMenu() == 351) {
                if (select == 0) {
                    TaskService.gI().doneTask(player, nro.models.consts.ConstTask.TASK_35_0);
                    nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, 207, -1, 250);
                }
                return;
            }
            if (player.idMark.getIndexMenu() == 352) {
                if (select == 0) {
                    TaskService.gI().doneTask(player, nro.models.consts.ConstTask.TASK_35_1);
                }
                return;
            }
            if (player.idMark.getIndexMenu() == 353) {
                if (select == 0) {
                    nro.models.item.Item manhNgoc = nro.models.services.InventoryService.gI().findItemBag(player, 2016);
                    if (manhNgoc != null && manhNgoc.quantity >= 99) {
                        nro.models.services.InventoryService.gI().subQuantityItemsBag(player, manhNgoc, 99);
                        nro.models.services.InventoryService.gI().sendItemBags(player);
                        if (player.playerTask != null && player.playerTask.taskMain != null && player.playerTask.taskMain.id == 35 && player.playerTask.taskMain.index == 2) {
                            player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).count = 98;
                        }
                        TaskService.gI().doneTask(player, nro.models.consts.ConstTask.TASK_35_2);
                        
                        int randomX = player.location.x + nro.models.utils.Util.nextInt(-50, 50);
                        if (randomX < 100) randomX = 100;
                        if (randomX > player.zone.map.mapWidth - 100) randomX = player.zone.map.mapWidth - 100;
                        int y = player.zone.map.yPhysicInTop(randomX, player.location.y);
                        nro.models.map.ItemMap biaDa = new nro.models.map.ItemMap(player.zone, 2017, 1, randomX, y, player.id);
                        nro.models.services.Service.gI().dropItemMap(player.zone, biaDa);
                        nro.models.services.Service.gI().sendThongBao(player, "Bia Đá Cổ Tự đã rơi xuống xung quanh, hãy tìm và nhặt nó!");
                    }
                } else if (select == 1) {
                    if (player.zone.map.mapId == 119 || player.zone.map.mapId == 191 || player.zone.map.mapId == 206 || player.zone.map.mapId == 207) {
                        nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, -1, 250);
                    } else {
                        nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, 207, -1, 250);
                    }
                } else if (select == 2 && player.zone.map.mapId != 206) {
                    nro.models.map.service.ChangeMapService.gI().changeMapInYard(player, 206, -1, -1);
                }
                return;
            }
            if (player.idMark.getIndexMenu() == 354) {
                if (select == 0) {
                    int countTinhKhoi = 0;
                    for (nro.models.item.Item it : player.inventory.itemsBag) {
                        if (it.isNotNullItem() && it.template.id == 2018) {
                            countTinhKhoi += it.quantity;
                        }
                    }
                    if (countTinhKhoi >= 1) {
                        int amountToRemove = 1;
                        for (int i = player.inventory.itemsBag.size() - 1; i >= 0; i--) {
                            nro.models.item.Item it = player.inventory.itemsBag.get(i);
                            if (it.isNotNullItem() && it.template.id == 2018) {
                                if (it.quantity >= amountToRemove) {
                                    nro.models.services.InventoryService.gI().subQuantityItemsBag(player, it, amountToRemove);
                                    amountToRemove = 0;
                                    break;
                                } else {
                                    amountToRemove -= it.quantity;
                                    nro.models.services.InventoryService.gI().subQuantityItemsBag(player, it, it.quantity);
                                }
                            }
                        }
                        nro.models.services.InventoryService.gI().sendItemBags(player);
                        if (player.playerTask != null && player.playerTask.taskMain != null && player.playerTask.taskMain.id == 35 && player.playerTask.taskMain.index == 3) {
                            player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).count = 1;
                            nro.models.services.TaskService.gI().doneTask(player, nro.models.consts.ConstTask.TASK_35_3);
                        }
                        
                        // Triệu hồi Rồng Tà Ác
                        if (player.zone.map.mapId == 119 || player.zone.map.mapId == 191 || player.zone.map.mapId == 206 || player.zone.map.mapId == 207) {
                            try {
                                new nro.models.boss.trai_dat.RongTaAc(player);
                            } catch (Exception e) {}
                            nro.models.services.Service.gI().sendThongBao(player, "Rồng Tà Ác đã xuất hiện!");
                        } else {
                            nro.models.services.Service.gI().sendThongBao(player, "Rồng Tà Ác đã xuất hiện tại Vùng Đất Hắc Ám!");
                        }
                    }
                } else if (select == 1) {
                    nro.models.item.Item manhNgoc = nro.models.services.InventoryService.gI().findItemBag(player, 2016);
                    if (manhNgoc != null && manhNgoc.quantity >= 99) {
                        nro.models.services.InventoryService.gI().subQuantityItemsBag(player, manhNgoc, 99);
                        nro.models.services.InventoryService.gI().sendItemBags(player);
                        int randomX = player.location.x + nro.models.utils.Util.nextInt(-50, 50);
                        if (randomX < 100) randomX = 100;
                        if (randomX > player.zone.map.mapWidth - 100) randomX = player.zone.map.mapWidth - 100;
                        int y = player.zone.map.yPhysicInTop(randomX, player.location.y);
                        nro.models.map.ItemMap biaDa = new nro.models.map.ItemMap(player.zone, 2017, 1, randomX, y, player.id);
                        nro.models.services.Service.gI().dropItemMap(player.zone, biaDa);
                        nro.models.services.Service.gI().sendThongBao(player, "Bia Đá Cổ Tự đã rơi xuống xung quanh, hãy tìm và nhặt nó!");
                    } else {
                        nro.models.services.Service.gI().sendThongBao(player, "Bạn cần thu thập đủ 99 Mảnh Ngọc Rồng Nứt!");
                    }
                } else if (select == 2) {
                    if (player.zone.map.mapId == 119 || player.zone.map.mapId == 191 || player.zone.map.mapId == 206 || player.zone.map.mapId == 207) {
                        nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, -1, 250);
                    } else {
                        nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, 207, -1, 250);
                    }
                } else if (select == 3 && player.zone.map.mapId != 206) {
                    nro.models.map.service.ChangeMapService.gI().changeMapInYard(player, 206, -1, -1);
                }
                return;
            }
            if (player.idMark.getIndexMenu() == 358) {
                if (select == 0) {
                    nro.models.item.Item manhNgoc = nro.models.services.InventoryService.gI().findItemBag(player, 2016);
                    if (manhNgoc != null && manhNgoc.quantity >= 99) {
                        nro.models.services.InventoryService.gI().subQuantityItemsBag(player, manhNgoc, 99);
                        nro.models.services.InventoryService.gI().sendItemBags(player);
                        int randomX = player.location.x + nro.models.utils.Util.nextInt(-50, 50);
                        if (randomX < 100) randomX = 100;
                        if (randomX > player.zone.map.mapWidth - 100) randomX = player.zone.map.mapWidth - 100;
                        int y = player.zone.map.yPhysicInTop(randomX, player.location.y);
                        nro.models.map.ItemMap biaDa = new nro.models.map.ItemMap(player.zone, 2017, 1, randomX, y, player.id);
                        nro.models.services.Service.gI().dropItemMap(player.zone, biaDa);
                        nro.models.services.Service.gI().sendThongBao(player, "Bia Đá Cổ Tự đã rơi xuống xung quanh, hãy tìm và nhặt nó!");
                    } else {
                        nro.models.services.Service.gI().sendThongBao(player, "Bạn cần thu thập đủ 99 Mảnh Ngọc Rồng Nứt!");
                    }
                } else if (select == 1) {
                    if (player.zone.map.mapId == 119 || player.zone.map.mapId == 191 || player.zone.map.mapId == 206 || player.zone.map.mapId == 207) {
                        nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, -1, 250);
                    } else {
                        nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, 207, -1, 250);
                    }
                } else if (select == 2 && player.zone.map.mapId != 206) {
                    nro.models.map.service.ChangeMapService.gI().changeMapInYard(player, 206, -1, -1);
                }
                return;
            }
            if (player.idMark.getIndexMenu() == 355) {
                if (select == 0) {
                    int countLoiNgoc = 0;
                    for (nro.models.item.Item it : player.inventory.itemsBag) {
                        if (it.isNotNullItem() && it.template.id == 2019) {
                            countLoiNgoc += it.quantity;
                        }
                    }
                    if (countLoiNgoc >= 3) {
                        int amountToRemove = 3;
                        for (int i = player.inventory.itemsBag.size() - 1; i >= 0; i--) {
                            nro.models.item.Item it = player.inventory.itemsBag.get(i);
                            if (it.isNotNullItem() && it.template.id == 2019) {
                                if (it.quantity >= amountToRemove) {
                                    nro.models.services.InventoryService.gI().subQuantityItemsBag(player, it, amountToRemove);
                                    amountToRemove = 0;
                                    break;
                                } else {
                                    amountToRemove -= it.quantity;
                                    nro.models.services.InventoryService.gI().subQuantityItemsBag(player, it, it.quantity);
                                }
                            }
                        }
                        nro.models.services.InventoryService.gI().sendItemBags(player);
                        if (player.playerTask != null && player.playerTask.taskMain != null && player.playerTask.taskMain.id == 35 && player.playerTask.taskMain.index == 6) {
                            nro.models.services.TaskService.gI().doneTask(player, nro.models.consts.ConstTask.TASK_35_6);
                        }
                        NpcService.gI().createTutorial(player, tempId, this.avartar, "Tuyệt vời! Luồng khí độc đã tạm thời tan bớt. Tuy nhiên, lõi năng lượng hắc ám lớn nhất đã bay về phía map tiếp theo... Đây là phần thưởng xứng đáng cho lòng dũng cảm của anh!");
                    }
                } else if (select == 1) {
                    nro.models.item.Item manhNgoc = nro.models.services.InventoryService.gI().findItemBag(player, 2016);
                    if (manhNgoc != null && manhNgoc.quantity >= 99) {
                        nro.models.services.InventoryService.gI().subQuantityItemsBag(player, manhNgoc, 99);
                        nro.models.services.InventoryService.gI().sendItemBags(player);
                        int randomX = player.location.x + nro.models.utils.Util.nextInt(-50, 50);
                        if (randomX < 100) randomX = 100;
                        if (randomX > player.zone.map.mapWidth - 100) randomX = player.zone.map.mapWidth - 100;
                        int y = player.zone.map.yPhysicInTop(randomX, player.location.y);
                        nro.models.map.ItemMap biaDa = new nro.models.map.ItemMap(player.zone, 2017, 1, randomX, y, player.id);
                        nro.models.services.Service.gI().dropItemMap(player.zone, biaDa);
                        nro.models.services.Service.gI().sendThongBao(player, "Bia Đá Cổ Tự đã rơi xuống xung quanh, hãy tìm và nhặt nó!");
                    } else {
                        nro.models.services.Service.gI().sendThongBao(player, "Bạn cần thu thập đủ 99 Mảnh Ngọc Rồng Nứt!");
                    }
                } else if (select == 2) {
                    if (player.zone.map.mapId == 119 || player.zone.map.mapId == 191 || player.zone.map.mapId == 206 || player.zone.map.mapId == 207) {
                        nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, -1, 250);
                    } else {
                        nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, 207, -1, 250);
                    }
                } else if (select == 3 && player.zone.map.mapId != 206) {
                    nro.models.map.service.ChangeMapService.gI().changeMapInYard(player, 206, -1, -1);
                }
                return;
            }
            if (player.idMark.getIndexMenu() == 356) {
                if (select == 0) {
                    if (player.zone.map.mapId == 119 || player.zone.map.mapId == 191 || player.zone.map.mapId == 206) {
                        try {
                            new nro.models.boss.trai_dat.RongTaAc(player);
                        } catch (Exception e) {}
                        Service.gI().sendThongBao(player, "Rồng Tà Ác đã xuất hiện!");
                    } else {
                        Service.gI().sendThongBao(player, "Rồng Tà Ác đã xuất hiện tại Vùng Đất Hắc Ám!");
                    }
                } else if (select == 1) {
                    if (player.zone.map.mapId == 119 || player.zone.map.mapId == 191 || player.zone.map.mapId == 206 || player.zone.map.mapId == 207) {
                        try {
                            new nro.models.boss.trai_dat.RongTaAc(player);
                        } catch (Exception e) {}
                        Service.gI().sendThongBao(player, "Rồng Tà Ác đã xuất hiện!");
                    } else {
                        nro.models.map.service.ChangeMapService.gI().changeMapBySpaceShip(player, 207, -1, 250);
                    }
                }
                return;
            }
            if (player.idMark.isBaseMenu()) {
                switch (select) {
                    case 0 -> {
                        //Shop
                        if (player.gender == ConstPlayer.NAMEC) {
                            ShopService.gI().opendShop(player, "DENDE", true);
                        } else {
                            this.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Xin lỗi anh, em chỉ bán đồ cho dân tộc Namếc", "Đóng");
                        }
                    }
                }
            } else if (player.idMark.getIndexMenu() == 1) {
                switch (select) {
                    case 0 ->
                        NpcService.gI().createTutorial(player, tempId, this.avartar, ConstNpc.HUONG_DAN_NRNM);
                    case 1 -> {
                        if (player.zone.map.mapId == 7 && player.idNRNM != -1) {
                            if (player.idNRNM != 353) {
                                NpcService.gI().createTutorial(player, tempId, this.avartar, "Anh phải có viên Ngọc Rồng Namek 1 sao");
                                return;
                            }
                            if (TimeUtil.getCurrHour() > 22 || TimeUtil.getCurrHour() < 8) {
                                NpcService.gI().createTutorial(player, tempId, this.avartar, "Xin lỗi mấy anh, em đang bận buôn bán nên chỉ rảnh gọi Rồng vào khoảng 8h đến 22h");
                                return;
                            }
                            if (!Util.canDoWithTime(player.lastTimePickNRNM, 600000)) {
                                NpcService.gI().createTutorial(player, tempId, this.avartar, "Ngọc bẩn quá, xin chờ em " + TimeUtil.getTimeLeft(player.lastTimePickNRNM, 600) + " nữa để lau bóng ngọc, gọi Rồng mới hiện linh");
                                return;
                            }
                            if (!NgocRongNamecService.gI().canCallDragonNamec(player)) {
                                NpcService.gI().createTutorial(player, tempId, this.avartar, "Hãy gom đủ 7 viên Ngọc Rồng tới đây");
                                return;
                            }
                            NgocRongNamecService.gI().tOpenNrNamec = System.currentTimeMillis() + 86400000;
                            NgocRongNamecService.gI().firstNrNamec = true;
                            NgocRongNamecService.gI().timeNrNamec = 0;
                            NgocRongNamecService.gI().doneDragonNamec();
                            NgocRongNamecService.gI().initNgocRongNamec((byte) 1);
                            NgocRongNamecService.gI().reInitNrNamec((long) 86399000);
                            SummonDragonNamek.gI().summonNamec(player);
                        }
                    }
                    case 2 -> {
                        if (player.playerTask != null && player.playerTask.taskMain != null && player.playerTask.taskMain.id >= 43) {
                            if (player.clan == null) {
                                NpcService.gI().createTutorial(player, tempId, this.avartar, "Anh phải có Bang Hội thì em mới giữ ngọc giúp được nhé!");
                                return;
                            }
                            if (player.idNRNM != -1) {
                                if (!Util.canDoWithTime(player.lastTimeBankNamekDB, 300000)) {
                                    NpcService.gI().createTutorial(player, tempId, this.avartar, "Xin chờ " + TimeUtil.getTimeLeft(player.lastTimeBankNamekDB, 300) + " nữa em mới có thể cất thêm ngọc!");
                                    return;
                                }
                                int index = player.idNRNM - 353;
                                NgocRongNamecService.gI().mapNrNamec[index] = 7;
                                NgocRongNamecService.gI().zoneNrNamec[index] = 0;
                                NgocRongNamecService.gI().pNrNamec[index] = "Clan " + player.clan.name;
                                NgocRongNamecService.gI().idpNrNamec[index] = (int) player.clan.id;
                                player.idNRNM = -1;
                                player.lastTimeBankNamekDB = System.currentTimeMillis();
                                Service.gI().sendThongBao(player, "Đã giao Ngọc Rồng Namếc thành công!");
                                Service.gI().sendFlagBag(player);
                                PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.NON_PK);
                            }
                        }
                    }
                }
            }
        }
    }

    private void showTask35Menu(Player player) {
        if (player.playerTask.taskMain.index == 0) {
            this.createOtherMenu(player, 351, "Năng lượng tà ác từ các viên ngọc rồng bị nứt đang mở ra một vết rách không gian tại Vùng Đất Hắc Ám! anh hãy mau đến đó giúp đỡ dân làng!", "Đi\nngay");
            return;
        }
        String btnText = (player.zone.map.mapId == 119 || player.zone.map.mapId == 191 || player.zone.map.mapId == 206 || player.zone.map.mapId == 207) ? "Về\nnhà" : "Đến\nVùng Đất\nHắc Ám";
        if (player.playerTask.taskMain.index == 1) {
            if (player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).count < 1000) {
                if (player.zone.map.mapId != 206) {
                    this.createOtherMenu(player, 357, "Cố lên chiến binh! Lũ Quái Vật Khói Đen vẫn đang tiếp tục sinh sôi từ năng lượng tiêu cực, hãy tiêu diệt đủ 1000 con quái vật để thanh tẩy bản đồ này!", btnText, "Đến Namek\nMới", "Đóng");
                } else {
                    this.createOtherMenu(player, 357, "Cố lên chiến binh! Lũ Quái Vật Khói Đen vẫn đang tiếp tục sinh sôi từ năng lượng tiêu cực, hãy tiêu diệt đủ 1000 con quái vật để thanh tẩy bản đồ này!", btnText, "Đóng");
                }
            } else {
                this.createOtherMenu(player, 352, "Tuyệt vời, anh đã tiêu diệt đủ 1000 con quái vật. Hãy tiếp tục nhiệm vụ nào!", "Báo cáo");
            }
            return;
        }
        if (player.playerTask.taskMain.index == 2) {
            nro.models.item.Item manhNgoc = nro.models.services.InventoryService.gI().findItemBag(player, 2016);
            if (manhNgoc != null && manhNgoc.quantity >= 99) {
                if (player.zone.map.mapId != 206) {
                    this.createOtherMenu(player, 353, "Anh đã mang đủ 99 Mảnh Ngọc Rồng Nứt rồi sao? Thật tuyệt vời!", "Giao Mảnh\nNgọc Rồng", btnText, "Đến Namek\nMới", "Đóng");
                } else {
                    this.createOtherMenu(player, 353, "Anh đã mang đủ 99 Mảnh Ngọc Rồng Nứt rồi sao? Thật tuyệt vời!", "Giao Mảnh\nNgọc Rồng", btnText, "Đóng");
                }
            } else {
                int amount = (manhNgoc != null) ? manhNgoc.quantity : 0;
                if (player.zone.map.mapId != 206) {
                    this.createOtherMenu(player, 357, "Anh hãy thu thập đủ 99 Mảnh Ngọc Rồng Nứt ở Vùng Đất Hắc Ám rồi mang về đây cho tôi nhé! (Đang có: " + amount + "/99)", btnText, "Đến Namek\nMới", "Đóng");
                } else {
                    this.createOtherMenu(player, 357, "Anh hãy thu thập đủ 99 Mảnh Ngọc Rồng Nứt ở Vùng Đất Hắc Ám rồi mang về đây cho tôi nhé! (Đang có: " + amount + "/99)", btnText, "Đóng");
                }
            }
            return;
        }
        if (player.playerTask.taskMain.index == 6) {
              int countLoiNgoc = 0;
              for (nro.models.item.Item it : player.inventory.itemsBag) {
                  if (it.isNotNullItem() && it.template.id == 2019) {
                      countLoiNgoc += it.quantity;
                  }
              }
              if (countLoiNgoc >= 3) {
                  if (player.zone.map.mapId != 206) {
                      this.createOtherMenu(player, 355, "Thật kinh ngạc! Anh đã tìm đủ 3 Lõi Ngọc Rồng Đen rồi sao?\nHãy đưa cho tôi để hoàn thành nhiệm vụ nhé!", "Giao 3\nLõi Ngọc", "Giao 99\nMảnh Ngọc", btnText, "Đến Namek\nMới", "Đóng");
                  } else {
                      this.createOtherMenu(player, 355, "Thật kinh ngạc! Anh đã tìm đủ 3 Lõi Ngọc Rồng Đen rồi sao?\nHãy đưa cho tôi để hoàn thành nhiệm vụ nhé!", "Giao 3\nLõi Ngọc", "Giao 99\nMảnh Ngọc", btnText, "Đóng");
                  }
                  return;
              }
        }
        if (player.playerTask.taskMain.index == 3 || player.playerTask.taskMain.index == 4 || player.playerTask.taskMain.index == 5 || player.playerTask.taskMain.index == 6) {
              int countTinhKhoi = 0;
              for (nro.models.item.Item it : player.inventory.itemsBag) {
                  if (it.isNotNullItem() && it.template.id == 2018) {
                      countTinhKhoi += it.quantity;
                  }
              }
              String msg = "";
              if (player.playerTask.taskMain.index == 3) {
                  msg = "Anh hãy dùng Bia Đá Cổ Tự để triệu hồi Bóng Ma Tà Ác.\nSau khi đánh bại nó, hãy mang 1 Tinh Khói Đậm Đặc về đây cho tôi.";
              } else if (player.playerTask.taskMain.index == 4) {
                  msg = "Anh hãy dùng Bia Đá Cổ Tự để triệu hồi Bóng Ma Tà Ác và thu thập đủ 5 Tinh Khói Đậm Đặc mang về cho tôi.";
              } else if (player.playerTask.taskMain.index == 5) {
                  msg = "Rồng Tà Ác đã xuất hiện! Hãy mau đi tiêu diệt nó!\n(Nếu Rồng biến mất, anh có thể đưa thêm 1 Tinh Khói để tôi gọi lại)";
              } else if (player.playerTask.taskMain.index == 6) {
                  int countLoiNgoc = 0;
                  for (nro.models.item.Item it : player.inventory.itemsBag) {
                      if (it.isNotNullItem() && it.template.id == 2019) {
                          countLoiNgoc += it.quantity;
                      }
                  }
                  msg = "Anh cần tìm 3 Lõi Ngọc Rồng Đen (tỷ lệ rơi từ Rồng Tà Ác). Đang có: " + countLoiNgoc + "/3.\nAnh có thể đưa 1 Tinh Khói để tôi gọi Rồng Tà Ác ra.";
              }

              if (countTinhKhoi >= 1) {
                  if (player.zone.map.mapId != 206) {
                      this.createOtherMenu(player, 354, msg + "\n\nAnh đang có " + countTinhKhoi + " Tinh Khói. Muốn gọi Rồng Tà Ác không?", "Giao 1\nTinh Khói", "Giao 99\nMảnh Ngọc", btnText, "Đến Namek\nMới", "Đóng");
                  } else {
                      this.createOtherMenu(player, 354, msg + "\n\nAnh đang có " + countTinhKhoi + " Tinh Khói. Muốn gọi Rồng Tà Ác không?", "Giao 1\nTinh Khói", "Giao 99\nMảnh Ngọc", btnText, "Đóng");
                  }
              } else {
                  if (player.zone.map.mapId != 206) {
                      this.createOtherMenu(player, 358, msg + "\n(Anh chưa có Tinh Khói nào)", "Giao 99\nMảnh Ngọc", btnText, "Đến Namek\nMới", "Đóng");
                  } else {
                      this.createOtherMenu(player, 358, msg + "\n(Anh chưa có Tinh Khói nào)", "Giao 99\nMảnh Ngọc", btnText, "Đóng");
                  }
              }
              return;
        }
    }

    private void showCallBossMenu(Player player) {
        int countTinhKhoi = 0;
        for (nro.models.item.Item it : player.inventory.itemsBag) {
            if (it.isNotNullItem() && it.template.id == 2018) {
                countTinhKhoi += it.quantity;
            }
        }
        int countManh = 0;
        nro.models.item.Item manhNgoc = nro.models.services.InventoryService.gI().findItemBag(player, 2016);
        if (manhNgoc != null) {
            countManh = manhNgoc.quantity;
        }
        
        String msg = "Cậu muốn triệu hồi Rồng Tà Ác hay chế tạo Bia Đá Cổ Tự?\n"
                + "• Tinh Khói Đậm Đặc đang có: " + countTinhKhoi + "\n"
                + "• Mảnh Ngọc Rồng Nứt đang có: " + countManh + "/99";
                
        this.createOtherMenu(player, 335, msg, "Gọi Rồng\nTà Ác", "Đổi Bia Đá\nCổ Tự", "Đóng");
    }

    private void handleCallBossMenu(Player player, int select) {
        if (select == 0) { // Triệu hồi Rồng Tà Ác
            int countTinhKhoi = 0;
            for (nro.models.item.Item it : player.inventory.itemsBag) {
                if (it.isNotNullItem() && it.template.id == 2018) {
                    countTinhKhoi += it.quantity;
                }
            }
            if (countTinhKhoi >= 1) {
                int amountToRemove = 1;
                for (int i = player.inventory.itemsBag.size() - 1; i >= 0; i--) {
                    nro.models.item.Item it = player.inventory.itemsBag.get(i);
                    if (it.isNotNullItem() && it.template.id == 2018) {
                        if (it.quantity >= amountToRemove) {
                            nro.models.services.InventoryService.gI().subQuantityItemsBag(player, it, amountToRemove);
                            amountToRemove = 0;
                            break;
                        } else {
                            amountToRemove -= it.quantity;
                            nro.models.services.InventoryService.gI().subQuantityItemsBag(player, it, it.quantity);
                        }
                    }
                }
                nro.models.services.InventoryService.gI().sendItemBags(player);
                if (player.zone.map.mapId == 119 || player.zone.map.mapId == 191 || player.zone.map.mapId == 206 || player.zone.map.mapId == 207) {
                    try {
                        new nro.models.boss.trai_dat.RongTaAc(player);
                    } catch (Exception e) {}
                    nro.models.services.Service.gI().sendThongBao(player, "Rồng Tà Ác đã xuất hiện!");
                } else {
                    nro.models.services.Service.gI().sendThongBao(player, "Rồng Tà Ác đã xuất hiện tại Vùng Đất Hắc Ám!");
                }
            } else {
                nro.models.services.Service.gI().sendThongBao(player, "Bạn không có Tinh Khói Đậm Đặc!");
            }
        } else if (select == 1) { // Đổi Bia Đá Cổ Tự
            nro.models.item.Item manhNgoc = nro.models.services.InventoryService.gI().findItemBag(player, 2016);
            if (manhNgoc != null && manhNgoc.quantity >= 99) {
                nro.models.services.InventoryService.gI().subQuantityItemsBag(player, manhNgoc, 99);
                nro.models.services.InventoryService.gI().sendItemBags(player);
                int randomX = player.location.x + nro.models.utils.Util.nextInt(-50, 50);
                if (randomX < 100) randomX = 100;
                if (randomX > player.zone.map.mapWidth - 100) randomX = player.zone.map.mapWidth - 100;
                int y = player.zone.map.yPhysicInTop(randomX, player.location.y);
                nro.models.map.ItemMap biaDa = new nro.models.map.ItemMap(player.zone, 2017, 1, randomX, y, player.id);
                nro.models.services.Service.gI().dropItemMap(player.zone, biaDa);
                nro.models.services.Service.gI().sendThongBao(player, "Bia Đá Cổ Tự đã rơi xuống xung quanh, hãy tìm và nhặt nó!");
            } else {
                nro.models.services.Service.gI().sendThongBao(player, "Bạn cần thu thập đủ 99 Mảnh Ngọc Rồng Nứt!");
            }
        }
    }
}
