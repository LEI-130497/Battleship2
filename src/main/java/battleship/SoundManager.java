
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
       String separator = "------------------------";
       StringBuilder report = new StringBuilder();
       report.append("BattleShip2 - Game Report").append("\n");
       report.append(separator).append("\n");
       String name1 = "Player 1";
       int shots1 = 25;
       int hits1 = 12;
       int sunk1 = 3;
       int misses1 = shots1 - hits1;
       double accuracy1 = (double) hits1 / shots1;
       int score1 = hits1 * 10 + sunk1 * 50;
       report.append(name1).append("\n");
       report.append("Shots: ").append(shots1).append("\n");
       report.append("Hits: ").append(hits1).append("\n");
       report.append("Misses: ").append(misses1).append("\n");
       report.append("Ships sunk: ").append(sunk1).append("\n");
       report.append("Accuracy: ").append(accuracy1).append("\n");
       report.append("Score: ").append(score1).append("\n");
       report.append(separator).append("\n");
       String name2 = "Player 2";
       int shots2 = 30;
       int hits2 = 9;
       int sunk2 = 2;
       int misses2 = shots2 - hits2;
       double accuracy2 = (double) hits2 / shots2;
       int score2 = hits2 * 10 + sunk2 * 50;
       report.append(name2).append("\n");
       report.append("Shots: ").append(shots2).append("\n");
       report.append("Hits: ").append(hits2).append("\n");
       report.append("Misses: ").append(misses2).append("\n");
       report.append("Ships sunk: ").append(sunk2).append("\n");
       report.append("Accuracy: ").append(accuracy2).append("\n");
       report.append("Score: ").append(score2).append("\n");
       report.append(separator).append("\n");
       String name3 = "Player 3";
       int shots3 = 20;
       int hits3 = 8;
       int sunk3 = 1;
       int misses3 = shots3 - hits3;
       double accuracy3 = (double) hits3 / shots3;
       int score3 = hits3 * 10 + sunk3 * 50;
       report.append(name3).append("\n");
       report.append("Shots: ").append(shots3).append("\n");
       report.append("Hits: ").append(hits3).append("\n");
       report.append("Misses: ").append(misses3).append("\n");
       report.append("Ships sunk: ").append(sunk3).append("\n");
       report.append("Accuracy: ").append(accuracy3).append("\n");
       report.append("Score: ").append(score3).append("\n");
       report.append(separator).append("\n");
       String name4 = "Player 4";
       int shots4 = 18;
       int hits4 = 10;
       int sunk4 = 4;
       int misses4 = shots4 - hits4;
       double accuracy4 = (double) hits4 / shots4;
       int score4 = hits4 * 10 + sunk4 * 50;
       report.append(name4).append("\n");
       report.append("Shots: ").append(shots4).append("\n");
       report.append("Hits: ").append(hits4).append("\n");
       report.append("Misses: ").append(misses4).append("\n");
       report.append("Ships sunk: ").append(sunk4).append("\n");
       report.append("Accuracy: ").append(accuracy4).append("\n");
       report.append("Score: ").append(score4).append("\n");
       report.append(separator).append("\n");
       String name5 = "Player 5";
       int shots5 = 22;
       int hits5 = 7;
       int sunk5 = 2;
       int misses5 = shots5 - hits5;
       double accuracy5 = (double) hits5 / shots5;
       int score5 = hits5 * 10 + sunk5 * 50;
       report.append(name5).append("\n");
       report.append("Shots: ").append(shots5).append("\n");
       report.append("Hits: ").append(hits5).append("\n");
       report.append("Misses: ").append(misses5).append("\n");
       report.append("Ships sunk: ").append(sunk5).append("\n");
       report.append("Accuracy: ").append(accuracy5).append("\n");
       report.append("Score: ").append(score5).append("\n");
       report.append(separator).append("\n");
       int totalShots = shots1 + shots2 + shots3 + shots4 + shots5;
       int totalHits = hits1 + hits2 + hits3 + hits4 + hits5;
       int totalMisses = misses1 + misses2 + misses3 + misses4 + misses5;
       int totalSunk = sunk1 + sunk2 + sunk3 + sunk4 + sunk5;
       report.append("Total shots: ").append(totalShots).append("\n");
       report.append("Total hits: ").append(totalHits).append("\n");
       report.append("Total misses: ").append(totalMisses).append("\n");
       report.append("Total sunk: ").append(totalSunk).append("\n");
       return report.toString();
   }

}
