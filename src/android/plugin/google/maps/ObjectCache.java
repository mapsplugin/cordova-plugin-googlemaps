package plugin.google.maps;

import android.util.LruCache;

import java.util.HashSet;

public class ObjectCache {
  public final HashSet<String> keys = new HashSet<String>();
  private LruCache<String, Object> objects = new LruCache<String, Object>(1024 * 1024 * 10);

  public boolean containsKey(String key) {
    return keys.contains(key);
  }

  public void put(String key, Object object) {
    keys.add(key);
    objects.put(key, object);
  }

  public Object remove(String key) {
    keys.remove(key);
    return objects.remove(key);
  }

  public Object get(String key) {
    return objects.get(key);
  }

  public int size() {
    return objects.size();
  }


  public void clear() {
    for (String key : keys) {
      objects.remove(key);
    }
    keys.clear();
  }

  public void destroy() {
    objects.evictAll();
  }


}
