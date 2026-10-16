
package battleship;

import com.adonax.audiocue.AudioCue;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SoundManager {

    private final AudioCue shot;
    private final AudioCue miss;
    private final AudioCue hit;
    private final AudioCue sunk;

    private final long shotDuration;
    private final long missDuration;
    private final long hitDuration;
    private final long sunkDuration;

    private final BlockingQueue<Sound> soundQueue =
            new LinkedBlockingQueue<>();

    private volatile boolean running = true;
    private final Thread soundThread;

    private record Sound(AudioCue cue, long duration) {}

    public SoundManager() {
        try {
            shotDuration = getDuration("/shot.wav");
            missDuration = getDuration("/miss.wav");
            hitDuration = getDuration("/hit.wav");
            sunkDuration = getDuration("/sunk.wav");

            shot = loadSound("/shot.wav");
            miss = loadSound("/miss.wav");
            hit = loadSound("/hit.wav");
            sunk = loadSound("/sunk.wav");

        } catch (IOException | UnsupportedAudioFileException
                 | LineUnavailableException e) {
            throw new RuntimeException(
                    "Failed to initialize sound effects", e);
        }

        soundThread = new Thread(this::processSounds);
        soundThread.setDaemon(true);
        soundThread.start();
    }

    private AudioCue loadSound(String path)
            throws IOException, UnsupportedAudioFileException,
            LineUnavailableException {

        URL url = getClass().getResource(path);

        if (url == null) {
            throw new IOException("Sound not found: " + path);
        }

        AudioCue cue = AudioCue.makeStereoCue(url, 1);
        cue.open();

        return cue;
    }

    private long getDuration(String path)
            throws IOException, UnsupportedAudioFileException {

        URL url = getClass().getResource(path);

        if (url == null) {
            throw new IOException("Sound not found: " + path);
        }

        try (var audioInput = AudioSystem.getAudioInputStream(url)) {
            return (long) (
                    audioInput.getFrameLength()
                            / audioInput.getFormat().getFrameRate()
                            * 1000
            );
        }
    }

    private void processSounds() {
        while (running || !soundQueue.isEmpty()) {
            try {
                Sound sound = soundQueue.take();

                sound.cue().play();
                Thread.sleep(sound.duration());

            } catch (InterruptedException e) {
                if (!running) {
                    break;
                }
            }
        }
    }

    public void playShot() {
        soundQueue.offer(new Sound(shot, shotDuration));
    }

    public void playMiss() {
        soundQueue.offer(new Sound(miss, missDuration));
    }

    public void playHit() {
        soundQueue.offer(new Sound(hit, hitDuration));
    }

    public void playSunk() {
        soundQueue.offer(new Sound(sunk, sunkDuration));
    }

    public void close() {
        running = false;
        soundThread.interrupt();

        shot.close();
        miss.close();
        hit.close();
        sunk.close();
    }
}
