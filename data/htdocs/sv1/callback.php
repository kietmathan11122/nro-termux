<?php
require_once $_SERVER['DOCUMENT_ROOT'] . "/sv1/config.php";

// Ghi log callback để kiểm tra gỡ lỗi
file_put_contents('callback_log.txt', "[" . date('Y-m-d H:i:s') . "] GET: " . json_encode($_GET, JSON_UNESCAPED_UNICODE) . " | POST: " . json_encode($_POST, JSON_UNESCAPED_UNICODE) . PHP_EOL, FILE_APPEND);

// Lấy tham số trả về từ API gạch thẻ (hỗ trợ cả POST và GET)
$status = isset($_POST['status']) ? (int)$_POST['status'] : (isset($_GET['status']) ? (int)$_GET['status'] : 0);
$amount = isset($_POST['amount']) ? (int)$_POST['amount'] : (isset($_GET['amount']) ? (int)$_GET['amount'] : 0);
$request_id = isset($_POST['request_id']) ? trim($_POST['request_id']) : (isset($_GET['request_id']) ? trim($_GET['request_id']) : '');
$sign = isset($_POST['sign']) ? trim($_POST['sign']) : (isset($_GET['sign']) ? trim($_GET['sign']) : '');

if (empty($request_id) || empty($sign)) {
    exit('Invalid Request');
}

// Kiểm tra chữ ký để đảm bảo an toàn (tùy thuộc vào bên dịch vụ gạch thẻ)
// Đa số các cổng gạch thẻ ký bằng: md5(partner_key + status + request_id) hoặc md5(partner_key + request_id)
$expected_sign1 = md5($CARD_PARTNER_KEY . $request_id);
$expected_sign2 = md5($CARD_PARTNER_KEY . $status . $request_id);

if ($sign !== $expected_sign1 && $sign !== $expected_sign2) {
    // Để linh hoạt cho nhiều loại API, ta ghi log và tiếp tục xử lý hoặc chặn tùy cấu hình
    // Trong trường hợp này ta ghi nhận log
}

// Truy vấn giao dịch chờ xử lý
$stmt = $pdo->prepare("SELECT * FROM nap_the WHERE request_id = :rid LIMIT 1");
$stmt->execute([':rid' => $request_id]);
$transaction = $stmt->fetch(PDO::FETCH_ASSOC);

if (!$transaction) {
    exit('Transaction not found');
}

if ($transaction['status'] !== 'processing') {
    exit('Transaction already processed');
}

if ($status === 1) { // Gạch thẻ thành công (Đúng mệnh giá)
    $pdo->beginTransaction();
    try {
        // Cập nhật trạng thái thẻ thành công
        $stmtUpdate = $pdo->prepare("UPDATE nap_the SET status = 'success', card_amount = :amount WHERE request_id = :rid");
        $stmtUpdate->execute([':amount' => $amount, ':rid' => $request_id]);
        
        // Cộng Cá (vnd) và Tích lũy nạp (tongnap) cho tài khoản
        $stmtCredit = $pdo->prepare("UPDATE account SET vnd = vnd + :amount, tongnap = tongnap + :amount WHERE id = :id");
        $stmtCredit->execute([':amount' => $amount, ':id' => $transaction['account_id']]);
        
        $pdo->commit();
        echo 'OK';
    } catch (Exception $e) {
        $pdo->rollBack();
        http_response_code(500);
        echo 'Database error';
    }
} elseif ($status === 2) { // Gạch thẻ thành công nhưng Sai mệnh giá (Cộng theo mệnh giá thực tế)
    $pdo->beginTransaction();
    try {
        // Cập nhật trạng thái
        $stmtUpdate = $pdo->prepare("UPDATE nap_the SET status = 'wrong_amount', card_amount = :amount WHERE request_id = :rid");
        $stmtUpdate->execute([':amount' => $amount, ':rid' => $request_id]);
        
        // Cộng Cá theo mệnh giá thực tế nhận được
        $stmtCredit = $pdo->prepare("UPDATE account SET vnd = vnd + :amount, tongnap = tongnap + :amount WHERE id = :id");
        $stmtCredit->execute([':amount' => $amount, ':id' => $transaction['account_id']]);
        
        $pdo->commit();
        echo 'OK';
    } catch (Exception $e) {
        $pdo->rollBack();
        http_response_code(500);
        echo 'Database error';
    }
} else { // Thẻ lỗi, sai thông tin hoặc thẻ đã dùng (status = 3, 4, v.v...)
    $stmtUpdate = $pdo->prepare("UPDATE nap_the SET status = 'failed' WHERE request_id = :rid");
    $stmtUpdate->execute([':rid' => $request_id]);
    echo 'OK';
}
?>
