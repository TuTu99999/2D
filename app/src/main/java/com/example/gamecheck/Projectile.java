package com.example.gamecheck;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

/** One class exposes three visibly and mechanically different attacks. */
public class Projectile {

    public enum Type {
        BULLET,
        MISSILE,
        BOMB
    }

    private final Type type;
    private final Paint primaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint secondaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shape = new Path();
    private final float radius;
    private final float baseSpeed;
    private final int damage;

    private float x;
    private float y;
    private float speedX;
    private float speedY;
    private float elapsed;
    private boolean active = true;
    private boolean exploding;
    private boolean explosionSoundPending;
    private boolean damageConsumed;

    public Projectile(Type type, float startX, float startY,
                      float targetX, float targetY, float uiScale) {
        this.type = type;
        float scale = Math.max(0.35f, uiScale);
        x = startX;
        y = startY;

        baseSpeed = switch (type) {
            case BULLET -> 760f * scale;
            case MISSILE -> 485f * scale;
            case BOMB -> 335f * scale;
        };
        radius = switch (type) {
            case BULLET -> 10f * scale;
            case MISSILE -> 15f * scale;
            case BOMB -> 19f * scale;
        };
        damage = switch (type) {
            case BULLET -> 12;
            case MISSILE -> 25;
            case BOMB -> 38;
        };

        setDirection(targetX - startX, targetY - startY, baseSpeed);
        primaryPaint.setColor(switch (type) {
            case BULLET -> 0xFF4DEAFF;
            case MISSILE -> 0xFFFFA726;
            case BOMB -> 0xFF263238;
        });
        secondaryPaint.setColor(switch (type) {
            case BULLET -> 0x8896F8FF;
            case MISSILE -> 0xFFFFF176;
            case BOMB -> 0xFFFF5722;
        });
        secondaryPaint.setStrokeWidth(Math.max(3f, radius * 0.55f));
        secondaryPaint.setStrokeCap(Paint.Cap.ROUND);
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        labelPaint.setTextSize(15f * scale);
    }

    public void update(float deltaTime, float screenWidth, float screenHeight,
                       float enemyX, float enemyY) {
        if (!active) return;
        elapsed += deltaTime;

        if (type == Type.MISSILE) {
            float desiredX = enemyX - x;
            float desiredY = enemyY - y;
            float length = Math.max(1f, (float) Math.hypot(desiredX, desiredY));
            desiredX = desiredX / length * baseSpeed;
            desiredY = desiredY / length * baseSpeed;
            float steering = Math.min(1f, deltaTime * 3.6f);
            speedX += (desiredX - speedX) * steering;
            speedY += (desiredY - speedY) * steering;
        }

        if (type == Type.BOMB) {
            speedX *= Math.max(0f, 1f - deltaTime * 0.75f);
            speedY *= Math.max(0f, 1f - deltaTime * 0.75f);
            if (!exploding && elapsed >= 1.05f) {
                exploding = true;
                explosionSoundPending = true;
                speedX = 0f;
                speedY = 0f;
            }
            if (exploding && elapsed >= 1.38f) active = false;
        }

        if (!exploding) {
            x += speedX * deltaTime;
            y += speedY * deltaTime;
        }

        float margin = radius * 6f;
        if (type != Type.BOMB && (x < -margin || x > screenWidth + margin
                || y < -margin || y > screenHeight + margin)) {
            active = false;
        }
    }

    public void draw(Canvas canvas) {
        if (!active) return;

        if (exploding) {
            float progress = Math.min(1f, (elapsed - 1.05f) / 0.33f);
            float blastRadius = radius * (1.5f + progress * 4.2f);
            secondaryPaint.setAlpha(Math.max(0, 210 - Math.round(progress * 190)));
            canvas.drawCircle(x, y, blastRadius, secondaryPaint);
            primaryPaint.setColor(0xFFFFEB3B);
            canvas.drawCircle(x, y, blastRadius * 0.48f, primaryPaint);
            return;
        }

        float length = Math.max(1f, (float) Math.hypot(speedX, speedY));
        float directionX = speedX / length;
        float directionY = speedY / length;

        if (type == Type.BULLET) {
            canvas.drawLine(
                    x - directionX * radius * 4f,
                    y - directionY * radius * 4f,
                    x, y, secondaryPaint
            );
            canvas.drawCircle(x, y, radius, primaryPaint);
        } else if (type == Type.MISSILE) {
            canvas.drawLine(
                    x - directionX * radius * 3.4f,
                    y - directionY * radius * 3.4f,
                    x - directionX * radius,
                    y - directionY * radius,
                    secondaryPaint
            );
            float sideX = -directionY * radius * 0.75f;
            float sideY = directionX * radius * 0.75f;
            shape.reset();
            shape.moveTo(x + directionX * radius * 1.6f, y + directionY * radius * 1.6f);
            shape.lineTo(x - directionX * radius + sideX, y - directionY * radius + sideY);
            shape.lineTo(x - directionX * radius - sideX, y - directionY * radius - sideY);
            shape.close();
            canvas.drawPath(shape, primaryPaint);
        } else {
            secondaryPaint.setAlpha(255);
            canvas.drawCircle(x, y, radius * 1.18f, secondaryPaint);
            canvas.drawCircle(x, y, radius, primaryPaint);
            canvas.drawText("B", x,
                    y - (labelPaint.ascent() + labelPaint.descent()) / 2f,
                    labelPaint);
        }
    }

    public boolean canDamage(float targetX, float targetY, float targetRadius) {
        if (!active || damageConsumed) return false;
        if (type == Type.BOMB && !exploding) return false;
        float hitRadius = exploding ? radius * 5.4f : radius;
        float dx = targetX - x;
        float dy = targetY - y;
        float range = hitRadius + targetRadius;
        return dx * dx + dy * dy <= range * range;
    }

    public void consumeDamage() {
        damageConsumed = true;
        if (type != Type.BOMB) active = false;
    }

    public boolean consumeExplosionSoundRequest() {
        if (!explosionSoundPending) return false;
        explosionSoundPending = false;
        return true;
    }

    public boolean isActive() {
        return active;
    }

    public int getDamage() {
        return damage;
    }

    private void setDirection(float differenceX, float differenceY, float speed) {
        float distance = (float) Math.hypot(differenceX, differenceY);
        if (distance < 1f) {
            differenceX = 1f;
            differenceY = 0f;
            distance = 1f;
        }
        speedX = differenceX / distance * speed;
        speedY = differenceY / distance * speed;
    }
}
