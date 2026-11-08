
package battleship;

import com.adonax.audiocue.AudioCue;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Manages the sound effects used throughout the Battleship game.
 * <p>
 * This class loads and initializes audio cues for shooting, missing,
 * hitting a ship, and sinking a ship. Sound requests are added to a
 * queue and processed sequentially by a dedicated background thread.
 * This prevents multiple sound effects from being played simultaneously.
 */
public class SoundManager {

    /** Audio cue played when a shot is fired. */
    private final AudioCue shot;

    /** Audio cue played when a shot misses its target. */
    private final AudioCue miss;

    /** Audio cue played when a shot hits a ship. */
    private final AudioCue hit;

    /** Audio cue played when a ship is sunk. */
    private final AudioCue sunk;

    /** Duration of the shot sound, in milliseconds. */
    private final long shotDuration;

    /** Duration of the miss sound, in milliseconds. */
    private final long missDuration;

    /** Duration of the hit sound, in milliseconds. */
    private final long hitDuration;

    /** Duration of the sunk sound, in milliseconds. */
    private final long sunkDuration;

    private final String SEPARATOR = "------------------------";

    /**
     * Queue containing the sound effects waiting to be played.
     */
    private final BlockingQueue<Sound> soundQueue =
            new LinkedBlockingQueue<>();

    /** Indicates whether the sound-processing thread should keep running. */
    private volatile boolean running = true;

    /** Background thread responsible for processing queued sounds. */
    private final Thread soundThread;

    /**
     * Represents a sound effect and its playback duration.
     *
     * @param cue the audio cue to play
     * @param duration the expected playback duration, in milliseconds
     */
    private record Sound(AudioCue cue, long duration) {}

    /**
     * Initializes all sound effects and starts the background thread.
     *
     * @throws RuntimeException if a sound file cannot be loaded or an
     *                          audio cue cannot be initialized
     */
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

    /**
     * Loads an audio file from the application's resources and opens
     * an audio cue for playing it.
     *
     * @param path the absolute resource path of the audio file
     * @return the initialized audio cue
     * @throws IOException if the resource cannot be found or read
     * @throws UnsupportedAudioFileException if the file format is unsupported
     * @throws LineUnavailableException if an audio line cannot be opened
     */
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

    /**
     * Retrieves the duration of an audio resource.
     *
     * @param path the absolute resource path of the audio file
     * @return the audio duration, in milliseconds
     * @throws IOException if the resource cannot be found or read
     * @throws UnsupportedAudioFileException if the file format is unsupported
     */
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

    /**
     * Processes queued sound effects sequentially.
     * The thread waits for a sound request, plays the corresponding
     * audio cue, and pauses for the sound's expected duration before
     * processing the next request.
     */
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

    /**
     * Stops the sound-processing thread and releases the audio resources.
     */
    public void close() {
        running = false;
        soundThread.interrupt();

        shot.close();
        miss.close();
        hit.close();
        sunk.close();
    }
 
    public String buildGameReport() {
        StringBuilder report = new StringBuilder();
        report.append("BattleShip2 - Game Report").append("\n");
        report.append(SEPARATOR).append("\n");
        int[] totals = new int[4];
        appendPlayer(report, "Player 1", 25, 12, 3, totals);
        appendPlayer(report, "Player 2", 30, 9, 2, totals);
        appendPlayer(report, "Player 3", 20, 8, 1, totals);
        appendPlayer(report, "Player 4", 18, 10, 4, totals);
        appendPlayer(report, "Player 5", 22, 7, 2, totals);
        appendTotals(report, totals);
        return report.toString();
    }
 
    private void appendPlayer(StringBuilder report, String name, int shots, int hits, int sunk, int[] totals) {
        int misses = shots - hits;
        double accuracy = (double) hits / shots;
        int score = hits * 10 + sunk * 50;
        report.append(name).append("\n");
        report.append("Shots: ").append(shots).append("\n");
        report.append("Hits: ").append(hits).append("\n");
        report.append("Misses: ").append(misses).append("\n");
        report.append("Ships sunk: ").append(sunk).append("\n");
        report.append("Accuracy: ").append(accuracy).append("\n");
        report.append("Score: ").append(score).append("\n");
        report.append(SEPARATOR).append("\n");
        totals[0] += shots;
        totals[1] += hits;
        totals[2] += misses;
        totals[3] += sunk;
    }
 
    private void appendTotals(StringBuilder report, int[] totals) {
        report.append("Total shots: ").append(totals[0]).append("\n");
        report.append("Total hits: ").append(totals[1]).append("\n");
        report.append("Total misses: ").append(totals[2]).append("\n");
        report.append("Total sunk: ").append(totals[3]).append("\n");
    }

}
