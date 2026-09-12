# Game Check

Project Android tối giản dùng để trình bày các yêu cầu chuyển động, âm thanh,
tấn công, phòng thủ, va chạm và HUD mà không chứa tính năng nâng cao của game chính.

## Điều khiển

- Joystick bên trái: điều khiển hướng và tốc độ di chuyển của A.
- Chạm vùng chơi: bắn loại vũ khí đang chọn theo hướng chạm.
- `BULLET`: đạn thẳng, tốc độ nhanh, 12 sát thương.
- `MISSILE`: tên lửa tự bám B, tốc độ vừa, 25 sát thương.
- `BOMB`: bom bay chậm, nổ trễ và gây sát thương vùng, 38 sát thương.
- `SHIELD`: chặn một lần va chạm, tồn tại 4 giây.
- `FREEZE`: vô hiệu hóa chuyển động của B trong 3 giây.

## Âm thanh

- Mỗi loại tấn công phát một hiệu ứng âm thanh ngắn khi được tạo từ A.
- Khi B bắt đầu đi vào màn hình chơi game, cảnh báo phát đúng 4 lần.
- Cảnh báo cũng phát lại khi B xuất hiện từ biên đối diện.
- `SOUND OFF` tắt hiệu ứng ngắn và được thay bằng `SOUND ON`.
- `SOUND ON` bật lại hiệu ứng ngắn và được thay bằng `SOUND OFF`.
- `MUSIC ON` phát nhạc nền lặp và được thay bằng `MUSIC OFF`.
- `MUSIC OFF` dừng nhạc nền và được thay bằng `MUSIC ON`.
- Hai trạng thái của từng nút dùng chung tọa độ và kích thước.

## Va chạm X, Y và Z

- X: hiệu ứng nổ, giảm 15 máu và 10 giáp; nếu có khiên thì làm mất khiên
  và giảm 5 giáp.
- Y: tăng tốc độ lên `x1.5` trong 5 giây và hồi 20 giáp.
- Z: tăng 10 vàng, 5 coin và 100 điểm.
- X, Y và Z biến mất khi được nhặt, sau đó xuất hiện lại ở vị trí ngẫu nhiên.

Như vậy ba vật thể tạo ra ít nhất 8 hiệu ứng: nổ, giảm máu, giảm giáp,
mất khiên, tăng tốc, tăng vàng, tăng coin và tăng điểm.

## HUD

HUD hiển thị máu, giáp, vàng, coin, điểm, tốc độ, HP của B, vị trí xuất phát
và trạng thái/cooldown của Shield và Freeze.

## Cấu trúc chính

- `GameView.java`: màn hình, điều khiển, HUD và luật gameplay.
- `MovingGameObject.java`: vị trí, vận tốc và quy tắc biên của A/B.
- `Projectile.java`: ba cơ chế tấn công có hướng và tốc độ riêng.
- `WorldItem.java`: va chạm và xuất hiện lại của X/Y/Z.
- `GameAudio.java`: SoundPool, cảnh báo lặp, nhạc nền và bật/tắt âm thanh.
- `AnimatedBackground.java`: hiệu ứng nền chuyển động và ánh sáng bay.

Project không chứa boss, chọn nhân vật hoặc hệ thống map của game chính.
