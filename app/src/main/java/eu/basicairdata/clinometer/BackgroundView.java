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

    int x;                      // The Width of Screen
    int y;                      // The Height of Screen
    int min_xy;                 // The minimum between Width and Height
    int max_xy;                 // The maximum between Width and Height
    int xc;                     // x screen center
    int yc;                     // y screen center
    double diag2c;              // Screen Diagonal/2 = distance between 0:0 and xc:yc
    int ncircles;               // The number of visible circles
    float r1_value;             // The scale (to how many degrees corresponds each circle)
    float r1;                   // The radius of the first circle = 1 deg.


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
        paint_DKGray.setDither(true);
        paint_DKGray.setAntiAlias(true);

        paint_bg = new Paint();
        paint_bg.setStyle(Paint.Style.FILL);
        isShaderCreated = false;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

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
        r1 = (min_xy / 2) / N_CIRCLES_FULLY_VISIBLE;    // The radius of the first circle.

        if (!isShaderCreated) {
            paint_bg.setShader(new RadialGradient(xc, yc, (int) (Math.sqrt(xc * xc + yc * yc) / 2),
                    getResources().getColor(R.color.bgpaint_dark),
                    getResources().getColor(R.color.bgpaint_light),
                    Shader.TileMode.MIRROR));
        }
        canvas.drawCircle(xc, yc, (int) Math.sqrt(xc*xc + yc*yc), paint_bg);

        for (int angle = 0; angle < 360; angle += 10) {
            canvas.drawLine(
                    xc - (int) (diag2c * Math.cos(Math.toRadians(angle))),
                    yc - (int) (diag2c * Math.sin(Math.toRadians(angle))),
                    xc - (int) ((angle % 90 == 0 ? 0 : r1) * Math.cos(Math.toRadians(angle))),
                    yc - (int) ((angle % 90 == 0 ? 0 : r1) * Math.sin(Math.toRadians(angle))),
                    paint_DKGray);
        }
    }
}
