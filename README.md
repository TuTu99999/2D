# Game Check

Project Android tối giản dùng để trình bày hai yêu cầu:

1. Màn hình vào game gồm splash screen, logo, ảnh giới thiệu, loading bar và các nút.
2. Màn hình chơi game có ảnh nền chuyển động liên tục.
3. Màn hình chơi có đối tượng A, B và đạn C:
   - A xuất hiện giữa biên trái, B xuất hiện giữa biên phải.
   - A và B có cùng kích thước, vận tốc theo cả hai trục.
   - A nảy lại khi chạm biên.
   - B chạm biên trái/trên sẽ xuất hiện ở biên đối diện tại vị trí ngẫu nhiên.
   - Chạm màn hình để bắn đạn C từ vị trí hiện tại của A.

## Cấu trúc chính

- `MainActivity.java`: khởi tạo màn hình và quản lý pause/resume.
- `GameView.java`: hiển thị Splash, Home, Game và xử lý các nút.
- `AnimatedBackground.java`: tạo hiệu ứng nền chuyển động và ánh sáng bay.
- `MovingGameObject.java`: vị trí, vận tốc và quy tắc biên của A/B.
- `Projectile.java`: hướng bay và tốc độ của đạn C.

Project không chứa boss, skill, chọn nhân vật hoặc các tính năng nâng cao của game chính.
