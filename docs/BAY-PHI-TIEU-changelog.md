# Bẫy phi tiêu — Cập nhật nhắm mục tiêu

## File thay đổi
- `WornDartTrap.java`
- `PoisonDartTrap.java` (`TenguDartTrap` kế thừa `PoisonDartTrap`, không có `activate()` riêng nên tự động ăn theo, không cần sửa)

## Vấn đề cũ
- Nếu có nhân vật đứng đúng ô bẫy → luôn bắn vào nhân vật đó, bất kể có mục tiêu gần hơn ở ô khác hay không.
- Chỉ khi **không ai đứng trên bẫy** (ví dụ bẫy bị kích hoạt do vật ném/rơi vào ô, như Gai ném) mới quét tìm mục tiêu gần nhất — nhưng phạm vi quét là cả tầm nhìn (6+ ô, tuỳ view distance), và khi hai mục tiêu hoà khoảng cách thì luôn ưu tiên chọn Hero.

## Thay đổi
- **Luôn** quét toàn bộ nhân vật (hero + quái) trong bán kính **4 ô** quanh bẫy, kể cả khi đang có người đứng trên ô bẫy — so khoảng cách trực tiếp giữa người đang đứng trên bẫy (nếu có) với những nhân vật khác trong phạm vi, ai gần nhất thắng.
- Bỏ luật ưu tiên Hero khi hoà khoảng cách — giờ hoàn toàn theo khoảng cách thật.
- Thu hẹp phạm vi quét từ tầm nhìn (6+ ô) xuống còn 4 ô.
- Giữ nguyên: check đường bắn thẳng (Ballistica — phi tiêu không xuyên tường được), bỏ qua mục tiêu đang tàng hình, và `canTarget()` riêng của `PoisonDartTrap`/`TenguDartTrap` (Tengu không tự bắn trúng chính mình).
- **Không đổi điều kiện kích hoạt bẫy** — trap vẫn chỉ activate khi có nhân vật bước lên ô hoặc có vật phẩm bị ném/rơi vào đúng ô đó. Thay đổi trên chỉ quyết định *ai bị bắn trúng sau khi* bẫy đã kích hoạt, không làm bẫy tự nổ vì có người đứng gần.

## Ví dụ
Người chơi cách bẫy 4 ô, có quái cách 2 ô → phi tiêu bay vào quái (trước đây, nếu quái không đứng thẳng trên bẫy, có thể bay vào người chơi tuỳ tình huống line-of-sight/khoảng cách).
