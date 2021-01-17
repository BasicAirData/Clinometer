/*
 * ClinometerApplication - Singleton Java Class for Android
 * Created by G.Capelli (BasicAirData) on 10/1/2021
 *
 * This file is part of BasicAirData Clinometer for Android.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package eu.basicairdata.clinometer;

import android.Manifest;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;


public class ClinometerApplication extends Application {

    public static final int CAMERA_REQUEST_CODE = 100;


    public static final String KEY_PREF_CALIBRATION                   = "prefCalibration";
    public static final String KEY_PREF_CALIBRATION_RESET             = "prefResetCalibration";
    public static final String KEY_PREF_CALIBRATION_TIME              = "prefCalibrationTime";
    public static final String KEY_PREF_AUTOLOCK                      = "prefAutoLock";
    public static final String KEY_PREF_AUTOLOCK_HORIZON_CHECK        = "prefAutoLockHorizonCheck";
    public static final String KEY_PREF_AUTOLOCK_PRECISION            = "prefAutoLockPrecision";
    public static final String KEY_PREF_CAMERA_PERMISSION             = "prefCameraPermission";
    public static final String KEY_PREF_CAMERA                        = "prefCamera";
    public static final String KEY_PREF_CAMERA_EXPOSURE_COMPENSATION  = "prefExposureCompensation";
    public static final String KEY_PREF_ABOUT                         = "prefAbout";
    public static final String KEY_PREF_KEEP_SCREEN_ON                = "prefKeepScreenOn";
    public static final String KEY_PREF_CALIBRATION_ANGLE_0           = "prefCalibrationAngle0";
    public static final String KEY_PREF_CALIBRATION_ANGLE_1           = "prefCalibrationAngle1";
    public static final String KEY_PREF_CALIBRATION_ANGLE_2           = "prefCalibrationAngle2";
    public static final String KEY_PREF_CALIBRATION_GAIN_0            = "prefCalibrationGain0";
    public static final String KEY_PREF_CALIBRATION_GAIN_1            = "prefCalibrationGain1";
    public static final String KEY_PREF_CALIBRATION_GAIN_2            = "prefCalibrationGain2";
    public static final String KEY_PREF_CALIBRATION_OFFSET_0          = "prefCalibrationOffset0";
    public static final String KEY_PREF_CALIBRATION_OFFSET_1          = "prefCalibrationOffset1";
    public static final String KEY_PREF_CALIBRATION_OFFSET_2          = "prefCalibrationOffset2";


    // Singleton instance
    private static ClinometerApplication singleton;
    public static ClinometerApplication getInstance(){
        return singleton;
    }

    private SharedPreferences preferences;

    private boolean hasACamera = false;                                                 // True if the device has at least a camera
    private final ArrayList<CameraInformation> listOfCameraInformation = new ArrayList<>();   // The list of Cameras of the device
    private CameraInformation selectedCameraInformation;                                // The Selected Camera


    // ----------------------------------------------------------------------------------------------------------------------
    // GETTERS AND SETTERS --------------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------------------------------


    public boolean hasCamera() {
        return hasACamera;
    }

    public ArrayList<CameraInformation> getListOfCameraInformation() {
        return listOfCameraInformation;
    }

    public CameraInformation getSelectedCameraInformation() {
        return selectedCameraInformation;
    }

    public void setSelectedCamera(int i) {
        this.selectedCameraInformation = listOfCameraInformation.get(i);
    }


    // ----------------------------------------------------------------------------------------------------------------------
    // CLASS METHODS --------------------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------------------------------


    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        scanCameras();
    }


    /** Check if this device has a camera */
    private boolean checkCameraHardware() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }


    /** Scan all the Cameras of the Device and populate the List of CameraInformation */
    public void scanCameras() {
        Log.d("ClinometerApplication", "Scan Cameras");
        listOfCameraInformation.clear();
        hasACamera = checkCameraHardware();         // checkCameraHardware() does NOT require CAMERA Permission
        if (hasACamera && (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)) {
            Log.d("ClinometerApplication", "Adding Cameras to list:");
            int numberOfCameras = Camera.getNumberOfCameras();
            for (int i = 0; i < numberOfCameras; i++) {
                Camera cam = Camera.open(i);
                Camera.Parameters params = cam.getParameters();
                if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                }

                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);

                CameraInformation cameraInformation = new CameraInformation();
                cameraInformation.id = i;
                cameraInformation.type = info.facing;
                cameraInformation.description = getString(info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ? R.string.pref_cameramode_camera_front : R.string.pref_cameramode_camera_rear);
                cameraInformation.minExposureCompensation = params.getMinExposureCompensation();
                cameraInformation.maxExposureCompensation = params.getMaxExposureCompensation();

                // TEST EXPOSURE COMPENSATION IN SETTINGS
//                if (i == 1) {
//                    cameraInformation.minExposureCompensation = -10;
//                    cameraInformation.maxExposureCompensation = 10;
//                }

                cameraInformation.horizontalViewAngle = params.getHorizontalViewAngle();
                listOfCameraInformation.add(cameraInformation);

//                if ((selectedCameraInformation == null) && (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK)) {
//                    selectedCameraInformation = cameraInformation;
//                    Log.d("ClinometerApplication", "Using Camera " + selectedCameraInformation.id);
//                }

                Log.d("ClinometerApplication", i + " = (" + cameraInformation.horizontalViewAngle + "°) " + (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ? "Front Camera" : "Rear Camera") +
                        " (" + cameraInformation.minExposureCompensation + " - " + cameraInformation.maxExposureCompensation + ")");
                cam.release();
                Log.d("ClinometerApplication", "Camera " + cameraInformation.id + " added to list");
            }

            int prefCamera = Integer.parseInt(preferences.getString(KEY_PREF_CAMERA, "0"));
            int prefExposureCompensation = preferences.getInt(KEY_PREF_CAMERA_EXPOSURE_COMPENSATION, 0);

            // Check if the Camera Index in Preferences is out of range. In case change it to index 0
            if (prefCamera >= listOfCameraInformation.size()) {
                prefCamera = 0;
                prefExposureCompensation = 0;
                SharedPreferences.Editor editor = preferences.edit();
                editor.remove(KEY_PREF_CAMERA_EXPOSURE_COMPENSATION);
                editor.remove(KEY_PREF_CAMERA);
                editor.commit();
            }
            selectedCameraInformation = listOfCameraInformation.get(prefCamera);

            // Check if the Exposure Compensation in Preferences is out of range. In case modify it
            boolean isExposureCompensationModified = false;
            if (prefExposureCompensation > selectedCameraInformation.maxExposureCompensation) {
                prefExposureCompensation = selectedCameraInformation.maxExposureCompensation;
                isExposureCompensationModified = true;
            }
            if (prefExposureCompensation < selectedCameraInformation.minExposureCompensation) {
                prefExposureCompensation = selectedCameraInformation.minExposureCompensation;
                isExposureCompensationModified = true;
            }
            if (isExposureCompensationModified) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(KEY_PREF_CAMERA_EXPOSURE_COMPENSATION, prefExposureCompensation);
                editor.commit();
            }
        } else {
            selectedCameraInformation = null;
        }
    }
}
