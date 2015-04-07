package plugin.google.maps;

import android.app.Dialog;
import android.content.Context;
import android.webkit.WebChromeClient;

public class MyDialog extends Dialog {

  private WebChromeClient.CustomViewCallback callback;
  
  public MyDialog(Context context, WebChromeClient.CustomViewCallback customViewCallback) {
    super(context, android.R.style.Theme_Black_NoTitleBar);
    this.callback = customViewCallback;
  }
  

  public void onBackPressed () {
    this.dismiss();
    callback.onCustomViewHidden();
  }
  
}
