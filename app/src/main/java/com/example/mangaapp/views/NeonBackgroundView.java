package com.example.mangaapp.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.core.content.res.ResourcesCompat;
import com.example.mangaapp.R;

public class NeonBackgroundView extends View {

    private Paint textPaint;
    private Paint glowPaint;
    private Paint borderPaint;

    private ValueAnimator animator;
    private String text = "Manga4U";
    private float drawProgress;

    public NeonBackgroundView(Context context) {
        super(context);
        init(context);
    }

    public NeonBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setLayerType(LAYER_TYPE_HARDWARE, null);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4);
        borderPaint.setColor(Color.parseColor("#A020F0"));

        Typeface neonFont = ResourcesCompat.getFont(context, R.font.manga_font);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setStyle(Paint.Style.STROKE);
        textPaint.setStrokeWidth(6f);
        textPaint.setColor(Color.parseColor("#E070FF"));
        textPaint.setTextSize(150f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(neonFont);

        glowPaint = new Paint(textPaint);
        glowPaint.setStrokeWidth(12f);
        glowPaint.setColor(Color.parseColor("#9D00FF"));
        glowPaint.setAlpha(160);

        // Анімація вперед-назад
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(4000); // швидкість малювання
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE); // ось тут включено зворотній хід
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

        canvas.drawColor(Color.BLACK);

        RectF borderRect = new RectF(10, 10, w - 10, h - 10);
        borderPaint.setAlpha((int) (100 + 80 * Math.sin(drawProgress * Math.PI * 2)));
        canvas.drawRoundRect(borderRect, 40, 40, borderPaint);

        // Центр тексту
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float textHeight = fontMetrics.descent - fontMetrics.ascent;
        float centerY = h / 2f + textHeight / 4f;

        // DashPathEffect для малювання/стирки
        float textWidth = textPaint.measureText(text);
        float phase = textWidth * drawProgress;
        DashPathEffect dashEffect = new DashPathEffect(new float[]{textWidth, textWidth}, textWidth - phase);
        textPaint.setPathEffect(dashEffect);
        glowPaint.setPathEffect(dashEffect);

        canvas.drawText(text, w / 2f, centerY, glowPaint);
        canvas.drawText(text, w / 2f, centerY, textPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) animator.cancel();
    }
}
