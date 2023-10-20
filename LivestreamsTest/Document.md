# Sigma Live Streams SDK

### 1. Live Camera

```
LiveManager.getInstance().setOrientation(int rotation);
```

rotation: góc xoay màn hình 

- 0: Điện thoại để vị trí mặc định (portrait)
- 90: Xoay ngang điện thoại sang bên trái (landscape)
- 270: Xoay ngang điện thoại sang bên phải(landscape)
- 180: Tương tự như góc xoay = 0

Cài đặt livestream

```
LiveManager.getInstance().setup(Context context, ViewGroup viewGroup, LiveListener listener)
```

Lắng nghe sự kiện:

```
 LiveListener() {
    @Override
    public void onLiveStarting() {
        Log.d("LiveListener=>", "onLiveStarting");
    }

    @Override
    public void onLiveStarted() {
        Log.d("LiveListener=>", "onLiveStarted");

    }

    @Override
    public void onLiveError(Exception ex) {
        Log.d("LiveListener=>", "onLiveError");

    }

    @Override
    public void onLiveStopped() {
        Log.d("LiveListener=>", "onLiveStopped");
    
    }

    @Override
    public void onConnectionStarted() {
        Log.d("LiveListener=>", "onConnectionStarted");
    }

    @Override
    public void onNewBitrateReceived(long b) {
    
    }
});
```

Bắt đầu start live

```
LiveManager.getInstance().start(String urlEndPoint);
```

Dừng luồng live

```
LiveManager.getInstance().stop();
```

***Lưu ý: Quá trình cài đặt livestream `LiveManager.getInstance().setup(Context context, ViewGroup viewGroup, LiveListener listener)` có thể mất một khoảng thời gian nhất định. Chỉ nên gọi `LiveManager.getInstance().start(String urlEndPoint)` sau khi quá trình cài đặt liveStream thành công.***

```java
LiveManager.getInstance().setWaitingImage(Bitmap bitmap);
```

Thêm ảnh thumbnail khi luông live tạm dừng



### 2. Live màn hình

Cài đặt livestream

```
LiveManager.getInstance().setupScreenStream(Context context, ViewGroup viewGroup, LiveListener listener)
```

Lắng nghe sự kiện:

```java
LiveListener() {
    @Override
    public void onLiveStarting() {
        Log.d("LiveListener=>", "onLiveStarting");
    }

    @Override
    public void onLiveStarted() {
        Log.d("LiveListener=>", "onLiveStarted");
        imgPlay.setImageResource(R.drawable.ic_pause);
    }

    @Override
    public void onLiveError(Exception ex) {
        Log.d("LiveListener=>", "onLiveError");
        imgPlay.setImageResource(R.drawable.ic_play_arrow);
    }

    @Override
    public void onLiveStopped() {
        Log.d("LiveListener=>", "onLiveStopped");
        imgPlay.setImageResource(R.drawable.ic_play_arrow);
    }

    @Override
    public void onConnectionStarted() {
        Log.d("LiveListener=>", "onConnectionStarted");
    }

    @Override
    public void onNewBitrateReceived(long b) {
    }
});
```

Thêm ảnh thumbnail khi luông live tạm dừng

```java
LiveManager.getInstance().setWaitingImage(Bitmap bitmap);
```

Request live screen (Hành động này sẽ mở một popup yêu cầu ghi màn hình)

```java
LiveManager.getInstance().start(String urlEndpoint);
```

Lắng nghe sự kiện của user trong `onActivityResult`

```java
protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    LiveManager.getInstance().onActivityResult(requestCode, resultCode, data);
}
```

Truyền data từ `onActivityResult` vào  `LiveManager.getInstance().onActivityResult(requestCode, resultCode, data)`

để bắt đầu phát live.