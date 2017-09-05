package plugin.google.maps;

import android.graphics.Bitmap;
import android.util.LruCache;

public class BitmapCache extends LruCache<String, Bitmap> {

  public BitmapCache(int maxSize) {
    super(maxSize);
  }

  @Override
  protected int sizeOf(String key, Bitmap bitmap) {
    // The cache size will be measured in kilobytes rather than
    // number of items.
    return bitmap.getByteCount() / 1024;
  }

  @Override
  protected void entryRemoved(boolean evicted, String key, Bitmap oldBitmap, Bitmap newBitmap) {
    if (!oldBitmap.isRecycled()) {
      oldBitmap.recycle();
      oldBitmap = null;
    }
  }
}