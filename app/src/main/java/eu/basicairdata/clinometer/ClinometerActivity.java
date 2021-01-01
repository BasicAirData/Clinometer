/*
 * ClinometerActivity - Singleton Java Class for Android
 * Created by G.Capelli (BasicAirData) on 21/5/2020
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

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;



public class ClinometerActivity extends AppCompatActivity implements SensorEventListener {

    ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE);
    private Vibrator vibrator;

    private static final float AUTOLOCK_MIN_TOLERANCE = 0.05f;          // The minimum tolerance of the AutoLock
    private static final float AUTOLOCK_MAX_TOLERANCE = 0.5f;           // The maximum tolerance of the AutoLock
    private static final float AUTOLOCK_HORIZON_CHECK_THRESHOLD = 5.0f; // The zone of horizon check (+- 5 degrees)
    private static final float ROTATION_THRESHOLD = 5;                  // The threshold of the boundaries for DisplayRotation (in degrees)
    private static final int SIZE_OF_MEANVARIANCE = 200;                // 2 seconds

    private static final float ALPHA = 0.04f;                           // Weight of the new sensor reading

    private SharedPreferences preferences;
    private boolean prefAutoLock = false;
    private boolean prefAutoLockHorizonCheck = true;
    private float prefAutoLockTolerance;

    public boolean isFlat = true;                       // True if the device is oriented flat (for example on a table)
    public boolean isLocked = false;                    // True if the angles are locked by user
    private boolean isLockRequested = false;
    public float DisplayRotation = 0;                   // The rotation angle from the natural position of the device

    // Singleton instance
    private static ClinometerActivity singleton;
    public static ClinometerActivity getInstance(){
        return singleton;
    }

    private ClinometerView mClinometerView;
    private TextView mTextViewAngles;
    private FrameLayout mFrameLayoutClinometer;
    private LinearLayout mLinearLayoutOverlays;
    private LinearLayout mLinearLayoutAngles;
    private ImageView mImageViewLock;
    private ImageView mImageViewSettings;

    private SensorManager mSensorManager;
    private Sensor mRotationSensor;

    public float[] gravity              = {0, 0, 0};    // The (filtered) current accelerometers values
    public float[] gravity_gain         = {0, 0, 0};
    public float[] gravity_offset       = {0, 0, 0};
    public float[] gravity_calibrated   = {0, 0, 0};    // The (filtered) current calibrated accelerometers values

    public float[] angle_calibration    = {0, 0, 0};    // The angles for calibration: alpha, beta, gamma (in degrees)
    public float[] angle                = {0, 0, 0};    // The (filtered) current angles (in degrees)

    private float[][] CalibrationMatrix = new float[3][3];

    public float gravityXY = 0;
    public float gravityXYZ = 0;
    public float angleXY = 0;                           // The angle on the horizontal plane (in degrees)
    public float angleXYZ = 0;                          // The angle between XY vector and the vertical (in degrees)
    public float angleTextLabels = 0;                   // The rotation angle for the text labels

    private final static int ACCELEROMETER_UPDATE_INTERVAL_MICROS = 10000;

    MeanVariance MVAngle0 = new MeanVariance(SIZE_OF_MEANVARIANCE);
    MeanVariance MVAngle1 = new MeanVariance(SIZE_OF_MEANVARIANCE);
    MeanVariance MVAngle2 = new MeanVariance(SIZE_OF_MEANVARIANCE);
    MeanVariance MVGravity0 = new MeanVariance(16);
    MeanVariance MVGravity1 = new MeanVariance(16);
    MeanVariance MVGravity2 = new MeanVariance(16);

    ValueAnimator animationR = new ValueAnimator();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        singleton = this;

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        setContentView(R.layout.activity_clinometer);

        mClinometerView = findViewById(R.id.id_clinometerview);
        mTextViewAngles = findViewById(R.id.id_textview_angles);
        mImageViewLock = findViewById(R.id.id_imageview_lock);
        mImageViewSettings = findViewById(R.id.id_imageview_settings);
        mFrameLayoutClinometer = findViewById(R.id.id_framelayout_clinometer);
        mLinearLayoutOverlays = findViewById(R.id.id_linearlayout_overlay);
        mLinearLayoutAngles = findViewById(R.id.id_linearlayout_angles);

//        for (int i = 0; i < SIZE_OF_MEANVARIANCE; i++) {
//            MVGravity0.LoadSample(0.0f);
//            MVGravity1.LoadSample(0.0f);
//            MVGravity2.LoadSample(9.81f);
//        }

        MVGravity0.Reset(0.0f);
        MVGravity1.Reset(0.0f);
        MVGravity2.Reset(9.80f);

        // Check sensors
        Log.d("SpiritLevel", "- ROTATION_VECTOR Sensors = " + mSensorManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR).size());
        Log.d("SpiritLevel", "- ACCELEROMETER Sensors = " + mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size());

        mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (mRotationSensor == null) Log.d("SpiritLevel", "NO ACCELEROMETER FOUND!");

        mLinearLayoutAngles.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.i("SpiritLevel", "Lock onTouch");
                if (isLocked) {
                    isLocked = false;
                    isLockRequested = false;
                }
                else isLockRequested = !isLockRequested;
                UpdateLockIcon();
                return false;
            }
        });

        mImageViewSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getBaseContext(), SettingsActivity.class);
                startActivity(intent);
            }
        });

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }


    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        LoadPreferences();

        mClinometerView.setSystemUiVisibility(
                //View.SYSTEM_UI_FLAG_IMMERSIVE |
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN //|
                // Hide the nav bar and status bar
                //View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                //View.SYSTEM_UI_FLAG_FULLSCREEN
        );

        this.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        isLockRequested = false;
        UpdateLockIcon();

        mSensorManager.registerListener(this, mRotationSensor, ACCELEROMETER_UPDATE_INTERVAL_MICROS);
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
                    UpdateLockIcon();
                    Beep();
                } else if ((MVAngle0.getTolerance() < prefAutoLockTolerance)
                        && (MVAngle1.getTolerance() < prefAutoLockTolerance)
                        && (MVAngle2.getTolerance() < prefAutoLockTolerance)
                        && MVAngle0.isLoaded()
                        && MVAngle1.isLoaded()
                        && MVAngle2.isLoaded()
                        && (
                        (!prefAutoLockHorizonCheck)
                                || (Math.abs(angle[2]) >= AUTOLOCK_HORIZON_CHECK_THRESHOLD)
                                || (prefAutoLockHorizonCheck && (Math.abs(angle[2]) < AUTOLOCK_HORIZON_CHECK_THRESHOLD) && (Math.abs(MVAngle2.getMeanValue()) < prefAutoLockTolerance)))) {

                    angle[0] = (float) (180 / Math.PI * Math.asin((MVGravity0.getMeanValue() / Math.max(gravityXYZ, 0.00001f))));
                    angle[1] = (float) (180 / Math.PI * Math.asin((MVGravity1.getMeanValue() / Math.max(gravityXYZ, 0.00001f))));
                    angle[2] = (float) (180 / Math.PI * Math.asin((MVGravity2.getMeanValue() / Math.max(gravityXYZ, 0.00001f))));

                    angleXY = 0;
                    if (gravityXY > 0) {
                        if (MVGravity0.getMeanValue() >= 0) {
                            if (MVGravity1.getMeanValue() < 0)
                                angleXY = (float) Math.toDegrees(-Math.asin(MVGravity1.getMeanValue() / gravityXY));
                            else
                                angleXY = (float) Math.toDegrees(2 * Math.PI - Math.asin(MVGravity1.getMeanValue() / gravityXY));
                        } else
                            angleXY = (float) Math.toDegrees(Math.PI + Math.asin(MVGravity1.getMeanValue() / gravityXY));
                    }

                    angleXYZ = 0;
                    if (gravityXY > 0) {
                        angleXYZ = (float) Math.toDegrees(Math.acos(MVGravity2.getMeanValue() / gravityXYZ));
                    }

                    Log.d("SpiritLevel", "------------------------------------------------------------------");
                    Log.d("SpiritLevel", String.format("Auto Locking Tolerance = %1.4f", prefAutoLockTolerance));
                    Log.d("SpiritLevel", (String.format("Measurement locked - Angle0: Mean=%+1.4f Uncertainty=%+1.4f", MVAngle0.getMeanValue(), MVAngle0.getTolerance())));
                    Log.d("SpiritLevel", (String.format("Measurement locked - Angle1: Mean=%+1.4f Uncertainty=%+1.4f", MVAngle1.getMeanValue(), MVAngle1.getTolerance())));
                    Log.d("SpiritLevel", (String.format("Measurement locked - Angle2: Mean=%+1.4f Uncertainty=%+1.4f", MVAngle2.getMeanValue(), MVAngle2.getTolerance())));
                    Log.d("SpiritLevel", "------------------------------------------------------------------");

                    isLockRequested = false;
                    isLocked = true;
                    mClinometerView.invalidate();
                    UpdateLockIcon();
                    Beep();

                    MVAngle0.Reset();
                    MVAngle1.Reset();
                    MVAngle2.Reset();
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

                gravity_calibrated[0] = (float) (gravity[0] * CalibrationMatrix[0][0] + gravity[1] * CalibrationMatrix[0][1] + gravity[2] * CalibrationMatrix[0][2]);
                gravity_calibrated[1] = (float) (gravity[0] * CalibrationMatrix[1][0] + gravity[1] * CalibrationMatrix[1][1] + gravity[2] * CalibrationMatrix[1][2]);
                gravity_calibrated[2] = (float) (gravity[0] * CalibrationMatrix[2][0] + gravity[1] * CalibrationMatrix[2][1] + gravity[2] * CalibrationMatrix[2][2]);

                MVGravity0.LoadSample(gravity_calibrated[0]);
                MVGravity1.LoadSample(gravity_calibrated[1]);
                MVGravity2.LoadSample(gravity_calibrated[2]);

                gravityXY = (float) Math.sqrt(MVGravity0.getMeanValue() * MVGravity0.getMeanValue() + MVGravity1.getMeanValue() * MVGravity1.getMeanValue());   // Vector over the screen plane
                gravityXYZ = (float) Math.sqrt(gravityXY * gravityXY + MVGravity2.getMeanValue() * MVGravity2.getMeanValue());                                  // Spatial Vector

                // Calculate Angles

                angleXY = 0;
                if (gravityXY > 0) {
                    if (MVGravity0.getMeanValue() >= 0) {
                        if (MVGravity1.getMeanValue() < 0)
                            angleXY = (float) Math.toDegrees(-Math.asin(MVGravity1.getMeanValue() / gravityXY));
                        else
                            angleXY = (float) Math.toDegrees(2 * Math.PI - Math.asin(MVGravity1.getMeanValue() / gravityXY));
                    } else
                        angleXY = (float) Math.toDegrees(Math.PI + Math.asin(MVGravity1.getMeanValue() / gravityXY));
                }

                angleXYZ = 0;
                if (gravityXY > 0) {
                    angleXYZ = (float) Math.toDegrees(Math.acos(MVGravity2.getMeanValue() / gravityXYZ));
                }

                angle[0] = (float) (180 / Math.PI * Math.asin((MVGravity0.getMeanValue() / Math.max(gravityXYZ, 0.00001f))));
                angle[1] = (float) (180 / Math.PI * Math.asin((MVGravity1.getMeanValue() / Math.max(gravityXYZ, 0.00001f))));
                angle[2] = (float) (180 / Math.PI * Math.asin((MVGravity2.getMeanValue() / Math.max(gravityXYZ, 0.00001f))));

                // Load angles into Auto-Locking MeanVariances

                MVAngle0.LoadSample(angle[0]);
                MVAngle1.LoadSample(angle[1]);
                MVAngle2.LoadSample(angle[2]);

                // Determine Rotation, ViewMode and Text Angles

                if (Math.abs(angle[2]) < 70) {
                    if ((angleXY > 270 - 45 + ROTATION_THRESHOLD) && (angleXY < 270 + 45 - ROTATION_THRESHOLD) && (DisplayRotation != 0)) {
                        DisplayRotation = 0;
                        Log.w("SpiritLevel", " ROTATION = " + DisplayRotation);
                        RotateOverlays(DisplayRotation, this.getWindowManager().getDefaultDisplay().getHeight(), this.getWindowManager().getDefaultDisplay().getWidth());
                    }
                    if ((angleXY > 90 - 45 + ROTATION_THRESHOLD) && (angleXY < 90 + 45 - ROTATION_THRESHOLD) && (DisplayRotation != 180)) {
                        DisplayRotation = 180;
                        Log.w("SpiritLevel", " ROTATION = " + DisplayRotation);
                        RotateOverlays(DisplayRotation, this.getWindowManager().getDefaultDisplay().getHeight(), this.getWindowManager().getDefaultDisplay().getWidth());
                    }
                    if ((angleXY > 180 - 45 + ROTATION_THRESHOLD) && (angleXY < 180 + 45 - ROTATION_THRESHOLD) && (DisplayRotation != 270)) {
                        DisplayRotation = 270;
                        Log.w("SpiritLevel", " ROTATION = " + DisplayRotation);
                        RotateOverlays(DisplayRotation, this.getWindowManager().getDefaultDisplay().getWidth(), this.getWindowManager().getDefaultDisplay().getHeight());
                    }
                    if (((angleXY > 270 + 45 + ROTATION_THRESHOLD) || (angleXY < 45 - ROTATION_THRESHOLD)) && (DisplayRotation != 90)) {
                        DisplayRotation = 90;
                        Log.w("SpiritLevel", " ROTATION = " + DisplayRotation);
                        RotateOverlays(DisplayRotation, this.getWindowManager().getDefaultDisplay().getWidth(), this.getWindowManager().getDefaultDisplay().getHeight());
                    }
                }

                if (Math.abs(angle[2]) < 70) {
                    if (isFlat) isFlat = false;
                    angleTextLabels = (90 + angleXY) % 360;
                }
                if ((Math.abs(angle[2]) >= 70) && (Math.abs(angle[2]) < 75)) {
                    if ((DisplayRotation == 0) && (angleXY < 270)) {
                        //Log.d("SpiritLevel", "ANG");
                        angleTextLabels = DisplayRotation * (Math.abs(angle[2]) - 70) / 5
                                + (((90 + angleXY) % 360) - 360) * (75 - Math.abs(angle[2])) / 5;
                    } else {
                        angleTextLabels = DisplayRotation * (Math.abs(angle[2]) - 70) / 5
                                + ((90 + angleXY) % 360) * (75 - Math.abs(angle[2])) / 5;
                    }
                }
                if (Math.abs(angle[2]) >= 75) {
                    if (!isFlat) isFlat = true;
                    angleTextLabels = DisplayRotation;
                }

                // Apply Changes

                mClinometerView.invalidate();
            }

            // You must put this setText here in order to force the re-layout also during the rotations.
            // Without this, if you lock the measure during the rotation animation, the layout doesn't change correctly :(
            mTextViewAngles.setText(String.format("%1.1f°  %1.1f°  %1.1f°", angle[0], angle[1], angle[2]));
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    private void UpdateLockIcon() {
        if (isLocked) {
            mImageViewLock.setImageDrawable(getResources().getDrawable(R.drawable.ic_lock_white_24dp));
            mImageViewLock.setAlpha(0.8f);
        } else {
            if (isLockRequested) {
                mImageViewLock.setImageDrawable(getResources().getDrawable(R.drawable.ic_lock_open_white_24dp));
                mImageViewLock.setAlpha(0.8f);
            } else {
                mImageViewLock.setImageDrawable(getResources().getDrawable(R.drawable.ic_lock_open_white_24dp));
                mImageViewLock.setAlpha(0.4f);
            }
        }
    }


    private void LoadPreferences() {
        prefAutoLock = preferences.getBoolean("prefAutoLock", false);
        prefAutoLockHorizonCheck = preferences.getBoolean("prefAutoLockHorizonCheck", true);
        prefAutoLockTolerance = AUTOLOCK_MAX_TOLERANCE - (AUTOLOCK_MAX_TOLERANCE - AUTOLOCK_MIN_TOLERANCE) * preferences.getInt("prefAutoLockPrecision", 500) / 1000;
        Log.d("SpiritLevel", String.format("Auto Locking Tolerance = %1.3f", prefAutoLockTolerance));

        angle_calibration[0]    = preferences.getFloat("prefCalibrationAngle0", 0);
        angle_calibration[1]    = preferences.getFloat("prefCalibrationAngle1", 0);
        angle_calibration[2]    = preferences.getFloat("prefCalibrationAngle2", 0);
        gravity_gain[0]         = preferences.getFloat("prefCalibrationGain0", 1);
        gravity_gain[1]         = preferences.getFloat("prefCalibrationGain1", 1);
        gravity_gain[2]         = preferences.getFloat("prefCalibrationGain2", 1);
        gravity_offset[0]       = preferences.getFloat("prefCalibrationOffset0", 0);
        gravity_offset[1]       = preferences.getFloat("prefCalibrationOffset1", 0);
        gravity_offset[2]       = preferences.getFloat("prefCalibrationOffset2", 0);

        CalibrationMatrix[0][0] = (float) (Math.cos(Math.toRadians(angle_calibration[2])) * Math.cos(Math.toRadians(angle_calibration[0])) + Math.sin(Math.toRadians(angle_calibration[2])) * Math.sin(Math.toRadians(angle_calibration[1])) * Math.sin(Math.toRadians(angle_calibration[0])));
        CalibrationMatrix[0][1] = (float) (Math.cos(Math.toRadians(angle_calibration[1])) * Math.sin(Math.toRadians(angle_calibration[0])));
        CalibrationMatrix[0][2] = (float) (-Math.sin(Math.toRadians(angle_calibration[2])) * Math.cos(Math.toRadians(angle_calibration[0])) + Math.cos(Math.toRadians(angle_calibration[2])) * Math.sin(Math.toRadians(angle_calibration[1])) * Math.sin(Math.toRadians(angle_calibration[0])));

        CalibrationMatrix[1][0] = (float) (-Math.cos(Math.toRadians(angle_calibration[2])) * Math.sin(Math.toRadians(angle_calibration[0])) + Math.sin(Math.toRadians(angle_calibration[2])) * Math.sin(Math.toRadians(angle_calibration[1])) * Math.cos(Math.toRadians(angle_calibration[0])));
        CalibrationMatrix[1][1] = (float) (Math.cos(Math.toRadians(angle_calibration[1])) * Math.cos(Math.toRadians(angle_calibration[0])));
        CalibrationMatrix[1][2] = (float) (Math.sin(Math.toRadians(angle_calibration[2])) * Math.sin(Math.toRadians(angle_calibration[0])) + Math.cos(Math.toRadians(angle_calibration[2])) * Math.sin(Math.toRadians(angle_calibration[1])) * Math.cos(Math.toRadians(angle_calibration[0])));

        CalibrationMatrix[2][0] = (float) (Math.sin(Math.toRadians(angle_calibration[2])) * Math.cos(Math.toRadians(angle_calibration[1])));
        CalibrationMatrix[2][1] = (float) (-Math.sin(Math.toRadians(angle_calibration[1])));
        CalibrationMatrix[2][2] = (float) (Math.cos(Math.toRadians(angle_calibration[2])) * Math.cos(Math.toRadians(angle_calibration[1])));
    }


    private void Beep() {
        //toneGen1.startTone(ToneGenerator.TONE_SUP_PIP,150);
        vibrator.vibrate(250);
        toneGen1.startTone(ToneGenerator.TONE_PROP_BEEP, 100);
    }


    private void RotateOverlays(final float rotationAngle, final int newHeight, final int newWidth) {
        if (animationR.isRunning()) animationR.cancel();

        animationR = ValueAnimator.ofFloat(0, 1);
        animationR.setDuration(700);
        animationR.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator updatedAnimation) {
                float animatedValue = (float) Math.sqrt(Math.sqrt((float) updatedAnimation.getAnimatedValue()));
                float alpha = 2 * Math.abs(animatedValue - 0.5f);           // Transparency for Fades
                //Log.d("SpiritLevel", "alpha = " + alpha);
                mLinearLayoutOverlays.setAlpha(alpha);
                if ((animatedValue >= 0.5f) && (mLinearLayoutOverlays.getRotation() != rotationAngle)) {
                    //Log.d("SpiritLevel", "change parameters = " + rotationAngle + ", " + newWidth + ", " + newHeight);
                    mLinearLayoutOverlays.getLayoutParams().height = newHeight;
                    mLinearLayoutOverlays.getLayoutParams().width = newWidth;
                    mLinearLayoutOverlays.setRotation(rotationAngle);
                }
            }
        });
        animationR.start();
    }
}
