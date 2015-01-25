package protego.com.protego;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import RootTools.RootTools;

/**
 * Created by muktichowkwale on 19/01/15.
 */

public class App extends Activity {

    private ProgressDialog progress;

    private void copyFile (InputStream in, OutputStream out) {
        byte[] buffer = new byte[1024];
        int bytesRead;

        try {
            while ((bytesRead = in.read(buffer)) > 0)
                out.write(buffer, 0, bytesRead);
            in.close();
            out.close();

        } catch (IOException ioe) {
            Log.d("App", "Error in copying file.");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new InitTask().execute();
    }

    public void installTcpdumpBinary() {
        if(RootTools.installBinary(this, R.raw.tcpdump, "tcpdump")==false)
            Toast.makeText(getApplicationContext(),"Extraction Error", Toast.LENGTH_SHORT).show();
    }

    private class InitTask extends AsyncTask<Void, Void, Void> {

        int flag;

        @Override
        protected Void doInBackground(Void... params) {
            // Initialize global queues
            if (GlobalVariables.last100Conn == null) {
                GlobalVariables.last100Conn = new PastConnQueue();
            } else {
                // Clears the queue
                GlobalVariables.last100Conn.clear();
            }

            if (GlobalVariables.lastTwoSec == null) {
                GlobalVariables.lastTwoSec = new LastTwoSecQueue();
            } else {
                // Clears the queue
                GlobalVariables.lastTwoSec.clear();
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.this);
            if (!prefs.getBoolean("copiedKDDTrainingDataset", false)) {
                InputStream in = null;
                OutputStream out = null;
                try {
                    String appFilesDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
                    String filename = "kddreduced.arff";

                    in = getApplicationContext().getResources().openRawResource(R.raw.kddreduced);
                    File outFile = new File(appFilesDirectory, filename);
                    out = new FileOutputStream(outFile);
                    copyFile(in, out);
                    in.close();
                    in = null;
                    out.flush();
                    out.close();
                    out = null;

                    // Mark that kdd dataset has been copied
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("copiedKDDTrainingDataset", true);
                    editor.commit();
                } catch (IOException e) {
                    Log.d ("App", "File failed to copy");
                }
            }

            // Build classifier
            if (!prefs.getBoolean("classifierBuilt", false)) {
                Tranny t1 = new Tranny();
                flag = t1.build();

                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("classifierBuilt", true);
                editor.commit();
                Log.d ("APP", "classifier built");
            }

            // Install tcpdump
            installTcpdumpBinary();
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress = ProgressDialog.show(App.this, "", "Initializing...", true);
            progress.setCancelable(false);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progress.dismiss();
            if (flag == 0) {
                Toast.makeText(getApplicationContext(), "Classifier built :)", Toast.LENGTH_SHORT).show();
            }
            else if(flag == 1) {
                Toast.makeText(getApplicationContext(), "Error :( File not found", Toast.LENGTH_SHORT).show();
            }
            else if(flag == 2) {
                Toast.makeText(getApplicationContext(), "Error :( Classifier not built", Toast.LENGTH_SHORT).show();
            }
            Intent i = new Intent("protego.com.protego.MAINACTIVITY");
            startActivity(i);
            finish();
        }
    }
}
