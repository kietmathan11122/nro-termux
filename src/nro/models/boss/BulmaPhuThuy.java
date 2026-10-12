package nro.models.boss;

import nro.models.consts.ConstPlayer;
import nro.models.map.Zone;
import nro.models.map.ItemMap;
import nro.models.item.Item;
import nro.models.services.PetCollectionService;
import nro.models.services.Service;
import nro.models.services.ItemService;
import nro.models.services.InventoryService;
import nro.models.map.service.MapService;
import nro.models.player.Player;
import nro.models.skill.Skill;
import nro.models.utils.Util;

/**
 * Boss map thường dùng skin Bulma Halloween (Spine/Skins/10173).
 * Mỗi instance được gắn với một map, vì vậy mỗi map chỉ có một Bulma Phù Thủy.
 */
public class BulmaPhuThuy extends Boss {

    private static final int MIN_HP = 1_000_000;
    private static final int MAX_HP = 16_000_000;
    private final int homeMapId;

    public BulmaPhuThuy(int bossId, int homeMapId) throws Exception {
        super(bossId, new BossData(
                "Bunma Phù Thủy",
                ConstPlayer.TRAI_DAT,
                // Skin 10173 (Bulma_halloween) tương ứng part 3102.
                new short[]{3102, 0, 0, -1, -1, -1},
                100,
                new int[]{MIN_HP},
                new int[]{homeMapId},
                new int[][]{
                    // Boss framework nhân đôi cooldown: 6000 = khoảng 12 giây thực tế.
                    {Skill.KAMEJOKO, 5, 6000},
                    {Skill.MASENKO, 5, 6000},
                    {Skill.DRAGON, 5, 5000}
                },
                new String[]{"|-1|Các ngươi dám quấy rầy ta sao?"},
                new String[]{"|-1|Ma thuật của ta chưa kết thúc!"},
                new String[]{"|-1|Ta sẽ trở lại sau 30 phút!"},
                30 * 60));
        this.homeMapId = homeMapId;
        this.damagePercentOfTargetHp = 10;
    }

    @Override
    public void respawn() {
        super.respawn();
        // Mỗi lần xuất hiện lấy ngẫu nhiên 1–16 triệu HP.
        this.nPoint.hpMax = Util.nextInt(MIN_HP, MAX_HP);
        this.nPoint.hp = this.nPoint.hpMax;
        this.nPoint.dame = Math.max(1, this.nPoint.hpMax / 100);
        this.nPoint.crit = Util.nextInt(5, 25);
    }

    @Override
    public Zone getMapJoin() {
        nro.models.map.Map map = MapService.gI().getMapById(homeMapId);
        if (map == null || map.zones == null || map.zones.isEmpty()) {
            return null;
        }
        // Boss.joinMap sẽ tự chọn khu trống trong map này. Chỉ có một instance/map.
        return map.zones.get(0);
    }

    @Override
    public synchronized int injured(Player attacker, long damage, boolean piercing, boolean isMobAttack) {
        if (this.isDie()) {
            return 0;
        }
        if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
            this.chat("Xí hụt");
            return 0;
        }
        damage = this.nPoint.subDameInjureWithDeff(damage);
        // Người chơi pém theo % máu: một đòn tối đa 1% HP tổng của boss.
        long maxDamage = Math.max(1L, this.nPoint.hpMax / 100L);
        if (!piercing && damage > maxDamage) {
            damage = maxDamage;
        }
        this.nPoint.subHP(damage);
        if (this.isDie()) {
            this.setDie(attacker);
            this.die(attacker);
        }
        return (int) damage;
    }
    @Override
    public void reward(Player plKill) {
        super.reward(plKill);
        if (plKill == null || this.zone == null) return;
        if (PetCollectionService.gI().hasAnyChibi(plKill)) {
            Service.gI().sendThongBao(plKill, "Bạn đã sở hữu một Chibi. Hãy vứt Chibi cũ trước khi săn con mới.");
            return;
        }
        int[] chibis = PetCollectionService.CHIBI_IDS;
        int itemId = chibis[Util.nextInt(0, chibis.length - 1)];
        Item chibi = ItemService.gI().createNewItem((short) itemId);
        chibi.itemOptions.add(new Item.ItemOption(PetCollectionService.CHIBI_LEVEL_OPTION, 1));
        plKill.inventory.chibi = chibi;
        PetCollectionService.gI().sendChibiState(plKill);
        Service.gI().point(plKill);
        Service.gI().sendThongBao(plKill, "Bạn nhận được " + chibi.template.name + " cấp 1.");
        // Chibi vừa nhận xuất hiện ngay và bay theo người hạ boss.
        PetCollectionService.gI().syncChibiFollower(plKill);
    }
}

