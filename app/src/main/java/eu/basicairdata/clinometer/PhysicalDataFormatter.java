/*
 * PhysicalDataFormatter - Java Class for Android
 * Created by G.Capelli on 18/9/2021
 * This file is part of BasicAirData Clinometer
 *
 * Copyright (C) 2011 BasicAirData
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

import java.util.Locale;

/**
 * A helper Class for the formatting of the physical data.
 * It returns the data formatted basing on the given criteria and on the Preferences.
 */
class PhysicalDataFormatter {

    private static final int UM_METRIC_MS       = 0;
    private static final int UM_METRIC_KMH      = 1;
    private static final int UM_IMPERIAL_FPS    = 8;
    private static final int UM_IMPERIAL_MPH    = 9;
    private static final int UM_NAUTICAL_KN     = 16;
    private static final int UM_NAUTICAL_MPH    = 17;

    private static final float M_TO_FT   = 3.280839895f;
    private static final float M_TO_NM   = 0.000539957f;
    private static final float MS_TO_MPH = 2.2369363f;
    private static final float MS_TO_KMH = 3.6f;
    private static final float MS_TO_KN  = 1.943844491f;
    private static final float KM_TO_MI  = 0.621371192237f;

    private final ClinometerApplication clinometerApp = ClinometerApplication.getInstance();

    /**
     * It returns a PhysicalData formatted basing on the given criteria and on the Preferences.
     *
     * @param number The float number to format as Physical Data
     * @return The Physical Data containing number and unit of measurement
     */
    public PhysicalData format(float number) {
        PhysicalData physicalData = new PhysicalData();
        physicalData.value = "";
        physicalData.um = "";

        physicalData.value = String.format(Locale.getDefault(), "%.1f", number);
        physicalData.um = "°";
//                switch (clinometerApp.getPrefUM()) {
//                    case UM_METRIC_KMH:
//                        physicalData.value = String.valueOf(Math.round(number * MS_TO_KMH));
//                        physicalData.um = clinometerApp.getString(R.string.UM_km_h);
//                        return (physicalData);
//                    case UM_METRIC_MS:
//                        physicalData.value = String.valueOf(Math.round(number));
//                        physicalData.um = clinometerApp.getString(R.string.UM_m_s);
//                        return (physicalData);
//                    case UM_IMPERIAL_MPH:
//                    case UM_NAUTICAL_MPH:
//                        physicalData.value = String.valueOf(Math.round(number * MS_TO_MPH));
//                        physicalData.um = clinometerApp.getString(R.string.UM_mph);
//                        return (physicalData);
//                    case UM_IMPERIAL_FPS:
//                        physicalData.value = String.valueOf(Math.round(number * M_TO_FT));
//                        physicalData.um = clinometerApp.getString(R.string.UM_fps);
//                        return (physicalData);
//                    case UM_NAUTICAL_KN:
//                        physicalData.value = String.valueOf(Math.round(number * MS_TO_KN));
//                        physicalData.um = clinometerApp.getString(R.string.UM_kn);
//                        return (physicalData);
//                }
        return (physicalData);
    }
}