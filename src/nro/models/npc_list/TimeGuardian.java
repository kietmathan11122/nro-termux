package nro.models.npc_list;
import nro.models.consts.ConstNpc;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.map.service.ChangeMapService;
public class TimeGuardian extends Npc {
    private static final java.util.Map<Player,Travel> TRAVEL=java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private static class Travel {
        final int from,to,x;
        final long started=System.currentTimeMillis();
        Travel(int from,int to,int x){this.from=from;this.to=to;this.x=x;}
    }
    private static void sendTravel(Player player,int seconds){
        nro.models.network.Message msg=null;
        try{
            msg=new nro.models.network.Message(-105);
            msg.writer().writeShort(seconds);msg.writer().writeByte(0);
            player.sendMessage(msg);
        }catch(Exception ex){nro.models.services.Service.gI().sendThongBao(player,"Không thể mở hành trình thời gian.");}
        finally{if(msg!=null)msg.cleanup();}
    }
    public static boolean finishTravel(Player player){
        Travel travel=TRAVEL.get(player);
        if(travel==null)return player.type==6;
        if(player.zone==null || player.zone.map.mapId!=travel.from || player.isDie()){
            TRAVEL.remove(player);return true;
        }
        if(System.currentTimeMillis()-travel.started<3000){sendTravel(player,1);return true;}
        if(!TRAVEL.remove(player,travel))return true;
        ChangeMapService.gI().changeMapBySpaceShip(player,travel.to,-1,travel.x);
        return true;
    }
    public TimeGuardian(int mapId,int status,int cx,int cy,int tempId,int avatar){super(mapId,status,cx,cy,tempId,avatar);}
    @Override public void openBaseMenu(Player player){
        if(!canOpenNpc(player))return;
        boolean present=mapId==198;
        createOtherMenu(player,ConstNpc.BASE_MENU,
            present?"Ta là Thời Sư Lạc Ấn. Cậu có muốn khám phá thế giới quá khứ không?":"Bảy miền ký ức đang chờ cậu khám phá. Ta có thể đưa cậu trở về Vampa bất cứ lúc nào.",
            present?"Về\nquá khứ":"Về\nVampa","Đóng");
    }
    @Override public void confirmMenu(Player player,int select){
        if(canOpenNpc(player) && player.idMark.isBaseMenu() && select==0 && !player.isDie()){
            synchronized(TRAVEL){
                Travel active=TRAVEL.get(player);
                if(active!=null && System.currentTimeMillis()-active.started<30000)return;
                TRAVEL.put(player,new Travel(mapId,mapId==198?228:198,mapId==198?180:300));
                player.type=6;
            }
            sendTravel(player,3);
        }
    }
}
