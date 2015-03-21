package ru.tumbler.androidrobot.remote;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by tumbler on 19.01.15.
 */
public class DrawView extends SurfaceView implements SurfaceHolder.Callback {

    public static final int BG_COLOR = Color.argb(255, 200, 200, 200);
    private int textStroke;
    private DrawThread drawThread;

    private float cursorX, cursorY;
    private float canvasW, canvasH;
    private float zeroX, zeroY;
    private int stroke;
    private float cx, cy;

    public void updateCursor(float x, float y, float screenx, float screeny) {
        cursorX = x;
        cursorY = y;
        cx = screenx;
        cy = screeny;

    }
    public void updateZero(float w, float h) {
        canvasW = w;
        canvasH = h;
        zeroY = h / 2;
        zeroX = w / 2;
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DrawView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public DrawView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        getHolder().addCallback(this);
        cursorX = 0;
        cursorY = 0;
        canvasW = 0;
        canvasH = 0;
        zeroX = 0;
        zeroY = 0;
        cx = 0;
        cy = 0;
        stroke = (int)(context.getResources().getDisplayMetrics().density * 2);
        textStroke = (int)(context.getResources().getDisplayMetrics().density);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        drawThread = new DrawThread(getHolder());
        drawThread.setRunning(true);
        drawThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        drawThread.setRunning(false);
        while (retry) {
            try {
                drawThread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }

    class DrawThread extends Thread {

        private boolean running = false;
        private SurfaceHolder surfaceHolder;

        StringBuilder sb;
        Paint p;

        public DrawThread(SurfaceHolder surfaceHolder) {
            this.surfaceHolder = surfaceHolder;
            sb = new StringBuilder();
            p = new Paint();
            p.setTextSize(60);
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        @Override
        public void run() {
            Canvas canvas;
            while (running) {
                canvas = null;
                try {
                    canvas = surfaceHolder.lockCanvas(null);
                    if (canvas == null)
                        continue;
                    updateZero(canvas.getWidth(), canvas.getHeight());
                    doDraw(canvas);

                } finally {
                    if (canvas != null) {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }

        private void doDraw(Canvas canvas) {
            canvas.drawARGB(255, 200, 200, 200);
            // настройка кисти
            // красный цвет
            p.setColor(Color.RED);
            p.setStrokeWidth(textStroke);
            p.setStyle(Paint.Style.FILL);
            // создаем строку с значениями ширины и высоты канвы
            sb.setLength(0);
            sb.append("x = ").append(String.format("%.1f", cursorX))
                    .append(", y = ").append(String.format("%.1f", cursorY));
            canvas.drawText(sb.toString(), 100, 100, p);

            canvas.drawLine(0, zeroY, canvasW, zeroY, p);
            canvas.drawLine(zeroX, 0, zeroX, canvasH, p);

            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.GRAY);
            canvas.drawCircle(zeroX, zeroY, canvasW / 20, p);
            p.setStrokeWidth(stroke);
            p.setStyle(Paint.Style.STROKE);
            p.setColor(Color.BLUE);
            canvas.drawCircle(zeroX, zeroY, canvasW / 20, p);
            if (cx * cy != 0) {
                p.setStyle(Paint.Style.STROKE);
                p.setColor(Color.argb(255, 0, 128, 0));
                canvas.drawLine(0, cy, canvasW, cy, p);
                canvas.drawLine(cx, 0, cx, canvasH, p);
            }
        }
    }

}
