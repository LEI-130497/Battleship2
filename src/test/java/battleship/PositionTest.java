package battleship;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Test class for class Position.
 * Author: Nathan
 * Date: 2026-11-22 19:00
 * Cyclomatic Complexity:
 * - randomPosition(): 1
 * - constructor (char, int): 1
 * - constructor (int, int): 1
 * - getRow(): 1
 * - getColumn(): 1
 * - getClassicRow(): 1
 * - getClassicColumn(): 1
 * - isInside(): 5
 * - isAdjacentTo(): 2
 * - adjacentPositions(): 3
 * - isOccupied(): 1
 * - isHit(): 1
 * - occupy(): 1
 * - shoot(): 1
 * - equals(): 4
 * - hashCode(): 1
 * - toString(): 1
 */
public class PositionTest {
	private Position position;

	@BeforeEach
	void setUp() {
		position = new Position(2, 3);
	//	position = new Position('C', 4);
	}

	@AfterEach
	void tearDown() {
		position = null;
	}

	@Test
	@DisplayName("constructor: guarda linha e coluna; nasce nao ocupada e nao atingida")
	void constructor() {
		Position pos = new Position(1, 1);
		assertNotNull(pos, "Failed to create Position: object is null");
		assertEquals(1, pos.getRow(), "Failed to set row: expected 1 but got " + pos.getRow());
		assertEquals(1, pos.getColumn(), "Failed to set column: expected 1 but got " + pos.getColumn());
		assertFalse(pos.isOccupied(), "New position should not be occupied");
		assertFalse(pos.isHit(), "New position should not be hit");
	}

	@Test
	@DisplayName("getRow: devolve a linha")
	void getRow() {
		assertEquals(2, position.getRow(), "Failed to get row: expected 2 but got " + position.getRow());
	}

	@Test
	@DisplayName("getColumn: devolve a coluna")
	void getColumn() {
		assertEquals(3, position.getColumn(), "Failed to get column: expected 3 but got " + position.getColumn());
	}

	@Test
	@DisplayName("getClassicRow: linha 2 -> letra C")
	void getClassicRow() {
		assertEquals('C', position.getClassicRow(), "Error: expected classic row 'C' but got '" + position.getClassicRow() + "'");
	}

	@Test
	@DisplayName("getClassicColumn: coluna 3 -> coluna classica 4")
	void getClassicColumn() {
		assertEquals(4, position.getClassicColumn(), "Error: expected classic column 4 but got " + position.getClassicColumn());
	}

	@Test
	@DisplayName("isInside1: (0,0) esta dentro do tabuleiro")
	void isValid1() {
		position = new Position(0, 0);
		assertTrue(position.isInside(), "Position (0,0) should be valid");
	}

	@Test
	@DisplayName("isInside2: linha negativa -> fora")
	void isValid2() {
		position = new Position(-1, 5);
		assertFalse(position.isInside(), "Position with negative row should be invalid");
	}

	@Test
	@DisplayName("isInside3: coluna negativa -> fora")
	void isValid3() {
		position = new Position(5, -1);
		assertFalse(position.isInside(), "Position with negative column should be invalid");
	}

	@Test
	@DisplayName("isInside4: linha >= BOARD_SIZE -> fora")
	void isValid4() {
		position = new Position(Game.BOARD_SIZE, 5);
		assertFalse(position.isInside(), "Position with row >= BOARD_SIZE should be invalid");
	}

	@Test
	@DisplayName("isInside5: coluna >= BOARD_SIZE -> fora")
	void isValid5() {
		position = new Position(5, Game.BOARD_SIZE);
		assertFalse(position.isInside(), "Position with column >= BOARD_SIZE should be invalid");
	}

	@Test
	@DisplayName("isAdjacentTo1: vizinha na horizontal")
	void isAdjacentTo1() {
		Position other = new Position(2, 4);
		assertTrue(position.isAdjacentTo(other), "Failed to detect horizontally adjacent position");
	}

	@Test
	@DisplayName("isAdjacentTo2: vizinha na vertical")
	void isAdjacentTo2() {
		Position other = new Position(3, 3);
		assertTrue(position.isAdjacentTo(other), "Failed to detect vertically adjacent position");
	}

	@Test
	@DisplayName("isAdjacentTo3: vizinha na diagonal")
	void isAdjacentTo3() {
		Position other = new Position(3, 4);
		assertTrue(position.isAdjacentTo(other), "Failed to detect diagonally adjacent position");
	}

	@Test
	@DisplayName("isAdjacentTo4: duas linhas de distancia -> nao adjacente")
	void isAdjacentTo4() {
		Position other = new Position(4, 5);
		assertFalse(position.isAdjacentTo(other), "Non-adjacent position incorrectly identified as adjacent");
	}

	@Test
	@DisplayName("isAdjacentTo6: posicao null -> NullPointerException")
	void isAdjacentToWithNull() {
		assertThrows(NullPointerException.class, () -> position.isAdjacentTo(null),
				"isAdjacentTo should throw NullPointerException for null input");
	}

	@Test
	@DisplayName("isOccupied: passa a true depois de occupy()")
	void isOccupied() {
		assertFalse(position.isOccupied(), "New position should not be occupied");
		position.occupy();
		assertTrue(position.isOccupied(), "Position should be occupied after occupy()");
	}

	@Test
	@DisplayName("isHit: passa a true depois de shoot()")
	void isHit() {
		assertFalse(position.isHit(), "New position should not be hit");
		position.shoot();
		assertTrue(position.isHit(), "Position should be hit after shoot()");
	}

	@Test
	@DisplayName("equals1: mesma linha e mesma coluna -> iguais")
	void equals1() {
		Position same = new Position(2, 3);
		assertTrue(position.equals(same), "Equal positions not identified as equal");
	}

	@Test
	@DisplayName("equals2: comparar com null -> false")
	void equals2() {
		assertFalse(position.equals(null), "Position should not equal null");
	}

	@Test
	@DisplayName("equals3: comparar com um Object qualquer -> false")
	void equals3() {
		Object other = new Object();
		assertFalse(position.equals(other), "Position should not equal non-Position object");
	}

	@Test
	@DisplayName("equals4: mesma linha, coluna diferente -> diferentes")
	void equals4() {
		Position other = new Position(2, 4);
		assertFalse(position.equals(other), "Positions with the same row but different column should not be equal");
	}

	@Test
	@DisplayName("equals5: comparar consigo propria -> true")
	void equals5() {
		assertTrue(position.equals(position), "A position should be equal to itself");
	}

	@Test
	@DisplayName("hashCode: posicoes iguais tem o mesmo hash")
	void hashCodeConsistency() {
		Position same = new Position(2, 3);
		assertEquals(position.hashCode(), same.hashCode(),
				"Hash codes not consistent for equal positions");
	}

	@Test
	@DisplayName("toString1: (2,3) -> \"C4\"")
	void toStringFormat() {
//		String expected = "Row = C, Column = 4";
		String expected = "C4";
		assertEquals(expected, position.toString(),
				"Incorrect string representation: expected '" + expected +
						"' but got '" + position.toString() + "'");
	}

	// ------------------------------------------------------------------
	// Testes adicionados para atingir 100% de cobertura (linhas e ramos)
	// ------------------------------------------------------------------

	@Test
	@DisplayName("randomPosition: gera sempre posicoes dentro do tabuleiro")
	void randomPosition() {
		for (int i = 0; i < 500; i++) {
			Position random = Position.randomPosition();
			assertTrue(random.isInside(),
					"Error: expected a position inside the board but got " + random);
		}
	}

	@Test
	@DisplayName("constructorChar1: ('C', 4) -> linha 2, coluna 3, nao ocupada e nao atingida")
	void constructorChar1() {
		Position pos = new Position('C', 4);
		assertAll(
				() -> assertEquals(2, pos.getRow(), "Error: expected row 2 but got " + pos.getRow()),
				() -> assertEquals(3, pos.getColumn(), "Error: expected column 3 but got " + pos.getColumn()),
				() -> assertFalse(pos.isOccupied(), "Error: a new position should not be occupied"),
				() -> assertFalse(pos.isHit(), "Error: a new position should not be hit")
		);
	}

	@Test
	@DisplayName("constructorChar2: letra minuscula ('c', 4) da o mesmo resultado que 'C'")
	void constructorChar2() {
		Position pos = new Position('c', 4);
		assertAll(
				() -> assertEquals(2, pos.getRow(), "Error: expected row 2 but got " + pos.getRow()),
				() -> assertEquals(3, pos.getColumn(), "Error: expected column 3 but got " + pos.getColumn())
		);
	}

	@Test
	@DisplayName("getClassicColumnValue: coluna 3 -> coluna classica 4")
	void getClassicColumnValue() {
		assertEquals(4, position.getClassicColumn(),
				"Error: expected classic column 4 but got " + position.getClassicColumn());
	}

	@Test
	@DisplayName("isAdjacentTo5: mesma linha mas 2 colunas de distancia -> nao adjacente")
	void isAdjacentTo5() {
		Position other = new Position(2, 5);
		assertFalse(position.isAdjacentTo(other),
				"Error: expected (2,3) and (2,5) not to be adjacent but they were");
	}

	@Test
	@DisplayName("adjacentPositions1: posicao no meio do tabuleiro -> 8 vizinhas")
	void adjacentPositions1() {
		List<IPosition> adjacent = position.adjacentPositions(); // (2,3)
		assertAll(
				() -> assertEquals(8, adjacent.size(), "Error: expected 8 neighbours but got " + adjacent.size()),
				() -> assertTrue(adjacent.contains(new Position(1, 2)), "Error: expected (1,2) among the neighbours"),
				() -> assertTrue(adjacent.contains(new Position(1, 3)), "Error: expected (1,3) among the neighbours"),
				() -> assertTrue(adjacent.contains(new Position(1, 4)), "Error: expected (1,4) among the neighbours"),
				() -> assertTrue(adjacent.contains(new Position(2, 2)), "Error: expected (2,2) among the neighbours"),
				() -> assertTrue(adjacent.contains(new Position(2, 4)), "Error: expected (2,4) among the neighbours"),
				() -> assertTrue(adjacent.contains(new Position(3, 2)), "Error: expected (3,2) among the neighbours"),
				() -> assertTrue(adjacent.contains(new Position(3, 3)), "Error: expected (3,3) among the neighbours"),
				() -> assertTrue(adjacent.contains(new Position(3, 4)), "Error: expected (3,4) among the neighbours"),
				() -> assertFalse(adjacent.contains(position), "Error: the position itself must not be a neighbour")
		);
	}

	@Test
	@DisplayName("adjacentPositions2: canto (0,0) -> so 3 vizinhas dentro do tabuleiro")
	void adjacentPositions2() {
		List<IPosition> adjacent = new Position(0, 0).adjacentPositions();
		assertAll(
				() -> assertEquals(3, adjacent.size(), "Error: expected 3 neighbours but got " + adjacent.size()),
				() -> assertTrue(adjacent.contains(new Position(0, 1)), "Error: expected (0,1) among the neighbours"),
				() -> assertTrue(adjacent.contains(new Position(1, 0)), "Error: expected (1,0) among the neighbours"),
				() -> assertTrue(adjacent.contains(new Position(1, 1)), "Error: expected (1,1) among the neighbours")
		);
	}

	@Test
	@DisplayName("adjacentPositions3: canto oposto (9,9) -> so 3 vizinhas dentro do tabuleiro")
	void adjacentPositions3() {
		List<IPosition> adjacent = new Position(Game.BOARD_SIZE - 1, Game.BOARD_SIZE - 1).adjacentPositions();
		assertEquals(3, adjacent.size(), "Error: expected 3 neighbours but got " + adjacent.size());
	}

	@Test
	@DisplayName("adjacentPositions4: borda (0,5) -> 5 vizinhas dentro do tabuleiro")
	void adjacentPositions4() {
		List<IPosition> adjacent = new Position(0, 5).adjacentPositions();
		assertEquals(5, adjacent.size(), "Error: expected 5 neighbours but got " + adjacent.size());
	}

	@Test
	@DisplayName("equals6: linhas diferentes (mesma coluna) -> posicoes diferentes")
	void equals6() {
		Position other = new Position(3, 3);
		assertFalse(position.equals(other),
				"Error: expected (2,3) and (3,3) to be different but they were equal");
	}

	@Test
	@DisplayName("toString2: cantos do tabuleiro -> 'A1' e 'J10'")
	void toString2() {
		assertAll(
				() -> assertEquals("A1", new Position(0, 0).toString(),
						"Error: expected 'A1' but got " + new Position(0, 0)),
				() -> assertEquals("J10", new Position(9, 9).toString(),
						"Error: expected 'J10' but got " + new Position(9, 9))
		);
	}
}
