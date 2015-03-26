package plugin.google.maps;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

public class PluginTileProvider implements TileProvider {
  private String tileUrlFormat = null;
  private int tileSize = 256;
  private Paint tilePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
  
  public PluginTileProvider(String tileUrlFormat, double opacity, int tileSize) {
    this.tileUrlFormat = tileUrlFormat;
    this.tileSize = tileSize;
    this.tilePaint.setAlpha((int) (opacity * 255));
  }
  
  @Override
  public Tile getTile(int x, int y, int zoom) {

    String urlStr = tileUrlFormat.replaceAll("<x>", x + "")
        .replaceAll("<y>", y + "")
        .replaceAll("<zoom>", zoom + "");
    
    try {
      InputStream inputStream = null;
      if (urlStr.startsWith("http://") || urlStr.startsWith("https://")) {
        URL url = new URL(urlStr);
        HttpURLConnection http = (HttpURLConnection)url.openConnection(); 
        http.setRequestMethod("GET");
        http.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
        http.addRequestProperty("User-Agent", "Mozilla");
        http.setInstanceFollowRedirects(true);
        HttpURLConnection.setFollowRedirects(true);
        
        boolean redirect = false;
        // normally, 3xx is redirect
        int status = http.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
          if (status == HttpURLConnection.HTTP_MOVED_TEMP
            || status == HttpURLConnection.HTTP_MOVED_PERM
              || status == HttpURLConnection.HTTP_SEE_OTHER)
          redirect = true;
        }
        if (redirect) {
          
          // get redirect url from "location" header field
          String newUrl = http.getHeaderField("Location");
       
          // get the cookie if need, for login
          String cookies = http.getHeaderField("Set-Cookie");
       
          // open the new connection again
          http = (HttpURLConnection) new URL(newUrl).openConnection();
          http.setRequestProperty("Cookie", cookies);
          http.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
          http.addRequestProperty("User-Agent", "Mozilla");
        }
        
        inputStream = http.getInputStream();
        
        Bitmap image = BitmapFactory.decodeStream(inputStream);
        Bitmap tileImage = this.resizeForTile(image);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();  
        image.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        Tile tile = new Tile(tileSize, tileSize, byteArray);
        
        outputStream.close();
        tileImage.recycle();
        image.recycle();
        inputStream.close();
        return tile;
      }
      
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
  
  public void setOpacity(double opacity) {
    this.tilePaint.setAlpha((int) (opacity * 255));
  }
  
  private Bitmap resizeForTile(Bitmap bitmap) {

    if (bitmap == null) {
      return null;
    }
    /**
     * http://stackoverflow.com/questions/4821488/bad-image-quality-after-resizing-scaling-bitmap#7468636
     */
    Bitmap scaledBitmap = Bitmap.createBitmap(tileSize, tileSize, Config.ARGB_8888);

    float ratioX = tileSize / (float) bitmap.getWidth();
    float ratioY = tileSize / (float) bitmap.getHeight();
    float middleX = tileSize / 2.0f;
    float middleY = tileSize / 2.0f;

    Matrix scaleMatrix = new Matrix();
    scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

    Canvas canvas = new Canvas(scaledBitmap);
    canvas.setMatrix(scaleMatrix);
    canvas.drawBitmap(bitmap, middleX - bitmap.getWidth() / 2, middleY - bitmap.getHeight() / 2, tilePaint);
    bitmap.recycle();
    
    return scaledBitmap;
  }

}
