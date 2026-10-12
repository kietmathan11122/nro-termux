package nro.models.services;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import nro.models.database.PlayerEconomyDAO;
import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.player.Player;
import org.json.simple.JSONObject;

/** Real bag items; all costs, random rolls and equipment checks belong to the server. */
public final class DarkStoneLabService {
    private static final DarkStoneLabService INSTANCE = new DarkStoneLabService();
    public static final int DARK = 2442, LIGHT = 2443;
    private static final int[] OPTIONS = {50,77,103,5};
    private static final int[] VALUES = {3,5,5,3};
    private final java.util.Map<Long,String> viewKeys = new java.util.concurrent.ConcurrentHashMap<>();
    public static DarkStoneLabService gI() { return INSTANCE; }
    private static boolean eligible(Item i) {
        return i != null && i.isNotNullItem() && i.quantity == 1 && i.template.type >= 0 && i.template.type <= 4;
    }
    private static int count(Item i) {
        int n=0; for (Item.ItemOption o:i.itemOptions) if(o.darkInfusion)n++; return n;
    }
    public static void addDarkLine(Item gear, Item.ItemOption option) {
        if(!eligible(gear)||count(gear)>=7||option==null||option.optionTemplate==null)
            throw new IllegalArgumentException("Trang bị không thể ép thêm.");
        int index=-1;for(int j=0;j<OPTIONS.length;j++)if(OPTIONS[j]==option.optionTemplate.id)index=j;
        if(index<0||option.param!=VALUES[index])throw new IllegalArgumentException("Dòng Hắc ám không hợp lệ.");
        option.darkInfusion=true;gear.itemOptions.add(option);
    }
    public static void clearDarkLines(Item gear) {
        if(!eligible(gear)||count(gear)==0)throw new IllegalArgumentException("Trang bị chưa có dòng Hắc ám.");
        gear.itemOptions.removeIf(o->o.darkInfusion);
    }
    private static int stones(Player p,int id) {
        int n=0;for(Item i:p.inventory.itemsBag)if(i.isNotNullItem()&&i.template.id==id)n+=i.quantity;return n;
    }
    private String token(Player p,Item i,int slot) {
        StringBuilder b=new StringBuilder(viewKeys.get(p.id)).append(':').append(slot).append(':')
                .append(i.template.id).append(':').append(i.createTime).append(':').append(i.quantity);
        for(Item.ItemOption o:i.itemOptions)b.append(':').append(o.optionTemplate.id).append(',').append(o.param).append(',').append(o.darkInfusion);
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(b.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch(Exception e){throw new IllegalStateException(e);}
    }
    public void handleMessage(Player p,Message m) {
        if(p==null||!p.isPl()||p.beforeDispose||!p.idMark.isLoadedAllDataPlayer()||p.inventory.gameDataPersistenceBlocked)return;
        synchronized(p) {
            String notice="";
            try {
                int action=m.reader().readUnsignedByte(),slot=-1;String submitted="";
                if(action==3||action==4){slot=m.reader().readShort();submitted=m.reader().readUTF();}
                if(action>4||m.reader().available()!=0)throw new IllegalArgumentException("Yêu cầu không hợp lệ.");
                if(action!=0) {
                    if(p.isDie()||p.zone==null)throw new IllegalArgumentException("Hãy hồi sinh trước khi sử dụng.");
                    long now=System.currentTimeMillis();
                    if(now-p.inventory.lastFishingActionAt<250)throw new IllegalArgumentException("Hãy chờ một chút.");
                    p.inventory.lastFishingActionAt=now;
                    JSONObject data=PlayerEconomyDAO.copyGameData(p);
                    FishingLabRules.State fish=FishingLabRules.fromJson(data.get("fishing"));
                    long gold=p.inventory.gold;
                    if(action==1||action==2) {
                        int id=action==1?DARK:LIGHT,cost=action==1?100000:50000,materials=action==1?5:3;
                        if(gold<cost)throw new IllegalArgumentException("Chưa đủ vàng chế tạo.");
                        FishingLabRules.State next=FishingLabRules.consumeFish(fish,materials);
                        Item target=null;int empty=-1;
                        for(int j=0;j<p.inventory.itemsBag.size();j++) {
                            Item i=p.inventory.itemsBag.get(j);
                            if(!i.isNotNullItem()&&empty<0)empty=j;
                            if(i.isNotNullItem()&&i.template.id==id&&i.quantity<9999)target=i;
                        }
                        if(target==null&&empty<0)throw new IllegalArgumentException("Túi đồ đã đầy.");
                        if(target!=null)target.quantity++;
                        else p.inventory.itemsBag.set(empty,ItemService.gI().createNewItem((short)id,1));
                        gold-=cost;data.put("fishing",next.toJson());notice="Đã chế tạo 1 "+(id==DARK?"Đá Hắc ám":"Đá Ánh sáng")+" vào túi đồ.";
                    } else {
                        if(slot<0||slot>=p.inventory.itemsBag.size())throw new IllegalArgumentException("Hãy chọn lại trang bị.");
                        Item gear=p.inventory.itemsBag.get(slot);
                        if(!eligible(gear)||!token(p,gear,slot).equals(submitted))throw new IllegalArgumentException("Trang bị đã thay đổi. Hãy chọn lại.");
                        int n=count(gear),id=action==3?DARK:LIGHT;
                        if(action==3&&n>=7)throw new IllegalArgumentException("Trang bị đã ép đủ 7 lần.");
                        if(action==4&&n==0)throw new IllegalArgumentException("Trang bị chưa có dòng Hắc ám.");
                        Item stone=null;for(Item i:p.inventory.itemsBag)if(i.isNotNullItem()&&i.template.id==id&&i.quantity>0){stone=i;break;}
                        if(stone==null)throw new IllegalArgumentException("Bạn chưa có đá cần dùng.");
                        if(action==3){int roll=ThreadLocalRandom.current().nextInt(4);addDarkLine(gear,new Item.ItemOption(OPTIONS[roll],VALUES[roll]));notice="Ép thành công: "+gear.itemOptions.get(gear.itemOptions.size()-1).getOptionString()+" ("+(n+1)+"/7).";}
                        else {clearDarkLines(gear);notice="Đã xóa các dòng Hắc ám. Có thể ép lại 7 lần.";}
                        stone.quantity--;if(stone.quantity==0)stone.template=null;
                        gear.info=gear.getInfo();gear.content=gear.getContent();
                    }
                    if(!PlayerEconomyDAO.commitBag(p,gold,data))return;
                    InventoryService.gI().sendItemBags(p);Service.gI().sendMoney(p);
                }
            } catch(Exception e){notice=e instanceof IllegalArgumentException?e.getMessage():"Chưa xử lý được. Hãy đồng bộ lại.";}
            sendState(p,notice);
        }
    }
    public void openEquipment(Player p) {
        Message message=null;
        try {message=new Message(-34);message.writer().writeByte(32);p.sendMessage(message);}
        catch(Exception e){nro.models.utils.Logger.logException(DarkStoneLabService.class,e);}
        finally{if(message!=null)message.cleanup();}
    }
    public void buyStone(Player p,int id) {
        if(p==null||!p.isPl()||p.beforeDispose||!p.idMark.isLoadedAllDataPlayer()||p.inventory.gameDataPersistenceBlocked)return;
        synchronized(p) {
            try {
                if(p.isDie()||p.zone==null)throw new IllegalArgumentException("Hãy hồi sinh trước khi mua đá.");
                if(p.idMark.getShopOpen()==null||!"CHI_CHI_STONES".equals(p.idMark.getShopOpen().tagName)||!"CHI_CHI_STONES".equals(p.idMark.getTagNameShop())||(id!=DARK&&id!=LIGHT))throw new IllegalArgumentException("Hãy mở cửa hàng đá ở Chi Chi.");
                long now=System.currentTimeMillis();if(now-p.inventory.lastFishingActionAt<250)throw new IllegalArgumentException("Hãy chờ một chút.");p.inventory.lastFishingActionAt=now;
                int cost=id==DARK?100000:50000;if(p.inventory.gold<cost)throw new IllegalArgumentException("Bạn chưa đủ vàng.");
                Item target=null;int empty=-1;
                for(int i=0;i<p.inventory.itemsBag.size();i++) {Item item=p.inventory.itemsBag.get(i);if(!item.isNotNullItem()&&empty<0)empty=i;if(item.isNotNullItem()&&item.template.id==id&&item.quantity<9999)target=item;}
                if(target==null&&empty<0)throw new IllegalArgumentException("Túi đồ đã đầy.");
                Item created=target==null?ItemService.gI().createNewItem((short)id,1):null;
                if(target!=null)target.quantity++;else p.inventory.itemsBag.set(empty,created);
                if(!PlayerEconomyDAO.commitBag(p,p.inventory.gold-cost,PlayerEconomyDAO.copyGameData(p)))return;
                InventoryService.gI().sendItemBags(p);Service.gI().sendMoney(p);Service.gI().sendThongBao(p,"Đã mua 1 "+(id==DARK?"Đá Hắc ám":"Đá Ánh sáng")+".");
            }catch(IllegalArgumentException e){Service.gI().sendThongBao(p,e.getMessage());}
        }
    }
    private void sendState(Player p,String notice) {
        Message out=null;
        try {
            viewKeys.put(p.id,UUID.randomUUID().toString());
            FishingLabRules.State fish=FishingLabRules.fromJson(p.inventory.gameData.get("fishing"));
            out=new Message(-34);out.writer().writeByte(31);out.writer().writeByte(1);
            out.writer().writeLong(p.inventory.gold);out.writer().writeInt(fish.totalFish());
            out.writer().writeInt(stones(p,DARK));out.writer().writeInt(stones(p,LIGHT));
            out.writer().writeInt(100000);out.writer().writeInt(5);out.writer().writeInt(50000);out.writer().writeInt(3);out.writer().writeByte(7);
            int total=0;for(Item i:p.inventory.itemsBag)if(eligible(i))total++;
            out.writer().writeShort(total);
            for(int j=0;j<p.inventory.itemsBag.size();j++) {
                Item i=p.inventory.itemsBag.get(j);if(!eligible(i))continue;
                out.writer().writeShort(j);out.writer().writeShort(i.template.id);out.writer().writeShort(i.template.iconID);
                out.writer().writeUTF(i.template.name);out.writer().writeUTF(token(p,i,j));out.writer().writeByte(count(i));
                for(int id:OPTIONS){int n=0;for(Item.ItemOption o:i.itemOptions)if(o.darkInfusion&&o.optionTemplate.id==id)n++;out.writer().writeByte(n);}
            }
            out.writer().writeUTF(notice==null?"":notice);p.sendMessage(out);
        }catch(Exception e){nro.models.utils.Logger.logException(DarkStoneLabService.class,e);}
        finally {if(out!=null)out.cleanup();}
    }
}
