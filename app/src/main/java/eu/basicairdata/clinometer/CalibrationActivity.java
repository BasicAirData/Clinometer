/*
 * CalibrationActivity - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 3/6/2020
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

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.preference.PreferenceManager;


public class CalibrationActivity extends AppCompatActivity implements SensorEventListener {

    private Vibrator vibrator;

    private final static int ACCELEROMETER_UPDATE_INTERVAL_MICROS = 10000;
    private final static float MIN_CALIBRATION_PRECISION = 0.05f;
    private final static int SIZE_OF_MEANVARIANCE = 300;                    // 4 seconds


    MeanVariance mvGravity0 = new MeanVariance(SIZE_OF_MEANVARIANCE);
    MeanVariance mvGravity1 = new MeanVariance(SIZE_OF_MEANVARIANCE);
    MeanVariance mvGravity2 = new MeanVariance(SIZE_OF_MEANVARIANCE);

    private final float[][] mean = new float[3][7];              // The Mean values of vectors

    private final float[] calibrationOffset = new float[3];      // The Offsets of accelerometers
    private final float[] calibrationGain = new float[3];        // The Gains of accelerometers
    private final float[] calibrationAngle = new float[3];       // The calibration angles

    private AppCompatButton buttonNext;
    private ProgressBar progressBar;
    private ImageView imageViewMain;
    private TextView textViewStepDescription;
    private TextView textViewLastCalibration;
    private TextView textViewProgress;

    private int currentStep = 0;                // The current step of the wizard;

    private static final int STEP_1         = 0;    // Step 1 of 7      Lay flat and press next
    private static final int STEP_1_CAL     = 1;    // Calibrating...   Don't move the device
    private static final int STEP_2         = 2;    // Step 2 of 7      Rotate 180° and press next
    private static final int STEP_2_CAL     = 3;    // Calibrating...   Don't move the device
    private static final int STEP_3         = 4;    // Step 3 of 7      Lay on the left side and press next
    private static final int STEP_3_CAL     = 5;    // Calibrating...   Don't move the device
    private static final int STEP_4         = 6;    // Step 4 of 7      Rotate 180° and press next
    private static final int STEP_4_CAL     = 7;    // Calibrating...   Don't move the device
    private static final int STEP_5         = 8;    // Step 5 of 7      Lay vertical and press next
    private static final int STEP_5_CAL     = 9;    // Calibrating...   Don't move the device
    private static final int STEP_6         = 10;   // Step 6 of 7      Rotate 180° upside-down and press next
    private static final int STEP_6_CAL     = 11;   // Calibrating...   Don't move the device
    private static final int STEP_7         = 12;   // Step 7 of 7      Press next and lay face down
    private static final int STEP_7_CAL     = 13;   // Calibrating...   Don't move the device
    private static final int STEP_COMPLETED = 14;   // Calibration completed (Message Box)

    private static final float STANDARD_GRAVITY = 9.807f;

    private static final int DISCARD_FIRST_SAMPLES = 20;
    private int samplesDiscarded = 0;

    ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE);

    private SensorManager mSensorManager;
    private Sensor mRotationSensor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        setContentView(R.layout.activity_calibration);

        buttonNext = findViewById(R.id.id_button_next);
        progressBar = findViewById(R.id.id_progressBar);
        textViewStepDescription = findViewById(R.id.id_textview_step_description);
        textViewLastCalibration = findViewById(R.id.id_textview_last_calibration);
        textViewProgress = findViewById(R.id.id_textview_progress);
        imageViewMain = findViewById(R.id.id_imageViewMain);

        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentStep++;
                startStep();
            }
        });

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (mRotationSensor == null) Log.d("Clinometer", "NO ACCELEROMETER FOUND!");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (preferences.contains("prefCalibrationAngle0")) {
            textViewLastCalibration.setText(getString(R.string.calibration_active_calibration) + "\n"
                    + getString(R.string.calibration_active_calibration_gains)
                    + String.format(" = %1.3f; %1.3f; %1.3f", preferences.getFloat("prefCalibrationGain0", 0), preferences.getFloat("prefCalibrationGain1", 0), preferences.getFloat("prefCalibrationGain2", 0))
                    + "\n"
                    + getString(R.string.calibration_active_calibration_offsets)
                    + String.format(" = %1.3f; %1.3f; %1.3f", preferences.getFloat("prefCalibrationOffset0", 0), preferences.getFloat("prefCalibrationOffset1", 0), preferences.getFloat("prefCalibrationOffset2", 0))
                    + "\n"
                    + getString(R.string.calibration_active_calibration_angles)
                    + String.format(" = %1.2f°; %1.2f°; %1.2f°", preferences.getFloat("prefCalibrationAngle0", 0), preferences.getFloat("prefCalibrationAngle1", 0), preferences.getFloat("prefCalibrationAngle2", 0))
            );
        }

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onBackPressed()
    {
        Log.d("CalibrationActivity", "onBackPressed on Step " + currentStep);
        if ((currentStep != STEP_1) && (currentStep != STEP_COMPLETED)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getResources().getString(R.string.calibration_abort));
            //builder.setIcon(android.R.drawable.ic_menu_info_details);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    finish();
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            finish();
            //super.onBackPressed();  // optional depending on your needs
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if ((int) (currentStep / 2) * 2 != currentStep) currentStep--;
        Log.d("CalibrationActivity", "CurrentStep = " + currentStep);

        startStep();

        //mSensorManager.registerListener(this, mRotationSensor, ACCELEROMETER_UPDATE_INTERVAL_MICROS);
    }


    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }


    private void startStep() {
        progressBar.setProgress(0);
        progressBar.setSecondaryProgress(0);
        textViewProgress.setText("");
        textViewLastCalibration.setVisibility(View.INVISIBLE);

        switch (currentStep) {
            case STEP_1:
                textViewLastCalibration.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
                buttonNext.setVisibility(View.VISIBLE);
                textViewProgress.setVisibility(View.INVISIBLE);
                imageViewMain.setImageResource(R.mipmap.cal_01);
                textViewStepDescription.setText(R.string.calibration_step1);
                break;
            case STEP_2:
                progressBar.setVisibility(View.INVISIBLE);
                buttonNext.setVisibility(View.VISIBLE);
                textViewProgress.setVisibility(View.INVISIBLE);
                imageViewMain.setImageResource(R.mipmap.cal_02);
                textViewStepDescription.setText(R.string.calibration_step2);
                break;
            case STEP_3:
                progressBar.setVisibility(View.INVISIBLE);
                buttonNext.setVisibility(View.VISIBLE);
                textViewProgress.setVisibility(View.INVISIBLE);
                imageViewMain.setImageResource(R.mipmap.cal_03);
                textViewStepDescription.setText(R.string.calibration_step3);
                break;
            case STEP_4:
                progressBar.setVisibility(View.INVISIBLE);
                buttonNext.setVisibility(View.VISIBLE);
                textViewProgress.setVisibility(View.INVISIBLE);
                imageViewMain.setImageResource(R.mipmap.cal_04);
                textViewStepDescription.setText(R.string.calibration_step4);
                break;
            case STEP_5:
                progressBar.setVisibility(View.INVISIBLE);
                buttonNext.setVisibility(View.VISIBLE);
                textViewProgress.setVisibility(View.INVISIBLE);
                imageViewMain.setImageResource(R.mipmap.cal_05);
                textViewStepDescription.setText(R.string.calibration_step5);
                break;
            case STEP_6:
                progressBar.setVisibility(View.INVISIBLE);
                buttonNext.setVisibility(View.VISIBLE);
                textViewProgress.setVisibility(View.INVISIBLE);
                imageViewMain.setImageResource(R.mipmap.cal_06);
                textViewStepDescription.setText(R.string.calibration_step6);
                break;
            case STEP_7:
                progressBar.setVisibility(View.INVISIBLE);
                buttonNext.setVisibility(View.VISIBLE);
                textViewProgress.setVisibility(View.INVISIBLE);
                imageViewMain.setImageResource(R.mipmap.cal_07);
                textViewStepDescription.setText(R.string.calibration_step7);
                break;
            case STEP_1_CAL:
            case STEP_2_CAL:
            case STEP_3_CAL:
            case STEP_4_CAL:
            case STEP_5_CAL:
            case STEP_6_CAL:
            case STEP_7_CAL:
                progressBar.setVisibility(View.VISIBLE);
                buttonNext.setVisibility(View.INVISIBLE);
                textViewProgress.setVisibility(View.VISIBLE);
                textViewStepDescription.setText(R.string.calibration_calibrating);
                mvGravity0.reset();
                mvGravity1.reset();
                mvGravity2.reset();
                samplesDiscarded = 0;
                mSensorManager.registerListener(this, mRotationSensor, ACCELEROMETER_UPDATE_INTERVAL_MICROS);
                break;
            case STEP_COMPLETED:
                // Calculations
                Log.d("Clinometer","-- MEAN NOT CORRECTED ------------------------------------------------------");
                for (int i = 0; i < 7; i++) {
                    Log.d("Clinometer", String.format("mean[ ][" + i + "]  =  %+1.4f  %+1.4f  %+1.4f", mean[0][i], mean[1][i], mean[2][i]));
                }

                // Calibration offset and Gain (https://www.digikey.it/it/articles/using-an-accelerometer-for-inclination-sensing)

                calibrationOffset[0] = (mean[0][2] + mean[0][3]) / 2;
                calibrationOffset[1] = (mean[1][4] + mean[1][5]) / 2;
                calibrationOffset[2] = (mean[2][0] + mean[2][6]) / 2;

                calibrationGain[0] = (mean[0][2] - mean[0][3]) / (STANDARD_GRAVITY * 2);
                calibrationGain[1] = (mean[1][4] - mean[1][5]) / (STANDARD_GRAVITY * 2);
                calibrationGain[2] = (mean[2][0] - mean[2][6]) / (STANDARD_GRAVITY * 2);

                // Estimation of the third axis
//                CalibrationGain[2] = (CalibrationGain[0] + CalibrationGain[1]) / 2;
//                CalibrationOffset[2] = (Mean[2][0] + Mean[2][0]) / 2 - (CalibrationGain[2] * STANDARD_GRAVITY);

                Log.d("Clinometer","-- ACCELEROMETERS ----------------------------------------------------------");
                Log.d("Clinometer", String.format("Offset  =  %+1.4f  %+1.4f  %+1.4f", calibrationOffset[0], calibrationOffset[1], calibrationOffset[2]));
                Log.d("Clinometer", String.format("Gain    =  %+1.4f  %+1.4f  %+1.4f", calibrationGain[0], calibrationGain[1], calibrationGain[2]));

                // Apply the Gain and Offset Correction to measurement

                for (int i = 0; i < 7; i++) {
                    mean[0][i] = (mean[0][i] - calibrationOffset[0]) / calibrationGain[0];
                    mean[1][i] = (mean[1][i] - calibrationOffset[1]) / calibrationGain[1];
                    mean[2][i] = (mean[2][i] - calibrationOffset[2]) / calibrationGain[2];
                }

                Log.d("Clinometer","-- MEAN CORRECTED ----------------------------------------------------------");
                for (int i = 0; i < 7; i++) {
                    Log.d("Clinometer", String.format("mean[ ][" + i + "]  =  %+1.4f  %+1.4f  %+1.4f", mean[0][i], mean[1][i], mean[2][i]));
                }

                // Calculation of Angles

                float[][] angle = new float[3][7];

                Log.d("Clinometer","-- ANGLES ------------------------------------------------------------------");
                for (int i = 0; i < 7; i++) {
                    angle[0][i] = (float) (Math.toDegrees(Math.asin(mean[0][i]
                            / Math.sqrt(mean[0][i] * mean[0][i] + mean[1][i] * mean[1][i] + mean[2][i] * mean[2][i]))));
                    angle[1][i] = (float) (Math.toDegrees(Math.asin(mean[1][i]
                            / Math.sqrt(mean[0][i] * mean[0][i] + mean[1][i] * mean[1][i] + mean[2][i] * mean[2][i]))));
                    angle[2][i] = (float) (Math.toDegrees(Math.asin(mean[2][i]
                            / Math.sqrt(mean[0][i] * mean[0][i] + mean[1][i] * mean[1][i] + mean[2][i] * mean[2][i]))));
                    Log.d("Clinometer", String.format("angle[ ][" + i + "] =  %+1.4f°  %+1.4f°  %+1.4f°", angle[0][i], angle[1][i], angle[2][i]));
                }

                calibrationAngle[2] =  (angle[0][0] + angle[0][1])/2;       // angle 0 = X axis
                calibrationAngle[1] = -(angle[1][0] + angle[1][1])/2;       // angle 1 = Y axis
                calibrationAngle[0] = -(angle[1][3] + angle[1][2])/2;       // angle 2 = Z axis

                Log.d("Clinometer","-- CALIBRATION ANGLES ------------------------------------------------------");
                Log.d("Clinometer", String.format("Cal.Angles =  %+1.4f°  %+1.4f°  %+1.4f°", calibrationAngle[0], calibrationAngle[1], calibrationAngle[2]));

                Log.d("Clinometer","----------------------------------------------------------------------------");

                // Write Calibration Angles into Preferences
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = preferences.edit();
                editor.putFloat("prefCalibrationAngle0", calibrationAngle[0]);
                editor.putFloat("prefCalibrationAngle1", calibrationAngle[1]);
                editor.putFloat("prefCalibrationAngle2", calibrationAngle[2]);
                editor.putFloat("prefCalibrationGain0", calibrationGain[0]);
                editor.putFloat("prefCalibrationGain1", calibrationGain[1]);
                editor.putFloat("prefCalibrationGain2", calibrationGain[2]);
                editor.putFloat("prefCalibrationOffset0", calibrationOffset[0]);
                editor.putFloat("prefCalibrationOffset1", calibrationOffset[1]);
                editor.putFloat("prefCalibrationOffset2", calibrationOffset[2]);
                editor.putLong("prefCalibrationTime", System.currentTimeMillis());
                editor.commit();

                finish();
        }
    }


    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if ((int) (currentStep / 2) * 2 == currentStep) {
                // Stop Calibration
                mSensorManager.unregisterListener(this);
            } else {

                if (samplesDiscarded < DISCARD_FIRST_SAMPLES) {
                    samplesDiscarded++;
                    return;
                }

                // Calibration
                //Log.d("CalibrationActivity", "CALIBRATION");

                mvGravity0.loadSample(event.values[0]);
                mvGravity1.loadSample(event.values[1]);
                mvGravity2.loadSample(event.values[2]);

                textViewProgress.setText(String.format("Progress %1.0f%%   Tolerance %1.3f", mvGravity0.percentLoaded(), mvGravity0.getTolerance()));
                int progress1 = (int) (10 * mvGravity0.percentLoaded());
                int progress2 = (int) (Math.min(1000, Math.max(0, 1000 - 1000 *(mvGravity0.getTolerance() / MIN_CALIBRATION_PRECISION))));
                progressBar.setSecondaryProgress(Math.max(progress1, progress2));
                progressBar.setProgress(Math.min(progress1, progress2));


                // DEVICE MOVED

                if (mvGravity0.isReady() && (mvGravity0.getTolerance() > MIN_CALIBRATION_PRECISION)) {
                    mvGravity0.reset();
                    mvGravity1.reset();
                    mvGravity2.reset();
                }

                // END OF CALIBRATION STEP

                if (mvGravity0.percentLoaded() == 100) {
                    mSensorManager.unregisterListener(this);

                    int i = (int) (currentStep / 2);

                    mean[0][i] = mvGravity0.getMeanValue(SIZE_OF_MEANVARIANCE-100);
                    mean[1][i] = mvGravity1.getMeanValue(SIZE_OF_MEANVARIANCE-100);
                    mean[2][i] = mvGravity2.getMeanValue(SIZE_OF_MEANVARIANCE-100);

                    beep();

                    currentStep++;
                    startStep();
                }
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    private void beep() {
        //toneGen1.startTone(ToneGenerator.TONE_SUP_PIP,150);
        vibrator.vibrate(250);
        toneGen1.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
    }
}
