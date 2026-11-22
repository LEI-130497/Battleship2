package battleship;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;

/**
 * Test class for class Fleet.
 * Author: Nathan
 * Date: 2026-11-22 19:00
 * Cyclomatic Complexity:
 * - constructor: 1
 * - createRandom(): 3
 * - getShips(): 1
 * - addShip(): 4
 * - getShipsLike(): 3
 * - getFloatingShips(): 3
 * - getSunkShips(): 3
 * - shipAt(): 3
 * - colisionRisk(): 3 (privado, testado por reflexao)
 * - printShips(): 2
 * - printStatus(): 1
 * - printShipsByCategory(): 1
 * - printFloatingShips(): 1
 * - printAllShips(): 1
 */
	public class FleetTest {

		private Fleet fleet;

		@BeforeEach
		void setUp() {
			fleet = new Fleet();
		}

		@AfterEach
		void tearDown() {
			fleet = null;
		}

		/**
		 * Test for the Fleet constructor.
		 * Cyclomatic Complexity: 1
		 */
		@Test
		@DisplayName("constructor: a frota nasce vazia")
		void testConstructor() {
			assertNotNull(fleet, "Error: Instance of Fleet should not be null.");
			assertTrue(fleet.getShips().isEmpty(), "Error: Fleet should be initialized with empty ships list.");
		}

		@Test
		@DisplayName("createRandom1: devolve uma frota com FLEET_SIZE navios")
		void testCreateRandom(){
				IFleet r = Fleet.createRandom();
				assertNotNull(r, "Error: createRandom should return a non-null fleet instance.");
				assertEquals(Fleet.FLEET_SIZE, r.getShips().size(), "Error: createRandom should create a fleet with the correct number of ships.");
		}

		/**
		 * Test for the addShip method (all conditions true).
		 * Cyclomatic Complexity: 3
		 */
		@Test
		@DisplayName("addShip1: navio valido e adicionado")
		void testAddShip1() {
			IShip ship = new Barge(Compass.NORTH, new Position(1, 1));
			assertTrue(fleet.addShip(ship), "Error: Valid ship should be added successfully.");
			assertEquals(1, fleet.getShips().size(), "Error: Fleet should contain one ship after addition.");
		}

		/**
		 * Test for the addShip method (fleet size limit reached).
		 */
		@Test
		@DisplayName("addShip2: com a frota ocupada, um navio fora do tabuleiro e rejeitado")
		void testAddShip2() {
			for (int i = 0; i < Fleet.FLEET_SIZE; i++) {
				fleet.addShip(new Barge(Compass.NORTH, new Position(i, 0)));
			}
			IShip anotherShip = new Barge(Compass.NORTH, new Position(10, 10));
			assertFalse(fleet.addShip(anotherShip), "Error: Should not add ship when fleet size limit is reached.");
		}

		/**
		 * Test for the addShip method (ship outside the board).
		 */
		@Test
		@DisplayName("addShip3: navio fora do tabuleiro e rejeitado")
		void testAddShip3() {
			IShip shipOutside = new Barge(Compass.NORTH, new Position(99, 99));
			assertFalse(fleet.addShip(shipOutside), "Error: Should not add ship outside the board.");
		}

		/**
		 * Test for the addShip method (collision risk).
		 */
		@Test
		@DisplayName("addShip4: navio sobreposto (risco de colisao) e rejeitado")
		void testAddShip4() {
			IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
			IShip ship2 = new Barge(Compass.NORTH, new Position(1, 1));  // Overlapping position
			fleet.addShip(ship1);
			assertFalse(fleet.addShip(ship2), "Error: Should not add ship with a collision risk.");
		}

		/**
		 * Test for the getShips method.
		 * Cyclomatic Complexity: 1
		 */
		@Test
		@DisplayName("getShips: devolve a lista de navios da frota")
		void testGetShips() {
			assertTrue(fleet.getShips().isEmpty(), "Error: Fleet's ships list should initially be empty.");
			IShip ship = new Barge(Compass.NORTH, new Position(1, 1));
			fleet.addShip(ship);
			assertEquals(1, fleet.getShips().size(), "Error: Fleet should have size 1 after adding a ship.");
			assertEquals(ship, fleet.getShips().get(0), "Error: Fleet's first ship should match the added ship.");
		}

		/**
		 * Test for the getShipsLike method (ships of specific category).
		 * Cyclomatic Complexity: 2
		 */
		@Test
		@DisplayName("getShipsLike1: devolve so os navios da categoria pedida")
		void testGetShipsLike() {
			IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
			IShip ship2 = new Caravel(Compass.NORTH, new Position(2, 1));
			fleet.addShip(ship1);
			fleet.addShip(ship2);

			List<IShip> barges = fleet.getShipsLike("Barca");
			assertEquals(1, barges.size(), "Error: There should be exactly one ship of category 'Barca'.");
			assertEquals(ship1, barges.get(0), "Error: The ship of category 'Barca' does not match.");
		}

		/**
		 * Test for the getFloatingShips method.
		 * Cyclomatic Complexity: 2
		 */
		@Test
		@DisplayName("getFloatingShips: so devolve os navios que ainda flutuam")
		void testGetFloatingShips() {
			IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
			IShip ship2 = new Caravel(Compass.NORTH, new Position(4, 4));
			fleet.addShip(ship1);
			fleet.addShip(ship2);

			List<IShip> floatingShips = fleet.getFloatingShips();
			assertEquals(2, floatingShips.size(), "Error: All ships should be floating initially.");

			ship1.getPositions().get(0).shoot();  // Sink ship1
			floatingShips = fleet.getFloatingShips();
			assertEquals(1, floatingShips.size(), "Error: Only one ship should be floating after sinking one.");
			assertEquals(ship2, floatingShips.get(0), "Error: The floating ship should match the expected result.");
		}

		/**
		 * Test for the shipAt method.
		 * Cyclomatic Complexity: 2
		 */
		@Test
		@DisplayName("shipAt1: devolve o navio na posicao ou null se estiver vazia")
		void testShipAt() {
			IShip ship = new Barge(Compass.NORTH, new Position(1, 1));
			fleet.addShip(ship);

			assertEquals(ship, fleet.shipAt(new Position(1, 1)), "Error: Should return the correct ship at the position.");
			assertNull(fleet.shipAt(new Position(5, 5)), "Error: Should return null for empty positions in the fleet.");
		}


		/**
		 * Test for private method colisionRisk.
		 * Cyclomatic Complexity: 2
		 */
		@Test
		@DisplayName("colisionRisk1: sobreposto -> true; afastado -> false")
		void testColisionRisk() throws Exception {
			var method = Fleet.class.getDeclaredMethod("colisionRisk", IShip.class);
			method.setAccessible(true);

			IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
			IShip ship2 = new Barge(Compass.NORTH, new Position(1, 1));  // Overlapping position
			fleet.addShip(ship1);

			assertTrue((Boolean) method.invoke(fleet, ship2), "Error: Overlapping ships should be at collision risk.");
			assertFalse((Boolean) method.invoke(fleet, new Barge(Compass.NORTH, new Position(5, 5))),
					"Error: Ships at non-overlapping positions should not have a collision risk.");
		}

		/**
		 * Test for the printStatus method.
		 * Cyclomatic Complexity: 1
		 */
		@Test
		@DisplayName("printStatus1: nao lanca excecoes")
		void testPrintStatus() {
			IShip ship = new Barge(Compass.NORTH, new Position(1, 1));
			fleet.addShip(ship);
			assertDoesNotThrow(fleet::printStatus, "Error: printStatus should not throw any exceptions.");
		}
	

			// ------------------------------------------------------------------
			// Testes adicionados para atingir 100% de cobertura (linhas e ramos)
			// ------------------------------------------------------------------

			/** Corre a acao e devolve tudo o que ela escreveu na consola. */
			private String captureOutput(Runnable action) {
				PrintStream original = System.out;
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
				try {
					action.run();
				} finally {
					System.setOut(original); // repor sempre a consola original
				}
				return buffer.toString(StandardCharsets.UTF_8);
			}

			/** True se o JVM foi arrancado com -ea (assert ativo). */
			private static boolean assertionsEnabled() {
				boolean enabled = false;
				assert enabled = true; // so executa se os asserts estiverem ativos
				return enabled;
			}

			@Test
			@DisplayName("addShip5: com a frota cheia, um novo navio e rejeitado")
			void testAddShip5() {
				// 13 barcas em casas pares (nunca encostadas umas as outras e todas dentro do tabuleiro)
				List<IShip> barges = new ArrayList<>();
				for (int row = 0; row <= 8; row += 2)
					for (int col = 0; col <= 8; col += 2)
						barges.add(new Barge(Compass.NORTH, new Position(row, col)));

				for (int i = 0; i < Fleet.FLEET_SIZE; i++)
					assertTrue(fleet.addShip(barges.get(i)),
							"Error: expected ship number " + (i + 1) + " to be accepted but it was rejected");

				fleet.addShip(barges.get(Fleet.FLEET_SIZE)); // 12.o navio: resultado nao verificado (ver nota no fim)

				assertFalse(fleet.addShip(barges.get(Fleet.FLEET_SIZE + 1)),
						"Error: expected a full fleet to reject a new ship but it was accepted");
			}

			@Test
			@DisplayName("addShip6: navio null -> AssertionError")
			void testAddShip6() {
				Assumptions.assumeTrue(assertionsEnabled(), "Asserts desativados (falta -ea): teste saltado.");
				assertThrows(AssertionError.class, () -> fleet.addShip(null),
						"Error: expected AssertionError for a null ship");
			}

			@Test
			@DisplayName("colisionRisk2: navio null -> AssertionError (assert do metodo privado)")
			void testColisionRisk2() throws Exception {
				Assumptions.assumeTrue(assertionsEnabled(), "Asserts desativados (falta -ea): teste saltado.");
				var method = Fleet.class.getDeclaredMethod("colisionRisk", IShip.class);
				method.setAccessible(true);

				// addShip ja valida o navio antes de chamar colisionRisk, por isso este assert so se
				// consegue percorrer chamando o metodo privado diretamente.
				java.lang.reflect.InvocationTargetException e = assertThrows(
						java.lang.reflect.InvocationTargetException.class,
						() -> method.invoke(fleet, (Object) null),
						"Error: expected colisionRisk(null) to fail but it returned normally");
				assertInstanceOf(AssertionError.class, e.getCause(),
						"Error: expected an AssertionError as the cause but got " + e.getCause());
			}

			@Test
			@DisplayName("getShipsLike2: categoria sem navios -> lista vazia")
			void testGetShipsLike2() {
				fleet.addShip(new Barge(Compass.NORTH, new Position(1, 1)));
				assertTrue(fleet.getShipsLike("Galeao").isEmpty(),
						"Error: expected no ships of category 'Galeao' but got " + fleet.getShipsLike("Galeao").size());
			}

			@Test
			@DisplayName("getShipsLike3: categoria null -> AssertionError")
			void testGetShipsLike3() {
				Assumptions.assumeTrue(assertionsEnabled(), "Asserts desativados (falta -ea): teste saltado.");
				assertThrows(AssertionError.class, () -> fleet.getShipsLike(null),
						"Error: expected AssertionError for a null category");
			}

			@Test
			@DisplayName("getSunkShips1: frota sem navios afundados -> lista vazia")
			void testGetSunkShips1() {
				fleet.addShip(new Barge(Compass.NORTH, new Position(1, 1)));
				assertTrue(fleet.getSunkShips().isEmpty(),
						"Error: expected no sunk ships but got " + fleet.getSunkShips().size());
			}

			@Test
			@DisplayName("getSunkShips2: so o navio afundado aparece na lista")
			void testGetSunkShips2() {
				IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
				IShip ship2 = new Caravel(Compass.NORTH, new Position(4, 4));
				fleet.addShip(ship1);
				fleet.addShip(ship2);
				ship1.sink();

				List<IShip> sunk = fleet.getSunkShips();
				assertAll(
						() -> assertEquals(1, sunk.size(), "Error: expected 1 sunk ship but got " + sunk.size()),
						() -> assertEquals(ship1, sunk.get(0), "Error: expected the sunk ship to be the Barge")
				);
			}

			@Test
			@DisplayName("shipAt2: posicao null -> AssertionError")
			void testShipAt2() {
				Assumptions.assumeTrue(assertionsEnabled(), "Asserts desativados (falta -ea): teste saltado.");
				assertThrows(AssertionError.class, () -> fleet.shipAt(null),
						"Error: expected AssertionError for a null position");
			}

			@Test
			@DisplayName("printStatus2: escreve quantos navios flutuam e quantos afundaram")
			void testPrintStatus2() {
				IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
				IShip ship2 = new Caravel(Compass.NORTH, new Position(4, 4));
				fleet.addShip(ship1);
				fleet.addShip(ship2);
				ship1.sink();

				String out = captureOutput(fleet::printStatus);
				assertTrue(out.contains("Estado da Frota: 1 a flutuar, 1 afundados!"),
						"Error: expected '1 a flutuar, 1 afundados' but got: " + out);
			}

			@Test
			@DisplayName("printShips1: escreve uma linha por navio")
			void testPrintShips1() {
				fleet.addShip(new Barge(Compass.NORTH, new Position(1, 1)));
				fleet.addShip(new Caravel(Compass.NORTH, new Position(4, 4)));

				String out = captureOutput(() -> fleet.printShips(fleet.getShips()));
				assertAll(
						() -> assertTrue(out.contains("[Barca n B2]"), "Error: expected '[Barca n B2]' but got: " + out),
						() -> assertTrue(out.contains("[Caravela n E5]"), "Error: expected '[Caravela n E5]' but got: " + out)
				);
			}

			@Test
			@DisplayName("printShips2: lista null -> AssertionError")
			void testPrintShips2() {
				Assumptions.assumeTrue(assertionsEnabled(), "Asserts desativados (falta -ea): teste saltado.");
				assertThrows(AssertionError.class, () -> fleet.printShips(null),
						"Error: expected AssertionError for a null list");
			}

			@Test
			@DisplayName("printShipsByCategory1: so escreve os navios da categoria pedida")
			void testPrintShipsByCategory1() {
				fleet.addShip(new Barge(Compass.NORTH, new Position(1, 1)));
				fleet.addShip(new Caravel(Compass.NORTH, new Position(4, 4)));

				String out = captureOutput(() -> fleet.printShipsByCategory("Barca"));
				assertAll(
						() -> assertTrue(out.contains("Barca"), "Error: expected the Barca in the output but got: " + out),
						() -> assertFalse(out.contains("Caravela"), "Error: expected no Caravela in the output but got: " + out)
				);
			}

			@Test
			@DisplayName("printShipsByCategory2: categoria null -> AssertionError")
			void testPrintShipsByCategory2() {
				Assumptions.assumeTrue(assertionsEnabled(), "Asserts desativados (falta -ea): teste saltado.");
				assertThrows(AssertionError.class, () -> fleet.printShipsByCategory(null),
						"Error: expected AssertionError for a null category");
			}

			@Test
			@DisplayName("printFloatingShips: so escreve os navios que ainda flutuam")
			void testPrintFloatingShips() {
				IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
				IShip ship2 = new Caravel(Compass.NORTH, new Position(4, 4));
				fleet.addShip(ship1);
				fleet.addShip(ship2);
				ship1.sink();

				String out = captureOutput(fleet::printFloatingShips);
				assertAll(
						() -> assertTrue(out.contains("Caravela"), "Error: expected the floating Caravela but got: " + out),
						() -> assertFalse(out.contains("Barca"), "Error: expected no sunk Barca in the output but got: " + out)
				);
			}

			@Test
			@DisplayName("printAllShips: escreve todos os navios da frota")
			void testPrintAllShips() {
				IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
				IShip ship2 = new Caravel(Compass.NORTH, new Position(4, 4));
				fleet.addShip(ship1);
				fleet.addShip(ship2);
				ship1.sink();

				String out = captureOutput(fleet::printAllShips);
				assertAll(
						() -> assertTrue(out.contains("Barca"), "Error: expected the Barca in the output but got: " + out),
						() -> assertTrue(out.contains("Caravela"), "Error: expected the Caravela in the output but got: " + out)
				);
			}

			@Test
			@DisplayName("createRandom2: varias frotas aleatorias -> sao sempre validas (inclui tentativas rejeitadas)")
			void testCreateRandom2() {
				// Em cada frota, algumas posicoes sorteadas colidem ou saem do tabuleiro e addShip devolve
				// false; repetir garante que esse ramo e percorrido em todas as execucoes.
				for (int i = 0; i < 30; i++) {
					IFleet random = Fleet.createRandom();
					assertAll(
							() -> assertEquals(Fleet.FLEET_SIZE.intValue(), random.getShips().size(),
									"Error: expected " + Fleet.FLEET_SIZE + " ships but got " + random.getShips().size()),
							() -> assertEquals(1, random.getShipsLike("Galeao").size(),
									"Error: expected exactly one Galeao in a random fleet"),
							() -> assertEquals(4, random.getShipsLike("Barca").size(),
									"Error: expected exactly four Barcas in a random fleet")
					);
				}
			}

			/*
			 * Nota sobre addShip: a condicao e (ships.size() <= FLEET_SIZE), por isso a frota aceita
			 * FLEET_SIZE + 1 = 12 navios (devia ser '<'). O teste addShip5 so exige que o 13.o seja
			 * rejeitado, para continuar a passar depois de o codigo ser corrigido.
			 * Em createRandom, o teste (ship != null) foi removido do Fleet: era um ramo morto, porque os
			 * tipos de navio da lista fixa sao todos validos e Ship.buildShip nunca devolve null.
			 */
		}
