<?php
session_start();
require_once $_SERVER['DOCUMENT_ROOT'] . "/sv1/config.php";
require_once __DIR__ . "/game_bridge.php";

if (!isset($_SESSION['account_id'])) {
    header("Location: /sv1/trangchu.php");
    exit;
}

$account_id = $_SESSION['account_id'];

$stmt = $pdo->prepare("SELECT * FROM account WHERE id = :id LIMIT 1");
$stmt->bindParam(":id", $account_id);
$stmt->execute();
$account = $stmt->fetch(PDO::FETCH_ASSOC);

if (!$account) {
    session_destroy();
    header("Location: /sv1/trangchu.php");
    exit;
}

// ── AJAX CUSTOM DROPS & MOBS ──
if (isset($_GET['action']) && $_GET['action'] === 'get_custom_drops') {
    header('Content-Type: application/json; charset=utf-8');
    $catalogPath = __DIR__ . '/../default_drops_catalog.json';
    if (!file_exists($catalogPath)) {
        $catalogPath = 'C:/xampp1/htdocs/sv1/default_drops_catalog.json';
    }
    $catalog = [];
    if (file_exists($catalogPath)) {
        $catalog = json_decode(@file_get_contents($catalogPath), true) ?: [];
    }
    echo json_encode([
        'custom_drops' => get_custom_drops(),
        'default_catalog' => $catalog
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

if (isset($_GET['action']) && $_GET['action'] === 'get_mob_templates') {
    header('Content-Type: application/json; charset=utf-8');
    try {
        $stmt = $pdo->query("SELECT id, name FROM mob_template ORDER BY id ASC");
        echo json_encode($stmt->fetchAll(PDO::FETCH_ASSOC), JSON_UNESCAPED_UNICODE);
    } catch (Exception $e) {
        echo json_encode([]);
    }
    exit;
}

// ── AJAX SEARCH ICONS (ITEM TEMPLATES) ──
if (isset($_GET['action']) && $_GET['action'] === 'search_icons' && isset($_GET['query'])) {
    header('Content-Type: application/json; charset=utf-8');
    $query = trim($_GET['query']);
    if ($query === '') {
        echo json_encode([]);
        exit;
    }
    
    try {
        $searchVal = '%' . $query . '%';
        $stmtSearch = $pdo->prepare("SELECT id, name, icon_id FROM item_template WHERE name LIKE :search OR id = :id ORDER BY id ASC LIMIT 100");
        $stmtSearch->bindValue(':search', $searchVal);
        $stmtSearch->bindValue(':id', (int)$query, PDO::PARAM_INT);
        $stmtSearch->execute();
        $results = $stmtSearch->fetchAll(PDO::FETCH_ASSOC);
        echo json_encode($results);
    } catch (Exception $e) {
        echo json_encode([]);
    }
    exit;
}

$stmt2 = $pdo->prepare("SELECT * FROM player WHERE account_id = :aid LIMIT 1");
$stmt2->bindParam(":aid", $account_id);
$stmt2->execute();
$player = $stmt2->fetch(PDO::FETCH_ASSOC);

$power = 0;
if (!empty($player['data_point'])) {
    $clean = trim($player['data_point'], "[]");
    $arr = explode(',', $clean);
    $power = isset($arr[1]) ? (int)$arr[1] : 0;
}

$gender = $player['gender'] ?? 0;
$genderIcon = '/img/24.jpg';
if ($gender == 1) { $genderIcon = '/img/25.jpg'; }
elseif ($gender == 2) { $genderIcon = '/img/26.jpg'; }

$playerName = $player['name'] ?? 'Vô Danh';
function format_fish_amount($vnd): string {
    return number_format((float)$vnd, 0, ',', '.');
}

$vnd = format_fish_amount($account['vnd'] ?? 0);
$active = $account['active'] ?? 0;
$is_admin = (int)($account['is_admin'] ?? 0);
$csrfToken = ensure_csrf_token();
if ($_SERVER['REQUEST_METHOD'] === 'POST' && ($_POST['action'] ?? '') === 'panel_set_event') {
    require_csrf_token();
    header('Content-Type: application/json; charset=utf-8');
    if ($is_admin !== 1) { http_response_code(403); echo json_encode(['error'=>'Chỉ admin được đổi sự kiện.']); exit; }
    $events = [1=>'Tết Nguyên Đán',2=>'Mùa Hè',3=>'Halloween',4=>'Giáng sinh',5=>'Hùng Vương',6=>'Trung Thu',9=>'Tắt toàn bộ sự kiện',10=>'Cô Hồn'];
    $id = filter_var($_POST['event_id'] ?? '', FILTER_VALIDATE_INT);
    if ($id === false || !isset($events[$id])) { http_response_code(400); echo json_encode(['error'=>'Sự kiện không hợp lệ.']); exit; }
    if (!append_panel_command('SET_EVENT|' . $id)) { http_response_code(500); echo json_encode(['error'=>'Không gửi được lệnh tới server.']); exit; }
    echo json_encode(['message'=>'Đã gửi lệnh: '.$events[$id].'. Server đang chạy sẽ áp dụng ở lượt xử lý kế tiếp và thông báo trong game.'], JSON_UNESCAPED_UNICODE);
    exit;
}

function get_current_exp_server(): int {
    $statusFile = panel_exp_status_file();
    if (is_file($statusFile)) {
        $value = (int)trim((string)@file_get_contents($statusFile));
        if ($value > 0) return $value;
    }
    $configFile = panel_config_file();
    if (is_file($configFile)) {
        foreach (@file($configFile, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES) ?: [] as $line) {
            if (preg_match('/^\s*server\.expserver\s*=\s*(\d+)/', $line, $m)) {
                $value = (int)$m[1];
                if ($value > 0) return $value;
            }
        }
    }
    return 10;
}

function get_current_drop_server(): int {
    $statusFile = panel_drop_status_file();
    if (is_file($statusFile)) {
        $value = (int)trim((string)@file_get_contents($statusFile));
        if ($value > 0) return $value;
    }
    $configFile = panel_config_file();
    if (is_file($configFile)) {
        foreach (@file($configFile, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES) ?: [] as $line) {
            if (preg_match('/^\s*server\.dropserver\s*=\s*(\d+)/', $line, $m)) {
                $value = (int)$m[1];
                if ($value > 0) return $value;
            }
        }
    }
    return 5;
}

function get_current_admin_mode(): bool {
    $statusFile = panel_admin_mode_status_file();
    if (is_file($statusFile)) {
        return trim((string)@file_get_contents($statusFile)) === '1';
    }
    return false;
}

function get_current_xnxx_mode(): bool {
    $statusFile = panel_xnxx_status_file();
    if (is_file($statusFile)) {
        return trim((string)@file_get_contents($statusFile)) === '1';
    }
    return false;
}

function get_current_boss_persist_mode(): bool {
    $statusFile = panel_boss_persist_status_file();
    if (is_file($statusFile)) {
        return trim((string)@file_get_contents($statusFile)) === '1';
    }
    return false;
}
function get_custom_drops(): array {
    $file = panel_custom_drops_file();
    $default = [
        'mob_drops' => [],
        'boss_drops' => [],
        'blocked_boss_drops' => [],
        'blocked_mob_drops' => [],
        'blocked_events' => []
    ];
    if (!$file || !is_file($file)) {
        return $default;
    }
    $raw = @file_get_contents($file);
    if (!is_string($raw) || trim($raw) === '') {
        return $default;
    }
    $data = json_decode($raw, true);
    if (!is_array($data)) {
        return $default;
    }
    return array_merge($default, [
        'mob_drops' => is_array($data['mob_drops'] ?? null) ? $data['mob_drops'] : [],
        'boss_drops' => is_array($data['boss_drops'] ?? null) ? $data['boss_drops'] : [],
        'blocked_boss_drops' => is_array($data['blocked_boss_drops'] ?? null) ? $data['blocked_boss_drops'] : [],
        'blocked_mob_drops' => is_array($data['blocked_mob_drops'] ?? null) ? $data['blocked_mob_drops'] : [],
        'blocked_events' => is_array($data['blocked_events'] ?? null) ? $data['blocked_events'] : [],
    ]);
}

function save_custom_drops(array $data): bool {
    $file = panel_custom_drops_file();
    $json = json_encode($data, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    if ($json === false) return false;
    
    @file_put_contents($file, $json);
    $rootFile = game_file_path('custom_drops.json');
    if ($rootFile !== $file) {
        @file_put_contents($rootFile, $json);
    }
    $htdocsFile = dirname(__DIR__) . DIRECTORY_SEPARATOR . 'custom_drops.json';
    if ($htdocsFile !== $file && $htdocsFile !== $rootFile) {
        @file_put_contents($htdocsFile, $json);
    }
    
    append_panel_command('RELOAD_CUSTOM_DROPS');
    return true;
}

function get_panel_bosses(): array {
    global $pdo;
    $rawList = [];
    $statusFile = panel_boss_status_file();
    if (is_file($statusFile)) {
        $raw = @file_get_contents($statusFile);
        if (is_string($raw) && trim($raw) !== '') {
            $data = json_decode($raw, true);
            if (is_array($data) && !empty($data['bosses']) && is_array($data['bosses'])) {
                $rawList = $data['bosses'];
            }
        }
    }

    $catalogFile = function_exists('panel_boss_catalog_file') ? panel_boss_catalog_file() : '';
    if ($catalogFile && is_file($catalogFile)) {
        $cRaw = @file_get_contents($catalogFile);
        if (is_string($cRaw) && trim($cRaw) !== '') {
            $cData = json_decode($cRaw, true);
            if (is_array($cData) && !empty($cData['bosses']) && is_array($cData['bosses'])) {
                $seenKeys = [];
                foreach ($rawList as $b) {
                    $k = (string)($b['configKey'] ?? ((int)($b['id'] ?? 0) . '_' . (int)($b['level'] ?? 0)));
                    $seenKeys[$k] = true;
                }
                foreach ($cData['bosses'] as $cb) {
                    $k = (string)($cb['configKey'] ?? ((int)($cb['id'] ?? 0) . '_' . (int)($cb['level'] ?? 0)));
                    if (!isset($seenKeys[$k])) {
                        $seenKeys[$k] = true;
                        $rawList[] = $cb;
                    }
                }
            }
        }
    }

    if (empty($rawList)) {
        return [];
    }

    $bosses = [];
    $bossIndex = [];
    foreach ($rawList as $boss) {
        if (!is_array($boss)) {
            continue;
        }
        $row = [
            'id' => (int)($boss['id'] ?? 0),
            'configKey' => (string)($boss['configKey'] ?? ((int)($boss['id'] ?? 0) . '_' . (int)($boss['level'] ?? 0))),
            'spawnable' => true,
            'level' => (int)($boss['level'] ?? 0),
            'name' => (string)($boss['name'] ?? ('Boss ' . (int)($boss['id'] ?? 0))),
            'state' => (string)($boss['state'] ?? 'Chưa xuất hiện'),
            'status' => (string)($boss['status'] ?? ''),
            'mapName' => (string)($boss['mapName'] ?? 'Chưa xuất hiện'),
            'mapId' => (int)($boss['mapId'] ?? -1),
            'zoneId' => (int)($boss['zoneId'] ?? -1),
            'hp' => (int)($boss['hp'] ?? 0),
            'maxHp' => (int)($boss['maxHp'] ?? 0),
            'dame' => (int)($boss['dame'] ?? 1),
            'armor' => (int)($boss['armor'] ?? 0),
            'targetHpPercent' => max(0, min(100, (int)($boss['targetHpPercent'] ?? 0))),
            'skills' => is_array($boss['skills'] ?? null) ? $boss['skills'] : [],
            'outfit0' => (int)($boss['outfit0'] ?? 0),
        ];
        $uniqueKey = $row['configKey'];
        if (isset($bossIndex[$uniqueKey])) {
            $index = $bossIndex[$uniqueKey];
            if (($bosses[$index]['state'] ?? '') !== 'Sống' && $row['state'] === 'Sống') {
                $bosses[$index] = $row;
            }
            continue;
        }
        $bossIndex[$uniqueKey] = count($bosses);
        $bosses[] = $row;
    }

    $headIds = array_values(array_unique(array_filter(array_column($bosses, 'outfit0'))));
    $avatars = [];
    if (!empty($headIds) && isset($pdo)) {
        try {
            $placeholders = implode(',', array_fill(0, count($headIds), '?'));
            $stmt = $pdo->prepare("SELECT head_id, avatar_id FROM head_avatar WHERE head_id IN ($placeholders)");
            $stmt->execute($headIds);
            foreach ($stmt->fetchAll(PDO::FETCH_ASSOC) as $row) {
                $avatars[(int)$row['head_id']] = (int)$row['avatar_id'];
            }
        } catch (Exception $e) {
        }
    }
    foreach ($bosses as &$boss) {
        $boss['avatarId'] = panel_boss_avatar_id($boss, $avatars);
    }
    unset($boss);

    return $bosses;
}

function panel_boss_avatar_id(array $boss, array $avatars): int {
    $head = (int)($boss['outfit0'] ?? 0);
    $name = (string)($boss['name'] ?? '');
    $overrides = [
        291 => 2893,
        294 => 2924,
        1501 => 13488,
    ];

    if (stripos($name, 'super broly') !== false) {
        return 2924;
    }
    if (stripos($name, 'broly') !== false) {
        return 2893;
    }
    return $overrides[$head] ?? ($avatars[$head] ?? 1);
}

function panel_boss_short_map(array $boss): string {
    $mapName = (string)($boss['mapName'] ?? '');
    $zoneId = (int)($boss['zoneId'] ?? -1);
    if (preg_match('/^(.*?)\s*\(khu\s+\d+\)\s*$/u', $mapName, $m)) {
        $mapName = trim($m[1]);
    }
    if ($mapName === '') {
        $mapName = 'Chua xuat hien';
    }
    return $zoneId >= 0 ? $mapName . ' - ' . $zoneId : $mapName;
}

// ── ĐIỂM DANH LOGIC ──
$diemDanhMsg    = '';
$diemDanhResult = '';
$todayStr       = date('Y-m-d');

$stmtCheckLast = $pdo->prepare("SELECT last_check FROM account WHERE id = :id LIMIT 1");
$stmtCheckLast->bindParam(":id", $account_id);
$stmtCheckLast->execute();
$lastCheckRow = $stmtCheckLast->fetch(PDO::FETCH_ASSOC);
$lastCheckRaw = $lastCheckRow['last_check'] ?? null;

if ($lastCheckRaw === null || $lastCheckRaw === '0000-00-00 00:00:00' || $lastCheckRaw === '') {
    $canDiemDanh = true;
} else {
    $lastCheckDate = date('Y-m-d', strtotime($lastCheckRaw));
    $canDiemDanh   = ($lastCheckDate < $todayStr);
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'diem_danh') {
    require_csrf_token();
    $stmtCheck = $pdo->prepare("SELECT last_check, vnd, tongnap FROM account WHERE id = :id LIMIT 1");
    $stmtCheck->bindParam(":id", $account_id);
    $stmtCheck->execute();
    $freshAccount  = $stmtCheck->fetch(PDO::FETCH_ASSOC);
    $freshLastCheck = $freshAccount['last_check'] ?? null;

    $alreadyDone = false;
    if ($freshLastCheck && $freshLastCheck !== '0000-00-00 00:00:00') {
        $alreadyDone = (date('Y-m-d', strtotime($freshLastCheck)) === $todayStr);
    }

    if ($alreadyDone) {
        header("Location: " . current_public_path() . "?dd=already");
        exit;
    } else {
        $reward   = rand(1, 3000);
        $newvnd  = ($freshAccount['vnd']  ?? 0) + $reward;
        $newtongnap = ($freshAccount['tongnap'] ?? 0) + $reward;
        $stmtUpd  = $pdo->prepare("UPDATE account SET vnd = :vnd, tongnap = :tongnap, last_check = NOW() WHERE id = :id");
        $stmtUpd->bindParam(":vnd",  $newvnd,    PDO::PARAM_INT);
        $stmtUpd->bindParam(":tongnap", $newtongnap,   PDO::PARAM_INT);
        $stmtUpd->bindParam(":id",    $account_id, PDO::PARAM_INT);
        $stmtUpd->execute();
        header("Location: " . current_public_path() . "?dd=ok&reward=" . $reward);
        exit;
    }
}

if (isset($_GET['dd'])) {
    if ($_GET['dd'] === 'ok' && isset($_GET['reward'])) {
        $reward         = (int)$_GET['reward'];
        $diemDanhResult = 'success';
        $diemDanhMsg    = '🎉 Điểm danh thành công! Nhận được +' . format_fish_amount($reward) . ' cá!';
        $canDiemDanh    = false;
        $stmtvnd = $pdo->prepare("SELECT vnd FROM account WHERE id = :id LIMIT 1");
        $stmtvnd->bindParam(":id", $account_id);
        $stmtvnd->execute();
        $freshvnd       = $stmtvnd->fetch(PDO::FETCH_ASSOC);
        $vnd            = format_fish_amount($freshvnd['vnd'] ?? 0);
        $account['vnd'] = $freshvnd['vnd'] ?? 0;
    } elseif ($_GET['dd'] === 'already') {
        $diemDanhResult = 'already';
        $diemDanhMsg    = '⚠️ Hôm nay bạn đã điểm danh rồi! Quay lại vào ngày mai nhé!';
        $canDiemDanh    = false;
    }
}

// ── NẠP TIỀN LOGIC ──
$topupMsg = '';
$topupResult = '';
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'topup') {
    require_csrf_token();
    $topup_type = $_POST['topup_type'] ?? '';
    
    if ($topup_type === 'card') {
        $card_type = $_POST['card_type'] ?? '';
        $card_amount = (int)($_POST['card_amount'] ?? 0);
        $serial = trim($_POST['serial'] ?? '');
        $pin = trim($_POST['pin'] ?? '');

        if ($card_amount <= 0 || empty($serial) || empty($pin)) {
            $topupMsg = 'Vui lòng nhập đầy đủ thông tin thẻ cào!';
            $topupResult = 'error';
        } elseif (strlen($serial) < 10 || strlen($pin) < 10) {
            $topupMsg = '❌ Số Serial và Mã thẻ cào phải có độ dài tối thiểu 10 ký tự!';
            $topupResult = 'error';
        } else {
            // Kiểm tra trùng lặp thẻ cào
            $stmtCheck = $pdo->prepare("SELECT id FROM nap_the WHERE serial = :serial OR pin = :pin LIMIT 1");
            $stmtCheck->execute([':serial' => $serial, ':pin' => $pin]);
            if ($stmtCheck->fetch()) {
                $topupMsg = '❌ Thẻ cào này đã tồn tại trên hệ thống (đang chờ duyệt hoặc đã nạp)!';
                $topupResult = 'error';
            } else {
                $request_id = time() . '_' . rand(100, 999) . '_' . $account_id;
                $stmt = $pdo->prepare("INSERT INTO nap_the (account_id, request_id, card_type, card_amount, serial, pin, status) VALUES (:aid, :rid, :type, :amount, :serial, :pin, 'processing')");
                if ($stmt->execute([
                    ':aid' => $account_id,
                    ':rid' => $request_id,
                    ':type' => $card_type,
                    ':amount' => $card_amount,
                    ':serial' => $serial,
                    ':pin' => $pin
                ])) {
                    $topupMsg = "🎉 Gửi yêu cầu nạp thẻ cào thành công! Vui lòng chờ Admin kiểm duyệt.";
                    $topupResult = 'success';
                } else {
                    $topupMsg = '❌ Gửi yêu cầu thất bại!';
                    $topupResult = 'error';
                }
            }
        }
    } elseif ($topup_type === 'atm') {
        $atm_amount = (int)str_replace('.', '', (string)($_POST['atm_amount'] ?? '0'));
        $transaction_code = trim($_POST['transaction_code'] ?? '');

        if ($atm_amount <= 0 || empty($transaction_code)) {
            $topupMsg = 'Vui lòng nhập đầy đủ số tiền và mã giao dịch!';
            $topupResult = 'error';
        } else {
            // Kiểm tra trùng lặp mã giao dịch ATM
            $stmtCheck = $pdo->prepare("SELECT id FROM nap_the WHERE serial = :code LIMIT 1");
            $stmtCheck->execute([':code' => $transaction_code]);
            if ($stmtCheck->fetch()) {
                $topupMsg = '❌ Mã giao dịch này đã tồn tại trên hệ thống!';
                $topupResult = 'error';
            } else {
                $request_id = time() . '_' . rand(100, 999) . '_' . $account_id;
                $stmt = $pdo->prepare("INSERT INTO nap_the (account_id, request_id, card_type, card_amount, serial, pin, status) VALUES (:aid, :rid, 'ATM', :amount, :serial, '', 'processing')");
                if ($stmt->execute([
                    ':aid' => $account_id,
                    ':rid' => $request_id,
                    ':amount' => $atm_amount,
                    ':serial' => $transaction_code
                ])) {
                    $topupMsg = "🎉 Gửi yêu cầu chuyển khoản thành công! Vui lòng chờ Admin kiểm duyệt.";
                    $topupResult = 'success';
                } else {
                    $topupMsg = '❌ Gửi yêu cầu chuyển khoản thất bại!';
                    $topupResult = 'error';
                }
            }
        }
    }
}

// ── DUYỆT NẠP TIỀN LOGIC (ADMIN ONLY) ──
$reviewMsg = '';
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'review_topup') {
    require_csrf_token();
    if ($is_admin !== 1) {
        http_response_code(403);
        exit('Forbidden');
    }
    
    $tx_id = (int)($_POST['tx_id'] ?? 0);
    $status_choice = $_POST['status_choice'] ?? '';
    
    $stmt = $pdo->prepare("SELECT * FROM nap_the WHERE id = :id LIMIT 1");
    $stmt->execute([':id' => $tx_id]);
    $tx = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$tx) {
        $reviewMsg = 'Không tìm thấy yêu cầu!';
    } elseif ($tx['status'] !== 'processing') {
        $reviewMsg = 'Yêu cầu này đã được xử lý!';
    } else {
        if ($status_choice === 'approve') {
            $pdo->beginTransaction();
            try {
                $stmtUpdate = $pdo->prepare("UPDATE nap_the SET status = 'success' WHERE id = :id");
                $stmtUpdate->execute([':id' => $tx_id]);
                
                $stmtCredit = $pdo->prepare("UPDATE account SET vnd = vnd + :amount, tongnap = tongnap + :amount WHERE id = :id");
                $stmtCredit->execute([
                    ':amount' => $tx['card_amount'],
                    ':id' => $tx['account_id']
                ]);
                
                $pdo->commit();
                $reviewMsg = 'Đã duyệt yêu cầu nạp tiền! Cộng Cá thành công.';
            } catch (Exception $ex) {
                $pdo->rollBack();
                $reviewMsg = 'Lỗi hệ thống khi duyệt!';
            }
        } elseif ($status_choice === 'reject') {
            $stmtUpdate = $pdo->prepare("UPDATE nap_the SET status = 'failed' WHERE id = :id");
            if ($stmtUpdate->execute([':id' => $tx_id])) {
                $reviewMsg = 'Đã hủy và từ chối yêu cầu nạp tiền.';
            } else {
                $reviewMsg = 'Lỗi hệ thống khi hủy!';
            }
        }
    }
}

// Lấy danh sách giao dịch chờ duyệt cho Admin
$pendingTopups = [];
if ($is_admin === 1) {
    try {
        $stmtPending = $pdo->query("SELECT nt.*, a.username FROM nap_the nt JOIN account a ON nt.account_id = a.id WHERE nt.status = 'processing' ORDER BY nt.created_at ASC");
        $pendingTopups = $stmtPending->fetchAll(PDO::FETCH_ASSOC);
    } catch (Exception $e) {}
}

// ── BUFF VND LOGIC ──
$buffMsg = '';
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'buff_vnd' && isset($_POST['char_name']) && isset($_POST['amount'])) {
    require_csrf_token();
    if ($is_admin !== 1) {
        http_response_code(403);
        exit('Forbidden');
    }
    $char_name = trim($_POST['char_name']);
    $amount = (int)str_replace('.', '', (string)$_POST['amount']);
    if ($amount <= 0) {
        $buffMsg = 'Số Cá phải > 0!';
    } elseif ($char_name === '') {
        $buffMsg = 'Tên nhân vật không được để trống!';
    } else {
        $command = sprintf('BUFF_VND|%s|%d', str_replace(["\\r", "\\n"], '', $char_name), $amount);
        if (append_panel_command($command)) {
            $buffMsg = "Đã Buff Cá " . number_format($amount, 0, ',', '.') . " cho nhân vật " . $char_name;
        } else {
            $buffMsg = 'Không thể ghi lệnh buff vào file điều khiển game.';
        }
    }
}

// ── KICK PLAYER LOGIC ──
$kickMsg = '';
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'kick_player' && isset($_POST['char_name'])) {
    require_csrf_token();
    if ($is_admin !== 1) {
        http_response_code(403);
        exit('Forbidden');
    }
    $char_name = trim($_POST['char_name']);
    if ($char_name === '') {
        $kickMsg = 'Tên nhân vật không được để trống!';
    } else {
        $command = sprintf('KICK_PLAYER|%s|300', str_replace(["\r", "\n"], '', $char_name));
        if (append_panel_command($command)) {
            $kickMsg = "Đã gửi lệnh kick nhân vật " . $char_name . ". Người chơi sẽ bị khóa đăng nhập 5 phút.";
        } else {
            $kickMsg = 'Không thể ghi lệnh kick vào file điều khiển game.';
        }
    }
}

// ── ADMIN MODE LOGIC ──
$adminModeMsg = '';
$currentAdminMode = get_current_admin_mode();
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'admin_mode') {
    require_csrf_token();
    if ($is_admin !== 1) {
        http_response_code(403);
        exit('Forbidden');
    }
    $enabled = (($_POST['enabled'] ?? '0') === '1');
    $command = 'ADMIN_MODE|' . ($enabled ? '1' : '0');
    if (append_panel_command($command)) {
        @file_put_contents(panel_admin_mode_status_file(), $enabled ? '1' : '0', LOCK_EX);
        $currentAdminMode = $enabled;
        $adminModeMsg = $enabled
            ? 'Đã bật chế độ admin. Server chỉ cho tài khoản admin đăng nhập.'
            : 'Đã tắt chế độ admin. Người chơi thường có thể đăng nhập lại.';
    } else {
        $adminModeMsg = 'Không thể ghi lệnh chế độ admin vào file điều khiển game.';
    }
}

// ── XNXX BOT MODE LOGIC ──
$xnxxModeMsg = '';
$currentXnxxMode = get_current_xnxx_mode();
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'xnxx_mode') {
    require_csrf_token();
    if ($is_admin !== 1) {
        http_response_code(403);
        exit('Forbidden');
    }
    $enabled = (($_POST['enabled'] ?? '0') === '1');
    $command = 'BOT_PEM|' . ($enabled ? '1' : '0');
    if (append_panel_command($command)) {
        @file_put_contents(panel_xnxx_status_file(), $enabled ? '1' : '0', LOCK_EX);
        $currentXnxxMode = $enabled;
        $xnxxModeMsg = $enabled
            ? 'Đã bật XNXX. Bot pem quái sẽ được bật giống lệnh chat bot.'
            : 'Đã tắt XNXX. Bot pem quái sẽ được tắt giống lệnh chat bot.';
    } else {
        $xnxxModeMsg = 'Không thể ghi lệnh XNXX vào file điều khiển game.';
    }
}

// ── BOT GIAO DỊCH (BOTTALL) LOGIC ──
$botGdMsg = '';
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'bot_gd') {
    require_csrf_token();
    if ($is_admin !== 1) {
        http_response_code(403);
        exit('Forbidden');
    }
    $enabled = (($_POST['enabled'] ?? '0') === '1');
    $command = $enabled ? 'BOTTALL' : 'REMOVE_BOT_GD';
    if (append_panel_command($command)) {
        $botGdMsg = $enabled
            ? 'Đã bật toàn bộ 10 Bot Giao Dịch tại Vách Kakarot khu 0 (bottall)!'
            : 'Đã tắt toàn bộ Bot Giao Dịch tại Vách Kakarot!';
    } else {
        $botGdMsg = 'Không thể ghi lệnh Bot Giao Dịch vào file điều khiển game.';
    }
}

$changeEventMsg = '';
// ── MAINTENANCE LOGIC ──
$maintenanceMsg = '';
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'maintenance') {
    require_csrf_token();
    if ($is_admin !== 1) {
        http_response_code(403);
        exit('Forbidden');
    }
    $command = 'MAINTENANCE_NOW';
    if (append_panel_command($command)) {
        $maintenanceMsg = 'Lệnh bảo trì đã được gửi, server sẽ thực hiện bảo trì trong 30 giây.';
    } else {
        $maintenanceMsg = 'Không thể ghi lệnh bảo trì vào file điều khiển game.';
    }
}


// ── EXP SERVER LOGIC ──
$expMsg = '';
$currentExpServer = get_current_exp_server();
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'set_exp' && isset($_POST['exp_rate'])) {
    require_csrf_token();
    if ($is_admin !== 1) {
        http_response_code(403);
        exit('Forbidden');
    }
    $expRate = (int)str_replace('.', '', (string)$_POST['exp_rate']);
    if ($expRate <= 0) {
        $expMsg = 'EXP server phải > 0!';
    } else {
        $command = sprintf('SET_EXP|%d', $expRate);
        if (append_panel_command($command)) {
            $expMsg = 'Đã tăng EXP server thành x' . number_format($expRate, 0, ',', '.');
            @file_put_contents(panel_exp_status_file(), (string)$expRate, LOCK_EX);
            $currentExpServer = $expRate;
        } else {
            $expMsg = 'Không thể ghi lệnh EXP vào file điều khiển game.';
        }
    }
}
// ── DROP SERVER LOGIC ──
$dropMsg = '';
$currentDropServer = get_current_drop_server();
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'set_drop' && isset($_POST['drop_rate'])) {
    require_csrf_token();
    if ($is_admin !== 1) {
        http_response_code(403);
        exit('Forbidden');
    }
    $dropRate = (int)str_replace('.', '', (string)$_POST['drop_rate']);
    if ($dropRate <= 0) {
        $dropMsg = 'Tỉ lệ rơi đồ phải > 0!';
    } else {
        $command = sprintf('SET_DROP|%d', $dropRate);
        if (append_panel_command($command)) {
            $dropMsg = 'Đã chỉnh tỉ lệ rơi đồ server thành x' . number_format($dropRate, 0, ',', '.');
            @file_put_contents(panel_drop_status_file(), (string)$dropRate, LOCK_EX);
            $currentDropServer = $dropRate;
        } else {
            $dropMsg = 'Không thể ghi lệnh rơi đồ vào file điều khiển game.';
        }
    }
}

// ── GIFTCODE ──
$bossMsg = '';
$bossEditMsg = '';
$bossPersistMsg = '';
$currentBossPersistMode = get_current_boss_persist_mode();
$panelBosses = get_panel_bosses();
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'spawn_boss' && isset($_POST['boss_key'])) {
    require_csrf_token();
    if ($is_admin !== 1) {
        http_response_code(403);
        exit('Forbidden');
    }

    $parts = explode('|', (string)$_POST['boss_key']);
    $bossId = isset($parts[0]) ? (int)$parts[0] : 0;
    $mapId = isset($parts[1]) ? (int)$parts[1] : -1;
    $zoneId = isset($parts[2]) ? (int)$parts[2] : -1;

    if ($bossId === 0 && trim((string)($parts[0] ?? '')) !== '0') {
        $bossMsg = 'Boss khong hop le!';
    } else {
        $command = sprintf('SPAWN_BOSS|%d|%d|%d', $bossId, $mapId, $zoneId);
        if (append_panel_command($command)) {
            $bossMsg = 'Đã gửi lệnh gọi Boss ID ' . $bossId . '. Boss cũ sẽ bị xóa và tạo ra Boss mới.';
        } else {
            $bossMsg = '❌ Không thể ghi lệnh gọi Boss vào file điều khiển game.';
        }
    }
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'boss_persist_mode') {
    require_csrf_token();
    if ($is_admin !== 1) {
        http_response_code(403);
        exit('Forbidden');
    }
    $enabled = (($_POST['enabled'] ?? '0') === '1');
    if (append_panel_command('BOSS_PERSIST_MODE|' . ($enabled ? '1' : '0'))) {
        @file_put_contents(panel_boss_persist_status_file(), $enabled ? '1' : '0', LOCK_EX);
        if (!$enabled) {
            @unlink(panel_boss_overrides_file());
        }
        $currentBossPersistMode = $enabled;
        $bossPersistMsg = $enabled
            ? 'Đã bật lưu toàn bộ cấu hình Boss.'
            : 'Đã tắt lưu cấu hình Boss.';
    } else {
        $bossPersistMsg = 'Khong the ghi lenh bat/tat luu HP boss.';
    }
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'edit_boss' && isset($_POST['boss_key'])) {
    require_csrf_token();
    if ($is_admin !== 1) {
        http_response_code(403);
        exit('Forbidden');
    }

    $bossKeyRaw = trim((string)$_POST['boss_key']);
    $bossName = trim((string)($_POST['boss_name'] ?? ''));
    $bossHp = (int)str_replace('.', '', (string)($_POST['boss_hp'] ?? '0'));
    $bossDame = (int)str_replace('.', '', (string)($_POST['boss_dame'] ?? '0'));
    $bossArmor = (int)str_replace('.', '', (string)($_POST['boss_armor'] ?? '0'));
    $bossTargetHpPercent = max(0, min(100, (int)($_POST['boss_target_hp_percent'] ?? 0)));
    $bossSkillsRaw = trim((string)($_POST['boss_skills'] ?? ''));
    $bossSkillParts = preg_split('/[,;\r\n]+/', $bossSkillsRaw) ?: [];
    $bossSkills = [];
    $invalidSkill = false;
    foreach ($bossSkillParts as $skillPart) {
        $skillPart = trim($skillPart);
        if ($skillPart === '') continue;
        if (!preg_match('/^(\d+)\s*:\s*(\d+)\s*:\s*(\d+)$/', $skillPart, $match)) {
            $invalidSkill = true;
            break;
        }
        $skillId = (int)$match[1];
        $skillLevel = (int)$match[2];
        $skillCooldown = (int)$match[3];
        if ($skillLevel < 1 || $skillLevel > 7 || $skillCooldown < 200) {
            $invalidSkill = true;
            break;
        }
        $bossSkills[] = $skillId . ':' . $skillLevel . ':' . $skillCooldown;
    }
    $bossSkillsText = implode(',', $bossSkills);

    if ($bossKeyRaw === '') {
        $bossEditMsg = 'Boss khong hop le!';
    } elseif ($bossName === '') {
        $bossEditMsg = 'Ten boss khong duoc de trong!';
    } elseif ($bossHp <= 0) {
        $bossEditMsg = 'HP boss phai > 0!';
    } elseif ($bossHp > 2000000000) {
        $bossEditMsg = 'HP boss khong duoc vuot qua 2 ty!';
    } elseif ($bossDame <= 0 || $bossDame > 2000000000) {
        $bossEditMsg = 'Dame boss phai tu 1 den 2 ty!';
    } elseif ($bossArmor < 0 || $bossArmor > 2000000000) {
        $bossEditMsg = 'Giap boss phai tu 0 den 2 ty!';
    } elseif ($invalidSkill || empty($bossSkills)) {
        $bossEditMsg = 'Skill sai dinh dang. Dung ID:cap:cooldown, vi du 0:4:1000.';
    } else {
        $bossName = str_replace(["\r", "\n", "|"], ' ', $bossName);
        if (strpos($bossKeyRaw, '|') !== false) {
            $parts = explode('|', $bossKeyRaw);
            $bossId = (int)$parts[0];
            $bossLevel = max(0, (int)($parts[1] ?? 0));
            $command = sprintf('EDIT_BOSS|%d|%d|%d|%d|%d|%s|%s',
                $bossId, $bossLevel, $bossHp, $bossDame, $bossArmor,
                rawurlencode($bossName), rawurlencode($bossSkillsText));
        } else {
            $bossDataKey = preg_replace('/[^A-Za-z0-9_]/', '', $bossKeyRaw);
            $command = sprintf('EDIT_BOSS_DATA|%s|%d|%d|%d|%s|%s',
                $bossDataKey, $bossHp, $bossDame, $bossArmor,
                rawurlencode($bossName), rawurlencode($bossSkillsText));
        }
        if (append_panel_command($command)) {
            if (strpos($bossKeyRaw, '|') !== false) {
                append_panel_command(sprintf('SET_BOSS_TARGET_HP_PERCENT|%d|%d', $bossId, $bossTargetHpPercent));
            }
            $bossEditMsg = 'Đã sửa Boss ' . $bossName . ': HP ' . number_format($bossHp, 0, ',', '.')
                . ', dame ' . number_format($bossDame, 0, ',', '.')
                . ', giáp ' . number_format($bossArmor, 0, ',', '.') . ' và bộ skill.';
        } else {
            $bossEditMsg = '❌ Không thể sửa Boss.';
        }
    }
}

// ── POST ACTIONS: CUSTOM DROPS ──
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'save_custom_drop') {
    require_csrf_token();
    if ($is_admin !== 1) {
        http_response_code(403);
        exit('Forbidden');
    }
    $dropType = trim((string)($_POST['drop_type'] ?? 'mob'));
    $category = trim((string)($_POST['category'] ?? 'normal')); // 'normal' or 'event'
    $ruleId = trim((string)($_POST['rule_id'] ?? ''));
    if ($ruleId === '') {
        $ruleId = ($dropType === 'boss' ? 'boss_' : 'mob_') . time() . '_' . mt_rand(100, 999);
    }
    $targetType = trim((string)($_POST['target_type'] ?? 'all'));
    $targetId = (int)($_POST['target_id'] ?? 0);
    $targetName = trim((string)($_POST['target_name'] ?? ''));
    $configKey = trim((string)($_POST['config_key'] ?? ''));
    $itemId = (int)($_POST['item_id'] ?? 0);
    $itemName = trim((string)($_POST['item_name'] ?? ''));
    $minQty = max(1, (int)($_POST['min_quantity'] ?? 1));
    $maxQty = max($minQty, (int)($_POST['max_quantity'] ?? $minQty));
    $rate = max(0.01, min(100.0, (float)($_POST['rate'] ?? 100.0)));
    $pickupType = trim((string)($_POST['pickup_type'] ?? 'killer'));
    $options = trim((string)($_POST['options'] ?? ''));
    $enabled = !isset($_POST['enabled']) || $_POST['enabled'] === '1' || $_POST['enabled'] === 'true' || $_POST['enabled'] === true;

    if ($itemId > 0 && $itemName === '' && isset($pdo)) {
        try {
            $st = $pdo->prepare("SELECT name FROM item_template WHERE id = :id LIMIT 1");
            $st->execute([':id' => $itemId]);
            $itRow = $st->fetch(PDO::FETCH_ASSOC);
            if ($itRow) $itemName = $itRow['name'];
        } catch (Exception $e) {}
    }

    $drops = get_custom_drops();
    if ($dropType === 'boss') {
        $found = false;
        foreach ($drops['boss_drops'] as &$r) {
            if (($r['id'] ?? '') === $ruleId) {
                $r = [
                    'id' => $ruleId,
                    'target_type' => $targetType,
                    'target_id' => $targetId,
                    'config_key' => $configKey,
                    'target_name' => $targetName,
                    'item_id' => $itemId,
                    'item_name' => $itemName,
                    'min_quantity' => $minQty,
                    'max_quantity' => $maxQty,
                    'rate' => $rate,
                    'pickup_type' => $pickupType,
                    'options' => $options,
                    'category' => $category,
                    'enabled' => $enabled
                ];
                $found = true;
                break;
            }
        }
        unset($r);
        if (!$found) {
            $drops['boss_drops'][] = [
                'id' => $ruleId,
                'target_type' => $targetType,
                'target_id' => $targetId,
                'config_key' => $configKey,
                'target_name' => $targetName,
                'item_id' => $itemId,
                'item_name' => $itemName,
                'min_quantity' => $minQty,
                'max_quantity' => $maxQty,
                'rate' => $rate,
                'pickup_type' => $pickupType,
                'options' => $options,
                'category' => $category,
                'enabled' => $enabled
            ];
        }
    } else {
        $found = false;
        foreach ($drops['mob_drops'] as &$r) {
            if (($r['id'] ?? '') === $ruleId) {
                $r = [
                    'id' => $ruleId,
                    'target_type' => $targetType,
                    'target_id' => $targetId,
                    'target_name' => $targetName,
                    'item_id' => $itemId,
                    'item_name' => $itemName,
                    'min_quantity' => $minQty,
                    'max_quantity' => $maxQty,
                    'rate' => $rate,
                    'options' => $options,
                    'category' => $category,
                    'enabled' => $enabled
                ];
                $found = true;
                break;
            }
        }
        unset($r);
        if (!$found) {
            $drops['mob_drops'][] = [
                'id' => $ruleId,
                'target_type' => $targetType,
                'target_id' => $targetId,
                'target_name' => $targetName,
                'item_id' => $itemId,
                'item_name' => $itemName,
                'min_quantity' => $minQty,
                'max_quantity' => $maxQty,
                'rate' => $rate,
                'options' => $options,
                'category' => $category,
                'enabled' => $enabled
            ];
        }
    }

    save_custom_drops($drops);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['status' => 'success', 'msg' => 'Đã lưu cấu hình rơi đồ thành công!']);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'delete_custom_drop') {
    require_csrf_token();
    if ($is_admin !== 1) {
        http_response_code(403);
        exit('Forbidden');
    }
    $dropType = trim((string)($_POST['drop_type'] ?? 'mob'));
    $ruleId = trim((string)($_POST['rule_id'] ?? ''));
    $drops = get_custom_drops();
    if ($dropType === 'boss') {
        $drops['boss_drops'] = array_values(array_filter($drops['boss_drops'], fn($r) => ($r['id'] ?? '') !== $ruleId));
    } else {
        $drops['mob_drops'] = array_values(array_filter($drops['mob_drops'], fn($r) => ($r['id'] ?? '') !== $ruleId));
    }
    save_custom_drops($drops);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['status' => 'success', 'msg' => 'Đã xóa quy tắc rơi đồ!']);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'toggle_custom_drop') {
    require_csrf_token();
    if ($is_admin !== 1) {
        http_response_code(403);
        exit('Forbidden');
    }
    $dropType = trim((string)($_POST['drop_type'] ?? 'mob'));
    $ruleId = trim((string)($_POST['rule_id'] ?? ''));
    $newState = false;
    $drops = get_custom_drops();
    $targetList = $dropType === 'boss' ? 'boss_drops' : 'mob_drops';
    foreach ($drops[$targetList] as &$r) {
        if (($r['id'] ?? '') === $ruleId) {
            $r['enabled'] = empty($r['enabled']);
            $newState = $r['enabled'];
            break;
        }
    }
    unset($r);
    save_custom_drops($drops);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['status' => 'success', 'enabled' => $newState]);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'block_default_drop') {
    require_csrf_token();
    if ($is_admin !== 1) {
        http_response_code(403);
        exit(json_encode(['status' => 'error', 'msg' => 'Forbidden']));
    }
    $targetType = trim((string)($_POST['target_type'] ?? 'boss')); // 'boss', 'mob', 'event'
    $bossKey = trim((string)($_POST['boss_key'] ?? ''));
    $bossId = (int)($_POST['boss_id'] ?? 0);
    $mobTempId = (int)($_POST['mob_temp_id'] ?? 0);
    $itemId = (int)($_POST['item_id'] ?? -1);
    $category = trim((string)($_POST['category'] ?? 'normal'));
    $eventKey = trim((string)($_POST['event_key'] ?? ''));

    $drops = get_custom_drops();
    if ($targetType === 'event') {
        if ($eventKey !== '' && !in_array(strtoupper($eventKey), $drops['blocked_events'], true)) {
            $drops['blocked_events'][] = strtoupper($eventKey);
        }
    } elseif ($targetType === 'mob') {
        $id = 'blk_mob_' . $mobTempId . '_' . $itemId;
        $exists = false;
        foreach ($drops['blocked_mob_drops'] as $b) {
            if (($b['id'] ?? '') === $id || (($b['mob_temp_id'] ?? 0) === $mobTempId && ($b['item_id'] ?? 0) === $itemId)) {
                $exists = true; break;
            }
        }
        if (!$exists) {
            $drops['blocked_mob_drops'][] = [
                'id' => $id,
                'mob_temp_id' => $mobTempId,
                'item_id' => $itemId,
                'category' => $category
            ];
        }
    } else { // boss
        $id = 'blk_boss_' . ($bossKey ?: $bossId) . '_' . $itemId;
        $exists = false;
        foreach ($drops['blocked_boss_drops'] as $b) {
            if (($b['id'] ?? '') === $id || (($b['boss_key'] ?? '') === $bossKey && ($b['item_id'] ?? 0) === $itemId)) {
                $exists = true; break;
            }
        }
        if (!$exists) {
            $drops['blocked_boss_drops'][] = [
                'id' => $id,
                'boss_key' => $bossKey,
                'boss_id' => $bossId,
                'item_id' => $itemId,
                'category' => $category
            ];
        }
    }
    save_custom_drops($drops);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['status' => 'success', 'msg' => 'Đã chặn rơi thành công!']);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'unblock_default_drop') {
    require_csrf_token();
    if ($is_admin !== 1) {
        http_response_code(403);
        exit(json_encode(['status' => 'error', 'msg' => 'Forbidden']));
    }
    $targetType = trim((string)($_POST['target_type'] ?? 'boss'));
    $bossKey = trim((string)($_POST['boss_key'] ?? ''));
    $mobTempId = (int)($_POST['mob_temp_id'] ?? 0);
    $itemId = (int)($_POST['item_id'] ?? -1);
    $eventKey = trim((string)($_POST['event_key'] ?? ''));

    $drops = get_custom_drops();
    if ($targetType === 'event') {
        $drops['blocked_events'] = array_values(array_filter($drops['blocked_events'], fn($ev) => strtoupper($ev) !== strtoupper($eventKey)));
    } elseif ($targetType === 'mob') {
        $drops['blocked_mob_drops'] = array_values(array_filter($drops['blocked_mob_drops'], function($b) use ($mobTempId, $itemId) {
            return !(($b['mob_temp_id'] ?? 0) === $mobTempId && ($b['item_id'] ?? 0) === $itemId);
        }));
    } else {
        $drops['blocked_boss_drops'] = array_values(array_filter($drops['blocked_boss_drops'], function($b) use ($bossKey, $itemId) {
            return !(($b['boss_key'] ?? '') === $bossKey && ($b['item_id'] ?? 0) === $itemId);
        }));
    }
    save_custom_drops($drops);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['status' => 'success', 'msg' => 'Đã khôi phục cho phép rơi đồ!']);
    exit;
}

$giftcodes = [];
try {
    $stmtGc      = $pdo->query("SELECT * FROM giftcode WHERE type = 0 ORDER BY id ASC");
    $rawGiftcodes = $stmtGc->fetchAll(PDO::FETCH_ASSOC);
    foreach ($rawGiftcodes as $gc) {
        $details    = [];
        $detailArr  = json_decode($gc['detail'] ?? '', true);
        if (is_array($detailArr)) {
            foreach ($detailArr as $item) {
                $itemId     = $item['id'] ?? null;
                $qty        = $item['quantity'] ?? 1;
                $optionsRaw = $item['options'] ?? [];
                $iconId     = null;
                $itemName   = '';
                if ($itemId !== null) {
                    $stmtIt = $pdo->prepare("SELECT id, icon_id, name FROM item_template WHERE id = :id LIMIT 1");
                    $stmtIt->bindParam(":id", $itemId);
                    $stmtIt->execute();
                    $itRow = $stmtIt->fetch(PDO::FETCH_ASSOC);
                    if ($itRow) { $iconId = $itRow['icon_id']; $itemName = $itRow['name']; }
                }
                $optionLabels = [];
                if (is_array($optionsRaw)) {
                    foreach ($optionsRaw as $opt) {
                        $optId    = $opt['id']    ?? null;
                        $optParam = $opt['param'] ?? 0;
                        if ($optId !== null) {
                            $stmtOpt = $pdo->prepare("SELECT name FROM item_option_template WHERE id = :id LIMIT 1");
                            $stmtOpt->bindParam(":id", $optId);
                            $stmtOpt->execute();
                            $optRow = $stmtOpt->fetch(PDO::FETCH_ASSOC);
                            if ($optRow) {
                                $name = $optRow['name'];
                                $optionLabels[] = strpos($name, '#') !== false
                                    ? str_replace('#', $optParam, $name)
                                    : $name . ': ' . $optParam;
                            }
                        }
                    }
                }
                $details[] = ['icon_id' => $iconId, 'name' => $itemName, 'quantity' => $qty, 'options' => $optionLabels];
            }
        }
        $giftcodes[] = ['code' => $gc['code'], 'count_left' => $gc['count_left'], 'details' => $details];
    }
} catch (Exception $e) {}
?>
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
<meta name="csrf-token" content="<?= htmlspecialchars($csrfToken, ENT_QUOTES) ?>">
<title>Panel Website</title>
<link rel="icon" type="image/jpeg" href="../../img/icon.jpg">
<style>
:root {
  font-size: 18.876px;
  --bg:      #ddeeff;
  --surf:    #ffffff;
  --surf2:   #f0f7ff;
  --bd:      #9bbedd;
  --bd2:     #7aaacf;
  --tx:      #09131f;
  --tx2:     #1a3a5c;
  --tx3:     #4a7a9b;
  --blue:    #0d5fa3;
  --blue2:   #1272c2;
  --success: #0a8a5a;
  --danger:  #d92b3a;
  --warn:    #c47a00;
  --blu-lt:  #cce3f8;
  --glow:    rgba(13,95,163,.15);
  --r:       13px;
  --rs:      8px;
  --sh:      0 4px 24px rgba(13,95,163,.13);
  --sh2:     0 1px 8px rgba(13,95,163,.08);
}
*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
html { height: 100%; scroll-behavior: smooth; }
body {
  font-family: 'Times New Roman', Times, serif;
  font-size: 1rem; line-height: 1.6;
  background: var(--bg); color: var(--tx);
  min-height: 100dvh; -webkit-font-smoothing: antialiased; overflow-x: hidden;
}
body::before {
  content: ''; position: fixed; inset: 0; pointer-events: none; z-index: 0;
  background-image: radial-gradient(circle, var(--bd) 1px, transparent 1px);
  background-size: 22px 22px; opacity: .45;
}

/* LOGOUT BTN */
.logout-circle {
  position: fixed; bottom: 28px; right: 28px; z-index: 1000;
  width: 56px; height: 56px; border-radius: 50%;
  background: linear-gradient(135deg, #d92b3a, #b71c2a);
  border: none; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(217,43,58,.4), 0 0 0 2px rgba(255,255,255,.2);
  transition: all 0.3s cubic-bezier(0.68,-0.55,0.265,1.55);
  animation: pulse-glow 2s infinite;
}
.logout-circle:hover { transform: scale(1.15) rotate(5deg); box-shadow: 0 8px 30px rgba(217,43,58,.7), 0 0 0 4px rgba(255,255,255,.4); }
.logout-circle:active { transform: scale(0.92) rotate(-5deg); }
.logout-circle img { width: 100%; height: 100%; object-fit: cover; display: block; }
@keyframes pulse-glow {
  0%,100% { box-shadow: 0 4px 20px rgba(217,43,58,.4), 0 0 0 2px rgba(255,255,255,.2); }
  50%      { box-shadow: 0 4px 25px rgba(217,43,58,.7), 0 0 0 5px rgba(255,255,255,.3); }
}

/* LAYOUT */
.layout { position: relative; z-index: 1; display: grid; grid-template-columns: 248px 1fr; min-height: 100vh; }

/* SIDEBAR */
.sidebar {
  background: var(--surf); border-right: 1.5px solid var(--bd2);
  padding: 1.2rem 0 2rem; display: flex; flex-direction: column; gap: 0; box-shadow: var(--sh2);
}
.char-card {
  padding: 1.2rem 1.2rem 1rem; border-bottom: 1px solid var(--bd);
  display: flex; flex-direction: column; align-items: center; gap: 0.55rem;
}
.panel-boss-grid {
  display: grid; grid-template-columns: repeat(6,minmax(0,1fr)); gap: 12px;
  max-height: calc(90vh - 220px); overflow: auto; padding: 2px 4px 4px 2px; margin: 12px 0 18px;
}
.panel-boss-card {
  border: 1.5px solid var(--bd2); border-radius: 12px; background: var(--surf2);
  padding: 10px; text-align: left; cursor: pointer; font-family: 'Times New Roman', Times, serif;
  display: grid; gap: 7px; min-width: 0;
  transition: transform .16s ease, border-color .16s ease, box-shadow .16s ease, background .16s ease;
}
.panel-boss-card:hover { transform: translateY(-2px); border-color: var(--blue2); box-shadow: 0 7px 18px rgba(13,95,163,.16); }
.panel-boss-card.is-selected {
  border: 2px solid var(--success); background: rgba(10,138,90,.16);
  box-shadow: 0 0 0 3px rgba(10,138,90,.18), 0 12px 26px rgba(10,138,90,.24);
  transform: translateY(-3px);
  animation: panel-boss-picked .28s ease;
}
@keyframes panel-boss-picked {
  0% { transform: scale(.96); }
  70% { transform: scale(1.035); }
  100% { transform: translateY(-3px) scale(1); }
}
.panel-boss-thumb {
  position: relative; height: 92px; border-radius: 9px; background: var(--surf);
  display: flex; align-items: center; justify-content: center; overflow: hidden;
}
.panel-boss-thumb img { max-width: 100%; max-height: 100%; object-fit: contain; }
.panel-boss-state {
  position: absolute; top: 7px; left: 7px; width: 22px; height: 22px; border-radius: 50%;
  display: inline-flex; align-items: center; justify-content: center; color: #fff; font-size: .78rem; font-weight: 700;
}
.panel-boss-state.is-alive { background: var(--success); }
.panel-boss-state.is-dead { background: var(--danger); }
.panel-boss-card.is-selected .panel-boss-state {
  width: 28px; height: 28px; font-size: .95rem;
  box-shadow: 0 0 0 3px rgba(10,138,90,.18);
}
.panel-boss-name { font-size: .9rem; font-weight: 700; color: var(--tx); line-height: 1.2; overflow-wrap: anywhere; }
.panel-boss-meta { font-size: .78rem; color: var(--tx2); line-height: 1.3; overflow-wrap: anywhere; }
.panel-boss-empty { color: var(--tx3); text-align: center; padding: 18px 8px; border: 1px dashed var(--bd2); border-radius: 10px; }
.edit-boss-box {
  width:min(1480px, calc(100vw - 40px)); height:min(90dvh, 820px);
  max-width:none;
  display:flex; flex-direction:column; text-align:left; padding:18px; overflow:hidden;
}
#editBossModal .edit-boss-box { max-width:none; width:min(1480px, calc(100vw - 40px)); }
.edit-boss-title { font-size:1.25rem; font-weight:800; color:var(--blue2); margin-bottom:10px; }
.edit-boss-form-shell { display:flex; flex-direction:column; min-height:0; flex:1; }
.panel-boss-edit-layout { display:grid; grid-template-columns:minmax(0,1fr) minmax(300px,340px); gap:14px; min-height:0; flex:1; align-items:stretch; overflow:hidden; }
.panel-boss-edit-left { display:flex; flex-direction:column; min-height:0; overflow:hidden; }
.panel-boss-edit-toolbar {
  display:flex; justify-content:space-between; align-items:center; gap:12px;
  padding:10px 12px; border:1.5px solid var(--bd2); border-radius:10px; background:var(--surf2); margin-bottom:10px;
}
.panel-boss-edit-toolbar strong { color:var(--tx); font-size:1rem; }
.panel-boss-edit-toolbar span { color:var(--tx3); font-size:.82rem; }
.panel-boss-edit-list {
  display:grid; grid-template-columns:repeat(6,minmax(0,1fr)); gap:10px;
  flex:1; min-height:0; overflow:auto; padding:2px 4px 4px 2px;
  -webkit-overflow-scrolling:touch;
}
.panel-boss-edit-card {
  border:1.5px solid var(--bd2); border-radius:10px; background:var(--surf2);
  padding:8px; text-align:left; cursor:pointer; font-family:'Times New Roman',Times,serif;
  display:grid; grid-template-columns:1fr; gap:6px; align-content:start;
  min-height:170px; min-width:0;
  transition:transform .16s ease,border-color .16s ease,box-shadow .16s ease,background .16s ease;
}
.panel-boss-edit-card:hover { transform:translateY(-1px); border-color:var(--blue2); box-shadow:0 7px 18px rgba(13,95,163,.14); }
.panel-boss-edit-card.is-selected {
  border:2px solid var(--success); background:rgba(10,138,90,.14);
  box-shadow:0 0 0 3px rgba(10,138,90,.16),0 10px 22px rgba(10,138,90,.18);
}
.panel-boss-edit-avatar {
  position:relative; width:100%; height:78px; border-radius:8px; background:var(--surf);
  display:flex; align-items:center; justify-content:center; overflow:hidden; border:1px solid var(--bd);
}
#editBossModal .panel-boss-edit-avatar img {
  width:auto; height:auto; max-width:100%; max-height:100%; object-fit:contain;
  border:0; border-radius:0; box-shadow:none; margin:0;
}
.panel-boss-edit-status {
  position:absolute; top:5px; left:5px; min-width:20px; height:20px; border-radius:999px;
  display:inline-flex; align-items:center; justify-content:center; color:#fff; background:var(--success); font-size:.72rem; font-weight:700;
}
.panel-boss-edit-main { min-width:0; display:grid; gap:5px; }
.panel-boss-edit-name { color:var(--tx); font-size:.9rem; font-weight:800; line-height:1.15; overflow-wrap:anywhere; }
.panel-boss-edit-map { color:var(--tx2); font-size:.76rem; line-height:1.22; overflow-wrap:anywhere; }
.panel-boss-edit-stats { display:grid; gap:5px; min-width:0; }
.panel-boss-edit-hp-text { display:block; color:var(--tx2); font-size:.72rem; font-weight:700; min-width:0; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.panel-boss-edit-hp-text span { min-width:0; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.panel-boss-edit-hpbar { height:7px; border-radius:999px; background:rgba(217,43,58,.12); overflow:hidden; border:1px solid rgba(217,43,58,.18); }
.panel-boss-edit-hpfill { height:100%; min-width:4px; border-radius:999px; background:linear-gradient(90deg,var(--success),var(--warn)); }
.panel-boss-edit-form { border:1.5px solid var(--bd2); border-radius:10px; background:var(--surf2); padding:14px; align-self:stretch; width:100%; min-height:0; overflow-y:auto; }
.panel-boss-edit-form-title { font-size:1rem; font-weight:800; color:var(--tx); margin-bottom:10px; }
.panel-boss-persist-box { display:flex; align-items:center; justify-content:space-between; gap:10px; border:1px solid var(--bd2); border-radius:8px; padding:10px; margin-bottom:12px; background:var(--surf); }
.panel-boss-persist-box strong { display:block; color:var(--tx); font-size:.92rem; line-height:1.2; }
.panel-boss-persist-box span { display:block; color:var(--tx3); font-size:.78rem; line-height:1.25; margin-top:2px; }
.panel-boss-persist-toggle { min-width:76px; min-height:38px; border-radius:999px; border:1.5px solid var(--bd2); background:var(--surf2); color:var(--tx2); font-weight:800; cursor:pointer; font-family:'Times New Roman',Times,serif; }
.panel-boss-persist-toggle.is-on { border-color:rgba(10,138,90,.45); background:rgba(10,138,90,.12); color:var(--success); }
.panel-boss-edit-form label { display:block; margin-bottom:5px; font-weight:700; color:var(--tx2); }
.panel-boss-edit-form input,.panel-boss-edit-form textarea { width:100%; min-height:44px; padding:10px; margin-bottom:14px; border:1px solid var(--bd2); border-radius:4px; font-size:1.05rem; font-weight:700; font-family:'Times New Roman',Times,serif; }
.panel-boss-edit-form textarea { min-height:92px; resize:vertical; font-family:Consolas,monospace; font-size:.88rem; }
#editBossSkillRows { max-height:132px !important; min-height:48px; }
.edit-boss-add-skill { width:100%; flex:0 0 auto; }
.edit-boss-actions { position:sticky; bottom:-14px; z-index:5; padding:10px 0 2px; margin-top:6px; background:var(--surf2); }
@media (min-width: 900px) {
  .panel-boss-edit-list { grid-template-columns:repeat(6,minmax(0,1fr)); }
  .panel-boss-edit-form { padding:16px; }
  .panel-boss-edit-form input { min-height:46px; }
}
@media (min-width: 1280px) {
  .edit-boss-box, #editBossModal .edit-boss-box { width:min(1500px, calc(100vw - 56px)); max-width:none; }
  .panel-boss-edit-layout { grid-template-columns:minmax(0,1fr) 350px; gap:18px; }
}
#customDropModal .modal-box {
  text-align: left !important;
  max-width: 1200px !important;
  width: min(1200px, calc(100vw - 30px)) !important;
  height: min(860px, 94vh) !important;
  max-height: 94vh !important;
  padding: 14px !important;
  box-sizing: border-box !important;
}
#customDropModal img {
  border-radius: 4px !important;
  border: none !important;
  box-shadow: none !important;
  margin-bottom: 0 !important;
}
#customDropModal .cd-item-thumb {
  width: 32px !important;
  height: 32px !important;
  object-fit: contain !important;
  flex: 0 0 32px;
  image-rendering: pixelated;
}
#customDropModal #cd_item_icon_preview {
  width: 32px !important;
  height: 32px !important;
  object-fit: contain !important;
  image-rendering: pixelated;
}
#customDropModal .custom-drop-header { margin-bottom:8px !important; padding-bottom:8px !important; }
#customDropModal .custom-drop-primary-tabs { margin-bottom:7px !important; }
#customDropModal .custom-drop-category-tabs { margin-bottom:8px !important; }
#customDropModal .custom-drop-grid { grid-template-columns:330px minmax(0,1fr) !important; gap:10px !important; }
#customDropModal .custom-drop-form-panel { padding:10px !important; gap:6px !important; }
#customDropModal .custom-drop-right-panel { gap:9px !important; }
#customDropModal .custom-drop-section { padding:10px !important; }
#customDropModal .custom-drop-footer { margin-top:7px !important; }
#customDropModal .drop-item-row { min-height:48px; gap:8px; }
#customDropModal .drop-item-main { min-width:0; display:flex; align-items:center; gap:8px; }
#customDropModal .drop-item-main > div { min-width:0; }
#customDropModal .drop-item-name { white-space:normal; overflow-wrap:anywhere; line-height:1.15; }
#customDropModal button { white-space:nowrap; }
@media (max-height: 760px) and (min-width: 851px) {
  #customDropModal .modal-box { height:96vh !important; max-height:96vh !important; }
  #customDropModal #defaultDropsCatalogBox { max-height:210px !important; }
}
@media (max-width: 850px) {
  #customDropModal .custom-drop-grid {
    grid-template-columns: 1fr !important;
    overflow-y: auto !important;
  }
}
@media (max-width: 768px) {
  #bossModal .modal-box { width: calc(100vw - 20px); padding: 1rem; }
  #editBossModal { align-items:flex-start; overflow:auto; padding:8px 0; }
  #editBossModal .modal-box { width: calc(100vw - 16px); height:auto; min-height:calc(100dvh - 16px); max-height:none; padding:12px; border-radius:12px; }
  .edit-boss-title { font-size:1.12rem; margin-bottom:8px; }
  .panel-boss-grid { grid-template-columns: repeat(3,minmax(0,1fr)); gap: 10px; }
  .panel-boss-edit-layout { grid-template-columns:1fr; gap:10px; }
  .panel-boss-edit-layout > div:first-child { order:2; }
  .panel-boss-edit-left { overflow:visible; }
  .panel-boss-edit-form { order:1; }
  .panel-boss-edit-list { max-height:none; grid-template-columns:repeat(2,minmax(0,1fr)); overflow:visible; padding-right:0; }
  .panel-boss-edit-form { position:static; }
  .panel-boss-card { padding: 8px; }
  .panel-boss-thumb { height: 78px; }
}
@media (max-width: 420px) {
  .panel-boss-edit-toolbar { padding:9px 10px; align-items:flex-start; }
  .panel-boss-edit-toolbar strong { font-size:.95rem; }
  .panel-boss-edit-card { min-height:154px; padding:7px; gap:5px; }
  .panel-boss-edit-avatar { height:64px; }
  .panel-boss-edit-name { font-size:.94rem; }
  .panel-boss-edit-map { font-size:.78rem; }
  .panel-boss-edit-form input { font-size:1rem; }
}
.avatar-wrap { position: relative; width: 80px; height: 80px; }
.avatar-wrap img {
  width: 80px; height: 80px; border-radius: 50%; object-fit: cover;
  border: 3px solid var(--blue2);
  box-shadow: 0 0 16px var(--glow), 0 0 5px rgba(13,95,163,.2);
}
.gender-badge {
  position: absolute; bottom: 2px; right: 2px;
  background: var(--blu-lt); border: 2px solid var(--bd2); border-radius: 50%;
  width: 22px; height: 22px; display: flex; align-items: center; justify-content: center;
  font-size: 0.7rem; font-weight: 700; color: var(--blue);
}
.char-name-frame {
  background: rgba(13,95,163,.08); border: 1.5px solid rgba(13,95,163,.3);
  border-radius: var(--rs); padding: 0.35rem 0.9rem;
  font-size: 0.9rem; font-weight: 700; color: var(--tx2); text-align: center; margin-top: 0.2rem;
}
.char-status { font-size: 0.78rem; font-weight: 700; padding: 0.18rem 0.75rem; border-radius: 999px; letter-spacing: 0.04em; }
.status-active   { background: rgba(10,138,90,.12); border: 1px solid rgba(10,138,90,.4);  color: var(--success); }
.status-inactive { background: rgba(217,43,58,.1);  border: 1px solid rgba(217,43,58,.35); color: var(--danger);  }
.sidebar-card {
  margin: 0.6rem 0.85rem 0; background: var(--surf2);
  border: 1.5px solid var(--bd2); border-radius: var(--r); padding: 0.9rem 1rem; box-shadow: var(--sh2);
}
.sidebar-card-label { font-size: 0.72rem; font-weight: 700; letter-spacing: 0.08em; color: var(--blue2); text-transform: uppercase; margin-bottom: 0.5rem; }
.sidebar-btn {
  display: block; width: 100%; margin-top: 6px; padding: 7px 10px;
  border-radius: var(--rs); border: 1.5px solid var(--bd2); background: var(--surf); color: var(--tx2);
  cursor: pointer; font-family: 'Times New Roman', Times, serif; font-weight: 700; font-size: 0.88rem;
  transition: all .15s; text-align: center; text-decoration: none;
}
.sidebar-btn:hover { background: var(--blu-lt); border-color: var(--blue2); }
.btn-danger-sidebar {
  display: block; width: 100%; margin-top: 6px; padding: 9px 10px;
  border-radius: var(--rs); border: 1.5px solid #1e8449;
  background: linear-gradient(135deg,#27ae60,#1e8449);
  color: #fff; text-align: center; font-weight: 700; text-decoration: none;
  font-family: 'Times New Roman', Times, serif; font-size: 0.88rem; transition: all .2s; cursor: pointer;
}
.btn-danger-sidebar:hover { transform: scale(1.02); box-shadow: 0 4px 14px rgba(39,174,96,.35); }
.btn-forum-sidebar {
  border-color: #2d7dd2;
  background: linear-gradient(135deg,#3ba7ff,#1f5fbf);
}
.btn-forum-sidebar:hover { box-shadow: 0 4px 14px rgba(59,167,255,.35); }
.sidebar-nav { display: flex; flex-direction: column; padding: 0.6rem 0; flex: 1; gap: 1px; }

/* MAIN */
.main { position: relative; z-index: 1; padding: 1.5rem; display: flex; flex-direction: column; gap: 1.5rem; overflow-y: auto; }

/* STATS */
.stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; }
.stat-card {
  background: var(--surf); border: 1.5px solid var(--bd2); border-radius: var(--r); padding: 1.2rem;
  display: flex; flex-direction: column; gap: 0.5rem; box-shadow: var(--sh2); transition: transform .18s, box-shadow .18s;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: var(--sh); }
.stat-card-label { font-size: 0.72rem; font-weight: 700; letter-spacing: 0.1em; color: var(--blue2); text-transform: uppercase; }
.stat-card-value {
  font-size: 1.35rem; font-weight: 700; color: var(--warn);
  display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap;
}
.no-topup-box { max-width: 420px; }
.no-topup-icon { font-size:2.8rem; margin-bottom:10px; }
.no-topup-title { font-size:1.25rem; font-weight:700; color:var(--warn); margin-bottom:8px; }
.no-topup-lines { color:var(--tx3); margin-bottom:18px; line-height:1.5; font-size:0.95rem; }
.no-topup-lines span { display:block; margin-top:4px; }
.no-topup-lines .main-line { color:var(--danger); font-weight:800; }
.no-topup-actions { display:flex; gap:10px; }
.no-topup-actions button { flex:1; padding:11px; border-radius:var(--rs); font-weight:700; cursor:pointer; font-family:'Times New Roman',Times,serif; }
.no-topup-actions .danger { border:1.5px solid rgba(217,43,58,.4); background:rgba(217,43,58,.1); color:var(--danger); }
.no-topup-actions .cancel { border:1.5px solid var(--bd2); background:var(--surf2); color:var(--tx2); }

/* SECTION TITLE */
.section-title {
  font-size: 1rem; font-weight: 700; color: var(--blue2); letter-spacing: 0.04em;
  margin-bottom: 0.75rem; display: flex; align-items: center; gap: 0.5rem;
}
.section-title::after { content: ''; flex: 1; height: 1px; background: linear-gradient(90deg, var(--bd2), transparent); }

/* BXH */
.bxh-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 0.9rem; }
.bxh-card {
  background: linear-gradient(135deg,#27ae60,#1e8449); border: 1.5px solid #1e8449;
  border-radius: var(--r); padding: 1.1rem; text-decoration: none;
  display: flex; align-items: center; gap: 0.9rem; transition: all .2s;
  box-shadow: 0 4px 16px rgba(39,174,96,.3);
}
.bxh-card:hover { transform: translateY(-2px); box-shadow: 0 6px 22px rgba(39,174,96,.5); }
.bxh-icon { font-size: 1.8rem; width: 46px; height: 46px; background: rgba(0,0,0,.12); border-radius: var(--rs); display: flex; align-items: center; justify-content: center; }
.bxh-info strong { display: block; font-size: 0.95rem; color: #fff; font-weight: 700; }
.bxh-info span { font-size: 0.78rem; color: rgba(255,255,255,.82); }

/* ĐIỂM DANH */
.diemdanh-card { background: var(--surf); border: 1.5px solid var(--bd2); border-radius: var(--r); padding: 1rem 1.2rem; box-shadow: var(--sh2); }
.diemdanh-inner { display: flex; align-items: center; gap: 1rem; flex-wrap: wrap; }
.diemdanh-icon {
  font-size: 2rem; width: 54px; height: 54px; border-radius: var(--rs);
  background: linear-gradient(135deg, rgba(196,122,0,.12), rgba(196,122,0,.05));
  border: 1px solid rgba(196,122,0,.25); display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.diemdanh-info { flex: 1; min-width: 120px; }
.diemdanh-info h4 { font-size: 0.95rem; font-weight: 700; color: var(--warn); margin-bottom: 2px; }
.diemdanh-info p { font-size: 0.8rem; color: var(--tx3); line-height: 1.4; }
.diemdanh-info .reward-range { font-weight: 700; color: var(--success); }
.btn-diemdanh {
  padding: 0.62rem 1.3rem; border-radius: var(--rs); border: none;
  font-family: 'Times New Roman', Times, serif; font-weight: 700; font-size: 0.95rem;
  cursor: pointer; transition: all .2s; white-space: nowrap; flex-shrink: 0;
}
.btn-diemdanh.can-claim {
  background: linear-gradient(135deg, #f5c842, #e8a020);
  color: #1a1000; box-shadow: 0 4px 14px rgba(196,122,0,.35);
}
.btn-diemdanh.can-claim:hover { transform: translateY(-2px) scale(1.04); box-shadow: 0 6px 20px rgba(196,122,0,.5); }
.btn-diemdanh.can-claim:disabled { opacity: .7; cursor: not-allowed; transform: none; }
.btn-diemdanh.claimed { background: var(--surf2); border: 1.5px solid var(--bd2); color: var(--tx3); cursor: not-allowed; }

/* ALERT / THÔNG BÁO */
.alert-banner {
  padding: 0.9rem 1.3rem; border-radius: var(--r);
  display: flex; align-items: center; gap: 0.9rem;
  animation: rewardPop 0.5s cubic-bezier(.34,1.56,.64,1) forwards;
  transition: opacity 0.4s ease;
}
.alert-success { border: 1.5px solid rgba(10,138,90,.4);  background: rgba(10,138,90,.08); }
.alert-warn    { border: 1.5px solid rgba(196,122,0,.4);  background: rgba(196,122,0,.08); }
@keyframes rewardPop {
  0%   { transform: scale(.7) translateY(16px); opacity: 0; }
  60%  { transform: scale(1.06) translateY(-3px); opacity: 1; }
  100% { transform: scale(1) translateY(0); opacity: 1; }
}

/* GIFTCODE */
.gc-quick-list {
  display: flex; flex-wrap: wrap; gap: 0.75rem; margin-bottom: 1.5rem; padding: 1rem 1.2rem;
  background: linear-gradient(135deg, #27ae60, #1e8449); border: 1.5px solid #1e8449;
  border-radius: var(--r); box-shadow: 0 4px 16px rgba(39,174,96,.3);
}
.gc-quick-item {
  display: flex; align-items: center; gap: 0.4rem;
  background: rgba(255,255,255,.15); border: 1px solid rgba(255,255,255,.3);
  border-radius: var(--rs); padding: 0.35rem 0.9rem;
  font-size: 1rem; font-weight: 700; color: #fff; letter-spacing: 0.05em; transition: all .2s;
}
.gc-quick-item:hover { transform: translateY(-2px); background: rgba(255,255,255,.25); box-shadow: 0 4px 12px rgba(0,0,0,.2); }
.gc-quick-left { font-size: 0.72rem; font-weight: 700; color: #1e8449; background: rgba(255,255,255,.9); border-radius: 4px; padding: 0.1rem 0.4rem; margin-right: 5px; }
.giftcode-list { display: flex; flex-direction: column; gap: 1rem; }
.gc-card {
  background: linear-gradient(135deg, #d4eaf5, #c0deed); border: 1.5px solid #8fb5cc;
  border-radius: var(--r); padding: 1.1rem 1.3rem;
  display: flex; flex-direction: column; gap: 0.75rem;
  box-shadow: 0 4px 16px rgba(100,130,140,.2); transition: all 0.25s ease;
  position: relative; overflow: hidden;
}
.gc-card::before { content: '🎁'; position: absolute; bottom: -20px; right: -20px; font-size: 80px; opacity: 0.12; pointer-events: none; }
.gc-card:hover { transform: translateX(4px) translateY(-2px); box-shadow: 0 8px 28px rgba(100,130,140,.4); }
.gc-top { display: flex; align-items: center; gap: 1rem; flex-wrap: wrap; border-bottom: 1px dashed rgba(50,80,90,.3); padding-bottom: 0.6rem; }
.gc-code { font-size: 1.1rem; font-weight: 800; color: #1a4a5a; letter-spacing: 0.1em; background: rgba(255,255,255,.6); padding: 0.3rem 0.9rem; border-radius: var(--rs); border: 1px solid rgba(50,80,90,.2); }
.gc-left { font-size: 0.85rem; color: #1a4a5a; font-weight: 600; background: rgba(255,255,255,.5); padding: 0.2rem 0.8rem; border-radius: 20px; }
.gc-left span { color: #004d40; font-weight: 800; font-size: 1rem; }
.gc-items { display: flex; flex-wrap: wrap; gap: 0.8rem; margin-top: 0.2rem; }
.gc-item {
  display: flex; flex-direction: column; align-items: center; gap: 0.25rem;
  background: rgba(255,255,255,.35); border: 1.5px solid rgba(50,80,90,.2);
  border-radius: var(--rs); padding: 0.6rem 0.7rem; min-width: 70px; transition: all .2s;
}
.gc-item:hover { transform: scale(1.05); background: rgba(255,255,255,.5); box-shadow: 0 4px 12px rgba(0,0,0,.05); }
.gc-item img { width: 42px; height: 42px; object-fit: contain; border-radius: 6px; image-rendering: pixelated; filter: drop-shadow(0 1px 2px rgba(0,0,0,.1)); }
.gc-item-qty  { font-size: 0.75rem; font-weight: 800; color: #004d40; background: rgba(255,255,255,.7); padding: 0.1rem 0.4rem; border-radius: 12px; }
.gc-item-opts { font-size: 0.65rem; color: #1a4a5a; text-align: center; line-height: 1.3; }
.gc-item-name { font-size: 0.7rem; color: #1a3a4a; text-align: center; max-width: 80px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 600; }
.no-gc { color: var(--tx3); font-size: 0.88rem; padding: 1.5rem; text-align: center; border: 1.5px dashed var(--bd2); border-radius: var(--r); background: var(--surf2); }

/* ADMIN */
.admin-section-stack { display: flex; flex-direction: column; gap: 1rem; }
.admin-menu-card {
  background: var(--surf); border: 1.5px solid var(--bd2); border-radius: var(--r);
  padding: 1rem; box-shadow: var(--sh2);
}
.admin-menu-card.ops {
  border-color: rgba(196,122,0,.45);
  background: linear-gradient(135deg, rgba(255,248,226,.95), rgba(255,255,255,.98));
}
.admin-menu-head { display: flex; align-items: center; justify-content: space-between; gap: 0.8rem; margin-bottom: 0.75rem; }
.admin-menu-head strong { display: block; color: var(--tx2); font-size: 0.98rem; }
.admin-menu-head span { display: block; color: var(--tx3); font-size: 0.78rem; margin-top: 1px; }
.admin-menu-card.ops .admin-menu-head strong { color: var(--warn); }
.admin-menu-toggle {
  border: 1.5px solid var(--bd2);
  background: var(--surf2);
  color: var(--blue);
  border-radius: var(--rs);
  padding: 0.42rem 0.75rem;
  font-weight: 700;
  font-size: 0.82rem;
  cursor: pointer;
  font-family:'Times New Roman',Times,serif;
  white-space: nowrap;
}
.admin-menu-toggle:hover { background: var(--blu-lt); border-color: var(--blue2); }
.admin-menu-card.is-collapsible.is-collapsed .admin-grid { display: none; }
.admin-menu-card.is-collapsible.is-collapsed .admin-menu-head { margin-bottom: 0; }
.admin-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(125px, 1fr)); gap: 0.7rem; }
.admin-btn {
  display: flex; flex-direction: column; align-items: center; gap: 0.38rem;
  padding: 0.85rem 0.5rem; background: var(--surf); border: 1.5px solid var(--bd2);
  border-radius: var(--r); text-decoration: none; color: var(--tx2);
  font-weight: 700; font-family: 'Times New Roman', Times, serif; font-size: 0.88rem;
  transition: all .18s; cursor: pointer; text-align: center;
}
.admin-btn:hover { background: var(--blu-lt); border-color: var(--blue2); transform: translateY(-2px); box-shadow: var(--sh2); color: var(--blue); }
.admin-btn .ab-icon { font-size: 1.55rem; }

/* MODAL */
.modal-overlay {
  display: none; position: fixed; inset: 0;
  background: rgba(9,19,31,.65); backdrop-filter: blur(6px);
  z-index: 9999; align-items: center; justify-content: center;
}
.modal-box {
  background: var(--surf); border: 1.5px solid var(--bd2); border-radius: var(--r);
  padding: 2rem; max-width: 420px; width: 90%; text-align: center;
  box-shadow: var(--sh); animation: popIn .22s ease forwards;
}
.modal-box img { width: 70px; height: 70px; border-radius: 50%; object-fit: cover; border: 3px solid var(--blue2); box-shadow: var(--sh2); margin-bottom: 14px; }
@keyframes popIn { from { transform: scale(.88); opacity: 0; } to { transform: scale(1); opacity: 1; } }

/* SCROLLBAR */
::-webkit-scrollbar { width: 5px; height: 5px; }
::-webkit-scrollbar-track { background: var(--surf2); }
::-webkit-scrollbar-thumb { background: var(--bd2); border-radius: 3px; }

/* RESPONSIVE */
@media (max-width: 768px) {
  .layout { grid-template-columns: 1fr; }
  .sidebar { border-right: none; border-bottom: 1.5px solid var(--bd2); }
  .char-card { flex-direction: row; align-items: center; gap: 1rem; }
  .main { padding: 1rem; }
  .logout-circle { bottom: 20px; right: 20px; width: 48px; height: 48px; }
}
@media (max-width: 480px) {
  .stats-grid { grid-template-columns: 1fr 1fr; }
  .admin-grid { grid-template-columns: repeat(3, 1fr); }
}
</style>
</head>
<body>

<button class="logout-circle" onclick="openLogoutModal(event)" title="Đăng xuất">
  <img src="/img/b.png" alt="Đăng xuất">
</button>

<div class="layout">
  <aside class="sidebar">
    <div class="char-card">
      <div class="avatar-wrap">
        <img src="<?= htmlspecialchars($genderIcon) ?>" alt="avatar" onerror="this.src='/img/22.jpg'">
        <div class="gender-badge"><?= $gender == 0 ? '♂' : ($gender == 1 ? '♀' : '⚔') ?></div>
      </div>
      <div class="char-name-frame">Player: <?= htmlspecialchars($playerName) ?></div>
      <?php if ($active == 1): ?>
        <div class="char-status status-active">✔ MTV</div>
      <?php else: ?>
        <div class="char-status status-inactive">✖ MTV</div>
      <?php endif; ?>
    </div>

    <div class="sidebar-card">
      <div class="sidebar-card-label">Làm Quả Lọ ⚙️</div>
      <button onclick="toggleForm()" class="sidebar-btn">➕ Mở / Đóng</button>
      <div id="loaForm" style="display:none;margin-top:10px;">
        <input id="loaInput" type="text" placeholder="Nhập Link or Key..."
          style="width:100%;padding:10px;font-size:0.92rem;border-radius:var(--rs);border:1.5px solid var(--bd2);background:var(--surf2);color:var(--tx);outline:none;font-family:'Times New Roman',Times,serif;">
        <button onclick="addLink()"
          style="width:100%;margin-top:6px;padding:6px;border-radius:var(--rs);border:1.5px solid rgba(10,138,90,.4);background:rgba(10,138,90,.1);color:var(--success);font-weight:700;cursor:pointer;font-family:'Times New Roman',Times,serif;">
          ➕ Lưu Link
        </button>
        <div id="linkList" style="margin-top:8px;display:flex;flex-direction:column;gap:5px;"></div>
      </div>
      <div id="loaStatus" style="margin-top:6px;font-size:0.72rem;color:var(--tx3);">Chưa có link</div>
    </div>

    <div class="sidebar-card">
      <div class="sidebar-card-label">MENU ⚠️</div>
      <a href="#" onclick="openTopupModal(event)" class="btn-danger-sidebar">☠️ NẠP TIỀN</a>
      <!-- Đã chuyển Góp ý và Khiếu nại thành BXH -->
      <a href="/sv1/bxh01" class="btn-danger-sidebar">🏆 ĐUA TOP</a>
      <a href="/sv1/bxh02" class="btn-danger-sidebar">🥇 Tài nguyên</a>
      <a href="/sv1/trangchu.php#communityComments" class="btn-danger-sidebar btn-forum-sidebar">💬 Về Diễn Đàn</a>
    </div>
    <nav class="sidebar-nav">
    </nav>
  </aside>

  <main class="main">

    <?php if (!empty($reviewMsg)): ?>
    <div id="reviewAlert" class="alert-banner alert-success">
      <span style="font-size:1.9rem;">📢</span>
      <div style="flex:1;">
        <div style="font-size:1.02rem;font-weight:700;color:var(--success);">
          <?= htmlspecialchars($reviewMsg) ?>
        </div>
      </div>
      <!-- Progress timer bar -->
      <div style="display:flex;flex-direction:column;align-items:center;gap:4px;flex-shrink:0;min-width:40px;">
        <span id="reviewCountNum" style="font-size:0.8rem;font-weight:700;color:var(--tx3);">5</span>
        <div style="width:36px;height:5px;background:var(--bd);border-radius:3px;overflow:hidden;">
          <div id="reviewCountBar" style="height:100%;width:100%;border-radius:3px;background:var(--success);"></div>
        </div>
      </div>
    </div>
    <script>
    (function(){
      const el  = document.getElementById('reviewAlert');
      const bar = document.getElementById('reviewCountBar');
      const num = document.getElementById('reviewCountNum');
      if (!el) return;
      const TOTAL = 5000;
      const start = Date.now();
      const iv = setInterval(function(){
        const elapsed = Date.now() - start;
        const remain  = Math.max(0, TOTAL - elapsed);
        bar.style.width = (remain / TOTAL * 100).toFixed(1) + '%';
        num.textContent  = Math.ceil(remain / 1000);
        if (remain <= 0) {
          clearInterval(iv);
          el.style.opacity = '0';
          setTimeout(function(){ el.style.display = 'none'; }, 420);
        }
      }, 50);
    })();
    
</script>
    <?php endif; ?>

    <?php if (!empty($topupMsg)): ?>
    <div id="topupAlert" class="alert-banner <?= $topupResult === 'success' ? 'alert-success' : 'alert-warn' ?>">
      <span style="font-size:1.9rem;"><?= $topupResult === 'success' ? '🎉' : '⚠️' ?></span>
      <div style="flex:1;">
        <div style="font-size:1.02rem;font-weight:700;color:<?= $topupResult === 'success' ? 'var(--success)' : 'var(--warn)' ?>;">
          <?= htmlspecialchars($topupMsg) ?>
        </div>
        <div style="font-size:0.8rem;color:var(--tx3);margin-top:2px;">
          Số Cá hiện tại: <strong style="color:var(--warn);"><?= $vnd ?> Cá</strong>
        </div>
      </div>
      <!-- Progress timer bar -->
      <div style="display:flex;flex-direction:column;align-items:center;gap:4px;flex-shrink:0;min-width:40px;">
        <span id="topupCountNum" style="font-size:0.8rem;font-weight:700;color:var(--tx3);">5</span>
        <div style="width:36px;height:5px;background:var(--bd);border-radius:3px;overflow:hidden;">
          <div id="topupCountBar" style="height:100%;width:100%;border-radius:3px;background:<?= $topupResult === 'success' ? 'var(--success)' : 'var(--warn)' ?>;"></div>
        </div>
      </div>
    </div>
    <script>
    (function(){
      const el  = document.getElementById('topupAlert');
      const bar = document.getElementById('topupCountBar');
      const num = document.getElementById('topupCountNum');
      if (!el) return;
      const TOTAL = 5000;
      const start = Date.now();
      const iv = setInterval(function(){
        const elapsed = Date.now() - start;
        const remain  = Math.max(0, TOTAL - elapsed);
        bar.style.width = (remain / TOTAL * 100).toFixed(1) + '%';
        num.textContent  = Math.ceil(remain / 1000);
        if (remain <= 0) {
          clearInterval(iv);
          el.style.opacity = '0';
          setTimeout(function(){ el.style.display = 'none'; }, 420);
        }
      }, 50);
    })();
    </script>
    <?php endif; ?>

    <?php if (!empty($diemDanhMsg)): ?>
    <div id="ddAlert" class="alert-banner <?= $diemDanhResult === 'success' ? 'alert-success' : 'alert-warn' ?>">
      <span style="font-size:1.9rem;"><?= $diemDanhResult === 'success' ? '🎉' : '⚠️' ?></span>
      <div style="flex:1;">
        <div style="font-size:1.02rem;font-weight:700;color:<?= $diemDanhResult === 'success' ? 'var(--success)' : 'var(--warn)' ?>;">
          <?= htmlspecialchars($diemDanhMsg) ?>
        </div>
        <?php if ($diemDanhResult === 'success'): ?>
        <div style="font-size:0.8rem;color:var(--tx3);margin-top:2px;">
          Số Cá hiện tại: <strong style="color:var(--warn);"><?= $vnd ?> Cá</strong>
        </div>
        <?php endif; ?>
      </div>
      <!-- Thanh đếm ngược 5 giây -->
      <div style="display:flex;flex-direction:column;align-items:center;gap:4px;flex-shrink:0;min-width:40px;">
        <span id="ddCountNum" style="font-size:0.8rem;font-weight:700;color:var(--tx3);">5</span>
        <div style="width:36px;height:5px;background:var(--bd);border-radius:3px;overflow:hidden;">
          <div id="ddCountBar" style="height:100%;width:100%;border-radius:3px;background:<?= $diemDanhResult === 'success' ? 'var(--success)' : 'var(--warn)' ?>;"></div>
        </div>
      </div>
    </div>
    <script>
    (function(){
      const el  = document.getElementById('ddAlert');
      const bar = document.getElementById('ddCountBar');
      const num = document.getElementById('ddCountNum');
      if (!el) return;
      const TOTAL = 5000;
      const start = Date.now();
      const iv = setInterval(function(){
        const elapsed = Date.now() - start;
        const remain  = Math.max(0, TOTAL - elapsed);
        bar.style.width = (remain / TOTAL * 100).toFixed(1) + '%';
        num.textContent  = Math.ceil(remain / 1);
        if (remain <= 0) {
          clearInterval(iv);
          el.style.opacity = '0';
          setTimeout(function(){ el.style.display = 'none'; }, 420);
        }
      }, 50);
    })();
    </script>
    <?php endif; ?>

    <!-- STATS -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-card-label">⚡ Sức mạnh</div>
        <div class="stat-card-value" style="color:var(--blue2);"><?= number_format($power) ?></div>
      </div>
      <div class="stat-card">
       <div class="stat-card-label">🐟 Số Cá</div>
        <div class="stat-card-value">
          <?= $vnd ?>
        </div>
      </div>
    </div>

    <!-- ĐIỂM DANH -->
    <div>
      <div class="section-title">📅 Điểm danh hàng ngày</div>
      <div class="diemdanh-card">
        <div class="diemdanh-inner">
          <div class="diemdanh-icon"><?= $canDiemDanh ? '🎁' : '✅' ?></div>
          <div class="diemdanh-info">
            <h4><?= $canDiemDanh ? 'Phần thưởng đang chờ bạn!' : 'Hôm nay bạn đã điểm danh' ?></h4>
            <p>
              Điểm danh nhận <span class="reward-range">1 – 3.000 Cá</span> vào tài khoản.<br>
              <?= $canDiemDanh ? 'Nhấn nút bên cạnh để nhận thưởng ngay!' : 'Quay lại vào ngày mai để tiếp tục nhận thưởng!' ?>
            </p>
          </div>
          <?php if ($canDiemDanh): ?>
            <form method="POST" onsubmit="
              const btn = this.querySelector('button');
              btn.disabled = true;
              btn.textContent = '⏳ Đang xử lý...';
            ">
              <input type="hidden" name="action" value="diem_danh">
              <input type="hidden" name="csrf_token" value="<?= htmlspecialchars($csrfToken, ENT_QUOTES) ?>">
              <button type="submit" class="btn-diemdanh can-claim">🎁 Điểm danh!</button>
            </form>
          <?php else: ?>
            <button class="btn-diemdanh claimed" disabled>✅ Đã điểm danh</button>
          <?php endif; ?>
        </div>
      </div>
    </div>

    <!-- ADMIN -->
    <div>
      <div class="section-title">⚙️ Quản trị hệ thống</div>
      <div class="admin-section-stack">
        <?php
        $operationMenus = [
          ['🐟','Tặng Cá',  'onclick="openBuffModal()"'],
          ['💳','Duyệt Nạp', 'onclick="openReviewTopupModal()"'],
          ['🎒','Tặng Item','/sv1/admin/buff_item'],
          ['🛠️','Bảo trì',    'onclick="openMaintenanceModal()"'],
          ['⚡','Tỉ Lệ EXP',        'onclick="openExpModal()"'],
          ['🍀','Tỉ Lệ Rơi đồ',     'onclick="openDropModal()"'],
          ['⛔','Kick',       'onclick="openKickModal()"'],
          ['🛡️','Chế độ Admin', 'onclick="openAdminModeModal()"'],
          ['🎉','Đổi Sự Kiện', 'onclick="openChangeEventModal()"'],
          ['🔞','XNXX', 'onclick="openXnxxModeModal()"'],
          ['🤖','Bot GD', 'onclick="openBotGdModal()"'],
          ['🔄','Reset',      '#reset'],
        ];
        array_splice($operationMenus, 5, 0, [['💀','Gọi Boss', 'onclick="openBossModal()"']]);
        $adminMenus = [
          ['🌦️','EFF bản đồ','onclick="openMapEffectsModal()"'],
          ['🖼️','Tra cứu Icon','onclick="openLookupIconModal()"'],
          ['🩸','Chỉnh Boss',  'onclick="openEditBossModal()"'],
          ['📦','Rơi Quái & Boss', 'onclick="openCustomDropModal()"'],
          ['👤','Tài khoản',  '/sv1/admin/account'],
          ['🧙','Nhân vật',   '/sv1/admin/player'],
          ['🎁','Giftcode',   '/sv1/admin/giftcode'],
          ['⚔️','Vật phẩm',  '/sv1/admin/item'],
          ['🏪','Cửa hàng',   '/sv1/admin/itemshop'],
          ['🔒','Chỉ số',     '/sv1/admin/gioihan'],
          ['🗺️','Bản đồ',    '/sv1/admin/map'],
          ['📋','Giao dịch',  '/sv1/admin/giaodich'],
          ['🎯','Nội tại',    '/sv1/admin/noitai'],
        ];
        $renderAdminMenu = function ($menus) use ($is_admin) {
          foreach ($menus as $m) {
            if ($is_admin !== 1) {
              echo '<a href="#" class="admin-btn" onclick="adminOnlyNotice(event)"><span class="ab-icon">' . $m[0] . '</span><span>' . htmlspecialchars($m[1]) . '</span></a>';
              continue;
            }
            if ($m[2] === '#reset') {
              echo '<a href="#" class="admin-btn" onclick="resetAll(event)"><span class="ab-icon">' . $m[0] . '</span><span>' . htmlspecialchars($m[1]) . '</span></a>';
            } elseif (strpos($m[2], 'onclick=') === 0) {
              echo '<button class="admin-btn" ' . $m[2] . '><span class="ab-icon">' . $m[0] . '</span><span>' . htmlspecialchars($m[1]) . '</span></button>';
            } else {
              echo '<a href="' . htmlspecialchars($m[2]) . '" class="admin-btn"><span class="ab-icon">' . $m[0] . '</span><span>' . htmlspecialchars($m[1]) . '</span></a>';
            }
          }
        };
        ?>

        <div class="admin-menu-card ops<?= $is_admin !== 1 ? ' is-collapsible is-collapsed' : '' ?>" data-admin-section="ops">
          <div class="admin-menu-head">
            <div>
              <strong>Vận hành</strong>
              <span>Thao tác ảnh hưởng trực tiếp server</span>
            </div>
            <?php if ($is_admin !== 1): ?><button type="button" class="admin-menu-toggle" onclick="toggleAdminMenu(this)">xem thêm</button><?php endif; ?>
          </div>
          <div class="admin-grid">
            <?php $renderAdminMenu($operationMenus); ?>
          </div>
        </div>

        <div class="admin-menu-card<?= $is_admin !== 1 ? ' is-collapsible is-collapsed' : '' ?>" data-admin-section="manage">
          <div class="admin-menu-head">
            <div>
              <strong>Quản trị</strong>
              <span>Quản lý dữ liệu, cấu hình và nội dung game</span>
            </div>
            <?php if ($is_admin !== 1): ?><button type="button" class="admin-menu-toggle" onclick="toggleAdminMenu(this)">xem thêm</button><?php endif; ?>
          </div>
          <div class="admin-grid">
            <?php $renderAdminMenu($adminMenus); ?>
          </div>
        </div>
      </div>
    </div>
    <?php if ($is_admin === 1): ?>
    <dialog id="mapEffectsModal" style="width:96vw;max-width:1600px;height:90vh;padding:0;border:1px solid var(--bd2);border-radius:14px;background:var(--surf);color:var(--tx);">
      <div style="display:flex;align-items:center;justify-content:space-between;padding:12px 18px;border-bottom:1px solid var(--bd2);">
        <strong>🌦️ EFF bản đồ — chọn map và hiệu ứng</strong>
        <button type="button" onclick="document.getElementById('mapEffectsModal').close()" style="cursor:pointer;padding:7px 14px;" aria-label="Đóng chỉnh EFF">Đóng ✕</button>
      </div>
      <iframe id="mapEffectsFrame" title="Chỉnh hiệu ứng bản đồ" style="width:100%;height:calc(100% - 58px);border:0;"></iframe>
    </dialog>
    <style>#mapEffectsModal::backdrop{background:rgba(12,35,60,.55)}</style>
    <script>
    function openMapEffectsModal(){
      const modal=document.getElementById('mapEffectsModal'),frame=document.getElementById('mapEffectsFrame');
      if(!frame.getAttribute('src')) frame.src='/sv1/admin/map-effects?embed=1';
      if(!modal.open) modal.showModal();
    }
    </script>
    <?php endif; ?>
    <!-- GIFTCODE -->
    <div>
      <div class="section-title">🎁 Giftcode hiện có</div>
      <?php if (empty($giftcodes)): ?>
        <div class="no-gc">✨ Hiện chưa có giftcode nào. Hãy quay lại sau! ✨</div>
      <?php else: ?>
        <div class="gc-quick-list">
          <?php foreach ($giftcodes as $i => $gc): ?>
            <div class="gc-quick-item">
              <span class="gc-quick-left">#<?= $i + 1 ?></span>
              <?= htmlspecialchars($gc['code']) ?>
            </div>
          <?php endforeach; ?>
        </div>
        <div class="giftcode-list">
          <?php foreach ($giftcodes as $gc): ?>
          <div class="gc-card">
            <div class="gc-top">
              <div class="gc-code">🎫 <?= htmlspecialchars($gc['code']) ?></div>
              <div class="gc-left">📦 Còn: <span><?= (int)$gc['count_left'] ?></span></div>
            </div>
            <?php if (!empty($gc['details'])): ?>
            <div class="gc-items">
              <?php foreach ($gc['details'] as $di): ?>
              <div class="gc-item">
                <?php if ($di['icon_id'] !== null): ?>
                  <img src="/icon/<?= (int)$di['icon_id'] ?>.png" alt="<?= htmlspecialchars($di['name']) ?>" onerror="this.style.display='none'">
                <?php else: ?>
                  <div style="width:42px;height:42px;display:flex;align-items:center;justify-content:center;font-size:1.5rem;">🎁</div>
                <?php endif; ?>
                <?php if ($di['name']): ?>
                  <div class="gc-item-name" title="<?= htmlspecialchars($di['name']) ?>"><?= mb_substr(htmlspecialchars($di['name']), 0, 12) ?></div>
                <?php endif; ?>
                <div class="gc-item-qty">x<?= (int)$di['quantity'] ?></div>
                <?php if (!empty($di['options'])): ?>
                  <div class="gc-item-opts"><?= htmlspecialchars(implode(' | ', $di['options'])) ?></div>
                <?php endif; ?>
              </div>
              <?php endforeach; ?>
            </div>
            <?php endif; ?>
          </div>
          <?php endforeach; ?>
        </div>
      <?php endif; ?>
    </div>

    <!-- GÓP Ý & KHIẾU NẠI (ĐƯỢC CHUYỂN XUỐNG CUỐI TRANG SAU GIFTCODE) -->
    <div>
      <div class="section-title">💬 Hỗ trợ</div>
      <div class="bxh-grid">
        <a href="https://zalo.me/84325420834" target="_blank" class="bxh-card" style="background: linear-gradient(135deg,#e67e22,#d35400); border-color:#d35400;">
          <div class="bxh-icon">💡</div>
          <div class="bxh-info"><strong>Góp ý</strong><span>Đóng góp ý kiến</span></div>
        </a>
        <a href="https://zalo.me/84325420834" target="_blank" class="bxh-card" style="background: linear-gradient(135deg,#e74c3c,#c0392b); border-color:#c0392b;">
          <div class="bxh-icon">⚠️</div>
          <div class="bxh-info"><strong>Khiếu nại</strong><span>Báo cáo vấn đề</span></div>
        </a>
      </div>
    </div>

  </main>
</div>

<!-- MODAL BUFF VND -->
<div id="buffModal" class="modal-overlay">
  <div class="modal-box" onclick="event.stopPropagation()" style="max-width:400px;">
    <div style="font-size:1.3rem;font-weight:700;color:var(--blue2);margin-bottom:8px;">Tặng Cá Cho Nhân Vật</div>
    <form method="POST" onsubmit="submitBuffForm(event)">
      <input type="hidden" name="action" value="buff_vnd">
      <input type="hidden" name="csrf_token" value="<?= htmlspecialchars($csrfToken, ENT_QUOTES) ?>">
      <label for="buff_char_name" style="display:block;margin-bottom:5px;font-weight:700;color:var(--tx2);">Tên Nhân Vật:</label>
      <input type="text" id="buff_char_name" name="char_name" required style="width:100%;padding:10px;margin-bottom:15px;border:1px solid var(--bd2);border-radius:4px;font-size:1.3rem;font-weight:700;font-family:'Times New Roman',Times,serif;">

      <label for="buff_amount" style="display:block;margin-bottom:5px;font-weight:700;color:var(--tx2);">Số Cá:</label>
      <input type="text" id="buff_amount" name="amount" inputmode="numeric" autocomplete="off" required placeholder="Ví dụ: 1.000 Cá" style="width:100%;padding:10px;margin-bottom:20px;border:1px solid var(--bd2);border-radius:4px;font-size:1.3rem;font-weight:700;font-family:'Times New Roman',Times,serif;">

      <div style="display:flex;gap:10px;">
        <button type="submit" style="flex:1;padding:11px;border-radius:var(--rs);border:1.5px solid rgba(10,138,90,.4);background:rgba(10,138,90,.1);color:var(--success);font-weight:700;font-size:0.92rem;cursor:pointer;font-family:'Times New Roman',Times,serif;">🐟 Tặng Cá</button>
        <button type="button" onclick="document.getElementById('buffModal').style.display='none'" style="flex:1;padding:11px;border-radius:var(--rs);border:1.5px solid var(--bd2);background:var(--surf2);color:var(--tx2);font-weight:700;font-size:0.92rem;cursor:pointer;font-family:'Times New Roman',Times,serif;">❌ Hủy</button>
      </div>
    </form>
    <?php if ($buffMsg): ?>
      <div class="panel-action-message" style="margin-top:15px;padding:10px;border-radius:4px;text-align:center;background:<?= strpos($buffMsg, 'Đã Buff') !== false ? 'rgba(10,138,90,.1)' : 'rgba(217,43,58,.1)' ?>;border:1px solid <?= strpos($buffMsg, 'Đã Buff') !== false ? 'rgba(10,138,90,.4)' : 'rgba(217,43,58,.4)' ?>;color:<?= strpos($buffMsg, 'Đã Buff') !== false ? 'var(--success)' : 'var(--danger)' ?>;">
        <?= htmlspecialchars($buffMsg) ?>
      </div>
    <?php endif; ?>
  </div>
</div>

<!-- MODAL BẢO TRÌ -->
<div id="maintenanceModal" class="modal-overlay">
  <div class="modal-box" onclick="event.stopPropagation()" style="max-width:400px;">
    <div style="font-size:1.3rem;font-weight:700;color:var(--danger);margin-bottom:8px;">Xác Nhận Bảo Trì</div>
    <div style="font-size:0.88rem;color:var(--tx3);margin-bottom:20px;line-height:1.6;">Bạn có chắc chắn muốn bắt đầu chế độ bảo trì?<br>Bảo trì sau 30s và Tự khởi động lại.</div>
    <form method="POST" onsubmit="submitMaintenanceForm(event)">
      <input type="hidden" name="action" value="maintenance">
      <input type="hidden" name="csrf_token" value="<?= htmlspecialchars($csrfToken, ENT_QUOTES) ?>">
      <div style="display:flex;gap:10px;">
        <button type="submit" style="flex:1;padding:11px;border-radius:var(--rs);border:1.5px solid rgba(217,43,58,.4);background:rgba(217,43,58,.1);color:var(--danger);font-weight:700;font-size:0.92rem;cursor:pointer;font-family:'Times New Roman',Times,serif;">🛠️ Bảo Trì</button>
        <button type="button" onclick="document.getElementById('maintenanceModal').style.display='none'" style="flex:1;padding:11px;border-radius:var(--rs);border:1.5px solid var(--bd2);background:var(--surf2);color:var(--tx2);font-weight:700;font-size:0.92rem;cursor:pointer;font-family:'Times New Roman',Times,serif;">❌ Hủy</button>
      </div>
    </form>
    <?php if ($maintenanceMsg): ?>
      <div class="panel-action-message" style="margin-top:15px;padding:10px;border-radius:4px;text-align:center;background:<?= strpos($maintenanceMsg, 'Lệnh bảo trì') !== false ? 'rgba(10,138,90,.1)' : 'rgba(217,43,58,.1)' ?>;border:1px solid <?= strpos($maintenanceMsg, 'Lệnh bảo trì') !== false ? 'rgba(10,138,90,.4)' : 'rgba(217,43,58,.4)' ?>;color:<?= strpos($maintenanceMsg, 'Lệnh bảo trì') !== false ? 'var(--success)' : 'var(--danger)' ?>;">
        <?= htmlspecialchars($maintenanceMsg) ?>
      </div>
    <?php endif; ?>
  </div>
</div>

<!-- MODAL EXP SERVER -->
<div id="expModal" class="modal-overlay">
  <div class="modal-box" onclick="event.stopPropagation()" style="max-width:400px;">
    <div style="font-size:1.3rem;font-weight:700;color:var(--blue2);margin-bottom:8px;">Set EXP Server</div>
    <div style="font-size:0.88rem;color:var(--tx3);margin-bottom:16px;line-height:1.6;">EXP hiện tại: <b id="currentExpRate" style="color:var(--warn);font-size:1.05rem;">x<?= number_format($currentExpServer, 0, ',', '.') ?></b><br>Mặc định là x10.</div>
    <form method="POST" onsubmit="submitExpForm(event)">
      <input type="hidden" name="action" value="set_exp">
      <input type="hidden" name="csrf_token" value="<?= htmlspecialchars($csrfToken, ENT_QUOTES) ?>">
      <label for="exp_rate" style="display:block;margin-bottom:5px;font-weight:700;color:var(--tx2);">Tỉ Lệ EXP:</label>
      <input type="text" id="exp_rate" name="exp_rate" inputmode="numeric" autocomplete="off" required placeholder="Ví dụ: 10" style="width:100%;padding:10px;margin-bottom:20px;border:1px solid var(--bd2);border-radius:4px;font-size:1.3rem;font-weight:700;font-family:'Times New Roman',Times,serif;">
      <div style="display:flex;gap:10px;">
        <button type="submit" style="flex:1;padding:11px;border-radius:var(--rs);border:1.5px solid rgba(10,138,90,.4);background:rgba(10,138,90,.1);color:var(--success);font-weight:700;font-size:0.92rem;cursor:pointer;font-family:'Times New Roman',Times,serif;">⚡ Set EXP</button>
        <button type="button" onclick="document.getElementById('expModal').style.display='none'" style="flex:1;padding:11px;border-radius:var(--rs);border:1.5px solid var(--bd2);background:var(--surf2);color:var(--tx2);font-weight:700;font-size:0.92rem;cursor:pointer;font-family:'Times New Roman',Times,serif;">❌ Hủy</button>
      </div>
    </form>
    <?php if ($expMsg): ?>
      <div class="panel-action-message" style="margin-top:15px;padding:10px;border-radius:4px;text-align:center;background:<?= strpos($expMsg, 'Đã tăng') !== false ? 'rgba(10,138,90,.1)' : 'rgba(217,43,58,.1)' ?>;border:1px solid <?= strpos($expMsg, 'Đã tăng') !== false ? 'rgba(10,138,90,.4)' : 'rgba(217,43,58,.4)' ?>;color:<?= strpos($expMsg, 'Đã tăng') !== false ? 'var(--success)' : 'var(--danger)' ?>;">
        <?= htmlspecialchars($expMsg) ?>
      </div>
    <?php endif; ?>
  </div>
</div>
<!-- MODAL RƠI ĐỒ SERVER -->
<div id="dropModal" class="modal-overlay">
  <div class="modal-box" onclick="event.stopPropagation()" style="max-width:400px;">
    <div style="font-size:1.3rem;font-weight:700;color:var(--blue2);margin-bottom:8px;">Chỉnh Tỉ Lệ Rơi Đồ</div>
    <div style="font-size:0.88rem;color:var(--tx3);margin-bottom:16px;line-height:1.6;">Rơi đồ hiện tại: <b id="currentDropRate" style="color:var(--warn);font-size:1.05rem;">x<?= number_format($currentDropServer, 0, ',', '.') ?></b><br>Mặc định là x5.</div>
    <form method="POST" onsubmit="submitDropForm(event)">
      <input type="hidden" name="action" value="set_drop">
      <input type="hidden" name="csrf_token" value="<?= htmlspecialchars($csrfToken, ENT_QUOTES) ?>">
      <label for="drop_rate" style="display:block;margin-bottom:5px;font-weight:700;color:var(--tx2);">Tỉ Lệ Rơi Đồ:</label>
      <input type="text" id="drop_rate" name="drop_rate" inputmode="numeric" autocomplete="off" required placeholder="Ví dụ: 10" style="width:100%;padding:10px;margin-bottom:20px;border:1px solid var(--bd2);border-radius:4px;font-size:1.3rem;font-weight:700;font-family:'Times New Roman',Times,serif;">
      <div style="display:flex;gap:10px;">
        <button type="submit" style="flex:1;padding:11px;border-radius:var(--rs);border:1.5px solid rgba(10,138,90,.4);background:rgba(10,138,90,.1);color:var(--success);font-weight:700;font-size:0.92rem;cursor:pointer;font-family:'Times New Roman',Times,serif;">🍀 Set Tỉ Lệ</button>
        <button type="button" onclick="document.getElementById('dropModal').style.display='none'" style="flex:1;padding:11px;border-radius:var(--rs);border:1.5px solid var(--bd2);background:var(--surf2);color:var(--tx2);font-weight:700;font-size:0.92rem;cursor:pointer;font-family:'Times New Roman',Times,serif;">❌ Hủy</button>
      </div>
    </form>
    <?php if ($dropMsg): ?>
      <div class="panel-action-message" style="margin-top:15px;padding:10px;border-radius:4px;text-align:center;background:<?= strpos($dropMsg, 'Đã chỉnh') !== false ? 'rgba(10,138,90,.1)' : 'rgba(217,43,58,.1)' ?>;border:1px solid <?= strpos($dropMsg, 'Đã chỉnh') !== false ? 'rgba(10,138,90,.4)' : 'rgba(217,43,58,.4)' ?>;color:<?= strpos($dropMsg, 'Đã chỉnh') !== false ? 'var(--success)' : 'var(--danger)' ?>;">
        <?= htmlspecialchars($dropMsg) ?>
      </div>
    <?php endif; ?>
  </div>
</div>

<!-- MODAL BOT GIAO DỊCH (BOTTALL) -->
<div id="botGdModal" class="modal-overlay">
  <div class="modal-box" onclick="event.stopPropagation()" style="max-width:440px;">
    <div style="font-size:1.3rem;font-weight:700;color:var(--blue2);margin-bottom:8px;">🤖 10 Bot Giao Dịch (bottall)</div>
    <div style="font-size:0.88rem;color:var(--tx3);margin-bottom:16px;line-height:1.6;">
      10 Bot Giao Dịch tại <b>Vách núi Kakarot (Khu 0)</b>:<br>
      • 5 Bot bán 5 loại đá (1M/viên)<br>
      • Bot 6 thu mua Đồ Thần Linh (75M/món)<br>
      • Bot 7 mua NR 3 sao (100M) &amp; bán (120M)<br>
      • Bot 8-10 bán Cuồng nộ, Bổ khí, Bổ huyết, Giáp xên
    </div>
    <form method="POST" onsubmit="submitBotGdForm(event, 1)" style="margin-bottom:10px;">
      <input type="hidden" name="action" value="bot_gd">
      <input type="hidden" name="enabled" value="1">
      <input type="hidden" name="csrf_token" value="<?= htmlspecialchars($csrfToken, ENT_QUOTES) ?>">
      <button type="submit" style="width:100%;padding:11px;border-radius:var(--rs);border:1.5px solid rgba(10,138,90,.4);background:rgba(10,138,90,.15);color:var(--success);font-weight:700;font-size:0.95rem;cursor:pointer;font-family:'Times New Roman',Times,serif;">
        ▶️ BẬT 10 BOT GIAO DỊCH (bottall)
      </button>
    </form>
    <form method="POST" onsubmit="submitBotGdForm(event, 0)">
      <input type="hidden" name="action" value="bot_gd">
      <input type="hidden" name="enabled" value="0">
      <input type="hidden" name="csrf_token" value="<?= htmlspecialchars($csrfToken, ENT_QUOTES) ?>">
      <div style="display:flex;gap:10px;">
        <button type="submit" style="flex:1;padding:11px;border-radius:var(--rs);border:1.5px solid rgba(217,43,58,.4);background:rgba(217,43,58,.1);color:var(--danger);font-weight:700;font-size:0.92rem;cursor:pointer;font-family:'Times New Roman',Times,serif;">
          ⏹️ TẮT BOT GD
        </button>
        <button type="button" onclick="document.getElementById('botGdModal').style.display='none'" style="flex:1;padding:11px;border-radius:var(--rs);border:1.5px solid var(--bd2);background:var(--surf2);color:var(--tx2);font-weight:700;font-size:0.92rem;cursor:pointer;font-family:'Times New Roman',Times,serif;">❌ Đóng</button>
      </div>
    </form>
    <?php if (!empty($botGdMsg)): ?>
      <div class="panel-action-message" style="margin-top:15px;padding:10px;border-radius:4px;text-align:center;background:rgba(10,138,90,.1);border:1px solid rgba(10,138,90,.4);color:var(--success);">
        <?= htmlspecialchars($botGdMsg) ?>
      </div>
    <?php endif; ?>
  </div>
</div>

<!-- MODAL GỌI BOSS -->
<div id="bossModal" class="modal-overlay">
  <div class="modal-box" onclick="event.stopPropagation()" style="max-width:1180px;height:90vh;display:flex;flex-direction:column;">
    <div style="font-size:1.3rem;font-weight:700;color:var(--blue2);margin-bottom:8px;">Gọi Boss</div>
    <form method="POST" onsubmit="submitBossForm(event)" style="display:flex;flex-direction:column;min-height:0;flex:1;">
      <input type="hidden" name="action" value="spawn_boss">
      <input type="hidden" name="csrf_token" value="<?= htmlspecialchars($csrfToken, ENT_QUOTES) ?>">
      <input type="hidden" id="boss_key" name="boss_key" required>
      <input type="text" id="searchSpawnBossInput" placeholder="🔍 Tìm nhanh Boss theo tên hoặc ID..." oninput="filterSpawnBossList(this.value)" style="width:100%;margin-bottom:10px;padding:9px 12px;border:1.5px solid var(--bd2);border-radius:8px;background:var(--surf2);color:var(--tx);font-size:0.92rem;font-family:'Times New Roman',Times,serif;outline:none;">
      <?php if (empty($panelBosses)): ?>
        <div class="panel-boss-empty">Chua co du lieu boss_status.json</div>
      <?php else: ?>
        <div class="panel-boss-grid" id="panelBossGrid" style="flex:1;">
          <?php foreach ($panelBosses as $boss): ?>
            <?php
              $bossKey = $boss['id'] . '|' . $boss['mapId'] . '|' . $boss['zoneId'];
              $hpText = $boss['maxHp'] > 0 ? number_format($boss['hp'], 0, ',', '.') . '/' . number_format($boss['maxHp'], 0, ',', '.') : '0';
              $isAlive = $boss['state'] === 'Sống';
            ?>
            <button type="button" class="panel-boss-card" data-boss-key="<?= htmlspecialchars($bossKey, ENT_QUOTES) ?>" onclick="selectPanelBoss(this)">
              <span class="panel-boss-thumb">
                <span class="panel-boss-state <?= $isAlive ? 'is-alive' : 'is-dead' ?>"><?= $isAlive ? '✓' : '×' ?></span>
                <img src="/icon/<?= (int)$boss['avatarId'] ?>.png" alt="<?= htmlspecialchars($boss['name'], ENT_QUOTES) ?>" onerror="this.src='/icon/1.png'">
              </span>
              <span class="panel-boss-name"><?= htmlspecialchars($boss['name']) ?></span>
              <span class="panel-boss-meta"><?= htmlspecialchars($boss['mapName']) ?></span>
            </button>
          <?php endforeach; ?>
        </div>
      <?php endif; ?>
      <div style="display:flex;gap:10px;">
        <button id="spawnBossSubmit" type="submit" disabled style="flex:1;padding:11px;border-radius:var(--rs);border:1.5px solid rgba(10,138,90,.4);background:rgba(10,138,90,.1);color:var(--success);font-weight:700;font-size:0.92rem;cursor:pointer;font-family:'Times New Roman',Times,serif;">💀 Triệu Hồi</button>
        <button type="button" onclick="document.getElementById('bossModal').style.display='none'" style="flex:1;padding:11px;border-radius:var(--rs);border:1.5px solid var(--bd2);background:var(--surf2);color:var(--tx2);font-weight:700;font-size:0.92rem;cursor:pointer;font-family:'Times New Roman',Times,serif;">❌ Hủy</button>
      </div>
    </form>
    <?php if ($bossMsg): ?>
      <div class="panel-action-message" style="margin-top:15px;padding:10px;border-radius:4px;text-align:center;background:<?= strpos($bossMsg, 'Da gui') !== false ? 'rgba(10,138,90,.1)' : 'rgba(217,43,58,.1)' ?>;border:1px solid <?= strpos($bossMsg, 'Da gui') !== false ? 'rgba(10,138,90,.4)' : 'rgba(217,43,58,.4)' ?>;color:<?= strpos($bossMsg, 'Da gui') !== false ? 'var(--success)' : 'var(--danger)' ?>;">
        <?= htmlspecialchars($bossMsg) ?>
      </div>
    <?php endif; ?>
  </div>
</div>

<div id="editBossModal" class="modal-overlay">
  <div class="modal-box edit-boss-box" onclick="event.stopPropagation()">
    <div class="edit-boss-title">Sửa Boss</div>
    <form method="POST" onsubmit="submitEditBossForm(event)" class="edit-boss-form-shell">
      <input type="hidden" name="action" value="edit_boss">
      <input type="hidden" name="csrf_token" value="<?= htmlspecialchars($csrfToken, ENT_QUOTES) ?>">
      <input type="hidden" id="edit_boss_key" name="boss_key" required>
      <div class="panel-boss-edit-layout">
        <div class="panel-boss-edit-left">
          <?php
            $activeBosses = array_values($panelBosses);
          ?>
          <div class="panel-boss-edit-toolbar" style="flex-direction:column;align-items:stretch;gap:8px;">
            <div style="display:flex;justify-content:space-between;align-items:center;">
              <strong>Danh sách Boss</strong>
              <span id="editBossCount"><?= count($activeBosses) ?> boss</span>
            </div>
            <input type="text" id="searchBossEditInput" placeholder="🔍 Tìm Boss theo tên hoặc mã..." oninput="filterEditBossList(this.value)" style="width:100%;padding:7px 10px;border:1.5px solid var(--bd2);border-radius:6px;background:var(--surf);color:var(--tx);font-size:0.88rem;font-family:'Times New Roman',Times,serif;outline:none;">
          </div>
          <div class="panel-boss-edit-list" id="editBossGrid">
          <?php if (empty($activeBosses)): ?>
            <div class="panel-boss-empty">Chua co boss dang hoat dong</div>
          <?php else: ?>
            <?php foreach ($activeBosses as $boss): ?>
              <?php
                $bossKey = $boss['configKey'] ?? ($boss['id'] . '|' . $boss['level']);
                $hpText = number_format(max(0, (int)$boss['hp']), 0, ',', '.') . ' / ' . number_format(max(0, (int)$boss['maxHp']), 0, ',', '.');
                $skillValues = [];
                foreach (($boss['skills'] ?? []) as $skill) {
                    if (!is_array($skill)) continue;
                    $skillValues[] = (int)($skill['id'] ?? 0) . ':' . max(1, (int)($skill['level'] ?? 1)) . ':' . max(200, (int)($skill['cooldown'] ?? 200));
                }
                $skillText = implode(', ', $skillValues);
              ?>
              <?php
                $hpNow = max(0, (int)$boss['hp']);
                $hpMax = max(1, (int)$boss['maxHp']);
                $hpPercent = max(1, min(100, (int)round($hpNow * 100 / $hpMax)));
                $shortMap = panel_boss_short_map($boss);
              ?>
              <button type="button" class="panel-boss-edit-card" data-boss-key="<?= htmlspecialchars($bossKey, ENT_QUOTES) ?>" data-boss-name="<?= htmlspecialchars($boss['name'], ENT_QUOTES) ?>" data-boss-map="<?= htmlspecialchars($shortMap, ENT_QUOTES) ?>" data-boss-hp="<?= (int)max(1, $boss['maxHp']) ?>" data-boss-dame="<?= (int)max(1, $boss['dame']) ?>" data-boss-armor="<?= (int)max(0, $boss['armor']) ?>" data-boss-target-hp-percent="<?= (int)($boss['targetHpPercent'] ?? 0) ?>" data-boss-skills="<?= htmlspecialchars($skillText, ENT_QUOTES) ?>" onclick="selectEditBoss(this)">
                <span class="panel-boss-edit-avatar">
                  <span class="panel-boss-edit-status" style="background:<?= ($boss['state'] ?? '') === 'Sống' ? 'var(--success)' : 'var(--danger)' ?>"><?= ($boss['state'] ?? '') === 'Sống' ? '✓' : '×' ?></span>
                  <img src="/icon/<?= (int)$boss['avatarId'] ?>.png" alt="<?= htmlspecialchars($boss['name'], ENT_QUOTES) ?>" onerror="this.src='/icon/1.png'">
                </span>
                <span class="panel-boss-edit-main">
                  <span class="panel-boss-edit-name"><?= htmlspecialchars($boss['name']) ?></span>
                  <span class="panel-boss-edit-map"><?= htmlspecialchars($shortMap) ?></span>
                </span>
                <span class="panel-boss-edit-stats">
                  <span class="panel-boss-edit-hp-text"><span><?= htmlspecialchars($hpText) ?></span></span>
                  <span class="panel-boss-edit-hpbar"><span class="panel-boss-edit-hpfill" style="width:<?= $hpPercent ?>%"></span></span>
                </span>
              </button>
            <?php endforeach; ?>
          <?php endif; ?>
          </div>
        </div>
        <div class="panel-boss-edit-form">
          <div class="panel-boss-edit-form-title">Thông tin chỉnh sửa</div>
          <div class="panel-boss-persist-box">
            <div>
              <strong>Lưu cấu hình Boss</strong>
              <span id="bossPersistModeText"><?= $currentBossPersistMode ? 'Đang bật' : 'Đang tắt' ?></span>
            </div>
            <button type="button" id="bossPersistToggle" class="panel-boss-persist-toggle<?= $currentBossPersistMode ? ' is-on' : '' ?>" data-enabled="<?= $currentBossPersistMode ? '1' : '0' ?>" onclick="toggleBossPersistMode(this)"><?= $currentBossPersistMode ? 'Bật' : 'Tắt' ?></button>
          </div>
          <label for="edit_boss_name">Tên Boss:</label>
          <input type="text" id="edit_boss_name" name="boss_name" required>
          <label for="edit_boss_hp">HP Boss:</label>
          <input type="text" id="edit_boss_hp" name="boss_hp" inputmode="numeric" autocomplete="off" required placeholder="Ví dụ: 1.000.000">
          <label for="edit_boss_dame">Dame Boss:</label>
          <input type="text" id="edit_boss_dame" name="boss_dame" inputmode="numeric" autocomplete="off" required placeholder="Ví dụ: 5.000">
          <label for="edit_boss_target_hp_percent">Dame theo % HP nhân vật (0 = dame cố định):</label>
          <input type="number" id="edit_boss_target_hp_percent" name="boss_target_hp_percent" min="0" max="100" value="0" inputmode="numeric">
          <label for="edit_boss_armor">Giáp Boss:</label>
          <input type="text" id="edit_boss_armor" name="boss_armor" inputmode="numeric" autocomplete="off" required placeholder="Ví dụ: 200">
          <label>Skill và thời gian ra chiêu:</label>
          <textarea id="edit_boss_skills" name="boss_skills" required hidden></textarea>
          <button type="button" class="edit-boss-add-skill" onclick="addBossSkillRow()" style="margin-bottom:6px;padding:7px;border:1.5px solid var(--blue2);border-radius:7px;background:var(--surf2);color:var(--blue2);font-weight:700;cursor:pointer;">＋ Thêm skill</button>
          <div style="display:grid;grid-template-columns:minmax(145px,1fr) 62px 105px 32px;gap:5px;padding:0 7px;font-size:.72rem;color:var(--tx3);"><span>Tên skill</span><span>Cấp</span><span>Thời gian (ms)</span><span></span></div>
          <div id="editBossSkillRows" style="display:flex;flex-direction:column;gap:6px;max-height:210px;overflow:auto;padding:7px;border:1.5px solid var(--bd2);border-radius:7px;background:var(--surf);"></div>
          <div class="edit-boss-actions" style="display:flex;gap:10px;">
            <button id="editBossSubmit" type="submit" disabled style="flex:1;min-height:44px;padding:11px;border-radius:var(--rs);border:1.5px solid rgba(10,138,90,.4);background:rgba(10,138,90,.1);color:var(--success);font-weight:700;font-size:0.92rem;cursor:pointer;font-family:'Times New Roman',Times,serif;">💾 Lưu</button>
            <button type="button" onclick="document.getElementById('editBossModal').style.display='none'" style="flex:1;min-height:44px;padding:11px;border-radius:var(--rs);border:1.5px solid var(--bd2);background:var(--surf2);color:var(--tx2);font-weight:700;font-size:0.92rem;cursor:pointer;font-family:'Times New Roman',Times,serif;">❌ Hủy</button>
          </div>
        </div>
      </div>
    </form>
    <?php $bossPanelMsg = $bossEditMsg ?: $bossPersistMsg; ?>
    <?php if ($bossPanelMsg): ?>
      <div class="panel-action-message" style="margin-top:15px;padding:10px;border-radius:4px;text-align:center;background:<?= (strpos($bossPanelMsg, 'Da gui') !== false || strpos($bossPanelMsg, 'Đã') !== false) ? 'rgba(10,138,90,.1)' : 'rgba(217,43,58,.1)' ?>;border:1px solid <?= (strpos($bossPanelMsg, 'Da gui') !== false || strpos($bossPanelMsg, 'Đã') !== false) ? 'rgba(10,138,90,.4)' : 'rgba(217,43,58,.4)' ?>;color:<?= (strpos($bossPanelMsg, 'Da gui') !== false || strpos($bossPanelMsg, 'Đã') !== false) ? 'var(--success)' : 'var(--danger)' ?>;">
        <?= htmlspecialchars($bossPanelMsg) ?>
      </div>
    <?php endif; ?>
  </div>
</div>

<!-- MODAL CHỈNH VẬT PHẨM RƠI QUÁI VÀ BOSS -->
<div id="customDropModal" class="modal-overlay">
  <div class="modal-box" onclick="event.stopPropagation()" style="max-width:1300px;width:min(1300px, calc(100vw - 24px));height:92vh;display:flex;flex-direction:column;padding:18px;border-radius:14px;box-sizing:border-box;">
    
    <!-- HEADER -->
    <div class="custom-drop-header" style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;border-bottom:1.5px solid var(--bd2);padding-bottom:10px;">
      <div style="display:flex;align-items:center;gap:10px;">
        <span style="font-size:1.5rem;">📦</span>
        <div>
          <span style="font-size:1.25rem;font-weight:700;color:var(--blue2);">Quản Lý Đồ Rơi & Tỷ Lệ</span>
          <span style="font-size:0.8rem;color:var(--tx3);margin-left:8px;">Xem, thêm, sửa tỷ lệ và bật/tắt đồ rơi của Quái/Boss</span>
        </div>
      </div>
      <button type="button" onclick="closeCustomDropModal()" style="background:transparent;border:none;color:var(--tx3);font-size:1.4rem;cursor:pointer;line-height:1;">✕</button>
    </div>

    <!-- LEVEL 1 TABS: QUÁI vs BOSS -->
    <div class="custom-drop-primary-tabs" style="display:flex;gap:8px;margin-bottom:10px;">
      <button type="button" id="tabBtnMobDrop" onclick="switchCustomDropTab('mob')" class="admin-btn" style="flex:1;padding:9px;background:var(--surf2);border:1.5px solid var(--blue2);color:var(--blue2);font-weight:700;border-radius:8px;cursor:pointer;font-family:'Times New Roman',Times,serif;display:flex;align-items:center;justify-content:center;gap:8px;font-size:0.95rem;">
        <span>🐾</span><span>Rơi Từ Quái (Monster Drops)</span>
      </button>
      <button type="button" id="tabBtnBossDrop" onclick="switchCustomDropTab('boss')" class="admin-btn" style="flex:1;padding:9px;background:var(--surf);border:1.5px solid var(--bd2);color:var(--tx2);font-weight:700;border-radius:8px;cursor:pointer;font-family:'Times New Roman',Times,serif;display:flex;align-items:center;justify-content:center;gap:8px;font-size:0.95rem;">
        <span>💀</span><span>Rơi Từ Boss (Boss Drops)</span>
      </button>
    </div>

    <!-- LEVEL 2 TABS: THƯỜNG vs SỰ KIỆN -->
    <div id="dropCategoryTabs" class="custom-drop-category-tabs" style="display:flex;gap:8px;margin-bottom:12px;background:var(--surf2);padding:4px;border-radius:8px;border:1px solid var(--bd2);">
      <button type="button" id="catBtnNormal" onclick="switchDropCategory('normal')" style="flex:1;padding:7px;border-radius:6px;border:none;background:var(--blue2);color:#fff;font-weight:700;cursor:pointer;font-family:'Times New Roman',Times,serif;display:flex;align-items:center;justify-content:center;gap:6px;font-size:0.9rem;">
        <span>⚙️</span><span>ĐỒ RƠI THƯỜNG (Normal Drops)</span>
      </button>
      <button type="button" id="catBtnEvent" onclick="switchDropCategory('event')" style="flex:1;padding:7px;border-radius:6px;border:none;background:transparent;color:var(--tx2);font-weight:700;cursor:pointer;font-family:'Times New Roman',Times,serif;display:flex;align-items:center;justify-content:center;gap:6px;font-size:0.9rem;">
        <span>🎉</span><span>ĐỒ RƠI SỰ KIỆN (Event Drops - Tết, Hè, Daishinkan,...)</span>
      </button>
    </div>

    <!-- MAIN BODY: FORM (LEFT) + RIGHT CONTENT (CATALOG & RULES) -->
    <div class="custom-drop-grid" style="display:grid;grid-template-columns:360px 1fr;gap:14px;flex:1;min-height:0;">
      
      <!-- FORM PANEL (LEFT) -->
      <div class="custom-drop-form-panel" style="background:var(--surf);border:1.5px solid var(--bd2);border-radius:10px;padding:12px;overflow-y:auto;display:flex;flex-direction:column;gap:9px;">
        <div style="font-size:1rem;font-weight:700;color:var(--gold);display:flex;justify-content:space-between;align-items:center;">
          <span id="dropFormTitle">Thêm Quy Tắc Rơi Đồ</span>
          <button type="button" onclick="resetDropForm()" style="font-size:0.75rem;padding:3px 8px;border-radius:4px;border:1px solid var(--bd2);background:var(--surf2);color:var(--tx3);cursor:pointer;">🔄 Mới</button>
        </div>

        <form id="customDropForm" onsubmit="submitCustomDropForm(event)">
          <input type="hidden" id="cd_drop_type" name="drop_type" value="mob">
          <input type="hidden" id="cd_category" name="category" value="normal">
          <input type="hidden" id="cd_rule_id" name="rule_id" value="">
          <input type="hidden" id="cd_target_name" name="target_name" value="">
          <input type="hidden" id="cd_item_name" name="item_name" value="">
          <input type="hidden" id="cd_csrf" name="csrf_token" value="<?= htmlspecialchars($csrfToken, ENT_QUOTES) ?>">

          <!-- CATEGORY FIELD -->
          <div id="wrap_drop_category" style="margin-bottom:8px;">
            <label style="display:block;font-size:0.83rem;font-weight:700;color:var(--tx2);margin-bottom:3px;">Nhóm phân loại:</label>
            <select id="cd_category_select" onchange="onFormCategoryChange(this.value)" style="width:100%;padding:7px;border-radius:6px;border:1.5px solid var(--bd2);background:var(--surf2);color:var(--tx);font-family:'Times New Roman',Times,serif;outline:none;">
              <option value="normal">⚙️ Đồ Rơi Thường</option>
              <option value="event">🎉 Đồ Rơi Sự Kiện</option>
            </select>
          </div>

          <!-- TARGET TYPE -->
          <div style="margin-bottom:8px;">
            <label style="display:block;font-size:0.83rem;font-weight:700;color:var(--tx2);margin-bottom:3px;">Mục tiêu áp dụng:</label>
            <select id="cd_target_type" name="target_type" onchange="onDropTargetTypeChange()" style="width:100%;padding:7px;border-radius:6px;border:1.5px solid var(--bd2);background:var(--surf2);color:var(--tx);font-family:'Times New Roman',Times,serif;outline:none;">
              <option value="all">Tất cả quái (Toàn server)</option>
              <option value="mob">Theo loại quái cụ thể</option>
              <option value="map">Theo bản đồ (Map ID)</option>
            </select>
          </div>

          <!-- TARGET MOB SELECT -->
          <div id="wrap_target_mob" style="display:none;margin-bottom:8px;">
            <label style="display:block;font-size:0.83rem;font-weight:700;color:var(--tx2);margin-bottom:3px;">Chọn Quái:</label>
            <select id="cd_target_mob" onchange="onSelectMobChange(this)" style="width:100%;padding:7px;border-radius:6px;border:1.5px solid var(--bd2);background:var(--surf2);color:var(--tx);font-family:'Times New Roman',Times,serif;outline:none;">
              <option value="0">-- Đang tải danh sách quái... --</option>
            </select>
          </div>

          <!-- TARGET BOSS SELECT -->
          <div id="wrap_target_boss" style="display:none;margin-bottom:8px;">
            <label style="display:block;font-size:0.83rem;font-weight:700;color:var(--tx2);margin-bottom:3px;">Chọn Boss:</label>
            <select id="cd_target_boss" onchange="onSelectBossChange(this)" style="width:100%;padding:7px;border-radius:6px;border:1.5px solid var(--bd2);background:var(--surf2);color:var(--tx);font-family:'Times New Roman',Times,serif;outline:none;">
              <option value="0">-- Chọn Boss Catalog --</option>
              <?php foreach ($panelBosses as $pb): ?>
                <option value="<?= (int)$pb['id'] ?>" data-config-key="<?= htmlspecialchars($pb['configKey'] ?? '', ENT_QUOTES) ?>" data-name="<?= htmlspecialchars($pb['name'], ENT_QUOTES) ?>">
                  [ID <?= (int)$pb['id'] ?>] <?= htmlspecialchars($pb['name']) ?>
                </option>
              <?php endforeach; ?>
            </select>
          </div>

          <!-- TARGET MAP INPUT -->
          <div id="wrap_target_map" style="display:none;margin-bottom:8px;">
            <label style="display:block;font-size:0.83rem;font-weight:700;color:var(--tx2);margin-bottom:3px;">Map ID:</label>
            <input type="number" id="cd_target_map_id" placeholder="Nhập Map ID (ví dụ: 5)" style="width:100%;padding:7px;border-radius:6px;border:1.5px solid var(--bd2);background:var(--surf2);color:var(--tx);outline:none;">
          </div>

          <input type="hidden" id="cd_target_id" name="target_id" value="0">
          <input type="hidden" id="cd_config_key" name="config_key" value="">

          <!-- ITEM SELECTION -->
          <div style="margin-bottom:8px;">
            <label style="display:block;font-size:0.83rem;font-weight:700;color:var(--tx2);margin-bottom:3px;">Vật phẩm rơi:</label>
            <div style="display:flex;gap:6px;align-items:center;">
              <div style="width:36px;height:36px;background:var(--surf2);border:1.5px solid var(--bd2);border-radius:6px;display:flex;align-items:center;justify-content:center;flex-shrink:0;">
                <img id="cd_item_icon_preview" src="/icon/1.png" style="width:28px;height:28px;object-fit:contain;" onerror="this.src='/icon/1.png'">
              </div>
              <div style="flex:1;position:relative;">
                <div style="display:flex;gap:5px;"><input type="number" id="cd_item_id" name="item_id" placeholder="Item ID..." oninput="onItemIdInput(this.value)" style="min-width:0;flex:1;padding:7px;border-radius:6px;border:1.5px solid var(--bd2);background:var(--surf2);color:var(--tx);outline:none;"><button type="button" onclick="addCurrentDropItem()" title="Thêm ID vào danh sách" style="padding:0 10px;border:1px solid var(--blue2);border-radius:6px;background:var(--blue2);color:#fff;cursor:pointer;font-weight:700;">＋</button></div>
              </div>
            </div>
            <div id="cd_item_name_display" style="font-size:0.8rem;color:var(--success);margin-top:2px;font-weight:700;min-height:16px;"></div>
            <div id="cd_selected_items" style="display:flex;flex-wrap:wrap;gap:5px;margin-top:5px;"></div>
            
            <div style="position:relative;margin-top:4px;">
              <input type="text" id="cd_item_search" placeholder="🔍 Gõ tên để tìm Item..." oninput="searchDropItems(this.value)" style="width:100%;padding:6px;font-size:0.82rem;border-radius:6px;border:1px solid var(--bd2);background:var(--surf2);color:var(--tx);outline:none;">
              <div id="cd_item_suggestions" style="display:none;position:absolute;top:100%;left:0;right:0;max-height:180px;overflow-y:auto;background:var(--surf);border:1.5px solid var(--blue2);border-radius:6px;z-index:9999;box-shadow:var(--sh);"></div>
            </div>
          </div>

          <!-- QUANTITY -->
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:6px;margin-bottom:8px;">
            <div>
              <label style="display:block;font-size:0.8rem;font-weight:700;color:var(--tx2);margin-bottom:3px;">Số lượng Min:</label>
              <input type="number" id="cd_min_qty" name="min_quantity" value="1" min="1" required style="width:100%;padding:7px;border-radius:6px;border:1.5px solid var(--bd2);background:var(--surf2);color:var(--tx);outline:none;">
            </div>
            <div>
              <label style="display:block;font-size:0.8rem;font-weight:700;color:var(--tx2);margin-bottom:3px;">Số lượng Max:</label>
              <input type="number" id="cd_max_qty" name="max_quantity" value="1" min="1" required style="width:100%;padding:7px;border-radius:6px;border:1.5px solid var(--bd2);background:var(--surf2);color:var(--tx);outline:none;">
            </div>
          </div>

          <!-- RATE % -->
          <div style="margin-bottom:8px;">
            <label style="display:block;font-size:0.83rem;font-weight:700;color:var(--tx2);margin-bottom:3px;">Tỷ lệ rơi (%):</label>
            <div style="display:flex;gap:6px;align-items:center;">
              <input type="number" id="cd_rate" name="rate" value="100" min="0.01" max="100" step="any" required style="flex:1;padding:7px;border-radius:6px;border:1.5px solid var(--bd2);background:var(--surf2);color:var(--tx);outline:none;">
              <div style="display:flex;gap:3px;">
                <button type="button" onclick="document.getElementById('cd_rate').value='100'" style="padding:4px 6px;font-size:0.75rem;border-radius:4px;border:1px solid var(--bd2);background:var(--surf2);color:var(--tx);cursor:pointer;">100%</button>
                <button type="button" onclick="document.getElementById('cd_rate').value='10'" style="padding:4px 6px;font-size:0.75rem;border-radius:4px;border:1px solid var(--bd2);background:var(--surf2);color:var(--tx);cursor:pointer;">10%</button>
                <button type="button" onclick="document.getElementById('cd_rate').value='1'" style="padding:4px 6px;font-size:0.75rem;border-radius:4px;border:1px solid var(--bd2);background:var(--surf2);color:var(--tx);cursor:pointer;">1%</button>
              </div>
            </div>
          </div>

          <!-- PICKUP TYPE (FOR BOSS) -->
          <div id="wrap_pickup_type" style="display:none;margin-bottom:8px;">
            <label style="display:block;font-size:0.83rem;font-weight:700;color:var(--tx2);margin-bottom:3px;">Cơ chế nhặt:</label>
            <select id="cd_pickup_type" name="pickup_type" style="width:100%;padding:7px;border-radius:6px;border:1.5px solid var(--bd2);background:var(--surf2);color:var(--tx);font-family:'Times New Roman',Times,serif;outline:none;">
              <option value="killer">Chỉ người kết liễu nhặt (plKill)</option>
              <option value="free">Rơi tự do ai cũng nhặt được</option>
            </select>
          </div>

          <!-- OPTIONS -->
          <div style="margin-bottom:10px;">
            <label style="display:block;font-size:0.83rem;font-weight:700;color:var(--tx2);margin-bottom:3px;">Chỉ số tùy chọn (Option ID:Param):</label>
            <input type="text" id="cd_options" name="options" placeholder="Để trống nếu mặc định. Ví dụ: 50:10, 77:5" style="width:100%;padding:7px;border-radius:6px;border:1.5px solid var(--bd2);background:var(--surf2);color:var(--tx);outline:none;">
          </div>

          <!-- SUBMIT BUTTON -->
          <button id="cd_submit_btn" type="submit" style="width:100%;padding:10px;border-radius:8px;border:1.5px solid rgba(10,138,90,.4);background:rgba(10,138,90,.15);color:var(--success);font-weight:700;font-size:0.92rem;cursor:pointer;font-family:'Times New Roman',Times,serif;">
            💾 Lưu Quy Tắc Rơi Đồ
          </button>
        </form>
      </div>

      <!-- RIGHT PANEL: DEFAULT DROPS CATALOG + CUSTOM RULES -->
      <div class="custom-drop-right-panel" style="display:flex;flex-direction:column;gap:12px;overflow-y:auto;min-height:0;padding-right:4px;">
        
        <!-- SECTION 1: DEFAULT DROPS IN SOURCE (WITH BLOCK / DELETE ACTION) -->
        <div class="custom-drop-section" style="background:var(--surf);border:1.5px solid var(--bd2);border-radius:10px;padding:12px;display:flex;flex-direction:column;">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;flex-wrap:wrap;gap:8px;">
            <div style="display:flex;align-items:center;gap:8px;">
              <span style="font-size:1.1rem;" id="defaultDropSectionIcon">📋</span>
              <strong style="font-size:0.95rem;color:var(--gold);" id="defaultDropSectionTitle">Đồ Rơi Mặc Định Trong Source Code</strong>
              <span id="defaultCatalogCountBadge" style="font-size:0.75rem;padding:2px 6px;border-radius:4px;background:var(--surf2);color:var(--tx3);border:1px solid var(--bd2);">0 món</span>
            </div>
            <!-- SEARCH IN DEFAULT DROPS -->
            <input type="text" id="searchDefaultDropsInput" oninput="renderDefaultDropsCatalog()" placeholder="🔍 Tìm Boss hoặc Tên đồ..." style="width:220px;padding:5px 8px;font-size:0.8rem;border-radius:6px;border:1px solid var(--bd2);background:var(--surf2);color:var(--tx);outline:none;">
          </div>
          
          <div style="font-size:0.78rem;color:var(--tx3);margin-bottom:8px;">
            ⚠️ Nhấn nút <strong>[🗑️ Xóa / Chặn rơi]</strong> để cấm món đồ đó rơi từ Boss/Quái. Nhấn <strong>[✓ Khôi phục]</strong> để cho phép rơi lại.
          </div>

          <!-- CONTAINER FOR DEFAULT DROPS -->
          <div id="defaultDropsCatalogBox" style="max-height:260px;overflow-y:auto;border:1px solid var(--bd2);border-radius:8px;background:var(--surf2);padding:8px;display:flex;flex-direction:column;gap:8px;">
            <div style="text-align:center;padding:16px;color:var(--tx3);">⏳ Đang tải dữ liệu đồ mặc định...</div>
          </div>
        </div>

        <!-- SECTION 2: CUSTOM RULES LIST -->
        <div class="custom-drop-section" style="background:var(--surf);border:1.5px solid var(--bd2);border-radius:10px;padding:12px;display:flex;flex-direction:column;flex:1;min-height:190px;">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;">
            <div style="display:flex;align-items:center;gap:8px;">
              <span style="font-size:1.1rem;">✨</span>
              <strong style="font-size:0.95rem;color:var(--blue2);" id="customRulesSectionTitle">Quy Tắc Rơi Tự Cấu Hình</strong>
              <span id="customRulesCountBadge" style="font-size:0.75rem;padding:2px 6px;border-radius:4px;background:var(--surf2);color:var(--tx3);border:1px solid var(--bd2);">0 quy tắc</span>
            </div>
          </div>

          <div style="flex:1;overflow-y:auto;border:1px solid var(--bd2);border-radius:8px;background:var(--surf2);">
            <table style="width:100%;border-collapse:collapse;font-size:0.85rem;text-align:left;">
              <thead>
                <tr style="background:rgba(0,0,0,.25);border-bottom:1.5px solid var(--bd2);color:var(--tx2);">
                  <th style="padding:7px 9px;">Nhóm</th>
                  <th style="padding:7px 9px;">Mục tiêu</th>
                  <th style="padding:7px 9px;">Vật phẩm</th>
                  <th style="padding:7px 9px;text-align:center;">Số lượng</th>
                  <th style="padding:7px 9px;text-align:center;">Tỷ lệ</th>
                  <th id="th_pickup" style="padding:7px 9px;display:none;">Ai nhặt</th>
                  <th style="padding:7px 9px;text-align:center;">Trạng thái</th>
                  <th style="padding:7px 9px;text-align:center;">Thao tác</th>
                </tr>
              </thead>
              <tbody id="customDropRulesTbody">
                <tr>
                  <td colspan="8" style="text-align:center;padding:20px;color:var(--tx3);">Đang tải dữ liệu...</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

      </div>

    </div>

    <!-- FOOTER -->
    <div class="custom-drop-footer" style="display:flex;justify-content:flex-end;gap:10px;margin-top:10px;">
      <button type="button" onclick="closeCustomDropModal()" style="padding:8px 20px;border-radius:8px;border:1.5px solid var(--bd2);background:var(--surf2);color:var(--tx2);font-weight:700;font-size:0.9rem;cursor:pointer;font-family:'Times New Roman',Times,serif;">
        ✕ Đóng
      </button>
    </div>
  </div>
</div>

<!-- MODAL THÔNG BÁO -->
<?php if ($is_admin === 1): ?>
<div id="changeEventModal" class="modal-overlay" role="dialog" aria-modal="true" aria-labelledby="eventTitle">
<div class="modal-box"><h3 id="eventTitle">🎉 Đổi sự kiện toàn server</h3>
<p>Sự kiện được chọn sẽ thay sự kiện đang bật, không cần khởi động lại server.</p>
<form id="eventForm"><label for="eventSelect">Sự kiện</label>
<select id="eventSelect" required style="width:100%;padding:10px;margin:12px 0;background:var(--surf2);color:var(--tx);">
<option value="">— Chọn sự kiện —</option>
<?php foreach ([1=>'Tết Nguyên Đán',2=>'Mùa Hè',3=>'Halloween',4=>'Giáng sinh',5=>'Hùng Vương',6=>'Trung Thu',10=>'Cô Hồn',9=>'Tắt toàn bộ sự kiện'] as $eid=>$ename): ?>
<option value="<?= $eid ?>"><?= htmlspecialchars($ename) ?></option>
<?php endforeach; ?>
</select><p id="eventStatus" role="status"></p>
<button id="eventApply" class="sidebar-btn" type="submit">Áp dụng toàn server</button>
<button class="sidebar-btn" type="button" onclick="document.getElementById('changeEventModal').style.display='none'">Đóng</button>
</form></div></div>
<script>
document.getElementById('eventForm').onsubmit=async function(e){
 e.preventDefault();const s=document.getElementById('eventSelect'),b=document.getElementById('eventApply'),status=document.getElementById('eventStatus');
 if(!s.value||b.disabled)return;
 if(!confirm('Áp dụng "'+s.options[s.selectedIndex].text+'" cho toàn server?'))return;
 b.disabled=true;s.disabled=true;status.textContent='Đang gửi lệnh…';
 try{const response=await fetch('/sv1/admin/panel',{method:'POST',body:new URLSearchParams({action:'panel_set_event',event_id:s.value,csrf_token:<?= json_encode($csrfToken) ?>})});
 const data=await response.json();if(!response.ok)throw Error(data.error||'Không gửi được lệnh.');status.textContent=data.message;
 }catch(error){status.textContent='Không xác nhận được kết quả. Kiểm tra thông báo trong game trước khi thử lại. '+error.message;}
 finally{b.disabled=false;s.disabled=false;}
};
</script>
<?php endif; ?>
<div id="msgBox" class="modal-overlay">
  <div class="modal-box" onclick="event.stopPropagation()">
    <div id="msgText" style="font-size:1rem;margin-bottom:14px;color:var(--tx);"></div>
    <button onclick="document.getElementById('msgBox').style.display='none'" style="padding:6px 16px;border-radius:var(--rs);border:1.5px solid var(--bd2);background:var(--surf2);color:var(--tx2);cursor:pointer;font-weight:700;font-family:'Times New Roman',Times,serif;">OK</button>
  </div>
</div>

<!-- MODAL RESET -->
<div id="resetModal" class="modal-overlay">
  <div class="modal-box" onclick="event.stopPropagation()">
    <div style="font-size:2.8rem;margin-bottom:10px;">⚠️</div>
    <div style="font-size:1.25rem;font-weight:700;color:var(--warn);margin-bottom:8px;">Xác nhận Reset</div>
    <div style="color:var(--tx3);margin-bottom:18px;line-height:1.5;font-size:0.88rem;">
      <b style="color:var(--danger);">XÓA TOÀN BỘ DỮ LIỆU</b>?<br>Xóa ALL: Tài Khoản, Player, Clan ...
    </div>
    <div style="display:flex;gap:10px;">
      <button onclick="doReset()" style="flex:1;padding:11px;border-radius:var(--rs);border:1.5px solid rgba(217,43,58,.4);background:rgba(217,43,58,.1);color:var(--danger);font-weight:700;cursor:pointer;font-family:'Times New Roman',Times,serif;">RESET</button>
      <button onclick="document.getElementById('resetModal').style.display='none'" style="flex:1;padding:11px;border-radius:var(--rs);border:1.5px solid var(--bd2);background:var(--surf2);color:var(--tx2);font-weight:700;cursor:pointer;font-family:'Times New Roman',Times,serif;">HỦY</button>
    </div>
  </div>
</div>

<script>
function openLogoutModal(e) { e.preventDefault(); document.getElementById("logoutModal").style.display = "flex"; }
function doLogout() {
  fetch('/sv1/admin/logout', { method: 'POST', credentials: 'same-origin', headers: csrfHeaders() })
    .then(r => r.json())
    .then(() => { window.location.href = '/sv1/trangchu.php'; })
    .catch(() => { window.location.href = '/sv1/trangchu.php'; });
}
const CSRF_TOKEN = document.querySelector('meta[name="csrf-token"]')?.content || '';
function csrfHeaders(extra = {}) { return Object.assign({}, extra, { 'X-CSRF-TOKEN': CSRF_TOKEN }); }
function showMsg(text) { document.getElementById("msgText").innerText = text; document.getElementById("msgBox").style.display = "flex"; }
function showNoTopupNotice(event) { event.preventDefault(); document.getElementById("noTopupModal").style.display = "flex"; }
function adminOnlyNotice(event) { event.preventDefault(); showMsg("Chức năng chỉ dành cho admin"); }
function resetAll(e) { e.preventDefault(); document.getElementById("resetModal").style.display = "flex"; }
function doReset() {
  fetch('/sv1/admin/reset', { method: 'POST', headers: csrfHeaders() })
    .then(r => r.text())
    .then(d => { document.getElementById("resetModal").style.display = "none"; showMsg("✔ " + d); })
    .catch(() => { document.getElementById("resetModal").style.display = "none"; showMsg("❌ Reset thất bại!"); });
}

function toggleAdminMenu(btn) {
  const card = btn.closest('.admin-menu-card');
  if (!card) return;
  const collapsed = card.classList.toggle('is-collapsed');
  btn.textContent = collapsed ? 'xem thêm' : 'ẩn';
}
function openBuffModal() { document.getElementById("buffModal").style.display = "flex"; }
function openMaintenanceModal() { document.getElementById("maintenanceModal").style.display = "flex"; }
function openExpModal() { document.getElementById("expModal").style.display = "flex"; }
function openDropModal() { document.getElementById("dropModal").style.display = "flex"; }
function openKickModal() { document.getElementById("kickModal").style.display = "flex"; }
function openAdminModeModal() { document.getElementById("adminModeModal").style.display = "flex"; }
function openChangeEventModal() { document.getElementById("changeEventModal").style.display = "flex"; }
function openXnxxModeModal() { document.getElementById("xnxxModeModal").style.display = "flex"; }
function openBotGdModal() { document.getElementById("botGdModal").style.display = "flex"; }
function submitBotGdForm(event, enabled) {
  event.preventDefault();
  const form = event.target;
  const btn = form.querySelector('button[type="submit"]');
  const originalText = btn.innerHTML;
  btn.innerHTML = '⏳ Đang xử lý...';
  btn.disabled = true;

  fetch('', { method: 'POST', headers: csrfHeaders(), body: new FormData(form) })
    .then(r => r.text())
    .then(html => {
      btn.innerHTML = originalText;
      btn.disabled = false;
      document.getElementById("botGdModal").style.display = "none";
      showMsg(enabled === 1 ? "✔ Đã bật 10 Bot Giao Dịch tại Vách Kakarot (bottall)!" : "✔ Đã tắt Bot Giao Dịch tại Vách Kakarot!");
    })
    .catch(() => {
      btn.innerHTML = originalText;
      btn.disabled = false;
      showMsg("❌ Xử lý Bot Giao Dịch thất bại!");
    });
}
function openBossModal() {
  document.getElementById("bossModal").style.display = "flex";
  loadPanelBosses();
}
function openEditBossModal() {
  document.getElementById("editBossModal").style.display = "flex";
  loadActiveBossesForEdit();
}
function selectPanelBoss(card) {
  document.querySelectorAll('.panel-boss-card').forEach(item => item.classList.remove('is-selected'));
  card.classList.add('is-selected');
  const input = document.getElementById('boss_key');
  const submit = document.getElementById('spawnBossSubmit');
  if (input) input.value = card.dataset.bossKey || '';
  if (submit) submit.disabled = !input || !input.value;
}
function escapePanelBossText(value) {
  return String(value ?? '').replace(/[&<>"']/g, char => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;'
  }[char]));
}
function formatPanelNumber(value) {
  return String(value ?? '').replace(/\D/g, '').replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}
function shortPanelBossMap(boss) {
  let mapName = String(boss?.mapName || 'Chua xuat hien').replace(/\s*\(khu\s+\d+\)\s*$/i, '').trim();
  if (!mapName) mapName = 'Chua xuat hien';
  const zoneId = Number(boss?.zoneId);
  return Number.isFinite(zoneId) && zoneId >= 0 ? `${mapName} - ${zoneId}` : mapName;
}
const panelBossSkillOptions = [
  [0,'Đấm Dragon'],[1,'Kamejoko'],[2,'Đấm Demon'],[3,'Masenko'],[4,'Galick'],[5,'Antomic'],
  [6,'Thái Dương Hạ San'],[7,'Trị thương'],[8,'Tái tạo năng lượng'],[9,'Kaioken'],[10,'Quả cầu kênh khi'],
  [11,'Makankosappo'],[12,'Đẻ trứng'],[13,'Biến khỉ'],[14,'Tự sát'],[17,'Liên hoàn'],[18,'Socola'],
  [19,'Khiên năng lượng'],[20,'Dịch chuyển tức thời'],[21,'Huýt sáo'],[22,'Thôi miên'],[23,'Trói'],
  [24,'Super Kame'],[25,'Liên hoàn chưởng'],[26,'Ma Phong Ba'],[27,'Nhát chém năng lượng'],
  [30,'Biến hình'],[31,'Tiến hóa sức mạnh'],[32,'Phân thân'],[35,'Thiên Hà Long Trảm'],
  [36,'Ma Phong Ba Vũ Trụ'],[37,'Ma Sương Sinh Hồi'],[38,'Xung Xoáy Ma Nhĩ'],[39,'Cuồng Nộ Siêu Saiyan'],
  [40,'Tia Chớp Nổ Vũ Trụ'],[41,'Thiên Ma Thần Chưởng'],[42,'Kamejoko Thần Vũ Trụ'],
  [43,'Quả Cầu Băng Giá Supernova'],[44,'Kiếm Khí Trunks'],[45,'Capsule Hẹn Giờ'],[46,'Đại Kiếm Hy Vọng'],
  [47,'Lôi Kiếm Phá Không'],[48,'Lục Quang Phân Rã'],[49,'Dịch Ảnh Liên Trảm'],[50,'Đuôi Độc Hư Không'],
  [51,'Quyền Kích Tương Lai'],[52,'Bộ Pháp Tàn Ảnh'],[53,'Xung Kích Capsule'],[54,'Pháo Sáng Hy Vọng'],
  [55,'Khiên Thời Gian'],[56,'Quả Cầu Hy Vọng'],[57,'Bộc Phá Tương Lai'],[58,'Liên Quyền Sinh Học'],
  [59,'Tia Lục Quang'],[60,'Kén Tái Sinh'],[61,'Vòng Xoáy Hấp Thu'],[62,'Nhà Tù Tế Bào'],
  [63,'Laser Hoàn Mỹ'],[64,'Nổ Lõi Sinh Học'],[65,'Chưởng Xuyên Phá']
];
function bossSkillSelectHtml(selectedId) {
  return panelBossSkillOptions.map(([id,name]) => `<option value="${id}" ${id === Number(selectedId) ? 'selected' : ''}>[${id}] ${name}</option>`).join('');
}
function addBossSkillRow(id = 0, level = 1, cooldown = 1000) {
  const box = document.getElementById('editBossSkillRows');
  if (!box) return;
  const row = document.createElement('div');
  row.className = 'edit-boss-skill-row';
  row.style.cssText = 'display:grid;grid-template-columns:minmax(145px,1fr) 62px 105px 32px;gap:5px;align-items:center;';
  row.innerHTML = `<select class="boss-skill-id" onchange="syncBossSkillInput()" style="min-width:0;padding:6px;border:1px solid var(--bd2);border-radius:5px;background:var(--surf2);color:var(--tx);">${bossSkillSelectHtml(id)}</select>
    <input class="boss-skill-level" type="number" min="1" max="7" value="${Math.max(1,Math.min(7,Number(level)||1))}" title="Cấp skill" oninput="syncBossSkillInput()" style="min-width:0;padding:6px;border:1px solid var(--bd2);border-radius:5px;">
    <input class="boss-skill-cooldown" type="number" min="200" step="100" value="${Math.max(200,Number(cooldown)||1000)}" title="Thời gian ra chiêu (ms)" oninput="syncBossSkillInput()" style="min-width:0;padding:6px;border:1px solid var(--bd2);border-radius:5px;">
    <button type="button" title="Xóa skill" onclick="this.closest('.edit-boss-skill-row').remove();syncBossSkillInput()" style="padding:6px;border:1px solid rgba(217,43,58,.4);border-radius:5px;background:rgba(217,43,58,.1);color:var(--danger);cursor:pointer;">×</button>`;
  box.appendChild(row);
  syncBossSkillInput();
}
function syncBossSkillInput() {
  const hidden = document.getElementById('edit_boss_skills');
  if (!hidden) return;
  hidden.value = Array.from(document.querySelectorAll('#editBossSkillRows .edit-boss-skill-row')).map(row => {
    const id = Number(row.querySelector('.boss-skill-id')?.value || 0);
    const level = Math.max(1, Math.min(7, Number(row.querySelector('.boss-skill-level')?.value || 1)));
    const cooldown = Math.max(200, Number(row.querySelector('.boss-skill-cooldown')?.value || 1000));
    return `${id}:${level}:${cooldown}`;
  }).join(', ');
}
function loadBossSkillEditor(value) {
  const box = document.getElementById('editBossSkillRows');
  if (!box) return;
  box.innerHTML = '';
  String(value || '').split(/[,;\r\n]+/).forEach(part => {
    const match = part.trim().match(/^(\d+)\s*:\s*(\d+)\s*:\s*(\d+)$/);
    if (match) addBossSkillRow(Number(match[1]), Number(match[2]), Number(match[3]));
  });
  if (!box.children.length) addBossSkillRow();
  syncBossSkillInput();
}
function selectEditBoss(card) {
  document.querySelectorAll('#editBossGrid .panel-boss-edit-card').forEach(item => item.classList.remove('is-selected'));
  card.classList.add('is-selected');
  const keyInput = document.getElementById('edit_boss_key');
  const nameInput = document.getElementById('edit_boss_name');
  const hpInput = document.getElementById('edit_boss_hp');
  const dameInput = document.getElementById('edit_boss_dame');
  const armorInput = document.getElementById('edit_boss_armor');
  const targetHpPercentInput = document.getElementById('edit_boss_target_hp_percent');
  const skillsInput = document.getElementById('edit_boss_skills');
  const submit = document.getElementById('editBossSubmit');
  if (keyInput) keyInput.value = card.dataset.bossKey || '';
  if (nameInput) nameInput.value = card.dataset.bossName || '';
  if (hpInput) hpInput.value = formatPanelNumber(card.dataset.bossHp || '');
  if (dameInput) dameInput.value = formatPanelNumber(card.dataset.bossDame || '');
  if (armorInput) armorInput.value = formatPanelNumber(card.dataset.bossArmor || '0');
  if (targetHpPercentInput) targetHpPercentInput.value = Math.max(0, Math.min(100, Number(card.dataset.bossTargetHpPercent || 0)));
  if (skillsInput) skillsInput.value = card.dataset.bossSkills || '';
  loadBossSkillEditor(card.dataset.bossSkills || '');
  if (submit) submit.disabled = !keyInput || !keyInput.value;
}
function syncBossPersistMode(enabled) {
  const toggle = document.getElementById('bossPersistToggle');
  const text = document.getElementById('bossPersistModeText');
  if (toggle) {
    toggle.dataset.enabled = enabled ? '1' : '0';
    toggle.textContent = enabled ? 'Bật' : 'Tắt';
    toggle.classList.toggle('is-on', enabled);
  }
  if (text) text.textContent = enabled ? 'Đang bật' : 'Đang tắt';
}
function toggleBossPersistMode(btn) {
  const nextEnabled = btn.dataset.enabled !== '1';
  const originalText = btn.textContent;
  btn.disabled = true;
  btn.textContent = '...';
  const formData = new FormData();
  formData.set('action', 'boss_persist_mode');
  formData.set('enabled', nextEnabled ? '1' : '0');
  formData.set('csrf_token', '<?= htmlspecialchars($csrfToken, ENT_QUOTES) ?>');
  fetch('', { method: 'POST', headers: csrfHeaders(), body: formData })
    .then(r => r.text())
    .then(html => {
      btn.disabled = false;
      syncBossPersistMode(nextEnabled);
      showMsg(extractPanelMessage(html, 'editBossModal', nextEnabled ? 'Đã bật lưu cấu hình Boss.' : 'Đã tắt lưu cấu hình Boss.'));
    })
    .catch(() => {
      btn.disabled = false;
      btn.textContent = originalText;
      showMsg('Cập nhật chế độ lưu cấu hình boss thất bại!');
    });
}
window._spawnBossListCache = [];
async function loadPanelBosses() {
  const grid = document.getElementById('panelBossGrid');
  const input = document.getElementById('boss_key');
  const submit = document.getElementById('spawnBossSubmit');
  if (!grid) return;
  grid.innerHTML = '<div class="panel-boss-empty">Đang tải dữ liệu boss...</div>';
  if (input) input.value = '';
  if (submit) submit.disabled = true;
  try {
    const response = await fetch('/sv1/boss_status', { cache: 'no-store' });
    const data = await response.json();
    if (!Array.isArray(data.bosses) || data.bosses.length === 0) {
      grid.innerHTML = '<div class="panel-boss-empty">Chưa có boss nào trong hệ thống.</div>';
      return;
    }
    window._spawnBossListCache = data.bosses;
    const searchInput = document.getElementById('searchSpawnBossInput');
    renderSpawnBossList(searchInput ? searchInput.value : '');
  } catch (error) {
    grid.innerHTML = '<div class="panel-boss-empty">Không tải được dữ liệu boss mới nhất.</div>';
  }
}

function renderSpawnBossList(query) {
  const grid = document.getElementById('panelBossGrid');
  if (!grid) return;
  const q = String(query || '').trim().toLowerCase();
  const filtered = (window._spawnBossListCache || []).filter(boss => {
    if (!q) return true;
    const name = String(boss.name || '').toLowerCase();
    const id = String(boss.id || '');
    const map = String(boss.mapName || '').toLowerCase();
    return name.includes(q) || id.includes(q) || map.includes(q);
  });
  if (filtered.length === 0) {
    grid.innerHTML = '<div class="panel-boss-empty" style="grid-column:1/-1;">Không tìm thấy boss phù hợp.</div>';
    return;
  }
  grid.innerHTML = filtered.map(boss => {
    const bossKey = `${boss.id}|${boss.mapId}|${boss.zoneId}`;
    const isAlive = boss.state === 'Sống';
    return `
      <button type="button" class="panel-boss-card" data-boss-key="${escapePanelBossText(bossKey)}" onclick="selectPanelBoss(this)">
        <span class="panel-boss-thumb">
          <span class="panel-boss-state ${isAlive ? 'is-alive' : 'is-dead'}">${isAlive ? '✓' : '×'}</span>
          <img src="/icon/${Number(boss.avatar_id || 1)}.png" alt="${escapePanelBossText(boss.name || '')}" onerror="this.src='/icon/1.png'">
        </span>
        <span class="panel-boss-name">${escapePanelBossText(boss.name || 'Boss không xác định')}</span>
        <span class="panel-boss-meta">${escapePanelBossText(boss.mapName || 'Chưa xuất hiện')}</span>
      </button>
    `;
  }).join('');
}

function filterSpawnBossList(val) {
  renderSpawnBossList(val);
}

window._editBossListCache = [];
async function loadActiveBossesForEdit() {
  const grid = document.getElementById('editBossGrid');
  const keyInput = document.getElementById('edit_boss_key');
  const nameInput = document.getElementById('edit_boss_name');
  const hpInput = document.getElementById('edit_boss_hp');
  const dameInput = document.getElementById('edit_boss_dame');
  const armorInput = document.getElementById('edit_boss_armor');
  const skillsInput = document.getElementById('edit_boss_skills');
  const submit = document.getElementById('editBossSubmit');
  const count = document.getElementById('editBossCount');
  if (!grid) return;
  grid.innerHTML = '<div class="panel-boss-empty">Đang tải dữ liệu boss...</div>';
  if (keyInput) keyInput.value = '';
  if (nameInput) nameInput.value = '';
  if (hpInput) hpInput.value = '';
  if (dameInput) dameInput.value = '';
  if (armorInput) armorInput.value = '';
  if (skillsInput) skillsInput.value = '';
  if (count) count.textContent = '...';
  if (submit) submit.disabled = true;
  try {
    const response = await fetch('/sv1/boss_status', { cache: 'no-store' });
    const data = await response.json();
    const bossMap = new Map();
    (Array.isArray(data.bosses) ? data.bosses : []).forEach(boss => {
      const key = String(boss.configKey || `${boss.id}_${Number(boss.level || 0)}`);
      const current = bossMap.get(key);
      if (!current || (current.state !== 'Sống' && boss.state === 'Sống')) bossMap.set(key, boss);
    });
    const bosses = Array.from(bossMap.values());
    window._editBossListCache = bosses;
    if (count) count.textContent = `${bosses.length} boss`;
    const searchInput = document.getElementById('searchBossEditInput');
    renderEditBossList(searchInput ? searchInput.value : '');
  } catch (error) {
    if (count) count.textContent = '0 boss';
    grid.innerHTML = '<div class="panel-boss-empty">Không tải được dữ liệu boss mới nhất.</div>';
  }
}

function renderEditBossList(query) {
  const grid = document.getElementById('editBossGrid');
  const count = document.getElementById('editBossCount');
  if (!grid) return;
  const q = String(query || '').trim().toLowerCase();
  const filtered = (window._editBossListCache || []).filter(boss => {
    if (!q) return true;
    const name = String(boss.name || '').toLowerCase();
    const id = String(boss.id || '');
    const key = String(boss.configKey || '').toLowerCase();
    return name.includes(q) || id.includes(q) || key.includes(q);
  });
  if (count) count.textContent = `${filtered.length} / ${(window._editBossListCache || []).length} boss`;
  if (filtered.length === 0) {
    grid.innerHTML = '<div class="panel-boss-empty" style="grid-column:1/-1;">Không tìm thấy boss phù hợp.</div>';
    return;
  }
  grid.innerHTML = filtered.map(boss => {
    const bossKey = boss.configKey || `${boss.id}|${Number(boss.level || 0)}`;
    const hpValue = Math.max(1, Number(boss.maxHp || 1));
    const hpNow = Math.max(0, Number(boss.hp || 0));
    const hpMax = Math.max(1, Number(boss.maxHp || 0));
    const hpText = `${formatPanelNumber(hpNow)} / ${formatPanelNumber(hpMax)}`;
    const hpPercent = Math.max(1, Math.min(100, Math.round(hpNow * 100 / hpMax)));
    const shortMap = shortPanelBossMap(boss);
    const skills = Array.isArray(boss.skills) ? boss.skills.map(skill => `${Number(skill.id)}:${Math.max(1,Number(skill.level||1))}:${Math.max(200,Number(skill.cooldown||200))}`).join(', ') : '';
    const isAlive = boss.state === 'Sống';
    return `
      <button type="button" class="panel-boss-edit-card" data-boss-key="${escapePanelBossText(bossKey)}" data-boss-name="${escapePanelBossText(boss.name || '')}" data-boss-map="${escapePanelBossText(shortMap)}" data-boss-hp="${hpValue}" data-boss-dame="${Math.max(1,Number(boss.dame||1))}" data-boss-armor="${Math.max(0,Number(boss.armor||0))}" data-boss-target-hp-percent="${Math.max(0,Math.min(100,Number(boss.targetHpPercent||0)))}" data-boss-skills="${escapePanelBossText(skills)}" onclick="selectEditBoss(this)">
        <span class="panel-boss-edit-avatar">
          <span class="panel-boss-edit-status" style="background:${isAlive ? 'var(--success)' : 'var(--danger)'}">${isAlive ? '✓' : '×'}</span>
          <img src="/icon/${Number(boss.avatar_id || 1)}.png" alt="${escapePanelBossText(boss.name || '')}" onerror="this.src='/icon/1.png'">
        </span>
        <span class="panel-boss-edit-main">
          <span class="panel-boss-edit-name">${escapePanelBossText(boss.name || 'Boss không xác định')}</span>
          <span class="panel-boss-edit-map">${escapePanelBossText(shortMap)}</span>
        </span>
        <span class="panel-boss-edit-stats">
          <span class="panel-boss-edit-hp-text"><span>${hpText}</span></span>
          <span class="panel-boss-edit-hpbar"><span class="panel-boss-edit-hpfill" style="width:${hpPercent}%"></span></span>
        </span>
      </button>
    `;
  }).join('');
}

function filterEditBossList(val) {
  renderEditBossList(val);
}

function extractPanelMessage(html, modalId, fallback) {
  const doc = new DOMParser().parseFromString(html, 'text/html');
  const msg = doc.querySelector('#' + modalId + ' .panel-action-message');
  return msg ? msg.textContent.trim() : fallback;
}
function finishPanelAction(modalId, message) {
  const modal = document.getElementById(modalId);
  if (modal) modal.style.display = 'none';
  showMsg(message);
}
function syncCurrentRate(html, sourceId, targetId) {
  const doc = new DOMParser().parseFromString(html, 'text/html');
  const nextValue = doc.getElementById(sourceId);
  const currentValue = document.getElementById(targetId);
  if (nextValue && currentValue) currentValue.textContent = nextValue.textContent.trim();
}
function formatBuffAmountInput(input) {
  const digits = (input.value || '').replace(/\D/g, '');
  input.value = digits.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}
const buffAmountInput = document.getElementById('buff_amount');
if (buffAmountInput) {
  buffAmountInput.addEventListener('input', function(){ formatBuffAmountInput(this); });
}
function submitBuffForm(event) {
  event.preventDefault();
  const form = event.target;
  const amountInput = document.getElementById('buff_amount');
  const btn = form.querySelector('button[type="submit"]');
  const originalText = btn.innerHTML;
  btn.innerHTML = '⏳ Đang xử lý...';
  btn.disabled = true;

  const formData = new FormData(form);
  if (amountInput) formData.set('amount', (amountInput.value || '').replace(/\D/g, ''));

  fetch('', { method: 'POST', headers: csrfHeaders(), body: formData })
    .then(r => r.text())
    .then(html => { btn.innerHTML = originalText; btn.disabled = false; finishPanelAction('buffModal', extractPanelMessage(html, 'buffModal', 'Đã tặng Cá thành công.')); })
    .catch(() => { btn.innerHTML = originalText; btn.disabled = false; showMsg("❌ Tặng Cá thất bại!"); });
}

function submitKickForm(event) {
  event.preventDefault();
  const form = event.target;
  const btn = form.querySelector('button[type="submit"]');
  const originalText = btn.innerHTML;
  btn.innerHTML = '⏳ Đang xử lý...';
  btn.disabled = true;

  fetch('', { method: 'POST', headers: csrfHeaders(), body: new FormData(form) })
    .then(r => r.text())
    .then(html => { btn.innerHTML = originalText; btn.disabled = false; finishPanelAction('kickModal', extractPanelMessage(html, 'kickModal', 'Đã gửi lệnh kick người chơi.')); })
    .catch(() => { btn.innerHTML = originalText; btn.disabled = false; showMsg("❌ Kick thất bại!"); });
}

function submitAdminModeForm(event) {
  event.preventDefault();
  const form = event.target;
  const btn = form.querySelector('button[type="submit"]');
  const originalText = btn.innerHTML;
  btn.innerHTML = '⏳ Đang xử lý...';
  btn.disabled = true;

  fetch('', { method: 'POST', headers: csrfHeaders(), body: new FormData(form) })
    .then(r => r.text())
    .then(html => {
      btn.innerHTML = originalText;
      btn.disabled = false;
      syncAdminModeState(html);
      finishPanelAction('adminModeModal', extractPanelMessage(html, 'adminModeModal', 'Đã cập nhật chế độ admin.'));
    })
    .catch(() => { btn.innerHTML = originalText; btn.disabled = false; showMsg("❌ Cập nhật chế độ admin thất bại!"); });
}

function syncAdminModeState(html) {
  const doc = new DOMParser().parseFromString(html, 'text/html');
  const nextMode = doc.getElementById('currentAdminMode');
  const currentMode = document.getElementById('currentAdminMode');
  const nextInput = doc.getElementById('admin_mode_enabled');
  const currentInput = document.getElementById('admin_mode_enabled');
  const nextButton = doc.querySelector('#adminModeModal button[type="submit"]');
  const currentButton = document.querySelector('#adminModeModal button[type="submit"]');
  if (nextMode && currentMode) {
    currentMode.textContent = nextMode.textContent.trim();
    currentMode.style.color = nextMode.style.color;
  }
  if (nextInput && currentInput) currentInput.value = nextInput.value;
  if (nextButton && currentButton) currentButton.innerHTML = nextButton.innerHTML;
}

function submitXnxxModeForm(event) {
  event.preventDefault();
  const form = event.target;
  const btn = form.querySelector('button[type="submit"]');
  const originalText = btn.innerHTML;
  btn.innerHTML = '⏳ Đang xử lý...';
  btn.disabled = true;

  fetch('', { method: 'POST', headers: csrfHeaders(), body: new FormData(form) })
    .then(r => r.text())
    .then(html => {
      btn.innerHTML = originalText;
      btn.disabled = false;
      syncXnxxModeState(html);
      finishPanelAction('xnxxModeModal', extractPanelMessage(html, 'xnxxModeModal', 'Đã cập nhật XNXX.'));
    })
    .catch(() => { btn.innerHTML = originalText; btn.disabled = false; showMsg("❌ Cập nhật XNXX thất bại!"); });
}

function syncXnxxModeState(html) {
  const doc = new DOMParser().parseFromString(html, 'text/html');
  const nextMode = doc.getElementById('currentXnxxMode');
  const currentMode = document.getElementById('currentXnxxMode');
  const nextInput = doc.getElementById('xnxx_mode_enabled');
  const currentInput = document.getElementById('xnxx_mode_enabled');
  const nextButton = doc.querySelector('#xnxxModeModal button[type="submit"]');
  const currentButton = document.querySelector('#xnxxModeModal button[type="submit"]');
  if (nextMode && currentMode) {
    currentMode.textContent = nextMode.textContent.trim();
    currentMode.style.color = nextMode.style.color;
  }
  if (nextInput && currentInput) currentInput.value = nextInput.value;
  if (nextButton && currentButton) currentButton.innerHTML = nextButton.innerHTML;
}
function submitExpForm(event) {
  event.preventDefault();
  const form = event.target;
  const input = document.getElementById('exp_rate');
  const btn = form.querySelector('button[type="submit"]');
  const originalText = btn.innerHTML;
  btn.innerHTML = '⏳ Đang xử lý...';
  btn.disabled = true;

  const formData = new FormData(form);
  if (input) formData.set('exp_rate', (input.value || '').replace(/\D/g, ''));

  fetch('', { method: 'POST', headers: csrfHeaders(), body: formData })
    .then(r => r.text())
    .then(html => { btn.innerHTML = originalText; btn.disabled = false; syncCurrentRate(html, 'currentExpRate', 'currentExpRate'); finishPanelAction('expModal', extractPanelMessage(html, 'expModal', 'Đã tăng EXP server thành công.')); })
    .catch(() => { btn.innerHTML = originalText; btn.disabled = false; showMsg("❌ Set EXP thất bại!"); });
}
function submitDropForm(event) {
  event.preventDefault();
  const form = event.target;
  const input = document.getElementById('drop_rate');
  const btn = form.querySelector('button[type="submit"]');
  const originalText = btn.innerHTML;
  btn.innerHTML = '⏳ Đang xử lý...';
  btn.disabled = true;

  const formData = new FormData(form);
  if (input) formData.set('drop_rate', (input.value || '').replace(/\D/g, ''));

  fetch('', { method: 'POST', headers: csrfHeaders(), body: formData })
    .then(r => r.text())
    .then(html => { btn.innerHTML = originalText; btn.disabled = false; syncCurrentRate(html, 'currentDropRate', 'currentDropRate'); finishPanelAction('dropModal', extractPanelMessage(html, 'dropModal', 'Đã chỉnh tỉ lệ rơi đồ server thành công.')); })
    .catch(() => { btn.innerHTML = originalText; btn.disabled = false; showMsg("❌ Set rơi đồ thất bại!"); });
}

function submitBossForm(event) {
  event.preventDefault();
  const form = event.target;
  const btn = form.querySelector('button[type="submit"]');
  const originalText = btn.innerHTML;
  btn.innerHTML = 'Dang xu ly...';
  btn.disabled = true;

  fetch('', { method: 'POST', headers: csrfHeaders(), body: new FormData(form) })
    .then(r => r.text())
    .then(html => { btn.innerHTML = originalText; btn.disabled = false; finishPanelAction('bossModal', extractPanelMessage(html, 'bossModal', 'Đã gửi lệnh gọi Boss.')); })
    .catch(() => { btn.innerHTML = originalText; btn.disabled = false; showMsg("❌ Gọi Boss thất bại!"); });
}

const editBossHpInput = document.getElementById('edit_boss_hp');
if (editBossHpInput) {
  editBossHpInput.addEventListener('input', function(){ this.value = formatPanelNumber(this.value); });
}
['edit_boss_dame','edit_boss_armor'].forEach(id => {
  const input = document.getElementById(id);
  if (input) input.addEventListener('input', function(){ this.value = formatPanelNumber(this.value); });
});
function submitEditBossForm(event) {
  event.preventDefault();
  syncBossSkillInput();
  const form = event.target;
  const hpInput = document.getElementById('edit_boss_hp');
  const dameInput = document.getElementById('edit_boss_dame');
  const armorInput = document.getElementById('edit_boss_armor');
  const btn = form.querySelector('button[type="submit"]');
  const originalText = btn.innerHTML;
  btn.innerHTML = 'Dang xu ly...';
  btn.disabled = true;

  const formData = new FormData(form);
  if (hpInput) formData.set('boss_hp', (hpInput.value || '').replace(/\D/g, ''));
  if (dameInput) formData.set('boss_dame', (dameInput.value || '').replace(/\D/g, ''));
  if (armorInput) formData.set('boss_armor', (armorInput.value || '').replace(/\D/g, ''));

  fetch('', { method: 'POST', headers: csrfHeaders(), body: formData })
    .then(r => r.text())
    .then(html => {
      btn.innerHTML = originalText;
      btn.disabled = false;
      finishPanelAction('editBossModal', extractPanelMessage(html, 'editBossModal', 'Đã gửi lệnh sửa Boss.'));
      setTimeout(loadActiveBossesForEdit, 2500);
    })
    .catch(() => { btn.innerHTML = originalText; btn.disabled = false; showMsg("❌ Sửa Boss thất bại!"); });
}

function submitMaintenanceForm(event) {
  event.preventDefault();
  const form = event.target;
  const btn = form.querySelector('button[type="submit"]');
  const originalText = btn.innerHTML;
  btn.innerHTML = '⏳ Đang xử lý...';
  btn.disabled = true;

  fetch('', { method: 'POST', headers: csrfHeaders(), body: new FormData(form) })
    .then(r => r.text())
    .then(html => { btn.innerHTML = originalText; btn.disabled = false; finishPanelAction('maintenanceModal', extractPanelMessage(html, 'maintenanceModal', 'Lệnh bảo trì đã được gửi.')); })
    .catch(() => { btn.innerHTML = originalText; btn.disabled = false; showMsg("❌ Bảo trì thất bại!"); });
}

// LOA LINKS
let links = JSON.parse(localStorage.getItem("loa_links") || "[]");
function toggleForm() { const f = document.getElementById("loaForm"); f.style.display = f.style.display === "none" ? "block" : "none"; renderLinks(); }
function addLink() {
  const val = document.getElementById("loaInput").value.trim();
  if (!val) return showMsg("⚠️ Nhập Link hoặc Key!");
  links.push(val); localStorage.setItem("loa_links", JSON.stringify(links));
  document.getElementById("loaInput").value = ""; renderLinks();
}
function renderLinks() {
  const box = document.getElementById("linkList");
  box.innerHTML = "";
  if (links.length === 0) { box.innerHTML = "<div style='font-size:11px;color:var(--tx3)'>Chưa có link</div>"; return; }
  links.forEach(function(l, i) {
    const div = document.createElement("div");
    div.style.cssText = "display:flex;justify-content:space-between;align-items:center;padding:8px;border:1px solid var(--bd);border-radius:var(--rs);font-size:0.85rem;";
    div.innerHTML = `<span style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;max-width:180px;">${l}</span>
      <div style="display:flex;gap:4px;">
        <button onclick="openGoogle(${i})" style="font-size:11px;cursor:pointer;border:none;background:none;">🔍</button>
        <button onclick="removeLink(${i})" style="font-size:11px;cursor:pointer;border:none;background:none;">❌</button>
      </div>`;
    box.appendChild(div);
  });
  document.getElementById("loaStatus").innerText = `Có ${links.length} link`;
}
function openGoogle(i) { window.open("https://www.google.com/search?q=" + encodeURIComponent(links[i]), "_blank"); }
function removeLink(i) { links.splice(i, 1); localStorage.setItem("loa_links", JSON.stringify(links)); renderLinks(); }

function openTopupModal(e) {
  e.preventDefault();
  document.getElementById("topupModal").style.display = "flex";
}
function switchTopupTab(tab) {
  const tabCardBtn = document.getElementById('tabCardBtn');
  const tabAtmBtn = document.getElementById('tabAtmBtn');
  const tabCardContent = document.getElementById('tabCardContent');
  const tabAtmContent = document.getElementById('tabAtmContent');
  
  if (tab === 'card') {
    tabCardBtn.style.background = 'var(--surf2)';
    tabCardBtn.style.color = 'var(--tx2)';
    tabAtmBtn.style.background = 'var(--surf)';
    tabAtmBtn.style.color = 'var(--tx)';
    tabCardContent.style.display = 'block';
    tabAtmContent.style.display = 'none';
  } else {
    tabCardBtn.style.background = 'var(--surf)';
    tabCardBtn.style.color = 'var(--tx)';
    tabAtmBtn.style.background = 'var(--surf2)';
    tabAtmBtn.style.color = 'var(--tx2)';
    tabCardContent.style.display = 'none';
    tabAtmContent.style.display = 'block';
  }
}
const atmAmountInput = document.getElementById('atm_amount');
if (atmAmountInput) {
  atmAmountInput.addEventListener('input', function(){ this.value = formatPanelNumber(this.value); });
}

function openReviewTopupModal() {
  document.getElementById("reviewTopupModal").style.display = "flex";
}

function openLookupIconModal() {
  document.getElementById("lookupIconModal").style.display = "flex";
  document.getElementById("lookupIconSearch").value = "";
  document.getElementById("lookupIconSearch").focus();
  document.getElementById("lookupIconGrid").innerHTML = '<div style="grid-column:1/-1; text-align:center; padding:40px; color:var(--tx3); font-size:0.95rem;">Nhập từ khóa tìm kiếm phía trên để hiển thị danh sách vật phẩm.</div>';
}
let searchTimeout = null;
function searchGameIcons(query) {
  clearTimeout(searchTimeout);
  const grid = document.getElementById("lookupIconGrid");
  if (!query.trim()) {
    grid.innerHTML = '<div style="grid-column:1/-1; text-align:center; padding:40px; color:var(--tx3); font-size:0.95rem;">Nhập từ khóa tìm kiếm phía trên để hiển thị danh sách vật phẩm.</div>';
    return;
  }
  grid.innerHTML = '<div style="grid-column:1/-1; text-align:center; padding:40px; color:var(--tx3); font-size:0.95rem;">⏳ Đang tìm kiếm...</div>';
  
  searchTimeout = setTimeout(() => {
    fetch(`?action=search_icons&query=${encodeURIComponent(query)}`)
      .then(r => r.json())
      .then(data => {
        if (!data || data.length === 0) {
          grid.innerHTML = '<div style="grid-column:1/-1; text-align:center; padding:40px; color:var(--danger); font-size:0.95rem;">❌ Không tìm thấy vật phẩm nào khớp!</div>';
          return;
        }
        grid.innerHTML = data.map(item => `
          <div class="item-card" title="${escapePanelBossText(item.name)}">
            <img src="/icon/${Number(item.icon_id || 1)}.png" alt="" onerror="this.src='/icon/1.png'">
            <span class="ic-id">ID: ${item.id}</span>
            <span class="ic-name">${escapePanelBossText(item.name)}</span>
            <span style="font-size:0.6rem; color:var(--tx3)">Icon: ${item.icon_id}</span>
          </div>
        `).join('');
      })
      .catch(() => {
        grid.innerHTML = '<div style="grid-column:1/-1; text-align:center; padding:40px; color:var(--danger); font-size:0.95rem;">❌ Lỗi kết nối máy chủ!</div>';
      });
  }, 300);
}

// ESC đóng modal
document.addEventListener('keydown', function(e) {
  if (e.key === 'Escape') ['customDropModal','logoutModal','msgBox','resetModal','buffModal','kickModal','maintenanceModal','expModal','dropModal','bossModal','topupModal','reviewTopupModal','lookupIconModal'].forEach(function(id){ document.getElementById(id).style.display = 'none'; });
});

// ── CUSTOM DROPS SYSTEM JS (THƯỜNG & SỰ KIỆN - CHẶN/XÓA ĐỒ SOURCE) ──
let currentDropTab = 'mob';       // 'mob' or 'boss'
let currentCategoryTab = 'normal'; // 'normal' or 'event'
let customDropsData = {
  mob_drops: [],
  boss_drops: [],
  blocked_boss_drops: [],
  blocked_mob_drops: [],
  blocked_events: []
};
let defaultDropsCatalog = {
  boss_drops: [],
  mob_normal_drops: [],
  mob_event_drops: []
};
let cachedMobTemplates = [];
let itemSearchTimeout = null;
let selectedDropItems = [];

function openCustomDropModal() {
  document.getElementById('customDropModal').style.display = 'flex';
  loadMobTemplates();
  loadCustomDrops();
}

function closeCustomDropModal() {
  document.getElementById('customDropModal').style.display = 'none';
}

function switchCustomDropTab(tab) {
  currentDropTab = tab;
  const mobBtn = document.getElementById('tabBtnMobDrop');
  const bossBtn = document.getElementById('tabBtnBossDrop');
  const categoryTabs = document.getElementById('dropCategoryTabs');
  const categoryWrap = document.getElementById('wrap_drop_category');
  const pickupWrap = document.getElementById('wrap_pickup_type');
  const thPickup = document.getElementById('th_pickup');

  if (tab === 'boss') {
    currentCategoryTab = 'normal';
    if (categoryTabs) categoryTabs.style.display = 'none';
    if (categoryWrap) categoryWrap.style.display = 'none';
    mobBtn.style.background = 'var(--surf)';
    mobBtn.style.border = '1.5px solid var(--bd2)';
    mobBtn.style.color = 'var(--tx2)';
    bossBtn.style.background = 'var(--surf2)';
    bossBtn.style.border = '1.5px solid var(--blue2)';
    bossBtn.style.color = 'var(--blue2)';
    if (pickupWrap) pickupWrap.style.display = 'block';
    if (thPickup) thPickup.style.display = 'table-cell';
  } else {
    if (categoryTabs) categoryTabs.style.display = 'flex';
    if (categoryWrap) categoryWrap.style.display = 'block';
    bossBtn.style.background = 'var(--surf)';
    bossBtn.style.border = '1.5px solid var(--bd2)';
    bossBtn.style.color = 'var(--tx2)';
    mobBtn.style.background = 'var(--surf2)';
    mobBtn.style.border = '1.5px solid var(--blue2)';
    mobBtn.style.color = 'var(--blue2)';
    if (pickupWrap) pickupWrap.style.display = 'none';
    if (thPickup) thPickup.style.display = 'none';
  }

  updateSectionTitles();
  resetDropForm();
  renderDefaultDropsCatalog();
  renderCustomDropsTable();
}

function switchDropCategory(cat) {
  if (currentDropTab === 'boss') cat = 'normal';
  currentCategoryTab = cat;
  const normalBtn = document.getElementById('catBtnNormal');
  const eventBtn = document.getElementById('catBtnEvent');

  if (cat === 'event') {
    normalBtn.style.background = 'transparent';
    normalBtn.style.color = 'var(--tx2)';
    eventBtn.style.background = 'var(--blue2)';
    eventBtn.style.color = '#fff';
  } else {
    eventBtn.style.background = 'transparent';
    eventBtn.style.color = 'var(--tx2)';
    normalBtn.style.background = 'var(--blue2)';
    normalBtn.style.color = '#fff';
  }

  document.getElementById('cd_category').value = cat;
  const catSel = document.getElementById('cd_category_select');
  if (catSel) catSel.value = cat;

  updateSectionTitles();
  resetDropForm();
  renderDefaultDropsCatalog();
  renderCustomDropsTable();
}

function onFormCategoryChange(val) {
  currentCategoryTab = val;
  switchDropCategory(val);
}

function updateSectionTitles() {
  const catLabel = currentDropTab === 'boss'
    ? '📦 Tất Cả Đồ Rơi'
    : (currentCategoryTab === 'event' ? '🎉 Sự Kiện' : '⚙️ Đồ Thường');
  const targetLabel = currentDropTab === 'boss' ? 'Boss' : 'Quái';
  
  const defTitle = document.getElementById('defaultDropSectionTitle');
  if (defTitle) defTitle.textContent = `Đồ Mặc Định Trong Source (${targetLabel} - ${catLabel})`;

  const formTitle = document.getElementById('dropFormTitle');
  if (formTitle) formTitle.textContent = `Thêm Quy Tắc Rơi ${targetLabel} (${catLabel})`;

  const rulesTitle = document.getElementById('customRulesSectionTitle');
  if (rulesTitle) rulesTitle.textContent = `Quy Tắc Tự Cấu Hình (${targetLabel} - ${catLabel})`;
}

async function loadMobTemplates() {
  if (cachedMobTemplates.length > 0) return;
  try {
    const res = await fetch('/sv1/admin/panel.php?action=get_mob_templates', { cache: 'no-store' });
    const data = await res.json();
    if (Array.isArray(data)) {
      cachedMobTemplates = data;
      populateMobSelect();
    }
  } catch (e) {}
}

function populateMobSelect() {
  const sel = document.getElementById('cd_target_mob');
  if (!sel) return;
  sel.innerHTML = '<option value="0">-- Chọn Quái --</option>' +
    cachedMobTemplates.map(m => `<option value="${m.id}">[ID ${m.id}] ${escapePanelBossText(m.name)}</option>`).join('');
}

async function loadCustomDrops() {
  try {
    const res = await fetch('/sv1/admin/panel.php?action=get_custom_drops', { cache: 'no-store' });
    const result = await res.json();
    if (result && result.custom_drops) {
      customDropsData = result.custom_drops;
    } else if (result && (result.mob_drops || result.boss_drops)) {
      customDropsData = result;
    }
    if (result && result.default_catalog) {
      defaultDropsCatalog = result.default_catalog;
    }
    renderDefaultDropsCatalog();
    renderCustomDropsTable();
  } catch (e) {}
}

// ── RENDER DEFAULT DROPS CATALOG WITH BLOCK ACTIONS ──
function renderDefaultDropsCatalog() {
  const box = document.getElementById('defaultDropsCatalogBox');
  const countBadge = document.getElementById('defaultCatalogCountBadge');
  if (!box) return;

  const query = (document.getElementById('searchDefaultDropsInput')?.value || '').toLowerCase().trim();
  const blockedBossList = customDropsData.blocked_boss_drops || [];
  const blockedMobList = customDropsData.blocked_mob_drops || [];

  let html = '';
  let totalMatches = 0;

  if (currentDropTab === 'boss') {
    const bossList = defaultDropsCatalog.boss_drops || [];
    const selectedBoss = document.getElementById('cd_target_boss');
    // BossID của server chủ yếu là số âm; chỉ 0 mới là dòng "chưa chọn".
    if (Number(selectedBoss?.value || 0) === 0) {
      box.innerHTML = '<div style="padding:34px 12px;text-align:center;color:var(--tx3);">Chọn một Boss ở form bên trái để xem các vật phẩm rơi.</div>';
      if (countBadge) countBadge.textContent = '0 món';
      return;
    }
    bossList.forEach(boss => {
      const targetType = document.getElementById('cd_target_type')?.value || 'all';
      const bossSelect = document.getElementById('cd_target_boss');
      const selectedBossId = Number(bossSelect?.value || 0);
      const selectedBossKey = bossSelect?.options[bossSelect.selectedIndex]?.getAttribute('data-config-key') || '';
      if (targetType === 'boss_id' && selectedBossId !== 0 && boss.boss_key !== 'GLOBAL_BOSS') {
        const sameId = Number(boss.boss_id || 0) === selectedBossId;
        const sameKey = selectedBossKey && String(boss.boss_key || '').toLowerCase() === selectedBossKey.toLowerCase();
        const sameSourceClass = selectedBossKey.startsWith('CAI_TRANG_') && boss.boss_key === 'HalloweenCaiTrangBoss';
        if (!sameId && !sameKey && !sameSourceClass) return;
      }
      // Boss phải hiện toàn bộ nguồn rơi thực tế. Tab phân loại của Boss đã bị bỏ,
      // vì vậy không được âm thầm loại các món mang nhãn event.
      const filteredItems = (boss.items || []).filter(it => {
        if (!query) return true;
        return boss.boss_name.toLowerCase().includes(query) ||
               (boss.boss_key && boss.boss_key.toLowerCase().includes(query)) ||
               it.name.toLowerCase().includes(query) ||
               String(it.id).includes(query);
      });

      if (filteredItems.length === 0) return;
      totalMatches += filteredItems.length;

      // Check if all default drops are blocked for this boss
      const isAllBlocked = blockedBossList.some(b => 
        (b.boss_key === boss.boss_key || (b.boss_id !== 0 && b.boss_id === boss.boss_id)) && b.item_id === -1
      );

      html += `
        <div style="background:var(--surf);border:1px solid var(--bd2);border-radius:8px;padding:9px;margin-bottom:6px;">
          <div style="display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid var(--bd2);padding-bottom:6px;margin-bottom:6px;">
            <div style="display:flex;align-items:center;gap:6px;">
              <strong style="color:var(--blue2);font-size:0.9rem;">💀 ${escapePanelBossText(boss.boss_name)}</strong>
              <span style="font-size:0.75rem;color:var(--tx3);">[Key: ${boss.boss_key || 'N/A'}]</span>
            </div>
            <div>
              ${isAllBlocked 
                ? `<button onclick="unblockAllBossDefault('${boss.boss_key}')" style="padding:3px 8px;font-size:0.75rem;border-radius:4px;border:1px solid rgba(10,138,90,.4);background:rgba(10,138,90,.15);color:var(--success);cursor:pointer;font-weight:700;">✓ Mở Lại Tất Cả</button>`
                : `<button onclick="blockAllBossDefault('${boss.boss_key}', ${boss.boss_id})" style="padding:3px 8px;font-size:0.75rem;border-radius:4px;border:1px solid rgba(217,43,58,.4);background:rgba(217,43,58,.1);color:var(--danger);cursor:pointer;font-weight:700;">🚫 Tắt toàn bộ đồ Boss này</button>`
              }
            </div>
          </div>
          <div style="display:grid;grid-template-columns:repeat(auto-fill, minmax(280px, 1fr));gap:6px;">
            ${filteredItems.map(it => {
              const isBlocked = isAllBlocked || blockedBossList.some(b => 
                (b.boss_key === boss.boss_key || (b.boss_id !== 0 && b.boss_id === boss.boss_id)) && b.item_id === it.id
              );
              return `
                <div class="drop-item-row" style="display:flex;align-items:center;justify-content:space-between;padding:6px 8px;background:${isBlocked ? 'rgba(217,43,58,.08)' : 'var(--surf2)'};border:1px solid ${isBlocked ? 'rgba(217,43,58,.3)' : 'var(--bd2)'};border-radius:6px;">
                  <div class="drop-item-main">
                    <img class="cd-item-thumb" loading="lazy" src="/icon/${Number(it.icon || 1)}.png" onerror="this.onerror=null;this.src='/icon/1.png'">
                    <div>
                      <div class="drop-item-name" style="font-weight:700;font-size:0.83rem;color:${isBlocked ? 'var(--tx3)' : 'var(--tx)'};${isBlocked ? 'text-decoration:line-through;' : ''}">
                        ${escapePanelBossText(it.name)}
                      </div>
                      <div style="font-size:0.72rem;color:var(--tx3);display:flex;gap:4px;align-items:center;">
                        <span>ID: ${it.id}</span>
                        ${it.event_name ? `<span style="background:rgba(59,130,246,.15);color:var(--blue2);padding:1px 4px;border-radius:3px;">${it.event_name}</span>` : ''}
                      </div>
                    </div>
                  </div>
                  <div>
                    ${isBlocked 
                      ? `<button onclick="unblockDefaultDrop('boss', '${boss.boss_key}', 0, ${it.id})" style="padding:3px 7px;font-size:0.74rem;border-radius:4px;border:1px solid rgba(10,138,90,.4);background:rgba(10,138,90,.15);color:var(--success);cursor:pointer;font-weight:700;">✓ Khôi phục</button>`
                      : `<button onclick="blockDefaultDrop('boss', '${boss.boss_key}', ${boss.boss_id}, 0, ${it.id}, '${currentCategoryTab}')" style="padding:3px 7px;font-size:0.74rem;border-radius:4px;border:1px solid rgba(217,43,58,.4);background:rgba(217,43,58,.1);color:var(--danger);cursor:pointer;font-weight:700;">🗑️ Xóa / Chặn</button>`
                    }
                  </div>
                </div>
              `;
            }).join('')}
          </div>
        </div>
      `;
    });
  } else {
    // MOB DROPS
    const mobItemList = currentCategoryTab === 'event' 
      ? (defaultDropsCatalog.mob_event_drops || [])
      : (defaultDropsCatalog.mob_normal_drops || []);

    const filtered = mobItemList.filter(it => {
      if (!query) return true;
      return it.name.toLowerCase().includes(query) ||
             (it.event_name && it.event_name.toLowerCase().includes(query)) ||
             (it.desc && it.desc.toLowerCase().includes(query)) ||
             String(it.id).includes(query);
    });

    totalMatches = filtered.length;

    html = `
      <div style="display:grid;grid-template-columns:repeat(auto-fill, minmax(280px, 1fr));gap:6px;">
        ${filtered.map(it => {
          const isBlocked = blockedMobList.some(b => b.item_id === it.id);
          return `
            <div class="drop-item-row" style="display:flex;align-items:center;justify-content:space-between;padding:6px 8px;background:${isBlocked ? 'rgba(217,43,58,.08)' : 'var(--surf)'};border:1px solid ${isBlocked ? 'rgba(217,43,58,.3)' : 'var(--bd2)'};border-radius:6px;">
              <div class="drop-item-main">
                <img class="cd-item-thumb" loading="lazy" src="/icon/${Number(it.icon || 1)}.png" onerror="this.onerror=null;this.src='/icon/1.png'">
                <div>
                  <div class="drop-item-name" style="font-weight:700;font-size:0.83rem;color:${isBlocked ? 'var(--tx3)' : 'var(--tx)'};${isBlocked ? 'text-decoration:line-through;' : ''}">
                    ${escapePanelBossText(it.name)}
                  </div>
                  <div style="font-size:0.72rem;color:var(--tx3);display:flex;gap:4px;align-items:center;flex-wrap:wrap;">
                    <span>ID: ${it.id}</span>
                    ${it.event_name ? `<span style="background:rgba(59,130,246,.15);color:var(--blue2);padding:1px 4px;border-radius:3px;">${it.event_name}</span>` : ''}
                    ${it.desc ? `<span style="color:var(--gold);">${it.desc}</span>` : ''}
                  </div>
                </div>
              </div>
              <div>
                ${isBlocked 
                  ? `<button onclick="unblockDefaultDrop('mob', '', 0, ${it.id})" style="padding:3px 7px;font-size:0.74rem;border-radius:4px;border:1px solid rgba(10,138,90,.4);background:rgba(10,138,90,.15);color:var(--success);cursor:pointer;font-weight:700;">✓ Khôi phục</button>`
                  : `<button onclick="blockDefaultDrop('mob', '', 0, 0, ${it.id}, '${currentCategoryTab}')" style="padding:3px 7px;font-size:0.74rem;border-radius:4px;border:1px solid rgba(217,43,58,.4);background:rgba(217,43,58,.1);color:var(--danger);cursor:pointer;font-weight:700;">🗑️ Xóa / Chặn</button>`
                }
              </div>
            </div>
          `;
        }).join('')}
      </div>
    `;
  }

  if (totalMatches === 0) {
    box.innerHTML = '<div style="text-align:center;padding:20px;color:var(--tx3);">Không tìm thấy vật phẩm mặc định nào phù hợp với bộ lọc hiện tại.</div>';
  } else {
    box.innerHTML = html;
  }
  if (countBadge) countBadge.textContent = `${totalMatches} món`;
}

// ── RENDER CUSTOM RULES TABLE ──
function renderCustomDropsTable() {
  const tbody = document.getElementById('customDropRulesTbody');
  const countBadge = document.getElementById('customRulesCountBadge');
  if (!tbody) return;

  const rawList = currentDropTab === 'boss'
    ? (customDropsData.boss_drops || [])
    : (customDropsData.mob_drops || []);

  // Filter by category
  const targetType = document.getElementById('cd_target_type')?.value || 'all';
  const selectedBossId = Number(document.getElementById('cd_target_boss')?.value || 0);
  const selectedMobId = Number(document.getElementById('cd_target_mob')?.value || 0);
  if (currentDropTab === 'boss' && selectedBossId === 0) {
    if (countBadge) countBadge.textContent = '0 quy tắc';
    tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;padding:28px;color:var(--tx3);">Chọn một Boss để xem hoặc thêm vật phẩm rơi.</td></tr>';
    return;
  }
  const list = rawList.filter(r => {
    if (currentDropTab !== 'boss' && (r.category || 'normal') !== currentCategoryTab) return false;
    if (currentDropTab === 'boss' && targetType === 'boss_id' && selectedBossId !== 0) {
      const bossSelect = document.getElementById('cd_target_boss');
      const selectedBossKey = bossSelect?.options[bossSelect.selectedIndex]?.getAttribute('data-config-key') || '';
      return r.target_type === 'all'
        || (r.target_type === 'boss_id' && Number(r.target_id) === selectedBossId)
        || (r.target_type === 'config_key' && selectedBossKey && String(r.config_key || '').toLowerCase() === selectedBossKey.toLowerCase());
    }
    if (currentDropTab === 'mob' && targetType === 'mob' && selectedMobId > 0) {
      return r.target_type === 'all' || (r.target_type === 'mob' && Number(r.target_id) === selectedMobId);
    }
    return true;
  });

  if (countBadge) countBadge.textContent = `${list.length} quy tắc`;

  if (list.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="8" style="text-align:center;padding:22px;color:var(--tx3);">
          Chưa có quy tắc rơi tự cấu hình nào cho <strong>${currentDropTab === 'boss' ? 'Boss đã chọn' : 'Quái'}</strong>${currentDropTab === 'boss' ? '' : ` (${currentCategoryTab === 'event' ? 'Sự Kiện' : 'Đồ Thường'})`}. Hãy thêm ở form bên trái!
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = list.map(r => {
    let targetText = 'Tất cả';
    if (r.target_type === 'mob') {
      targetText = `Quái ID ${r.target_id}`;
    } else if (r.target_type === 'boss_id') {
      targetText = `Boss ID ${r.target_id}`;
    } else if (r.target_type === 'config_key') {
      targetText = `Boss Key: ${r.config_key || ''}`;
    } else if (r.target_type === 'map') {
      targetText = `Map ID ${r.target_id}`;
    }
    if (r.target_name) {
      targetText += ` (${r.target_name})`;
    }

    const catBadge = (r.category === 'event')
      ? '<span style="background:rgba(59,130,246,.15);color:var(--blue2);padding:2px 5px;border-radius:4px;font-size:0.75rem;">🎉 Sự Kiện</span>'
      : '<span style="background:var(--surf);color:var(--tx3);padding:2px 5px;border-radius:4px;font-size:0.75rem;border:1px solid var(--bd2);">⚙️ Thường</span>';

    const qtyText = (r.min_quantity === r.max_quantity) ? `${r.min_quantity}` : `${r.min_quantity} - ${r.max_quantity}`;
    const pickupText = (r.pickup_type === 'free') ? 'Tự do' : 'Người kết liễu';

    return `
      <tr style="border-bottom:1px solid var(--bd2);hover:background:var(--surf);">
        <td style="padding:7px 9px;">${catBadge}</td>
        <td style="padding:7px 9px;font-weight:700;color:var(--tx);">${escapePanelBossText(targetText)}</td>
        <td style="padding:7px 9px;">
          <div style="display:flex;align-items:center;gap:6px;">
            <img src="/icon/1.png" class="cd-rule-icon cd-item-thumb" data-item-id="${r.item_id}" loading="lazy" onerror="this.onerror=null;this.src='/icon/1.png'">
            <div>
              <div style="font-weight:700;">${escapePanelBossText(r.item_name || ('Item ' + r.item_id))}</div>
              <div style="font-size:0.72rem;color:var(--tx3);">ID: ${r.item_id}</div>
            </div>
          </div>
        </td>
        <td style="padding:7px 9px;text-align:center;font-weight:700;">${qtyText}</td>
        <td style="padding:7px 9px;text-align:center;color:var(--gold);font-weight:700;">${r.rate}%</td>
        ${currentDropTab === 'boss' ? `<td style="padding:7px 9px;font-size:0.8rem;color:var(--tx3);">${pickupText}</td>` : ''}
        <td style="padding:7px 9px;text-align:center;">
          <button onclick="toggleCustomDropRule('${r.id}')" style="border:none;background:transparent;cursor:pointer;font-size:1.1rem;" title="${r.enabled ? 'Đang bật - Bấm để tắt' : 'Đang tắt - Bấm để bật'}">
            ${r.enabled ? '🟢' : '⚪'}
          </button>
        </td>
        <td style="padding:7px 9px;text-align:center;">
          <div style="display:flex;gap:4px;justify-content:center;">
            <button onclick='editCustomDropRule(${JSON.stringify(r)})' style="padding:3px 6px;border-radius:4px;border:1px solid var(--bd2);background:var(--surf);color:var(--tx2);cursor:pointer;font-size:0.78rem;">✏️ Sửa</button>
            <button onclick="deleteCustomDropRule('${r.id}')" style="padding:3px 6px;border-radius:4px;border:1px solid rgba(217,43,58,.4);background:rgba(217,43,58,.1);color:var(--danger);cursor:pointer;font-size:0.78rem;">🗑️ Xóa</button>
          </div>
        </td>
      </tr>
    `;
  }).join('');

  // Async load rule icons
  document.querySelectorAll('.cd-rule-icon').forEach(img => {
    const id = img.getAttribute('data-item-id');
    if (id) {
      lookupSingleItemIcon(id, img);
    }
  });
}

// ── ACTIONS: BLOCK / UNBLOCK DEFAULT DROPS ──
async function blockDefaultDrop(targetType, bossKey, bossId, mobTempId, itemId, category, eventKey) {
  const formData = new FormData();
  formData.append('action', 'block_default_drop');
  formData.append('target_type', targetType);
  formData.append('boss_key', bossKey || '');
  formData.append('boss_id', bossId || 0);
  formData.append('mob_temp_id', mobTempId || 0);
  formData.append('item_id', itemId || -1);
  formData.append('category', category || 'normal');
  formData.append('event_key', eventKey || '');
  formData.append('csrf_token', document.getElementById('cd_csrf').value);

  try {
    const res = await fetch('/sv1/admin/panel.php', { method: 'POST', body: formData });
    const data = await res.json();
    if (data.status === 'success') {
      showMsg('✓ ' + data.msg);
      loadCustomDrops();
    } else {
      showMsg('❌ ' + (data.msg || 'Thao tác thất bại'));
    }
  } catch (e) {
    showMsg('❌ Lỗi kết nối khi gửi lệnh chặn rơi!');
  }
}

async function unblockDefaultDrop(targetType, bossKey, mobTempId, itemId, eventKey) {
  const formData = new FormData();
  formData.append('action', 'unblock_default_drop');
  formData.append('target_type', targetType);
  formData.append('boss_key', bossKey || '');
  formData.append('mob_temp_id', mobTempId || 0);
  formData.append('item_id', itemId || -1);
  formData.append('event_key', eventKey || '');
  formData.append('csrf_token', document.getElementById('cd_csrf').value);

  try {
    const res = await fetch('/sv1/admin/panel.php', { method: 'POST', body: formData });
    const data = await res.json();
    if (data.status === 'success') {
      showMsg('✓ ' + data.msg);
      loadCustomDrops();
    } else {
      showMsg('❌ ' + (data.msg || 'Thao tác thất bại'));
    }
  } catch (e) {
    showMsg('❌ Lỗi kết nối khi gửi lệnh khôi phục!');
  }
}

function blockAllBossDefault(bossKey, bossId) {
  if (!confirm(`Bạn có chắc muốn tắt TOÀN BỘ đồ rơi mặc định của Boss này? (Boss sẽ chỉ rơi những món cấu hình ở Panel)`)) return;
  blockDefaultDrop('boss', bossKey, bossId, 0, -1, currentCategoryTab);
}

function unblockAllBossDefault(bossKey) {
  unblockDefaultDrop('boss', bossKey, 0, -1);
}

// ── ITEM LOOKUP & SEARCH ──
function onItemIdInput(val) {
  const id = Number(val);
  if (!id || id <= 0) {
    document.getElementById('cd_item_name_display').textContent = '';
    document.getElementById('cd_item_icon_preview').src = '/icon/1.png';
    return;
  }
  lookupSingleItem(id);
}

async function lookupSingleItemIcon(id, imgEl) {
  try {
    const res = await fetch('/sv1/admin/panel.php?action=search_icons&query=' + id);
    const items = await res.json();
    const item = Array.isArray(items) ? items.find(it => Number(it.id) === Number(id)) : null;
    if (item && item.icon_id) {
      imgEl.src = `/icon/${Number(item.icon_id || 1)}.png`;
    }
  } catch (e) {}
}

async function lookupSingleItem(id) {
  try {
    const res = await fetch('/sv1/admin/panel.php?action=search_icons&query=' + id);
    const items = await res.json();
    const item = Array.isArray(items) ? items.find(it => Number(it.id) === id) : null;
    if (item) {
      document.getElementById('cd_item_name').value = item.name;
      document.getElementById('cd_item_name_display').textContent = `✓ ${item.name}`;
      document.getElementById('cd_item_icon_preview').src = `/icon/${Number(item.icon_id || 1)}.png`;
    }
  } catch (e) {}
}

function searchDropItems(query) {
  clearTimeout(itemSearchTimeout);
  const dropdown = document.getElementById('cd_item_suggestions');
  if (!query.trim()) {
    dropdown.style.display = 'none';
    return;
  }
  itemSearchTimeout = setTimeout(async () => {
    try {
      const res = await fetch('/sv1/admin/panel.php?action=search_icons&query=' + encodeURIComponent(query));
      const items = await res.json();
      if (!Array.isArray(items) || items.length === 0) {
        dropdown.innerHTML = '<div style="padding:8px 12px;color:var(--danger);font-size:0.85rem;">Không tìm thấy vật phẩm nào!</div>';
        dropdown.style.display = 'block';
        return;
      }
      dropdown.innerHTML = items.slice(0, 20).map(it => `
        <div onclick="selectSearchedDropItem(${it.id}, '${escapePanelBossText(it.name)}', ${it.icon_id || 1})" style="padding:6px 10px;display:flex;align-items:center;gap:8px;cursor:pointer;border-bottom:1px solid var(--bd2);font-size:0.85rem;" onmouseover="this.style.background='var(--surf2)'" onmouseout="this.style.background='transparent'">
          <img class="cd-item-thumb" loading="lazy" src="/icon/${Number(it.icon_id || 1)}.png" onerror="this.onerror=null;this.src='/icon/1.png'">
          <span style="font-weight:700;color:var(--tx);">${escapePanelBossText(it.name)}</span>
          <span style="color:var(--tx3);font-size:0.78rem;">(ID: ${it.id})</span>
        </div>
      `).join('');
      dropdown.style.display = 'block';
    } catch (e) {
      dropdown.style.display = 'none';
    }
  }, 250);
}

function selectSearchedDropItem(id, name, iconId) {
  document.getElementById('cd_item_id').value = id;
  document.getElementById('cd_item_name').value = name;
  document.getElementById('cd_item_name_display').textContent = `✓ ${name}`;
  document.getElementById('cd_item_icon_preview').src = `/icon/${Number(iconId || 1)}.png`;
  addDropItemToSelection(id, name, iconId);
  document.getElementById('cd_item_suggestions').style.display = 'none';
  document.getElementById('cd_item_search').value = '';
}

// ── FORM TARGET CONTROLS ──
function onDropTargetTypeChange() {
  const type = document.getElementById('cd_target_type').value;
  const wrapMob = document.getElementById('wrap_target_mob');
  const wrapBoss = document.getElementById('wrap_target_boss');
  const wrapMap = document.getElementById('wrap_target_map');

  wrapMob.style.display = (type === 'mob') ? 'block' : 'none';
  wrapBoss.style.display = (type === 'boss_id' || type === 'config_key') ? 'block' : 'none';
  wrapMap.style.display = (type === 'map') ? 'block' : 'none';
  renderDefaultDropsCatalog();
  renderCustomDropsTable();
}

function addDropItemToSelection(id, name, iconId) {
  id = Number(id || 0);
  if (id <= 0 || selectedDropItems.some(item => item.id === id)) return;
  selectedDropItems.push({ id, name: name || `Item ${id}`, iconId: Number(iconId || 1) });
  renderSelectedDropItems();
}
function addCurrentDropItem() {
  const id = Number(document.getElementById('cd_item_id').value || 0);
  if (id <= 0) return showMsg('⚠️ Vui lòng nhập Item ID hợp lệ!');
  const name = document.getElementById('cd_item_name').value || `Item ${id}`;
  const match = (document.getElementById('cd_item_icon_preview').src || '').match(/\/icon\/(\d+)\.png/);
  addDropItemToSelection(id, name, match ? Number(match[1]) : 1);
}
function removeSelectedDropItem(id) { selectedDropItems = selectedDropItems.filter(item => item.id !== Number(id)); renderSelectedDropItems(); }
function renderSelectedDropItems() {
  const box = document.getElementById('cd_selected_items');
  if (!box) return;
  box.innerHTML = selectedDropItems.map(item => `<span style="display:inline-flex;align-items:center;gap:4px;padding:3px 6px;border:1px solid var(--bd2);border-radius:6px;background:var(--surf2);font-size:.76rem;"><img src="/icon/${item.iconId}.png" style="width:20px;height:20px;object-fit:contain;" onerror="this.src='/icon/1.png'"><b>${escapePanelBossText(item.name)}</b><small>ID ${item.id}</small><button type="button" onclick="removeSelectedDropItem(${item.id})" style="border:0;background:transparent;color:var(--danger);cursor:pointer;font-weight:700;">×</button></span>`).join('');
}

function onSelectMobChange(sel) {
  const id = Number(sel.value || 0);
  const opt = sel.options[sel.selectedIndex];
  document.getElementById('cd_target_id').value = id;
  document.getElementById('cd_target_name').value = opt ? opt.text : '';
  renderDefaultDropsCatalog();
  renderCustomDropsTable();
}

function onSelectBossChange(sel) {
  const opt = sel.options[sel.selectedIndex];
  if (!opt) return;
  const id = Number(opt.value || 0);
  const cfg = opt.getAttribute('data-config-key') || '';
  const name = opt.getAttribute('data-name') || '';

  document.getElementById('cd_target_id').value = id;
  document.getElementById('cd_config_key').value = cfg;
  document.getElementById('cd_target_name').value = name;
  renderDefaultDropsCatalog();
  renderCustomDropsTable();
}

function resetDropForm() {
  selectedDropItems = [];
  renderSelectedDropItems();
  document.getElementById('cd_drop_type').value = currentDropTab;
  document.getElementById('cd_category').value = currentCategoryTab;
  const catSel = document.getElementById('cd_category_select');
  if (catSel) catSel.value = currentCategoryTab;

  document.getElementById('cd_rule_id').value = '';
  document.getElementById('cd_target_id').value = '0';
  document.getElementById('cd_config_key').value = '';
  document.getElementById('cd_target_name').value = '';
  document.getElementById('cd_item_id').value = '';
  document.getElementById('cd_item_name').value = '';
  document.getElementById('cd_item_name_display').textContent = '';
  document.getElementById('cd_item_search').value = '';
  document.getElementById('cd_item_icon_preview').src = '/icon/1.png';
  document.getElementById('cd_min_qty').value = '1';
  document.getElementById('cd_max_qty').value = '1';
  document.getElementById('cd_rate').value = '100';
  document.getElementById('cd_options').value = '';
  document.getElementById('cd_submit_btn').textContent = '💾 Lưu Quy Tắc Rơi Đồ';

  const typeSel = document.getElementById('cd_target_type');
  if (currentDropTab === 'boss') {
    typeSel.innerHTML = '<option value="boss_id">Chọn Boss để xem đồ rơi</option>';
    typeSel.value = 'boss_id';
    const bossSelect = document.getElementById('cd_target_boss');
    if (bossSelect) bossSelect.value = '0';
  } else {
    typeSel.innerHTML = '<option value="all">Tất cả quái (Toàn server)</option><option value="mob">Theo loại quái cụ thể</option><option value="map">Theo bản đồ (Map ID)</option>';
  }
  onDropTargetTypeChange();
}

function editCustomDropRule(rule) {
  selectedDropItems = [{ id: Number(rule.item_id), name: rule.item_name || `Item ${rule.item_id}`, iconId: 1 }];
  renderSelectedDropItems();
  document.getElementById('cd_drop_type').value = currentDropTab;
  document.getElementById('cd_category').value = rule.category || 'normal';
  const catSel = document.getElementById('cd_category_select');
  if (catSel) catSel.value = rule.category || 'normal';

  document.getElementById('cd_rule_id').value = rule.id || '';
  
  const typeSel = document.getElementById('cd_target_type');
  if (currentDropTab === 'boss') {
    typeSel.innerHTML = '<option value="all">Tất cả Boss</option><option value="boss_id">Chọn Boss cụ thể</option>';
  } else {
    typeSel.innerHTML = '<option value="all">Tất cả quái (Toàn server)</option><option value="mob">Theo loại quái cụ thể</option><option value="map">Theo bản đồ (Map ID)</option>';
  }
  typeSel.value = rule.target_type || 'all';
  onDropTargetTypeChange();

  document.getElementById('cd_target_id').value = rule.target_id || 0;
  document.getElementById('cd_config_key').value = rule.config_key || '';
  document.getElementById('cd_target_name').value = rule.target_name || '';

  if (rule.target_type === 'mob') {
    document.getElementById('cd_target_mob').value = rule.target_id || 0;
  } else if (rule.target_type === 'boss_id') {
    document.getElementById('cd_target_boss').value = rule.target_id || 0;
  } else if (rule.target_type === 'map') {
    document.getElementById('cd_target_map_id').value = rule.target_id || 0;
  }

  document.getElementById('cd_item_id').value = rule.item_id || '';
  document.getElementById('cd_item_name').value = rule.item_name || '';
  document.getElementById('cd_item_name_display').textContent = rule.item_name ? `✓ ${rule.item_name}` : '';
  lookupSingleItem(rule.item_id);

  document.getElementById('cd_min_qty').value = rule.min_quantity || 1;
  document.getElementById('cd_max_qty').value = rule.max_quantity || 1;
  document.getElementById('cd_rate').value = rule.rate || 100;
  if (document.getElementById('cd_pickup_type')) {
    document.getElementById('cd_pickup_type').value = rule.pickup_type || 'killer';
  }
  document.getElementById('cd_options').value = rule.options || '';

  document.getElementById('dropFormTitle').textContent = '✏️ Chỉnh Sửa Quy Tắc';
  document.getElementById('cd_submit_btn').textContent = '💾 Cập Nhật Quy Tắc';
}

async function submitCustomDropForm(event) {
  event.preventDefault();
  const type = document.getElementById('cd_target_type').value;

  if (type === 'all') {
    document.getElementById('cd_target_id').value = '0';
    document.getElementById('cd_target_name').value = currentDropTab === 'boss' ? 'Tất cả Boss' : 'Tất cả quái';
  } else if (type === 'mob') {
    const sel = document.getElementById('cd_target_mob');
    document.getElementById('cd_target_id').value = sel.value;
    document.getElementById('cd_target_name').value = sel.options[sel.selectedIndex]?.text || '';
  } else if (type === 'boss_id') {
    const sel = document.getElementById('cd_target_boss');
    const opt = sel.options[sel.selectedIndex];
    document.getElementById('cd_target_id').value = sel.value;
    document.getElementById('cd_config_key').value = opt?.getAttribute('data-config-key') || '';
    document.getElementById('cd_target_name').value = opt?.getAttribute('data-name') || '';
  } else if (type === 'map') {
    const mapVal = document.getElementById('cd_target_map_id').value;
    document.getElementById('cd_target_id').value = mapVal;
    document.getElementById('cd_target_name').value = `Map ${mapVal}`;
  }

  const directItemId = Number(document.getElementById('cd_item_id').value || 0);
  if (type === 'boss_id' && Number(document.getElementById('cd_target_boss').value || 0) === 0) {
    showMsg('⚠️ Vui lòng chọn Boss cần áp dụng!');
    return;
  }
  if (type === 'mob' && Number(document.getElementById('cd_target_mob').value || 0) === 0) {
    showMsg('⚠️ Vui lòng chọn loại quái cần áp dụng!');
    return;
  }
  if (type === 'map' && Number(document.getElementById('cd_target_map_id').value || 0) <= 0) {
    showMsg('⚠️ Vui lòng nhập Map ID hợp lệ!');
    return;
  }
  if (selectedDropItems.length === 0 && directItemId > 0) addCurrentDropItem();
  if (selectedDropItems.length === 0) {
    showMsg('⚠️ Vui lòng chọn ít nhất một vật phẩm!');
    return;
  }

  const form = document.getElementById('customDropForm');

  try {
    const editingRuleId = document.getElementById('cd_rule_id').value;
    const itemsToSave = editingRuleId ? selectedDropItems.slice(0, 1) : [...selectedDropItems];
    for (const item of itemsToSave) {
      const formData = new FormData(form);
      formData.set('item_id', item.id);
      formData.set('item_name', item.name);
      formData.append('action', 'save_custom_drop');
      formData.append('category', currentCategoryTab);
      const res = await fetch('/sv1/admin/panel.php', { method: 'POST', body: formData });
      const data = await res.json();
      if (data.status !== 'success') throw new Error(data.msg || 'Không thể lưu vật phẩm');
    }
    showMsg(`✓ Đã lưu ${itemsToSave.length} vật phẩm rơi!`);
    resetDropForm();
    await loadCustomDrops();
  } catch (e) {
    showMsg('❌ ' + (e.message || 'Không thể gửi yêu cầu lưu cấu hình.'));
  }
}

async function toggleCustomDropRule(ruleId) {
  const formData = new FormData();
  formData.append('action', 'toggle_custom_drop');
  formData.append('drop_type', currentDropTab);
  formData.append('rule_id', ruleId);
  formData.append('csrf_token', document.getElementById('cd_csrf').value);

  try {
    const res = await fetch('/sv1/admin/panel.php', { method: 'POST', body: formData });
    const data = await res.json();
    if (data.status === 'success') {
      loadCustomDrops();
    }
  } catch (e) {}
}

async function deleteCustomDropRule(ruleId) {
  if (!confirm('Bạn có chắc muốn xóa quy tắc rơi đồ này?')) return;
  const formData = new FormData();
  formData.append('action', 'delete_custom_drop');
  formData.append('drop_type', currentDropTab);
  formData.append('rule_id', ruleId);
  formData.append('csrf_token', document.getElementById('cd_csrf').value);

  try {
    const res = await fetch('/sv1/admin/panel.php', { method: 'POST', body: formData });
    const data = await res.json();
    if (data.status === 'success') {
      showMsg('✓ Đã xóa quy tắc rơi đồ.');
      loadCustomDrops();
    }
  } catch (e) {}
}

</script>
</body>
</html>








