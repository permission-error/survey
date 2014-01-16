/*
 * Copyright (C) 2012-2013 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.survey.android.activities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.survey.android.R;
import org.opendatakit.survey.android.logic.PropertiesSingleton;
import org.opendatakit.survey.android.preferences.PreferencesActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * Code to display a splash screen
 *
 * @author Carl Hartung
 *
 */
public class SplashScreenActivity extends Activity {
  
  public static final String KEY_LAST_VERSION = "lastVersion";
  public static final String KEY_FIRST_RUN = "firstRun";

  private int mImageMaxWidth;
  private int mSplashTimeout = 2000; // milliseconds

  private AlertDialog mAlertDialog;
  private static final boolean EXIT = true;

  @SuppressWarnings("deprecation")
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // verify that the external SD Card is available.
    try {
      ODKFileUtils.verifyExternalStorageAvailability();
    } catch (RuntimeException e) {
      createErrorDialog(e.getMessage(), EXIT);
      return;
    }

    mImageMaxWidth = getWindowManager().getDefaultDisplay().getWidth();

    // this splash screen should be a blank slate
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.splash_screen);

    // get the shared preferences object
    SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    Editor editor = mSharedPreferences.edit();
    PropertiesSingleton propSingleton = PropertiesSingleton.INSTANCE;

    // get the package info object with version number
    PackageInfo packageInfo = null;
    try {
      packageInfo = getPackageManager().getPackageInfo(getPackageName(),
          PackageManager.GET_META_DATA);
    } catch (NameNotFoundException e) {
      e.printStackTrace();
    }

    // TODO: Revisit if this splash screen implementation is correct - leaving it in SharedPreferences for now
    boolean firstRun = mSharedPreferences.getBoolean(KEY_FIRST_RUN, true);
    // boolean firstRun = (propSingleton.getProperty(PreferencesActivity.KEY_FIRST_RUN)).equals("true") ? true : false;
    
    boolean showSplash = (propSingleton.getProperty(PreferencesActivity.KEY_SHOW_SPLASH)).equals("true") ? true : false;
    
    // String splashPath = mSharedPreferences.getString(PreferencesActivity.KEY_SPLASH_PATH,
    //    getString(R.string.default_splash_path));
    String splashPath = propSingleton.getProperty(PreferencesActivity.KEY_SPLASH_PATH);

    // if you've increased version code, then update the version number and
    // set firstRun to true
    if (mSharedPreferences.getLong(KEY_LAST_VERSION, 0) < packageInfo.versionCode) {
      editor.putLong(KEY_LAST_VERSION, packageInfo.versionCode);
      editor.commit();

      firstRun = true;
    }
    
    // String sKeyLastVer = propSingleton.getPropertyOrDefault(PreferencesActivity.KEY_LAST_VERSION, "0");
    // String sKeyLastVer = propSingleton.getProperty(PreferencesActivity.KEY_LAST_VERSION);
    /*
    long keyLastVer =  Long.valueOf(sKeyLastVer);
    if (keyLastVer < packageInfo.versionCode) {
      propSingleton.setProperty(PreferencesActivity.KEY_LAST_VERSION, Integer.toString(packageInfo.versionCode));
      propSingleton.writeProperties();

      firstRun = true;
    }*/

    // do all the first run things
    if (firstRun || showSplash) {
      editor.putBoolean(KEY_FIRST_RUN, false);
    	// propSingleton.setProperty(PreferencesActivity.KEY_FIRST_RUN, "false");
      editor.commit();
    	// propSingleton.writeProperties();
      startSplashScreen(splashPath);
    } else {
      endSplashScreen();
    }

  }

  private void endSplashScreen() {

    // launch new activity and close splash screen
    startActivity(new Intent(SplashScreenActivity.this, MainMenuActivity.class));
    finish();
  }

  // decodes image and scales it to reduce memory consumption
  private Bitmap decodeFile(File f) {
    Bitmap b = null;
    try {
      // Decode image size
      BitmapFactory.Options o = new BitmapFactory.Options();
      o.inJustDecodeBounds = true;

      FileInputStream fis = new FileInputStream(f);
      BitmapFactory.decodeStream(fis, null, o);
      try {
        fis.close();
      } catch (IOException e) {
        e.printStackTrace();
      }

      int scale = 1;
      if (o.outHeight > mImageMaxWidth || o.outWidth > mImageMaxWidth) {
        scale = (int) Math.pow(
            2,
            (int) Math.round(Math.log(mImageMaxWidth / (double) Math.max(o.outHeight, o.outWidth))
                / Math.log(0.5)));
      }

      // Decode with inSampleSize
      BitmapFactory.Options o2 = new BitmapFactory.Options();
      o2.inSampleSize = scale;
      fis = new FileInputStream(f);
      b = BitmapFactory.decodeStream(fis, null, o2);
      try {
        fis.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    } catch (FileNotFoundException e) {
    }
    return b;
  }

  private void startSplashScreen(String path) {

    // add items to the splash screen here. makes things less distracting.
    ImageView iv = (ImageView) findViewById(R.id.splash);
    LinearLayout ll = (LinearLayout) findViewById(R.id.splash_default);

    File f = new File(path);
    if (f.exists()) {
      iv.setImageBitmap(decodeFile(f));
      ll.setVisibility(View.GONE);
      iv.setVisibility(View.VISIBLE);
    }

    // create a thread that counts up to the timeout
    Thread t = new Thread() {
      int count = 0;

      @Override
      public void run() {
        try {
          super.run();
          while (count < mSplashTimeout) {
            sleep(100);
            count += 100;
          }
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          endSplashScreen();
        }
      }
    };
    t.start();
  }

  private void createErrorDialog(String errorMsg, final boolean shouldExit) {
    mAlertDialog = new AlertDialog.Builder(this).create();
    mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
    mAlertDialog.setMessage(errorMsg);
    DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int i) {
        switch (i) {
        case DialogInterface.BUTTON_POSITIVE:
          if (shouldExit) {
            finish();
          }
          break;
        }
      }
    };
    mAlertDialog.setCancelable(false);
    mAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok), errorListener);
    mAlertDialog.show();
  }

}
