/*
 * CameraInformation - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 8/1/2021
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

public class CameraInformation {

    public static final int REAR_CAMERA = 0;
    public static final int FRONT_CAMERA = 1;
    public static final int EXTERNAL_CAMERA = 2;

    public int id = -1;
    public int type = -1;
    public String description = "";
    public int minExposureCompensation = 0;
    public int maxExposureCompensation = 0;
    public float horizontalViewAngle = 0;
}
