package plugin.google.maps;


public interface PluginAsyncInterface {
  public void onPostExecute(Object object);
  public void onError(String errorMsg);
}
