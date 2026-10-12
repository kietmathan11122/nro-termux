package nro.models.services;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/** Pure, bounded rules for the side adventure. Main tasks are never read or changed. */
public final class StoryAdventureRules {
    public static final String KEY = "storyAdventure";
    public static final int VERSION = 1, CHAPTER_COUNT = 10;
    public static final long MILK_MS = 180_000L, MEMORY_MS = 90_000L, MENU_GAP_MS = 350L;
    public static final int[] REWARDS = {10000,15000,15000,20000,20000,20000,20000,25000,25000,30000};
    public static final String[] TITLES = {"Nhập môn Quy Lão","Giao sữa buổi sớm","Radar trục trặc","Đường tới Tháp Karin","Bắt lấy nhịp chân","Đường lên Điện Thần","Dọn đường cho Dende","Tín hiệu Namếc","Lời cảnh báo từ tương lai","Trở lại Kame"};
    public static final String[] INTRO = {
        "Quy Lão: Sức mạnh bắt đầu từ khả năng kiểm soát. Luyện đòn cơ bản rồi trở lại gặp ta.",
        "Quy Lão: Goku và Krillin từng luyện bằng việc giao sữa. Đi đúng lộ trình, đừng chỉ chăm chăm đánh nhau.",
        "Bunma: Đội Capsule cần đem linh kiện sửa Radar qua rừng. Nhờ cậu dọn đường cho họ!",
        "Karin: Con đường leo tháp cũng là bài luyện. Sức mạnh phải được rèn luyện, không tự nhiên xuất hiện.",
        "Karin: Quan sát nhịp chân của ta, nhớ thật rõ rồi mới hành động.",
        "Karin: Hãy lên Điện Thần quan sát con đường, rồi về kể lại. Ta chưa yêu cầu con xin sức mạnh của ai.",
        "Dende: Ta đang chữa trị cho mọi người. Nhờ cậu dọn đường để dân làng về nhà an toàn.",
        "Guru: Trước khi chiến đấu, hãy hiểu nơi mình cần bảo vệ và những người đang sống ở đó.",
        "Calick: Mẹ cháu cần biết tình hình hai thành phố ở tương lai. Hãy trinh sát trước khi kết luận.",
        "Quy Lão: Chiến binh mạnh còn phải biết giúp người khác. Con đã nhớ các bài học trên đường chưa?"
    };
    public static final String[] SYMBOLS = {"Trái", "Phải", "Dừng"};
    public static final int[][] RADAR_ROUTES = {{1,0,5},{29,4,6},{9,10,13}};
    private static final int[] DONE_STEPS = {3,4,3,2,1,3,3,4,4,2};
    public static final int[] CLAIM_NPCS = {13,13,7,18,18,18,8,14,37,13};
    public static final int[] CLAIM_MAPS = {5,5,0,46,46,46,7,13,102,5};
    private static final ZoneId DAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private StoryAdventureRules() { }

    public static final class State {
        int chapter=1, step, progress, claimedMask, dailyRewardMask;
        boolean accepted;
        long acceptedAt, milkUntil, day, radarStarted, radarBest, memoryStarted, memoryLastAnswer, memoryBest;
        String milkSession="", radarSession="", memorySession="";
        int radarRoute=-1, radarStep, memoryLength, memoryStep, memoryBestLength;
        int[] radarX=new int[0], radarY=new int[0], memory=new int[0];
        boolean memoryChapter;
        List<String> killTokens=new ArrayList<>();
        public State copy() { return fromJson(toJson()); }
        public JSONObject toJson() {
            JSONObject o=new JSONObject(); o.put("version",VERSION);
            o.put("chapter",chapter);o.put("step",step);o.put("progress",progress);o.put("accepted",accepted);o.put("acceptedAt",acceptedAt);o.put("claimedMask",claimedMask);
            o.put("milkUntil",milkUntil);o.put("milkSession",milkSession);o.put("day",day);o.put("dailyRewardMask",dailyRewardMask);
            o.put("radarRoute",radarRoute);o.put("radarStep",radarStep);o.put("radarX",ints(radarX));o.put("radarY",ints(radarY));o.put("radarStarted",radarStarted);o.put("radarSession",radarSession);o.put("radarBest",radarBest);
            o.put("memoryLength",memoryLength);o.put("memoryStep",memoryStep);o.put("memory",ints(memory));o.put("memoryStarted",memoryStarted);o.put("memoryLastAnswer",memoryLastAnswer);o.put("memorySession",memorySession);o.put("memoryChapter",memoryChapter);o.put("memoryBest",memoryBest);o.put("memoryBestLength",memoryBestLength);
            JSONArray tokens=new JSONArray();tokens.addAll(killTokens);o.put("killTokens",tokens); return o;
        }
    }
    public static State initial() { return new State(); }
    private static JSONArray ints(int[] values) { JSONArray a=new JSONArray();for(int v:values)a.add(v);return a; }
    private static long number(JSONObject o,String key,long min,long max) {
        Object v=o.get(key);if(!(v instanceof Number)||v instanceof Double||v instanceof Float)throw new IllegalArgumentException("Dữ liệu hành trình chưa hợp lệ.");
        long n=((Number)v).longValue();if(n<min||n>max)throw new IllegalArgumentException("Dữ liệu hành trình ngoài giới hạn.");return n;
    }
    private static String string(JSONObject o,String key) { Object v=o.get(key);if(!(v instanceof String)||((String)v).length()>96)throw new IllegalArgumentException("Dữ liệu lượt chơi chưa hợp lệ.");return (String)v; }
    private static boolean bool(JSONObject o,String key) { Object v=o.get(key);if(!(v instanceof Boolean))throw new IllegalArgumentException("Dữ liệu lượt chơi chưa hợp lệ.");return (Boolean)v; }
    private static int[] array(JSONObject o,String key,int max,int valueMax) {
        Object v=o.get(key);if(!(v instanceof JSONArray)||((JSONArray)v).size()>max)throw new IllegalArgumentException("Dữ liệu lượt chơi chưa hợp lệ.");JSONArray a=(JSONArray)v;int[] r=new int[a.size()];
        for(int i=0;i<r.length;i++){Object n=a.get(i);if(!(n instanceof Number)||n instanceof Double||n instanceof Float||((Number)n).longValue()<0||((Number)n).longValue()>valueMax)throw new IllegalArgumentException("Dữ liệu lượt chơi chưa hợp lệ.");r[i]=((Number)n).intValue();}return r;
    }
    public static State fromJson(Object raw) {
        if(raw==null)return initial();if(!(raw instanceof JSONObject))throw new IllegalArgumentException("Không đọc được dữ liệu hành trình.");JSONObject o=(JSONObject)raw;
        if(number(o,"version",0,Long.MAX_VALUE)!=VERSION)throw new IllegalArgumentException("Phiên bản hành trình chưa được hỗ trợ.");State s=new State();
        s.chapter=(int)number(o,"chapter",1,11);s.step=(int)number(o,"step",0,4);s.progress=(int)number(o,"progress",0,8);s.claimedMask=(int)number(o,"claimedMask",0,1023);s.accepted=bool(o,"accepted");s.acceptedAt=number(o,"acceptedAt",0,Long.MAX_VALUE);
        int expected=(1<<(s.chapter-1))-1;if(s.claimedMask!=expected||s.chapter==11&&(s.accepted||s.step!=0||s.progress!=0))throw new IllegalArgumentException("Tiến độ hành trình chưa hợp lệ.");
        if(s.chapter<=10&&(s.step>DONE_STEPS[s.chapter-1]||!s.accepted&&(s.step!=0||s.progress!=0)||s.accepted&&s.acceptedAt==0))throw new IllegalArgumentException("Tiến độ hành trình chưa hợp lệ.");
        int maxProgress=s.accepted&&s.chapter==1&&s.step==1?7:s.accepted&&s.chapter==3&&s.step==1?3:s.accepted&&s.chapter==7&&s.step==1?5:s.accepted&&s.chapter==10&&s.step==1?2:0;
        if(s.progress>maxProgress)throw new IllegalArgumentException("Tiến độ mục tiêu chưa hợp lệ.");
        s.milkUntil=number(o,"milkUntil",0,Long.MAX_VALUE);s.milkSession=string(o,"milkSession");s.day=number(o,"day",0,1000000);s.dailyRewardMask=(int)number(o,"dailyRewardMask",0,3);
        s.radarRoute=(int)number(o,"radarRoute",-1,2);s.radarStep=(int)number(o,"radarStep",0,3);s.radarX=array(o,"radarX",3,65535);s.radarY=array(o,"radarY",3,65535);s.radarStarted=number(o,"radarStarted",0,Long.MAX_VALUE);s.radarSession=string(o,"radarSession");s.radarBest=number(o,"radarBest",0,Long.MAX_VALUE);
        s.memoryLength=(int)number(o,"memoryLength",0,5);s.memoryStep=(int)number(o,"memoryStep",0,5);s.memory=array(o,"memory",5,2);s.memoryStarted=number(o,"memoryStarted",0,Long.MAX_VALUE);s.memoryLastAnswer=number(o,"memoryLastAnswer",0,Long.MAX_VALUE);s.memorySession=string(o,"memorySession");s.memoryChapter=bool(o,"memoryChapter");s.memoryBest=number(o,"memoryBest",0,Long.MAX_VALUE);s.memoryBestLength=(int)number(o,"memoryBestLength",0,5);
        if(s.radarRoute>=0&&(s.radarX.length!=3||s.radarY.length!=3||s.radarStarted==0||s.radarSession.isEmpty())||s.memoryLength>0&&(s.memoryLength<3||s.memory.length!=s.memoryLength||s.memoryStep>s.memoryLength||s.memorySession.isEmpty()||s.memoryStarted==0)||s.memoryLength>0&&s.radarRoute>=0)throw new IllegalArgumentException("Lượt chơi chưa hợp lệ.");
        Object t=o.get("killTokens");if(!(t instanceof JSONArray)||((JSONArray)t).size()>16)throw new IllegalArgumentException("Dữ liệu quái chưa hợp lệ.");for(Object token:(JSONArray)t){if(!(token instanceof String)||((String)token).length()>96||s.killTokens.contains(token))throw new IllegalArgumentException("Dữ liệu quái chưa hợp lệ.");s.killTokens.add((String)token);}return s;
    }
    public static JSONObject store(JSONObject all,State state) { Object copy=JSONValue.parse(all.toJSONString());if(!(copy instanceof JSONObject))throw new IllegalArgumentException("Dữ liệu game chưa hợp lệ.");JSONObject result=(JSONObject)copy;result.put(KEY,state.toJson());return result; }
    public static long credit(long gold,long reward,long limit) { if(gold<0||reward<0||gold>limit||reward>limit-gold)throw new IllegalArgumentException("Ví vàng đã gần đầy. Hãy dùng bớt vàng trước khi nhận thưởng.");return gold+reward; }
    public static boolean ready(State s) { return s.chapter<=10&&s.accepted&&s.step==DONE_STEPS[s.chapter-1]; }
    public static State accept(State in,long now,String session) { if(in.chapter>10||in.accepted||now<=0||session==null||session.isEmpty())throw new IllegalArgumentException("Không thể nhận chương này.");State s=in.copy();s.accepted=true;s.acceptedAt=now;s.step=0;s.progress=0;s.killTokens.clear();return s; }
    private static void advance(State s) { s.step++;s.progress=0; }
    public static State event(State in,String kind,int target,int map,String token,long now,String session) {
        State s=expire(in,now,session,false);if(!s.accepted||ready(s)||now<s.acceptedAt)return s;boolean match=false;
        switch(s.chapter) {
            case 1: match=s.step==1?kind.equals("mob")&&target==0&&map==0:kind.equals("talk")&&target==13&&map==5;break;
            case 2: if(s.step==0&&kind.equals("talk")&&target==13&&map==5){s.milkUntil=now+MILK_MS;s.milkSession=session;advance(s);return s;}match=s.step>=1&&s.milkUntil>now&&s.milkSession.equals(session)&&kind.equals("map")&&map==(s.step==1?29:s.step==2?1:5);break;
            case 3: match=s.step==1?kind.equals("mob")&&target==16&&map==27:kind.equals("talk")&&target==7&&map==0;break;
            case 4: match=s.step==0?kind.equals("map")&&map==47:kind.equals("talk")&&target==18&&map==46;break;
            case 5: return s;
            case 6: match=s.step==1?kind.equals("map")&&map==45:kind.equals("talk")&&target==18&&map==46;break;
            case 7: match=s.step==1?kind.equals("mob")&&target==2&&map==9:kind.equals("talk")&&target==8&&map==7;break;
            case 8: if(s.step==3)return s;match=s.step==1?kind.equals("map")&&map==10:kind.equals("talk")&&target==14&&map==13;break;
            case 9: match=s.step==0?kind.equals("talk")&&target==38&&map==102:s.step==1?kind.equals("map")&&map==92:s.step==2?kind.equals("map")&&map==93:kind.equals("talk")&&target==37&&map==102;break;
            case 10: match=s.step==0&&kind.equals("talk")&&target==13&&map==5;break;
        }
        if(!match)return s;
        if(kind.equals("mob")){if(token==null||token.isEmpty()||token.length()>96||s.killTokens.contains(token))return s;s.killTokens.add(token);while(s.killTokens.size()>16)s.killTokens.remove(0);int count=s.chapter==1?8:s.chapter==3?4:6;if(++s.progress<count)return s;}
        advance(s);if(s.chapter==2&&ready(s)){s.milkUntil=0;s.milkSession="";}return s;
    }
    public static State chooseGuru(State in,int choice) { if(!in.accepted||in.chapter!=8||in.step!=3||choice<0||choice>1)throw new IllegalArgumentException("Chưa đến phần hội thoại Guru.");State s=in.copy();if(choice==0)advance(s);return s; }
    public static State answerQuiz(State in,int choice) { if(!in.accepted||in.chapter!=10||in.step!=1||choice<0||choice>2)throw new IllegalArgumentException("Chưa đến phần hỏi đáp Quy Lão.");State s=in.copy();int[] correct={1,2,0};if(choice!=correct[s.progress]){s.progress=0;return s;}s.progress++;if(s.progress==3)advance(s);return s; }
    public static State claim(State in) { if(!ready(in)||(in.claimedMask&(1<<(in.chapter-1)))!=0)throw new IllegalArgumentException("Chương chưa hoàn thành hoặc đã nhận thưởng.");State s=in.copy();s.claimedMask|=1<<(s.chapter-1);s.chapter++;s.accepted=false;s.step=0;s.progress=0;s.acceptedAt=0;s.milkUntil=0;s.milkSession="";s.killTokens.clear();return s; }
    public static State resetMilk(State in) { if(in.chapter!=2||!in.accepted||ready(in))throw new IllegalArgumentException("Bài giao sữa chưa nhận hoặc đã hoàn thành. Bài hoàn thành được giữ để trả thưởng.");State s=in.copy();s.step=0;s.progress=0;s.milkUntil=0;s.milkSession="";return s; }
    private static void clearMemory(State s) { s.memoryLength=0;s.memoryStep=0;s.memory=new int[0];s.memoryStarted=0;s.memoryLastAnswer=0;s.memorySession="";s.memoryChapter=false; }
    private static void clearRadar(State s) { s.radarRoute=-1;s.radarStep=0;s.radarX=new int[0];s.radarY=new int[0];s.radarStarted=0;s.radarSession=""; }
    public static State expire(State in,long now,String session,boolean dead) { State s=in.copy();if(s.chapter==2&&s.accepted&&s.milkUntil>0&&(dead||!s.milkSession.equals(session)||now>=s.milkUntil)) {s.step=0;s.progress=0;s.milkUntil=0;s.milkSession="";}if(s.memoryLength>0&&(dead||!s.memorySession.equals(session)||now-s.memoryStarted>=MEMORY_MS))clearMemory(s);if(s.radarRoute>=0&&(dead||!s.radarSession.equals(session)))clearRadar(s);return s; }
    public static State startMemory(State in,int length,long now,String session,long seed,boolean chapter) {
        if(length<3||length>5||now<=0||session==null||session.isEmpty()||chapter&&(!in.accepted||in.chapter!=5||in.step!=0||length!=3))throw new IllegalArgumentException("Bài nhớ nhịp chưa hợp lệ.");State s=in.copy();clearRadar(s);s.memoryLength=length;s.memoryStep=0;s.memoryStarted=now;s.memoryLastAnswer=now;s.memorySession=session;s.memoryChapter=chapter;s.memory=new int[length];Random r=new Random(seed);for(int i=0;i<length;i++)s.memory[i]=r.nextInt(3);return s;
    }
    public static State answerMemory(State in,int choice,long now,String session) {
        State s=expire(in,now,session,false);if(s.memoryLength==0||s.memoryStep>=s.memoryLength||choice<0||choice>2||!s.memorySession.equals(session))throw new IllegalArgumentException("Bài nhớ nhịp đã hết hạn. Có thể bắt đầu lại miễn phí.");
        if(now-s.memoryLastAnswer<MENU_GAP_MS)throw new IllegalArgumentException("Chờ một nhịp rồi chọn tiếp.");s.memoryLastAnswer=now;
        if(choice!=s.memory[s.memoryStep]){clearMemory(s);return s;}s.memoryStep++;
        if(s.memoryStep==s.memoryLength){long elapsed=Math.max(1,now-s.memoryStarted);if(s.memoryLength>s.memoryBestLength||s.memoryLength==s.memoryBestLength&&(s.memoryBest==0||elapsed<s.memoryBest)){s.memoryBestLength=s.memoryLength;s.memoryBest=elapsed;}if(s.memoryChapter)advance(s);}return s;
    }
    public static State finishMemory(State in) { State s=in.copy();clearMemory(s);return s; }
    public static State startRadar(State in,int route,long now,String session,int[] xs,int[] ys) {
        if(route<0||route>=3||route==2&&in.chapter<=7||xs==null||ys==null||xs.length!=3||ys.length!=3||now<=0||session==null||session.isEmpty())throw new IllegalArgumentException("Tuyến Radar chưa mở hoặc thiếu điểm đứng hợp lệ.");for(int i=0;i<3;i++)if(xs[i]<0||ys[i]<0||xs[i]>65535||ys[i]>65535)throw new IllegalArgumentException("Điểm quét chưa hợp lệ.");State s=in.copy();clearMemory(s);s.radarRoute=route;s.radarStep=0;s.radarStarted=now;s.radarSession=session;s.radarX=xs.clone();s.radarY=ys.clone();return s;
    }
    public static State scanRadar(State in,int map,int x,int y,long now,String session) {
        State s=expire(in,now,session,false);if(s.radarRoute<0||s.radarStep>=3||!s.radarSession.equals(session))throw new IllegalArgumentException("Chưa có chuyến Radar đang chạy.");int i=s.radarStep;if(map!=RADAR_ROUTES[s.radarRoute][i])throw new IllegalArgumentException("Hãy đến đúng map tiếp theo trong sổ.");long dx=(long)x-s.radarX[i],dy=(long)y-s.radarY[i];if(dx*dx+dy*dy>96L*96L)throw new IllegalArgumentException("Hãy tiến gần điểm tín hiệu ghi trong sổ (96px).");s.radarStep++;if(s.radarStep==3){long time=Math.max(1,now-s.radarStarted);if(s.radarBest==0||time<s.radarBest)s.radarBest=time;}return s;
    }
    public static State finishRadar(State in) { State s=in.copy();clearRadar(s);return s; }
    public static long day(long now) { return Instant.ofEpochMilli(now).atZone(DAY_ZONE).toLocalDate().toEpochDay(); }
    public static State dailyReward(State in,int bit,long now) { if(bit!=1&&bit!=2)throw new IllegalArgumentException("Phần thưởng chưa hợp lệ.");if(bit==1&&(in.radarRoute<0||in.radarStep!=3)||bit==2&&(in.memoryLength==0||in.memoryStep!=in.memoryLength||in.memoryChapter))throw new IllegalArgumentException("Chưa hoàn thành trò chơi.");State s=in.copy();long today=day(now);if(s.day!=today){s.day=today;s.dailyRewardMask=0;}if((s.dailyRewardMask&bit)!=0)throw new IllegalArgumentException("Đã nhận thưởng trò này hôm nay.");s.dailyRewardMask|=bit;return s; }
    public static boolean dailyAvailable(State s,int bit,long now) { return s.day!=day(now)||(s.dailyRewardMask&bit)==0; }
    public static String objective(State s) {
        if(s.chapter>10)return "Đã hoàn thành 10 chương. Hai trò chơi miễn phí vẫn mở mỗi ngày.";if(!s.accepted)return "Nhận chương để bắt đầu. Chỉ mục tiêu sau khi nhận mới được tính.";if(ready(s))return "Đủ mục tiêu. Gặp người giao ở đúng map rồi chọn Trả chương.";
        switch(s.chapter){
            case 1:return s.step==1?"Hạ Mộc nhân tại Làng Aru [0]: "+s.progress+"/8":"Nói Quy Lão tại Đảo Kamê [5].";
            case 2:return s.step==0?"Đến Quy Lão tại Kame [5] để bắt đầu giao sữa 180 giây.":"Giao sữa: đến "+(s.step==1?"Nam Kamê [29]":s.step==2?"Đồi hoa cúc [1]":"Đảo Kamê [5]")+" đúng thứ tự. Đường đi: Nam Kamê → Kame → Rừng xương → Rừng nấm → Thung lũng tre → Đồi hoa cúc, rồi quay lại. Hết giờ, chết hoặc thoát: thử lại miễn phí.";
            case 3:return s.step==1?"Hạ Heo rừng tại Rừng Bamboo [27]: "+s.progress+"/4":"Nói Bunma tại Làng Aru [0].";
            case 4:return s.step==0?"Đến Rừng Karin [47].":"Nói Thần Mèo Karin tại Tháp Karin [46]. Đi theo lối và điều kiện mở map vốn có.";
            case 5:return "Đến Karin tại Tháp Karin [46], chọn Luyện nhịp để nhớ 3 ký hiệu.";
            case 6:return s.step==1?"Từ cổng trên Tháp Karin [46] đến Điện Thần [45] để quan sát; rơi xuống đáy Điện Thần để về Karin trả bài.":"Nói Thần Mèo Karin tại Tháp Karin [46].";
            case 7:return s.step==1?"Hạ Lợn lòi tại Thị trấn Moori [9]: "+s.progress+"/6":"Nói Dende tại Làng Mori [7]. Dùng trạm tàu vũ trụ để đổi hành tinh.";
            case 8:return s.step==1?"Đến Thung lũng Namếc [10].":s.step==3?"Đứng gần Guru ở Đảo Guru [13], mở sổ và chọn Lời đáp.":"Nói Guru tại Đảo Guru [13].";
            case 9:return s.step==0?"Nói Calick tại Nhà Bunma [102]. NPC mở từ nhiệm vụ chính 21. Gặp Calick ở Rừng Bamboo / Rừng dương xỉ / Nam Kamê để đi tương lai.":s.step==1?"Khảo sát Thành phố phía đông [92]. Dùng Calick theo điều kiện tương lai sẵn có.":s.step==2?"Khảo sát Thành phố phía nam [93].":"Nói Bunma tương lai tại Nhà Bunma [102].";
            case 10:return s.step==0?"Nói Quy Lão tại Đảo Kamê [5].":"Đứng gần Quy Lão, chọn Hỏi đáp. Đã trả lời: "+s.progress+"/3.";
            default:return "";
        }
    }
}
