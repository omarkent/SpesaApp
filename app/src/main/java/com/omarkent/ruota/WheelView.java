package com.omarkent.ruota;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.Random;

public class WheelView extends View {
    public interface OnSpinEndListener {
        void onSpinEnd(String result);
    }

    private final String[] segments = {
            "100", "150", "200", "250", "300", "350", "400", "450",
            "500", "550", "600", "650", "700", "750", "800", "900",
            "1000", "PASSA", "200", "350", "BANCA ROTTA", "500", "700", "JACKPOT"
    };

    private final int[] palette = {
            Color.rgb(255, 193, 7), Color.rgb(0, 188, 212), Color.rgb(244, 67, 54),
            Color.rgb(76, 175, 80), Color.rgb(156, 39, 176), Color.rgb(255, 112, 67),
            Color.rgb(33, 150, 243), Color.rgb(205, 220, 57)
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private float rotationDegrees = 0f;
    private boolean spinning = false;
    private OnSpinEndListener listener;

    public WheelView(Context context) {
        super(context);
        init();
    }

    public WheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        linePaint.setColor(Color.argb(210, 255, 255, 255));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3f);
    }

    public void setOnSpinEndListener(OnSpinEndListener listener) {
        this.listener = listener;
    }

    public boolean isSpinning() {
        return spinning;
    }

    public void spin() {
        if (spinning) return;
        spinning = true;
        float extraTurns = 6f + random.nextInt(5);
        float randomStop = random.nextFloat() * 360f;
        final float start = rotationDegrees;
        final float end = start + extraTurns * 360f + randomStop;

        ValueAnimator animator = ValueAnimator.ofFloat(start, end);
        animator.setDuration(4200L);
        animator.setInterpolator(new DecelerateInterpolator(2.35f));
        animator.addUpdateListener(animation -> {
            rotationDegrees = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                rotationDegrees = normalize(rotationDegrees);
                spinning = false;
                int index = selectedIndex();
                if (listener != null) listener.onSpinEnd(segments[index]);
            }
        });
        animator.start();
    }

    private int selectedIndex() {
        float segmentAngle = 360f / segments.length;
        float target = normalize(-rotationDegrees);
        return ((int) Math.floor((target + segmentAngle / 2f) / segmentAngle)) % segments.length;
    }

    private float normalize(float value) {
        value %= 360f;
        if (value < 0) value += 360f;
        return value;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        int size = Math.min(w, h);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) size = 700;
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) size = 700;
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(w, h) * 0.45f;
        RectF rect = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
        float sweep = 360f / segments.length;

        canvas.save();
        canvas.rotate(rotationDegrees, cx, cy);
        for (int i = 0; i < segments.length; i++) {
            float start = -90f - sweep / 2f + i * sweep;
            String label = segments[i];
            if ("BANCA ROTTA".equals(label)) paint.setColor(Color.rgb(20, 20, 24));
            else if ("PASSA".equals(label)) paint.setColor(Color.rgb(90, 90, 100));
            else if ("JACKPOT".equals(label)) paint.setColor(Color.rgb(214, 0, 108));
            else paint.setColor(palette[i % palette.length]);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawArc(rect, start, sweep, true, paint);
            canvas.drawArc(rect, start, sweep, true, linePaint);

            canvas.save();
            canvas.rotate(start + sweep / 2f, cx, cy);
            textPaint.setTextSize(Math.max(18f, radius * 0.075f));
            String shown = label;
            if ("BANCA ROTTA".equals(label)) shown = "BANKRUPT";
            canvas.drawText(shown, cx, cy - radius * 0.67f, textPaint);
            canvas.restore();
        }
        canvas.restore();

        paint.setColor(Color.rgb(8, 19, 35));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, radius * 0.19f, paint);
        paint.setColor(Color.rgb(255, 215, 64));
        canvas.drawCircle(cx, cy, radius * 0.13f, paint);
        textPaint.setTextSize(radius * 0.095f);
        textPaint.setColor(Color.rgb(8, 19, 35));
        canvas.drawText("GIRA", cx, cy + textPaint.getTextSize() * 0.32f, textPaint);
        textPaint.setColor(Color.WHITE);

        Path pointer = new Path();
        pointer.moveTo(cx, cy - radius - radius * 0.06f);
        pointer.lineTo(cx - radius * 0.075f, cy - radius * 0.87f);
        pointer.lineTo(cx + radius * 0.075f, cy - radius * 0.87f);
        pointer.close();
        paint.setColor(Color.WHITE);
        paint.setShadowLayer(10f, 0f, 4f, Color.BLACK);
        canvas.drawPath(pointer, paint);
        paint.clearShadowLayer();
    }
}
