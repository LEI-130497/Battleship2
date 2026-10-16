
package battleship;

import com.adonax.audiocue.AudioCue;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.URL;

public class SoundManager {

    private final AudioCue shot;

    public SoundManager() {
        try {
            shot = loadSound("/a.wav");
        } catch (IOException | UnsupportedAudioFileException
                 | LineUnavailableException e) {
            throw new RuntimeException("Failed to initialize sound effects", e);
        }
    }

    private AudioCue loadSound(String path) throws IOException, UnsupportedAudioFileException, LineUnavailableException {

        URL url = getClass().getResource(path);

        if (url == null) {
            throw new IOException("Sound not found: " + path);
        }

        AudioCue cue = AudioCue.makeStereoCue(url, 4);
        cue.open();

        return cue;
    }

    public void playShot() {
        shot.play();
    }

    public void playMiss() {
        shot.play();
    }

    public void playHit() {
        shot.play();
    }

    public void playSunk() {
        shot.play();
    }

    public void close() {
        shot.close();
    }


}