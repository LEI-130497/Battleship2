package battleship;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for class Move.
 * Author: Nathan
 * Date: 2026-11-22 16:00
 * Cyclomatic Complexity:
 * - constructor: 1
 * - toString(): 1
 * - getNumber(): 1
 * - getShots(): 1
 * - getShotResults(): 1
 * - createObjectMapper(): 1
 * - processEnemyFire(): ~36 (muito alta: ver nota abaixo)
 *
 * Nota sobre processEnemyFire: a complexidade ciclomatica e muito alta porque
 * cada operador ternario (? :) e cada if conta como um caminho. Em vez de 37
 * testes quase iguais, cada teste abaixo percorre varios ramos de uma vez
 * (cenarios reais de uma jogada) e, no conjunto, cobrem todos os ramos
 * alcancaveis. Os ramos que nao se conseguem cobrir estao indicados no fim.
 */
class MoveTest {

	private Move move;
	private List<IPosition> shots;
	private PrintStream originalOut;
	private ByteArrayOutputStream capturedOut;

	@BeforeEach
	void setUp() {
		shots = new ArrayList<>(List.of(new Position(0, 0), new Position(1, 1), new Position(2, 2)));
		move = new Move(1, shots, new ArrayList<>());

		// processEnemyFire escreve na consola: guardamos o texto para o poder verificar
		originalOut = System.out;
		capturedOut = new ByteArrayOutputStream();
		System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
	}

	@AfterEach
	void tearDown() {
		System.setOut(originalOut);
		move = null;
		shots = null;
	}

	// ------------------------------------------------------------------
	// Metodos auxiliares (para os testes ficarem curtos e legiveis)
	// ------------------------------------------------------------------

	/** Tiro fora do tabuleiro. */
	private static IGame.ShotResult invalid() {
		return new IGame.ShotResult(false, false, null, false);
	}

	/** Tiro repetido. */
	private static IGame.ShotResult repeated() {
		return new IGame.ShotResult(true, true, null, false);
	}

	/** Tiro valido que cai na agua. */
	private static IGame.ShotResult water() {
		return new IGame.ShotResult(true, false, null, false);
	}

	/** Tiro valido que acerta num navio sem o afundar. */
	private static IGame.ShotResult hit(IShip ship) {
		return new IGame.ShotResult(true, false, ship, false);
	}

	/** Tiro valido que afunda um navio. */
	private static IGame.ShotResult sunk(IShip ship) {
		return new IGame.ShotResult(true, false, ship, true);
	}

	private Move moveWith(IGame.ShotResult... results) {
		return new Move(1, shots, new ArrayList<>(Arrays.asList(results)));
	}

	private String output() {
		return capturedOut.toString(StandardCharsets.UTF_8);
	}

	private JsonNode parse(String json) throws Exception {
		return new ObjectMapper().readTree(json);
	}

	private void assertCounts(JsonNode json, int valid, int outside, int repeated, int missed) {
		assertAll(
				() -> assertEquals(valid, json.get("validShots").asInt(),
						"Error: expected " + valid + " validShots but got " + json.get("validShots").asInt()),
				() -> assertEquals(outside, json.get("outsideShots").asInt(),
						"Error: expected " + outside + " outsideShots but got " + json.get("outsideShots").asInt()),
				() -> assertEquals(repeated, json.get("repeatedShots").asInt(),
						"Error: expected " + repeated + " repeatedShots but got " + json.get("repeatedShots").asInt()),
				() -> assertEquals(missed, json.get("missedShots").asInt(),
						"Error: expected " + missed + " missedShots but got " + json.get("missedShots").asInt())
		);
	}

	// ------------------------------------------------------------------
	// Construtor e getters (CC = 1 cada)
	// ------------------------------------------------------------------

	@Test
	@DisplayName("constructor: guarda o numero, os tiros e os resultados")
	void constructor() {
		List<IGame.ShotResult> results = new ArrayList<>();
		Move m = new Move(5, shots, results);
		assertAll(
				() -> assertEquals(5, m.getNumber(), "Error: expected move number 5 but got " + m.getNumber()),
				() -> assertSame(shots, m.getShots(), "Error: expected the same shots list that was given"),
				() -> assertSame(results, m.getShotResults(), "Error: expected the same results list that was given")
		);
	}

	@Test
	@DisplayName("toString: mostra numero, nº de tiros e nº de resultados")
	void toStringFormat() {
		Move m = moveWith(water(), water());
		String expected = "Move{number=1, shots=3, results=2}";
		assertEquals(expected, m.toString(), "Error: expected '" + expected + "' but got '" + m + "'");
	}

	@Test
	@DisplayName("getNumber: devolve o numero da jogada")
	void getNumber() {
		assertEquals(1, move.getNumber(), "Error: expected move number 1 but got " + move.getNumber());
	}

	@Test
	@DisplayName("getShots: devolve a lista de tiros")
	void getShots() {
		assertAll(
				() -> assertSame(shots, move.getShots(), "Error: expected the shots list given to the constructor"),
				() -> assertEquals(3, move.getShots().size(), "Error: expected 3 shots but got " + move.getShots().size())
		);
	}

	@Test
	@DisplayName("getShotResults: devolve a lista de resultados")
	void getShotResults() {
		Move m = moveWith(water(), invalid());
		assertEquals(2, m.getShotResults().size(),
				"Error: expected 2 results but got " + m.getShotResults().size());
	}

	// ------------------------------------------------------------------
	// processEnemyFire
	// ------------------------------------------------------------------

	@Test
	@DisplayName("processEnemyFire1: sem resultados (ciclo nao executa) -> 3 tiros exteriores")
	void processEnemyFire1() throws Exception {
		JsonNode json = parse(move.processEnemyFire(false));
		assertCounts(json, 0, 3, 0, 0);
	}

	@Test
	@DisplayName("processEnemyFire2: verbose=false nao escreve a mensagem 'Jogada nº'")
	void processEnemyFire2() {
		moveWith(invalid(), invalid(), invalid()).processEnemyFire(false);
		assertFalse(output().contains("Jogada"),
				"Error: expected no 'Jogada' message when verbose is false but got: " + output());
	}

	@Test
	@DisplayName("processEnemyFire3: 3 tiros invalidos (verbose) -> '3 tiros exteriores'")
	void processEnemyFire3() throws Exception {
		String result = moveWith(invalid(), invalid(), invalid()).processEnemyFire(true);
		assertAll(
				() -> assertTrue(output().contains("Jogada nº1 -> 3 tiros exteriores"),
						"Error: expected 'Jogada nº1 -> 3 tiros exteriores' but got: " + output()),
				() -> assertCounts(parse(result), 0, 3, 0, 0)
		);
	}

	@Test
	@DisplayName("processEnemyFire4: 3 tiros repetidos -> '3 tiros repetidos'")
	void processEnemyFire4() throws Exception {
		String result = moveWith(repeated(), repeated(), repeated()).processEnemyFire(true);
		assertAll(
				() -> assertTrue(output().contains("3 tiros repetidos"),
						"Error: expected '3 tiros repetidos' but got: " + output()),
				() -> assertCounts(parse(result), 0, 0, 3, 0)
		);
	}

	@Test
	@DisplayName("processEnemyFire5: 1 repetido + 2 invalidos -> singular em 'repetido', plural em 'exteriores'")
	void processEnemyFire5() throws Exception {
		String result = moveWith(repeated(), invalid(), invalid()).processEnemyFire(true);
		assertAll(
				() -> assertTrue(output().contains("1 tiro repetido, 2 tiros exteriores"),
						"Error: expected '1 tiro repetido, 2 tiros exteriores' but got: " + output()),
				() -> assertCounts(parse(result), 0, 2, 1, 0)
		);
	}

	@Test
	@DisplayName("processEnemyFire6: 2 repetidos + 1 invalido -> plural em 'repetidos', singular em 'exterior'")
	void processEnemyFire6() throws Exception {
		String result = moveWith(repeated(), repeated(), invalid()).processEnemyFire(true);
		assertAll(
				() -> assertTrue(output().contains("2 tiros repetidos, 1 tiro exterior"),
						"Error: expected '2 tiros repetidos, 1 tiro exterior' but got: " + output()),
				() -> assertCounts(parse(result), 0, 1, 2, 0)
		);
	}

	@Test
	@DisplayName("processEnemyFire7: 3 tiros na agua -> '3 tiros validos: 3 tiros na agua'")
	void processEnemyFire7() throws Exception {
		String result = moveWith(water(), water(), water()).processEnemyFire(true);
		assertAll(
				() -> assertTrue(output().contains("3 tiros válidos: 3 tiros na água"),
						"Error: expected '3 tiros válidos: 3 tiros na água' but got: " + output()),
				() -> assertCounts(parse(result), 3, 0, 0, 3)
		);
	}

	@Test
	@DisplayName("processEnemyFire8: 1 tiro na agua + 2 invalidos -> singular em 'valido' e 'agua'")
	void processEnemyFire8() throws Exception {
		String result = moveWith(water(), invalid(), invalid()).processEnemyFire(true);
		assertAll(
				() -> assertTrue(output().contains("1 tiro válido: 1 tiro na água, 2 tiros exteriores"),
						"Error: expected '1 tiro válido: 1 tiro na água, 2 tiros exteriores' but got: " + output()),
				() -> assertCounts(parse(result), 1, 2, 0, 1)
		);
	}

	@Test
	@DisplayName("processEnemyFire9: 2 acertos num navio que nao afunda + 1 agua")
	void processEnemyFire9() throws Exception {
		IShip caravel = new Caravel(Compass.NORTH, new Position(3, 3));
		String result = moveWith(hit(caravel), hit(caravel), water()).processEnemyFire(true);
		JsonNode json = parse(result);
		assertAll(
				() -> assertTrue(output().contains("3 tiros válidos: 2 tiros num(a) Caravela + 1 tiro na água"),
						"Error: expected '2 tiros num(a) Caravela + 1 tiro na água' but got: " + output()),
				() -> assertCounts(json, 3, 0, 0, 1),
				() -> assertEquals(0, json.get("sunkBoats").size(), "Error: expected no sunk boats"),
				() -> assertEquals(1, json.get("hitsOnBoats").size(), "Error: expected 1 entry in hitsOnBoats"),
				() -> assertEquals("Caravela", json.get("hitsOnBoats").get(0).get("type").asText(),
						"Error: expected type Caravela in hitsOnBoats"),
				() -> assertEquals(2, json.get("hitsOnBoats").get(0).get("hits").asInt(),
						"Error: expected 2 hits on Caravela")
		);
	}

	@Test
	@DisplayName("processEnemyFire10: 1 acerto sem afundar + 2 invalidos (sem tiros na agua)")
	void processEnemyFire10() throws Exception {
		IShip caravel = new Caravel(Compass.NORTH, new Position(3, 3));
		String result = moveWith(hit(caravel), invalid(), invalid()).processEnemyFire(true);
		assertAll(
				() -> assertTrue(output().contains("1 tiro num(a) Caravela"),
						"Error: expected '1 tiro num(a) Caravela' but got: " + output()),
				() -> assertTrue(output().contains("2 tiros exteriores"),
						"Error: expected '2 tiros exteriores' but got: " + output()),
				() -> assertCounts(parse(result), 1, 2, 0, 0)
		);
	}

	@Test
	@DisplayName("processEnemyFire11: 1 navio afundado + 2 tiros na agua")
	void processEnemyFire11() throws Exception {
		IShip barge = new Barge(Compass.NORTH, new Position(1, 1));
		String result = moveWith(sunk(barge), water(), water()).processEnemyFire(true);
		JsonNode json = parse(result);
		assertAll(
				() -> assertTrue(output().contains("3 tiros válidos: 1 Barca ao fundo + 2 tiros na água"),
						"Error: expected '1 Barca ao fundo + 2 tiros na água' but got: " + output()),
				() -> assertCounts(json, 3, 0, 0, 2),
				() -> assertEquals(1, json.get("sunkBoats").size(), "Error: expected 1 entry in sunkBoats"),
				() -> assertEquals("Barca", json.get("sunkBoats").get(0).get("type").asText(),
						"Error: expected type Barca in sunkBoats"),
				() -> assertEquals(1, json.get("sunkBoats").get(0).get("count").asInt(),
						"Error: expected count 1 in sunkBoats"),
				// o navio afundado nao pode aparecer tambem em hitsOnBoats
				() -> assertEquals(0, json.get("hitsOnBoats").size(),
						"Error: a sunk boat must not appear in hitsOnBoats")
		);
	}

	@Test
	@DisplayName("processEnemyFire12: 2 navios do mesmo tipo afundados -> plural 'Barcas'")
	void processEnemyFire12() throws Exception {
		IShip barge1 = new Barge(Compass.NORTH, new Position(1, 1));
		IShip barge2 = new Barge(Compass.NORTH, new Position(5, 5));
		String result = moveWith(sunk(barge1), sunk(barge2), water()).processEnemyFire(true);
		JsonNode json = parse(result);
		assertAll(
				() -> assertTrue(output().contains("2 Barcas ao fundo + 1 tiro na água"),
						"Error: expected '2 Barcas ao fundo + 1 tiro na água' but got: " + output()),
				() -> assertEquals(2, json.get("sunkBoats").get(0).get("count").asInt(),
						"Error: expected count 2 in sunkBoats")
		);
	}

	@Test
	@DisplayName("processEnemyFire13: 1 navio afundado + 2 invalidos (sem tiros na agua)")
	void processEnemyFire13() throws Exception {
		IShip barge = new Barge(Compass.NORTH, new Position(1, 1));
		String result = moveWith(sunk(barge), invalid(), invalid()).processEnemyFire(true);
		assertAll(
				() -> assertTrue(output().contains("1 Barca ao fundo"),
						"Error: expected '1 Barca ao fundo' but got: " + output()),
				() -> assertTrue(output().contains("2 tiros exteriores"),
						"Error: expected '2 tiros exteriores' but got: " + output()),
				() -> assertCounts(parse(result), 1, 2, 0, 0)
		);
	}

	@Test
	@DisplayName("processEnemyFire14: navio afundado + acerto noutro navio + agua")
	void processEnemyFire14() throws Exception {
		IShip barge = new Barge(Compass.NORTH, new Position(1, 1));
		IShip caravel = new Caravel(Compass.NORTH, new Position(5, 5));
		String result = moveWith(sunk(barge), hit(caravel), water()).processEnemyFire(true);
		JsonNode json = parse(result);
		assertAll(
				() -> assertTrue(output().contains(
								"3 tiros válidos: 1 Barca ao fundo + 1 tiro num(a) Caravela + 1 tiro na água"),
						"Error: expected sunk Barca + hit Caravela + water but got: " + output()),
				() -> assertEquals(1, json.get("sunkBoats").size(), "Error: expected 1 entry in sunkBoats"),
				() -> assertEquals(1, json.get("hitsOnBoats").size(), "Error: expected 1 entry in hitsOnBoats"),
				() -> assertEquals("Caravela", json.get("hitsOnBoats").get(0).get("type").asText(),
						"Error: expected only Caravela in hitsOnBoats")
		);
	}

	@Test
	@DisplayName("processEnemyFire15: 1 agua + 1 repetido + 1 invalido -> tudo no singular")
	void processEnemyFire15() throws Exception {
		String result = moveWith(water(), repeated(), invalid()).processEnemyFire(true);
		assertAll(
				() -> assertTrue(output().contains("1 tiro válido: 1 tiro na água, 1 tiro repetido, 1 tiro exterior"),
						"Error: expected singular valid/repeated/outside but got: " + output()),
				() -> assertCounts(parse(result), 1, 1, 1, 1)
		);
	}

	@Test
	@DisplayName("processEnemyFire16: 1 agua + 2 repetidos -> plural em 'repetidos' depois de tiros validos")
	void processEnemyFire16() throws Exception {
		String result = moveWith(water(), repeated(), repeated()).processEnemyFire(true);
		assertAll(
				() -> assertTrue(output().contains("1 tiro válido: 1 tiro na água, 2 tiros repetidos"),
						"Error: expected '1 tiro válido: 1 tiro na água, 2 tiros repetidos' but got: " + output()),
				() -> assertCounts(parse(result), 1, 0, 2, 1)
		);
	}

	@Test
	@DisplayName("processEnemyFire17: o JSON devolvido tem sempre as 6 chaves esperadas")
	void processEnemyFire17() throws Exception {
		JsonNode json = parse(moveWith(water(), water(), water()).processEnemyFire(false));
		assertAll(
				() -> assertTrue(json.has("validShots"), "Error: expected key validShots in the JSON"),
				() -> assertTrue(json.has("outsideShots"), "Error: expected key outsideShots in the JSON"),
				() -> assertTrue(json.has("repeatedShots"), "Error: expected key repeatedShots in the JSON"),
				() -> assertTrue(json.has("missedShots"), "Error: expected key missedShots in the JSON"),
				() -> assertTrue(json.has("sunkBoats"), "Error: expected key sunkBoats in the JSON"),
				() -> assertTrue(json.has("hitsOnBoats"), "Error: expected key hitsOnBoats in the JSON")
		);
	}

	@Test
	@DisplayName("processEnemyFire18: falha ao serializar o JSON -> RuntimeException com a causa original")
	void processEnemyFire18() {
		Move failing = new Move(1, shots, new ArrayList<>()) {
			@Override
			ObjectMapper createObjectMapper() {
				return new ObjectMapper() {
					@Override
					public String writeValueAsString(Object value) throws JsonProcessingException {
						throw new JsonProcessingException("falha simulada pelo teste") {
						};
					}
				};
			}
		};

		RuntimeException e = assertThrows(RuntimeException.class, () -> failing.processEnemyFire(false),
				"Error: expected a RuntimeException when the JSON cannot be serialized");
		assertAll(
				() -> assertEquals("Erro ao serializar o JSON dos resultados da jogada", e.getMessage(),
						"Error: unexpected exception message: " + e.getMessage()),
				() -> assertInstanceOf(JsonProcessingException.class, e.getCause(),
						"Error: expected the cause to be a JsonProcessingException but got " + e.getCause())
		);
	}

	@Test
	@DisplayName("createObjectMapper: devolve um mapper com a indentacao do JSON ligada")
	void createObjectMapper() {
		ObjectMapper mapper = move.createObjectMapper();
		assertAll(
				() -> assertNotNull(mapper, "Error: expected a mapper but got null"),
				() -> assertTrue(mapper.isEnabled(SerializationFeature.INDENT_OUTPUT),
						"Error: expected INDENT_OUTPUT to be enabled")
		);
	}
}
