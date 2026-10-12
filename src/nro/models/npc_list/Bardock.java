package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.consts.ConstTask;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.TaskService;
import nro.models.map.service.ChangeMapService;

/**
 *
 * @author By Mr Blue
 *
 */
public class Bardock extends Npc {

    public Bardock(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                if (player.zone.map.mapId == 52) {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Hãy đến vùng đất Xayda để tiêu diệt kẻ thù", "Đến đó", "Từ chối");
                } else if (player.zone.map.mapId == 202 || player.zone.map.mapId == 206) {
                    if (TaskService.gI().getIdTask(player) == ConstTask.TASK_32_0) {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Ngươi muốn nhận nhiệm vụ Ký Ức Bi Tráng chứ?", "Nhận\nnhiệm vụ", "Về nhà", "Đóng");
                    } else if (TaskService.gI().getIdTask(player) == ConstTask.TASK_32_1) {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Ngươi đã thu thập đủ 100 Linh hồn đồng đội chưa?", "Báo cáo", "Về nhà", "Đóng");
                    } else if (TaskService.gI().getIdTask(player) == ConstTask.TASK_32_2) {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Ngươi hãy tiêu diệt 1000 tên Toppo!", "Về nhà", "Đóng");
                    } else if (TaskService.gI().getIdTask(player) == ConstTask.TASK_32_3) {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Hãy tìm và sử dụng Bản đồ quá khứ để đột kích tàu Frieza!", "Về nhà", "Đóng");
                    } else if (TaskService.gI().getIdTask(player) == ConstTask.TASK_32_4) {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Ngươi đã tìm được Băng buộc đầu thắm máu chưa?", "Báo cáo", "Về nhà", "Đóng");
                    } else if (TaskService.gI().getIdTask(player) == ConstTask.TASK_32_5) {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Tiếp tục đánh bại bạo chúa Fide Vàng ở quá khứ!", "Về nhà", "Đóng");
                    } else if (TaskService.gI().getIdTask(player) == ConstTask.TASK_32_6) {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Hãy tìm và tiêu diệt Chilled 1 để kết thúc mọi ân oán!", "Về nhà", "Đóng");
                    } else if (TaskService.gI().getIdTask(player) == ConstTask.TASK_32_7) {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Hãy tìm và tiêu diệt Chilled 2 để kết thúc mọi ân oán!", "Về nhà", "Đóng");
                    } else if (TaskService.gI().getIdTask(player) == ConstTask.TASK_32_8) {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Ngươi làm tốt lắm! Đây là phần thưởng cho ngươi.", "Nhận\nthưởng", "Về nhà", "Đóng");
                    } else {
                        this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Ngươi tìm ta có việc gì?", "Nâng cấp\nNhẫn Bardock", "Về nhà", "Đóng");
                    }
                } else {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Ngươi tìm ta có việc gì?", "Đóng");
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.isBaseMenu()) {
                if (player.zone.map.mapId == 52) {
                    switch (select) {
                        case 0 -> {
                            if (TaskService.gI().isCurrentTask(player, nro.models.consts.ConstTask.TASK_32_0)) {
                                TaskService.gI().doneTask(player, nro.models.consts.ConstTask.TASK_32_0);
                            }
                            ChangeMapService.gI().changeMap(player, 202, -1, 185, 312);
                        }
                    }
                } else if (player.zone.map.mapId == 202 || player.zone.map.mapId == 206) {
                    int taskId = TaskService.gI().getIdTask(player);
                    if (taskId == ConstTask.TASK_32_0 || taskId == ConstTask.TASK_32_1 || taskId == ConstTask.TASK_32_4 || taskId == ConstTask.TASK_32_8) {
                        switch (select) {
                            case 0 -> {
                                if (taskId == ConstTask.TASK_32_0) {
                                    TaskService.gI().doneTask(player, ConstTask.TASK_32_0);
                                    nro.models.services.Service.gI().sendThongBao(player, "Bạn đã nhận nhiệm vụ: Thu thập 100 Linh hồn đồng đội!");
                                } else if (taskId == ConstTask.TASK_32_1) {
                                    nro.models.item.Item linhHon = nro.models.services.InventoryService.gI().findItemBag(player, 2007);
                                    if (linhHon != null && linhHon.quantity >= 100) {
                                        TaskService.gI().doneTask(player, ConstTask.TASK_32_1);
                                        nro.models.services.Service.gI().sendThongBao(player, "Nhiệm vụ tiếp theo: Tiêu diệt 1000 Toppo!");
                                    } else {
                                        nro.models.services.Service.gI().sendThongBao(player, "Bạn chưa đủ 100 Linh hồn đồng đội!");
                                    }
                                } else if (taskId == ConstTask.TASK_32_4) {
                                    nro.models.item.Item bangBuocDau = nro.models.services.InventoryService.gI().findItemBag(player, 2009);
                                    if (bangBuocDau != null && bangBuocDau.quantity >= 1) {
                                        TaskService.gI().doneTask(player, ConstTask.TASK_32_4);
                                        nro.models.services.Service.gI().sendThongBao(player, "Nhiệm vụ tiếp theo: Đánh bại Fide Vàng!");
                                    } else {
                                        nro.models.services.Service.gI().sendThongBao(player, "Bạn chưa tìm được Băng buộc đầu thắm máu!");
                                    }
                                } else if (taskId == ConstTask.TASK_32_8) {
                                    if (nro.models.services.InventoryService.gI().getCountEmptyBag(player) > 0) {
                                        TaskService.gI().doneTask(player, ConstTask.TASK_32_8);
                                        nro.models.item.Item nhan = nro.models.services.ItemService.gI().createNewItem((short) 2020);
                                        nhan.itemOptions.add(new nro.models.item.Item.ItemOption(30, 1));
                                        nhan.itemOptions.add(new nro.models.item.Item.ItemOption(77, 5));
                                        nhan.itemOptions.add(new nro.models.item.Item.ItemOption(103, 5));
                                        nhan.itemOptions.add(new nro.models.item.Item.ItemOption(50, 5));
                                        nhan.itemOptions.add(new nro.models.item.Item.ItemOption(14, 1));
                                        nro.models.services.InventoryService.gI().addItemBag(player, nhan);
                                        nro.models.services.InventoryService.gI().sendItemBags(player);
                                        nro.models.services.Service.gI().sendThongBao(player, "Bạn nhận được Nhẫn Bardock cấp 0!");
                                    } else {
                                        nro.models.services.Service.gI().sendThongBao(player, "Hành trang đầy!");
                                    }
                                }
                            }
                            case 1 -> {
                                ChangeMapService.gI().changeMapInYard(player, player.gender + 21, -1, -1);
                            }
                        }
                    } else if (taskId == ConstTask.TASK_32_2 || taskId == ConstTask.TASK_32_3 || taskId == ConstTask.TASK_32_5 || taskId == ConstTask.TASK_32_6 || taskId == ConstTask.TASK_32_7) {
                        switch (select) {
                            case 0 -> {
                                ChangeMapService.gI().changeMapInYard(player, player.gender + 21, -1, -1);
                            }
                        }
                    } else {
                        switch (select) {
                            case 0 -> {
                                nro.models.combine.CombineService.gI().openTabCombine(player, nro.models.combine.CombineService.NANG_CAP_NHAN_BARDOCK);
                            }
                            case 1 -> {
                                ChangeMapService.gI().changeMapInYard(player, player.gender + 21, -1, -1);
                            }
                        }
                    }
                }
            } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE) {
                if (player.combineNew.typeCombine == nro.models.combine.CombineService.NANG_CAP_NHAN_BARDOCK) {
                    if (select == 0) {
                        nro.models.combine.CombineService.gI().startCombine(player);
                    }
                }
            }
        }
    }
}
