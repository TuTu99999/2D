package com.example.gamecheck;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;

public class GameView extends View {

    private enum Screen {
        SPLASH,
        HOME,
        GAME
    }

    private enum StartEdges {
        LEFT_AND_RIGHT,
        TOP_AND_BOTTOM
    }

    private static final long SPLASH_DURATION = 2_400L;
    private static final long SHIELD_DURATION = 4_000L;
    private static final long SHIELD_COOLDOWN = 7_000L;
    private static final long FREEZE_DURATION = 3_000L;
    private static final long FREEZE_COOLDOWN = 8_000L;
    private static final long SPEED_BOOST_DURATION = 5_000L;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shieldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint joystickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF playButton = new RectF();
    private final RectF exitButton = new RectF();
    private final RectF homeButton = new RectF();
    private final RectF soundToggleButton = new RectF();
    private final RectF musicToggleButton = new RectF();
    private final RectF bulletButton = new RectF();
    private final RectF missileButton = new RectF();
    private final RectF bombButton = new RectF();
    private final RectF shieldButton = new RectF();
    private final RectF freezeButton = new RectF();

    private final Bitmap logo;
    private final Bitmap homeBackground;
    private final Bitmap adventurer;
    private final Bitmap enemy;
    private final AnimatedBackground animatedBackground;
    private final GameAudio audio;
    private final ArrayList<Projectile> projectiles = new ArrayList<>();
    private final ArrayList<WorldItem> worldItems = new ArrayList<>();

    private Screen currentScreen = Screen.SPLASH;
    private StartEdges currentStartEdges = StartEdges.LEFT_AND_RIGHT;
    private long splashStartedAt = SystemClock.uptimeMillis();
    private long gameStartedAt;
    private long lastFrameAt;
    private boolean animationRunning = true;
    private boolean useVerticalStartNext;
    private boolean audioReleased;
    private MovingGameObject objectA;
    private MovingGameObject objectB;

    private Projectile.Type selectedAttack = Projectile.Type.BULLET;
    private int health = 100;
    private int armor = 60;
    private int gold;
    private int coins;
    private int score;
    private int enemyHealth = 100;
    private float speedMultiplier = 1f;
    private long speedBoostUntil;
    private long shieldUntil;
    private long shieldCooldownUntil;
    private long freezeUntil;
    private long freezeCooldownUntil;
    private long enemyCollisionAllowedAt;
    private boolean initialEnemyWarningPending;

    private boolean joystickActive;
    private float joystickCenterX;
    private float joystickCenterY;
    private float joystickKnobX;
    private float joystickKnobY;
    private float joystickRadius;

    private String eventMessage = "Use joystick to move A";
    private long eventMessageUntil;
    private int eventColor = Color.WHITE;
    private long impactUntil;
    private int impactColor;

    public GameView(Context context) {
        super(context);
        setFocusable(true);

        logo = BitmapFactory.decodeResource(getResources(), R.drawable.game_logo);
        homeBackground = BitmapFactory.decodeResource(getResources(), R.drawable.home_background);
        adventurer = BitmapFactory.decodeResource(getResources(), R.drawable.adventurer);
        enemy = BitmapFactory.decodeResource(getResources(), R.drawable.enemy_b);
        animatedBackground = new AnimatedBackground(context, R.drawable.forest_background);
        audio = new GameAudio(context);

        textPaint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.SANS_SERIF,
                android.graphics.Typeface.BOLD
        ));
        shieldPaint.setStyle(Paint.Style.STROKE);
        shieldPaint.setStrokeWidth(6f);
        joystickPaint.setStrokeWidth(4f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        switch (currentScreen) {
            case SPLASH -> drawSplash(canvas);
            case HOME -> drawHome(canvas);
            case GAME -> drawGame(canvas);
        }

        if (animationRunning && currentScreen != Screen.HOME) {
            postInvalidateOnAnimation();
        }
    }

    private void drawSplash(Canvas canvas) {
        long elapsed = SystemClock.uptimeMillis() - splashStartedAt;
        float progress = Math.min(1f, elapsed / (float) SPLASH_DURATION);
        float scale = getUiScale(canvas);

        paint.setShader(new LinearGradient(
                0, 0, canvas.getWidth(), canvas.getHeight(),
                Color.rgb(8, 17, 36), Color.rgb(41, 28, 74),
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
        paint.setShader(null);

        float logoSize = 230f * scale;
        drawRoundImage(canvas, logo, canvas.getWidth() / 2f,
                canvas.getHeight() * 0.39f, logoSize);
        drawCenteredText(canvas, "JOURNEY OF THE MONSTER SLAYER",
                canvas.getHeight() * 0.67f, 42f * scale, Color.WHITE);
        drawCenteredText(canvas, "Preparing movement and combat demo...",
                canvas.getHeight() * 0.74f, 22f * scale, 0xFFCBD5E1);

        float barWidth = 560f * scale;
        float barHeight = 22f * scale;
        float left = (canvas.getWidth() - barWidth) / 2f;
        float top = canvas.getHeight() * 0.82f;
        RectF bar = new RectF(left, top, left + barWidth, top + barHeight);
        paint.setColor(0x55334155);
        canvas.drawRoundRect(bar, barHeight / 2f, barHeight / 2f, paint);
        paint.setColor(0xFFF4C95D);
        canvas.drawRoundRect(left, top, left + barWidth * progress,
                top + barHeight, barHeight / 2f, barHeight / 2f, paint);

        if (progress >= 1f) {
            currentScreen = Screen.HOME;
            invalidate();
        }
    }

    private void drawHome(Canvas canvas) {
        drawCoverImage(canvas, homeBackground);
        paint.setColor(0x80030A18);
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);

        float scale = getUiScale(canvas);
        float logoSize = 150f * scale;
        drawRoundImage(canvas, logo, canvas.getWidth() * 0.16f,
                canvas.getHeight() * 0.22f, logoSize);
        drawCenteredText(canvas, "JOURNEY OF THE MONSTER SLAYER",
                canvas.getHeight() * 0.30f, 48f * scale, Color.WHITE);
        drawCenteredText(canvas, "Movement, combat, defense and audio demo",
                canvas.getHeight() * 0.40f, 24f * scale, 0xFFE2E8F0);

        float buttonWidth = 380f * scale;
        float buttonHeight = 82f * scale;
        float centerX = canvas.getWidth() / 2f;
        float firstTop = canvas.getHeight() * 0.53f;
        playButton.set(centerX - buttonWidth / 2f, firstTop,
                centerX + buttonWidth / 2f, firstTop + buttonHeight);
        exitButton.set(centerX - buttonWidth / 2f,
                firstTop + buttonHeight + 24f * scale,
                centerX + buttonWidth / 2f,
                firstTop + buttonHeight * 2f + 24f * scale);
        drawButton(canvas, playButton, "PLAY", 0xFFE0A82E, scale, 28f);
        drawButton(canvas, exitButton, "EXIT", 0xAA23324A, scale, 28f);
    }

    private void drawGame(Canvas canvas) {
        long now = SystemClock.uptimeMillis();
        animatedBackground.draw(canvas, now - gameStartedAt);

        float scale = getUiScale(canvas);
        ensureGameObjects(canvas, scale);
        layoutGameControls(canvas, scale);
        updateGameObjects(canvas, now, scale);

        for (WorldItem item : worldItems) item.draw(canvas);
        for (Projectile projectile : projectiles) projectile.draw(canvas);

        objectB.draw(canvas, scale);
        if (isEnemyFrozen(now)) drawFrozenEffect(canvas, objectB, scale);
        objectA.draw(canvas, scale);
        if (isShieldActive(now)) drawShield(canvas, objectA, scale, now);
        drawImpactEffect(canvas, scale, now);

        drawJoystick(canvas, scale);
        drawActionButtons(canvas, scale, now);
        drawHud(canvas, scale, now);
    }

    private void ensureGameObjects(Canvas canvas, float scale) {
        if (objectA != null && objectB != null) return;

        float objectSize = 112f * scale;
        float halfSize = objectSize / 2f;
        float centerX = canvas.getWidth() / 2f;
        float centerY = canvas.getHeight() / 2f;
        boolean horizontal = currentStartEdges == StartEdges.LEFT_AND_RIGHT;

        float objectAX = horizontal ? halfSize : centerX;
        float objectAY = horizontal ? centerY : halfSize;
        float objectBX = horizontal ? canvas.getWidth() - halfSize : centerX;
        float objectBY = horizontal ? centerY : canvas.getHeight() - halfSize;

        objectA = new MovingGameObject(
                adventurer, objectAX, objectAY, objectSize,
                0f, 0f, "A", 0xFF2563EB,
                MovingGameObject.MovementType.PLAYER_CLAMP
        );
        objectB = new MovingGameObject(
                enemy, objectBX, objectBY, objectSize,
                -165f * scale, -115f * scale, "B", 0xFFDC2626,
                MovingGameObject.MovementType.WRAP_LEFT_AND_TOP
        );
        initialEnemyWarningPending = true;

        worldItems.clear();
        float itemRadius = 25f * scale;
        worldItems.add(new WorldItem(WorldItem.Type.X_HAZARD,
                canvas.getWidth() * 0.31f, canvas.getHeight() * 0.40f, itemRadius));
        worldItems.add(new WorldItem(WorldItem.Type.Y_BOOST,
                canvas.getWidth() * 0.55f, canvas.getHeight() * 0.68f, itemRadius));
        worldItems.add(new WorldItem(WorldItem.Type.Z_TREASURE,
                canvas.getWidth() * 0.73f, canvas.getHeight() * 0.43f, itemRadius));
    }

    private void layoutGameControls(Canvas canvas, float scale) {
        float margin = 18f * scale;
        float topButtonHeight = 43f * scale;
        homeButton.set(margin, margin, margin + 112f * scale,
                margin + topButtonHeight);

        float toggleWidth = 132f * scale;
        float toggleGap = 10f * scale;
        musicToggleButton.set(
                canvas.getWidth() - margin - toggleWidth,
                margin,
                canvas.getWidth() - margin,
                margin + topButtonHeight
        );
        soundToggleButton.set(
                musicToggleButton.left - toggleGap - toggleWidth,
                margin,
                musicToggleButton.left - toggleGap,
                margin + topButtonHeight
        );

        joystickRadius = 74f * scale;
        joystickCenterX = 105f * scale;
        joystickCenterY = canvas.getHeight() - 100f * scale;
        if (!joystickActive) {
            joystickKnobX = joystickCenterX;
            joystickKnobY = joystickCenterY;
        }

        float actionWidth = 105f * scale;
        float actionHeight = 52f * scale;
        float gap = 9f * scale;
        float right = canvas.getWidth() - margin;
        float attackTop = canvas.getHeight() - 130f * scale;
        bombButton.set(right - actionWidth, attackTop, right, attackTop + actionHeight);
        missileButton.set(bombButton.left - gap - actionWidth, attackTop,
                bombButton.left - gap, attackTop + actionHeight);
        bulletButton.set(missileButton.left - gap - actionWidth, attackTop,
                missileButton.left - gap, attackTop + actionHeight);

        float defenseTop = canvas.getHeight() - 68f * scale;
        freezeButton.set(right - actionWidth, defenseTop, right, defenseTop + actionHeight);
        shieldButton.set(freezeButton.left - gap - actionWidth, defenseTop,
                freezeButton.left - gap, defenseTop + actionHeight);

    }

    private void updateGameObjects(Canvas canvas, long now, float scale) {
        if (lastFrameAt == 0L) lastFrameAt = now;
        float deltaTime = Math.min(0.04f, (now - lastFrameAt) / 1000f);
        lastFrameAt = now;

        if (speedMultiplier > 1f && now >= speedBoostUntil) {
            speedMultiplier = 1f;
            showEvent("Speed boost finished", Color.WHITE, now);
        }
        objectA.update(deltaTime, canvas.getWidth(), canvas.getHeight());
        if (!isEnemyFrozen(now)) {
            objectB.update(deltaTime, canvas.getWidth(), canvas.getHeight());
        }

        float topInset = 105f * scale;
        for (WorldItem item : worldItems) {
            item.update(now, canvas.getWidth(), canvas.getHeight(), topInset);
            if (item.collidesWith(objectA.getX(), objectA.getY(), objectA.getSize() * 0.30f)) {
                applyWorldItem(item, now);
            }
        }

        Iterator<Projectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            projectile.update(deltaTime, canvas.getWidth(), canvas.getHeight(),
                    objectB.getX(), objectB.getY());
            if (projectile.consumeExplosionSoundRequest()) audio.playBombExplosion();
            if (projectile.canDamage(objectB.getX(), objectB.getY(),
                    objectB.getSize() * 0.30f)) {
                enemyHealth -= projectile.getDamage();
                score += projectile.getDamage();
                projectile.consumeDamage();
                showEvent("B hit: -" + projectile.getDamage() + " HP",
                        0xFFFFD54F, now);
                if (enemyHealth <= 0) {
                    enemyHealth = 100;
                    score += 200;
                    showEvent("Enemy B defeated: +200 score", 0xFFFFD54F, now);
                }
            }
            if (!projectile.isActive()) iterator.remove();
        }

        if ((initialEnemyWarningPending || objectB.consumeEnteredFromOppositeEdge())
                && !isEnemyFrozen(now)) {
            initialEnemyWarningPending = false;
            audio.playWarningBurst();
            showEvent("WARNING: B entered the game screen (4 alerts)",
                    0xFFFF5252, now);
        }

        float dx = objectA.getX() - objectB.getX();
        float dy = objectA.getY() - objectB.getY();
        float collisionRange = objectA.getSize() * 0.58f;
        if (dx * dx + dy * dy <= collisionRange * collisionRange
                && now >= enemyCollisionAllowedAt) {
            enemyCollisionAllowedAt = now + 900L;
            if (isShieldActive(now)) {
                shieldUntil = 0L;
                showEvent("Shield blocked B and was consumed", 0xFF80DEEA, now);
            } else if (armor > 0) {
                armor = Math.max(0, armor - 8);
                showEvent("Enemy collision: armor -8", 0xFFFF8A80, now);
            } else {
                health = Math.max(0, health - 12);
                showEvent("Enemy collision: HP -12", 0xFFFF5252, now);
            }
        }

        if (health <= 0) {
            health = 100;
            armor = 30;
            score = Math.max(0, score - 100);
            showEvent("A recovered: score -100", 0xFFFF8A80, now);
        }
    }

    private void applyWorldItem(WorldItem item, long now) {
        item.collect(now);
        impactUntil = now + 430L;

        switch (item.getType()) {
            case X_HAZARD -> {
                impactColor = 0xFFE53935;
                if (isShieldActive(now)) {
                    shieldUntil = 0L;
                    armor = Math.max(0, armor - 5);
                    showEvent("X exploded: shield lost, armor -5", 0xFFFF5252, now);
                } else {
                    health = Math.max(0, health - 15);
                    armor = Math.max(0, armor - 10);
                    showEvent("X exploded: HP -15, armor -10", 0xFFFF5252, now);
                }
            }
            case Y_BOOST -> {
                impactColor = 0xFF29B6F6;
                speedMultiplier = 1.5f;
                speedBoostUntil = now + SPEED_BOOST_DURATION;
                armor = Math.min(100, armor + 20);
                showEvent("Y collected: speed x1.5, armor +20", 0xFF40C4FF, now);
            }
            case Z_TREASURE -> {
                impactColor = 0xFFFFC107;
                gold += 10;
                coins += 5;
                score += 100;
                showEvent("Z collected: gold +10, coins +5, score +100",
                        0xFFFFD54F, now);
            }
        }
    }

    private void fireAttack(Projectile.Type type, float targetX, float targetY, long now) {
        if (objectA == null) return;
        selectedAttack = type;
        if (projectiles.size() >= 40) projectiles.remove(0);
        projectiles.add(new Projectile(
                type, objectA.getX(), objectA.getY(), targetX, targetY,
                Math.min(getWidth() / 1280f, getHeight() / 720f)
        ));
        audio.playAttack(type);
        String description = switch (type) {
            case BULLET -> "Bullet fired: fast straight shot";
            case MISSILE -> "Missile fired: homing attack";
            case BOMB -> "Bomb thrown: delayed area explosion";
        };
        showEvent(description, 0xFF80DEEA, now);
    }

    private void activateShield(long now) {
        if (now < shieldCooldownUntil) {
            showEvent("Shield is cooling down", 0xFFB0BEC5, now);
            return;
        }
        shieldUntil = now + SHIELD_DURATION;
        shieldCooldownUntil = now + SHIELD_COOLDOWN;
        showEvent("Shield active for 4 seconds", 0xFF80DEEA, now);
    }

    private void activateFreeze(long now) {
        if (now < freezeCooldownUntil) {
            showEvent("Freeze is cooling down", 0xFFB0BEC5, now);
            return;
        }
        freezeUntil = now + FREEZE_DURATION;
        freezeCooldownUntil = now + FREEZE_COOLDOWN;
        audio.stopWarning();
        showEvent("Enemy B disabled for 3 seconds", 0xFF82B1FF, now);
    }

    private boolean isShieldActive(long now) {
        return now < shieldUntil;
    }

    private boolean isEnemyFrozen(long now) {
        return now < freezeUntil;
    }

    private void drawShield(Canvas canvas, MovingGameObject target,
                            float scale, long now) {
        float pulse = 1f + 0.05f * (float) Math.sin(now / 90.0);
        shieldPaint.setColor(0xFF4DD0E1);
        shieldPaint.setStrokeWidth(5f * scale);
        canvas.drawCircle(target.getX(), target.getY(),
                target.getSize() * 0.57f * pulse, shieldPaint);
        shieldPaint.setColor(0x6680DEEA);
        shieldPaint.setStrokeWidth(11f * scale);
        canvas.drawCircle(target.getX(), target.getY(),
                target.getSize() * 0.52f * pulse, shieldPaint);
    }

    private void drawFrozenEffect(Canvas canvas, MovingGameObject target, float scale) {
        shieldPaint.setColor(0xFF82B1FF);
        shieldPaint.setStrokeWidth(6f * scale);
        canvas.drawCircle(target.getX(), target.getY(),
                target.getSize() * 0.55f, shieldPaint);
        drawCenteredTextAt(canvas, "FROZEN", target.getX(),
                target.getY() + target.getSize() * 0.67f,
                17f * scale, 0xFFBBDEFB);
    }

    private void drawImpactEffect(Canvas canvas, float scale, long now) {
        if (now >= impactUntil || objectA == null) return;
        float progress = 1f - (impactUntil - now) / 430f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(7f * scale);
        paint.setColor(impactColor);
        paint.setAlpha(Math.max(0, 230 - Math.round(progress * 210)));
        canvas.drawCircle(objectA.getX(), objectA.getY(),
                objectA.getSize() * (0.45f + progress * 0.9f), paint);
        paint.setAlpha(255);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawJoystick(Canvas canvas, float scale) {
        joystickPaint.setStyle(Paint.Style.FILL);
        joystickPaint.setColor(0x66121C2F);
        canvas.drawCircle(joystickCenterX, joystickCenterY,
                joystickRadius, joystickPaint);
        joystickPaint.setStyle(Paint.Style.STROKE);
        joystickPaint.setStrokeWidth(4f * scale);
        joystickPaint.setColor(0xFF90A4C3);
        canvas.drawCircle(joystickCenterX, joystickCenterY,
                joystickRadius, joystickPaint);
        joystickPaint.setStyle(Paint.Style.FILL);
        joystickPaint.setColor(0xCC4F6B95);
        canvas.drawCircle(joystickKnobX, joystickKnobY,
                joystickRadius * 0.42f, joystickPaint);
        drawCenteredTextAt(canvas, "MOVE", joystickCenterX,
                joystickCenterY - joystickRadius - 14f * scale,
                16f * scale, Color.WHITE);
    }

    private void drawActionButtons(Canvas canvas, float scale, long now) {
        drawSmallButton(canvas, bulletButton, "BULLET",
                selectedAttack == Projectile.Type.BULLET, 0xFF00838F, scale);
        drawSmallButton(canvas, missileButton, "MISSILE",
                selectedAttack == Projectile.Type.MISSILE, 0xFFEF6C00, scale);
        drawSmallButton(canvas, bombButton, "BOMB",
                selectedAttack == Projectile.Type.BOMB, 0xFF455A64, scale);

        String shieldText = now < shieldCooldownUntil && !isShieldActive(now)
                ? "SHIELD " + secondsLeft(shieldCooldownUntil, now)
                : isShieldActive(now) ? "SHIELD ON" : "SHIELD";
        String freezeText = now < freezeCooldownUntil && !isEnemyFrozen(now)
                ? "FREEZE " + secondsLeft(freezeCooldownUntil, now)
                : isEnemyFrozen(now) ? "FROZEN" : "FREEZE";
        drawSmallButton(canvas, shieldButton, shieldText,
                isShieldActive(now), 0xFF00796B, scale);
        drawSmallButton(canvas, freezeButton, freezeText,
                isEnemyFrozen(now), 0xFF3949AB, scale);
    }

    private void drawHud(Canvas canvas, float scale, long now) {
        paint.setColor(0xCC071323);
        canvas.drawRect(0, 0, canvas.getWidth(), 105f * scale, paint);
        drawButton(canvas, homeButton, "HOME", 0xAA17253D, scale, 17f);

        String soundLabel = audio.areEffectsEnabled() ? "SOUND OFF" : "SOUND ON";
        String musicLabel = audio.isMusicEnabled() ? "MUSIC OFF" : "MUSIC ON";
        drawButton(canvas, soundToggleButton, soundLabel,
                audio.areEffectsEnabled() ? 0xFF315C54 : 0xFF6D3742,
                scale, 15f);
        drawButton(canvas, musicToggleButton, musicLabel,
                audio.isMusicEnabled() ? 0xFF315C54 : 0xFF6D3742,
                scale, 15f);

        float statsLeft = homeButton.right + 24f * scale;
        drawHudText(canvas, "HP " + health + "/100", statsLeft,
                31f * scale, 19f * scale, 0xFFFF8A80);
        drawHudText(canvas, "ARMOR " + armor + "/100", statsLeft,
                62f * scale, 18f * scale, 0xFF80DEEA);
        drawHudText(canvas, "GOLD " + gold + "   COINS " + coins
                        + "   SCORE " + score,
                statsLeft + 185f * scale, 31f * scale,
                18f * scale, 0xFFFFD54F);
        drawHudText(canvas, "SPEED x" + String.format(java.util.Locale.US, "%.1f", speedMultiplier)
                        + "   B HP " + enemyHealth + "/100",
                statsLeft + 185f * scale, 62f * scale,
                17f * scale, 0xFFE0E7FF);

        String startInfo = currentStartEdges == StartEdges.LEFT_AND_RIGHT
                ? "START A: LEFT CENTER  |  B: RIGHT CENTER"
                : "START A: TOP CENTER  |  B: BOTTOM CENTER";
        drawHudText(canvas, startInfo, statsLeft,
                91f * scale, 14f * scale, 0xFFB0BEC5);

        if (now < eventMessageUntil || eventMessageUntil == 0L) {
            drawCenteredTextAt(canvas, eventMessage, canvas.getWidth() / 2f,
                    132f * scale, 19f * scale, eventColor);
        }
    }

    private int secondsLeft(long endTime, long now) {
        return Math.max(1, (int) Math.ceil((endTime - now) / 1000.0));
    }

    private void showEvent(String message, int color, long now) {
        eventMessage = message;
        eventColor = color;
        eventMessageUntil = now + 2_400L;
    }

    private void updateJoystick(float touchX, float touchY) {
        float dx = touchX - joystickCenterX;
        float dy = touchY - joystickCenterY;
        float distance = (float) Math.hypot(dx, dy);
        if (distance > joystickRadius) {
            dx = dx / distance * joystickRadius;
            dy = dy / distance * joystickRadius;
        }
        joystickKnobX = joystickCenterX + dx;
        joystickKnobY = joystickCenterY + dy;

        float movementSpeed = 260f * getUiScaleForView() * speedMultiplier;
        objectA.setVelocity(
                dx / joystickRadius * movementSpeed,
                dy / joystickRadius * movementSpeed
        );
    }

    private void stopJoystick() {
        joystickActive = false;
        joystickKnobX = joystickCenterX;
        joystickKnobY = joystickCenterY;
        if (objectA != null) objectA.setVelocity(0f, 0f);
    }

    private void startGame() {
        currentScreen = Screen.GAME;
        currentStartEdges = useVerticalStartNext
                ? StartEdges.TOP_AND_BOTTOM
                : StartEdges.LEFT_AND_RIGHT;
        useVerticalStartNext = !useVerticalStartNext;
        gameStartedAt = SystemClock.uptimeMillis();
        lastFrameAt = gameStartedAt;
        objectA = null;
        objectB = null;
        projectiles.clear();
        worldItems.clear();
        health = 100;
        armor = 60;
        gold = 0;
        coins = 0;
        score = 0;
        enemyHealth = 100;
        speedMultiplier = 1f;
        speedBoostUntil = 0L;
        shieldUntil = 0L;
        shieldCooldownUntil = 0L;
        freezeUntil = 0L;
        freezeCooldownUntil = 0L;
        initialEnemyWarningPending = true;
        eventMessage = "Joystick moves A; tap the field or an attack button";
        eventColor = Color.WHITE;
        eventMessageUntil = gameStartedAt + 4_000L;
        audio.setGameScreenActive(true);
        invalidate();
    }

    private void returnHome() {
        stopJoystick();
        currentScreen = Screen.HOME;
        projectiles.clear();
        audio.setGameScreenActive(false);
        invalidate();
    }

    private void drawSmallButton(Canvas canvas, RectF bounds, String label,
                                 boolean active, int color, float scale) {
        paint.setColor(active ? color : 0xCC1B2942);
        canvas.drawRoundRect(bounds, 10f * scale, 10f * scale, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth((active ? 4f : 2f) * scale);
        paint.setColor(active ? 0xFFFFD54F : 0xFF8294B4);
        canvas.drawRoundRect(bounds, 10f * scale, 10f * scale, paint);
        paint.setStyle(Paint.Style.FILL);
        drawCenteredTextAt(canvas, label, bounds.centerX(), bounds.centerY(),
                15f * scale, Color.WHITE);
    }

    private void drawButton(Canvas canvas, RectF bounds, String label,
                            int backgroundColor, float scale, float textSize) {
        paint.setColor(backgroundColor);
        canvas.drawRoundRect(bounds, 12f * scale, 12f * scale, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f * scale);
        paint.setColor(0xFFF4C95D);
        canvas.drawRoundRect(bounds, 12f * scale, 12f * scale, paint);
        paint.setStyle(Paint.Style.FILL);
        drawCenteredTextAt(canvas, label, bounds.centerX(), bounds.centerY(),
                textSize * scale, Color.WHITE);
    }

    private void drawRoundImage(Canvas canvas, Bitmap bitmap,
                                float centerX, float centerY, float size) {
        if (bitmap == null) return;
        canvas.save();
        Path circle = new Path();
        circle.addCircle(centerX, centerY, size / 2f, Path.Direction.CW);
        canvas.clipPath(circle);
        RectF destination = new RectF(
                centerX - size / 2f, centerY - size / 2f,
                centerX + size / 2f, centerY + size / 2f
        );
        canvas.drawBitmap(bitmap, null, destination, null);
        canvas.restore();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(3f, size * 0.025f));
        paint.setColor(0xFFF4C95D);
        canvas.drawCircle(centerX, centerY, size / 2f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawCoverImage(Canvas canvas, Bitmap bitmap) {
        if (bitmap == null) {
            canvas.drawColor(Color.rgb(8, 17, 36));
            return;
        }
        float viewRatio = (float) canvas.getWidth() / canvas.getHeight();
        float imageRatio = (float) bitmap.getWidth() / bitmap.getHeight();
        Rect source;
        if (imageRatio > viewRatio) {
            int sourceWidth = Math.round(bitmap.getHeight() * viewRatio);
            int left = (bitmap.getWidth() - sourceWidth) / 2;
            source = new Rect(left, 0, left + sourceWidth, bitmap.getHeight());
        } else {
            int sourceHeight = Math.round(bitmap.getWidth() / viewRatio);
            int top = (bitmap.getHeight() - sourceHeight) / 2;
            source = new Rect(0, top, bitmap.getWidth(), top + sourceHeight);
        }
        canvas.drawBitmap(bitmap, source,
                new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), paint);
    }

    private void drawCenteredText(Canvas canvas, String text, float y,
                                  float size, int color) {
        drawCenteredTextAt(canvas, text, canvas.getWidth() / 2f, y, size, color);
    }

    private void drawCenteredTextAt(Canvas canvas, String text, float x, float centerY,
                                    float size, int color) {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(size);
        textPaint.setColor(color);
        float baseline = centerY - (textPaint.ascent() + textPaint.descent()) / 2f;
        canvas.drawText(text, x, baseline, textPaint);
    }

    private void drawHudText(Canvas canvas, String text, float x, float centerY,
                             float size, int color) {
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(size);
        textPaint.setColor(color);
        float baseline = centerY - (textPaint.ascent() + textPaint.descent()) / 2f;
        canvas.drawText(text, x, baseline, textPaint);
    }

    private float getUiScale(Canvas canvas) {
        return Math.min(canvas.getWidth() / 1280f, canvas.getHeight() / 720f);
    }

    private float getUiScaleForView() {
        return Math.min(getWidth() / 1280f, getHeight() / 720f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (currentScreen == Screen.HOME) {
            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                if (playButton.contains(event.getX(), event.getY())) {
                    startGame();
                } else if (exitButton.contains(event.getX(), event.getY())) {
                    ((Activity) getContext()).finish();
                }
            }
            return true;
        }
        if (currentScreen != Screen.GAME) return true;

        float x = event.getX();
        float y = event.getY();
        long now = SystemClock.uptimeMillis();
        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN) {
            if (homeButton.contains(x, y)) {
                returnHome();
            } else if (soundToggleButton.contains(x, y)) {
                audio.toggleEffects();
                showEvent(audio.areEffectsEnabled()
                                ? "Short sound effects ON" : "Short sound effects OFF",
                        Color.WHITE, now);
            } else if (musicToggleButton.contains(x, y)) {
                audio.toggleMusic();
                showEvent(audio.isMusicEnabled()
                                ? "Background music ON" : "Background music OFF",
                        Color.WHITE, now);
            } else if (bulletButton.contains(x, y)) {
                fireAttack(Projectile.Type.BULLET, objectB.getX(), objectB.getY(), now);
            } else if (missileButton.contains(x, y)) {
                fireAttack(Projectile.Type.MISSILE, objectB.getX(), objectB.getY(), now);
            } else if (bombButton.contains(x, y)) {
                fireAttack(Projectile.Type.BOMB, objectB.getX(), objectB.getY(), now);
            } else if (shieldButton.contains(x, y)) {
                activateShield(now);
            } else if (freezeButton.contains(x, y)) {
                activateFreeze(now);
            } else {
                float dx = x - joystickCenterX;
                float dy = y - joystickCenterY;
                if (dx * dx + dy * dy <= joystickRadius * joystickRadius * 2.0f) {
                    joystickActive = true;
                    updateJoystick(x, y);
                } else if (y > 145f * getUiScaleForView()) {
                    fireAttack(selectedAttack, x, y, now);
                }
            }
        } else if (action == MotionEvent.ACTION_MOVE && joystickActive) {
            updateJoystick(x, y);
        } else if (action == MotionEvent.ACTION_UP
                || action == MotionEvent.ACTION_CANCEL) {
            if (joystickActive) stopJoystick();
        }
        invalidate();
        return true;
    }

    public void pauseAnimation() {
        animationRunning = false;
        stopJoystick();
        audio.onPause();
    }

    public void resumeAnimation() {
        animationRunning = true;
        audio.onResume();
        if (currentScreen == Screen.SPLASH) {
            splashStartedAt = SystemClock.uptimeMillis();
        } else if (currentScreen == Screen.GAME) {
            gameStartedAt = SystemClock.uptimeMillis();
            lastFrameAt = gameStartedAt;
        }
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!audioReleased) {
            audioReleased = true;
            audio.release();
        }
        super.onDetachedFromWindow();
    }
}
