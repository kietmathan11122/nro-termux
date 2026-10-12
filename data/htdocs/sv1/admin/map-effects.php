<?php
declare(strict_types=1);
session_start();
require_once $_SERVER['DOCUMENT_ROOT'].'/sv1/config.php';
require_once __DIR__.'/game_bridge.php';
$auth=$pdo->prepare('SELECT is_admin FROM account WHERE id=?');
$auth->execute([$_SESSION['account_id']??-1]);
if ((int)$auth->fetchColumn()!==1) { http_response_code(403); exit('Chỉ quản trị viên được chỉnh EFF.'); }
$_SESSION['map_eff_csrf']??=bin2hex(random_bytes(32));
$catalog=json_decode(ltrim(file_get_contents(__DIR__.'/effect-assets/catalog.json'),"\xEF\xBB\xBF"),true,512,JSON_THROW_ON_ERROR);
$assets=array_column($catalog,null,'id');
function eff_decode(string $bytes): array {
 $pos=0;$read=function()use(&$pos,$bytes){if($pos+2>strlen($bytes))throw new RuntimeException('File EFF bị thiếu dữ liệu');$n=unpack('n',substr($bytes,$pos,2))[1];$pos+=2;return $n;};
 $count=$read();$out=[];for($i=0;$i<$count;$i++){ $pair=[];for($j=0;$j<2;$j++){ $len=$read();if($pos+$len>strlen($bytes))throw new RuntimeException('EFF không hợp lệ');$pair[]=substr($bytes,$pos,$len);$pos+=$len; }$out[]=$pair; }
 if($pos!==strlen($bytes))throw new RuntimeException('EFF có dữ liệu ngoài định dạng');return $out;
}
function eff_encode(array $pairs): string {if(count($pairs)>32767)throw new RuntimeException('Quá nhiều EFF');$s=pack('n',count($pairs));foreach($pairs as $p)foreach($p as $v){if(strlen($v)>65535)throw new RuntimeException('EFF quá dài');$s.=pack('n',strlen($v)).$v;}return $s;}
function number_field(array $r,string $key,int $min,int $max,int $default): int {$v=filter_var($r[$key]??$default,FILTER_VALIDATE_INT);if($v===false||$v<$min||$v>$max)throw new RuntimeException('Giá trị không hợp lệ: '.$key);return $v;}
if(isset($_GET['api'])) {
 header('Content-Type: application/json; charset=utf-8');
 try {
  if($_GET['api']==='maps'){echo json_encode($pdo->query('SELECT id,NAME AS name FROM map_template ORDER BY id')->fetchAll(PDO::FETCH_ASSOC));exit;}
  $map=filter_var($_GET['map']??null,FILTER_VALIDATE_INT);if($map===false||$map<0||$map>32767)throw new RuntimeException('Map không hợp lệ');
  $q=$pdo->prepare('SELECT id FROM map_template WHERE id=?');$q->execute([$map]);if($q->fetchColumn()===false)throw new RuntimeException('Map không tồn tại');
  $path=game_root().'/data/map/eff_map/'.$map;
  if($_GET['api']==='load'){
   $bytes=is_file($path)?file_get_contents($path):pack('n',0);$rows=[];$other=0;
   foreach(eff_decode($bytes) as [$key,$value]){if($key==='panel_fx')$rows[]=json_decode($value,true,512,JSON_THROW_ON_ERROR);else $other++;}
   echo json_encode(['rows'=>$rows,'revision'=>hash('sha256',$bytes),'other'=>$other]);exit;
  }
  if($_GET['api']!=='save'||$_SERVER['REQUEST_METHOD']!=='POST'){http_response_code(405);exit;}
  if(!hash_equals($_SESSION['map_eff_csrf'],$_SERVER['HTTP_X_CSRF_TOKEN']??'')){http_response_code(403);exit;}
  if((int)($_SERVER['CONTENT_LENGTH']??0)>65536)throw new RuntimeException('Dữ liệu quá lớn');
  $body=json_decode(file_get_contents('php://input'),true,32,JSON_THROW_ON_ERROR);$rows=$body['rows']??null;
  if(!is_array($rows)||count($rows)>32)throw new RuntimeException('Tối đa 32 EFF mỗi map');
  $clean=[];foreach($rows as $r){
   if(!is_array($r)||!isset($assets[$r['id']??'']))throw new RuntimeException('EFF không có trong danh mục');$a=$assets[$r['id']];
   $v=['id'=>$a['id'],'path'=>$a['path'],'weather'=>$a['weather']];
   foreach(['x'=>[0,32767,300],'y'=>[0,32767,250],'layer'=>[0,1,0],'cols'=>[1,32,$a['cols']],'rows'=>[1,64,$a['rows']],'start'=>[0,2047,0],'frames'=>[1,128,$a['frames']],'delay'=>[80,5000,350],'count'=>[1,24,1],'motion'=>[0,3,0]] as $k=>$lim)$v[$k]=number_field($r,$k,...$lim);
   if($v['start']+$v['frames']>$v['cols']*$v['rows'])throw new RuntimeException('Khung vượt kích thước atlas');$clean[]=$v;
  }
  $lock=fopen($path.'.lock','c');if(!$lock||!flock($lock,LOCK_EX))throw new RuntimeException('Không khóa được file');
  try {
   $bytes=is_file($path)?file_get_contents($path):pack('n',0);if(!hash_equals(hash('sha256',$bytes),(string)($body['revision']??''))){http_response_code(409);throw new RuntimeException('Map vừa được sửa ở nơi khác. Tải lại trước khi lưu.');}
   $pairs=array_values(array_filter(eff_decode($bytes),fn($p)=>$p[0]!=='panel_fx'));
   foreach($clean as $r)$pairs[]=['panel_fx',json_encode($r,JSON_THROW_ON_ERROR)];$new=eff_encode($pairs);
   $backup=game_root().'/backups/panel-effects';if(!is_dir($backup)&&!mkdir($backup,0775,true))throw new RuntimeException('Không tạo được thư mục sao lưu');
   if(file_put_contents($backup.'/'.$map.'-'.date('Ymd-His').'-'.bin2hex(random_bytes(3)).'.bin',$bytes)===false)throw new RuntimeException('Sao lưu thất bại');
   $tmp=tempnam(dirname($path),'eff');try{if(file_put_contents($tmp,$new)!==strlen($new)||!rename($tmp,$path))throw new RuntimeException('Không lưu được EFF');}finally{if(is_file($tmp))unlink($tmp);}
   echo json_encode(['revision'=>hash('sha256',$new),'message'=>'Đã lưu. Nhân vật vào lại map để nhận EFF mới.']);
  }finally{flock($lock,LOCK_UN);fclose($lock);}
 }catch(Throwable $e){if(http_response_code()<400)http_response_code(400);echo json_encode(['error'=>$e->getMessage()]);}exit;
}
?><!doctype html><html lang="vi"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>EFF bản đồ</title>
<style>
*{box-sizing:border-box}body{margin:0;background:#101923;color:#e7eff6;font:16px system-ui}header{padding:18px 24px;background:#182737;display:flex;gap:24px;align-items:center}a{color:#72d8d1}h1{font-size:23px;margin:0}main{padding:20px;display:grid;grid-template-columns:minmax(320px,1fr) minmax(380px,1fr);gap:24px}section{min-width:0}input,select,button{font:inherit;color:inherit;background:#223649;border:1px solid #557084;border-radius:6px;padding:8px;max-width:100%}button{cursor:pointer}button:hover{border-color:#64dbbc}button:disabled{opacity:.5;cursor:wait}label{display:inline-flex;flex-direction:column;gap:5px;font-size:14px}#catalog{display:grid;grid-template-columns:repeat(auto-fill,minmax(140px,1fr));gap:10px;margin-top:15px;max-height:75vh;overflow:auto}.card{padding:8px;overflow:hidden;text-align:left}.card img{width:100%;height:90px;object-fit:contain;background:#0b1118}.card small{display:block;overflow-wrap:anywhere}#selected article{border:1px solid #426079;border-radius:10px;padding:12px;margin:12px 0;background:#172735}.fields{display:flex;flex-wrap:wrap;gap:8px}.fields input{width:88px}.rowhead{display:flex;justify-content:space-between;gap:8px;margin-bottom:10px}.preview{height:100px;max-width:100%;object-fit:contain;background:#0b1118}#status{min-height:28px;color:#8fe0c1}p{color:#b9cbd8;font-size:14px}.toolbar{display:flex;gap:10px;flex-wrap:wrap}@media(max-width:850px){main{grid-template-columns:1fr}#catalog{max-height:45vh}}
</style><header><a href="panel.php">← Panel</a><h1>EFF bản đồ</h1></header><main>
<section><div class="toolbar"><input id="search" aria-label="Tìm EFF" placeholder="Tìm tên hoặc ID EFF"><select id="group" aria-label="Nhóm EFF"><option value="">Tất cả nhóm</option></select></div><p>Bấm ảnh để thêm. Atlas nhiều hình: chỉnh ô bắt đầu và số khung liên tiếp; đánh số từ 0, trái sang phải.</p><div id="catalog"></div></section>
<section><div class="toolbar"><label>Bản đồ<select id="map"><option value="">Đang tải map…</option></select></label><button id="reload">Tải lại</button><button id="save" disabled>Lưu EFF vào map</button></div><p id="existing"></p><div id="status" role="status"></div><div id="selected"></div></section></main>
<?php if (isset($_GET['embed'])): ?>
<style>
body{background:#f3f8ff;color:#193e64}header{display:none}main{padding:16px;gap:18px}
input,select,button{background:#fff;color:#193e64;border-color:#9ebcdb}
.card img,.preview,canvas{background:#e8f0fa!important}
#selected article{background:#fff;border-color:#9ebcdb}p{color:#476b8d}#status{color:#087d55}
</style>
<?php endif; ?>
<script>
const assets=<?=json_encode($catalog,JSON_HEX_TAG|JSON_HEX_AMP|JSON_HEX_APOS|JSON_HEX_QUOT)?>,csrf=<?=json_encode($_SESSION['map_eff_csrf'])?>;
const $=id=>document.getElementById(id);let rows=[],revision='',activeMap='',dirty=false,loading=false;
const say=t=>$('status').textContent=t;
async function api(action,map,body){const r=await fetch(`map-effects.php?api=${action}&map=${encodeURIComponent(map||'')}`,body?{method:'POST',headers:{'Content-Type':'application/json','X-CSRF-Token':csrf},body:JSON.stringify(body)}:{});const j=await r.json();if(!r.ok)throw Error(j.error||'Không thể kết nối panel');return j;}
for(const g of new Set(assets.map(a=>a.group))){const o=new Option(g,g);$('group').add(o);}
function catalog(){const q=$('search').value.toLowerCase(),group=$('group').value;$('catalog').replaceChildren();for(const a of assets.filter(a=>(!group||a.group===group)&&(a.name+' '+a.id).toLowerCase().includes(q))){const b=document.createElement('button');b.className='card';const im=document.createElement('img');im.src=a.image;im.alt=a.name;im.loading='lazy';const t=document.createElement('small');t.textContent=a.name;b.append(im,t);b.onclick=()=>{if(!activeMap||loading)return say('Hãy chọn map trước.');if(rows.length>=32)return say('Tối đa 32 EFF.');rows.push({...a,x:300,y:250,layer:0,start:0,delay:350,count:1,motion:0});dirty=true;render();};$('catalog').append(b);}}
function render(){ $('selected').replaceChildren();rows.forEach((r,i)=>{const a=assets.find(a=>a.id===r.id);const box=document.createElement('article'),head=document.createElement('div');head.className='rowhead';const title=document.createElement('strong');title.textContent=a?.name||r.id;const del=document.createElement('button');del.textContent='Gỡ';del.onclick=()=>{rows.splice(i,1);dirty=true;render();};head.append(title,del);box.append(head);if(a){const img=document.createElement('img');img.src=a.image;img.className='preview';img.alt=a.name;box.append(img);}const fs=document.createElement('div');fs.className='fields';if((a?.weather??-1)<0){for(const [k,text,min,max] of [['x','Tọa độ X',0,32767],['y','Tọa độ Y',0,32767],['cols','Số cột',1,32],['rows','Số hàng',1,64],['start','Ô bắt đầu',0,2047],['frames','Số khung',1,128],['delay','Mỗi khung (ms)',80,5000],['count','Số lượng',1,24]]){const l=document.createElement('label');l.textContent=text;const n=document.createElement('input');n.type='number';n.min=min;n.max=max;n.value=r[k];n.oninput=()=>{r[k]=Number(n.value);dirty=true;};l.append(n);fs.append(l);}for(const [k,text,opts] of [['layer','Lớp',['Sau nhân vật','Trước nhân vật']],['motion','Chuyển động',['Tại chỗ','Rơi xuống','Bay ngang','Bay lên']]]){const l=document.createElement('label');l.textContent=text;const s=document.createElement('select');opts.forEach((t,n)=>s.add(new Option(t,n)));s.value=r[k];s.onchange=()=>{r[k]=Number(s.value);dirty=true;};l.append(s);fs.append(l);}}else{const p=document.createElement('p');p.textContent='Thời tiết phủ toàn màn hình.';fs.append(p);}box.append(fs);$('selected').append(box);});if(!rows.length)$('selected').textContent='Chưa thêm EFF từ panel cho map này.';}
async function loadMap(id){if(dirty&&!confirm('Bỏ thay đổi chưa lưu?')){$('map').value=activeMap;return;}loading=true;$('save').disabled=true;try{const j=await api('load',id);activeMap=id;rows=j.rows;revision=j.revision;dirty=false;render();$('existing').textContent=`Giữ nguyên ${j.other} cấu hình EFF có sẵn. Lưu sẽ chỉ thay phần EFF của panel.`;say('Đã tải map.');}catch(e){say(e.message);}finally{loading=false;$('save').disabled=!activeMap;}}
$('search').oninput=catalog;$('group').onchange=catalog;$('map').onchange=()=>loadMap($('map').value);$('reload').onclick=()=>loadMap($('map').value);
$('save').onclick=async()=>{if(loading||!activeMap)return;loading=true;$('save').disabled=true;$('map').disabled=true;try{const j=await api('save',activeMap,{rows,revision});revision=j.revision;dirty=false;say(j.message);}catch(e){say(e.message);}finally{loading=false;$('save').disabled=false;$('map').disabled=false;}};
window.addEventListener('beforeunload',e=>{if(dirty){e.preventDefault();e.returnValue='';}});catalog();api('maps').then(ms=>{$('map').replaceChildren(new Option('Chọn map',''));for(const m of ms)$('map').add(new Option(`[${m.id}] ${m.name}`,m.id));}).catch(e=>say(e.message));
</script>
<script>
// Display the selected atlas cell at animation speed; clicking the sheet chooses its first cell.
let previews=[];const originalRender=render;
render=function(){originalRender();previews=[];document.querySelectorAll('#selected article').forEach((box,i)=>{
 const r=rows[i],a=assets.find(a=>a.id===r.id),im=box.querySelector('img');if(!im||!a||a.weather>=0)return;
 const c=document.createElement('canvas');c.width=160;c.height=120;c.style.cssText='display:block;background:#0b1118;border:1px solid #557084;border-radius:6px;margin:8px 0';c.setAttribute('aria-label','Xem chuyển động EFF');box.insertBefore(c,im);previews.push({c,im,r});
 im.title='Bấm vào một ô để chọn';im.style.cursor='crosshair';im.onclick=e=>{if(loading)return;const b=im.getBoundingClientRect();r.start=Math.min(r.cols*r.rows-1,Math.floor((e.clientY-b.top)/b.height*r.rows)*r.cols+Math.floor((e.clientX-b.left)/b.width*r.cols));r.frames=1;dirty=true;render();};
 });};
setInterval(()=>{for(const {c,im,r} of previews){if(!im.complete||!im.naturalWidth||r.cols<1||r.rows<1||r.frames<1)continue;const w=im.naturalWidth/r.cols,h=im.naturalHeight/r.rows,f=r.start+Math.floor(performance.now()/Math.max(80,r.delay))%r.frames;if(f>=r.cols*r.rows)continue;const ctx=c.getContext('2d'),s=Math.min(c.width/w,c.height/h);ctx.clearRect(0,0,c.width,c.height);ctx.drawImage(im,f%r.cols*w,Math.floor(f/r.cols)*h,w,h,(c.width-w*s)/2,(c.height-h*s)/2,w*s,h*s);}},100);
const originalLoad=loadMap;loadMap=async function(id){if(loading){$('map').value=activeMap;return;}if(!id){$('map').value=activeMap;return;}const old=activeMap;$('map').disabled=true;$('selected').inert=true;try{await originalLoad(id);if(activeMap!==id)$('map').value=old;}finally{$('map').disabled=false;$('selected').inert=false;}};
const originalSave=$('save').onclick;$('save').onclick=async()=>{$('selected').inert=true;try{await originalSave();}finally{$('selected').inert=false;}};
</script></html>
