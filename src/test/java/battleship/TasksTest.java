package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for class Tasks.
 * Author: Nathan
 * Date: 2026-11-22 18:00
 * Cyclomatic Complexity:
 * - menu(): 20
 * - menuHelp(): 1
 * - buildFleet(): 4
 * - readShip(): 1
 * - readPosition(): 1
 * - readClassicPosition(): 5
 *
 * Nota: para testar os caminhos do menu() que terminam o programa, a classe Tasks tem dois
 * "ganchos" (package-private) que os testes substituem:
 *  - Tasks.exitHandler (por omissao System::exit): aqui lanca ExitCalled, em vez de fechar o JVM;
 *  - Tasks.pauser (por omissao Thread::sleep): aqui nao espera 3 s por jogada.
 * O comando "application" tenta abrir uma janela JavaFX; num teste o toolkit nao esta
 * iniciado e o Platform.runLater lanca IllegalStateException (a linha fica coberta).
 */
class TasksTest {

	/** Frota valida (11 navios, nenhum encostado a outro) no formato do comando "lefrota". */
	private static final String VALID_FLEET =
			"galeao 0 0 n\n" +
			"fragata 0 4 e\n" +
			"nau 2 4 e\n" +
			"nau 4 0 e\n" +
			"caravela 4 4 e\n" +
			"caravela 6 0 e\n" +
			"caravela 6 4 e\n" +
			"barca 8 0 n\n" +
			"barca 8 2 n\n" +
			"barca 8 4 n\n" +
			"barca 8 6 n\n";

	/** Excecao lancada pelo exitHandler de teste para parar o menu() em vez de fechar o JVM. */
	private static class ExitCalled extends RuntimeException {
		private final int status;

		ExitCalled(int status) {
			super("exit(" + status + ")");
			this.status = status;
		}
	}

	private Tasks.Pauser originalPauser;
	private java.util.function.IntConsumer originalExitHandler;
	private InputStream originalIn;
	private PrintStream originalOut;
	private ByteArrayOutputStream capturedOut;

	@BeforeEach
	void setUp() {
		originalIn = System.in;
		originalOut = System.out;
		originalPauser = Tasks.pauser;
		originalExitHandler = Tasks.exitHandler;
		Tasks.pauser = millis -> { };                                     // sem esperas nos testes
		Tasks.exitHandler = status -> { throw new ExitCalled(status); };  // nunca fecha o JVM
		capturedOut = new ByteArrayOutputStream();
		System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
	}

	@AfterEach
	void tearDown() {
		System.setIn(originalIn);
		System.setOut(originalOut);
		Tasks.pauser = originalPauser;
		Tasks.exitHandler = originalExitHandler;
		Thread.interrupted(); // limpa a flag de interrupcao, se algum teste a deixou ligada
	}

	// ------------------------------------------------------------------
	// Metodos auxiliares
	// ------------------------------------------------------------------

	private String output() {
		return capturedOut.toString(StandardCharsets.UTF_8);
	}

	/** Simula o teclado com o texto dado, corre o menu e devolve o que ele escreveu. */
	private String runMenu(String script) {
		System.setIn(new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)));
		Tasks.menu();
		return output();
	}

	private static int count(String text, String part) {
		return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
	}

	/** True se o JVM foi arrancado com -ea (assert ativo). */
	private static boolean assertionsEnabled() {
		boolean enabled = false;
		assert enabled = true; // so executa se os asserts estiverem ativos
		return enabled;
	}

	// ------------------------------------------------------------------
	// menu()  (CC = 20)
	// ------------------------------------------------------------------

	@Test
	@DisplayName("menu1: 'desisto' logo ao inicio -> mostra ajuda e despede-se")
	void menu1() {
		String out = runMenu("desisto\n");
		assertAll(
				() -> assertTrue(out.contains("AJUDA DO MENU"), "Error: expected the help text but got: " + out),
				() -> assertTrue(out.contains("Bons ventos!"), "Error: expected the goodbye message but got: " + out),
				() -> assertFalse(out.contains("Que comando"), "Error: expected no 'unknown command' message")
		);
	}

	@Test
	@DisplayName("menu2: comando desconhecido -> 'Que comando e esse???'")
	void menu2() {
		String out = runMenu("xpto\ndesisto\n");
		assertTrue(out.contains("Que comando é esse??? Repete ..."),
				"Error: expected the unknown-command message but got: " + out);
	}

	@Test
	@DisplayName("menu3: comando 'ajuda' -> a ajuda aparece uma segunda vez")
	void menu3() {
		String out = runMenu("ajuda\ndesisto\n");
		assertEquals(2, count(out, "AJUDA DO MENU"),
				"Error: expected the help to be printed 2 times but got " + count(out, "AJUDA DO MENU"));
	}

	@Test
	@DisplayName("menu4: 'gerafrota' -> cria frota aleatoria e mostra o mapa com legenda")
	void menu4() {
		String out = runMenu("gerafrota\ndesisto\n");
		assertTrue(out.contains("LEGENDA"), "Error: expected the board with legend but got: " + out);
	}

	@Test
	@DisplayName("menu5: 'lefrota' -> le 11 navios e mostra o mapa; 'estado' mostra 11 a flutuar")
	void menu5() {
		String out = runMenu("lefrota\n" + VALID_FLEET + "estado\ndesisto\n");
		assertAll(
				() -> assertTrue(out.contains("LEGENDA"), "Error: expected the board with legend but got: " + out),
				() -> assertTrue(out.contains("Estado da Frota: 11 a flutuar, 0 afundados!"),
						"Error: expected 11 floating ships but got: " + out)
		);
	}

	@Test
	@DisplayName("menu6: 'estado' com frota -> mostra o estado da frota")
	void menu6() {
		String out = runMenu("gerafrota\nestado\ndesisto\n");
		assertTrue(out.contains("Estado da Frota: 11 a flutuar, 0 afundados!"),
				"Error: expected the fleet status but got: " + out);
	}

	@Test
	@DisplayName("menu7: 'estado' sem frota -> nao mostra nada")
	void menu7() {
		String out = runMenu("estado\ndesisto\n");
		assertFalse(out.contains("Estado da Frota"),
				"Error: expected no fleet status without a fleet but got: " + out);
	}

	@Test
	@DisplayName("menu8: 'mapa' com frota -> o mapa aparece outra vez")
	void menu8() {
		String out = runMenu("gerafrota\nmapa\ndesisto\n");
		assertEquals(2, count(out, "LEGENDA"),
				"Error: expected the board twice (gerafrota + mapa) but got " + count(out, "LEGENDA"));
	}

	@Test
	@DisplayName("menu9: 'mapa' sem frota -> nao mostra o mapa")
	void menu9() {
		String out = runMenu("mapa\ndesisto\n");
		assertEquals(0, count(out, "LEGENDA"),
				"Error: expected no board without a fleet but got " + count(out, "LEGENDA"));
	}

	@Test
	@DisplayName("menu10: 'application' sem frota -> nao faz nada (nao abre janela)")
	void menu10() {
		String out = runMenu("application\ndesisto\n");
		assertTrue(out.contains("Bons ventos!"), "Error: expected the menu to end normally but got: " + out);
	}

	@Test
	@DisplayName("menu11: 'rajada' com jogo -> processa 3 tiros e mostra o estado")
	void menu11() {
		String out = runMenu("gerafrota\nrajada A1 B2 C3\ndesisto\n");
		assertAll(
				() -> assertTrue(out.contains("Jogada nº1"), "Error: expected the move summary but got: " + out),
				() -> assertTrue(out.contains("Estado da Frota:"), "Error: expected the fleet status but got: " + out),
				() -> assertTrue(out.contains("Bons ventos!"), "Error: expected the menu to end normally")
		);
	}

	@Test
	@DisplayName("menu12: 'rajada' sem jogo -> ignora o comando")
	void menu12() {
		String out = runMenu("rajada\ndesisto\n");
		assertFalse(out.contains("Jogada"), "Error: expected no move without a game but got: " + out);
	}

	@Test
	@DisplayName("menu13: 'simula' sem jogo -> ignora o comando")
	void menu13() {
		String out = runMenu("simula\ndesisto\n");
		assertAll(
				() -> assertFalse(out.contains("Estado da Frota"), "Error: expected no simulation without a game"),
				() -> assertTrue(out.contains("Bons ventos!"), "Error: expected the menu to end normally")
		);
	}

	@Test
	@DisplayName("menu14: 'tiros' com jogo -> mostra o mapa com os tiros e legenda")
	void menu14() {
		String out = runMenu("gerafrota\ntiros\ndesisto\n");
		assertEquals(2, count(out, "LEGENDA"),
				"Error: expected the board twice (gerafrota + tiros) but got " + count(out, "LEGENDA"));
	}

	@Test
	@DisplayName("menu15: 'tiros' sem jogo -> ignora o comando")
	void menu15() {
		String out = runMenu("tiros\ndesisto\n");
		assertEquals(0, count(out, "LEGENDA"),
				"Error: expected no board without a game but got " + count(out, "LEGENDA"));
	}

	@Test
	@DisplayName("menu16: 'application' com frota -> tenta abrir a janela (sem toolkit JavaFX nos testes)")
	void menu16() {
		try {
			runMenu("gerafrota\napplication\ndesisto\n");
		} catch (IllegalStateException e) {
			// Sem toolkit JavaFX iniciado, Platform.runLater lanca esta excecao: o caminho foi percorrido.
		}
		assertTrue(output().contains("LEGENDA"),
				"Error: expected the board of 'gerafrota' to be printed before 'application' but got: " + output());
	}

	@Test
	@DisplayName("menu17: 'rajada' que afunda a frota toda -> mensagem final e exit(0)")
	void menu17() {
		// Frota igual a do comando "lefrota": conhecemos todas as casas ocupadas.
		List<String> cells = new ArrayList<>();
		for (IShip ship : Tasks.buildFleet(new Scanner(VALID_FLEET)).getShips())
			for (IPosition pos : ship.getPositions())
				cells.add("" + pos.getClassicRow() + pos.getClassicColumn());
		while (cells.size() % Game.NUMBER_SHOTS != 0)
			cells.add(cells.get(0)); // completa a ultima rajada com um tiro repetido

		StringBuilder script = new StringBuilder("lefrota\n").append(VALID_FLEET);
		for (int i = 0; i < cells.size(); i += Game.NUMBER_SHOTS)
			script.append("rajada ").append(String.join(" ", cells.subList(i, i + Game.NUMBER_SHOTS))).append("\n");
		script.append("desisto\n");

		ExitCalled exit = assertThrows(ExitCalled.class, () -> runMenu(script.toString()),
				"Error: expected the game to end with exit(0) after sinking the whole fleet");
		assertAll(
				() -> assertEquals(0, exit.status, "Error: expected exit status 0 but got " + exit.status),
				() -> assertTrue(output().contains("Maldito sejas"),
						"Error: expected the game-over message but got: " + output()),
				() -> assertFalse(output().contains("Bons ventos!"),
						"Error: expected the menu NOT to reach the goodbye message")
		);
	}

	@Test
	@DisplayName("menu18: 'simula' com jogo -> joga ate afundar tudo, pausa entre jogadas e exit(0)")
	void menu18() {
		AtomicInteger pauses = new AtomicInteger();
		Tasks.pauser = millis -> {
			assertEquals(3000, millis, "Error: expected a pause of 3000 ms but got " + millis);
			pauses.incrementAndGet();
		};

		ExitCalled exit = assertThrows(ExitCalled.class, () -> runMenu("gerafrota\nsimula\ndesisto\n"),
				"Error: expected the simulation to end with exit(0)");
		assertAll(
				() -> assertEquals(0, exit.status, "Error: expected exit status 0 but got " + exit.status),
				() -> assertTrue(pauses.get() > 0, "Error: expected at least one pause between moves"),
				() -> assertTrue(output().contains("Estado da Frota: 0 a flutuar"),
						"Error: expected the last status to show no floating ships but got: " + output()),
				() -> assertTrue(output().contains("Maldito sejas"),
						"Error: expected the game-over message but got: " + output())
		);
	}

	@Test
	@DisplayName("menu19: 'simula' com pausa interrompida -> repoe a flag de interrupcao e continua")
	void menu19() {
		AtomicInteger pauses = new AtomicInteger();
		AtomicBoolean interruptedAtExit = new AtomicBoolean();
		Tasks.pauser = millis -> {
			if (pauses.incrementAndGet() == 1)
				throw new InterruptedException("interrompido pelo teste");
		};
		Tasks.exitHandler = status -> {
			interruptedAtExit.set(Thread.currentThread().isInterrupted());
			throw new ExitCalled(status);
		};

		assertThrows(ExitCalled.class, () -> runMenu("gerafrota\nsimula\ndesisto\n"),
				"Error: expected the simulation to end with exit(0)");
		assertAll(
				() -> assertTrue(pauses.get() > 1,
						"Error: expected the simulation to continue after the interrupted pause but pauses = " + pauses.get()),
				() -> assertTrue(interruptedAtExit.get(),
						"Error: expected the interrupt status to be restored after InterruptedException")
		);
	}

	// ------------------------------------------------------------------
	// menuHelp()  (CC = 1)
	// ------------------------------------------------------------------

	@Test
	@DisplayName("menuHelp: lista todos os comandos do menu")
	void menuHelp() {
		Tasks.menuHelp();
		String out = output();
		assertAll(
				() -> assertTrue(out.contains("AJUDA DO MENU"), "Error: expected the help title"),
				() -> assertTrue(out.contains("gerafrota"), "Error: expected command gerafrota in the help"),
				() -> assertTrue(out.contains("lefrota"), "Error: expected command lefrota in the help"),
				() -> assertTrue(out.contains("estado"), "Error: expected command estado in the help"),
				() -> assertTrue(out.contains("mapa"), "Error: expected command mapa in the help"),
				() -> assertTrue(out.contains("rajada"), "Error: expected command rajada in the help"),
				() -> assertTrue(out.contains("simula"), "Error: expected command simula in the help"),
				() -> assertTrue(out.contains("tiros"), "Error: expected command tiros in the help"),
				() -> assertTrue(out.contains("desisto"), "Error: expected command desisto in the help"),
				() -> assertTrue(out.contains("application"), "Error: expected command application in the help")
		);
	}

	// ------------------------------------------------------------------
	// buildFleet()  (CC = 4)
	// ------------------------------------------------------------------

	@Test
	@DisplayName("buildFleet1: 11 navios validos -> frota completa")
	void buildFleet1() {
		Fleet fleet = Tasks.buildFleet(new Scanner(VALID_FLEET));
		assertEquals(Fleet.FLEET_SIZE.intValue(), fleet.getShips().size(),
				"Error: expected " + Fleet.FLEET_SIZE + " ships but got " + fleet.getShips().size());
	}

	@Test
	@DisplayName("buildFleet2: navio desconhecido e navio fora do tabuleiro sao ignorados")
	void buildFleet2() {
		String input = "xpto 0 0 n\n"      // tipo de navio desconhecido -> readShip devolve null
				+ "barca 99 99 n\n"        // fora do tabuleiro -> addShip devolve false
				+ VALID_FLEET;
		Fleet fleet = Tasks.buildFleet(new Scanner(input));
		assertEquals(Fleet.FLEET_SIZE.intValue(), fleet.getShips().size(),
				"Error: expected " + Fleet.FLEET_SIZE + " ships but got " + fleet.getShips().size());
	}

	@Test
	@DisplayName("buildFleet3: scanner null -> AssertionError")
	void buildFleet3() {
		Assumptions.assumeTrue(assertionsEnabled(), "Asserts desativados (falta -ea): teste saltado.");
		assertThrows(AssertionError.class, () -> Tasks.buildFleet(null),
				"Error: expected AssertionError for a null scanner");
	}

	// ------------------------------------------------------------------
	// readShip()  (CC = 1)
	// ------------------------------------------------------------------

	@Test
	@DisplayName("readShip1: 'barca 3 4 n' -> Barca virada a norte na posicao (3,4)")
	void readShip1() {
		Ship ship = Tasks.readShip(new Scanner("barca 3 4 n"));
		assertNotNull(ship, "Error: expected a ship but got null");
		assertAll(
				() -> assertEquals("Barca", ship.getCategory(),
						"Error: expected category Barca but got " + ship.getCategory()),
				() -> assertEquals(Compass.NORTH, ship.getBearing(),
						"Error: expected bearing NORTH but got " + ship.getBearing()),
				() -> assertEquals(3, ship.getPosition().getRow(),
						"Error: expected row 3 but got " + ship.getPosition().getRow()),
				() -> assertEquals(4, ship.getPosition().getColumn(),
						"Error: expected column 4 but got " + ship.getPosition().getColumn())
		);
	}

	@Test
	@DisplayName("readShip2: tipo de navio desconhecido -> null")
	void readShip2() {
		assertNull(Tasks.readShip(new Scanner("xpto 1 1 n")),
				"Error: expected null for an unknown ship kind");
	}

	@Test
	@DisplayName("readShip3: scanner null -> AssertionError")
	void readShip3() {
		Assumptions.assumeTrue(assertionsEnabled(), "Asserts desativados (falta -ea): teste saltado.");
		assertThrows(AssertionError.class, () -> Tasks.readShip(null),
				"Error: expected AssertionError for a null scanner");
	}

	// ------------------------------------------------------------------
	// readPosition()  (CC = 1)
	// ------------------------------------------------------------------

	@Test
	@DisplayName("readPosition1: '3 4' -> linha 3, coluna 4")
	void readPosition1() {
		Position pos = Tasks.readPosition(new Scanner("3 4"));
		assertAll(
				() -> assertEquals(3, pos.getRow(), "Error: expected row 3 but got " + pos.getRow()),
				() -> assertEquals(4, pos.getColumn(), "Error: expected column 4 but got " + pos.getColumn())
		);
	}

	@Test
	@DisplayName("readPosition2: texto em vez de numeros -> InputMismatchException")
	void readPosition2() {
		assertThrows(InputMismatchException.class, () -> Tasks.readPosition(new Scanner("a b")),
				"Error: expected InputMismatchException for non-numeric input");
	}

	@Test
	@DisplayName("readPosition3: scanner null -> AssertionError")
	void readPosition3() {
		Assumptions.assumeTrue(assertionsEnabled(), "Asserts desativados (falta -ea): teste saltado.");
		assertThrows(AssertionError.class, () -> Tasks.readPosition(null),
				"Error: expected AssertionError for a null scanner");
	}

	// ------------------------------------------------------------------
	// readClassicPosition()  (CC = 5)
	// ------------------------------------------------------------------

	@Test
	@DisplayName("readClassicPosition1: sem texto -> IllegalArgumentException")
	void readClassicPosition1() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> Tasks.readClassicPosition(new Scanner("")),
				"Error: expected IllegalArgumentException for an empty input");
		assertEquals("Nenhuma posição válida encontrada!", e.getMessage(),
				"Error: unexpected message: " + e.getMessage());
	}

	@Test
	@DisplayName("readClassicPosition2: formato compacto 'A3' -> linha 0, coluna 2")
	void readClassicPosition2() {
		IPosition pos = Tasks.readClassicPosition(new Scanner("A3"));
		assertAll(
				() -> assertEquals(0, pos.getRow(), "Error: expected row 0 but got " + pos.getRow()),
				() -> assertEquals(2, pos.getColumn(), "Error: expected column 2 but got " + pos.getColumn())
		);
	}

	@Test
	@DisplayName("readClassicPosition3: formato com espaco e minuscula 'c 4' -> linha 2, coluna 3")
	void readClassicPosition3() {
		IPosition pos = Tasks.readClassicPosition(new Scanner("c 4"));
		assertAll(
				() -> assertEquals(2, pos.getRow(), "Error: expected row 2 but got " + pos.getRow()),
				() -> assertEquals(3, pos.getColumn(), "Error: expected column 3 but got " + pos.getColumn())
		);
	}

	@Test
	@DisplayName("readClassicPosition4: 'J10' (ultima casa do tabuleiro) -> linha 9, coluna 9")
	void readClassicPosition4() {
		IPosition pos = Tasks.readClassicPosition(new Scanner("J10"));
		assertAll(
				() -> assertEquals(9, pos.getRow(), "Error: expected row 9 but got " + pos.getRow()),
				() -> assertEquals(9, pos.getColumn(), "Error: expected column 9 but got " + pos.getColumn())
		);
	}

	@Test
	@DisplayName("readClassicPosition5: 'ZZ9' (nao segue o formato, sem 2.o token) -> IllegalArgumentException")
	void readClassicPosition5() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> Tasks.readClassicPosition(new Scanner("ZZ9")),
				"Error: expected IllegalArgumentException for 'ZZ9'");
		assertTrue(e.getMessage().contains("Formato inválido"),
				"Error: expected 'Formato inválido' but got " + e.getMessage());
	}

	@Test
	@DisplayName("readClassicPosition6: 'AB 3' (1.o token com 2 letras) -> IllegalArgumentException")
	void readClassicPosition6() {
		assertThrows(IllegalArgumentException.class, () -> Tasks.readClassicPosition(new Scanner("AB 3")),
				"Error: expected IllegalArgumentException for 'AB 3'");
	}

	@Test
	@DisplayName("readClassicPosition7: 'A -3' (2.o token nao e so digitos) -> IllegalArgumentException")
	void readClassicPosition7() {
		assertThrows(IllegalArgumentException.class, () -> Tasks.readClassicPosition(new Scanner("A -3")),
				"Error: expected IllegalArgumentException for 'A -3'");
	}

	@Test
	@DisplayName("readClassicPosition8: so um numero '3' -> IllegalArgumentException")
	void readClassicPosition8() {
		assertThrows(IllegalArgumentException.class, () -> Tasks.readClassicPosition(new Scanner("3")),
				"Error: expected IllegalArgumentException for '3'");
	}

	@Test
	@DisplayName("readClassicPosition9: scanner null -> excecao (IllegalArgumentException do @NotNull ou NullPointerException)")
	void readClassicPosition9() {
		// O parametro esta anotado com @NotNull: o IntelliJ pode instrumentar a verificacao
		// (IllegalArgumentException); sem instrumentacao, a primeira utilizacao do scanner da NullPointerException.
		RuntimeException e = assertThrows(RuntimeException.class, () -> Tasks.readClassicPosition(null),
				"Error: expected an exception for a null scanner but none was thrown");
		assertTrue(e instanceof IllegalArgumentException || e instanceof NullPointerException,
				"Error: expected IllegalArgumentException or NullPointerException but got " + e);
	}
}
