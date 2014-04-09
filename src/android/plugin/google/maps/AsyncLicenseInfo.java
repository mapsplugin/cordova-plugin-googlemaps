package plugin.google.maps;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.AsyncTask;

import com.google.android.gms.common.GooglePlayServicesUtil;

public class AsyncLicenseInfo extends AsyncTask<Void, Void, AlertDialog.Builder> {
  private Activity mActivity;
  private ProgressDialog mProgress;
  
  public AsyncLicenseInfo(Activity activity) {
    mActivity = activity;
    mProgress = ProgressDialog.show(activity, "", "Please wait...", true);
  }
  @Override
  protected AlertDialog.Builder doInBackground(Void... arg0) {
    String licenseInfo = GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(mActivity);
    
    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
    
    alertDialogBuilder
      .setMessage(licenseInfo)
      .setCancelable(false)
      .setPositiveButton("Close", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog,int id) {
          dialog.dismiss();
        }
      });
    
    return alertDialogBuilder;
  }
  protected void onPostExecute(AlertDialog.Builder alertDialogBuilder) {
    // create alert dialog
    AlertDialog alertDialog = alertDialogBuilder.create();
    
    alertDialog.setOnShowListener(new OnShowListener() {

      @Override
      public void onShow(DialogInterface arg0) {
        mProgress.dismiss();
      }
      
    });

    // show it
    alertDialog.show();
  }

}
