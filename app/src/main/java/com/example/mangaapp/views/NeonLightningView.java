package com.example.mangaapp.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class NeonLightningView extends View {

    private Paint lightningPaint;
    private Path lightningPath;
    private float drawProgress;
    private ValueAnimator animator;

    public NeonLightningView(Context context) {
        super(context);
        init();
    }

    public NeonLightningView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_HARDWARE, null);

        lightningPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lightningPaint.setStyle(Paint.Style.STROKE);
        lightningPaint.setStrokeWidth(6);
        lightningPaint.setColor(Color.parseColor("#C000FF"));
        lightningPaint.setPathEffect(new CornerPathEffect(8));

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(8888);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            drawProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();

        if (lightningPath == null) {
            // Увеличиваем шестиугольник
            float radius = Math.min(w, h) / 2f - 5;
            lightningPath = generateHexagonPath(w / 2f, h / 2f, radius);
        }

        PathMeasure pm = new PathMeasure(lightningPath, false);
        float len = pm.getLength();

        Path dst = new Path();
        pm.getSegment(0, len * drawProgress, dst, true);

        lightningPaint.setAlpha((int) (150 + 100 * Math.sin(drawProgress * Math.PI * 4)));
        canvas.drawPath(dst, lightningPaint);
    }

    private Path generateHexagonPath(float cx, float cy, float radius) {
        Path path = new Path();
        int segments = 6; // шестиугольник
        double angleStep = 2 * Math.PI / segments;

        for (int i = 0; i <= segments; i++) {
            double angle = i * angleStep - Math.PI / 2; // поворачиваем, чтобы вершина была вверх
            float x = (float) (cx + radius * Math.cos(angle));
            float y = (float) (cy + radius * Math.sin(angle));
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        path.close();
        return path;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) animator.cancel();
    }
}
