/**
 * Copyright (C) 2016 The Pure Nexus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package mx.mdroid.magicalworld.extra;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.IActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.SystemProperties;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.UserManager;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.Vibrator;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.PreferenceFragment;
import android.text.TextUtils;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.DisplayInfo;
import android.view.Surface;
import android.view.WindowManager;

import com.android.settings.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public final class MDroidUtils {
    private static final String TAG = "MDroidUtils";

    // Device types
    private static final int DEVICE_PHONE = 0;
    private static final int DEVICE_HYBRID = 1;
    private static final int DEVICE_TABLET = 2;

    // Device type reference
    private static int sDeviceType = -1;

    /**
     * Returns whether the device is voice-capable (meaning, it is also a phone).
     */
    public static boolean isVoiceCapable(Context context) {
        TelephonyManager telephony =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephony != null && telephony.isVoiceCapable();
    }

    public static boolean isWifiOnly(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        return (cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE) == false);
    }

    public static boolean hasMultipleUsers(Context context) {
        return ((UserManager) context.getSystemService(Context.USER_SERVICE))
                .getUsers().size() > 1;
    }

    private static int getScreenType(Context context) {
        if (sDeviceType == -1) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
           DisplayInfo outDisplayInfo = new DisplayInfo();
            wm.getDefaultDisplay().getDisplayInfo(outDisplayInfo);
            int shortSize = Math.min(outDisplayInfo.logicalHeight, outDisplayInfo.logicalWidth);
            int shortSizeDp = shortSize * DisplayMetrics.DENSITY_DEFAULT
                    / outDisplayInfo.logicalDensityDpi;
            if (shortSizeDp < 600) {
                // 0-599dp: "phone" UI with a separate status & navigation bar
                sDeviceType =  DEVICE_PHONE;
            } else if (shortSizeDp < 720) {
                // 600-719dp: "phone" UI with modifications for larger screens
                sDeviceType = DEVICE_HYBRID;
            } else {
                // 720dp: "tablet" UI with a single combined status & navigation bar
                sDeviceType = DEVICE_TABLET;
            }
        }
        return sDeviceType;
    }

    public static boolean isPhone(Context context) {
        return getScreenType(context) == DEVICE_PHONE;
    }

    public static boolean isHybrid(Context context) {
        return getScreenType(context) == DEVICE_HYBRID;
    }

    public static boolean isTablet(Context context) {
        return getScreenType(context) == DEVICE_TABLET;
    }

    /**
     * Determine whether a package is a "system package", in which case certain things (like
     * disabling notifications or disabling the package altogether) should be disallowed.
     */
    public static boolean isSystemPackage(PackageManager pm, PackageInfo pkg) {
        if (sSystemSignature == null) {
            sSystemSignature = new Signature[]{ getSystemSignature(pm) };
        }
        return sSystemSignature[0] != null && sSystemSignature[0].equals(getFirstSignature(pkg));
    }

    private static Signature[] sSystemSignature;

    private static Signature getFirstSignature(PackageInfo pkg) {
        if (pkg != null && pkg.signatures != null && pkg.signatures.length > 0) {
            return pkg.signatures[0];
        }
        return null;
    }

    private static Signature getSystemSignature(PackageManager pm) {
        try {
            final PackageInfo sys = pm.getPackageInfo("android", PackageManager.GET_SIGNATURES);
            return getFirstSignature(sys);
        } catch (NameNotFoundException e) {
        }
        return null;
    }

    public static boolean isPackageInstalled(Context context, String pkg, boolean ignoreState) {
        if (pkg != null) {
            try {
                PackageInfo pi = context.getPackageManager().getPackageInfo(pkg, 0);
                if (!pi.applicationInfo.enabled && !ignoreState) {
                    return false;
                }
            } catch (NameNotFoundException e) {
                return false;
            }
        }

        return true;
    }

    public static boolean isPackageInstalled(Context context, String pkg) {
        return isPackageInstalled(context, pkg, true);
    }

    /**
     * Locks the activity orientation to the current device orientation
     * @param activity
     */
    public static void lockCurrentOrientation(Activity activity) {
        int currentRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int orientation = activity.getResources().getConfiguration().orientation;
        int frozenRotation = 0;
        switch (currentRotation) {
            case Surface.ROTATION_0:
                frozenRotation = orientation == Configuration.ORIENTATION_LANDSCAPE
                        ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                break;
            case Surface.ROTATION_90:
                frozenRotation = orientation == Configuration.ORIENTATION_PORTRAIT
                        ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                        : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                break;
            case Surface.ROTATION_180:
                frozenRotation = orientation == Configuration.ORIENTATION_LANDSCAPE
                        ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                        : ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                break;
            case Surface.ROTATION_270:
                frozenRotation = orientation == Configuration.ORIENTATION_PORTRAIT
                        ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        : ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                break;
        }
        activity.setRequestedOrientation(frozenRotation);
    }

    public static boolean deviceSupportsFlashLight(Context context) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(
                Context.CAMERA_SERVICE);
        try {
            String[] ids = cameraManager.getCameraIdList();
            for (String id : ids) {
                CameraCharacteristics c = cameraManager.getCameraCharacteristics(id);
                Boolean flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
                if (flashAvailable != null
                        && flashAvailable
                        && lensFacing != null
                        && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    return true;
                }
            }
        } catch (CameraAccessException e) {
            // Ignore
        }
        return false;
    }

    /**
     * This can not reliably detect whether the user has root access,
     * but it can detect some cases when the user hasn't.
     */
    public static boolean hasSu() {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(new String[] { "which", "su" });
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            return br.readLine() != null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (p != null) p.destroy();
        }
        return false;
    }

    public static void requireRoot(Preference preference) {
        if (!hasSu()) {
            preference.getParent().removePreference(preference);
        }
    }

   public static String readStringFromFile(File inputFile) throws IOException {
        FileReader fileReader = new FileReader(inputFile);
        StringBuffer stringBuffer = new StringBuffer();
        int numCharsRead;
        char[] charArray = new char[1024];
        while ((numCharsRead = fileReader.read(charArray)) > 0) {
            stringBuffer.append(charArray, 0, numCharsRead);
        }
        fileReader.close();
        return stringBuffer.toString();
    }

    public static void rebootSystem(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        pm.reboot(null);
    }

    public static void showRebootDialog(Context context, String title, String message,
                                        boolean soft) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.reboot_dialog_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (soft) {
                                    new SoftRebootTask(context).execute();
                                } else {
                                    rebootSystem(context);
                                }
                            }
                })
                .setNegativeButton(R.string.reboot_dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Only close dialog
                            }
                })
                .show();
    }

    private static class SoftRebootTask extends AsyncTask<Void, Void, Void> {
        private Context mContext;
        private AlertDialog mDialog;

        public SoftRebootTask(Context context) {
            super();
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog = new AlertDialog.Builder(mContext)
                .setTitle(R.string.soft_reboot_title)
                .setMessage(R.string.soft_reboot_message)
                .create();
            mDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                final IActivityManager am =
                      ActivityManagerNative.asInterface(ServiceManager.checkService("activity"));
                if (am != null) {
                    am.restart();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "failure trying to perform soft reboot", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mDialog.dismiss();
        }
    }
}
