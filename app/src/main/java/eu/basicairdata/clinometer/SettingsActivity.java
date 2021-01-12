/*
 * SettingsActivity - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 2/6/2020
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
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static eu.basicairdata.clinometer.ClinometerApplication.CAMERA_REQUEST_CODE;


public class SettingsActivity extends AppCompatActivity {

    private ClinometerApplication clinometerApplication;
    SettingsFragment settingsFragment = new SettingsFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        clinometerApplication = ClinometerApplication.getInstance();

        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        setContentView(R.layout.activity_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.framelayout_settings, settingsFragment)
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            Log.w("SettingsActivity", "onRequestPermissionsResult()");
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                clinometerApplication.scanCameras();
                settingsFragment.updatePreferences();
            }
        }
    }


    public static class SettingsFragment extends PreferenceFragmentCompat {

        private ClinometerApplication clinometerApplication;

        SharedPreferences preferences;
        SharedPreferences.OnSharedPreferenceChangeListener listener;

        ListPreference preferenceCameraToUse;
        SeekBarPreference preferenceExposureCompensation;
        Preference preferenceCalibration;
        Preference preferenceAbout;
        Preference preferenceCameraPermission;
        Preference preferenceResetCalibration;


        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            clinometerApplication = ClinometerApplication.getInstance();
            preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

            listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    Log.d("SettingsActivity", "onSharedPreferenceChanged");
                    if (key.equals("prefCamera")) {
                        clinometerApplication.setSelectedCamera(Integer.parseInt(preferenceCameraToUse.getValue()));
                        setupCameraPreference();
                        setupCompensationPreference();
                    }
                }
            };

            preferenceCameraToUse = findPreference("prefCamera");

            preferenceCameraPermission = findPreference("prefCameraPermission");
            preferenceCameraPermission.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
                    } else {
                        setupCameraPreference();
                        setupCompensationPreference();
                    }
                    return false;
                }
            });

            preferenceExposureCompensation = findPreference("prefExposureCompensation");
            preferenceCalibration = findPreference("prefCalibration");

            preferenceAbout = findPreference("prefAbout");
            preferenceAbout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    FragmentManager fm = getActivity().getSupportFragmentManager();
                    FragmentAboutDialog aboutDialog = new FragmentAboutDialog();

                    aboutDialog.show(fm, "");
                    return false;
                }
            });

            preferenceResetCalibration = findPreference("prefResetCalibration");
            preferenceResetCalibration.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (preferences.contains("prefCalibrationAngle0")) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setMessage(getResources().getString(R.string.reset_calibration_confirmation));
                        builder.setIcon(android.R.drawable.ic_menu_info_details);
                        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.remove("prefCalibrationAngle0");
                                editor.remove("prefCalibrationAngle1");
                                editor.remove("prefCalibrationAngle2");
                                editor.remove("prefCalibrationGain0");
                                editor.remove("prefCalibrationGain1");
                                editor.remove("prefCalibrationGain2");
                                editor.remove("prefCalibrationOffset0");
                                editor.remove("prefCalibrationOffset1");
                                editor.remove("prefCalibrationOffset2");
                                editor.remove("prefCalibrationTime");
                                editor.commit();

                                updatePreferences();
                            }
                        });
                        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                    return false;
                }
            });
        }


        @Override
        public void onPause() {
            super.onPause();
            preferences.unregisterOnSharedPreferenceChangeListener(listener);
        }

        @Override
        public void onResume() {
            super.onResume();
            preferences.registerOnSharedPreferenceChangeListener(listener);
            updatePreferences();
        }


        private void updatePreferences() {
            SimpleDateFormat dfdt = new SimpleDateFormat("dd LLL yyyy HH:mm");        // date and time formatter for timestamp
            //dfdtGPX.setTimeZone(TimeZone.getTimeZone("GMT"));

            if (preferences.contains("prefCalibrationAngle0")) {
                preferenceCalibration.setSummary(getResources().getString(R.string.pref_calibration_summary_calibrated) + " ("
                                + dfdt.format(preferences.getLong("prefCalibrationTime", 0)) + ")");
//                preferenceCalibration.setSummary(getResources().getString(R.string.pref_calibration_summary_calibrated)
//                        + String.format(" (%1.2f°; %1.2f°; %1.2f°)", preferences.getFloat("prefCalibrationAngle0", 0),
//                        preferences.getFloat("prefCalibrationAngle1", 0), preferences.getFloat("prefCalibrationAngle2", 0)));
                preferenceResetCalibration.setEnabled(true);
            } else {
                preferenceCalibration.setSummary(getResources().getString(R.string.pref_calibration_summary_notcalibrated));
                preferenceResetCalibration.setEnabled(false);
            }
            setupCameraPreference();
            setupCompensationPreference();
        }


        private void setupCameraPreference() {
            preferenceCameraToUse.setVisible(true);
            preferenceCameraPermission.setVisible(false);
            preferenceExposureCompensation.setVisible(true);

            if (!clinometerApplication.hasCamera()) {
                // The Device has NO Cameras
                preferenceCameraToUse.setEnabled(false);
                preferenceCameraToUse.setSummary(getString(R.string.pref_cameramode_no_cameras));
                preferenceExposureCompensation.setEnabled(false);
                preferenceExposureCompensation.setVisible(false);
            } else {
                if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    // Camera present
                    ArrayList<CameraInformation> ci = clinometerApplication.getListOfCameraInformation();
                    if (ci.isEmpty()) {
                        // The Device has NO Cameras
                        preferenceCameraToUse.setEnabled(false);
                        preferenceCameraToUse.setSummary(getString(R.string.pref_cameramode_no_cameras));
                        preferenceExposureCompensation.setEnabled(false);
                        return;
                    }
                    List<String> ci_entries = new ArrayList<>();
                    List<String> ci_values = new ArrayList<>();
                    for (CameraInformation c : ci) {
                        ci_entries.add(c.description + " (" + c.horizontalViewAngle + "°)");
                        ci_values.add(String.valueOf(c.id));
                    }

                    preferenceCameraToUse.setEntries(ci_entries.toArray(new CharSequence[ci_entries.size()]));
                    preferenceCameraToUse.setEntryValues(ci_values.toArray(new CharSequence[ci_values.size()]));
                    preferenceCameraToUse.setEnabled(true);
                } else {
                    // Camera present, but no camera permission granted
                    preferenceCameraToUse.setVisible(false);
                    preferenceCameraPermission.setVisible(true);
                    preferenceExposureCompensation.setVisible(false);
                }
            }
        }


        private void setupCompensationPreference() {
            if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                ArrayList<CameraInformation> ci = clinometerApplication.getListOfCameraInformation();

                if (!ci.isEmpty()) {
                    CameraInformation selCameraInformation = ci.get(Integer.parseInt(preferenceCameraToUse.getValue()));
                    int compensationRange = selCameraInformation.maxExposureCompensation - selCameraInformation.minExposureCompensation;
                    int compensation = preferenceExposureCompensation.getValue();

                    Log.d("SettingsActivity", "Selected Camera " + selCameraInformation.id + " (compensation Range = " + compensationRange + ")");

                    if (compensationRange > 0) {
                        // Exposure Compensation feature is present
                        preferenceExposureCompensation.setEnabled(true);

                        preferenceExposureCompensation.setMin(selCameraInformation.minExposureCompensation);
                        preferenceExposureCompensation.setMax(selCameraInformation.maxExposureCompensation);

                        // Check if the Exposure Compensation set is out of range. In case modify it
                        if (compensation > selCameraInformation.maxExposureCompensation)
                            preferenceExposureCompensation.setValue(selCameraInformation.maxExposureCompensation);
                        if (compensation < selCameraInformation.minExposureCompensation)
                            preferenceExposureCompensation.setValue(selCameraInformation.minExposureCompensation);
                        if (compensation != preferenceExposureCompensation.getValue()) {
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putInt("prefExposureCompensation", preferenceExposureCompensation.getValue());
                            editor.commit();
                        }
                    } else {
                        preferenceExposureCompensation.setMin(-1);
                        preferenceExposureCompensation.setMax(1);
                        preferenceExposureCompensation.setValue(0);
                        preferenceExposureCompensation.setEnabled(false);
                    }
                }
            }
        }
    }
}