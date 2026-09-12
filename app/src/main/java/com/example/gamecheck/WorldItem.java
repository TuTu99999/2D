package com.example.gamecheck;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.Random;

/** X, Y and Z are reusable collision objects with timed random respawns. */
public class WorldItem {

    public enum Type {
        X_HAZARD,
        Y_BOOST,
        Z_TREASURE
    }

    private static final long RESPAWN_DELAY_MS = 3_500L;
    private static final Random RANDOM = new Random();

    private final Type type;
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path diamondPath = new Path();

    private float x;
    private float y;
    private final float radius;
    private boolean active = true;
    private long respawnAt;

    public WorldItem(Type type, float x, float y, float radius) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.radius = radius;

        int color = switch (type) {
            case X_HAZARD -> 0xFFE53935;
            case Y_BOOST -> 0xFF29B6F6;
            case Z_TREASURE -> 0xFFFFC107;
        };
        fillPaint.setColor(color);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(Math.max(2f, radius * 0.10f));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        textPaint.setTextSize(radius * 0.78f);
    }

    public void update(long now, float width, float height, float topInset) {
        if (active || now < respawnAt) return;
        float margin = radius * 2.2f;
        x = margin + RANDOM.nextFloat() * Math.max(1f, width - margin * 2f);
        y = topInset + margin
                + RANDOM.nextFloat() * Math.max(1f, height - topInset - margin * 2f);
        active = true;
    }

    public void collect(long now) {
        active = false;
        respawnAt = now + RESPAWN_DELAY_MS;
    }

    public boolean collidesWith(float targetX, float targetY, float targetRadius) {
        if (!active) return false;
        float dx = x - targetX;
        float dy = y - targetY;
        float range = radius + targetRadius;
        return dx * dx + dy * dy <= range * range;
    }

    public void draw(Canvas canvas) {
        if (!active) return;

        float pulse = 1f + 0.08f * (float) Math.sin(System.nanoTime() / 180_000_000.0);
        float drawRadius = radius * pulse;
        diamondPath.reset();
        diamondPath.moveTo(x, y - drawRadius);
        diamondPath.lineTo(x + drawRadius, y);
        diamondPath.lineTo(x, y + drawRadius);
        diamondPath.lineTo(x - drawRadius, y);
        diamondPath.close();
        canvas.drawPath(diamondPath, fillPaint);
        canvas.drawPath(diamondPath, borderPaint);

        String label = switch (type) {
            case X_HAZARD -> "X";
            case Y_BOOST -> "Y";
            case Z_TREASURE -> "Z";
        };
        float textY = y - (textPaint.ascent() + textPaint.descent()) / 2f;
        canvas.drawText(label, x, textY, textPaint);
    }

    public Type getType() {
        return type;
    }
}
