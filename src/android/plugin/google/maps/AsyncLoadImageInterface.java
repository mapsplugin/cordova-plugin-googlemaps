package plugin.google.maps;

public interface AsyncLoadImageInterface {
  public void setParams(Object target, Object option, PluginAsyncInterface callback);
  public void onPostExecute(AsyncLoadImage.AsyncLoadImageResult result) ;
}
