package com.example.gamecheck;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Projectile {

    private static final float SPEED = 620f;

    private final Paint trailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float radius;

    private float x;
    private float y;
    private final float speedX;
    private final float speedY;
    private boolean active = true;

    public Projectile(float startX, float startY, float targetX,
                      float targetY, float uiScale) {
        x = startX;
        y = startY;
        radius = 12f * uiScale;

        float differenceX = targetX - startX;
        float differenceY = targetY - startY;
        float distance = (float) Math.hypot(differenceX, differenceY);
        if (distance < 1f) {
            differenceX = 1f;
            differenceY = 0f;
            distance = 1f;
        }

        speedX = differenceX / distance * SPEED * uiScale;
        speedY = differenceY / distance * SPEED * uiScale;

        trailPaint.setColor(0x8896F8FF);
        trailPaint.setStrokeWidth(8f * uiScale);
        trailPaint.setStrokeCap(Paint.Cap.ROUND);
        bodyPaint.setColor(0xFF4DEAFF);
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        labelPaint.setTextSize(18f * uiScale);
    }

    public void update(float deltaTime, float screenWidth, float screenHeight) {
        x += speedX * deltaTime;
        y += speedY * deltaTime;

        float margin = radius * 4f;
        if (x < -margin || x > screenWidth + margin
                || y < -margin || y > screenHeight + margin) {
            active = false;
        }
    }

    public void draw(Canvas canvas) {
        float speedLength = (float) Math.hypot(speedX, speedY);
        float trailX = x - speedX / speedLength * radius * 3f;
        float trailY = y - speedY / speedLength * radius * 3f;

        canvas.drawLine(trailX, trailY, x, y, trailPaint);
        canvas.drawCircle(x, y, radius * 1.55f, trailPaint);
        canvas.drawCircle(x, y, radius, bodyPaint);

        float textY = y - radius * 2.2f
                - (labelPaint.ascent() + labelPaint.descent()) / 2f;
        canvas.drawText("C", x, textY, labelPaint);
    }

    public boolean isActive() {
        return active;
    }
}

