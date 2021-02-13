/*
 * PIDAnimator - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 13/2/2021
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

import android.os.Handler;

public class PIDAnimator extends Thread {

    float r = 0;            // Setpoint
    float y = 0;
    float y_old = 0;
    float u = 0;            // Output Value
    float v = 0;
    float e = 0;        // Error

    float t;
    long t_ms;

    float kp;
    float ki;
    float kd;
    float kt = 0.3f;    // Desaturation gain

    float P = 0;
    float I = 0;
    float D = 0;

    public PIDAnimator(float initialValue, float Kp, float Ki, float Kd, long t_millis) {
        u = initialValue;
        y = initialValue;
        r = initialValue;
        t_ms = t_millis;
        t = t_ms / 1000.0f;

        kp = Kp;
        kd = Kd;
        ki = Ki;

        P = kp * (r - y);
        D = kd * (y - y_old) / t;

        v = P + I + D;
        u = Math.min(3600.0f, Math.max(v, -3600.0f));
        y_old = y;
        y = y + 0.3f *u;

        I = I + ki * (r - y) * t + kt * (u - v) * t;

        start();
    }

    public void run() {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        //handler.postDelayed(periodicUpdate, t - SystemClock.elapsedRealtime()%1000);
        handler.postDelayed(periodicUpdate, t_ms);
    }


    public void setTargetValue(float setPoint) {
        r = setPoint;
    }


    public float getValue() {
        return y;
    }


    Handler handler = new Handler();
    private final Runnable periodicUpdate = new Runnable() {
        @Override
        public void run() {
            //handler.postDelayed(periodicUpdate, t - SystemClock.elapsedRealtime()%1000);
            handler.postDelayed(periodicUpdate, t_ms);

            P = kp * (r - y);
            D = kd * (y - y_old) / t;

            v = P + I + D;
            u = Math.min(3600.0f, Math.max(v, -3600.0f));
            y_old = y;
            y = y + 0.3f *u;

            I = I + ki * (r - y) * t + kt * (u - v) * t;

            // DEBUG LOG
//            long now = System.currentTimeMillis();
//            if (now - prevtime > 100) {
//                Log.i("PIDAnimator", "UPDATE: u = " + u + " y = " + y + " r = " + r + " P = " + P + " I = " + I + " D = " + D);
//                prevtime = now;
//            }
        }
    };
}
