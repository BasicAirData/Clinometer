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

import java.util.Timer;
import java.util.TimerTask;

public class PIDAnimator extends TimerTask {

    Handler handler = new Handler();

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

    Timer mTimer = new Timer();

    public PIDAnimator(float initialValue, float Kp, float Ki, float Kd, long t_millis) {
        u = initialValue;
        y = initialValue;
        r = initialValue;
        t_ms = t_millis;
        t = t_ms / 1000.0f;

        kp = Kp;
        kd = Kd;
        ki = Ki;

        mTimer.scheduleAtFixedRate(this, 0, t_millis);
    }


    public void run() {
        handler.post(new Runnable() {
             @Override
             public void run() {
                 //Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

                 calculate();

                  // DEBUG LOG
//                long now = System.currentTimeMillis();
//                if (now - prevtime > 100) {
//                    Log.i("PIDAnimator", "UPDATE: u = " + u + " y = " + y + " r = " + r + " P = " + P + " I = " + I + " D = " + D);
//                    prevtime = now;
//                }
             }
        });
    }

    /**
     * Makes a step of the discrete time PID.
     */
    private void calculate() {
        P = kp * (r - y);                               // Proportional Action
        D = kd * (y - y_old) / t;                       // Derivative Action

        v = P + I + D;
        u = Math.min(3600.0f, Math.max(v, -3600.0f));   // Simulate the saturation of the sensor
        y_old = y;
        y = y + 0.3f *u;

        I = I + ki * (r - y) * t + kt * (u - v) * t;    // Integral Action
    }

    /**
     * Changes the final value of the Animation to a new value.
     * @param setPoint The new set Point
     */
    public void setTargetValue(float setPoint) {
        r = setPoint;
    }

    /**
     * Returns the current value of the Animation.
     * @return The current value
     */
    public float getValue() {
        return y;
    }
}
