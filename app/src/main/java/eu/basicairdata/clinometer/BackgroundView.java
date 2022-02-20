/*
 * BackgroundView - Java Class for Android
 * Created by G.Capelli on 7/1/2021
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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class BackgroundView extends View {

    private static final float N_CIRCLES_FULLY_VISIBLE = 4.5f;

    private Paint paint_bg;                 // For Background Gradient
    private Paint paint_DKGray;             // For Background Lines != 30°
    private boolean isShaderCreated = false;                    // True if the Background Shader has been created

    private final ClinometerActivity clinometerActivity = ClinometerActivity.getInstance();
    private final ClinometerApplication clinometerApplication = ClinometerApplication.getInstance();

    private float refbgAxis = 0;             // The reference axis for ref Angles

    private int x;                      // The Width of Screen
    private int y;                      // The Height of Screen
    private int min_xy;                 // The minimum between Width and Height
    private int max_xy;                 // The maximum between Width and Height
    private int xc;                     // x screen center
    private int yc;                     // y screen center
    private double diag2c;              // Screen Diagonal/2 = distance between 0:0 and xc:yc
    private int ncircles;               // The number of visible circles
    private float r1_value;             // The scale (to how many degrees corresponds each circle)
    private float r1;                   // The radius of the first circle = 1 deg.

    private int angle;


    public BackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        createPaints();

    }


    public BackgroundView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        createPaints();

    }


    public BackgroundView(Context context) {
        super(context);
        createPaints();

    }


    private void createPaints() {
        // create the Paint and set its color

        paint_DKGray = new Paint();
        paint_DKGray.setColor(getResources().getColor(R.color.line_dark));
        paint_DKGray.setStyle(Paint.Style.STROKE);
        paint_DKGray.setStrokeWidth(1.0f);
        paint_DKGray.setDither(true);
        paint_DKGray.setAntiAlias(true);

        paint_bg = new Paint();
        paint_bg.setStyle(Paint.Style.FILL);
        isShaderCreated = false;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        //super.onDraw(canvas);

        refbgAxis = clinometerActivity.getbgPIDValue();

        x = getWidth();
        y = getHeight();
        min_xy = Math.min(x, y);
        max_xy = Math.max(x, y);
        xc = x / 2;                                     // x screen center
        yc = y / 2;                                     // y screen center

        diag2c = Math.sqrt(xc * xc + yc * yc);          // Screen Diagonal/2 = distance between 0:0 and xc:yc
        r1_value = 2;                                   // The scale (to how many degrees corresponds each circle)
        ncircles = (int) Math.ceil(N_CIRCLES_FULLY_VISIBLE * 2 * diag2c / min_xy);
        // The number of circles to be drawn
        r1 = (min_xy / 2.0f) / N_CIRCLES_FULLY_VISIBLE; // The radius of the first circle.

        if (!isShaderCreated) {
            paint_bg.setShader(new RadialGradient(xc, yc, (int) (Math.sqrt(xc * xc + yc * yc) / 2),
                    getResources().getColor(R.color.bgpaint_dark),
                    getResources().getColor(R.color.bgpaint_light),
                    Shader.TileMode.MIRROR));
            isShaderCreated = true;
        }
        canvas.drawCircle(xc, yc, (int) Math.sqrt(xc*xc + yc*yc), paint_bg);

        canvas.save();
        canvas.rotate(refbgAxis, xc, yc);

        for (angle = 0; angle < 360; angle += 10) {
            if (angle % 30 != 0) canvas.drawLine(
                xc - (int) (diag2c * Math.cos(Math.toRadians(angle))),
                yc - (int) (diag2c * Math.sin(Math.toRadians(angle))),
                xc - (int) (r1 * Math.cos(Math.toRadians(angle))),
                yc - (int) (r1 * Math.sin(Math.toRadians(angle))),
                paint_DKGray);
        }

        canvas.restore();
    }
}
