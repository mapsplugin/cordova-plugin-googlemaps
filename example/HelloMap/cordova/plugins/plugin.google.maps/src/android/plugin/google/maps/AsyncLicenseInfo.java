package plugin.google.maps;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;

import com.google.android.gms.common.GooglePlayServicesUtil;

public class AsyncLicenseInfo extends AsyncTask<Void, Void, String> {
  private Activity mActivity;
  private ProgressDialog mProgress;
  
  public AsyncLicenseInfo(Activity activity) {
    mActivity = activity;
    mProgress = ProgressDialog.show(activity, "", "Please wait...", true);
  }
  @Override
  protected String doInBackground(Void... arg0) {
    return GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(mActivity);
  }
  protected void onPostExecute(String licenseInfo) {
    mProgress.dismiss();

    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
    
    alertDialogBuilder
      .setMessage(licenseInfo)
      .setCancelable(false)
      .setPositiveButton("Close", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog,int id) {
          dialog.dismiss();
        }
      });

    // create alert dialog
    AlertDialog alertDialog = alertDialogBuilder.create();

    // show it
    alertDialog.show();
  }

}
