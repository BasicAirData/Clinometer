/*
 * ClinometerView - Java Class for Android
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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class ClinometerView extends View {

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

    private final ClinometerActivity svActivity = ClinometerActivity.getInstance();

    private Paint paint_LTGray;             // For Background Lines 30° + Circles
    private Paint paint_White;              // For White Angles Lines
    private Paint paint_WhiteDashed;        // For Reference Axis Line
    private Paint paint_Black00;            // For Black Contrast Lines stroke=0
    private Paint paint_Black15;            // For Black Contrast Lines stroke=1.5
    private Paint paint_Black15Dashed;      // For Black Contrast Reference Line stroke=1.5
    private Paint paint_Black30;            // For Black Contrast Lines stroke=3.0
    private Paint paint_WhiteText;          // For White Angles Text
    private Paint paint_ShadowText;         // For Shadows of Text
    private Paint paint_Arc;                // For White Angles Arcs
    private Paint paint_Yellow_Spirit;      // For Lines and Spirit Bubbles
    private Paint paint_bg_horizon;         // For Horizon Background

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

    private int i;
    private float r;
    private int angle;

    private float angles[] = {0, 0, 0};
    private boolean isFlat;
    private float displayRotation = 0;
    private float angleXY;
    private float angleXYZ;
    private float angleTextLabels;


    private float rot_angle_rad;            // The angle of rotation between absolute 3 o'clock and the white axis
    private float horizon_angle_deg;        // Horizon angle

    private float angle1Start;         // The Arc 1 start
    private float angle2Start;         // The Arc 2 start
    private float angle1Extension;     // The Arc 1 angle (+)
    private float angle2Extension;     // The Arc 2 angle (-)

    private float refAxis = 0;             // The reference axis for white Angles
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


    private void createPaints() {
        DashPathEffect dashPathEffect = new DashPathEffect(new float[]{200,10,20,10},0);

        // create the Paint and set its color
        paint_LTGray = new Paint();
        paint_LTGray.setColor(getResources().getColor(R.color.line_light));
        paint_LTGray.setStyle(Paint.Style.STROKE);
        paint_LTGray.setDither(true);
        paint_LTGray.setAntiAlias(true);

        paint_White = new Paint();
        paint_White.setColor(getResources().getColor(R.color.line_white));
        paint_White.setStyle(Paint.Style.STROKE);
        paint_White.setStrokeWidth(1.5f);
        paint_White.setDither(true);
        paint_White.setAntiAlias(true);

        paint_WhiteDashed = new Paint();
        paint_WhiteDashed.setColor(getResources().getColor(R.color.line_white));
        paint_WhiteDashed.setStyle(Paint.Style.STROKE);
        paint_WhiteDashed.setStrokeWidth(1.5f);
        paint_WhiteDashed.setDither(true);
        paint_WhiteDashed.setAntiAlias(true);
        paint_WhiteDashed.setPathEffect(dashPathEffect);

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

        paint_Black15Dashed = new Paint();
        paint_Black15Dashed.setColor(getResources().getColor(R.color.black_contrast));
        paint_Black15Dashed.setStyle(Paint.Style.STROKE);
        paint_Black15Dashed.setStrokeWidth(1.5f + CONTRAST_STROKE);
        paint_Black15Dashed.setDither(true);
        paint_Black15Dashed.setAntiAlias(true);
        paint_Black15Dashed.setPathEffect(dashPathEffect);

        paint_Black30 = new Paint();
        paint_Black30.setColor(getResources().getColor(R.color.black_contrast));
        paint_Black30.setStyle(Paint.Style.STROKE);
        paint_Black30.setStrokeWidth(3f + CONTRAST_STROKE);
        paint_Black30.setDither(true);
        paint_Black30.setAntiAlias(true);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        //super.onDraw(canvas);

        angles = svActivity.getAngles();
        angleXY = svActivity.getAngleXY();
        angleXYZ = svActivity.getAngleXYZ();
        angleTextLabels = svActivity.getAngleTextLabels();
        isFlat = svActivity.isFlat();
        displayRotation = svActivity.getDisplayRotation();
        refAxis = svActivity.getPIDValue();

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

        for (angle = 0; angle < 360; angle += 30) {
            canvas.drawLine(
                    xc - (int) (diag2c * Math.cos(Math.toRadians(angle))),
                    yc - (int) (diag2c * Math.sin(Math.toRadians(angle))),
                    xc - (int) ((angle % 90 == 0 ? 0 : r1) * Math.cos(Math.toRadians(angle))),
                    yc - (int) ((angle % 90 == 0 ? 0 : r1) * Math.sin(Math.toRadians(angle))),
                    paint_LTGray);
        }

        // --------[ CONTRAST SHADOWS ]----------------------------------------------------------------------

        // Horizontal and Vertical Axis
        canvas.save();
        canvas.rotate(refAxis, xc, yc);
        canvas.drawLine(xc, yc, xc - max_xy/2, yc, paint_Black15Dashed);
        canvas.drawLine(xc, yc, max_xy, yc, paint_Black15Dashed);
        canvas.restore();
        // Cross
        canvas.drawLine(0, ys, x, ys, paint_Black30);
        canvas.drawLine(xs, 0, xs, y, paint_Black30);
        // Bubble
        canvas.drawCircle(xs, ys, r1 / 4, paint_Black00);
        // Angle Arcs
        r = 1.9f * r1;
        arcRectF.left = xc - r;           // The RectF for the Arc
        arcRectF.right = xc + r;
        arcRectF.top = yc - r;
        arcRectF.bottom = yc + r;
        canvas.drawArc(arcRectF, angle1Start + 2, angle1Extension - 4, false, paint_Black15);
        r = 2.1f * r1;
        arcRectF.left = xc - r;           // The RectF for the Arc
        arcRectF.right = xc + r;
        arcRectF.top = yc - r;
        arcRectF.bottom = yc + r;
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
        canvas.drawLine(xc, yc, xc - max_xy, yc, paint_WhiteDashed);
        canvas.drawLine(xc, yc, max_xy, yc, paint_WhiteDashed);
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

        r = 1.9f * r1;
        arcRectF.left = xc - r;           // The RectF for the Arc
        arcRectF.right = xc + r;
        arcRectF.top = yc - r;
        arcRectF.bottom = yc + r;
        canvas.drawArc(arcRectF, angle1Start + 2, angle1Extension - 4, false, paint_White);

        r = 2.1f * r1;
        arcRectF.left = xc - r;           // The RectF for the Arc
        arcRectF.right = xc + r;
        arcRectF.top = yc - r;
        arcRectF.bottom = yc + r;
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
        drawTextWithShadow(canvas, String.format("%1.1f°", Math.abs(90 - angles[2])),
                (int) (min_xy - (r1)), yc,
                TEXT_ALIGNMENT_CENTER, TEXT_ALIGNMENT_CENTER,
                (angleTextLabels - (float) Math.toDegrees(rot_angle_rad) - 180) , paint_WhiteText);
        canvas.restore();

        // Angle 0 + 1
        if (displayRotation == 0f) {
            drawTextWithShadow(canvas, String.format("%1.1f°", angles[0]), (int)xs - 20, y - 20,
                    TEXT_ALIGNMENT_RIGHT, TEXT_ALIGNMENT_BOTTOM, TEXT_ROTATION_0, paint_Yellow_Spirit);
            drawTextWithShadow(canvas, String.format("%1.1f°", angles[1]), 20, (int)ys - 20,
                    TEXT_ALIGNMENT_LEFT, TEXT_ALIGNMENT_BOTTOM, TEXT_ROTATION_0, paint_Yellow_Spirit);
        }
        if (displayRotation == 90f) {
            drawTextWithShadow(canvas, String.format("%1.1f°", angles[0]), (int)xs + 20, 20,
                    TEXT_ALIGNMENT_LEFT, TEXT_ALIGNMENT_BOTTOM, TEXT_ROTATION_90, paint_Yellow_Spirit);
            drawTextWithShadow(canvas, String.format("%1.1f°", angles[1]), 20, (int)ys - 20,
                    TEXT_ALIGNMENT_RIGHT, TEXT_ALIGNMENT_BOTTOM, TEXT_ROTATION_90, paint_Yellow_Spirit);
        }
        if (displayRotation == 180f) {
            drawTextWithShadow(canvas, String.format("%1.1f°", angles[0]), (int)xs + 20, 20,
                    TEXT_ALIGNMENT_RIGHT, TEXT_ALIGNMENT_BOTTOM, TEXT_ROTATION_180, paint_Yellow_Spirit);
            drawTextWithShadow(canvas, String.format("%1.1f°", angles[1]), x - 20, (int)ys + 20,
                    TEXT_ALIGNMENT_LEFT, TEXT_ALIGNMENT_BOTTOM, TEXT_ROTATION_180, paint_Yellow_Spirit);
        }
        if (displayRotation == 270f) {
            drawTextWithShadow(canvas, String.format("%1.1f°", angles[0]), (int)xs - 20, y - 20,
                    TEXT_ALIGNMENT_LEFT, TEXT_ALIGNMENT_BOTTOM, TEXT_ROTATION_270, paint_Yellow_Spirit);
            drawTextWithShadow(canvas, String.format("%1.1f°", angles[1]), x - 20, (int)ys + 20,
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
                drawTextWithShadow(canvas, String.format("%1.1f°", angles[2]), 20, yc - 20,
                        TEXT_ALIGNMENT_LEFT, TEXT_ALIGNMENT_BOTTOM, TEXT_ROTATION_0, paint_Yellow_Spirit);
            } else {
                // DX
                drawTextWithShadow(canvas, String.format("%1.1f°", angles[2]), x - 20 , yc - 20,
                        TEXT_ALIGNMENT_RIGHT, TEXT_ALIGNMENT_BOTTOM, TEXT_ROTATION_0, paint_Yellow_Spirit);
            }
            canvas.restore();
        }

        // --------[ WHITE LABELS ]-----------------------------------------------------------------

        canvas.save();
        canvas.rotate( angle1Start + angle1Extension /2, xc, yc);
        drawTextWithShadow(canvas, String.format("%1.1f°", Math.abs(angle1Extension)),
                (int) (xc + (r1 * 2) + 30 + paint_White.measureText("100.0°") / 2), yc,
                TEXT_ALIGNMENT_CENTER, TEXT_ALIGNMENT_CENTER,
                -angle1Extension /2 - refAxis + angleTextLabels , paint_WhiteText);
        canvas.rotate( 90 , xc, yc);
        drawTextWithShadow(canvas, String.format("%1.1f°", Math.abs(angle2Extension)),
                (int) (xc + (r1 * 2.1) + 30 + paint_White.measureText("100.0°") / 2), yc,
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
                Log.d("SpiritLevel", "Center Screen " + xc + " " + yc);
                Log.d("SpiritLevel", String.format("TouchEvent %1.0f %1.0f", event.getX(), event.getY()));

                // Change Ref Axis
                if (Math.sqrt((xc-event.getX())*(xc-event.getX()) + (yc-event.getY())*(yc-event.getY())) > 2*r1) {
                    if (Math.abs(xc - event.getX()) < 1 * r1) svActivity.setPIDTargetValue(yc < event.getY() ? 90 : 270);
                    if (Math.abs(yc - event.getY()) < 1 * r1) svActivity.setPIDTargetValue(xc < event.getX() ? 0 : 180);
                }
                // Invalidate the whole view. If the view is visible.
                //invalidate();
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
