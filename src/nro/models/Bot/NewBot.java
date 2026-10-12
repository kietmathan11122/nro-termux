package nro.models.Bot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import nro.models.clan.Clan;
import nro.models.item.Item;
import nro.models.map.Map;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.MapService;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.ItemService;
import nro.models.utils.Util;

public class NewBot {

    public static NewBot i;

    private static final int[] TRAINING_MAPS = {
        5, 77, 68, 79, 83, 92, 100, 99, 93, 109,
        2, 3, 9, 11, 16, 17, 70, 69, 71, 76
    };
    private static final int BOTS_PER_MAP = 5;
    private static final short[] COSTUME_IDS = {
        1566, 878, 879, 1255, 405, 406, 407, 423, 424, 425,
        426, 427, 428, 429, 430, 431, 432, 433, 448, 449,
        450, 451, 452, 455, 452, 458, 461, 524, 525, 526,
        527, 528, 549, 550, 551, 552, 575, 576, 577, 578,
        583, 633, 634, 630, 631, 632, 614, 612, 613, 724,
        647, 1553, 765, 1103, 1043, 1042, 1041, 974, 937,
        1864, 1866, 1868, 1860, 1863, 1865, 1780, 1781,
        1841, 1842, 1847, 1848
    };
    private static final short[] ACCESSORY_IDS = {1531, 1113, 1813, 1814};
    private static final String[] CLAN_NAMES = {
        "Top 1", "Cay Chay", "Dam Me NRO", "Dai Vuong", "Nguc To",
        "Tu Tien", "Trum Namek", "Yasuo", "Anh Em"
    };

    public boolean LOAD_PART = true;
    public int MAXPART = 0;
    public static int[][] PARTBOT = new int[Manager.ITEM_TEMPLATES.size()][4];

    private final String[] FIRST_NAMES = java.util.stream.IntStream.rangeClosed(1, 50000)
            .mapToObj(i -> "mrblue" + i)
            .toArray(String[]::new);

    public static NewBot gI() {
        if (i == null) {
            i = new NewBot();
        }
        return i;
    }

    public synchronized void spawnTrainingBots() {
        resetTrainingBots();
        Set<String> usedNames = new HashSet<>();
        for (int mapId : TRAINING_MAPS) {
            List<Zone> spawnZones = randomizeZonesForMap(mapId);
            for (Zone zone : spawnZones) {
                Bot bot = createTrainingBot(zone, usedNames);
                if (bot == null) {
                    continue;
                }
                BotManager.gI().bot.add(bot);
                bot.joinAssignedZone(zone);
                moveBotToMobArea(bot, zone);
            }
        }
    }

    public synchronized void stopTrainingBots() {
        resetTrainingBots();
    }

    public boolean isTrainingBotsActive() {
        for (Bot bot : BotManager.gI().bot) {
            if (bot != null && bot.trainingMob != null) {
                return true;
            }
        }
        return false;
    }

    public void LoadPart() {
        if (LOAD_PART) {
            int i = 0;
            for (Template.ItemTemplate it : Manager.ITEM_TEMPLATES) {
                if (it.type == 5) {
                    if (it.head != -1 && it.leg != -1 && it.body != -1 && it.leg != 194) {
                        PARTBOT[i][0] = it.head;
                        PARTBOT[i][1] = it.leg;
                        PARTBOT[i][2] = it.body;
                        PARTBOT[i][3] = it.gender;
                        i++;
                        MAXPART++;
                    }
                }
            }
            LOAD_PART = false;
        }
    }

    public String Getname() {
        return FIRST_NAMES[new Random().nextInt(FIRST_NAMES.length)];
    }

    public int getIndex(int gender) {
        int Random = new Random().nextInt(MAXPART);
        int gend = PARTBOT[Random][3];
        if (gend == gender) {
            return Random;
        } else {
            return getIndex(gender);
        }
    }

    public void runBot(int type, BotGiaoDich shop, int slot) {
        if (type == 0) {
            spawnTrainingBots();
            return;
        }
        LoadPart();
        for (int i = 0; i < slot; i++) {
            int Gender = new Random().nextInt(3);
            int Random1 = getIndex(Gender);
            int head = PARTBOT[Random1][0];
            int leg = PARTBOT[Random1][1];
            int body = PARTBOT[Random1][2];

            BotGiaoDich shopBotCopy = null;
            if (shop != null) {
                shopBotCopy = new BotGiaoDich(shop);
            }

            int flag = Manager.gI().FLAGS_BAGS.get(new Random().nextInt(Manager.gI().FLAGS_BAGS.size())).id;

            Bot b;
            if (type == 3) {
                b = new BotAttackplayer((short) head, (short) body, (short) leg, type, Getname(), (short) flag);
            } else {
                b = new Bot((short) head, (short) body, (short) leg, type, Getname(), shopBotCopy, (short) flag);
            }

            BotSanBoss bos = new BotSanBoss(b);
            BotPemQuai mo1 = new BotPemQuai(b);
            b.mo1 = mo1;
            b.boss = bos;

            int congThem = new Random().nextInt(50000000);
            b.nPoint.limitPower = 8;
            b.nPoint.power = 1000 + congThem;
            b.nPoint.tiemNang = 20000000 + congThem;
            b.nPoint.dameg = 600000;
            b.nPoint.mpg = 600000;
            b.nPoint.mpMax = 2000000000;
            b.nPoint.mp = 600000;
            b.nPoint.hpg = 675000;
            b.nPoint.hpMax = 10000;
            b.nPoint.hp = 600000;
            b.nPoint.maxStamina = 20000;
            b.nPoint.stamina = 20000;
            b.nPoint.critg = 10;
            b.nPoint.defg = 10;
            b.gender = (byte) Gender;
            b.charms.tdThuHut = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000);

            b.leakSkill();
            b.joinMap();

            for (int j = 0; j < 500; j++) {
                b.inventory.itemsBag.add(ItemService.gI().createItemNull());
            }

            if (shopBotCopy != null) {
                shopBotCopy.bot = b;
            }

            BotManager.gI().bot.add(b);
        }
    }

    public Zone getAssignedZone(int mapId, int zoneId) {
        Map map = MapService.gI().getMapById(mapId);
        if (map == null || zoneId < 0 || zoneId >= map.zones.size()) {
            return null;
        }
        return map.zones.get(zoneId);
    }

    public Zone getRandomEligibleZone(int mapId, int excludeZoneId) {
        Map map = MapService.gI().getMapById(mapId);
        if (map == null || map.zones == null || map.zones.isEmpty()) {
            return null;
        }
        List<Zone> eligibleZones = new ArrayList<>();
        for (Zone zone : map.zones) {
            if (zone == null) {
                continue;
            }
            if (mapId == 5 && zone.zoneId == 0) {
                continue;
            }
            if (zone.zoneId == excludeZoneId && map.zones.size() > 1) {
                continue;
            }
            eligibleZones.add(zone);
        }
        if (eligibleZones.isEmpty()) {
            return getAssignedZone(mapId, excludeZoneId);
        }
        return eligibleZones.get(Util.nextInt(0, eligibleZones.size() - 1));
    }

    private void resetTrainingBots() {
        List<Bot> bots = new ArrayList<>(BotManager.gI().bot);
        for (Bot bot : bots) {
            if (bot == null || bot.trainingMob == null) {
                continue;
            }
            BotManager.gI().bot.remove(bot);
            try {
                ChangeMapService.gI().exitMap(bot);
            } catch (Exception ignored) {
            }
            try {
                bot.dispose();
            } catch (Exception ignored) {
            }
        }
    }

    private Bot createTrainingBot(Zone zone, Set<String> usedNames) {
        if (zone == null) {
            return null;
        }
        byte gender = (byte) Util.nextInt(0, 2);
        Item costume = ItemService.gI().createNewItem(COSTUME_IDS[Util.nextInt(0, COSTUME_IDS.length - 1)]);
        Item accessory = ItemService.gI().createNewItem(ACCESSORY_IDS[Util.nextInt(0, ACCESSORY_IDS.length - 1)]);
        if (costume == null || costume.template == null || accessory == null || accessory.template == null) {
            return null;
        }
        String name = BotNameRepository.randomTrainingBotName(usedNames);
        Bot bot = new Bot(name, gender, (short) costume.template.head, (short) costume.template.body,
                (short) costume.template.leg, (short) accessory.template.part, zone.map.mapId, zone.zoneId);
        bot.trainingMob = new Mobb(bot);
        bot.nPoint.limitPower = 8;
        if (isLowTrainingMap(zone.map.mapId)) {
            bot.nPoint.power = Util.nextLong(50000000L, 100000000L);
        } else if (isMidTrainingMap(zone.map.mapId)) {
            bot.nPoint.power = Util.nextLong(500000000L, 20000000000L);
        } else {
            bot.nPoint.power = Util.nextLong(30000000000L, 60000000000L);
        }
        bot.nPoint.tiemNang = bot.nPoint.power;
        if (isLowTrainingMap(zone.map.mapId)) {
            bot.nPoint.hpg = Util.nextInt(5000, 10000);
            bot.nPoint.dameg = Util.nextInt(500, 3000);
        } else if (isMidTrainingMap(zone.map.mapId)) {
            bot.nPoint.hpg = Util.nextInt(50000, 200000);
            bot.nPoint.dameg = Util.nextInt(5000, 15000);
        } else {
            bot.nPoint.hpg = Util.nextInt(150000, 500000);
            bot.nPoint.dameg = Util.nextInt(10000, 20000);
        }
        bot.nPoint.mpg = 2000000000;
        bot.nPoint.mpMax = 2000000000;
        bot.nPoint.mp = bot.nPoint.mpMax;
        bot.nPoint.hpMax = bot.nPoint.hpg;
        bot.nPoint.hp = bot.nPoint.hpMax;
        bot.nPoint.maxStamina = 20000;
        bot.nPoint.stamina = 20000;
        bot.nPoint.critg = 10;
        bot.nPoint.defg = isHighTrainingMap(zone.map.mapId) ? 10000 : 500;
        bot.clan = createFakeClan();
        ensureBodySlots(bot);
        bot.leakSkill();
        return bot;
    }

    public boolean isLowTrainingMap(int mapId) {
        return mapId == 5 || mapId == 2 || mapId == 3 || mapId == 9 || mapId == 11
                || mapId == 16 || mapId == 17;
    }

    public boolean isMidTrainingMap(int mapId) {
        return mapId == 68 || mapId == 77 || mapId == 79 || mapId == 83
                || mapId == 70 || mapId == 69 || mapId == 71 || mapId == 76;
    }

    public boolean isHighTrainingMap(int mapId) {
        return mapId == 92 || mapId == 100 || mapId == 99 || mapId == 93 || mapId == 109;
    }

    private void ensureBodySlots(Bot bot) {
        while (bot.inventory.itemsBody.size() < 12) {
            bot.inventory.itemsBody.add(ItemService.gI().createItemNull());
        }
    }

    private void moveBotToMobArea(Bot bot, Zone zone) {
        if (zone == null || zone.mobs == null || zone.mobs.isEmpty()) {
            return;
        }
        int index = Util.nextInt(0, zone.mobs.size() - 1);
        bot.moveTo(zone.mobs.get(index).location.x, zone.mobs.get(index).location.y);
    }

    private List<Zone> randomizeZonesForMap(int mapId) {
        Map map = MapService.gI().getMapById(mapId);
        List<Zone> result = new ArrayList<>();
        if (map == null || map.zones == null || map.zones.isEmpty()) {
            return result;
        }
        List<Zone> eligibleZones = new ArrayList<>();
        for (Zone zone : map.zones) {
            if (zone == null) {
                continue;
            }
            if (mapId == 5 && zone.zoneId == 0) {
                continue;
            }
            eligibleZones.add(zone);
        }
        if (eligibleZones.isEmpty()) {
            return result;
        }
        int[] perZone = new int[eligibleZones.size()];
        int assigned = 0;
        while (assigned < BOTS_PER_MAP) {
            List<Integer> candidates = new ArrayList<>();
            for (int i = 0; i < eligibleZones.size(); i++) {
                if (perZone[i] < 2) {
                    candidates.add(i);
                }
            }
            if (candidates.isEmpty()) {
                for (int i = 0; i < eligibleZones.size(); i++) {
                    candidates.add(i);
                }
            }
            int pickedIndex = candidates.get(Util.nextInt(0, candidates.size() - 1));
            perZone[pickedIndex]++;
            assigned++;
        }
        for (int i = 0; i < eligibleZones.size(); i++) {
            for (int count = 0; count < perZone[i]; count++) {
                result.add(eligibleZones.get(i));
            }
        }
        return result;
    }

    private Clan createFakeClan() {
        Clan clan = new Clan();
        clan.name = CLAN_NAMES[Util.nextInt(0, CLAN_NAMES.length - 1)];
        clan.name2 = "";
        clan.slogan = "";
        clan.imgId = -1;
        clan.active = false;
        return clan;
    }
}
