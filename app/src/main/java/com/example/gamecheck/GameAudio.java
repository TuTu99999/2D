package com.example.gamecheck;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

/** Keeps short sound effects separate from the looping background music. */
public class GameAudio {

    private final SoundPool soundPool;
    private final int bulletSound;
    private final int missileSound;
    private final int bombSound;
    private final int warningSound;
    private final MediaPlayer backgroundMusic;

    private boolean effectsEnabled = true;
    private boolean musicEnabled;
    private boolean gameScreenActive;
    private boolean lifecyclePaused;
    private int warningStreamId;

    public GameAudio(Context context) {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(attributes)
                .build();

        bulletSound = soundPool.load(context, R.raw.shot, 1);
        missileSound = soundPool.load(context, R.raw.missile, 1);
        bombSound = soundPool.load(context, R.raw.bomb, 1);
        warningSound = soundPool.load(context, R.raw.warning, 1);

        backgroundMusic = MediaPlayer.create(context, R.raw.background_music);
        if (backgroundMusic != null) {
            backgroundMusic.setLooping(true);
            backgroundMusic.setVolume(0.38f, 0.38f);
        }
    }

    public void playAttack(Projectile.Type type) {
        if (!effectsEnabled) return;

        int soundId = switch (type) {
            case BULLET -> bulletSound;
            case MISSILE -> missileSound;
            case BOMB -> bombSound;
        };
        float volume = type == Projectile.Type.BOMB ? 0.72f : 0.58f;
        soundPool.play(soundId, volume, volume, 1, 0, 1f);
    }

    public void playBombExplosion() {
        if (!effectsEnabled) return;
        soundPool.play(bombSound, 0.82f, 0.82f, 2, 0, 0.88f);
    }

    /** loop=3 means one initial play plus three repeats: four warnings total. */
    public void playWarningBurst() {
        stopWarning();
        if (!effectsEnabled) return;
        warningStreamId = soundPool.play(
                warningSound, 0.78f, 0.78f, 3, 3, 1.15f
        );
    }

    public void stopWarning() {
        if (warningStreamId != 0) {
            soundPool.stop(warningStreamId);
            warningStreamId = 0;
        }
    }

    public void toggleEffects() {
        effectsEnabled = !effectsEnabled;
        if (!effectsEnabled) stopWarning();
    }

    public void toggleMusic() {
        musicEnabled = !musicEnabled;
        updateMusicPlayback();
    }

    public boolean areEffectsEnabled() {
        return effectsEnabled;
    }

    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    public void setGameScreenActive(boolean active) {
        gameScreenActive = active;
        if (!active) stopWarning();
        updateMusicPlayback();
    }

    public void onPause() {
        lifecyclePaused = true;
        stopWarning();
        pauseMusic();
    }

    public void onResume() {
        lifecyclePaused = false;
        updateMusicPlayback();
    }

    public void release() {
        stopWarning();
        soundPool.release();
        if (backgroundMusic != null) {
            backgroundMusic.release();
        }
    }

    private void updateMusicPlayback() {
        if (backgroundMusic == null) return;

        if (musicEnabled && gameScreenActive && !lifecyclePaused) {
            try {
                if (!backgroundMusic.isPlaying()) backgroundMusic.start();
            } catch (IllegalStateException ignored) {
                // The view may be detaching while Android releases audio.
            }
        } else {
            pauseMusic();
        }
    }

    private void pauseMusic() {
        if (backgroundMusic == null) return;
        try {
            if (backgroundMusic.isPlaying()) backgroundMusic.pause();
        } catch (IllegalStateException ignored) {
            // The player is already being released.
        }
    }
}
