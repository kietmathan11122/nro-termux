package nro.models.server;

import nro.models.services.PlayerService;
import nro.models.boss.Boss_Manager.BrolyManager;
import nro.models.consts.ConstNpc;
import nro.models.managers.GiftCodeManager;
import nro.models.item.Item;
import java.util.ArrayList;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.network.SessionManager;
import nro.models.services.ItemService;
import nro.models.services.PetService;
import nro.models.services.RadarService;
import nro.models.services.Service;
import nro.models.services_func.Input;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.NpcService;
import nro.models.services.InventoryService;
import nro.models.utils.SystemMetrics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.Connection;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import nro.models.Bot.BotManager;
import nro.models.consts.ConstPlayer;
import nro.models.data.LocalManager;
import nro.models.database.ShopDAO;
import nro.models.database.PlayerDAO;
import nro.models.services.HeartsRebirth;
import nro.models.services.TaskService;
import nro.models.skill.Skill;
import nro.models.utils.SkillUtil;

/**
 *
 * @author By Mr Blue
 *
 */
public class Command {

    private static final int FIRST_WEATHER_EFFECT = 1001;
    private static final int LAST_WEATHER_EFFECT = 1009;
    private static final Pattern MAP_EFFECT_COMMAND = Pattern.compile(
            "^eff\\s+(\\d+)\\s+map\\s+(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final String[] WEATHER_EFFECT_NAMES = {
        "", "Mưa lá phong", "Mưa sấm sét", "Hoa bay", "Mưa thiên thạch",
        "Lá vàng rơi", "Mưa trái cây", "Mưa đá", "Lá cây rơi", "Mưa axit"
    };

    private static Command instance;

    private final Map<String, Consumer<Player>> adminCommands = new HashMap<>();
    private final Map<String, BiConsumer<Player, String>> parameterizedCommands = new HashMap<>();

    public static Command gI() {
        if (instance == null) {
            instance = new Command();
        }
        return instance;
    }

    private Command() {
        initAdminCommands();
        initParameterizedCommands();
    }

    private void initAdminCommands() {
		for (int skinNumber = 0; skinNumber <= 112; skinNumber++) {
			final int selectedSkin = skinNumber;
			adminCommands.put("skin" + skinNumber, player -> {
				player.adminSpineSkinIndex = (short) (selectedSkin == 0 ? -1 : selectedSkin - 1);
				Service.gI().Send_Caitrang(player);
				Service.gI().sendThongBao(player, selectedSkin == 0
						? "Đã trở về ngoại hình thường."
						: "Đã sử dụng skin Spine " + selectedSkin + "/112.");
			});
		}
        adminCommands.put("skipnv", player -> {
            if (player.playerTask != null && player.playerTask.taskMain != null) {
                player.playerTask.taskMain.index++;
                if (player.playerTask.taskMain.index >= player.playerTask.taskMain.subTasks.size()) {
                    TaskService.gI().sendNextTaskMain(player);
                } else {
                    TaskService.gI().sendNextSubTask(player);
                }
                Service.gI().sendThongBao(player, "Đã bỏ qua bước nhiệm vụ hiện tại!");
            }
        });
        adminCommands.put("item", player -> Input.gI().createFormGiveItem(player));
        adminCommands.put("c", player -> Input.gI().createFormAdminAddVnd(player));
        adminCommands.put("b", player -> Input.gI().createFormSenditem1(player));

        adminCommands.put("brl", player -> BrolyManager.gI().showListBoss(player));
        adminCommands.put("lg", player -> {
            GiftCodeManager.gI().loadGiftCodeFromDB();
            Service.gI().sendThongBaoOK(player, "Reload giftcode thành công!");
        });
        adminCommands.put("ls", player -> {
            try (Connection con = LocalManager.getConnection()) {
                Manager.SHOPS = ShopDAO.getShops(con);
                Service.gI().sendThongBaoOK(player, "Reload shop thành công!");
                System.out.println("Reload Shop: " + Manager.SHOPS.size());

            } catch (Exception e) {
                e.printStackTrace();
                Service.gI().sendThongBaoOK(player, "Reload shop thất bại!");
            }
        });

        adminCommands.put("getitem", player -> Input.gI().createFormGetItem(player));
        adminCommands.put("hs", player -> Service.gI().releaseCooldownSkill(player));
		adminCommands.put("tinhlinh1", player -> {
			int randomForm = nro.models.utils.Util.nextInt(
					nro.models.services.SpiritDiscipleRules.WIND,
					nro.models.services.SpiritDiscipleRules.DEMON);
			nro.models.services.SpiritDiscipleService.choose(player, randomForm);
		});
        adminCommands.put("cstd1", player -> {
            synchronized (player) {
                if (player.gender != ConstPlayer.TRAI_DAT) {
                    Service.gI().sendThongBao(player,
                            "Lệnh cstd1 chỉ dùng cho nhân vật Trái Đất.");
                    return;
                }

                boolean firstGrant = !HeartsRebirth.isReborn(player);
                // A previous admin skin preview overrides all three rebirth parts.
                player.adminSpineSkinIndex = -1;
                player.head = 2139;
                player.heartsReborn = true;
                // Repeating cstd1 must also repair the current client's skill list.
                HeartsRebirth.ensureSkills(player, true);
                player.nPoint.calPoint();
                player.nPoint.hp = player.nPoint.hpMax;
                player.nPoint.mp = player.nPoint.mpMax;
                Service.gI().point(player);
                PlayerService.gI().sendInfoHpMpMoney(player);
                Service.gI().Send_Caitrang(player);
                PlayerDAO.updatePlayer(player);

                Service.gI().sendThongBao(player, firstGrant
                        ? "Chuyển sinh Hearts cấp 1 thành công! Đã mở 5 kỹ năng Hearts."
                        : "Nhân vật đã là Hearts cấp 1; kỹ năng và ngoại hình đã được đồng bộ.");
            }
        });
        adminCommands.put("bc1", player -> {
            nro.models.services.FootstepService.gI().toggleFootstep(player, nro.models.services.FootstepService.EFFECT_ROSE_BLUE, "Hoa hồng xanh");
        });
        adminCommands.put("bc2", player -> {
            nro.models.services.FootstepService.gI().toggleFootstep(player, nro.models.services.FootstepService.EFFECT_ROSE_RED, "Hoa hồng đỏ");
        });
        adminCommands.put("bcoff", player -> {
            player.activeFootstepEffect = 0;
            Service.gI().sendThongBao(player, "Đã tắt hiệu ứng bước chân.");
        });
        adminCommands.put("itembc", player -> {
            nro.models.services.FootstepService.gI().giveFootstepItem(player, nro.models.services.FootstepService.ITEM_ROSE_RED);
            nro.models.services.FootstepService.gI().giveFootstepItem(player, nro.models.services.FootstepService.ITEM_ROSE_BLUE);
        });
        adminCommands.put("d", player -> Service.gI().setPos(player, player.location.x, player.location.y + 10));
        adminCommands.put("dt3", player -> {
            if (player.pet != null) {
                PetService.gI().changeKidBeerPet(player);
            } else {
                PetService.gI().createKidBeerPet(player);
            }
            Service.gI().sendThongBao(player, "Đã nhận được đệ tử Kid Beerus (Cấp 3) thành công!");
        });
        adminCommands.put("callfrost", player -> {
            nro.models.boss.Boss_Manager.BossManager.gI().createBoss(nro.models.boss.BossID.FROST_1);
            nro.models.services.Service.gI().sendThongBao(player, "Đã gọi Boss Frost 1!");
        });
        adminCommands.put("callgete", player -> {
            nro.models.boss.Boss_Manager.BossManager.gI().createBoss(nro.models.boss.BossID.BIG_GETE_STAR);
            nro.models.services.Service.gI().sendThongBao(player, "Đã gọi Boss Big Gete Star!");
        });
        adminCommands.put("callhiru", player -> {
            nro.models.boss.Boss_Manager.BossManager.gI().createBoss(nro.models.boss.BossID.HIRUDEGARN);
            nro.models.services.Service.gI().sendThongBao(player, "Đã gọi Boss Hirudegarn!");
        });
        adminCommands.put("callsbv1", player -> {
            nro.models.boss.Boss_Manager.BossManager.gI().createBoss(nro.models.boss.BossID.SUPER_BROLY_V1);
            nro.models.services.Service.gI().sendThongBao(player, "Đã gọi Boss Super Broly V1!");
        });
        adminCommands.put("boss 1", player -> {
            nro.models.boss.Boss_Manager.BossManager.gI().createBoss(nro.models.boss.BossID.FU);
            nro.models.services.Service.gI().sendThongBao(player, "Đã gọi Boss Fu!");
        });
        adminCommands.put("boss1", player -> {
            nro.models.boss.Boss_Manager.BossManager.gI().createBoss(nro.models.boss.BossID.FU);
            nro.models.services.Service.gI().sendThongBao(player, "Đã gọi Boss Fu!");
        });
        adminCommands.put("goiboss 1", player -> {
            nro.models.boss.Boss_Manager.BossManager.gI().createBoss(nro.models.boss.BossID.FU);
            nro.models.services.Service.gI().sendThongBao(player, "Đã gọi Boss Fu!");
        });
        adminCommands.put("goiboss1", player -> {
            nro.models.boss.Boss_Manager.BossManager.gI().createBoss(nro.models.boss.BossID.FU);
            nro.models.services.Service.gI().sendThongBao(player, "Đã gọi Boss Fu!");
        });
        adminCommands.put("goiboss 2", player -> {
            nro.models.boss.Boss_Manager.BossManager.gI().createBoss(nro.models.boss.BossID.CUMBER);
            nro.models.services.Service.gI().sendThongBao(player, "Đã gọi Boss Cumber!");
        });
        adminCommands.put("goiboss2", player -> {
            nro.models.boss.Boss_Manager.BossManager.gI().createBoss(nro.models.boss.BossID.CUMBER);
            nro.models.services.Service.gI().sendThongBao(player, "Đã gọi Boss Cumber!");
        });
        adminCommands.put("callallboss", player -> {
            try {
                int mapId = player.zone.map.mapId;
                int zoneId = player.zone.zoneId;
                
                // Spawn Frost 1, 2, 3
                nro.models.boss.Boss f1 = nro.models.boss.Boss_Manager.BossManager.gI().createBoss(nro.models.boss.BossID.FROST_1);
                if (f1 != null) {
                    f1.zoneFinal = player.zone;
                    f1.joinMapByZone(player.zone);
                }
                nro.models.boss.Boss f2 = nro.models.boss.Boss_Manager.BossManager.gI().createBoss(nro.models.boss.BossID.FROST_2);
                if (f2 != null) {
                    f2.zoneFinal = player.zone;
                    f2.joinMapByZone(player.zone);
                }
                nro.models.boss.Boss f3 = nro.models.boss.Boss_Manager.BossManager.gI().createBoss(nro.models.boss.BossID.FROST_3);
                if (f3 != null) {
                    f3.zoneFinal = player.zone;
                    f3.joinMapByZone(player.zone);
                }
                
                // Spawn Hirudegarn & Big Gete Star
                nro.models.boss.Boss hiru = nro.models.boss.Boss_Manager.BossManager.gI().createBoss(nro.models.boss.BossID.HIRUDEGARN);
                if (hiru != null) {
                    hiru.zoneFinal = player.zone;
                    hiru.joinMapByZone(player.zone);
                }
                nro.models.boss.Boss bgs = nro.models.boss.Boss_Manager.BossManager.gI().createBoss(nro.models.boss.BossID.BIG_GETE_STAR);
                if (bgs != null) {
                    bgs.zoneFinal = player.zone;
                    bgs.joinMapByZone(player.zone);
                }
                
                // Spawn Chilled 1 & 2
                nro.models.boss.Boss c1 = nro.models.boss.Boss_Manager.BossManager.gI().createBoss(nro.models.boss.BossID.CHILL_1);
                if (c1 != null) {
                    c1.zoneFinal = player.zone;
                    c1.joinMapByZone(player.zone);
                }
                nro.models.boss.Boss c2 = nro.models.boss.Boss_Manager.BossManager.gI().createBoss(nro.models.boss.BossID.CHILL_2);
                if (c2 != null) {
                    c2.zoneFinal = player.zone;
                    c2.joinMapByZone(player.zone);
                }
                
                // Zarbon, Dodoria, Fide Vang
                new nro.models.boss.Nappa.Zarbon(mapId, zoneId);
                new nro.models.boss.Nappa.Dodoria(mapId, zoneId);
                new nro.models.boss.Nappa.FideVang(mapId, zoneId);
                
                // Bong Ma Ta Ac
                new nro.models.boss.nhan_ban.BongMaTaAc(player);

                Service.gI().sendThongBao(player, "Đã gọi tất cả các Boss vào khu vực!");
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Có lỗi xảy ra khi gọi Boss!");
                e.printStackTrace();
            }
        });
        adminCommands.put("a", player -> {

    // ===== OS BEAN (chỉ khai báo 1 lần) =====
    com.sun.management.OperatingSystemMXBean osBean =
            (com.sun.management.OperatingSystemMXBean)
                    java.lang.management.ManagementFactory.getOperatingSystemMXBean();

    // ===== RAM =====
    Runtime rt = Runtime.getRuntime();
    long usedMemory = rt.totalMemory() - rt.freeMemory(); // x (java)

    long totalRAM = osBean.getTotalPhysicalMemorySize(); // z
    long freeRAM = osBean.getFreePhysicalMemorySize();
    long usedRAM = totalRAM - freeRAM; // y

    double usedMemoryGB = usedMemory / 1024.0 / 1024 / 1024;
    double usedRAMGB = usedRAM / 1024.0 / 1024 / 1024;
    double totalRAMGB = totalRAM / 1024.0 / 1024 / 1024;

    String ram = String.format("RAM: %.2fGB/%.2fGB/%.2fGB",
            usedMemoryGB, usedRAMGB, totalRAMGB);

    // ===== CPU =====
    double cpuProcess = osBean.getProcessCpuLoad() * 100;
    double cpuSystem = osBean.getSystemCpuLoad() * 100;
    int cpuCore = osBean.getAvailableProcessors();

    String cpu = String.format("CPU: %.1f%%/%.1f%%/%d core",
            cpuProcess, cpuSystem, cpuCore);

    NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_ADMIN, 16103,
            "|0|Time start: " + ServerManager.timeStart
                    + "\nNgười chơi: " + Client.gI().getPlayers().size()
                    + "\nSessions: " + SessionManager.gI().getNumSession()
                    + "\nThreads: " + Thread.activeCount() + " luồng"
                    + "\n" + ram
                    + "\n" + cpu,
            "Ngọc rồng", "Đệ tử", "Bảo trì", "Tìm kiếm\nngười chơi", "Boss", "Đóng");
});
    }
    private void initParameterizedCommands() {
        parameterizedCommands.put("eff ", (player, text) -> {
            Matcher matcher = MAP_EFFECT_COMMAND.matcher(text.trim());
            if (!matcher.matches()) {
                Service.gI().sendThongBao(player, "Cú pháp: eff <0-9> map <mapId>. Dùng eff 0 để xóa hiệu ứng.");
                return;
            }
            try {
                int effectNumber = Integer.parseInt(matcher.group(1));
                int mapId = Integer.parseInt(matcher.group(2));
                setMapWeatherEffect(player, effectNumber, mapId);
            } catch (Exception e) {
                e.printStackTrace();
                Service.gI().sendThongBao(player, "Không thể cập nhật hiệu ứng map: " + e.getMessage());
            }
        });

        parameterizedCommands.put("ur", (player, text) -> {
            try {
                int auraId = Integer.parseInt(text.substring(2).trim());
                if (auraId == -1) {
                    player.adminAuraId = -1;
                    RadarService.gI().sendAura(player, player.getAuraId(), player.getEffFront());
                    Service.gI().sendThongBao(player, "Đã trả về hào quang mặc định.");
                    return;
                }

                if (auraId < 0 || auraId > Short.MAX_VALUE) {
                    Service.gI().sendThongBao(player, "ID aura phải từ 0 đến " + Short.MAX_VALUE + ".");
                    return;
                }

                String auraBehind = "aura_" + auraId + "_0";
                String auraFront = "aura_" + auraId + "_1";
                if (!Manager.IMAGES_BY_NAME.containsKey(auraBehind)
                        && !Manager.IMAGES_BY_NAME.containsKey(auraFront)) {
                    Service.gI().sendThongBao(player, "Không tìm thấy aura ID " + auraId + ".");
                    return;
                }

                player.isHideAura = false;
                player.adminAuraId = (short) auraId;
                RadarService.gI().sendAura(player, auraId, player.getEffFront());
                Service.gI().sendThongBao(player, "Đã đổi sang aura ID " + auraId + ".");
            } catch (NumberFormatException e) {
                Service.gI().sendThongBao(player, "Cú pháp: ur <id aura>, dùng ur -1 để đặt lại.");
            }
        });
        parameterizedCommands.put("m", (player, text) -> {
            try {
                int mapId = Integer.parseInt(text.replace("m", "").trim());
                ChangeMapService.gI().changeMapInYard(player, mapId, -1, -1);
            } catch (NumberFormatException e) {
                Service.gI().sendThongBao(player, "Sai định dạng map ID!");
            }
        });
        parameterizedCommands.put("exp", (player, text) -> {
            try {
                int rate = Integer.parseInt(text.replace("exp", "").trim());

                if (rate <= 0) {
                    Service.gI().sendThongBao(player, "Rate phải > 0!");
                    return;
                }

                Manager.RATE_EXP_SERVER = rate;

                Service.gI().sendThongBaoOK(player,
                        "EXP server: x" + rate);

                Service.gI().sendThongBaoAllPlayer(
                        "🔥 EXP server hiện tại: x" + rate);

            } catch (NumberFormatException e) {
                Service.gI().sendThongBao(player, "Sai định dạng! Ví dụ: exp2 hoặc exp 2");
            } catch (Exception e) {
                e.printStackTrace();
                Service.gI().sendThongBao(player, "Lỗi!");
            }
        });
        parameterizedCommands.put("toado", (player, text) -> {
            Service.gI().sendThongBaoOK(player, "x: " + player.location.x + " - y: " + player.location.y);
        });

        parameterizedCommands.put("1", (player, text) -> {
            NpcService.gI().createMenuConMeo(player, 206783, 206783, "|7| Menu bot\n"
                    + "Player online : " + Client.gI().getPlayers().size() + "\n"
                    + "\b|1|Thread: " + Thread.activeCount() + "\n"
                    + "\n Sessions: " + SessionManager.gI().getNumSession() + "\n"
                    + "Bot online : " + BotManager.gI().bot.size(),
                    "Bot\nPem Quái", "Bot\nBán Item", "Bot\nSăn Boss", "Bot\nAttack Player", "Bot\nBán Đá Vách");
            return;
        });

        parameterizedCommands.put("bottall", (player, text) -> {
            BotManager.gI().spawnStoneTradeBots();
            Service.gI().sendThongBao(player, "Đã bật/gọi toàn bộ 10 Bot Giao Dịch tại Vách Kakarot khu 0!");
        });

        parameterizedCommands.put("botall", (player, text) -> {
            BotManager.gI().spawnStoneTradeBots();
            Service.gI().sendThongBao(player, "Đã bật/gọi toàn bộ 10 Bot Giao Dịch tại Vách Kakarot khu 0!");
        });

        parameterizedCommands.put("botgd", (player, text) -> {
            BotManager.gI().spawnStoneTradeBots();
            Service.gI().sendThongBao(player, "Đã bật/gọi toàn bộ 10 Bot Giao Dịch tại Vách Kakarot khu 0!");
        });

        parameterizedCommands.put("tatbotgd", (player, text) -> {
            BotManager.gI().removeStoneTradeBots();
            Service.gI().sendThongBao(player, "Đã tắt toàn bộ Bot Giao Dịch tại Vách Kakarot!");
        });

        parameterizedCommands.put("tatbottall", (player, text) -> {
            BotManager.gI().removeStoneTradeBots();
            Service.gI().sendThongBao(player, "Đã tắt toàn bộ Bot Giao Dịch tại Vách Kakarot!");
        });

        parameterizedCommands.put("botda", (player, text) -> {
            BotManager.gI().spawnStoneTradeBots();
            Service.gI().sendThongBao(player, "Đã gọi/làm mới 10 bot mua bán tại Vách Kakarot khu 0!");
        });

        parameterizedCommands.put("panel", (player, text) -> {
            Service.gI().sendThongBao(player, "Link Panel: http://localhost/sv1/admin/panel.php");
        });

        BiConsumer<Player, String> anTromAction = (player, text) -> {
            try {
                nro.models.boss.Boss_mini.AnTrom boss = new nro.models.boss.Boss_mini.AnTrom();
                boss.summonAt(player.zone, player.location.x + 50, player.location.y);
                Service.gI().sendThongBao(player, "Đã gọi Boss Ăn Trộm đến cạnh bạn!");
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Lỗi gọi Boss Ăn Trộm: " + e.getMessage());
            }
        };
        parameterizedCommands.put("antrom", anTromAction);
        parameterizedCommands.put("an trom", anTromAction);

        parameterizedCommands.put("rati", (player, text) -> {
            try {
                nro.models.boss.Boss_mini.BotRati boss = new nro.models.boss.Boss_mini.BotRati();
                boss.summonAt(player.zone, player.location.x + 50, player.location.y);
                Service.gI().sendThongBao(player, "Đã gọi Bot Rati đến cạnh bạn!");
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Lỗi gọi Bot Rati: " + e.getMessage());
            }
        });

        BiConsumer<Player, String> santaAction = (player, text) -> {
            try {
                nro.models.boss.Boss_mini.QuySanta boss = new nro.models.boss.Boss_mini.QuySanta();
                boss.summonAt(player.zone, player.location.x + 50, player.location.y);
                Service.gI().sendThongBao(player, "Đã gọi Quỷ Santa đến cạnh bạn!");
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Lỗi gọi Quỷ Santa: " + e.getMessage());
            }
        };
        parameterizedCommands.put("santan", santaAction);
        parameterizedCommands.put("santa", santaAction);

        parameterizedCommands.put("brydt", (player, text) -> {
            try {
                if (player.zone == null) {
                    Service.gI().sendThongBao(player, "Bạn chưa ở trong bản đồ!");
                    return;
                }
                new nro.models.boss.Broly.SuperBroly(
                        player.zone, player.location.x + 50, player.location.y);
                Service.gI().sendThongBao(player,
                        "Đã triệu hồi Super Broly tóc xanh cạnh bạn!");
            } catch (Exception e) {
                Service.gI().sendThongBao(player,
                        "Lỗi triệu hồi Super Broly: " + e.getMessage());
            }
        });

        parameterizedCommands.put("gb ", (player, text) -> {
            try {
                String[] split = text.split(" ");
                int bossId = Integer.parseInt(split[1]);
                if (bossId > 0) {
                    bossId = -bossId;
                }
                int count = split.length > 2 ? Integer.parseInt(split[2]) : 1;
                nro.models.boss.Boss_Manager.BossManager.gI().createBoss(bossId, count);
                Service.gI().sendThongBao(player, "Đã gọi boss ID: " + bossId + " (" + count + " con)");
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Lỗi cú pháp! Dùng: gb <bossID> [số lượng]");
            }
        });

        parameterizedCommands.put("hocskill", (player, text) -> {
            try {
                String[] split = text.trim().split("\\s+");
                if (split.length < 2) {
                    learnAllSkills(player, player.gender);
                    return;
                }

                String planet = split[1].toLowerCase();
                if (planet.equals("td") || planet.equals("nm") || planet.equals("xd")) {
                    byte gender = planet.equals("td") ? ConstPlayer.TRAI_DAT
                            : planet.equals("nm") ? ConstPlayer.NAMEC : ConstPlayer.XAYDA;
                    learnAllSkills(player, gender);
                    return;
                }

                int skillId = Integer.parseInt(split[1]);
                int level = split.length >= 3 ? Integer.parseInt(split[2]) : 7;
                nro.models.player_system.Template.SkillTemplate skillTemplate = SkillUtil.findSkillTemplate(skillId);
                if (skillTemplate == null) {
                    Service.gI().sendThongBao(player, "Không tìm thấy kỹ năng có ID: " + skillId);
                    return;
                }
                Skill nextSkill = SkillUtil.createSkill(skillTemplate.id, level);
                if (nextSkill != null) {
                    SkillUtil.setSkill(player, nextSkill);
                    sendAdminSkillUpdate(player, nextSkill);
                    nro.models.skill.NClass nClass = nro.models.server.Manager.NCLASS.get(player.gender);
                    refreshAdminSkillState(player, nClass);
                    Service.gI().sendThongBao(player, "Đã học kỹ năng " + skillTemplate.name + " lên cấp " + level + "!");
                } else {
                    int maxLvl = (skillTemplate.skillss != null) ? skillTemplate.skillss.size() : 0;
                    Service.gI().sendThongBao(player, "Cấp độ học không hợp lệ! (Kỹ năng này có tối đa " + maxLvl + " cấp)");
                }
            } catch (Exception e) {
                Service.gI().sendThongBao(player,
                        "Cú pháp: hocskill <td|nm|xd> hoặc hocskill <id_kỹ_năng> [cấp_độ]");
                e.printStackTrace();
            }
        });

        parameterizedCommands.put("n", (player, text) -> {
            try {
                int idTask = Integer.parseInt(text.replaceAll("n", "").trim());
                player.playerTask.taskMain.id = idTask - 1;
                player.playerTask.taskMain.index = 0;
                TaskService.gI().sendNextTaskMain(player);
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Sai định dạng task ID!");
            }
        });
        // dmg <value>
        parameterizedCommands.put("dm", (player, text) -> {
            try {
                int dmg = Integer.parseInt(text.replace("dm", "").trim());
                player.nPoint.dameg = dmg;
                Service.gI().point(player);
                Service.gI().sendThongBao(player, "SET DAMAGE = " + dmg);
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Sai cú pháp: dmg <số>");
            }
        });

        // hpg <value>
        parameterizedCommands.put("hp", (player, text) -> {
            try {
                int hpg = Integer.parseInt(text.replace("hp", "").trim());
                player.nPoint.hpg = hpg;
                Service.gI().point(player);
                Service.gI().sendThongBao(player, "SET HP GỐC = " + hpg);
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Sai cú pháp: hpg <số>");
            }
        });

        // ki <value>
        parameterizedCommands.put("ki", (player, text) -> {
            try {
                int ki = Integer.parseInt(text.replace("ki", "").trim());
                player.nPoint.mpg = ki;
                Service.gI().point(player);
                Service.gI().sendThongBao(player, "SET KI GỐC = " + ki);
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Sai cú pháp: ki <số>");
            }
        });
        // up <power>
        parameterizedCommands.put("up", (player, text) -> {
            try {
                long power = Long.parseLong(text.replace("up", "").trim());
                Service.gI().addSMTN(player, (byte) 2, power, false);
                Service.gI().sendThongBao(player, "UP SMTN = " + power);
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Sai cú pháp: up <số>");
            }
        });
        parameterizedCommands.put("upp", (player, text) -> {
            try {
                if (player.pet == null) {
                    Service.gI().sendThongBao(player, "Bạn chưa có đệ tử");
                    return;
                }

                long power = Long.parseLong(text.replace("upp", "").trim());
                Service.gI().addSMTN(player.pet, (byte) 2, power, false);
                Service.gI().sendThongBao(
                        player,
                        "UP TNSM cho đệ tử = " + power);
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Sai cú pháp: upp <số>");
            }
        });

        parameterizedCommands.put("sk", (player, text) -> {
            try {
                int eventId = Integer.parseInt(text.replace("sk", "").trim());
                if (eventId != 1 && eventId != 2 && eventId != 3 && eventId != 4 && eventId != 5
                        && eventId != 6 && eventId != 9 && eventId != 10) {
                    Service.gI().sendThongBao(player,
                            "Sự kiện khả dụng: sk1, sk2, sk3, sk4, sk5, sk6, sk9, sk10");
                    return;
                }
                nro.models.event.EventManager.gI().changeEvent(eventId);
                String eventName = nro.models.event.EventManager.gI().getEventName(eventId);
                if (eventId == 9) {
                    Service.gI().sendThongBaoAllPlayer("🎉 Admin đã tắt toàn bộ sự kiện!");
                } else {
                    Service.gI().sendThongBaoAllPlayer("🎉 Admin đã đổi sự kiện sang: " + eventName + "!");
                }
            } catch (Exception e) {
                Service.gI().sendThongBao(player,
                        "Cú pháp: sk <1|2|3|4|5|6|9|10>");
            }
        });

        parameterizedCommands.put("i ", (player, text) -> {
            try {
                String[] split = text.split(" ");
                if (split.length < 2) {
                    Service.gI().sendThongBao(player, "Cú pháp: i <itemId> <số lượng> [option:value...]");
                    return;
                }

                int itemId = Integer.parseInt(split[1]);
                int quantity = split.length >= 3 ? Integer.parseInt(split[2]) : 1;

                List<Item.ItemOption> customOptions = new ArrayList<>();
                for (int i = 3; i < split.length; i++) {
                    if (split[i].contains(":")) {
                        String[] optSplit = split[i].split(":");
                        int optionId = Integer.parseInt(optSplit[0]);
                        int optionValue = Integer.parseInt(optSplit[1]);
                        customOptions.add(new Item.ItemOption(optionId, optionValue));
                    }
                }

                if (quantity <= 0 || itemId < 0 || itemId > Short.MAX_VALUE
                        || ItemService.gI().getTemplate(itemId) == null) {
                    Service.gI().sendThongBao(player, "ID vật phẩm hoặc số lượng không hợp lệ!");
                    return;
                }
                Item item = ItemService.gI().createNewItem((short) itemId);
                item.quantity = quantity;
                if (!customOptions.isEmpty()) {
                    item.itemOptions = new ArrayList<>(customOptions);
                } else {
                    if (ItemService.isVongCo(itemId)) {
                        ItemService.gI().initVongCoOptions(item);
                    } else {
                        List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop((short) itemId);
                        if (!ops.isEmpty()) item.itemOptions = ops;
                    }
                }
                // One admin grant occupies one slot, regardless of its quantity.
                if (InventoryService.gI().addAdminItemToFront(player, item)) {
                    InventoryService.gI().sendItemBags(player);
                    Service.gI().sendThongBao(player, "GET " + quantity + " x " + ItemService.gI().getTemplate(itemId).name
                            + " [" + itemId + "] vào một ô SUCCESS!");
                } else {
                    Service.gI().sendThongBao(player, "Cần một ô trống để nhận vật phẩm!");
                }

            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Lỗi cú pháp! Dùng: i <itemId> <số lượng> [optionId:value]");
            }
        });

        parameterizedCommands.put("vongco", (player, text) -> {
            try {
                int star = 1;
                String arg = text.replace("vongco", "").trim();
                if (!arg.isEmpty()) {
                    star = Integer.parseInt(arg);
                }
                if (star < 1) star = 1;
                if (star > 7) star = 7;
                short itemId = (short) (2304 + star);
                Item item = ItemService.gI().createVongCo(itemId);
                if (InventoryService.gI().addAdminItemToFront(player, item)) {
                    InventoryService.gI().sendItemBags(player);
                    Service.gI().sendThongBao(player, "Nhận thành công " + item.template.name + "!");
                } else {
                    Service.gI().sendThongBao(player, "Hành trang đầy!");
                }
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Cú pháp: vongco <1-7>");
            }
        });

        parameterizedCommands.put("upvongco", (player, text) -> {
            Item body = player.inventory.itemsBody.size() > 14 ? player.inventory.itemsBody.get(14) : null;
            Item target = (body != null && ItemService.isVongCo(body.template.id)) ? body : null;
            if (target == null) {
                for (Item it : player.inventory.itemsBag) {
                    if (it != null && it.isNotNullItem() && ItemService.isVongCo(it.template.id)) {
                        target = it;
                        break;
                    }
                }
            }
            if (target != null) {
                if (ItemService.gI().upgradeVongCo(target)) {
                    InventoryService.gI().sendItemBody(player);
                    InventoryService.gI().sendItemBags(player);
                    Service.gI().point(player);
                    Service.gI().sendThongBao(player, "Nâng cấp thành công lên " + target.template.name + " (+1% cả 2 chỉ số)!");
                } else {
                    Service.gI().sendThongBao(player, "Vòng Cổ đã đạt cấp tối đa (7 Sao)!");
                }
            } else {
                Service.gI().sendThongBao(player, "Không tìm thấy Vòng Cổ Rồng trong người hoặc hành trang!");
            }
        });
    }

    private void setMapWeatherEffect(Player player, int effectNumber, int mapId) throws Exception {
        if (effectNumber < 0 || effectNumber >= WEATHER_EFFECT_NAMES.length) {
            Service.gI().sendThongBao(player, "Effect chỉ từ 1 đến 9; dùng 0 để xóa.");
            return;
        }
        boolean mapExists = false;
        String mapName = "Map " + mapId;
        for (nro.models.map.Map map : Manager.MAPS) {
            if (map.mapId == mapId) {
                mapExists = true;
                mapName = map.mapName;
                break;
            }
        }
        if (!mapExists) {
            Service.gI().sendThongBao(player, "Không tồn tại map ID " + mapId + ".");
            return;
        }

        Path effectDirectory = Path.of("data", "map", "eff_map");
        Files.createDirectories(effectDirectory);
        Path target = effectDirectory.resolve(Integer.toString(mapId));
        List<String[]> entries = readMapEffectEntries(target);
        entries.removeIf(entry -> entry.length == 2
                && "beff".equals(entry[0])
                && isCustomWeatherEffect(entry[1]));

        if (effectNumber > 0) {
            entries.add(new String[]{"beff", Integer.toString(FIRST_WEATHER_EFFECT + effectNumber - 1)});
        }
        writeMapEffectEntries(target, entries);

        if (effectNumber == 0) {
            Service.gI().sendThongBao(player, "Đã xóa hiệu ứng thời tiết khỏi " + mapName + " [" + mapId + "]. Vào lại map để cập nhật.");
        } else {
            Service.gI().sendThongBao(player, "Đã gắn " + WEATHER_EFFECT_NAMES[effectNumber]
                    + " vào " + mapName + " [" + mapId + "]. Vào lại map để cập nhật.");
        }
    }

    private boolean isCustomWeatherEffect(String value) {
        try {
            int id = Integer.parseInt(value);
            return id >= FIRST_WEATHER_EFFECT && id <= LAST_WEATHER_EFFECT;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private List<String[]> readMapEffectEntries(Path path) throws Exception {
        List<String[]> entries = new ArrayList<>();
        if (!Files.isRegularFile(path)) {
            return entries;
        }
        byte[] bytes = Files.readAllBytes(path);
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int count = input.readUnsignedShort();
            for (int i = 0; i < count; i++) {
                entries.add(new String[]{input.readUTF(), input.readUTF()});
            }
        }
        return entries;
    }

    private void writeMapEffectEntries(Path target, List<String[]> entries) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeShort(entries.size());
            for (String[] entry : entries) {
                output.writeUTF(entry[0]);
                output.writeUTF(entry[1]);
            }
        }

        Path temporary = target.resolveSibling(target.getFileName() + ".weather.tmp");
        Files.write(temporary, bytes.toByteArray());
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void sendAdminSkillUpdate(Player player, Skill skill) {
        if (player == null || skill == null) return;
        nro.models.network.Message message = null;
        try {
            message = Service.gI().messageSubCommand((byte) 62);
            message.writer().writeShort(skill.skillId);
            player.sendMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (message != null) message.cleanup();
        }
    }

    private void learnAllSkills(Player player, byte gender) {
        if (gender < 0 || gender >= Manager.NCLASS.size()) {
            Service.gI().sendThongBao(player, "Hành tinh không hợp lệ!");
            return;
        }
        nro.models.skill.NClass nClass = Manager.NCLASS.get(gender);
        if (nClass == null) {
            Service.gI().sendThongBao(player, "Không tìm thấy bộ kỹ năng của hành tinh!");
            return;
        }

        int learnedCount = 0;
        for (nro.models.player_system.Template.SkillTemplate skillTemplate : nClass.skillTemplatess) {
            if (skillTemplate.skillss == null || skillTemplate.skillss.isEmpty()) continue;
            int maxLevel = Math.min(7, skillTemplate.skillss.size());
            Skill skill = SkillUtil.createSkill(skillTemplate.id, maxLevel);
            if (skill == null) continue;
            SkillUtil.setSkill(player, skill);
            sendAdminSkillUpdate(player, skill);
            learnedCount++;
        }
        refreshAdminSkillState(player, nClass);

        String planetName = gender == ConstPlayer.TRAI_DAT ? "Trái Đất"
                : gender == ConstPlayer.NAMEC ? "Namếc" : "Xayda";
        Service.gI().sendThongBao(player,
                "Đã học " + learnedCount + " kỹ năng " + planetName + " lên cấp tối đa!");
    }

    private void refreshAdminSkillState(Player player, nro.models.skill.NClass nClass) {
        if (player == null || player.playerSkill == null || nClass == null) return;
        for (int i = 0; i < player.playerSkill.skillShortCut.length; i++) {
            player.playerSkill.skillShortCut[i] = -1;
        }
        Skill firstUsable = null;
        int shortcutIndex = 0;
        for (nro.models.player_system.Template.SkillTemplate template : nClass.skillTemplatess) {
            Skill learned = player.playerSkill.getSkillbyId(template.id);
            if (learned == null || learned.skillId == -1) continue;
            if (shortcutIndex < player.playerSkill.skillShortCut.length) {
                player.playerSkill.skillShortCut[shortcutIndex++] = template.id;
            }
            if (firstUsable == null && learned.template.type == 1) firstUsable = learned;
        }
        if (firstUsable == null && !player.playerSkill.skills.isEmpty()) {
            firstUsable = player.playerSkill.skills.get(0);
        }
        if (firstUsable != null) player.playerSkill.skillSelect = firstUsable;
        player.playerSkill.sendSkillShortCut();
        // Skill levels already use sub-command 62 and shortcuts use 61. A full
        // login packet resets the client's map lists and resolved costume parts.
        // Resend only the resolved appearance, also repairing an earlier reset.
        Service.gI().Send_Caitrang(player);
    }

    public void chat(Player player, String text) {
        String cleanedText = text.trim();
        if (cleanedText.isEmpty()) {
            return;
        }
        if (!check(player, cleanedText)) {
            Service.gI().chat(player, cleanedText);
        }
    }

    public boolean check(Player player, String text) {
        String cmd = text != null ? text.trim() : "";
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1).trim();
        }
        if (cmd.equalsIgnoreCase("cottruyen") || cmd.equalsIgnoreCase("hanhtrinh")) {
            nro.models.services.StoryAdventureService.gI().openJournal(player);
            return true;
        }
        if (cmd.startsWith("bcy ")) {
            try {
                int off = Integer.parseInt(cmd.substring(4).trim());
                nro.models.services.FootstepService.offsetY = off;
                Service.gI().sendThongBao(player, "Đã chỉnh độ cao bước chân Y: " + off);
                return true;
            } catch (Exception ignored) {}
        }
        if (player.isAdmin()) {
            if (adminCommands.containsKey(cmd) || adminCommands.containsKey(text)) {
                Consumer<Player> action = adminCommands.get(cmd);
                if (action == null) action = adminCommands.get(text);
                action.accept(player);
                return true;
            }

            // 1. Kiểm tra khớp chính xác lệnh trong parameterizedCommands
            if (parameterizedCommands.containsKey(cmd)) {
                parameterizedCommands.get(cmd).accept(player, cmd);
                return true;
            }
            if (parameterizedCommands.containsKey(text)) {
                parameterizedCommands.get(text).accept(player, text);
                return true;
            }

            // 2. Sắp xếp key theo độ dài giảm dần để lệnh dài (vd: bottall) không bị lệnh ngắn (vd: bot) chặn
            List<Map.Entry<String, BiConsumer<Player, String>>> sortedEntries = new ArrayList<>(parameterizedCommands.entrySet());
            sortedEntries.sort((e1, e2) -> Integer.compare(e2.getKey().length(), e1.getKey().length()));

            for (Map.Entry<String, BiConsumer<Player, String>> entry : sortedEntries) {
                String key = entry.getKey();
                if (cmd.equals(key)) {
                    entry.getValue().accept(player, cmd);
                    return true;
                }
                if (key.endsWith(" ") && cmd.startsWith(key)) {
                    entry.getValue().accept(player, cmd);
                    return true;
                }
                if (cmd.startsWith(key + " ")) {
                    entry.getValue().accept(player, cmd);
                    return true;
                }
                if (cmd.startsWith(key) && (key.equals("m") || key.equals("exp") || key.equals("ur"))) {
                    entry.getValue().accept(player, cmd);
                    return true;
                }
            }
        }

        if (cmd.equalsIgnoreCase("clearbag") || cmd.equalsIgnoreCase("clear bag")) {
            for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
                player.inventory.itemsBag.set(i, ItemService.gI().createItemNull());
            }
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Đã dọn sạch hành trang!");
            return true;
        }

        if (text.startsWith("clear ")) {
            try {
                int itemId = Integer.parseInt(text.replace("clear ", "").trim());
                int count = 0;
                for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
                    Item item = player.inventory.itemsBag.get(i);
                    if (item != null && item.isNotNullItem() && item.template.id == itemId) {
                        player.inventory.itemsBag.set(i, ItemService.gI().createItemNull());
                        count++;
                    }
                }
                if (count > 0) {
                    InventoryService.gI().sendItemBags(player);
                    Service.gI().sendThongBao(player, "Đã xóa vật phẩm ID " + itemId + " khỏi hành trang!");
                } else {
                    Service.gI().sendThongBao(player, "Không có vật phẩm ID " + itemId + " trong hành trang!");
                }
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Sai cú pháp! Dùng: clear <itemId>");
            }
            return true;
        }

        if (text.startsWith("ten con la ")) {
            PetService.gI().changeNamePet(player, text.replace("ten con la ", ""));
        }

        if (player.pet != null) {
            switch (text) {
                case "di theo", "follow" ->
                    player.pet.changeStatus(Pet.FOLLOW);
                case "bao ve", "protect" ->
                    player.pet.changeStatus(Pet.PROTECT);
                case "tan cong", "attack" ->
                    player.pet.changeStatus(Pet.ATTACK);
                case "ve nha", "go home" ->
                    player.pet.changeStatus(Pet.GOHOME);
                case "bien hinh" ->
                    player.pet.transform();
                case "sach tuyet ky" -> {
                    int typePet = player.pet.typePet;
                    if (typePet == 2 || typePet == 3 || typePet == 4) {
                        for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
                            Item item = player.inventory.itemsBag.get(i);
                            if (item != null && item.isNotNullItem() && item.template.type == 25) {
                                if (player.pet.nPoint != null && player.pet.nPoint.power >= 1500000) {
                                    Item old = InventoryService.gI().putItemBody(player.pet, item);
                                    player.inventory.itemsBag.set(i, old);
                                    InventoryService.gI().sendItemBags(player);
                                    InventoryService.gI().sendItemBody(player);
                                    Service.gI().Send_Caitrang(player.pet);
                                    Service.gI().Send_Caitrang(player);
                                    Service.gI().sendThongBao(player, "Đã dùng " + item.template.name + " cho đệ tử");
                                } else {
                                    Service.gI().sendThongBaoOK(player, "Đệ tử cần đạt 1tr5 sức mạnh để trang bị.");
                                }
                                break;
                            }
                        }
                    } else {
                        Service.gI().sendThongBaoOK(player,
                                "Chỉ đệ tử (Goku vô cực, Kid Beerus, Jiren) mới có thể dùng sách tuyệt kỹ.");
                    }
                }
            }
        }
        return false;
    }
}
