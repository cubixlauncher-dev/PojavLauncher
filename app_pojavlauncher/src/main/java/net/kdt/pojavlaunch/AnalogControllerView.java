package net.kdt.pojavlaunch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import org.lwjgl.glfw.CallbackBridge;

public class AnalogControllerView extends View {

    private final Paint controllerRingPaint = new Paint();
    private final Paint controllerInnerPaint = new Paint();
    float innerX, innerY;

    private static final int BUTTON_W = 0;
    private static final int BUTTON_A = 1;
    private static final int BUTTON_S = 2;
    private static final int BUTTON_D = 3;
    private static final int BUTTON_CTRL = 4;

    private static final byte BUTTON_PRESSED = 0;
    private static final byte BUTTON_RELEASED = 1;

    private float DEADZONE_SIZE;

    byte[] whichButtonsPressed = new byte[5];
    byte[] whichButtonsPressedNew = new byte[5];
    private float widthHalf;
    private float heightHalf;
    private float centerCircleRadius;
    private float outerCircleRadius;
    private float borderRadius;

    public AnalogControllerView(Context context) {
        super(context);
        init();
    }

    public AnalogControllerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnalogControllerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public AnalogControllerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        controllerRingPaint.setColor(Color.argb(255, 255,255,255));
        controllerRingPaint.setStyle(Paint.Style.STROKE);
        controllerInnerPaint.setColor(Color.argb(70, 0,0,0));
        controllerInnerPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        canvas.drawCircle(widthHalf, heightHalf, outerCircleRadius, controllerInnerPaint);
        canvas.drawCircle(widthHalf, heightHalf, outerCircleRadius, controllerRingPaint);
        float adjustedInnerX = innerX;
        float adjustedInnerY = innerY;
        float xdiff = (adjustedInnerX - widthHalf);
        float ydiff = (adjustedInnerY - heightHalf);
        float abs = (float) Math.sqrt(xdiff * xdiff + ydiff * ydiff);
        if(abs > borderRadius && abs != 0) {
            adjustedInnerX = (innerX - widthHalf) * borderRadius / abs + widthHalf;
            adjustedInnerY = (innerY - widthHalf) * borderRadius / abs + heightHalf;
        }
        canvas.drawCircle(adjustedInnerX, adjustedInnerY, centerCircleRadius, controllerRingPaint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                innerX = widthHalf;
                innerY = heightHalf;
                break;
            case MotionEvent.ACTION_MOVE:
                innerX = event.getX();
                innerY = event.getY();
                break;
        }
        whichButtonsPressedNew[BUTTON_S] = innerY > heightHalf + DEADZONE_SIZE ? BUTTON_PRESSED : BUTTON_RELEASED;
        whichButtonsPressedNew[BUTTON_W] = innerY < heightHalf - DEADZONE_SIZE ? BUTTON_PRESSED : BUTTON_RELEASED;
        whichButtonsPressedNew[BUTTON_D] = innerX > widthHalf + DEADZONE_SIZE ? BUTTON_PRESSED : BUTTON_RELEASED;
        whichButtonsPressedNew[BUTTON_A] = innerX < widthHalf - DEADZONE_SIZE ? BUTTON_PRESSED : BUTTON_RELEASED;
        whichButtonsPressedNew[BUTTON_CTRL] = innerY < heightHalf - (DEADZONE_SIZE * 2) ? BUTTON_PRESSED : BUTTON_RELEASED;
        updateButtons();
        invalidate();
        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        DisplayMetrics metrics = new DisplayMetrics();
        getDisplay().getMetrics(metrics);
        DEADZONE_SIZE = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, metrics);
        controllerRingPaint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, metrics));
        outerCircleRadius = (w / 2f) - controllerRingPaint.getStrokeWidth();
        centerCircleRadius = w / 8f;
        borderRadius = outerCircleRadius - centerCircleRadius;
        widthHalf = w / 2f;
        heightHalf = h / 2f;
        if(innerX == 0) innerX = widthHalf;
        if(innerY == 0) innerY = heightHalf;
    }

    public int toKeycode(int index) {
        switch (index) {
            case BUTTON_A:
                return LwjglGlfwKeycode.GLFW_KEY_A;
            case BUTTON_D:
                return LwjglGlfwKeycode.GLFW_KEY_D;
            case BUTTON_S:
                return LwjglGlfwKeycode.GLFW_KEY_S;
            case BUTTON_W:
                return LwjglGlfwKeycode.GLFW_KEY_W;
            case BUTTON_CTRL:
                return LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL;
        }
        return -1;
    }

    public void updateButtons() {
        for(int i = 0; i < 5; i++) {
            if(whichButtonsPressedNew[i] != whichButtonsPressed[i]) {
                boolean isDown = whichButtonsPressedNew[i] == BUTTON_PRESSED;
                int kc = toKeycode(i);
                CallbackBridge.setModifiers(kc, isDown);
                CallbackBridge.sendKeyPress(kc, CallbackBridge.getCurrentMods(), isDown);
                whichButtonsPressed[i] = whichButtonsPressedNew[i];
            }
        }
    }
}
