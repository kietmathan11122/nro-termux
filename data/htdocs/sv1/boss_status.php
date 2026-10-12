<?php
header('Content-Type: application/json; charset=utf-8');
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/admin/game_bridge.php';

$bossStatusPath = panel_boss_status_file();
$data = ['bosses' => []];

if (file_exists($bossStatusPath)) {
    $raw = @file_get_contents($bossStatusPath);
    if ($raw !== false) {
        $parsed = json_decode($raw, true);
        if (is_array($parsed) && !empty($parsed['bosses']) && is_array($parsed['bosses'])) {
            $data = $parsed;
        }
    }
}

$catalogPath = panel_boss_catalog_file();
if (file_exists($catalogPath)) {
    $cRaw = @file_get_contents($catalogPath);
    if ($cRaw) {
        $cData = json_decode($cRaw, true);
        if (is_array($cData) && !empty($cData['bosses']) && is_array($cData['bosses'])) {
            $catalogById = [];
            foreach ($cData['bosses'] as $cb) {
                if (isset($cb['id'])) {
                    $catalogById[(int)$cb['id']] = $cb;
                }
            }
            foreach ($data['bosses'] as &$b) {
                if (empty($b['configKey']) && isset($b['id']) && isset($catalogById[(int)$b['id']])) {
                    $b['configKey'] = $catalogById[(int)$b['id']]['configKey'] ?? '';
                }
            }
            unset($b);

            $seen = [];
            foreach ($data['bosses'] as $b) {
                $k = (string)($b['configKey'] ?? ((int)($b['id'] ?? 0) . '_' . (int)($b['level'] ?? 0)));
                $seen[$k] = true;
            }
            foreach ($cData['bosses'] as $cb) {
                $k = (string)($cb['configKey'] ?? ((int)($cb['id'] ?? 0) . '_' . (int)($cb['level'] ?? 0)));
                if (!isset($seen[$k])) {
                    $seen[$k] = true;
                    $data['bosses'][] = $cb;
                }
            }
        }
    }
}

$headIds = [];
foreach ($data['bosses'] as $boss) {
    if (isset($boss['outfit0'])) {
        $headIds[] = (int)$boss['outfit0'];
    }
}
$headIds = array_values(array_unique($headIds));
$avatars = [];
if (!empty($headIds) && isset($pdo)) {
    try {
        $placeholders = implode(',', array_fill(0, count($headIds), '?'));
        $stmt = $pdo->prepare("SELECT head_id, avatar_id FROM head_avatar WHERE head_id IN ($placeholders)");
        $stmt->execute($headIds);
        while ($row = $stmt->fetch()) {
            $avatars[(int)$row['head_id']] = (int)$row['avatar_id'];
        }
    } catch (Exception $e) {
    }
}

$avatarOverrides = [
    291 => 2893,   // Broly
    294 => 2924,   // Super Broly
    1501 => 13488, // Mat Troi
];

foreach ($data['bosses'] as &$boss) {
    $head = isset($boss['outfit0']) ? (int)$boss['outfit0'] : 0;
    $name = (string)($boss['name'] ?? '');

    if (strcasecmp(trim($name), 'Boss Test Dame') === 0 && (int)($boss['avatar_id'] ?? 0) === 18144) {
        $boss['avatar_id'] = 18144;
    } elseif (stripos($name, 'super broly') !== false) {
        $boss['avatar_id'] = 2924;
    } elseif (stripos($name, 'broly') !== false) {
        $boss['avatar_id'] = 2893;
    } elseif (isset($avatarOverrides[$head])) {
        $boss['avatar_id'] = $avatarOverrides[$head];
    } elseif (isset($avatars[$head])) {
        $boss['avatar_id'] = $avatars[$head];
    } else {
        $boss['avatar_id'] = 1;
    }
}
unset($boss);

echo json_encode($data, JSON_UNESCAPED_UNICODE);
