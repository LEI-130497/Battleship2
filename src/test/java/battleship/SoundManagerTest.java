package battleship;

import com.adonax.audiocue.AudioCue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Test class for class SoundManager.
 * Author: Nathan
 * Date: 2026-11-22 18:00
 * Cyclomatic Complexity:
 * - constructor: 2
 * - loadSound(): 2 (privado, testado por reflexao)
 * - getDuration(): 2 (privado, testado por reflexao)
 * - processSounds(): 5 (privado, corre na thread de som)
 * - playShot(): 1
 * - playMiss(): 1
 * - playHit(): 1
 * - playSunk(): 1
 * - close(): 1
 *
 * Nota: os testes que tocam sons precisam de uma placa de som. Se o computador
 * (ou o GitHub Actions) nao tiver uma, o construtor lanca RuntimeException e
 * esses testes sao SALTADOS (assumeTrue) em vez de falharem. O caminho de falha
 * do construtor e testado com um ClassLoader que "esconde" os ficheiros .wav,
 * por isso funciona com ou sem audio e nunca fica "ignorado".
 */
class SoundManagerTest {

	private SoundManager soundManager;

	@BeforeEach
	void setUp() {
		try {
			soundManager = new SoundManager();
		} catch (RuntimeException e) {
			soundManager = null; // sem audio disponivel
		}
	}

	@AfterEach
	void tearDown() {
		if (soundManager != null) {
			soundManager.close();
			soundManager = null;
		}
	}

	// ------------------------------------------------------------------
	// Metodos auxiliares
	// ------------------------------------------------------------------

	private void requireAudio() {
		assumeTrue(soundManager != null, "Sem dispositivo de audio: teste saltado.");
	}

	/** Chama um metodo privado (que recebe um String) do SoundManager. */
	private Object callPrivate(String methodName, String path) throws Exception {
		Method method = SoundManager.class.getDeclaredMethod(methodName, String.class);
		method.setAccessible(true);
		return method.invoke(soundManager, path);
	}

	/** Le um atributo privado do SoundManager. */
	private Object readField(String fieldName) throws Exception {
		Field field = SoundManager.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(soundManager);
	}

	/** Espera (ate 3 segundos) que a thread de som tenha retirado tudo da fila. */
	private boolean queueIsConsumed() throws Exception {
		BlockingQueue<?> queue = (BlockingQueue<?>) readField("soundQueue");
		long deadline = System.currentTimeMillis() + 3000;
		while (!queue.isEmpty() && System.currentTimeMillis() < deadline) {
			Thread.sleep(20);
		}
		return queue.isEmpty();
	}

	// ------------------------------------------------------------------
	// Construtor
	// ------------------------------------------------------------------

	@Test
	@DisplayName("constructor1: com audio disponivel, cria a thread de som (daemon e viva)")
	void constructor1() throws Exception {
		requireAudio();
		Thread thread = (Thread) readField("soundThread");
		assertAll(
				() -> assertNotNull(soundManager, "Error: expected a SoundManager instance but got null"),
				() -> assertTrue(thread.isAlive(), "Error: expected the sound thread to be alive but it is not"),
				() -> assertTrue(thread.isDaemon(), "Error: expected the sound thread to be a daemon thread")
		);
	}

	/**
	 * ClassLoader que carrega a sua propria copia de SoundManager e finge que nao existe nenhum .wav,
	 * para o construtor falhar (getResource devolve null) sem depender da placa de som.
	 */
	private static class NoSoundFilesLoader extends ClassLoader {
		NoSoundFilesLoader() {
			super(SoundManagerTest.class.getClassLoader());
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (name.equals("battleship.SoundManager") || name.startsWith("battleship.SoundManager$")) {
				synchronized (getClassLoadingLock(name)) {
					Class<?> loaded = findLoadedClass(name);
					if (loaded == null) {
						String file = name.replace('.', '/') + ".class";
						try (InputStream in = SoundManagerTest.class.getClassLoader().getResourceAsStream(file)) {
							if (in == null) throw new ClassNotFoundException(name);
							byte[] bytes = in.readAllBytes();
							loaded = defineClass(name, bytes, 0, bytes.length);
						} catch (IOException e) {
							throw new ClassNotFoundException(name, e);
						}
					}
					return loaded;
				}
			}
			return super.loadClass(name, resolve);
		}

		@Override
		public URL getResource(String name) {
			return name.endsWith(".wav") ? null : super.getResource(name);
		}
	}

	@Test
	@DisplayName("constructor2: sem ficheiros de som -> RuntimeException com causa IOException")
	void constructor2() throws Exception {
		Class<?> isolated = new NoSoundFilesLoader().loadClass("battleship.SoundManager");
		InvocationTargetException e = assertThrows(InvocationTargetException.class,
				() -> isolated.getConstructor().newInstance(),
				"Error: expected the constructor to fail when the sound files are missing");
		Throwable cause = e.getCause();
		assertAll(
				() -> assertInstanceOf(RuntimeException.class, cause,
						"Error: expected RuntimeException but got " + cause),
				() -> assertEquals("Failed to initialize sound effects", cause.getMessage(),
						"Error: unexpected exception message: " + cause.getMessage()),
				() -> assertInstanceOf(IOException.class, cause.getCause(),
						"Error: expected the cause to be an IOException but got " + cause.getCause())
		);
	}

	// ------------------------------------------------------------------
	// loadSound (privado)
	// ------------------------------------------------------------------

	@Test
	@DisplayName("loadSound1: ficheiro existente -> devolve um AudioCue")
	void loadSound1() throws Exception {
		requireAudio();
		Object cue = callPrivate("loadSound", "/hit.wav");
		assertInstanceOf(AudioCue.class, cue, "Error: expected an AudioCue but got " + cue);
		((AudioCue) cue).close(); // libertar a linha de audio aberta por este teste
	}

	@Test
	@DisplayName("loadSound2: ficheiro inexistente -> IOException 'Sound not found'")
	void loadSound2() {
		requireAudio();
		InvocationTargetException e = assertThrows(InvocationTargetException.class,
				() -> callPrivate("loadSound", "/nao_existe.wav"),
				"Error: expected an exception for a missing sound file");
		assertAll(
				() -> assertInstanceOf(IOException.class, e.getCause(),
						"Error: expected IOException but got " + e.getCause()),
				() -> assertTrue(e.getCause().getMessage().contains("Sound not found"),
						"Error: expected 'Sound not found' but got " + e.getCause().getMessage())
		);
	}

	// ------------------------------------------------------------------
	// getDuration (privado)
	// ------------------------------------------------------------------

	@Test
	@DisplayName("getDuration1: ficheiro existente -> duracao positiva em milissegundos")
	void getDuration1() throws Exception {
		requireAudio();
		long duration = (Long) callPrivate("getDuration", "/shot.wav");
		assertTrue(duration > 0, "Error: expected a duration > 0 ms but got " + duration);
	}

	@Test
	@DisplayName("getDuration2: ficheiro inexistente -> IOException 'Sound not found'")
	void getDuration2() {
		requireAudio();
		InvocationTargetException e = assertThrows(InvocationTargetException.class,
				() -> callPrivate("getDuration", "/nao_existe.wav"),
				"Error: expected an exception for a missing sound file");
		assertAll(
				() -> assertInstanceOf(IOException.class, e.getCause(),
						"Error: expected IOException but got " + e.getCause()),
				() -> assertTrue(e.getCause().getMessage().contains("Sound not found"),
						"Error: expected 'Sound not found' but got " + e.getCause().getMessage())
		);
	}

	// ------------------------------------------------------------------
	// play*  (CC = 1 cada)
	// ------------------------------------------------------------------

	@Test
	@DisplayName("playShot: poe o som na fila e a thread consome-o")
	void playShot() throws Exception {
		requireAudio();
		assertDoesNotThrow(() -> soundManager.playShot(), "Error: playShot should not throw");
		assertTrue(queueIsConsumed(), "Error: expected the shot sound to be taken from the queue");
	}

	@Test
	@DisplayName("playMiss: poe o som na fila e a thread consome-o")
	void playMiss() throws Exception {
		requireAudio();
		assertDoesNotThrow(() -> soundManager.playMiss(), "Error: playMiss should not throw");
		assertTrue(queueIsConsumed(), "Error: expected the miss sound to be taken from the queue");
	}

	@Test
	@DisplayName("playHit: poe o som na fila e a thread consome-o")
	void playHit() throws Exception {
		requireAudio();
		assertDoesNotThrow(() -> soundManager.playHit(), "Error: playHit should not throw");
		assertTrue(queueIsConsumed(), "Error: expected the hit sound to be taken from the queue");
	}

	@Test
	@DisplayName("playSunk: poe o som na fila e a thread consome-o")
	void playSunk() throws Exception {
		requireAudio();
		assertDoesNotThrow(() -> soundManager.playSunk(), "Error: playSunk should not throw");
		assertTrue(queueIsConsumed(), "Error: expected the sunk sound to be taken from the queue");
	}

	// ------------------------------------------------------------------
	// processSounds (privado, corre na thread de som)
	// ------------------------------------------------------------------

	@Test
	@DisplayName("processSounds1: interrupcao com running=true -> a thread continua viva")
	void processSounds1() throws Exception {
		requireAudio();
		Thread thread = (Thread) readField("soundThread");
		thread.interrupt(); // o 'catch' ve running == true e nao sai do ciclo
		Thread.sleep(300);
		assertTrue(thread.isAlive(),
				"Error: expected the sound thread to keep running after an interrupt while running=true");
	}

	@Test
	@DisplayName("processSounds2: running=false com sons na fila -> processa-os todos e a thread termina sozinha")
	void processSounds2() throws Exception {
		requireAudio();
		Thread thread = (Thread) readField("soundThread");
		Field running = SoundManager.class.getDeclaredField("running");
		running.setAccessible(true);
		running.setBoolean(soundManager, false); // sem interromper: a condicao do 'while' e que decide

		// 1.o som acorda a thread; o 2.o fica na fila (running=false e fila nao vazia -> continua)
		soundManager.playShot();
		soundManager.playMiss();
		thread.join(5000);

		BlockingQueue<?> queue = (BlockingQueue<?>) readField("soundQueue");
		assertAll(
				() -> assertFalse(thread.isAlive(),
						"Error: expected the sound thread to end by itself when running=false and the queue is empty"),
				() -> assertTrue(queue.isEmpty(),
						"Error: expected the thread to process every queued sound before ending")
		);
	}

	// ------------------------------------------------------------------
	// close
	// ------------------------------------------------------------------

	@Test
	@DisplayName("close: para a thread de som e liberta os recursos")
	void close() throws Exception {
		requireAudio();
		Thread thread = (Thread) readField("soundThread");
		soundManager.close();
		thread.join(3000);
		boolean stillAlive = thread.isAlive();
		soundManager = null; // ja foi fechado: o tearDown nao o deve fechar outra vez
		assertFalse(stillAlive, "Error: expected the sound thread to stop after close() but it is still alive");
	}
}
