/**
 * 
 */
package battleship;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Main
{
	/**
	 * Main.
	 *
	 * @param args the args
	 */
	public static void main(String[] args) throws java.io.IOException, UnsupportedAudioFileException, LineUnavailableException {
		SoundManager sm = new SoundManager();
		sm.playShot();
		System.out.println("***  Battleship  ***");

		Tasks.menu();
		sm.close();
    }
}
