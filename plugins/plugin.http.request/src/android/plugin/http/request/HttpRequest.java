package plugin.http.request;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

public class HttpRequest extends CordovaPlugin {
  private Context context = null;
  private RequestQueue queue = null;
  
  private enum REQUEST_METHODS {
      get, post, delete, put
  };

  public void initialize(CordovaInterface cordova, final CordovaWebView webView) {
    this.context = cordova.getActivity();
    this.queue = Volley.newRequestQueue(this.context);
  }

  @Override
  public boolean execute(String action, JSONArray args,
      final CallbackContext callback){

    if (!action.contentEquals("execute")) {
      return false;
    }
    int method = 0;
    
    
    try {
      String methodStr = args.getString(0);
      String url = args.getString(1);
      JSONObject params = null;
      Map<String, String> options = new HashMap<String, String>();
      if (args.length() > 2 && !args.get(2).equals(null)) {
        params = args.getJSONObject(2);
        @SuppressWarnings("unchecked")
        Iterator<Object> iterator = params.keys();
        String key;
        while(iterator.hasNext()){
          key = String.valueOf(iterator.next());
          options.put(key, params.getString(key));
        }
      }
      
      try {
        switch(REQUEST_METHODS.valueOf(methodStr)) {
        case get:
          method = Method.GET;
          break;
        case post:
          method = Method.POST;
          break;
        case put:
          method = Method.PUT;
          break;
        case delete:
          method = Method.DELETE;
          break;
        default:
          method = Method.DEPRECATED_GET_OR_POST;
        }
      } catch (Exception e) {}
      
      Listener<String> onSuccess = new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
          callback.success(response);
        }
      };
      ErrorListener onError = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
          callback.error(error.getMessage());
        }
      };
      
      StringRequest request = new StringRequest(this.context, method, url, options, onSuccess, onError);
      queue.add(request);
      
      return true;
    } catch (JSONException e) {
      e.printStackTrace();
      callback.error(e.getMessage());
      return false;
    }
  }
}