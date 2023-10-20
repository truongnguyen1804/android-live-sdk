Camera

1. Luồng chính:

   Bước 1: Khởi tạo

   ```java
   LiveManager.getInstance().setup(Activity activity, ViewGroup container, int[] paddings, LiveListener listener, PreviewSizeListener previewSizeListener, OpenGlView.SurfaceListener surfaceListener)
   ```

   ```java
   // set camera front or back. Hàm này có thể gọi trước hoặc sau hàm khởi tạo.
   LiveManager.getInstance().setCameraFace(CameraFace.Front);
   ```

   ```java
   public interface LiveListener {
   
   // gọi khi đang kết nối socket
   void onLiveStarting();
   
   // gọi khi kết nối thành công
   void onLiveStarted();
   
   // gọi khi luồng live gặp sự cố
   void onLiveError(Exception ex);
   
   // gọi khi dừng luồng live
   void onLiveStopped();
   
   // gọi khi luồng live bị mất kết nối
   void onDisConnect();
   
   // gọi khi kết nối luồng live thất bại
   void onConnectFailed(Exception err);
   
   void onConnectionStarted();
   void onNewBitrateReceived(long b);
   
   // gọi khi chưa được cấp quyền
   void onPermissionDenied();
   }
   ```

   ```java
   public interface SurfaceListener {
     // Surface khởi tạo thành công
       void onCreated();
     // Surface bị huỷ
       void onDestroyed();
     // Surface thay đổi
       void onChanged();
     // Cố gắng tạo surface mới khi socket đang connect
       void onError(Exception exception);
     // Cố gắng start livestream khi surface chưa khởi tạo thành công
       void onSurfaceInvalid(Exception exception);
   }
   ```



```java
public interface PreviewSizeListener {
  // trả về kích thước của camera => render view (tránh tình trạng live bị méo)
    void onPreviewSize(int width, int height);
}
```



Bước 2:

Sau khi **SurfaceListener** call **onCreated** =>> đủ điều kiện gọi hàm start

```java
// gọi để connect to rtmp và bắt đầu live khi connect success
LiveManager.getInstance().start("rtmp://live.twitch.tv/app/...");
```



Bước 3: Kết thúc phiên live

```java
// gọi khi rời khỏi màn hình live
LiveManager.getInstance().stopPreview();
// gọi khi thoát luồng
LiveManager.getInstance().stop();
```



Note: 

Khi luông live camera ở background

```java
LiveManager.getInstance().stopPreview();
LiveManager.getInstance().stop();
```

Xử lý resume
Khi luồng live bị disconnect và cần kết nối lại:

```java
// option: gọi lại camera front
LiveManager.getInstance().setCameraFace(CameraFace.Front);

// Khởi tạo lại
LiveManager.getInstance().setup(Activity activity, ViewGroup container, int[] paddings, LiveListener listener, PreviewSizeListener previewSizeListener, OpenGlView.SurfaceListener surfaceListener);
  
// start live 
 LiveManager.getInstance().start("rtmp://live.twitch.tv/app/...");
```



Screen:

Bước 1: Khởi tạo

```java
LiveManager.getInstance().setupScreenStream(Activity activity, ViewGroup container, LiveListener listener)
```

```java
public interface LiveListener {

// gọi khi đang kết nối socket
void onLiveStarting();

// gọi khi kết nối thành công
void onLiveStarted();

// gọi khi luồng live gặp sự cố
void onLiveError(Exception ex);

// gọi khi dừng luồng live
void onLiveStopped();

// gọi khi luồng live bị mất kết nối
void onDisConnect();

// gọi khi kết nối luồng live thất bại
void onConnectFailed(Exception err);

void onConnectionStarted();
void onNewBitrateReceived(long b);

// gọi khi chưa được cấp quyền
void onPermissionDenied();
}
```

Bước 2: Request quyền ghi màn hình

```java
LiveManager.getInstance().start("rtmp://live.twitch.tv/app/...");
```

Bước 3: Lắng nghe kết quả trả về khi xin quyền tại onActivityResult

```java
LiveManager.getInstance().onActivityResult(requestCode, resultCode, data);
```

Bước 4: Kết thúc live

```java
LiveManager.getInstance().stop();
```



Note: Xử lý luồng resume

Khi gặp sự cố kết nối với socket =>> hàm **onDisconnect()** được gọi

```java
// gọi nếu muốn kết nối lại
LiveManager.getInstance().reconnect();
// gọi nêú muốn huỷ phiên live (thu hồi quyền đọc màn hình)
LiveManager.getInstance().stop();
```

Để cho ứng dụng thuận tiện trong việc xử lý cơ chế reconnect, nên hai sự kiện **onConnectionFailedRtmp()** và **onDisconnectRtmp()** cùng trả về sự kiện **onDisconnect()** trong **LiveListenner**. Vì vậy **onDisconnect()** có thể được gọi 2 lần trong cùng 1 lần connect.

```java
// gọi khi kết nối Rtmp thất bại.
onConnectionFailedRtmp()

// gọi khi mất kết nối Rtmp.
onDisconnectRtmp() 
```

Để giải quyết vấn đề này, ứng dụng nên tích hợp 

