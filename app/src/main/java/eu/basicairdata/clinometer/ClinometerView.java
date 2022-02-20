/*
 * ClinometerView - Java Class for Android
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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class ClinometerView extends View {

    private static final float TEXT_OFFSET = 10.0f;             // The distance in dp between text and its reference geometry
    private static final double TOUCH_ANGLE_TOLERANCE = 20;     // The tolerance (+-) for the Touch events that rotate the reference axis

    private static final float TEXT_ALIGNMENT_LEFT = 0.0f;
    private static final float TEXT_ALIGNMENT_CENTER = 0.5f;
    private static final float TEXT_ALIGNMENT_RIGHT = 1.0f;
    private static final float TEXT_ALIGNMENT_TOP = 1.0f;
    private static final float TEXT_ALIGNMENT_BOTTOM = 0.0f;
    private static final float TEXT_ROTATION_0 = 0.0f;
    private static final float TEXT_ROTATION_90 = 90.0f;
    private static final float TEXT_ROTATION_180 = 180.0f;
    private static final float TEXT_ROTATION_270 = 270.0f;

    private static final float N_CIRCLES_FULLY_VISIBLE = 4.5f;
    private static final float CONTRAST_STROKE = 6f;

    private final ClinometerActivity clinometerActivity = ClinometerActivity.getInstance();
    private final ClinometerApplication clinometerApplication = ClinometerApplication.getInstance();

    private Paint paint_LTGray;             // For Background Lines 30° + Circles
    private Paint paint_White;              // For White Angles Lines
    private Paint paint_Black00;            // For Black Contrast Lines stroke=0
    private Paint paint_Black15;            // For Black Contrast Lines stroke=1.5
    private Paint paint_Black30;            // For Black Contrast Lines stroke=3.0
    private Paint paint_WhiteText;          // For White Angles Text
    private Paint paint_ShadowText;         // For Shadows of Text
    private Paint paint_Arc;                // For White Angles Arcs
    private Paint paint_Yellow_Spirit;      // For Lines and Spirit Bubbles
    private Paint paint_bg_horizon;         // For Horizon Background

    private DataFormatter dataFormatter = new DataFormatter(); // Formatter for angles
    private String formattedAngle;

    private final Rect textbounds = new Rect();
    private final RectF arcRectF = new RectF();

    private int x;                      // The Width of Screen
    private int y;                      // The Height of Screen
    private int min_xy;                 // The minimum between Width and Height
    private int max_xy;                 // The maximum between Width and Height
    private int xc;                     // x screen center
    private int yc;                     // y screen center
    private double diag2c;              // Screen Diagonal/2 = distance between 0:0 and xc:yc
    private int ncircles;               // The number of visible circles
    private float xs;                   // The X Coordinate of the spirit bubble
    private float ys;                   // The Y Coordinate of the spirit bubble
    private float r1_value;             // The scale (to how many degrees corresponds each circle)
    private float r1;                   // The radius of the first circle = 1 deg.

    private float dash[] = new float[20];   // The Array of Lines for Dashed Line
    private float j;                        // the space between lines for Dashed Line
    private float ll, ls;                   // Long Line and Short Line length

    private int i;                      // A counter for onDraw();
    private float r;
    private int angle;
    private float angle1OffsetFromR = -0.1f;
    private float angle2OffsetFromR = 0.1f;

    private float angles[] = {0, 0, 0};
    private boolean isFlat;
    private float displayRotation = 0;
    private float angleXY;
    private float angleXYZ;
    private float angleTextLabels;

    private int textOffsetPx = 0;


    private float rot_angle_rad;            // The angle of rotation between absolute 3 o'clock and the white axis
    private float horizon_angle_deg;        // Horizon angle

    private float angle1Start;         // The Arc 1 start
    private float angle2Start;         // The Arc 2 start
    private float angle1Extension;     // The Arc 1 angle (+)
    private float angle2Extension;     // The Arc 2 angle (-)

    private float refAxis = 0;             // The reference axis for white Angles
    private float refbgAxis = 0;           // The reference axis for ref Angles
    // 0  = Horizontal axis
    // 90 = Vertical axis

    private boolean isAngle2LabelOnLeft = true;                 // True if the label of the Angle[2] must be placed on left instead of right
    private static final int ANGLE2LABELSWITCH_THRESHOLD = 2;   // 2 Degrees of Threshold for switching L/R the Angle[2] label


    public ClinometerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        createPaints();
    }


    public ClinometerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        createPaints();
    }


    public ClinometerView(Context context) {
        super(context);
        createPaints();
    }


    // Based on code posted in https://stackoverflow.com/questions/4605527/converting-pixels-to-dp
    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float dpToPx(float dp){
        return dp * ((float) ClinometerApplication.getInstance().getApplicationContext().getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }


    // Based on code posted in https://stackoverflow.com/questions/4605527/converting-pixels-to-dp
    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @return A float value to represent dp equivalent to px value
     */
    public static float pxToDp(float px){
        return px / ((float) ClinometerApplication.getInstance().getApplicationContext().getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }


    private void createPaints() {
        // create the Paint and set its color
        paint_LTGray = new Paint();
        paint_LTGray.setColor(getResources().getColor(R.color.line_light));
        paint_LTGray.setStyle(Paint.Style.STROKE);
        paint_LTGray.setStrokeWidth(1.0f);
        paint_LTGray.setDither(true);
        paint_LTGray.setAntiAlias(true);

        paint_White = new Paint();
        paint_White.setColor(getResources().getColor(R.color.line_white));
        paint_White.setStyle(Paint.Style.STROKE);
        paint_White.setStrokeWidth(1.5f);
        paint_White.setDither(true);
        paint_White.setAntiAlias(true);

        paint_WhiteText = new Paint();
        paint_WhiteText.setColor(getResources().getColor(R.color.line_white));
        paint_WhiteText.setStyle(Paint.Style.FILL);
        paint_WhiteText.setDither(true);
        paint_WhiteText.setAntiAlias(true);
        paint_WhiteText.setTextSize(getResources().getDimensionPixelSize(R.dimen.myFontSize));
        paint_WhiteText.setFakeBoldText(true);

        paint_ShadowText = new Paint();
        paint_ShadowText.setColor(getResources().getColor(R.color.black_overlay));
        paint_ShadowText.setStyle(Paint.Style.STROKE);
        paint_ShadowText.setStrokeWidth(5);
        paint_ShadowText.setStrokeJoin(Paint.Join.ROUND);
        paint_ShadowText.setDither(true);
        paint_ShadowText.setAntiAlias(true);
        paint_ShadowText.setTextSize(getResources().getDimensionPixelSize(R.dimen.myFontSize));
        paint_ShadowText.setFakeBoldText(true);

        paint_Arc = new Paint();
        paint_Arc.setColor(getResources().getColor(R.color.line_white));
        paint_Arc.setStyle(Paint.Style.STROKE);
        paint_Arc.setStrokeWidth(3);
        paint_Arc.setDither(true);
        paint_Arc.setAntiAlias(true);

        paint_Yellow_Spirit = new Paint();
        paint_Yellow_Spirit.setStyle(Paint.Style.FILL);
        paint_Yellow_Spirit.setStrokeWidth(3);
        paint_Yellow_Spirit.setDither(true);
        paint_Yellow_Spirit.setAntiAlias(true);
        paint_Yellow_Spirit.setTextSize(getResources().getDimensionPixelSize(R.dimen.myFontSize));
        paint_Yellow_Spirit.setFakeBoldText(true);
        paint_Yellow_Spirit.setColor(getResources().getColor(R.color.colorAccent));

        paint_bg_horizon = new Paint();
        paint_bg_horizon.setStyle(Paint.Style.FILL);
        paint_bg_horizon.setColor(getResources().getColor(R.color.bghorizon));

        paint_Black00 = new Paint();
        paint_Black00.setColor(getResources().getColor(R.color.black_contrast));
        paint_Black00.setStyle(Paint.Style.STROKE);
        paint_Black00.setStrokeWidth(0f + CONTRAST_STROKE);
        paint_Black00.setDither(true);
        paint_Black00.setAntiAlias(true);

        paint_Black15 = new Paint();
        paint_Black15.setColor(getResources().getColor(R.color.black_contrast));
        paint_Black15.setStyle(Paint.Style.STROKE);
        paint_Black15.setStrokeWidth(1.5f + CONTRAST_STROKE);
        paint_Black15.setDither(true);
        paint_Black15.setAntiAlias(true);
        //paint_Black15.setStrokeCap(Paint.Cap.ROUND);

        paint_Black30 = new Paint();
        paint_Black30.setColor(getResources().getColor(R.color.black_contrast));
        paint_Black30.setStyle(Paint.Style.STROKE);
        paint_Black30.setStrokeWidth(3f + CONTRAST_STROKE);
        paint_Black30.setDither(true);
        paint_Black30.setAntiAlias(true);

        textOffsetPx = Math.round(dpToPx(TEXT_OFFSET));
    }


    @Override
    protected void onDraw(Canvas canvas) {
        //super.onDraw(canvas);

        angles = clinometerActivity.getAngles();
        angleXY = clinometerActivity.getAngleXY();
        angleXYZ = clinometerActivity.getAngleXYZ();
        angleTextLabels = clinometerActivity.getAngleTextLabels();
        isFlat = clinometerActivity.isFlat();
        displayRotation = clinometerActivity.getDisplayRotation();
        refAxis = clinometerActivity.getPIDValue();
        refbgAxis = clinometerActivity.getbgPIDValue();

        // --------[ CALCULATIONS ]-----------------------------------------------------------------

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

        xs = xc + angles[0] * r1 / r1_value;  // The X coordinate of the spirit bubble center
        ys = yc - angles[1] * r1 / r1_value;  // The X coordinate of the spirit bubble center

        rot_angle_rad = (float) Math.toRadians(angleXY);
        // The angle of rotation between absolute 3 o'clock and the white axis
        horizon_angle_deg = angleXY + 90;    // The angle of rotation between absolute 3 o'clock and the white axis

        angle1Start = refAxis;
        angle1Extension = (360 + (horizon_angle_deg % 180) - refAxis) % 180;
        angle2Start = 180 + refAxis;
        angle2Extension = - 180 - (- 360 + refAxis - horizon_angle_deg) % 180;

        // Set the position of the arcs in order to avoid to switch from internal to the external one
        if (((angleXY - angle1Start + 360) % 360 < 270) && ((angleXY - angle1Start + 360) % 360 >= 90)) {
            angle1OffsetFromR = 0.1f;
            angle2OffsetFromR = -0.1f;
        } else {
            angle1OffsetFromR = -0.1f;
            angle2OffsetFromR = 0.1f;
        }

        // Dashed line drawn as Array of Lines
        // because DashPathEffect is not supported by some devices

        j  = max_xy * 0.011f;
        ll = max_xy * 0.15625f;
        ls = max_xy * 0.015625f;

        dash[0] = xc;
        dash[1] = yc;
        dash[2] = dash[0] - ll;
        dash[3] = yc;

        dash[4] = dash[2] - j;
        dash[5] = yc;
        dash[6] = dash[4] - ls;
        dash[7] = yc;

        dash[8] = dash[6] - j;
        dash[9] = yc;
        dash[10] = dash[8] - ll;
        dash[11] = yc;

        dash[12] = dash[10] - j;
        dash[13] = yc;
        dash[14] = dash[12] - ls;
        dash[15] = yc;

        dash[16] = dash[14] - j;
        dash[17] = yc;
        dash[18] = xc - (float) diag2c;
        dash[19] = yc;


        // For angle starting from reference axis
//        angle2Start = refAxis;
//        angle2Extension = - 180 + angle1Extension;

        // -----------------------------------------------------------------------------------------
        // --------[ BACKGROUND ]-------------------------------------------------------------------

//        if (!isShaderCreated) {
//            paint_bg.setShader(new RadialGradient(xc, yc, (int) (Math.sqrt(xc * xc + yc * yc) / 2),
//                    getResources().getColor(R.color.bgpaint_dark),
//                    getResources().getColor(R.color.bgpaint_light),
//                    Shader.TileMode.MIRROR));
//        }
//        if (!svActivity.isCameraActive)
//            canvas.drawCircle(xc, yc, (int) Math.sqrt(xc*xc + yc*yc), paint_bg);

        // --------[ BACKGROUND OF SPIRIT LEVEL HORIZON ]-------------------------------------------

        canvas.save();
        canvas.rotate(angleXY + 90, xc, yc);
        //canvas.translate(0, (int) ((90 - Math.toDegrees(angleXYZ)) * r1 / r1_value));
        canvas.drawRect((int) (xc - diag2c), yc + (int) ((90 - angleXYZ) * r1 / r1_value),
                (int)(xc + diag2c), (int)(yc + diag2c), paint_bg_horizon);
        canvas.restore();

        // --------[ BACKGROUND LINES ]-------------------------------------------------------------

        canvas.save();
        canvas.rotate(refbgAxis, xc, yc);
        for (angle = 0; angle < 360; angle += 30) {
            canvas.drawLine(
                    xc - (int) (diag2c * Math.cos(Math.toRadians(angle))),
                    yc - (int) (diag2c * Math.sin(Math.toRadians(angle))),
                    xc - (int) ((angle % 90 == 0 ? 0 : r1) * Math.cos(Math.toRadians(angle))),
                    yc - (int) ((angle % 90 == 0 ? 0 : r1) * Math.sin(Math.toRadians(angle))),
                    paint_LTGray);
        }
        canvas.restore();

        // --------[ CONTRAST SHADOWS ]----------------------------------------------------------------------

        // Horizontal and Vertical Axis
        canvas.save();
        canvas.rotate(refAxis, xc, yc);
        canvas.drawLines(dash, 0, 20, paint_Black15);
        canvas.rotate(180, xc, yc);
        canvas.drawLines(dash, 0, 20, paint_Black15);
        canvas.restore();
        // Cross
        canvas.drawLine(0, ys, x, ys, paint_Black30);
        canvas.drawLine(xs, 0, xs, y, paint_Black30);
        // Bubble
        canvas.drawCircle(xs, ys, r1 / 4, paint_Black00);
        // Angle Arcs
        r = (2.0f + angle1OffsetFromR) * r1;
        arcRectF.left = xc - r;           // The RectF for the Arc
        arcRectF.right = xc + r;
        arcRectF.top = yc - r;
        arcRectF.bottom = yc + r;
        if ((clinometerApplication.getPrefUM() < DataFormatter.UM_PERCENT) || (Math.abs(angle1Extension) <= 90))
            canvas.drawArc(arcRectF, angle1Start + 2, angle1Extension - 4, false, paint_Black15);
        r = (2.0f + angle2OffsetFromR) * r1;
        arcRectF.left = xc - r;           // The RectF for the Arc
        arcRectF.right = xc + r;
        arcRectF.top = yc - r;
        arcRectF.bottom = yc + r;
        if ((clinometerApplication.getPrefUM() < DataFormatter.UM_PERCENT) || (Math.abs(angle2Extension) <= 90))
            canvas.drawArc(arcRectF, angle2Start - 2, angle2Extension + 4, false, paint_Black15);
        // Spirit level Horizon
        if (!isFlat) {
            canvas.save();
            canvas.rotate(angleXY + 90, xc, yc);
            //canvas.translate(0, (int) ((90 - angleXYZ) * r1 / r1_value));
            canvas.drawLine((int) (xc - diag2c), yc + (int) ((90 - angleXYZ) * r1 / r1_value),
                    (int) (xc + diag2c), yc + (int) ((90 - angleXYZ) * r1 / r1_value), paint_Black30);
            canvas.restore();
        }
        // Horizon and max gradient
        canvas.save();
        canvas.rotate((float) Math.toDegrees(rot_angle_rad), xc, yc);
        //canvas.drawLine(xc - (xc + yc), yc, xc + (xc + yc), yc, paint_LTGray);    // Max Gradient
        canvas.drawLine(xc - min_xy / 2 + r1 / 2, yc, (float) -diag2c, yc, paint_Black15);                 // Max Gradient
        //canvas.drawLine(xc - min_xy/2 + r1, yc, xc, yc, paint_LTGray);                 // Max Gradient
        canvas.rotate(90, xc, yc);
        canvas.drawLine(xc - (xc + yc), yc, xc + (xc + yc), yc, paint_Black15);       // Horizon
        canvas.restore();

        // --------[ HORIZONTAL AND VERTICAL AXIS ]----------------------------------------------------------------------

        canvas.save();
        canvas.rotate(refAxis, xc, yc);
        canvas.drawLines(dash, 0, 20, paint_White);
        canvas.rotate(180, xc, yc);
        canvas.drawLines(dash, 0, 20, paint_White);
        canvas.restore();

        // --------[ BACKGROUND CIRCLES ]-----------------------------------------------------------

        for (i = 1; i <= ncircles; i=i+1) canvas.drawCircle(xc, yc, Math.round(r1*i), paint_LTGray);
        //for (int i = 2; i <= ncircles*2; i=i+2) canvas.drawCircle(xc, yc, Math.round(r1*i), paint);
        //for (int i = 3; i <= ncircles*2; i=i+2) canvas.drawCircle(xc, yc, Math.round(r1*i), paint_secondary);



        // -----------------------------------------------------------------------------------------
        // --------[ SPIRIT LEVEL ]-----------------------------------------------------------------

        // Horizon and max gradient
        canvas.save();
        canvas.rotate((float) Math.toDegrees(rot_angle_rad), xc, yc);
        //canvas.drawLine(xc - (xc + yc), yc, xc + (xc + yc), yc, paint_LTGray);    // Max Gradient
        canvas.drawLine(xc - min_xy/2 + r1/2, yc, (float)-diag2c, yc, paint_White);                 // Max Gradient
        //canvas.drawLine(xc - min_xy/2 + r1, yc, xc, yc, paint_LTGray);                 // Max Gradient
        canvas.rotate(90, xc, yc);
        canvas.drawLine(xc - (xc + yc), yc, xc + (xc + yc), yc, paint_White);       // Horizon
        canvas.restore();

        // Cross
        canvas.drawLine(0, ys, x, ys, paint_Yellow_Spirit);
        canvas.drawLine(xs, 0, xs, y, paint_Yellow_Spirit);

        // White angles

        r = (2.0f + angle1OffsetFromR) * r1;
        arcRectF.left = xc - r;           // The RectF for the Arc
        arcRectF.right = xc + r;
        arcRectF.top = yc - r;
        arcRectF.bottom = yc + r;
        if ((clinometerApplication.getPrefUM() < DataFormatter.UM_PERCENT) || (Math.abs(angle1Extension) <= 90))
            canvas.drawArc(arcRectF, angle1Start + 2, angle1Extension - 4, false, paint_White);

        r = (2.0f + angle2OffsetFromR) * r1;
        arcRectF.left = xc - r;           // The RectF for the Arc
        arcRectF.right = xc + r;
        arcRectF.top = yc - r;
        arcRectF.bottom = yc + r;
        if ((clinometerApplication.getPrefUM() < DataFormatter.UM_PERCENT) || (Math.abs(angle2Extension) <= 90))
            canvas.drawArc(arcRectF, angle2Start - 2, angle2Extension + 4, false, paint_White);

        // Bubble Circle
        canvas.drawCircle(xs, ys,r1/4, paint_Yellow_Spirit);

        // Spirit level Horizon
        if (!isFlat) {
            canvas.save();
            canvas.rotate(angleXY + 90, xc, yc);
            //canvas.translate(0, (int) ((90 - angleXYZ) * r1 / r1_value));
            canvas.drawLine((int) (xc - diag2c), yc + (int) ((90 - angleXYZ) * r1 / r1_value),
                    (int)(xc + diag2c), yc + (int) ((90 - angleXYZ) * r1 / r1_value), paint_Yellow_Spirit);
            canvas.restore();

            //canvas.drawLine(x_horizon[0], y_horizon[0], x_horizon[1], y_horizon[1], paint_Yellow_Spirit);
            //canvas.drawCircle(xs_vertical, ys_vertical,r1/4, paint_Yellow_Spirit);
        }



        // -----------------------------------------------------------------------------------------
        // --------[ TEXT LABELS ]------------------------------------------------------------------

        // Angle Z
        canvas.save();
        canvas.rotate( (float) Math.toDegrees(rot_angle_rad) + 180, xc, yc);
        formattedAngle = dataFormatter.format(Math.abs(90 - angles[2]));
        drawTextWithShadow(canvas, formattedAngle,
                (int) (min_xy - (r1)), yc,
                TEXT_ALIGNMENT_CENTER, TEXT_ALIGNMENT_CENTER,
                (angleTextLabels - (float) Math.toDegrees(rot_angle_rad) - 180) , paint_WhiteText);
        canvas.restore();

        // Angle 0 + 1
        if (displayRotation == 0f) {
            formattedAngle = dataFormatter.format(angles[0]);
            drawTextWithShadow(canvas, formattedAngle, (int)xs - textOffsetPx, y - textOffsetPx,
                    TEXT_ALIGNMENT_RIGHT, TEXT_ALIGNMENT_BOTTOM, TEXT_ROTATION_0, paint_Yellow_Spirit);
            formattedAngle = dataFormatter.format(angles[1]);
            drawTextWithShadow(canvas, formattedAngle, textOffsetPx, (int)ys - textOffsetPx,
                    TEXT_ALIGNMENT_LEFT, TEXT_ALIGNMENT_BOTTOM, TEXT_ROTATION_0, paint_Yellow_Spirit);
        }
        if (displayRotation == 90f) {
            formattedAngle = dataFormatter.format(angles[0]);
            drawTextWithShadow(canvas, formattedAngle, (int)xs + textOffsetPx, textOffsetPx,
                    TEXT_ALIGNMENT_LEFT, TEXT_ALIGNMENT_BOTTOM, TEXT_ROTATION_90, paint_Yellow_Spirit);
            formattedAngle = dataFormatter.format(angles[1]);
            drawTextWithShadow(canvas, formattedAngle, textOffsetPx, (int)ys - textOffsetPx,
                    TEXT_ALIGNMENT_RIGHT, TEXT_ALIGNMENT_BOTTOM, TEXT_ROTATION_90, paint_Yellow_Spirit);
        }
        if (displayRotation == 180f) {
            formattedAngle = dataFormatter.format(angles[0]);
            drawTextWithShadow(canvas, formattedAngle, (int)xs + textOffsetPx, textOffsetPx,
                    TEXT_ALIGNMENT_RIGHT, TEXT_ALIGNMENT_BOTTOM, TEXT_ROTATION_180, paint_Yellow_Spirit);
            formattedAngle = dataFormatter.format(angles[1]);
            drawTextWithShadow(canvas, formattedAngle, x - textOffsetPx, (int)ys + textOffsetPx,
                    TEXT_ALIGNMENT_LEFT, TEXT_ALIGNMENT_BOTTOM, TEXT_ROTATION_180, paint_Yellow_Spirit);
        }
        if (displayRotation == 270f) {
            formattedAngle = dataFormatter.format(angles[0]);
            drawTextWithShadow(canvas, formattedAngle, (int)xs - textOffsetPx, y - textOffsetPx,
                    TEXT_ALIGNMENT_LEFT, TEXT_ALIGNMENT_BOTTOM, TEXT_ROTATION_270, paint_Yellow_Spirit);
            formattedAngle = dataFormatter.format(angles[1]);
            drawTextWithShadow(canvas, formattedAngle, x - textOffsetPx, (int)ys + textOffsetPx,
                    TEXT_ALIGNMENT_RIGHT, TEXT_ALIGNMENT_BOTTOM, TEXT_ROTATION_270, paint_Yellow_Spirit);
        }

        // Angle 2
        if (!isFlat) {
            if ((Math.abs(angles[2]) > ANGLE2LABELSWITCH_THRESHOLD)
                    && (Math.abs(angles[0]) > ANGLE2LABELSWITCH_THRESHOLD)
                    && (Math.abs(angles[1]) > ANGLE2LABELSWITCH_THRESHOLD)) {
                // Switch evaluation
                isAngle2LabelOnLeft = false;
                if (((displayRotation == 0f) || (displayRotation == 180f)) &&
                        (angles[2] * angles[0] < 0)) isAngle2LabelOnLeft = true;
                if (((displayRotation == 90f) || (displayRotation == 270f)) &&
                        (angles[2] * angles[1] < 0)) isAngle2LabelOnLeft = true;
                if ((displayRotation == 180f) || (displayRotation == 270f)) isAngle2LabelOnLeft = !isAngle2LabelOnLeft;
            }

            canvas.save();

            if (displayRotation == 0f) {
                canvas.rotate(angles[0], xc, yc);
            }
            if (displayRotation == 90f) {
                canvas.rotate(- 270 - angles[1], xc, yc);
            }
            if (displayRotation == 180f) {
                canvas.rotate(180 - angles[0], xc, yc);
            }
            if (displayRotation == 270f) {
                canvas.rotate(+ 270 + angles[1], xc, yc);
            }

            //canvas.drawLine(xc-(xc+yc), yc, xc+(xc+yc), yc, paint_White);
            canvas.translate(0, angles[2] * r1 / r1_value);

            if (isAngle2LabelOnLeft) {
                // SX
                formattedAngle = dataFormatter.format(angles[2]);
                drawTextWithShadow(canvas, formattedAngle, textOffsetPx, yc - textOffsetPx,
                        TEXT_ALIGNMENT_LEFT, TEXT_ALIGNMENT_BOTTOM, TEXT_ROTATION_0, paint_Yellow_Spirit);
            } else {
                // DX
                formattedAngle = dataFormatter.format(angles[2]);
                drawTextWithShadow(canvas, formattedAngle, x - textOffsetPx , yc - textOffsetPx,
                        TEXT_ALIGNMENT_RIGHT, TEXT_ALIGNMENT_BOTTOM, TEXT_ROTATION_0, paint_Yellow_Spirit);
            }
            canvas.restore();
        }

        // --------[ WHITE LABELS ]-----------------------------------------------------------------

        canvas.save();
        canvas.rotate( angle1Start + angle1Extension /2, xc, yc);
        formattedAngle = dataFormatter.format(Math.abs(angle1Extension));
        if ((clinometerApplication.getPrefUM() < DataFormatter.UM_PERCENT) || (Math.abs(angle1Extension) <= 90))
            drawTextWithShadow(canvas, formattedAngle,
                (int) (xc + (r1 * (2.1f + angle1OffsetFromR)) + (textOffsetPx * 1.5) + paint_White.measureText("100.0°") / 2), yc,
                TEXT_ALIGNMENT_CENTER, TEXT_ALIGNMENT_CENTER,
                -angle1Extension /2 - refAxis + angleTextLabels , paint_WhiteText);
        canvas.rotate( 90 , xc, yc);
        formattedAngle = dataFormatter.format(Math.abs(angle2Extension));
        if ((clinometerApplication.getPrefUM() < DataFormatter.UM_PERCENT) || (Math.abs(angle2Extension) <= 90))
            drawTextWithShadow(canvas, formattedAngle,
                (int) (xc + (r1 * (2.1f + angle2OffsetFromR)) + (textOffsetPx * 1.5) + paint_White.measureText("100.0°") / 2), yc,
                TEXT_ALIGNMENT_CENTER, TEXT_ALIGNMENT_CENTER,
                -angle1Extension /2 - 90 - refAxis + angleTextLabels, paint_WhiteText);
        // For angle starting from reference axis
//        canvas.rotate( -90 , xc, yc);
//        drawTextWithShadow(canvas, String.format("%1.1f°", Math.abs(angle2Extension)),
//                (int) (xc + (r1 * 2.1) + 30 + paint_White.measureText("100.0°") / 2), yc,
//                TEXT_ALIGNMENT_CENTER, TEXT_ALIGNMENT_CENTER,
//                -angle1Extension /2 + 90 - refAxis + svActivity.angleTextLabels, paint_WhiteText);
        canvas.restore();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
//                Log.d("SpiritLevel", "Center Screen " + xc + " " + yc);
//                Log.d("SpiritLevel", String.format("TouchEvent %1.0f %1.0f", event.getX(), event.getY()));

                // Change Ref Axis
                double touchAngle = Math.toDegrees(Math.asin((event.getY() - yc)/(Math.sqrt((event.getX() - xc) * (event.getX() - xc) + (event.getY() - yc) * (event.getY() - yc)))));
                if ((xc > event.getX())) touchAngle = 180 - touchAngle;
                if ((xc <= event.getX()) && (yc > event.getY())) touchAngle = 360 + touchAngle;

                if ((touchAngle - clinometerActivity.getRefAngleXY() + TOUCH_ANGLE_TOLERANCE) % 90 < TOUCH_ANGLE_TOLERANCE * 2)
                    clinometerActivity.setRefAngleXY(clinometerActivity.getRefAngleXY() % 90 + 90 * Math.round((touchAngle - clinometerActivity.getRefAngleXY() % 90) / 90));
                    clinometerActivity.setPIDTargetValue(clinometerActivity.getRefAngleXY());

                Log.w("myApp", "[#] ClinometerView - Angle = " + touchAngle);

                break;
            case MotionEvent.ACTION_MOVE:
                break;
            default:
                break;
        }
        return true;
    }


    private int tHeight = 0;
    private int tWidth = 0;

    private void drawTextWithShadow(Canvas canvas, String text, int x, int y, float horizontal_alignment, float vertical_alignment, float rotation, Paint paint) {
        paint_Yellow_Spirit.getTextBounds(text, 0, text.length(), textbounds);
        tHeight = textbounds.height();
        tWidth = textbounds.width();

        canvas.save();
        canvas.rotate(rotation, x, y);
        paint_ShadowText.setAlpha(paint.getAlpha());
        canvas.drawText(text, x-tWidth*horizontal_alignment, y+tHeight*vertical_alignment, paint_ShadowText);
        canvas.drawText(text, x-tWidth*horizontal_alignment, y+tHeight*vertical_alignment, paint);
        canvas.restore();
        //canvas.drawRoundRect(rect,4, 4, paint_spirit);
    }
}
