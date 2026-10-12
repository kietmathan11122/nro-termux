package nro.models.server;

import nro.models.radar.OptionCard;
import nro.models.services.RadarService;
import nro.models.radar.RadarCard;
import nro.models.data.LocalManager;
import nro.models.consts.ConstPlayer;
import nro.models.consts.ConstMap;
import nro.models.consts.ConstNpc;
import nro.models.data.DataGame;
import nro.models.database.ShopDAO;
import nro.models.player_system.Template.*;
import nro.models.clan.Clan;
import nro.models.clan.ClanMember;
import static nro.models.data.DataGame.MAP_MOUNT_NUM;
import nro.models.player_system.GiftCode;
import nro.models.managers.GiftCodeManager;
import nro.models.intrinsic.Intrinsic;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.map.WayPoint;
import nro.models.npc.Npc;
import nro.models.npc.NpcFactory;
import nro.models.shop.Shop;
import nro.models.skill.NClass;
import nro.models.skill.Skill;
import nro.models.task.SideTaskTemplate;
import nro.models.task.SubTaskMain;
import nro.models.task.TaskMain;
import nro.models.map.service.MapService;
import nro.models.utils.Logger;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import nro.models.map.Zone;
import nro.models.matches.TOP;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import nro.models.npc.NonInteractiveNPC;
import nro.models.player.Player;
import nro.models.player_badges.BagesTemplate;
import nro.models.shop_ky_gui.ConsignItem;
import nro.models.shop_ky_gui.ConsignShopManager;
import nro.models.task.BadgesTaskTemplate;
import nro.models.task.ClanTaskTemplate;
import nro.models.utils.FileIO;
import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 *
 */
public final class Manager {

    private static Manager instance;
    public static long timeRealTop = 0;
    public static byte SERVER = 1;
    public static byte SECOND_WAIT_LOGIN = 5;
    public static int MAX_PER_IP = 1000;
    public static int MAX_PLAYER = 2000;
    public static double RATE_EXP_SERVER = 1;
    public static double RATE_DROP_SERVER = 5;
    public static final long[] POWER_LIMITS = {
        17999999999L, 29999999999L, 39999999999L, 49999999999L, 59999999999L,
        70010000000L, 80010000000L, 90010000000L, 100010000000L, 110010000000L,
        120000000000L, 130000000000L, 140000000000L, 150000000000L
    };
    public static final int[] HP_MP_LIMITS = {221980, 300000, 350000, 400000, 425000, 450000, 475000, 500000, 525000, 550000, 570000, 600000, 625000, 650000};
    public static final int[] DAME_LIMITS = {11099, 13000, 15000, 16000, 17000, 18000, 19000, 20000, 22000, 24000, 26000, 28000, 30000, 32000};
    public static final short[] DEF_LIMITS = {550, 600, 700, 800, 1000, 1200, 1400, 1500, 1600, 1800, 2000, 2200, 2400, 2600};
    public static final byte[] CRIT_LIMITS = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
    public static boolean LOCAL = false;
    public static boolean TEST = false;
    public static boolean DAO_AUTO_UPDATER = false;
    public static MapTemplate[] MAP_TEMPLATES;
    public static final List<nro.models.map.Map> MAPS = new ArrayList<>();
    private static final int MAP_UPDATE_WORKERS = Math.max(1,
            Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final ExecutorService executor = Executors.newFixedThreadPool(MAP_UPDATE_WORKERS);
    public static final List<ItemOptionTemplate> ITEM_OPTION_TEMPLATES = new ArrayList<>();
    public static final List<ArrHead2Frames> ARR_HEAD_2_FRAMES = new ArrayList<>();
    private static volatile byte[] PART_TYPES = new byte[0];
    public static final Map<String, Byte> IMAGES_BY_NAME = new HashMap<>();
    public static final List<ItemTemplate> ITEM_TEMPLATES = new ArrayList<>();
    public static final List<MobTemplate> MOB_TEMPLATES = new ArrayList<>();
    public static final List<NpcTemplate> NPC_TEMPLATES = new ArrayList<>();
    public static final List<TaskMain> TASKS = new ArrayList<>();
    public static final List<SideTaskTemplate> SIDE_TASKS_TEMPLATE = new ArrayList<>();
    public static final List<ClanTaskTemplate> CLAN_TASKS_TEMPLATE = new ArrayList<>();
    public static final List<AchievementTemplate> ACHIEVEMENT_TEMPLATE = new ArrayList<>();
    public static final List<Intrinsic> INTRINSICS = new ArrayList<>();
    public static final List<Intrinsic> INTRINSIC_TD = new ArrayList<>();
    public static final List<Intrinsic> INTRINSIC_NM = new ArrayList<>();
    public static final List<Intrinsic> INTRINSIC_XD = new ArrayList<>();
    public static final List<HeadAvatar> HEAD_AVATARS = new ArrayList<>();
    public static final List<BgItem> BG_ITEMS = new ArrayList<>();
    public static final List<FlagBag> FLAGS_BAGS = new ArrayList<>();
    public static final List<NClass> NCLASS = new ArrayList<>();
    public static final List<Npc> NPCS = new ArrayList<>();
    public static List<Shop> SHOPS = new ArrayList<>();
    public static final List<Clan> CLANS = new ArrayList<>();
    public static final List<String> NOTIFY = new ArrayList<>();
    public static final List<BadgesTaskTemplate> TASKS_BADGES_TEMPLATE = new ArrayList<>();
    public static final List<BagesTemplate> BAGES_TEMPLATES = new ArrayList<>();
    public static final short[][] trangBiKichHoat = { { 0, 6, 21, 27 }, { 1, 7, 22, 28 }, { 2, 8, 23, 29 } };
    public static List<TOP> Topsukien;
    public static List<TOP> Topsukien1;
    public static List<TOP> Topsukien2 = new ArrayList<>();
    public static List<TOP> Topwhis;
    public static List<TOP> Topmaydam;
    public static final String queryTopmaydam = "SELECT id, point_maydam, total_damage_maydam FROM player ORDER BY point_maydam DESC LIMIT 100";
    public static final String queryTopsukien1 = "SELECT id, point_sukien1 FROM player ORDER BY point_sukien1 DESC LIMIT 100";
    public static final String queryTopsukien2 = "SELECT id, point_sukien2 FROM player ORDER BY point_sukien2 DESC LIMIT 100";
    public static final String queryTopwhis = "SELECT id, thachdauwhis FROM player ORDER BY thachdauwhis DESC LIMIT 100";
    public static final String queryTopsukien = "SELECT id, point_sukien FROM player ORDER BY point_sukien DESC LIMIT 100";
    public static boolean isTopMaydamChanged = false;
    public static boolean isTopSukienChanged = false;
    public static boolean isTopSukien1Changed = false;
    public static boolean isTopSukien2Changed = false;
    public static boolean isTopWhisChanged = false;

    public static Manager gI() {
        if (instance == null) {
            instance = new Manager();
        }
        return instance;
    }

    public static boolean hasNewTopScores() {
        return isTopMaydamChanged || isTopSukien2Changed || isTopSukienChanged || isTopSukien1Changed
                || isTopWhisChanged;
    }

    public static void resetTopFlags() {
        isTopMaydamChanged = false;
        isTopSukienChanged = false;
        isTopSukien1Changed = false;
        isTopSukien2Changed = false;
        isTopWhisChanged = false;
    }

    public class MapBgDataManager {

        private static final Map<Integer, byte[]> cacheBgItem = new ConcurrentHashMap<>();

        public static byte[] getBgItem(int mapId) {
            return cacheBgItem.computeIfAbsent(mapId, id -> {
                try {
                    return FileIO.readFile("data/map/item_bg_map_data/" + id);
                } catch (Exception e) {
                    return null;
                }
            });
        }
    }

    public static boolean isExactly11AM() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        return hour == 8 && minute == 13;
    }

    private Manager() {
        try {
            loadProperties();

        } catch (IOException ex) {
            Logger.logException(Manager.class, ex, "Lỗi load properites");
            System.exit(0);
        }

        this.loadDatabase();
        loadPowerLimits();
        NpcFactory.createNpcConMeo();
        NpcFactory.createNpcRongThieng();
        this.initMap();
    }

    private void initMap() {
        int[][] tileTyleTop = readTileIndexTileType(ConstMap.TILE_TOP);
        for (MapTemplate mapTemp : MAP_TEMPLATES) {
            int[][] tileMap = readTileMap(mapTemp.id, mapTemp.tileId);
            int tileIndex = mapTemp.tileId - 1;
            if (tileIndex < 0) tileIndex = 0;
            if (tileIndex >= tileTyleTop.length) tileIndex = tileTyleTop.length - 1;
            int[] tileTop = tileTyleTop[tileIndex];
            if (tileTop == null) {
                tileTop = new int[200];
                for (int j = 0; j < tileTop.length; j++) {
                    tileTop[j] = j + 1;
                }
            }
            nro.models.map.Map map = new nro.models.map.Map(
                    mapTemp.id, mapTemp.name, mapTemp.planetId, mapTemp.tileId,
                    mapTemp.bgId, mapTemp.bgType, mapTemp.type, tileMap, tileTop,
                    mapTemp.zones, mapTemp.maxPlayerPerZone, mapTemp.wayPoints);
            MAPS.add(map);
            map.initMob(mapTemp.mobTemp, mapTemp.mobLevel, mapTemp.mobHp, mapTemp.mobX, mapTemp.mobY);
            map.initNpc(mapTemp.npcId, mapTemp.npcX, mapTemp.npcY);
        }
        new NonInteractiveNPC().initNonInteractiveNPC();

        // Chia zone một lần khi khởi động. Bản cũ tạo lại hàng trăm danh sách
        // và Callable mỗi giây, gây nhiều rác GC và context switch trên VPS.
        List<List<Zone>> zoneGroups = new ArrayList<>(MAP_UPDATE_WORKERS);
        for (int i = 0; i < MAP_UPDATE_WORKERS; i++) {
            zoneGroups.add(new ArrayList<>());
        }
        int zoneIndex = 0;
        for (nro.models.map.Map map : MAPS) {
            for (Zone zone : map.zones) {
                zoneGroups.get(zoneIndex++ % MAP_UPDATE_WORKERS).add(zone);
            }
        }
        List<Callable<Void>> mapUpdateTasks = new ArrayList<>(MAP_UPDATE_WORKERS);
        for (List<Zone> group : zoneGroups) {
            mapUpdateTasks.add(() -> {
                for (Zone zone : group) {
                    try {
                        zone.updateOptimized();
                    } catch (Exception e) {
                        Logger.logException(Manager.class, e,
                                "Lỗi update map " + zone.map.mapId + " khu " + zone.zoneId);
                    }
                }
                return null;
            });
        }

        scheduler.scheduleAtFixedRate(() -> {
            try {
                executor.invokeAll(mapUpdateTasks);
            } catch (Exception e) {
                Logger.logException(Manager.class, e, "Lỗi update maps");
            }
        }, 0, 1, TimeUnit.SECONDS);

        Logger.success(Logger.RED + "Update maps started with " + MAP_UPDATE_WORKERS + " worker(s)!\n");
    }

    public static void loadPart() {
        JSONValue jv = new JSONValue();
        JSONArray dataArray = null;
        JSONObject dataObject = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try (Connection con = LocalManager.getConnection();) {
            // Part mappings are managed in SQL; do not overwrite edited event pets on startup.
            // Find max ID
            ps = con.prepareStatement("select max(id) from part");
            rs = ps.executeQuery();
            int maxId = 0;
            if (rs.next()) {
                maxId = rs.getInt(1);
            }
            rs.close();
            ps.close();

            // load part
            ps = con.prepareStatement("select * from part");
            rs = ps.executeQuery();
            Part[] parts = new Part[maxId + 1];
            while (rs.next()) {
                Part part = new Part();
                part.id = rs.getInt("id");
                part.type = rs.getByte("type");
                String rawData = rs.getString("data");
                if (rawData != null && !rawData.trim().isEmpty()) {
                    dataArray = (JSONArray) jv.parse(rawData.replaceAll("\\\"", ""));
                    if (dataArray != null) {
                        for (int j = 0; j < dataArray.size(); j++) {
                            JSONArray pd = (JSONArray) jv.parse(String.valueOf(dataArray.get(j)));
                            part.partDetails.add(new PartDetail(Integer.parseInt(String.valueOf(pd.get(0))),
                                    Byte.parseByte(String.valueOf(pd.get(1))),
                                    Byte.parseByte(String.valueOf(pd.get(2)))));
                            pd.clear();
                        }
                        dataArray.clear();
                    }
                }
                if (part.id >= 0 && part.id <= maxId) {
                    parts[part.id] = part;
                }
            }
            updatePartTypes(parts);
            rs.close();
            ps.close();

            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream("data/update_data/part"))) {
                dos.writeShort(parts.length);
                for (int i = 0; i < parts.length; i++) {
                    Part part = parts[i];
                    if (part == null) {
                        part = new Part();
                        part.id = i;
                        part.type = 0;
                    }
                    dos.writeByte(part.type);
                    int expectedCount = (part.type == 0) ? 3 : ((part.type == 1) ? 17 : 14);
                    for (int j = 0; j < expectedCount; j++) {
                        if (j < part.partDetails.size()) {
                            PartDetail partDetail = part.partDetails.get(j);
                            dos.writeShort(partDetail.iconId);
                            dos.writeByte(partDetail.dx);
                            dos.writeByte(partDetail.dy);
                        } else {
                            dos.writeShort(0);
                            dos.writeByte(0);
                            dos.writeByte(0);
                        }
                    }
                }
                dos.flush();
            }
        } catch (Exception e) {
            System.err.print("\nError at 299\n");
            e.printStackTrace();
        }
    }

    public static synchronized void loadPowerLimits() {
        try (Connection con = LocalManager.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT id, power, hp, mp, damage, defense, critical FROM power_limit ORDER BY id ASC");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                if (id < 0 || id >= POWER_LIMITS.length) {
                    continue;
                }
                POWER_LIMITS[id] = rs.getLong("power");
                HP_MP_LIMITS[id] = Math.max(rs.getInt("hp"), rs.getInt("mp"));
                DAME_LIMITS[id] = rs.getInt("damage");
                DEF_LIMITS[id] = (short) rs.getInt("defense");
                CRIT_LIMITS[id] = (byte) rs.getInt("critical");
            }
            Logger.success("Loaded power_limit config successfully!\n");
        } catch (Exception e) {
            Logger.logException(Manager.class, e, "Cannot load power_limit config");
        }
    }

    private void loadDatabase() {
        long st = System.currentTimeMillis();
        JSONArray dataArray;
        JSONObject dataObject;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try (Connection ConnectionDatabase = LocalManager.getConnection()) {
            // Find max ID
            ps = ConnectionDatabase.prepareStatement("select max(id) from part");
            rs = ps.executeQuery();
            int maxId = 0;
            if (rs.next()) {
                maxId = rs.getInt(1);
            }
            rs.close();
            ps.close();

            // load part
            ps = ConnectionDatabase.prepareStatement("select * from part");
            rs = ps.executeQuery();
            Part[] parts = new Part[maxId + 1];
            while (rs.next()) {
                Part part = new Part();
                part.id = rs.getInt("id");
                part.type = rs.getByte("type");
                String rawData = rs.getString("data");
                if (rawData != null && !rawData.trim().isEmpty()) {
                    dataArray = (JSONArray) JSONValue.parse(rawData.replaceAll("\\\"", ""));
                    if (dataArray != null) {
                        for (int j = 0; j < dataArray.size(); j++) {
                            JSONArray pd = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(j)));
                            part.partDetails.add(new PartDetail(Integer.parseInt(String.valueOf(pd.get(0))),
                                    Byte.parseByte(String.valueOf(pd.get(1))),
                                    Byte.parseByte(String.valueOf(pd.get(2)))));
                            pd.clear();
                        }
                        dataArray.clear();
                    }
                }
                if (part.id >= 0 && part.id <= maxId) {
                    parts[part.id] = part;
                }
            }
            updatePartTypes(parts);
            rs.close();
            ps.close();

            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream("data/update_data/part"))) {
                dos.writeShort(parts.length);
                for (int i = 0; i < parts.length; i++) {
                    Part part = parts[i];
                    if (part == null) {
                        part = new Part();
                        part.id = i;
                        part.type = 0;
                    }
                    dos.writeByte(part.type);
                    int expectedCount = (part.type == 0) ? 3 : ((part.type == 1) ? 17 : 14);
                    for (int j = 0; j < expectedCount; j++) {
                        if (j < part.partDetails.size()) {
                            PartDetail partDetail = part.partDetails.get(j);
                            dos.writeShort(partDetail.iconId);
                            dos.writeByte(partDetail.dx);
                            dos.writeByte(partDetail.dy);
                        } else {
                            dos.writeShort(0);
                            dos.writeByte(0);
                            dos.writeByte(0);
                        }
                    }
                }
                dos.flush();
            }
            Logger.success(Logger.PURPLE + "Successfully loaded part (" + parts.length + ")\n");

            // load bg item template
            // The client assigns template IDs from the order received, so keep
            // this deterministic and aligned with the IDs used by map data.
            ps = ConnectionDatabase.prepareStatement("select * from bg_item_template order by id asc");
            rs = ps.executeQuery();
            while (rs.next()) {
                BgItem bgItem = new BgItem();
                bgItem.id = rs.getInt("id");
                bgItem.layer = rs.getByte("layer");
                bgItem.dx = rs.getShort("dx");
                bgItem.dy = rs.getShort("dy");
                bgItem.idImage = rs.getShort("image_id");
                BG_ITEMS.add(bgItem);
            }
            Logger.success(Logger.RED + "Successfully loaded bg item template (" + BG_ITEMS.size() + ")\n");

            // load array head 2 frames
            ps = ConnectionDatabase.prepareStatement("select * from array_head_2_frames");
            rs = ps.executeQuery();
            while (rs.next()) {
                ArrHead2Frames arrHead2Frames = new ArrHead2Frames();
                dataArray = (JSONArray) JSONValue.parse(rs.getString("data"));
                for (int i = 0; i < dataArray.size(); i++) {
                    arrHead2Frames.frames.add(Integer.valueOf(dataArray.get(i).toString()));
                }
                ARR_HEAD_2_FRAMES.add(arrHead2Frames);
            }
            Logger.success(
                    Logger.PURPLE + "Successfully loaded arr head 2 frames (" + ARR_HEAD_2_FRAMES.size() + ")\n");

            // load clan
            ps = ConnectionDatabase.prepareStatement("select * from clan");
            rs = ps.executeQuery();
            while (rs.next()) {
                Clan clan = new Clan();
                clan.id = rs.getInt("id");
                clan.name = rs.getString("name");
                clan.name2 = rs.getString("name_2");
                clan.slogan = rs.getString("slogan");
                clan.imgId = rs.getByte("img_id");
                clan.powerPoint = rs.getLong("power_point");
                clan.maxMember = rs.getByte("max_member");
                clan.capsuleClan = rs.getInt("clan_point");
                clan.level = rs.getByte("level");
                if (clan.level < 1) {
                    clan.level = 1;
                }
                clan.createTime = (int) (rs.getTimestamp("create_time").getTime() / 1000);
                dataArray = (JSONArray) JSONValue.parse(rs.getString("members"));
                for (int i = 0; i < dataArray.size(); i++) {
                    dataObject = (JSONObject) JSONValue.parse(String.valueOf(dataArray.get(i)));
                    ClanMember cm = new ClanMember();
                    cm.clan = clan;
                    cm.id = Integer.parseInt(String.valueOf(dataObject.get("id")));
                    cm.name = String.valueOf(dataObject.get("name"));
                    cm.head = Short.parseShort(String.valueOf(dataObject.get("head")));
                    cm.body = Short.parseShort(String.valueOf(dataObject.get("body")));
                    cm.leg = Short.parseShort(String.valueOf(dataObject.get("leg")));
                    cm.role = Byte.parseByte(String.valueOf(dataObject.get("role")));
                    cm.donate = Integer.parseInt(String.valueOf(dataObject.get("donate")));
                    cm.receiveDonate = Integer.parseInt(String.valueOf(dataObject.get("receive_donate")));
                    cm.memberPoint = Integer.parseInt(String.valueOf(dataObject.get("member_point")));
                    cm.clanPoint = Integer.parseInt(String.valueOf(dataObject.get("clan_point")));
                    cm.joinTime = Integer.parseInt(String.valueOf(dataObject.get("join_time")));
                    cm.timeAskPea = Long.parseLong(String.valueOf(dataObject.get("ask_pea_time")));
                    try {
                        cm.powerPoint = Long.parseLong(String.valueOf(dataObject.get("power")));
                    } catch (NumberFormatException e) {
                    }
                    clan.addClanMember(cm);
                }
                // dataArray = (JSONArray) JSONValue.parse(rs.getString("thanhTichBDKB"));
                // if (!dataArray.isEmpty()) {
                // clan.levelDoneBanDoKhoBau =
                // Integer.parseInt(String.valueOf(dataArray.get(0)));
                // clan.thoiGianHoanThanhBDKB =
                // Long.parseLong(String.valueOf(dataArray.get(1)));
                // }
                dataArray.clear();
                CLANS.add(clan);
            }

            ps = ConnectionDatabase.prepareStatement("select id from clan order by id desc limit 1");
            rs = ps.executeQuery();
            if (rs.first()) {
                Clan.NEXT_ID = rs.getInt("id") + 1;
            }

            Logger.success(Logger.RED + "Successfully loaded clan (" + CLANS.size() + "), clan next id: " + Clan.NEXT_ID
                    + "\n");

            // load skill
            ps = ConnectionDatabase.prepareStatement("select * from skill_template order by nclass_id, slot");
            rs = ps.executeQuery();
            byte nClassId = -1;
            NClass nClass = null;
            while (rs.next()) {
                byte id = rs.getByte("nclass_id");
                if (id != nClassId) {
                    nClassId = id;
                    nClass = new NClass();
                    nClass.name = id == ConstPlayer.TRAI_DAT ? "Trái Đất" : id == ConstPlayer.NAMEC ? "Namếc" : "Xayda";
                    nClass.classId = nClassId;
                    NCLASS.add(nClass);
                }
                SkillTemplate skillTemplate = new SkillTemplate();
                skillTemplate.classId = nClassId;
                skillTemplate.id = rs.getByte("id");
                skillTemplate.name = rs.getString("name");
                skillTemplate.maxPoint = rs.getByte("max_point");
                skillTemplate.manaUseType = rs.getByte("mana_use_type");
                skillTemplate.type = rs.getByte("type");
                skillTemplate.iconId = rs.getShort("icon_id");
                skillTemplate.damInfo = rs.getString("dam_info");
                nClass.skillTemplatess.add(skillTemplate);

                dataArray = (JSONArray) JSONValue.parse(
                        rs.getString("skills")
                                .replaceAll("\\[\"", "[")
                                .replaceAll("\"\\[", "[")
                                .replaceAll("\"\\]", "]")
                                .replaceAll("\\]\"", "]")
                                .replaceAll("\\}\",\"\\{", "},{"));
                if (dataArray != null) {
                    for (int j = 0; j < dataArray.size(); j++) {
                        JSONObject dts = (JSONObject) JSONValue.parse(String.valueOf(dataArray.get(j)));
                        Skill skill = new Skill();
                        skill.template = skillTemplate;
                        skill.skillId = Short.parseShort(String.valueOf(dts.get("id")));
                        skill.point = Byte.parseByte(String.valueOf(dts.get("point")));
                        skill.powRequire = Long.parseLong(String.valueOf(dts.get("power_require")));
                        skill.manaUse = Integer.parseInt(String.valueOf(dts.get("mana_use")));
                        skill.coolDown = Integer.parseInt(String.valueOf(dts.get("cool_down")));
                        skill.dx = Integer.parseInt(String.valueOf(dts.get("dx")));
                        skill.dy = Integer.parseInt(String.valueOf(dts.get("dy")));
                        skill.maxFight = Integer.parseInt(String.valueOf(dts.get("max_fight")));
                        skill.damage = Short.parseShort(String.valueOf(dts.get("damage")));
                        skill.price = Short.parseShort(String.valueOf(dts.get("price")));
                        skill.moreInfo = String.valueOf(dts.get("info"));
                        skillTemplate.skillss.add(skill);
                    }
                } else {
                    System.err.println("CRITICAL: Failed to parse skills for skill template: " + skillTemplate.name + " (ID: " + skillTemplate.id + ")");
                }
            }
			// Loại bỏ hoàn toàn các slot class 3/4 còn sót từ dữ liệu cũ.
			while (NCLASS.size() > 3) {
				NCLASS.remove(NCLASS.size() - 1);
			}

            // Giữ lại tuyệt kỹ gốc của từng hành tinh: Super Kame (24),
            // Liên Hoàn Chưởng (25), Ma Phong Ba (26). Chỉ dọn nhóm mở rộng
            // cũ từ 27 trở đi để ba chiêu hành tinh mới không bị trùng khi nạp lại.
            for (NClass planetClass : NCLASS) {
                planetClass.skillTemplatess.removeIf(skillTemplate ->
                        skillTemplate.id > Skill.MA_PHONG_BA
                        && skillTemplate.id <= Skill.MOC_DANG);
            }

            // Mỗi hành tinh giữ bộ skill gốc, tuyệt kỹ riêng và thêm một skill nâng cấp.
            NCLASS.get(ConstPlayer.TRAI_DAT).skillTemplatess.add(createPlanetUpgradeSkill(
                    Skill.QUA_CAU_HY_VONG, ConstPlayer.TRAI_DAT, Skill.SUPER_KAME,
                    "Mưa Thiên Thạch", "Gọi mưa thiên thạch, mạnh hơn Super Kame 30%, hồi chiêu lâu hơn 30%", 130, 130, 30600));
            NCLASS.get(ConstPlayer.NAMEC).skillTemplatess.add(createPlanetUpgradeSkill(
                    Skill.CHUONG_XUYEN_PHA, ConstPlayer.NAMEC, Skill.MAKANKOSAPPO,
                    "Chưởng Xuyên Phá", "Phóng hắc khí xuyên mục tiêu, mạnh hơn Chưởng Laze 65%, hồi chiêu lâu hơn 30%; sát thương dựa trên dame nhân vật", 165, 130, 30601));
            NCLASS.get(ConstPlayer.XAYDA).skillTemplatess.add(createPlanetUpgradeSkill(
                    Skill.NO_LOI_SINH_HOC, ConstPlayer.XAYDA, Skill.TU_SAT,
                    "Xên Bùng Nổ", "Bùng nổ diện rộng, mạnh hơn Tự Sát 50%; người dùng sẽ chết sau khi nổ", 150, 140, 30602));

            NCLASS.get(ConstPlayer.TRAI_DAT).skillTemplatess.add(createPlanetTechnique(
                    Skill.THIEN_LONG_QUYEN, ConstPlayer.TRAI_DAT, "Thiên Long Quyền",
                    "Lao đấm gây choáng và chảy máu 5 giây theo HP tối đa; rồng xuyên mục tiêu rồi nổ; PvE", 30614));
            NCLASS.get(ConstPlayer.NAMEC).skillTemplatess.add(createPlanetTechnique(
                    Skill.CUONG_PHONG, ConstPlayer.NAMEC, "Cuồng Phong",
                    "Lốc lướt xuyên quái và boss, gây sát thương và choáng; boss choáng ngắn", 30615));
            NCLASS.get(ConstPlayer.XAYDA).skillTemplatess.add(createPlanetTechnique(
                    Skill.MOC_DANG, ConstPlayer.XAYDA, "Mộc Đằng",
                    "Dây leo gai trói quái/boss và rút máu mỗi giây theo HP tối đa; PvE", 30616));

            nro.models.services.HeartsRebirth.installTemplates(NCLASS);
            Logger.success(Logger.PURPLE + "Successfully loaded skill (" + NCLASS.size() + ")\n");

            // load head avatar
            ps = ConnectionDatabase.prepareStatement("select * from head_avatar");
            rs = ps.executeQuery();
            while (rs.next()) {
                HeadAvatar headAvatar = new HeadAvatar(rs.getInt("head_id"), rs.getInt("avatar_id"));
                HEAD_AVATARS.add(headAvatar);
            }
            Logger.success(Logger.RED + "Successfully loaded head avatar (" + HEAD_AVATARS.size() + ")\n");

            // load flag bag
            ps = ConnectionDatabase.prepareStatement("select * from flag_bag");
            rs = ps.executeQuery();
            while (rs.next()) {
                FlagBag flagBag = new FlagBag();
                flagBag.id = rs.getInt("id");
                flagBag.name = rs.getString("name");
                flagBag.gold = rs.getInt("gold");
                flagBag.gem = rs.getInt("gem");
                flagBag.iconId = rs.getShort("icon_id");
                String[] iconData = rs.getString("icon_data").split(",");
                flagBag.iconEffect = new short[iconData.length];
                for (int j = 0; j < iconData.length; j++) {
                    flagBag.iconEffect[j] = Short.parseShort(iconData[j].trim());
                }
                FLAGS_BAGS.add(flagBag);
            }
            Logger.success(Logger.PURPLE + "Successfully loaded flag bag (" + FLAGS_BAGS.size() + ")\n");

            // load intrinsic
            ps = ConnectionDatabase.prepareStatement("select * from intrinsic");
            rs = ps.executeQuery();
            while (rs.next()) {
                Intrinsic intrinsic = new Intrinsic();
                intrinsic.id = rs.getByte("id");
                intrinsic.name = rs.getString("name");
                intrinsic.paramFrom1 = rs.getShort("param_from_1");
                intrinsic.paramTo1 = rs.getShort("param_to_1");
                intrinsic.paramFrom2 = rs.getShort("param_from_2");
                intrinsic.paramTo2 = rs.getShort("param_to_2");
                intrinsic.icon = rs.getShort("icon");
                intrinsic.gender = rs.getByte("gender");
                switch (intrinsic.gender) {
                    case ConstPlayer.TRAI_DAT ->
                        INTRINSIC_TD.add(intrinsic);
                    case ConstPlayer.NAMEC ->
                        INTRINSIC_NM.add(intrinsic);
                    case ConstPlayer.XAYDA ->
                        INTRINSIC_XD.add(intrinsic);
                    default -> {
                        INTRINSIC_TD.add(intrinsic);
                        INTRINSIC_NM.add(intrinsic);
                        INTRINSIC_XD.add(intrinsic);
                    }
                }
                INTRINSICS.add(intrinsic);
            }
            Logger.success(Logger.RED + "Successfully loaded intrinsic (" + INTRINSICS.size() + ")\n");

            // load task
            ps = ConnectionDatabase.prepareStatement("SELECT id, task_main_template.name, detail, "
                    + "task_sub_template.name AS 'sub_name', max_count, notify, npc_id, map "
                    + "FROM task_main_template JOIN task_sub_template ON task_main_template.id = "
                    + "task_sub_template.task_main_id ORDER BY task_main_template.id, task_sub_template.ducvupro");
            rs = ps.executeQuery();
            int taskId = -1;
            TaskMain task = null;
            while (rs.next()) {
                int id = rs.getInt("id");
                if (id != taskId) {
                    taskId = id;
                    task = new TaskMain();
                    task.id = taskId;
                    task.name = rs.getString("name");
                    task.detail = rs.getString("detail");
                    TASKS.add(task);
                }
                SubTaskMain subTask = new SubTaskMain();
                subTask.name = rs.getString("sub_name");
                subTask.maxCount = rs.getShort("max_count");
                subTask.notify = rs.getString("notify");
                subTask.npcId = rs.getByte("npc_id");
                subTask.mapId = rs.getShort("map");
                task.subTasks.add(subTask);
            }
            nro.models.services.TaskService.PANEL_RULES.clear();
            try (PreparedStatement rulesQuery = ConnectionDatabase.prepareStatement(
                    "SELECT s.task_main_id,s.ducvupro,r.event_type,r.target_id,r.map_id FROM task_sub_template s LEFT JOIN task_step_rules r ON r.step_id=s.ducvupro ORDER BY s.task_main_id,s.ducvupro");
                    ResultSet rules = rulesQuery.executeQuery()) {
                int previousTask = -1, stepIndex = 0;
                while (rules.next()) {
                    int mainId = rules.getInt("task_main_id");
                    if (mainId != previousTask) { previousTask = mainId; stepIndex = 0; }
                    if (rules.getString("event_type") != null) {
                        nro.models.services.TaskService.PANEL_RULES.put(mainId + ":" + stepIndex,
                            new String[]{rules.getString("event_type"), rules.getString("target_id"), rules.getString("map_id")});
                    }
                    stepIndex++;
                }
            } catch (SQLException ex) {
                System.err.println("Task panel rules unavailable: " + ex.getMessage());
            }
            if (TASKS.stream().noneMatch(template -> template.id == nro.models.task.MainQuest40.ID)) {
                TASKS.add(nro.models.task.MainQuest40.createTemplate());
            }
            Logger.success(Logger.PURPLE + "Successfully loaded task (" + TASKS.size() + ")\n");

            // load side task
            ps = ConnectionDatabase.prepareStatement("select * from side_task_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                SideTaskTemplate sideTask = new SideTaskTemplate();
                sideTask.id = rs.getInt("id");
                sideTask.name = rs.getString("name");
                String[] mc1 = rs.getString("max_count_lv1").split("-");
                String[] mc2 = rs.getString("max_count_lv2").split("-");
                String[] mc3 = rs.getString("max_count_lv3").split("-");
                String[] mc4 = rs.getString("max_count_lv4").split("-");
                String[] mc5 = rs.getString("max_count_lv5").split("-");
                sideTask.count[0][0] = Integer.parseInt(mc1[0]);
                sideTask.count[0][1] = Integer.parseInt(mc1[1]);
                sideTask.count[1][0] = Integer.parseInt(mc2[0]);
                sideTask.count[1][1] = Integer.parseInt(mc2[1]);
                sideTask.count[2][0] = Integer.parseInt(mc3[0]);
                sideTask.count[2][1] = Integer.parseInt(mc3[1]);
                sideTask.count[3][0] = Integer.parseInt(mc4[0]);
                sideTask.count[3][1] = Integer.parseInt(mc4[1]);
                sideTask.count[4][0] = Integer.parseInt(mc5[0]);
                sideTask.count[4][1] = Integer.parseInt(mc5[1]);
                SIDE_TASKS_TEMPLATE.add(sideTask);
            }
            Logger.success(Logger.RED + "Successfully loaded side task (" + SIDE_TASKS_TEMPLATE.size() + ")\n");

            // load task badges
            ps = ConnectionDatabase.prepareStatement("select * from task_badges_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                BadgesTaskTemplate badgesTaskTemplate = new BadgesTaskTemplate();
                badgesTaskTemplate.id = rs.getInt("id");
                badgesTaskTemplate.name = rs.getString("NAME");
                badgesTaskTemplate.count = rs.getInt("maxCount");
                badgesTaskTemplate.idbadgesReward = rs.getInt("idbadgesReward");
                TASKS_BADGES_TEMPLATE.add(badgesTaskTemplate);
            }
            Logger.success(Logger.PURPLE + "Successfully loaded task badges (" + TASKS_BADGES_TEMPLATE.size() + ")\n");

            // load clan task
            ps = ConnectionDatabase.prepareStatement("select * from clan_task_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                ClanTaskTemplate clanTask = new ClanTaskTemplate();
                clanTask.id = rs.getInt("id");
                clanTask.name = rs.getString("name");
                String[] mc1 = rs.getString("max_count_lv1").split("-");
                String[] mc2 = rs.getString("max_count_lv2").split("-");
                String[] mc3 = rs.getString("max_count_lv3").split("-");
                String[] mc4 = rs.getString("max_count_lv4").split("-");
                String[] mc5 = rs.getString("max_count_lv5").split("-");
                clanTask.count[0][0] = Integer.parseInt(mc1[0]);
                clanTask.count[0][1] = Integer.parseInt(mc1[1]);
                clanTask.count[1][0] = Integer.parseInt(mc2[0]);
                clanTask.count[1][1] = Integer.parseInt(mc2[1]);
                clanTask.count[2][0] = Integer.parseInt(mc3[0]);
                clanTask.count[2][1] = Integer.parseInt(mc3[1]);
                clanTask.count[3][0] = Integer.parseInt(mc4[0]);
                clanTask.count[3][1] = Integer.parseInt(mc4[1]);
                clanTask.count[4][0] = Integer.parseInt(mc5[0]);
                clanTask.count[4][1] = Integer.parseInt(mc5[1]);
                CLAN_TASKS_TEMPLATE.add(clanTask);
            }
            Logger.success(Logger.RED + "Successfully loaded clan task (" + CLAN_TASKS_TEMPLATE.size() + ")\n");

            // load achievement template
            ps = ConnectionDatabase.prepareStatement("select * from achievement_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                ACHIEVEMENT_TEMPLATE.add(new AchievementTemplate(rs.getString("info1"), rs.getString("info2"),
                        rs.getInt("money"), rs.getLong("max_count")));
            }
            Logger.success(Logger.PURPLE + "Successfully loaded achievement (" + ACHIEVEMENT_TEMPLATE.size() + ")\n");

            int batchSize = 750;
            int offset = 0;

            try {
                // Sửa dữ liệu sự kiện bị lưu sai bảng mã/type trong các bản DB cũ
                // trước khi nạp template vào bộ nhớ.
                try {
                    try (PreparedStatement repair = ConnectionDatabase.prepareStatement(
                            "UPDATE item_template SET type = 99, gender = 3, name = ?, description = ?, "
                            + "icon_id = ?, part = -1, is_up_to_up = 1 WHERE id = ?")) {
                        String[][] eventItems = {
                            {"Nhang Trầm", "Nhang dùng trong sự kiện Cô Hồn.", "31268", "2255"},
                            {"Cháo Trắng", "Cần 5 nhang và 20 cháo để gọi ma.", "31269", "2256"},
                            {"Hộp Ma Quỷ", "Mở ra cải trang ma quỷ, 20 triệu vàng, 50 ngọc hoặc Ngọc Rồng.", "31271", "2257"},
                            {"Hộp Tây Du", "Hộp thưởng khi hộ tống Đường Tăng.", "32729", "2258"}
                        };
                        for (String[] eventItem : eventItems) {
                            repair.setString(1, eventItem[0]);
                            repair.setString(2, eventItem[1]);
                            repair.setInt(3, Integer.parseInt(eventItem[2]));
                            repair.setInt(4, Integer.parseInt(eventItem[3]));
                            repair.addBatch();
                        }
                        repair.executeBatch();
                    }
                } catch (SQLException repairError) {
                    Logger.error("Cannot repair event item metadata; continuing to load templates: "
                            + repairError.getMessage());
                }
                while (true) {
                    ps = ConnectionDatabase.prepareStatement("SELECT * FROM item_template ORDER BY id ASC LIMIT ? OFFSET ?");
                    ps.setInt(1, batchSize);
                    ps.setInt(2, offset);
                    rs = ps.executeQuery();
                    if (!rs.next()) {
                        break;
                    }
                    do {
                        ItemTemplate itemTemp = new ItemTemplate();
                        itemTemp.id = rs.getShort("id");
                        if (itemTemp.id < 0) {
                            continue;
                        }
                        itemTemp.type = rs.getByte("type");
                        itemTemp.gender = rs.getByte("gender");
                        itemTemp.name = rs.getString("name");
                        if (itemTemp.name == null) {
                            itemTemp.name = "";
                        }
                        itemTemp.description = rs.getString("description");
                        if (itemTemp.description == null) {
                            itemTemp.description = "";
                        }
                        itemTemp.level = rs.getByte("level");
                        itemTemp.iconID = rs.getShort("icon_id");
                        itemTemp.part = rs.getShort("part");
                        itemTemp.isUpToUp = rs.getBoolean("is_up_to_up");
                        itemTemp.strRequire = rs.getInt("power_require");
                        itemTemp.gold = rs.getInt("gold");
                        itemTemp.gem = rs.getInt("gem");
                        itemTemp.head = rs.getInt("head");
                        itemTemp.body = rs.getInt("body");
                        itemTemp.leg = rs.getInt("leg");

                        while (ITEM_TEMPLATES.size() <= itemTemp.id) {
                            ItemTemplate dummy = new ItemTemplate();
                            dummy.id = (short) ITEM_TEMPLATES.size();
                            dummy.name = "";
                            dummy.description = "";
                            ITEM_TEMPLATES.add(dummy);
                        }
                        ITEM_TEMPLATES.set(itemTemp.id, itemTemp);
                    } while (rs.next());
                    offset += batchSize;
                }

                Logger.success(
                        Logger.RED + "Successfully loaded map item template (" + ITEM_TEMPLATES.size() + " items)\n");

                ensureItemTemplate(2255, "Nhang Trầm", "Nhang dùng trong sự kiện Cô Hồn.", 31268);
                ensureItemTemplate(2256, "Cháo Trắng", "Cần 5 nhang, 20 cháo gọi ma", 31269);
                ensureItemTemplate(2257, "Hộp Ma Quỷ",
                        "Mở ra cải trang ma quỷ, 20 triệu vàng, 50 ngọc hoặc Ngọc Rồng.", 31271);
                ensureItemTemplate(2258, "Hộp Tây Du",
                        "Mở ra vật phẩm Tây Du, vàng, ngọc hoặc Ngọc Rồng.", 32729);
                ensureItemTemplate(2278, "Mâm Cúng", "Dùng tại Vách núi Aru, Moori hoặc Kakarot trong sự kiện Cô Hồn để nhận Pet Cô Hồn.", 20857);
                ensureItemTemplate(2380, "Hộp Tinh Linh thường", "Mở ngẫu nhiên một bộ Tinh Linh bậc 1-5 gồm 5 món.", 24010);
                ensureItemTemplate(2381, "Hộp Tinh Linh cao cấp", "Mở ngẫu nhiên một bộ Tinh Linh bậc 6-10 gồm 5 món.", 24011);
                ensureItemTemplate(2382, "Đậu Tinh Linh", "Phục hồi đầy HP và KI cho Tinh Linh.", 24012);
                ensureItemTemplate(2383, "Bùa Tinh Linh x2", "Tinh Linh nhận x2 sức mạnh và tiềm năng trong 1 giờ.", 24013);
                ensureItemTemplate(2384, "Bùa Tinh Linh x3", "Tinh Linh nhận x3 sức mạnh và tiềm năng trong 1 giờ.", 24014);
                String[] periods = {"1 giờ", "1 ngày", "1 tháng"};
                for (int period = 0; period < periods.length; period++) {
                    for (int power = 2; power <= 3; power++) {
                        int id = 2450 + period * 2 + power - 2;
                        ensureItemTemplate(id, "Bùa Tinh Linh x" + power + " " + periods[period],
                                "Tăng x" + power + " sức mạnh và tiềm năng Tinh Linh trong " + periods[period] + ".", 24011 + power);
                        ITEM_TEMPLATES.get(id).type = 13;
                    }
                }
                for (int slot = 2; slot <= 4; slot++) {
                    int id = 2454 + slot;
                    ensureItemTemplate(id, "Ấn đổi kỹ năng " + slot + " Tinh Linh",
                            "Dùng để đổi ngẫu nhiên kỹ năng ô " + slot + " của Tinh Linh đã mở khóa.", 24013 + slot);
                }
                for (int tier = 0; tier < 10; tier++) {
                    String armorName = ITEM_TEMPLATES.get(2320 + tier * 4).name;
                    String setName = armorName != null && armorName.startsWith("Áo ")
                            ? armorName.substring(3) : "Tinh Linh bậc " + (tier + 1);
                    ensureSpiritGloveTemplate(2360 + tier, "Găng " + setName, 30190 + tier, tier + 1);
                }

                ensureVongCoTemplate(2305, "Vòng Cổ Rồng 1 Sao", "Vòng cổ Rồng 1 Sao tăng sức mạnh toàn diện cho nhân vật", 20955, 1);
                ensureVongCoTemplate(2306, "Vòng Cổ Rồng 2 Sao", "Vòng cổ Rồng 2 Sao tăng sức mạnh toàn diện cho nhân vật", 20956, 2);
                ensureVongCoTemplate(2307, "Vòng Cổ Rồng 3 Sao", "Vòng cổ Rồng 3 Sao tăng sức mạnh toàn diện cho nhân vật", 20957, 3);
                ensureVongCoTemplate(2308, "Vòng Cổ Rồng 4 Sao", "Vòng cổ Rồng 4 Sao tăng sức mạnh toàn diện cho nhân vật", 20958, 4);
                ensureVongCoTemplate(2309, "Vòng Cổ Rồng 5 Sao", "Vòng cổ Rồng 5 Sao tăng sức mạnh toàn diện cho nhân vật", 20959, 5);
                ensureVongCoTemplate(2310, "Vòng Cổ Rồng 6 Sao", "Vòng cổ Rồng 6 Sao tăng sức mạnh toàn diện cho nhân vật", 20960, 6);
                ensureVongCoTemplate(2311, "Vòng Cổ Rồng 7 Sao", "Vòng cổ Rồng 7 Sao tăng sức mạnh toàn diện cho nhân vật", 20961, 7);

            } catch (SQLException e) {
                Logger.error("Error loading item templates: " + e.getMessage());
            } finally {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (ps != null) {
                        ps.close();
                    }
                } catch (SQLException e) {
                    Logger.error("Error closing resources: " + e.getMessage());
                }
            }

            if (ITEM_TEMPLATES.isEmpty()) {
                throw new IllegalStateException("item_template was not loaded; map initialization was cancelled");
            }

            ensureItemTemplate(2439, "Bước chân Hoa hồng đỏ", "Hiệu ứng bước chân nở hoa hồng đỏ khi di chuyển (áp dụng cả đệ tử).", 2439);
            ensureItemTemplate(2440, "Bước chân Hoa hồng xanh", "Hiệu ứng bước chân nở hoa hồng xanh khi di chuyển (áp dụng cả đệ tử).", 2440);
            ensureItemTemplate(2441, "Bước chân Sóng nước", "Hiệu ứng bước chân sóng nước thanh khiết khi di chuyển.", 2441);
            ensureItemTemplate(2442, "Đá Hắc ám", "Ép ngẫu nhiên 1 dòng, tối đa 7 lần. Dùng tại Phòng thí nghiệm.", 30012);
            ensureItemTemplate(2443, "Đá Ánh sáng", "Xóa dòng do đá Hắc ám thêm, giữ chỉ số gốc. Dùng tại PTN.", 30013);

            // load item option template
            ps = ConnectionDatabase.prepareStatement("select id, name from item_option_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                ItemOptionTemplate optionTemp = new ItemOptionTemplate();
                optionTemp.id = rs.getInt("id");
                optionTemp.name = rs.getString("name");
                ITEM_OPTION_TEMPLATES.add(optionTemp);
            }
            Logger.success(Logger.PURPLE + "Successfully loaded map item option template ("
                    + ITEM_OPTION_TEMPLATES.size() + ")\n");

            // load shop
            SHOPS = ShopDAO.getShops(ConnectionDatabase);
            Logger.success(Logger.RED + "Successfully loaded shop (" + SHOPS.size() + ")\n");

            // load notify
            ps = ConnectionDatabase.prepareStatement("select * from notify order by id desc");
            rs = ps.executeQuery();
            while (rs.next()) {
                NOTIFY.add(rs.getString("name") + "<>" + rs.getString("text"));
            }
            Logger.success(Logger.PURPLE + "Successfully loaded notify (" + NOTIFY.size() + ")\n");

            // load image by name
            ps = ConnectionDatabase.prepareStatement("select name, n_frame from img_by_name");
            rs = ps.executeQuery();
            while (rs.next()) {
                IMAGES_BY_NAME.put(rs.getString("name"), rs.getByte("n_frame"));
            }
            Logger.success(Logger.RED + "Successfully loaded images by name (" + IMAGES_BY_NAME.size() + ")\n");

            // Load mount
            for (ItemTemplate item : ITEM_TEMPLATES) {
                if (item.type == 23 && getNFrameImageByName("mount_" + item.part + "_0") != 0) {
                    MAP_MOUNT_NUM.put(item.id, (short) (item.part + 30000));
                }
            }
            Logger.success(Logger.PURPLE + "Successfully loaded mount (" + MAP_MOUNT_NUM.size() + ")\n");

            // Load item ki gui
            ps = ConnectionDatabase.prepareStatement("SELECT * FROM shop_ky_gui");
            rs = ps.executeQuery();
            while (rs.next()) {
                int i = rs.getInt("id");
                int idPl = rs.getInt("player_id");
                byte tab = rs.getByte("tab");
                short itemId = rs.getShort("item_id");
                int gold = rs.getInt("gold");
                int gem = rs.getInt("gem");
                int quantity = rs.getInt("quantity");
                byte isUp = rs.getByte("isUpTop");
                boolean isBuy = rs.getByte("isBuy") == 1;
                List<Item.ItemOption> op = new ArrayList<>();
                JSONArray jsa2 = (JSONArray) JSONValue.parse(rs.getString("itemOption"));
                for (int j = 0; j < jsa2.size(); ++j) {
                    JSONObject jso2 = (JSONObject) jsa2.get(j);
                    int idOptions = Integer.parseInt(jso2.get("id").toString());
                    int param = Integer.parseInt(jso2.get("param").toString());
                    op.add(new Item.ItemOption(idOptions, param, Boolean.TRUE.equals(jso2.get("darkInfusion"))));
                }
                ConsignShopManager.gI().listItem
                        .add(new ConsignItem(i, itemId, idPl, tab, gold, gem, quantity, isUp, op, isBuy));
            }
            Logger.success(Logger.RED + "Successfully loaded Consign Item (" + ConsignShopManager.gI().listItem.size()
                    + ")\n");

            // load mob template
            ps = ConnectionDatabase.prepareStatement("select * from mob_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                MobTemplate mobTemp = new MobTemplate();
                mobTemp.id = rs.getInt("id");
                mobTemp.type = rs.getByte("type");
                mobTemp.name = rs.getString("name");
                mobTemp.hp = rs.getInt("hp");
                mobTemp.rangeMove = rs.getByte("range_move");
                mobTemp.speed = rs.getByte("speed");
                mobTemp.dartType = rs.getByte("dart_type");
                mobTemp.percentDame = rs.getByte("percent_dame");
                mobTemp.percentTiemNang = rs.getByte("percent_tiem_nang");
                while (MOB_TEMPLATES.size() <= mobTemp.id) {
                    MobTemplate placeholder = new MobTemplate();
                    placeholder.name = "";
                    MOB_TEMPLATES.add(placeholder);
                }
                MOB_TEMPLATES.set(mobTemp.id, mobTemp);
            }
            Logger.success(Logger.PURPLE + "Successfully loaded mob template (" + MOB_TEMPLATES.size() + ")\n");

            // load npc template
            ps = ConnectionDatabase.prepareStatement("select * from npc_template");
            rs = ps.executeQuery();
            while (rs.next()) {
                NpcTemplate npcTemp = new NpcTemplate();
                // Dùng int làm chỉ số; đọc thẳng byte khiến NPC mở rộng ở cuối bảng
                // có thể bị thiếu hoặc thành chỉ số âm khi ID vượt giới hạn byte có dấu.
                int npcTemplateId = rs.getInt("id");
                npcTemp.id = npcTemplateId;
                npcTemp.name = rs.getString("name");
                npcTemp.head = rs.getShort("head");
                npcTemp.body = rs.getShort("body");
                npcTemp.leg = rs.getShort("leg");
                npcTemp.avatar = rs.getInt("avatar");
                while (NPC_TEMPLATES.size() <= npcTemplateId) {
                    NpcTemplate t = new NpcTemplate();
                    t.name = "";
                    NPC_TEMPLATES.add(t);
                }
                NPC_TEMPLATES.set(npcTemplateId, npcTemp);
            }
            // Hai NPC nông trại phải luôn tồn tại cả khi bảng template cũ được cache
            // hoặc bản database chỉ tải đến ID 119. Client cũng nhận hai template này
            // trong gói updateMap nên mới có thể vẽ NPC 120/121.
            ensureNpcTemplate(120, "Cậu Bé", 3307, 3308, 3309, 17275);
            ensureNpcTemplate(121, "Trưởng Làng", 3285, 3286, 3287, 17279);
            ensureNpcTemplate(122, "Cô Hồn", 3288, 3289, 3290, 32767);
            Logger.success(Logger.RED + "Successfully loaded npc template (" + NPC_TEMPLATES.size() + ")\n");
            ps = ConnectionDatabase.prepareStatement("select * from data_badges");
            rs = ps.executeQuery();
            while (rs.next()) {
                BagesTemplate template = new BagesTemplate();
                template.id = rs.getInt("id");
                template.idEffect = rs.getInt("idEffect");
                template.idItem = rs.getInt("idItem");
                template.NAME = rs.getString("NAME");

                JSONArray option = (JSONArray) JSONValue.parse(rs.getString("Options"));
                ;
                if (option != null) {
                    for (int u = 0; u < option.size(); u++) {
                        JSONObject jsonobject = (JSONObject) option.get(u);
                        int optionId = Integer.parseInt(jsonobject.get("id").toString());
                        int param = Integer.parseInt(jsonobject.get("param").toString());
                        template.options.add(new Item.ItemOption(optionId, param));
                    }
                }
                BAGES_TEMPLATES.add(template);
            }
            Logger.success(Logger.PURPLE + "Successfully loaded badges template (" + BAGES_TEMPLATES.size() + ")\n");
            // load map template
            ps = ConnectionDatabase.prepareStatement("select count(id) from map_template");
            rs = ps.executeQuery();
            if (rs.first()) {
                int countRow = rs.getShort(1);
                MAP_TEMPLATES = new MapTemplate[countRow];
                ps = ConnectionDatabase.prepareStatement("select * from map_template");
                rs = ps.executeQuery();
                short i = 0;
                while (rs.next()) {
                    MapTemplate mapTemplate = new MapTemplate();
                    int mapId = rs.getInt("id");
                    String mapName = rs.getString("name");
                    mapTemplate.id = mapId;
                    mapTemplate.name = mapName;
                    mapTemplate.type = rs.getByte("type");
                    mapTemplate.planetId = rs.getByte("planet_id");
                    mapTemplate.bgType = rs.getByte("bg_type");
                    mapTemplate.tileId = rs.getByte("tile_id");
                    mapTemplate.bgId = rs.getByte("bg_id");
                    mapTemplate.zones = rs.getByte("zones");
                    mapTemplate.maxPlayerPerZone = rs.getByte("max_player");
                    // load waypoints
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("waypoints")
                            .replaceAll("\\[\"\\[", "[[")
                            .replaceAll("\\]\"\\]", "]]")
                            .replaceAll("\",\"", ","));
                    if (dataArray == null) {
                        dataArray = new JSONArray();
                    }
                    for (int j = 0; j < dataArray.size(); j++) {
                        WayPoint wp = new WayPoint();
                        JSONArray dtwp = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(j)));
                        wp.name = String.valueOf(dtwp.get(0));
                        wp.minX = Short.parseShort(String.valueOf(dtwp.get(1)));
                        wp.minY = Short.parseShort(String.valueOf(dtwp.get(2)));
                        wp.maxX = Short.parseShort(String.valueOf(dtwp.get(3)));
                        wp.maxY = Short.parseShort(String.valueOf(dtwp.get(4)));
                        wp.isEnter = Byte.parseByte(String.valueOf(dtwp.get(5))) == 1;
                        wp.isOffline = Byte.parseByte(String.valueOf(dtwp.get(6))) == 1;
                        wp.goMap = Short.parseShort(String.valueOf(dtwp.get(7)));
                        wp.goX = Short.parseShort(String.valueOf(dtwp.get(8)));
                        wp.goY = Short.parseShort(String.valueOf(dtwp.get(9)));
                        mapTemplate.wayPoints.add(wp);
                        dtwp.clear();
                    }
                    dataArray.clear();
                    // The database version of map 101 contains a duplicated
                    // doorway above the walkable floor. Build the Trunks intro
                    // exit exactly like the original maps 39/40/41 so walking
                    // to the right edge reliably enters map 95.
                    if (mapId == 101) {
                        mapTemplate.wayPoints.clear();
                        WayPoint trunksHome = new WayPoint();
                        trunksHome.name = "Nhà Trunks";
                        trunksHome.minX = 744;
                        trunksHome.minY = 360;
                        trunksHome.maxX = 768;
                        trunksHome.maxY = 384;
                        trunksHome.isEnter = false;
                        trunksHome.isOffline = true;
                        trunksHome.goMap = 95;
                        trunksHome.goX = 100;
                        trunksHome.goY = 336;
                        mapTemplate.wayPoints.add(trunksHome);
                    }
                    // Luồng tạo nhân vật Xên: bắt đầu ở map 134 rồi đi qua
                    // cửa bên phải giống các map tân thủ 39/40/41 để tới map 121.
                    if (mapId == 134) {
                        mapTemplate.wayPoints.clear();
                        WayPoint xenNextMap = new WayPoint();
                        xenNextMap.name = "Khu vực Xên nhí";
                        xenNextMap.minX = 744;
                        xenNextMap.minY = 360;
                        xenNextMap.maxX = 768;
                        xenNextMap.maxY = 384;
                        xenNextMap.isEnter = false;
                        xenNextMap.isOffline = true;
                        xenNextMap.goMap = 121;
                        xenNextMap.goX = 100;
                        xenNextMap.goY = 336;
                        mapTemplate.wayPoints.add(xenNextMap);
                    }
                    // Map 121 là nhà của Xên. Dữ liệu cũ không có cửa ra nên
                    // người chơi tới đây sẽ bị kẹt sau phần hướng dẫn.
                    if (mapId == 121) {
                        mapTemplate.type = ConstMap.MAP_OFFLINE;
                        mapTemplate.wayPoints.clear();
                        WayPoint xenVillage = new WayPoint();
                        xenVillage.name = "Làng Aru";
                        xenVillage.minX = 696;
                        xenVillage.minY = 264;
                        xenVillage.maxX = 744;
                        xenVillage.maxY = 312;
                        xenVillage.isEnter = true;
                        xenVillage.isOffline = true;
                        xenVillage.goMap = 0;
                        xenVillage.goX = 332;
                        xenVillage.goY = 432;
                        mapTemplate.wayPoints.add(xenVillage);
                    }
                    // load mobs
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("mobs").replaceAll("\\\"", ""));
                    if (dataArray == null) {
                        dataArray = new JSONArray();
                    }
                    mapTemplate.mobTemp = new int[dataArray.size()];
                    mapTemplate.mobLevel = new byte[dataArray.size()];
                    mapTemplate.mobHp = new int[dataArray.size()];
                    mapTemplate.mobX = new short[dataArray.size()];
                    mapTemplate.mobY = new short[dataArray.size()];
                    for (int j = 0; j < dataArray.size(); j++) {
                        JSONArray dtm = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(j)));
                        mapTemplate.mobTemp[j] = Integer.parseInt(String.valueOf(dtm.get(0)));
                        mapTemplate.mobLevel[j] = Byte.parseByte(String.valueOf(dtm.get(1)));
                        mapTemplate.mobHp[j] = Integer.parseInt(String.valueOf(dtm.get(2)));
                        mapTemplate.mobX[j] = Short.parseShort(String.valueOf(dtm.get(3)));
                        mapTemplate.mobY[j] = Short.parseShort(String.valueOf(dtm.get(4)));
                        dtm.clear();
                    }
                    dataArray.clear();
                    // load npcs
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("npcs").replaceAll("\\\"", ""));
                    if (dataArray == null) {
                        dataArray = new JSONArray();
                    }
                    mapTemplate.npcId = new byte[dataArray.size()];
                    mapTemplate.npcX = new short[dataArray.size()];
                    mapTemplate.npcY = new short[dataArray.size()];
                    for (int j = 0; j < dataArray.size(); j++) {
                        JSONArray dtn = (JSONArray) JSONValue.parse(String.valueOf(dataArray.get(j)));
                        mapTemplate.npcId[j] = Byte.parseByte(String.valueOf(dtn.get(0)));
                        mapTemplate.npcX[j] = Short.parseShort(String.valueOf(dtn.get(1)));
                        mapTemplate.npcY[j] = Short.parseShort(String.valueOf(dtn.get(2)));
                        dtn.clear();
                    }
                    dataArray.clear();
                    MAP_TEMPLATES[i++] = mapTemplate;
                }
                Logger.success(Logger.RED + "Successfully loaded map template (" + MAP_TEMPLATES.length + ")\n");
            }

            ps = ConnectionDatabase.prepareStatement("select * from radar");
            rs = ps.executeQuery();
            while (rs.next()) {
                RadarCard rd = new RadarCard();
                rd.Id = rs.getShort("id");
                rd.IconId = rs.getShort("iconId");
                rd.Rank = rs.getByte("rank");
                rd.Max = rs.getByte("max");
                rd.Type = rs.getByte("type");
                rd.Template = rs.getShort("mob_id");
                rd.Name = rs.getString("name");
                rd.Info = rs.getString("info");
                JSONArray arr = (JSONArray) JSONValue.parse(rs.getString("body"));
                for (int i = 0; i < arr.size(); i++) {
                    JSONObject ob = (JSONObject) arr.get(i);
                    if (ob != null) {
                        rd.Head = Short.parseShort(ob.get("head").toString());
                        rd.Body = Short.parseShort(ob.get("body").toString());
                        rd.Leg = Short.parseShort(ob.get("leg").toString());
                        rd.Bag = Short.parseShort(ob.get("bag").toString());
                    }
                }
                rd.Options.clear();
                arr = (JSONArray) JSONValue.parse(rs.getString("options"));
                for (int i = 0; i < arr.size(); i++) {
                    JSONObject ob = (JSONObject) arr.get(i);
                    if (ob != null) {
                        rd.Options.add(new OptionCard(Integer.parseInt(ob.get("id").toString()),
                                Short.parseShort(ob.get("param").toString()),
                                Byte.parseByte(ob.get("activeCard").toString())));
                    }
                }
                rd.Require = rs.getShort("require");
                rd.RequireLevel = rs.getShort("require_level");
                rd.AuraId = rs.getShort("aura_id");
                RadarService.gI().RADAR_TEMPLATE.add(rd);
            }
            Logger.success(Logger.PURPLE + "Successfully loaded radar template ("
                    + RadarService.gI().RADAR_TEMPLATE.size() + ")\n");

            // Load giftcode
            ps = ConnectionDatabase.prepareStatement("SELECT * FROM giftcode");
            rs = ps.executeQuery();
            while (rs.next()) {
                GiftCode giftcode = new GiftCode();
                giftcode.code = rs.getString("code");
                giftcode.id = rs.getInt("id");
                giftcode.countLeft = rs.getInt("count_left");
                giftcode.type = rs.getInt("type");
                if (giftcode.countLeft == -1) {
                    giftcode.countLeft = 999999999;
                }
                giftcode.datecreate = rs.getTimestamp("datecreate");
                giftcode.dateexpired = rs.getTimestamp("expired");
                JSONArray jar = (JSONArray) JSONValue.parse(rs.getString("detail"));
                if (jar != null) {
                    for (int i = 0; i < jar.size(); ++i) {
                        JSONObject jsonObj = (JSONObject) jar.get(i);

                        int id = Integer.parseInt(jsonObj.get("id").toString());
                        int quantity = Integer.parseInt(jsonObj.get("quantity").toString());

                        JSONArray option = (JSONArray) jsonObj.get("options");
                        ArrayList<ItemOption> optionList = new ArrayList<>();

                        if (option != null) {
                            for (int u = 0; u < option.size(); u++) {
                                JSONObject jsonobject = (JSONObject) option.get(u);
                                int optionId = Integer.parseInt(jsonobject.get("id").toString());
                                int param = Integer.parseInt(jsonobject.get("param").toString());
                                optionList.add(new Item.ItemOption(optionId, param));
                            }
                        }

                        giftcode.option.put(id, optionList);
                        giftcode.detail.put(id, quantity);
                    }
                }

                GiftCodeManager.gI().listGiftCode.add(giftcode);
            }
            Logger.success(
                    Logger.RED + "Successfully loaded giftcode (" + GiftCodeManager.gI().listGiftCode.size() + ")\n");

            Topsukien = realTop(queryTopsukien, ConnectionDatabase);
            Logger.success(Logger.PURPLE + "Successfully Top Su Kien (" + Topsukien.size() + ")\n");
            Topsukien1 = realTop(queryTopsukien1, ConnectionDatabase);
            Topwhis = realTop(queryTopwhis, ConnectionDatabase);
            Logger.success(Logger.RED + "Successfully top Thach Dau Whis (" + Topwhis.size() + ")\n");
            Topmaydam = realTop(queryTopmaydam, ConnectionDatabase);
            Manager.timeRealTop = System.currentTimeMillis();

        } catch (Exception e) {
            Logger.logException(Manager.class, e, "Database loading error");
            System.exit(0);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException e) {
            }
        }

        Logger.log(Logger.PURPLE, "Total database loading time: " + (System.currentTimeMillis() - st) + " (ms)\n");

    }

    private static void updatePartTypes(Part[] parts) {
        byte[] types = new byte[parts.length];
        Arrays.fill(types, (byte) -1);
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] != null) {
                types[i] = (byte) parts[i].type;
            }
        }
        PART_TYPES = types;
    }

    private static NClass createCustomNClass(int classId, String name, int[] skillTemplateIds) {
        NClass customClass = new NClass();
        customClass.classId = classId;
        customClass.name = name;
        for (int skillTemplateId : skillTemplateIds) {
            for (NClass sourceClass : NCLASS) {
                nro.models.player_system.Template.SkillTemplate template = sourceClass.getSkillTemplate(skillTemplateId);
                if (template != null) {
                    customClass.skillTemplatess.add(template);
                    break;
                }
            }
        }
        return customClass;
    }

    private static void installNClass(int classId, NClass nClass) {
        if (classId < NCLASS.size()) {
            NCLASS.set(classId, nClass);
        } else {
            NCLASS.add(nClass);
        }
    }

    private static void assignClassSkillIcons(NClass nClass, int firstIconId) {
        for (int i = 0; i < nClass.skillTemplatess.size(); i++) {
            nClass.skillTemplatess.get(i).iconId = (short) (firstIconId + i);
        }
    }

    private static nro.models.player_system.Template.SkillTemplate createSpineClassSkill(
            int templateId, int classId, String name, int type, int paintSourceId,
            String description, int baseCooldown, int range, int baseDamage) {
        nro.models.player_system.Template.SkillTemplate paintSource = null;
        for (NClass sourceClass : NCLASS) {
            paintSource = sourceClass.getSkillTemplate(paintSourceId);
            if (paintSource != null) break;
        }
        nro.models.player_system.Template.SkillTemplate template =
                new nro.models.player_system.Template.SkillTemplate();
        template.id = (byte) templateId;
        template.classId = (byte) classId;
        template.name = name;
        template.maxPoint = 7;
        template.manaUseType = 0;
        template.type = (byte) type;
        template.damInfo = description;
        long[] powers = {1L, 10_000L, 100_000L, 1_000_000L, 10_000_000L, 100_000_000L, 1_000_000_000L};
        for (int i = 0; i < template.maxPoint; i++) {
            Skill paintLevel = paintSource != null && !paintSource.skillss.isEmpty()
                    ? paintSource.skillss.get(Math.min(i, paintSource.skillss.size() - 1)) : null;
            Skill level = new Skill();
            level.template = template;
            // Mỗi cấp của 14 skill phải có ID toàn cục riêng. Nếu dùng lại paint
            // ID cũ, lệnh admin nâng skill sẽ cập nhật nhầm template trên client.
            // Client tự chọn paint nền an toàn và dựng hiệu ứng mới theo templateId.
            // Skill gốc của game đã dùng các ID đến ít nhất 515. Dải 400+
            // làm Skills.get() trên client trả nhầm template, nên chỉ chiêu đầu
            // tình cờ hoạt động. Dùng vùng riêng, vẫn nằm an toàn trong short.
            level.skillId = (short) (20_000 + (templateId - Skill.QUYEN_KICH_TUONG_LAI) * 7 + i);
            level.point = i + 1;
            level.powRequire = powers[i];
            level.manaUse = type == 3 ? 20 + i * 5 : 8 + i * 4;
            level.coolDown = Math.max(500, baseCooldown - i * Math.max(50, baseCooldown / 18));
            if (templateId == Skill.KHIEN_THOI_GIAN) {
                // Đá Liên Hoàn phải hồi đúng bằng Kaioken cùng cấp. Dữ liệu
                // Kaioken hiện tại là 500 ms; giữ 500 ms làm fallback để skill
                // không rơi về baseCooldown 8,5 giây nếu paint source bị thiếu.
                level.coolDown = 500;
            }
            level.dx = range;
            level.dy = range;
            level.maxFight = 1;
            int damage = baseDamage + i * 55;
            if (templateId == Skill.KHIEN_THOI_GIAN && paintLevel != null) {
                damage = Math.round(paintLevel.damage * 1.1f);
            }
            level.damage = (short) Math.min(Short.MAX_VALUE, damage);
            level.price = (short) (i == 0 ? 0 : Math.min(30_000, i * 4_000));
            level.moreInfo = name + " cấp " + (i + 1) + ": " + description;
            template.skillss.add(level);
        }
        return template;
    }

    private static nro.models.player_system.Template.SkillTemplate createPlanetTechnique(
            int id, int planet, String name, String description, int icon) {
        nro.models.player_system.Template.SkillTemplate template = createPlanetUpgradeSkill(
                id, planet, -1, name, description, 100, 100, icon);
        template.manaUseType = 1;
        for (Skill level : template.skillss) {
            level.manaUse = nro.models.services.PlanetTechniqueRules.manaPercent(id);
            level.coolDown = nro.models.services.PlanetTechniqueRules.cooldown(id, level.point);
            level.damage = (short) nro.models.services.PlanetTechniqueRules.damagePercent(id, level.point);
            level.dx = nro.models.services.PlanetTechniqueRules.range(id);
            level.dy = nro.models.services.PlanetTechniqueRules.radius(id);
            level.maxFight = 20;
            level.moreInfo = name + " cấp " + level.point + ": " + description;
            if (id == Skill.THIEN_LONG_QUYEN) {
                level.moreInfo += "; choáng " + (nro.models.services.PlanetTechniqueRules.punchStunMs(level.point) / 1000.0)
                        + "s, chảy máu " + (nro.models.services.PlanetTechniqueRules.bleedBasisPoints(level.point) / 100.0)
                        + "% HP tối đa/giây trong 5s";
            } else if (id == Skill.MOC_DANG) {
                level.moreInfo += "; trói " + nro.models.services.PlanetTechniqueRules.vineTicks(level.point)
                        + "s, mất " + level.point + "% HP tối đa/giây";
            }
        }
        return template;
    }

    private static nro.models.player_system.Template.SkillTemplate createPlanetUpgradeSkill(
            int templateId, int classId, int sourceTemplateId, String name, String description,
            int damagePercent, int cooldownPercent, int iconId) {
        nro.models.player_system.Template.SkillTemplate source = NCLASS.get(classId).getSkillTemplate(sourceTemplateId);
        nro.models.player_system.Template.SkillTemplate template = new nro.models.player_system.Template.SkillTemplate();
        template.id = (byte) templateId;
        template.classId = (byte) classId;
        template.name = name;
        template.maxPoint = 7;
        template.manaUseType = source != null ? source.manaUseType : 0;
        // Ba skill nâng cấp dùng cùng nhịp thi triển chậm của nhóm tuyệt kỹ
        // Super Kame / Liên Hoàn Chưởng / Ma Phong Ba.
        template.type = 4;
        // Skill nâng cấp có icon riêng trong client (SkillIcons/30600..30602),
        // không dùng lại icon của skill nguồn.
        template.iconId = (short) iconId;
        template.damInfo = description;
        for (int i = 0; i < template.maxPoint; i++) {
            Skill sourceLevel = source != null && i < source.skillss.size() ? source.skillss.get(i) : null;
            Skill level = sourceLevel != null ? new Skill(sourceLevel) : new Skill();
            level.template = template;
            level.skillId = (short) (22_000 + (templateId - Skill.QUA_CAU_HY_VONG) * 7 + i);
            level.point = i + 1;
            if (sourceLevel == null) {
                level.powRequire = i == 0 ? 1 : 10_000L * (long) Math.pow(10, i - 1);
                level.manaUse = 20 + i * 8;
                level.coolDown = 10_000;
                level.dx = 420;
                level.dy = 320;
                level.maxFight = 1;
                level.damage = (short) (200 + i * 55);
                level.price = (short) (i == 0 ? 0 : Math.min(30_000, i * 4_000));
            }
            level.damage = (short) Math.min(Short.MAX_VALUE,
                    Math.max(1, Math.round(level.damage * damagePercent / 100f)));
            level.coolDown = Math.max(500, Math.round(level.coolDown * cooldownPercent / 100f));
            level.moreInfo = name + " cấp " + (i + 1) + ": " + description
                    + ", hồi chiêu x" + (cooldownPercent / 100f);
            template.skillss.add(level);
        }
        return template;
    }

    /**
     * A standalone Trunks skill. Paint IDs 128..134 are included in the
     * client and use a different attack chain from the old punch paint 186.
     */
    private static nro.models.player_system.Template.SkillTemplate createTrunksSwordSkill() {
        nro.models.player_system.Template.SkillTemplate galick = null;
        for (NClass sourceClass : NCLASS) {
            galick = sourceClass.getSkillTemplate(Skill.GALICK);
            if (galick != null) {
                break;
            }
        }
        nro.models.player_system.Template.SkillTemplate template = new nro.models.player_system.Template.SkillTemplate();
        template.id = Skill.KIEM_KHI_TRUNKS;
        template.classId = ConstPlayer.TRUNKS_TUONG_LAI;
        template.name = "Liệt Không Kiếm";
        template.maxPoint = galick != null ? galick.maxPoint : 7;
        template.manaUseType = galick != null ? galick.manaUseType : 0;
        template.type = galick != null ? galick.type : 1;
        template.iconId = 31948;
        template.damInfo = galick != null ? galick.damInfo : "Vung kiếm tạo ra luồng năng lượng gây sát thương";

        long[] power = {1L, 1_500L, 15_000L, 150_000L, 1_500_000L, 15_000_000L, 150_000_000L};
        int[] damage = {180, 220, 270, 330, 400, 480, 580};
        for (int i = 0; i < template.maxPoint; i++) {
            Skill galickLevel = galick != null && i < galick.skillss.size() ? galick.skillss.get(i) : null;
            Skill skill = new Skill();
            skill.template = template;
            skill.skillId = (short) (128 + i);
            skill.point = i + 1;
            skill.powRequire = galickLevel != null ? galickLevel.powRequire : power[i];
            skill.manaUse = galickLevel != null ? galickLevel.manaUse : 10 + i * 5;
            skill.coolDown = galickLevel != null ? galickLevel.coolDown : 700;
            skill.dx = galickLevel != null ? galickLevel.dx : 100;
            skill.dy = galickLevel != null ? galickLevel.dy : 100;
            skill.maxFight = galickLevel != null ? galickLevel.maxFight : 1;
            skill.damage = galickLevel != null ? galickLevel.damage : (short) damage[i];
            skill.price = galickLevel != null ? galickLevel.price
                    : (short) (i == 0 ? 0 : Math.min(30_000, i * 5_000));
            skill.moreInfo = "Kiếm khí xé không cấp " + (i + 1);
            template.skillss.add(skill);
        }
        return template;
    }

    private static nro.models.player_system.Template.SkillTemplate createTrunksSkillFromSource(
            int sourceTemplateId, int newTemplateId, String name, int type, String damInfo) {
        nro.models.player_system.Template.SkillTemplate source = null;
        for (NClass sourceClass : NCLASS) {
            source = sourceClass.getSkillTemplate(sourceTemplateId);
            if (source != null) {
                break;
            }
        }
        nro.models.player_system.Template.SkillTemplate template = new nro.models.player_system.Template.SkillTemplate();
        template.id = (byte) newTemplateId;
        template.classId = ConstPlayer.TRUNKS_TUONG_LAI;
        template.name = name;
        template.maxPoint = source != null ? source.maxPoint : 7;
        template.manaUseType = source != null ? source.manaUseType : 0;
        template.type = (byte) type;
        template.iconId = source != null ? source.iconId : 31948;
        template.damInfo = damInfo;
        for (int i = 0; i < template.maxPoint; i++) {
            Skill sourceLevel = source != null && i < source.skillss.size() ? source.skillss.get(i) : null;
            Skill skill = sourceLevel != null ? new Skill(sourceLevel) : new Skill();
            skill.template = template;
            if (sourceLevel == null) {
                skill.skillId = (short) (210 + i);
                skill.point = i + 1;
                skill.powRequire = i == 0 ? 1 : 1_500L * (long) Math.pow(10, i - 1);
                skill.manaUse = 10 + i * 5;
                skill.coolDown = newTemplateId == Skill.CAPSULE_HEN_GIO ? 8_000 : 12_000;
                skill.dx = 300;
                skill.dy = 300;
                skill.maxFight = 1;
                skill.damage = (short) (200 + i * 50);
            }
            if (newTemplateId == Skill.CAPSULE_HEN_GIO) {
                skill.damage = (short) (150 + i * 40);
                skill.coolDown = Math.max(5_000, 8_000 - i * 500);
                skill.manaUse = 10 + i * 2;
                skill.dx = 350;
                skill.dy = 250;
            } else if (newTemplateId == Skill.DAI_KIEM_HY_VONG) {
                skill.damage = (short) (350 + i * 70);
                skill.coolDown = Math.max(8_000, 15_000 - i * 750);
                skill.manaUse = 40 + i * 10;
                skill.dx = 450;
                skill.dy = 300;
            } else if (newTemplateId == Skill.KAMEJOKO) {
                skill.manaUse = 30 + i * 10;
            }
            skill.moreInfo = name + " cấp " + (i + 1);
            template.skillss.add(skill);
        }
        return template;
    }

    private static nro.models.player_system.Template.SkillTemplate createXenSkillFromSource(
            int sourceTemplateId, String name, int type, String damInfo) {
        nro.models.player_system.Template.SkillTemplate source = null;
        for (NClass sourceClass : NCLASS) {
            source = sourceClass.getSkillTemplate(sourceTemplateId);
            if (source != null) {
                break;
            }
        }
        nro.models.player_system.Template.SkillTemplate template = new nro.models.player_system.Template.SkillTemplate();
        template.id = (byte) sourceTemplateId;
        template.classId = ConstPlayer.XEN_BO_HUNG;
        template.name = name;
        template.maxPoint = source != null ? source.maxPoint : 7;
        template.manaUseType = source != null ? source.manaUseType : 0;
        template.type = (byte) type;
        template.iconId = source != null ? source.iconId : 0;
        template.damInfo = damInfo;
        for (int i = 0; i < template.maxPoint; i++) {
            Skill sourceLevel = source != null && i < source.skillss.size() ? source.skillss.get(i) : null;
            Skill skill = sourceLevel != null ? new Skill(sourceLevel) : new Skill();
            skill.template = template;
            if (sourceLevel == null) {
                skill.skillId = (short) ((sourceTemplateId == Skill.GALICK ? 28 : 35) + i);
                skill.point = i + 1;
                skill.powRequire = i == 0 ? 1 : 1_500L * (long) Math.pow(10, i - 1);
                skill.manaUse = 10 + i * 5;
                skill.coolDown = 700;
                skill.dx = sourceTemplateId == Skill.GALICK ? 100 : 300;
                skill.dy = sourceTemplateId == Skill.GALICK ? 100 : 300;
                skill.maxFight = 1;
                skill.damage = (short) (180 + i * 50);
            }
            skill.moreInfo = name + " cấp " + (i + 1);
            template.skillss.add(skill);
        }
        return template;
    }

    private static nro.models.player_system.Template.SkillTemplate createCustomAttackSkill(
            int classId, int sourceTemplateId, int newTemplateId, String name,
            String damInfo, int baseCooldown, int baseDamage) {
        nro.models.player_system.Template.SkillTemplate source = null;
        for (NClass sourceClass : NCLASS) {
            source = sourceClass.getSkillTemplate(sourceTemplateId);
            if (source != null) {
                break;
            }
        }
        nro.models.player_system.Template.SkillTemplate template = new nro.models.player_system.Template.SkillTemplate();
        template.id = (byte) newTemplateId;
        template.classId = (byte) classId;
        template.name = name;
        template.maxPoint = source != null ? source.maxPoint : 7;
        template.manaUseType = source != null ? source.manaUseType : 0;
        template.type = 1;
        template.iconId = source != null ? source.iconId : 0;
        template.damInfo = damInfo;
        for (int i = 0; i < template.maxPoint; i++) {
            Skill sourceLevel = source != null && i < source.skillss.size() ? source.skillss.get(i) : null;
            Skill skill = sourceLevel != null ? new Skill(sourceLevel) : new Skill();
            skill.template = template;
            skill.skillId = sourceLevel != null ? sourceLevel.skillId : (short) (260 + i);
            skill.point = i + 1;
            skill.powRequire = sourceLevel != null ? sourceLevel.powRequire
                    : (i == 0 ? 1 : 1_500L * (long) Math.pow(10, i - 1));
            skill.manaUse = 25 + i * 8;
            skill.coolDown = Math.max(2_500, baseCooldown - i * 400);
            skill.dx = classId == ConstPlayer.TRUNKS_TUONG_LAI ? 380 : 420;
            skill.dy = 300;
            skill.maxFight = 1;
            skill.damage = (short) (baseDamage + i * 55);
            skill.moreInfo = name + " cấp " + (i + 1);
            template.skillss.add(skill);
        }
        return template;
    }

    private static nro.models.player_system.Template.SkillTemplate createXenAbsorbSkill() {
        nro.models.player_system.Template.SkillTemplate source = null;
        for (NClass sourceClass : NCLASS) {
            source = sourceClass.getSkillTemplate(Skill.BIEN_KHI);
            if (source != null) {
                break;
            }
        }
        nro.models.player_system.Template.SkillTemplate template = new nro.models.player_system.Template.SkillTemplate();
        template.id = Skill.BIEN_KHI;
        template.classId = ConstPlayer.XEN_BO_HUNG;
        template.name = "Hấp Thụ Tiến Hóa";
        template.maxPoint = 1;
        template.manaUseType = source != null ? source.manaUseType : 0;
        template.type = 3;
        template.iconId = source != null ? source.iconId : 0;
        template.damInfo = "Hấp thụ để tiến hóa ngoại hình, tăng sức đánh, HP, KI và chí mạng";
        for (int i = 0; i < template.maxPoint; i++) {
            Skill sourceLevel = source != null && i < source.skillss.size() ? source.skillss.get(i) : null;
            Skill skill = sourceLevel != null ? new Skill(sourceLevel) : new Skill();
            skill.template = template;
            if (sourceLevel == null) {
                skill.skillId = (short) (230 + i);
                skill.point = i + 1;
                skill.powRequire = i == 0 ? 1 : 150_000L * (long) Math.pow(10, i - 1);
                skill.manaUse = 20 + i * 10;
                skill.coolDown = 5_000;
                skill.dx = 0;
                skill.dy = 0;
                skill.maxFight = 1;
                skill.damage = 0;
            }
            skill.coolDown = 3_000;
            skill.moreInfo = "Chỉ có 1 cấp học. Mỗi lần dùng tiến hóa lần lượt 4 dạng Xên; "
                    + "mỗi dạng tăng thêm 20% sức đánh/HP/KI và 5% chí mạng";
            template.skillss.add(skill);
        }
        return template;
    }

    private static nro.models.player_system.Template.SkillTemplate createXenExplosionSkill() {
        nro.models.player_system.Template.SkillTemplate source = null;
        for (NClass sourceClass : NCLASS) {
            source = sourceClass.getSkillTemplate(Skill.TU_SAT);
            if (source != null) {
                break;
            }
        }
        nro.models.player_system.Template.SkillTemplate template = new nro.models.player_system.Template.SkillTemplate();
        template.id = Skill.TU_SAT;
        template.classId = ConstPlayer.XEN_BO_HUNG;
        template.name = "Tinh Cầu Tự Hủy";
        template.maxPoint = 7;
        template.manaUseType = source != null ? source.manaUseType : 0;
        template.type = source != null ? source.type : 3;
        template.iconId = source != null ? source.iconId : 0;
        template.damInfo = "Gồng năng lượng xanh rồi phát nổ diện rộng theo phần trăm HP";
        int[] hpDamagePercent = {50, 75, 100, 125, 150, 175, 200};
        for (int i = 0; i < template.maxPoint; i++) {
            Skill sourceLevel = source != null && i < source.skillss.size() ? source.skillss.get(i) : null;
            Skill skill = sourceLevel != null ? new Skill(sourceLevel) : new Skill();
            skill.template = template;
            if (sourceLevel == null) {
                skill.skillId = (short) (98 + i);
                skill.point = i + 1;
                skill.powRequire = i == 0 ? 1 : 150_000L * (long) Math.pow(10, i - 1);
                skill.manaUse = 10 + i * 5;
                skill.coolDown = 60_000;
                skill.dx = 300;
                skill.dy = 300;
                skill.maxFight = 1;
            }
            skill.damage = (short) hpDamagePercent[i];
            skill.moreInfo = "Bùng nổ cấp " + (i + 1) + ": gây " + hpDamagePercent[i]
                    + "% HP hiện tại, hồi chiêu như Tự Sát";
            template.skillss.add(skill);
        }
        return template;
    }

    public static boolean isValidPart(int partId, int expectedType) {
        byte[] types = PART_TYPES;
        return types.length == 0
                || (partId >= 0 && partId < types.length && types[partId] == expectedType);
    }

    public static List<TOP> realTop(String query, Connection con) {
        List<TOP> tops = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(query); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                TOP top = TOP.builder()
                        .id_player(rs.getInt("id"))
                        .build();

                if (query.equals(Manager.queryTopsukien)) {
                    int point = rs.getInt("point_sukien");
                    top.setInfo1(point + " điểm");
                    top.setInfo2(point + " điểm");

                } else if (query.equals(Manager.queryTopsukien1)) {
                    int point1 = rs.getInt("point_sukien1");
                    top.setInfo1(point1 + " điểm");
                    top.setInfo2(point1 + " điểm");

                } else if (query.equals(Manager.queryTopsukien2)) {
                    int point2 = rs.getInt("point_sukien2");
                    top.setInfo1(point2 + " điểm");
                    top.setInfo2(point2 + " điểm");

                } else if (query.equals(Manager.queryTopwhis)) {
                    int whis = rs.getInt("thachdauwhis");
                    top.setInfo1(whis + " Level");
                    top.setInfo2(whis + " Level");

                } else if (query.equals(Manager.queryTopmaydam)) {
                    int maydam = rs.getInt("point_maydam");
                    long totalDame = rs.getLong("total_damage_maydam");
                    top.setInfo1(maydam + " điểm");
                    top.setInfo2(Util.formatNumber(totalDame) + " sát thương");
                }

                tops.add(top);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (tops.isEmpty()) {
            System.out.println("No data found for the query: " + query);
        }

        return tops;
    }

    public void loadProperties() throws IOException {
        Properties properties = new Properties();
        java.io.File file = new java.io.File("Config.properties");
        System.out.println("Loading server configuration...");
        properties.load(new java.io.FileInputStream(file));
        Object value;
        if ((value = properties.get("server.sv")) != null) {
            SERVER = Byte.parseByte(String.valueOf(value));
        }
        if ((value = properties.get("server.name")) != null) {
            String name = String.valueOf(value);
            ServerManager.NAME = name.equals("Local") ? " Local" : name;
        }
        if ((value = properties.get("server.port")) != null) {
            ServerManager.PORT = Integer.parseInt(String.valueOf(value));
        }
        String linkServer = "";
        if ((value = properties.get("server.ip")) != null) {
            ServerManager.IP = String.valueOf(value);
            linkServer += ServerManager.NAME + ":" + ServerManager.IP + ":" + ServerManager.PORT + ":0,";
        }
        for (int i = 1; i <= 10; i++) {
            value = properties.get("server.sv" + i);
            if (value != null) {
                linkServer += String.valueOf(value) + ":0,";
            }
        }
        DataGame.LINK_IP_PORT = linkServer.substring(0, linkServer.length() - 1);
        if ((value = properties.get("server.waitlogin")) != null) {
            SECOND_WAIT_LOGIN = Byte.parseByte(String.valueOf(value));
        }
        if ((value = properties.get("server.maxperip")) != null) {
            MAX_PER_IP = Integer.parseInt(String.valueOf(value));
        }
        if ((value = properties.get("server.maxplayer")) != null) {
            MAX_PLAYER = Integer.parseInt(String.valueOf(value));
        }
        if ((value = properties.get("server.expserver")) != null) {
            RATE_EXP_SERVER = Double.parseDouble(String.valueOf(value));
        }
        if ((value = properties.get("server.dropserver")) != null) {
            RATE_DROP_SERVER = Double.parseDouble(String.valueOf(value));
        }
        if ((value = properties.get("server.local")) != null) {
            LOCAL = String.valueOf(value).toLowerCase().equals("true");
        }
        if ((value = properties.get("server.test")) != null) {
            TEST = String.valueOf(value).toLowerCase().equals("true");
        }
        if ((value = properties.get("server.daoautoupdater")) != null) {
            DAO_AUTO_UPDATER = String.valueOf(value).equalsIgnoreCase("true");
        }
    }

    /**
     * @param tileTypeFocus tile type: top, bot, left, right...
     * @return [tileMapId][tileType]
     */
    private int[][] readTileIndexTileType(int tileTypeFocus) {
        int[][] tileIndexTileType = null;
        try {
            DataInputStream dis = new DataInputStream(new FileInputStream("data/map/tile_set_info"));
            int numTileMap = dis.readByte();
            tileIndexTileType = new int[numTileMap][];
            for (int i = 0; i < numTileMap; i++) {
                int numTileOfMap = dis.readByte();
                for (int j = 0; j < numTileOfMap; j++) {
                    int tileType = dis.readInt();
                    int numIndex = dis.readByte();
                    if (tileType == tileTypeFocus) {
                        tileIndexTileType[i] = new int[numIndex];
                    }
                    for (int k = 0; k < numIndex; k++) {
                        int typeIndex = dis.readByte();
                        if (tileType == tileTypeFocus) {
                            tileIndexTileType[i][k] = typeIndex;

                        }
                    }
                }
            }
        } catch (IOException e) {
            Logger.logException(MapService.class, e);
        }
        return tileIndexTileType;
    }

    /**
     * @param mapId mapId
     * @return tile map for paint
     */
    private int[][] readTileMap(int mapId, byte tileId) {
        int[][] tileMap = null;
        try {
			try (DataInputStream dis = new DataInputStream(new FileInputStream("data/map/tile_map_data/" + (mapId == 3 && tileId == 90 ? "3_native90" : mapId == 0 && tileId == 91 ? "0_aru91" : mapId == 0 && tileId == 92 ? "0_aru92_bankfix" : mapId == 42 && tileId == 93 ? "42_aru_cliff_hd" : mapId == 1 && tileId == 94 ? "1_flower_hill_hd" : mapId == 1 && tileId == 95 ? "1_flower_hill_decor95" : String.valueOf(mapId))))) {
                int w = dis.readByte();
                int h = dis.readByte();
                tileMap = new int[h][w];
                for (int[] tm : tileMap) {
                    for (int j = 0; j < tm.length; j++) {
                        tm[j] = dis.readByte();
                    }
                }
            }
        } catch (IOException e) {
        }
        return tileMap;
    }

    public static Clan getClanById(int id) throws Exception {
        for (Clan clan : CLANS) {
            if (clan.id == id) {
                return clan;
            }
        }
        throw new Exception("Không tìm thấy clan id: " + id);
    }

    public static void addClan(Clan clan) {
        CLANS.add(clan);
    }

    public static int getNumClan() {
        return CLANS.size();

    }

    public static MobTemplate getMobTemplateByTemp(int mobTempId) {
        for (MobTemplate mobTemp : MOB_TEMPLATES) {
            if (mobTemp.id == mobTempId) {
                return mobTemp;
            }
        }
        return null;
    }

    public static byte getNFrameImageByName(String name) {
        Object n = IMAGES_BY_NAME.get(name);
        if (n != null) {
            return Byte.parseByte(String.valueOf(n));
        } else {
            return 0;
        }
    }

    private static void ensureNpcTemplate(int id, String name, int head, int body, int leg, int avatar) {
        while (NPC_TEMPLATES.size() <= id) {
            NpcTemplate empty = new NpcTemplate();
            empty.id = NPC_TEMPLATES.size();
            empty.name = "";
            NPC_TEMPLATES.add(empty);
        }
        NpcTemplate current = NPC_TEMPLATES.get(id);
        if (current == null || current.name == null || current.name.isBlank()) {
            current = new NpcTemplate();
            current.id = id;
            current.name = name;
            current.head = head;
            current.body = body;
            current.leg = leg;
            current.avatar = avatar;
            NPC_TEMPLATES.set(id, current);
        }
    }

    private static void ensureVongCoTemplate(int id, String name, String description, int iconId, int star) {
        while (ITEM_TEMPLATES.size() <= id) {
            ItemTemplate empty = new ItemTemplate();
            empty.id = (short) ITEM_TEMPLATES.size();
            empty.name = "";
            empty.description = "";
            ITEM_TEMPLATES.add(empty);
        }
        ItemTemplate current = ITEM_TEMPLATES.get(id);
        if (current == null) {
            current = new ItemTemplate();
            current.id = (short) id;
            ITEM_TEMPLATES.set(id, current);
        }
        current.type = 4;
        current.gender = 3;
        current.name = name;
        current.description = description;
        current.iconID = (short) iconId;
        current.part = -1;
        current.level = (byte) star;
        current.isUpToUp = false;
    }

    private static void ensureGhostFollowPetTemplate(int id, String name, String description, int iconId, int partId, int headPart, int bodyPart, int legPart) {
        while (ITEM_TEMPLATES.size() <= id) {
            ItemTemplate empty = new ItemTemplate();
            empty.id = (short) ITEM_TEMPLATES.size();
            empty.name = "";
            empty.description = "";
            ITEM_TEMPLATES.add(empty);
        }
        ItemTemplate current = ITEM_TEMPLATES.get(id);
        if (current == null) {
            current = new ItemTemplate();
            current.id = (short) id;
            ITEM_TEMPLATES.set(id, current);
        }
        current.type = 27;
        current.gender = 3;
        current.name = name;
        current.description = description;
        current.iconID = (short) iconId;
        current.part = (short) partId;
        current.head = (short) headPart;
        current.body = (short) bodyPart;
        current.leg = (short) legPart;
        current.isUpToUp = false;
    }

    private static void ensureItemTemplate(int id, String name, String description, int iconId) {
        while (ITEM_TEMPLATES.size() <= id) {
            ItemTemplate empty = new ItemTemplate();
            empty.id = (short) ITEM_TEMPLATES.size();
            empty.name = "";
            empty.description = "";
            ITEM_TEMPLATES.add(empty);
        }
        ItemTemplate current = ITEM_TEMPLATES.get(id);
        if (current == null || current.name == null || current.name.isBlank()) {
            current = new ItemTemplate();
            current.id = (short) id;
            current.type = 99;
            current.gender = 3;
            current.name = name;
            current.description = description;
            current.iconID = (short) iconId;
            current.part = -1;
            current.isUpToUp = true;
            ITEM_TEMPLATES.set(id, current);
        } else {
            current.name = name;
            current.description = description;
            current.iconID = (short) iconId;
            current.isUpToUp = true;
        }
        current.type = 99;
        current.gender = 3;
        current.part = -1;
    }

    private static void ensureSpiritGloveTemplate(int id, String name, int iconId, int tier) {
        while (ITEM_TEMPLATES.size() <= id) {
            ItemTemplate empty = new ItemTemplate();
            empty.id = (short) ITEM_TEMPLATES.size();
            empty.name = "";
            empty.description = "";
            ITEM_TEMPLATES.add(empty);
        }
        ItemTemplate item = ITEM_TEMPLATES.get(id);
        item.id = (short) id;
        item.type = 2;
        item.gender = 3;
        item.name = name;
        item.description = "Đủ 5 món: Đệ x2 HP/KI/SĐ/sát thương chiêu; chủ x2 tỉ lệ rơi SKH.";
        item.level = (byte) tier;
        item.iconID = (short) iconId;
        item.part = -1;
        item.isUpToUp = false;
        item.strRequire = 0;
    }

}

