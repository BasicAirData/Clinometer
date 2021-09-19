/*
 * ClinometerActivity - Singleton Java Class for Android
 * Created by G.Capelli on 21/5/2020
 * This file is part of BasicAirData Clinometer
 *
 * Copyright (C) 2020 BasicAirData
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
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static eu.basicairdata.clinometer.ClinometerApplication.CAMERA_REQUEST_CODE;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_AUTOLOCK;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_AUTOLOCK_HORIZON_CHECK;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_AUTOLOCK_PRECISION;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CALIBRATION_ANGLE_0;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CALIBRATION_ANGLE_1;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CALIBRATION_ANGLE_2;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CALIBRATION_GAIN_0;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CALIBRATION_GAIN_1;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CALIBRATION_GAIN_2;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CALIBRATION_OFFSET_0;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CALIBRATION_OFFSET_1;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CALIBRATION_OFFSET_2;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_CAMERA_EXPOSURE_COMPENSATION;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_KEEP_SCREEN_ON;
import static eu.basicairdata.clinometer.ClinometerApplication.KEY_PREF_UNIT_OF_MEASUREMENT;


public class ClinometerActivity extends AppCompatActivity implements SensorEventListener {

    // Singleton instance
    private static ClinometerActivity singleton;

    public static ClinometerActivity getInstance(){
        return singleton;
    }

    private ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE);
    private Vibrator vibrator;

    // RefAxis Animator
    private PIDAnimator pid = new PIDAnimator(0.0f, 0.3f, 0.0f, 0.03f, 16);
    private float old_PIDValue = 0.0f;

    private static final int TOAST_TIME = 2500;                         // The time a toast is shown

    private static final float AUTOLOCK_MIN_TOLERANCE = 0.05f;          // The minimum tolerance of the AutoLock
    private static final float AUTOLOCK_MAX_TOLERANCE = 0.5f;           // The maximum tolerance of the AutoLock
    private static final float AUTOLOCK_HORIZON_CHECK_THRESHOLD = 5.0f; // The zone of horizon check (+- 5 degrees)
    private static final float ROTATION_THRESHOLD = 5;                  // The threshold of the boundaries for DisplayRotation (in degrees)
    private static final int   SIZE_OF_MEANVARIANCE = 200;              // 2 seconds
    private static final float ALPHA = 0.04f;                           // Weight of the new sensor reading

    private ClinometerApplication clinometerApplication;
    private SharedPreferences preferences;

    private boolean prefAutoLock = false;
    private boolean prefAutoLockHorizonCheck = true;
    private float prefAutoLockTolerance;
    private int prefExposureCompensation = 0;

    private boolean isSettingsClicked = false;           // True when the Three-dots button has been clicked

    private boolean isFlat = true;                       // True if the device is oriented flat (for example on a table)
    private boolean isLocked = false;                    // True if the angles are locked by user
    private boolean isLockRequested = false;
    private float displayRotation = 0;                   // The rotation angle from the natural position of the device

    private boolean isInCameraMode = false;              // True if Camera Mode is active
    private boolean isCameraLivePreviewActive = false;  // True if the Live Preview with Camera is active
    private Bitmap cameraPreviewBitmap;                 // The image saved from Camera Preview (used by Locking and onPause/onResume)

    // Singleton instance
//    private static ClinometerActivity singleton;
//    public static ClinometerActivity getInstance(){
//        return singleton;
//    }

    private PhysicalData phdAngle0;
    private PhysicalData phdAngle1;
    private PhysicalData phdAngle2;
    private PhysicalDataFormatter physicalDataFormatter = new PhysicalDataFormatter();

    private ClinometerView mClinometerView;
    private TextView mTextViewAngles;
    private TextView mTextViewToast;
    private TextView mTextViewKeepScreenVertical;
    private FrameLayout mFrameLayoutClinometer;
    private FrameLayout mFrameLayoutOverlays;
    private LinearLayout mLinearLayoutAngles;
    private LinearLayout mLinearLayoutToolbar;
    private ImageView mImageViewLock;
    private ImageView mImageViewSettings;
    private ImageView mImageViewCamera;
    private ImageView mImageViewCameraImage;
    private FrameLayout mFrameLayoutPreview;
    private BackgroundView mBackgroundView;

    private SensorManager mSensorManager;
    private Sensor mRotationSensor;

    private float[] gravity              = {0, 0, 0};    // The (filtered) current accelerometers values
    private float[] gravity_gain         = {0, 0, 0};
    private float[] gravity_offset       = {0, 0, 0};
    private float[] gravity_calibrated   = {0, 0, 0};    // The (filtered) current calibrated accelerometers values

    private float[] angle_calibration    = {0, 0, 0};    // The angles for calibration: alpha, beta, gamma (in degrees)
    private float[] angle                = {0, 0, 0};    // The (filtered) current angles (in degrees)

    private final float[][] calibrationMatrix = new float[3][3];

    private float gravityXY = 0;
    private float gravityXYZ = 0;
    private float angleXY = 0;                          // The angle on the horizontal plane (in degrees)
    private float angleXYZ = 0;                         // The angle between XY vector and the vertical (in degrees)
    private float angleTextLabels = 0;                   // The rotation angle for the text labels

    private final static int ACCELEROMETER_UPDATE_INTERVAL_MICROS = 10000;

    private final MeanVariance mvAngle0 = new MeanVariance(SIZE_OF_MEANVARIANCE);
    private final MeanVariance mvAngle1 = new MeanVariance(SIZE_OF_MEANVARIANCE);
    private final MeanVariance mvAngle2 = new MeanVariance(SIZE_OF_MEANVARIANCE);
    private final MeanVariance mvGravity0 = new MeanVariance(16);
    private final MeanVariance mvGravity1 = new MeanVariance(16);
    private final MeanVariance mvGravity2 = new MeanVariance(16);

    private ValueAnimator animationR = new ValueAnimator();

    private static Camera mCamera = null;
    private CameraPreview mPreview;


    // --------------------------------------------------------------------------------------------------------------------------
    // --- GETTERS AND SETTERS --------------------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------------------------------------------


    public void setPIDTargetValue(float newValue) {
        pid.setTargetValue(newValue);
    }


    public float getPIDValue() {
        return pid.getValue();
    }


    public float getDisplayRotation() {
        return displayRotation;
    }

    public boolean isFlat() {
        return isFlat;
    }


    public float[] getAngles() {
        return angle;
    }


    public float getAngleXY() {
        return angleXY;
    }


    public float getAngleXYZ() {
        return angleXYZ;
    }


    public float getAngleTextLabels() {
        return angleTextLabels;
    }


    // --------------------------------------------------------------------------------------------------------------------------
    // --- CLASS METHODS --------------------------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------------------------------------------


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

        singleton = this;

        clinometerApplication = ClinometerApplication.getInstance();
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        setContentView(R.layout.activity_clinometer);

        mClinometerView = findViewById(R.id.id_clinometerview);
        mTextViewAngles = findViewById(R.id.id_textview_angles);
        mTextViewToast = findViewById(R.id.id_textview_toast);
        mTextViewKeepScreenVertical = findViewById(R.id.id_textview_keep_screen_vertical);
        mImageViewLock = findViewById(R.id.id_imageview_lock);
        mImageViewSettings = findViewById(R.id.id_imageview_settings);
        mImageViewCamera = findViewById(R.id.id_imageview_camera);
        mFrameLayoutClinometer = findViewById(R.id.id_framelayout_clinometer);
        mFrameLayoutOverlays = findViewById(R.id.id_framelayout_overlay);
        mLinearLayoutAngles = findViewById(R.id.id_linearlayout_angles);
        mLinearLayoutToolbar = findViewById(R.id.id_linearlayout_toolbar);

        mImageViewCameraImage = findViewById(R.id.id_imageview_cameraimage);
        mFrameLayoutPreview = findViewById(R.id.camera_preview);
        mBackgroundView = findViewById(R.id.id_backgroundview);


        mImageViewCamera.setAlpha(0.4f);
        mLinearLayoutToolbar.setBackground(null);

//        for (int i = 0; i < SIZE_OF_MEANVARIANCE; i++) {
//            MVGravity0.LoadSample(0.0f);
//            MVGravity1.LoadSample(0.0f);
//            MVGravity2.LoadSample(9.81f);
//        }

        mvGravity0.reset(0.0f);
        mvGravity1.reset(0.0f);
        mvGravity2.reset(9.80f);

        // ---------- Check sensors

        Log.d("Clinometer", "- ROTATION_VECTOR Sensors = " + mSensorManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR).size());
        Log.d("Clinometer", "- ACCELEROMETER Sensors = " + mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size());

        mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (mRotationSensor == null) Log.d("Clinometer", "NO ACCELEROMETER FOUND!");

        // ---------- Button Listeners

        mLinearLayoutAngles.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.i("SpiritLevel", "Lock onTouch");
                if (isLocked) {
                    isLocked = false;
                    isLockRequested = false;
                    if (isInCameraMode) activateCamera();
                    mImageViewCameraImage.setImageBitmap(null);
                    cameraPreviewBitmap = null;
                }
                else isLockRequested = !isLockRequested;
                updateLockIcon();
                return false;
            }
        });

        mImageViewSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isSettingsClicked) {
                    isSettingsClicked = true;
                    Intent intent = new Intent(getBaseContext(), SettingsActivity.class);
                    startActivity(intent);
                }
            }
        });

        if (clinometerApplication.hasCamera()) {
            mImageViewCamera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(ClinometerActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
                    } else {
                        isInCameraMode = switchToCameraMode(!isInCameraMode);
                    }
                }
            });
        } else {
            mImageViewCamera.setVisibility(View.GONE);
        }

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if (!preferences.contains(KEY_PREF_CALIBRATION_ANGLE_0)) {
            // Not Calibrated!
            showToast(getString(R.string.toast_calibrate_before_use));
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            Log.w("ClinometerActivity", "onRequestPermissionsResult()");
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                isInCameraMode = switchToCameraMode(!isInCameraMode);
            } else {
                showToast(getString(R.string.toast_please_grant_camera_permission));
            }
        }
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }


    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        if (isInCameraMode) releaseCamera(true);
    }


    @Override
    protected void onResume() {
        super.onResume();
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        loadPreferences();

        phdAngle0 = physicalDataFormatter.format(angle[0]);
        phdAngle1 = physicalDataFormatter.format(angle[1]);
        phdAngle2 = physicalDataFormatter.format(angle[2]);
        mTextViewAngles.setText(phdAngle0.value + phdAngle0.um + "  "
                + phdAngle1.value + phdAngle1.um + "  "
                + phdAngle2.value + phdAngle2.um);

        mFrameLayoutClinometer.setSystemUiVisibility(
                //View.SYSTEM_UI_FLAG_IMMERSIVE |
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                //View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                // Hide the nav bar and status bar
                //View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN
        );

        this.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        isSettingsClicked = false;
        isLockRequested = false;
        updateLockIcon();

        mSensorManager.registerListener(this, mRotationSensor, ACCELEROMETER_UPDATE_INTERVAL_MICROS);

        if (isInCameraMode && !isLocked){
            cameraPreviewBitmap = null;
            mImageViewCameraImage.setImageBitmap(null);
            activateCamera();
        }
    }


    @Override
    public void onStop() {
        Log.w("myApp", "[#] " + this + " - onStop()");
        super.onStop();
    }


    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            // LOCKING

            if (isLockRequested) {
                if (!prefAutoLock) {
                    isLockRequested = false;
                    isLocked = true;
                    mClinometerView.invalidate();
                    updateLockIcon();
                    beep();
                    if (isInCameraMode) releaseCamera(true);
                } else if ((mvAngle0.getTolerance() < prefAutoLockTolerance)
                        && (mvAngle1.getTolerance() < prefAutoLockTolerance)
                        && (mvAngle2.getTolerance() < prefAutoLockTolerance)
                        && mvAngle0.getLoaded()
                        && mvAngle1.getLoaded()
                        && mvAngle2.getLoaded()
                        && ((!prefAutoLockHorizonCheck)
                                || (Math.abs(angle[2]) >= AUTOLOCK_HORIZON_CHECK_THRESHOLD)
                                || (prefAutoLockHorizonCheck && (Math.abs(angle[2]) < AUTOLOCK_HORIZON_CHECK_THRESHOLD) && (Math.abs(mvAngle2.getMeanValue()) < prefAutoLockTolerance)))) {

                    angle[0] = (float) (180 / Math.PI * Math.asin((mvGravity0.getMeanValue() / Math.max(gravityXYZ, 0.00001f))));
                    angle[1] = (float) (180 / Math.PI * Math.asin((mvGravity1.getMeanValue() / Math.max(gravityXYZ, 0.00001f))));
                    angle[2] = (float) (180 / Math.PI * Math.asin((mvGravity2.getMeanValue() / Math.max(gravityXYZ, 0.00001f))));

                    angleXY = 0;
                    if (gravityXY > 0) {
                        if (mvGravity0.getMeanValue() >= 0) {
                            if (mvGravity1.getMeanValue() < 0)
                                angleXY = (float) Math.toDegrees(-Math.asin(mvGravity1.getMeanValue() / gravityXY));
                            else
                                angleXY = (float) Math.toDegrees(2 * Math.PI - Math.asin(mvGravity1.getMeanValue() / gravityXY));
                        } else
                            angleXY = (float) Math.toDegrees(Math.PI + Math.asin(mvGravity1.getMeanValue() / gravityXY));
                    }

                    angleXYZ = 0;
                    if (gravityXY > 0) {
                        angleXYZ = (float) Math.toDegrees(Math.acos(mvGravity2.getMeanValue() / gravityXYZ));
                    }

                    Log.d("SpiritLevel", "------------------------------------------------------------------");
                    Log.d("SpiritLevel", String.format("Auto Locking Tolerance = %1.4f", prefAutoLockTolerance));
                    Log.d("SpiritLevel", (String.format("Measurement locked - Angle0: Mean=%+1.4f Uncertainty=%+1.4f", mvAngle0.getMeanValue(), mvAngle0.getTolerance())));
                    Log.d("SpiritLevel", (String.format("Measurement locked - Angle1: Mean=%+1.4f Uncertainty=%+1.4f", mvAngle1.getMeanValue(), mvAngle1.getTolerance())));
                    Log.d("SpiritLevel", (String.format("Measurement locked - Angle2: Mean=%+1.4f Uncertainty=%+1.4f", mvAngle2.getMeanValue(), mvAngle2.getTolerance())));
                    Log.d("SpiritLevel", "------------------------------------------------------------------");

                    isLockRequested = false;
                    isLocked = true;
                    mClinometerView.invalidate();
                    updateLockIcon();
                    beep();
                    if (isInCameraMode) releaseCamera(true);

                    mvAngle0.reset();
                    mvAngle1.reset();
                    mvAngle2.reset();
                }
            }

            // SIGNAL PROCESSING

            if (!isLocked) {

                // Weighted gravity reads

                if ((gravity[0] == 0) && (gravity[1] == 0) && (gravity[2] == 0)) {
                    gravity[0] = (event.values[0] - gravity_offset[0]) / gravity_gain[0];   // X
                    gravity[1] = (event.values[1] - gravity_offset[1]) / gravity_gain[1];   // Y
                    gravity[2] = (event.values[2] - gravity_offset[2]) / gravity_gain[2];   // Z
                } else {
                    gravity[0] = (1 - ALPHA) * gravity[0] + (ALPHA) * (event.values[0] - gravity_offset[0]) / gravity_gain[0];
                    gravity[1] = (1 - ALPHA) * gravity[1] + (ALPHA) * (event.values[1] - gravity_offset[1]) / gravity_gain[1];
                    gravity[2] = (1 - ALPHA) * gravity[2] + (ALPHA) * (event.values[2] - gravity_offset[2]) / gravity_gain[2];
                }

                // Apply Calibration values

                gravity_calibrated[0] = (float) (gravity[0] * calibrationMatrix[0][0] + gravity[1] * calibrationMatrix[0][1] + gravity[2] * calibrationMatrix[0][2]);
                gravity_calibrated[1] = (float) (gravity[0] * calibrationMatrix[1][0] + gravity[1] * calibrationMatrix[1][1] + gravity[2] * calibrationMatrix[1][2]);
                gravity_calibrated[2] = (float) (gravity[0] * calibrationMatrix[2][0] + gravity[1] * calibrationMatrix[2][1] + gravity[2] * calibrationMatrix[2][2]);

                mvGravity0.loadSample(gravity_calibrated[0]);
                mvGravity1.loadSample(gravity_calibrated[1]);
                mvGravity2.loadSample(gravity_calibrated[2]);

                gravityXY = (float) Math.sqrt(mvGravity0.getMeanValue() * mvGravity0.getMeanValue() + mvGravity1.getMeanValue() * mvGravity1.getMeanValue());   // Vector over the screen plane
                gravityXYZ = (float) Math.sqrt(gravityXY * gravityXY + mvGravity2.getMeanValue() * mvGravity2.getMeanValue());                                  // Spatial Vector

                // Calculate Angles

                angleXY = 0;
                if (gravityXY > 0) {
                    if (mvGravity0.getMeanValue() >= 0) {
                        if (mvGravity1.getMeanValue() < 0)
                            angleXY = (float) Math.toDegrees(-Math.asin(mvGravity1.getMeanValue() / gravityXY));
                        else
                            angleXY = (float) Math.toDegrees(2 * Math.PI - Math.asin(mvGravity1.getMeanValue() / gravityXY));
                    } else
                        angleXY = (float) Math.toDegrees(Math.PI + Math.asin(mvGravity1.getMeanValue() / gravityXY));
                }

                angleXYZ = 0;
                if (gravityXY > 0) {
                    angleXYZ = (float) Math.toDegrees(Math.acos(mvGravity2.getMeanValue() / gravityXYZ));
                }

                angle[0] = (float) (180 / Math.PI * Math.asin((mvGravity0.getMeanValue() / Math.max(gravityXYZ, 0.00001f))));
                angle[1] = (float) (180 / Math.PI * Math.asin((mvGravity1.getMeanValue() / Math.max(gravityXYZ, 0.00001f))));
                angle[2] = (float) (180 / Math.PI * Math.asin((mvGravity2.getMeanValue() / Math.max(gravityXYZ, 0.00001f))));

                // Load angles into Auto-Locking MeanVariances

                mvAngle0.loadSample(angle[0]);
                mvAngle1.loadSample(angle[1]);
                mvAngle2.loadSample(angle[2]);

                // Determine Rotation, ViewMode and Text Angles

                if (Math.abs(angle[2]) < 70) {
                    if ((angleXY > 270 - 45 + ROTATION_THRESHOLD) && (angleXY < 270 + 45 - ROTATION_THRESHOLD) && (displayRotation != 0)) {
                        displayRotation = 0;
                        Log.w("SpiritLevel", " ROTATION = " + displayRotation);
                        rotateOverlays(displayRotation, this.getWindowManager().getDefaultDisplay().getHeight(), this.getWindowManager().getDefaultDisplay().getWidth());
                    }
                    if ((angleXY > 90 - 45 + ROTATION_THRESHOLD) && (angleXY < 90 + 45 - ROTATION_THRESHOLD) && (displayRotation != 180)) {
                        displayRotation = 180;
                        Log.w("SpiritLevel", " ROTATION = " + displayRotation);
                        rotateOverlays(displayRotation, this.getWindowManager().getDefaultDisplay().getHeight(), this.getWindowManager().getDefaultDisplay().getWidth());
                    }
                    if ((angleXY > 180 - 45 + ROTATION_THRESHOLD) && (angleXY < 180 + 45 - ROTATION_THRESHOLD) && (displayRotation != 270)) {
                        displayRotation = 270;
                        Log.w("SpiritLevel", " ROTATION = " + displayRotation);
                        rotateOverlays(displayRotation, this.getWindowManager().getDefaultDisplay().getWidth(), this.getWindowManager().getDefaultDisplay().getHeight());
                    }
                    if (((angleXY > 270 + 45 + ROTATION_THRESHOLD) || (angleXY < 45 - ROTATION_THRESHOLD)) && (displayRotation != 90)) {
                        displayRotation = 90;
                        Log.w("SpiritLevel", " ROTATION = " + displayRotation);
                        rotateOverlays(displayRotation, this.getWindowManager().getDefaultDisplay().getWidth(), this.getWindowManager().getDefaultDisplay().getHeight());
                    }
                }

                if (Math.abs(angle[2]) < 70) {
                    if (isFlat) isFlat = false;
                    angleTextLabels = (90 + angleXY) % 360;
                }
                if ((Math.abs(angle[2]) >= 70) && (Math.abs(angle[2]) < 75)) {
                    if ((displayRotation == 0) && (angleXY < 270)) {
                        //Log.d("SpiritLevel", "ANG");
                        angleTextLabels = displayRotation * (Math.abs(angle[2]) - 70) / 5
                                + (((90 + angleXY) % 360) - 360) * (75 - Math.abs(angle[2])) / 5;
                    } else {
                        angleTextLabels = displayRotation * (Math.abs(angle[2]) - 70) / 5
                                + ((90 + angleXY) % 360) * (75 - Math.abs(angle[2])) / 5;
                    }
                }
                if (Math.abs(angle[2]) >= 75) {
                    if (!isFlat) isFlat = true;
                    angleTextLabels = displayRotation;
                }

                // Show hint in camera mode
                if (isCameraLivePreviewActive) {
                    if (Math.abs(angle[2]) > 7.5f) {
                        // The Screen is in vertical
                        mTextViewKeepScreenVertical.setVisibility(View.VISIBLE);
                    }
                    if (Math.abs(angle[2]) < 7f) {
                        mTextViewKeepScreenVertical.setVisibility(View.GONE);
                    }
                }
                // Apply Changes
                mClinometerView.invalidate();

                // You must put this setText here in order to force the re-layout also during the rotations.
                // Without this, if you lock the measure during the rotation animation, the layout doesn't change correctly :(

                phdAngle0 = physicalDataFormatter.format(angle[0]);
                phdAngle1 = physicalDataFormatter.format(angle[1]);
                phdAngle2 = physicalDataFormatter.format(angle[2]);
                mTextViewAngles.setText(phdAngle0.value + phdAngle0.um + "  "
                        + phdAngle1.value + phdAngle1.um + "  "
                        + phdAngle2.value + phdAngle2.um);
//                mTextViewAngles.setText(String.format("%1.1f°  %1.1f°  %1.1f°", angle[0], angle[1], angle[2]));
            }

            if (Math.abs(pid.getValue() - old_PIDValue) > 0.001) {
                old_PIDValue = pid.getValue();
                mClinometerView.invalidate();
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    private void updateLockIcon() {
        if (isLocked) {
            mImageViewLock.setImageResource(R.drawable.ic_lock_24);
            mImageViewLock.setAlpha(0.9f);
        } else {
            if (isLockRequested) {
                mImageViewLock.setImageResource(R.drawable.ic_lock_open_24);
                mImageViewLock.setAlpha(0.9f);
            } else {
                mImageViewLock.setImageResource(R.drawable.ic_lock_open_24);
                mImageViewLock.setAlpha(0.4f);
            }
        }
    }


    private void loadPreferences() {
        if (preferences.getBoolean(KEY_PREF_KEEP_SCREEN_ON, true)) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        prefAutoLock = preferences.getBoolean(KEY_PREF_AUTOLOCK, false);
        clinometerApplication.setPrefUM(Integer.parseInt(preferences.getString(KEY_PREF_UNIT_OF_MEASUREMENT, (getResources().getStringArray(R.array.UMAnglesValues))[0])));
        prefAutoLockHorizonCheck = preferences.getBoolean(KEY_PREF_AUTOLOCK_HORIZON_CHECK, true);
        prefAutoLockTolerance = AUTOLOCK_MAX_TOLERANCE - (AUTOLOCK_MAX_TOLERANCE - AUTOLOCK_MIN_TOLERANCE) * preferences.getInt(KEY_PREF_AUTOLOCK_PRECISION, 500) / 1000;
        Log.d("Clinometer", String.format("Auto Locking Tolerance = %1.3f", prefAutoLockTolerance));

        prefExposureCompensation = preferences.getInt(KEY_PREF_CAMERA_EXPOSURE_COMPENSATION, 0);

        angle_calibration[0]    = preferences.getFloat(KEY_PREF_CALIBRATION_ANGLE_0, 0);
        angle_calibration[1]    = preferences.getFloat(KEY_PREF_CALIBRATION_ANGLE_1, 0);
        angle_calibration[2]    = preferences.getFloat(KEY_PREF_CALIBRATION_ANGLE_2, 0);
        gravity_gain[0]         = preferences.getFloat(KEY_PREF_CALIBRATION_GAIN_0, 1);
        gravity_gain[1]         = preferences.getFloat(KEY_PREF_CALIBRATION_GAIN_1, 1);
        gravity_gain[2]         = preferences.getFloat(KEY_PREF_CALIBRATION_GAIN_2, 1);
        gravity_offset[0]       = preferences.getFloat(KEY_PREF_CALIBRATION_OFFSET_0, 0);
        gravity_offset[1]       = preferences.getFloat(KEY_PREF_CALIBRATION_OFFSET_1, 0);
        gravity_offset[2]       = preferences.getFloat(KEY_PREF_CALIBRATION_OFFSET_2, 0);

        calibrationMatrix[0][0] = (float) (Math.cos(Math.toRadians(angle_calibration[2])) * Math.cos(Math.toRadians(angle_calibration[0])) + Math.sin(Math.toRadians(angle_calibration[2])) * Math.sin(Math.toRadians(angle_calibration[1])) * Math.sin(Math.toRadians(angle_calibration[0])));
        calibrationMatrix[0][1] = (float) (Math.cos(Math.toRadians(angle_calibration[1])) * Math.sin(Math.toRadians(angle_calibration[0])));
        calibrationMatrix[0][2] = (float) (-Math.sin(Math.toRadians(angle_calibration[2])) * Math.cos(Math.toRadians(angle_calibration[0])) + Math.cos(Math.toRadians(angle_calibration[2])) * Math.sin(Math.toRadians(angle_calibration[1])) * Math.sin(Math.toRadians(angle_calibration[0])));

        calibrationMatrix[1][0] = (float) (-Math.cos(Math.toRadians(angle_calibration[2])) * Math.sin(Math.toRadians(angle_calibration[0])) + Math.sin(Math.toRadians(angle_calibration[2])) * Math.sin(Math.toRadians(angle_calibration[1])) * Math.cos(Math.toRadians(angle_calibration[0])));
        calibrationMatrix[1][1] = (float) (Math.cos(Math.toRadians(angle_calibration[1])) * Math.cos(Math.toRadians(angle_calibration[0])));
        calibrationMatrix[1][2] = (float) (Math.sin(Math.toRadians(angle_calibration[2])) * Math.sin(Math.toRadians(angle_calibration[0])) + Math.cos(Math.toRadians(angle_calibration[2])) * Math.sin(Math.toRadians(angle_calibration[1])) * Math.cos(Math.toRadians(angle_calibration[0])));

        calibrationMatrix[2][0] = (float) (Math.sin(Math.toRadians(angle_calibration[2])) * Math.cos(Math.toRadians(angle_calibration[1])));
        calibrationMatrix[2][1] = (float) (-Math.sin(Math.toRadians(angle_calibration[1])));
        calibrationMatrix[2][2] = (float) (Math.cos(Math.toRadians(angle_calibration[2])) * Math.cos(Math.toRadians(angle_calibration[1])));
    }


    private void beep() {
        //toneGen1.startTone(ToneGenerator.TONE_SUP_PIP,150);
        vibrator.vibrate(250);
        toneGen1.startTone(ToneGenerator.TONE_PROP_BEEP, 100);
    }


    private void rotateOverlays(final float rotationAngle, final int newHeight, final int newWidth) {
        if (animationR.isRunning()) animationR.cancel();

        animationR = ValueAnimator.ofFloat(0, 1);
        animationR.setDuration(700);
        animationR.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator updatedAnimation) {
                float animatedValue = (float) Math.sqrt(Math.sqrt((float) updatedAnimation.getAnimatedValue()));
                float alpha = 2 * Math.abs(animatedValue - 0.5f);           // Transparency for Fades
                //Log.d("SpiritLevel", "alpha = " + alpha);
                mFrameLayoutOverlays.setAlpha(alpha);
                if ((animatedValue >= 0.5f) && (mFrameLayoutOverlays.getRotation() != rotationAngle)) {
                    //Log.d("SpiritLevel", "change parameters = " + rotationAngle + ", " + newWidth + ", " + newHeight);
                    mFrameLayoutOverlays.getLayoutParams().height = newHeight;
                    mFrameLayoutOverlays.getLayoutParams().width = newWidth;
                    mFrameLayoutOverlays.setRotation(rotationAngle);
                }
                // You must put this setText here in order to force the re-layout also during the rotations.
                // Without this, if you lock the measure during the rotation animation, the layout doesn't change correctly :(
                phdAngle0 = physicalDataFormatter.format(angle[0]);
                phdAngle1 = physicalDataFormatter.format(angle[1]);
                phdAngle2 = physicalDataFormatter.format(angle[2]);
                mTextViewAngles.setText(phdAngle0.value + phdAngle0.um + "  "
                        + phdAngle1.value + phdAngle1.um + "  "
                        + phdAngle2.value + phdAngle2.um);
//                mTextViewAngles.setText(String.format("%1.1f°  %1.1f°  %1.1f°", angle[0], angle[1], angle[2]));
            }
        });
        animationR.start();
    }


    // --------------------------------------------------------------------------------------------------------------------------
    // --- TOASTS ---------------------------------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------------------------------------------


    final Handler toastHandler = new Handler();
    final Runnable r = new Runnable() {
        @Override
        public void run() {
            hideToast();
        }
    };


    private void showToast(String text) {
        toastHandler.removeCallbacks(r);
        mTextViewToast.setText(text);
        mTextViewToast.setVisibility(View.VISIBLE);
        toastHandler.postDelayed(r, TOAST_TIME);
    }


    private void hideToast() {
        if (mTextViewToast != null) mTextViewToast.setVisibility(View.GONE);
    }


    // --------------------------------------------------------------------------------------------------------------------------
    // --- CAMERA MANAGEMENT ----------------------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------------------------------------------


    /** A safe way to get an instance of the Camera object. */
    private Camera getCameraInstance(){
        Camera c = null;
        try {
            clinometerApplication.scanCameras();
            c = Camera.open(clinometerApplication.getSelectedCameraInformation().id); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }


    /** Switch on/off the Camera Mode */
    private boolean switchToCameraMode(boolean newState) {
        Log.d("Clinometer", "switchToCameraMode(" + newState + ")");
        boolean result = newState;
        if (newState) {
            // Switch ON the Camera Mode
            if (!isLocked || (cameraPreviewBitmap == null)) {
                result = activateCamera();
                Log.d("Clinometer", "switchToCameraMode result = " + result);
                if (!result) return false;
                mImageViewCameraImage.setVisibility(View.GONE);
            } else {
                mImageViewCameraImage.setVisibility(View.VISIBLE);
            }
            mImageViewCamera.setAlpha(0.9f);
            mLinearLayoutToolbar.setBackgroundResource(R.drawable.rounded_corner);
            mBackgroundView.setVisibility(View.GONE);
        } else {
            // Switch OFF the Camera Mode
            releaseCamera(false);
            mImageViewCamera.setAlpha(0.4f);
            mLinearLayoutToolbar.setBackground(null);
            mBackgroundView.setVisibility(View.VISIBLE);
            mImageViewCameraImage.setVisibility(View.GONE);
        }
        return result;
    }


    /** Activate the Camera Preview */
    private boolean activateCamera() {
        if (isCameraLivePreviewActive) return true;
        if (mCamera == null) {
            //showToast(getString(R.string.toast_activation_of_the_camera));
            // Create an instance of Camera
            mCamera = getCameraInstance();
        }
        if (mCamera == null) return false;

        // Create our Preview view and set it as the content of our activity.
        mFrameLayoutPreview.removeAllViews();
        mPreview = new CameraPreview(this, mCamera, prefExposureCompensation);
        mFrameLayoutPreview.addView(mPreview);
        mFrameLayoutPreview.setVisibility(View.VISIBLE);
        mImageViewCameraImage.setVisibility(View.GONE);

        // SCALE TO FILL SCREEN WITHOUT DEFORMATION ------------------------------------------------
        // Based on: https://startandroid.ru/ru/uroki/vse-uroki-spiskom/264-urok-132-kamera-vyvod-izobrazhenija-na-ekran-obrabotka-povorota.html

        Display display = getWindowManager().getDefaultDisplay();

        // RectF of the Display, it matches the size of the Window
        RectF rectDisplay = new RectF();
        rectDisplay.set(0, 0, display.getWidth(), display.getHeight());
        Size size = mCamera.getParameters().getPreviewSize();

        // RectF of the Camera Preview
        RectF rectPreview = new RectF();
        if (display.getWidth() > display.getHeight()) {
            rectPreview.set(0, 0, size.width, size.height);
        } else {
            rectPreview.set(0, 0, size.height, size.width);                // vertical preview
        }

        // Transformation matrix
        Matrix matrix = new Matrix();
        matrix.setRectToRect(rectPreview, rectDisplay, Matrix.ScaleToFit.START);    // if the preview is "squeezed" into the screen
        matrix.mapRect(rectPreview);                                                // transformation

        // Find the max Scale Factor in order to scale the image and the preview
        // to fill the whole screen (without distortion). They will be cropped when necessary (drawn offscreen)
        float maxScaleFactor = Math.max(rectDisplay.width()/rectPreview.width(), rectDisplay.height()/rectPreview.height());
        Log.d("Clinometer", "Scale Factors: W=" + rectDisplay.width()/rectPreview.width() + " H=" + rectDisplay.height()/rectPreview.height());

        // Set gravity, height and width of Camera Preview and Camera Image
        FrameLayout.LayoutParams layout = new FrameLayout.LayoutParams(mPreview.getWidth(), mPreview.getHeight());
        layout.gravity = Gravity.CENTER;
        layout.height = (int) (rectPreview.bottom * maxScaleFactor);
        layout.width = (int) (rectPreview.right * maxScaleFactor);
        mPreview.setLayoutParams(layout);
        mImageViewCameraImage.setLayoutParams(layout);

        // -----------------------------------------------------------------------------------------

        isCameraLivePreviewActive = true;
        return true;
    }


    final Camera.PreviewCallback cameraPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
            try {
                mPreview.pausePreview();
                Camera.Parameters parameters = camera.getParameters();
                Log.d("Clinometer", "onPreviewFrame");

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                YuvImage yuvImage = new YuvImage(bytes, parameters.getPreviewFormat(), parameters.getPreviewSize().width, parameters.getPreviewSize().height, null);
                yuvImage.compressToJpeg(new Rect(0, 0, parameters.getPreviewSize().width, parameters.getPreviewSize().height), 90, out);
                byte[] imageBytes = out.toByteArray();
                cameraPreviewBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                out.flush();
                out.close();
            } catch (IOException e) {
                Log.d("Clinometer", "IOException: " + e);
            }
            Matrix matrix = new Matrix();
            //this will prevent mirror effect
            if (clinometerApplication.getSelectedCameraInformation().type == CameraInformation.FRONT_CAMERA) matrix.preScale(-1.0f, 1.0f);
            matrix.postRotate(mPreview.getRotationDegrees());
            mImageViewCameraImage.setImageBitmap(Bitmap.createBitmap(cameraPreviewBitmap, 0, 0,
                    cameraPreviewBitmap.getWidth(),
                    cameraPreviewBitmap.getHeight(), matrix, true));
            stopCamera();
        }
    };


    /** Stops the Camera Preview.
     * If saveImage is true, the method saves the last frame before stop the Preview */
    private void releaseCamera(boolean saveImage) {
        mTextViewKeepScreenVertical.setVisibility(View.GONE);
        if (mCamera != null) {
            if (saveImage) {
                mCamera.setOneShotPreviewCallback(cameraPreviewCallback);
            } else {
                stopCamera();
            }
        }
    }


    /** Stops the Camera Preview */
    private void stopCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        mImageViewCameraImage.setVisibility(View.VISIBLE);
        mFrameLayoutPreview.setVisibility(View.GONE);
        mFrameLayoutPreview.removeAllViews();
        isCameraLivePreviewActive = false;
    }
}
