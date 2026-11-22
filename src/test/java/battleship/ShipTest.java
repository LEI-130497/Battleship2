package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Ship (JUnit 6, without Mockito).
 * Author: ${user.name} (replace using the IntelliJ file-template variable).
 * Date: 2026-11-22
 * Time: 17:35 (Europe/Lisbon)
 *
 * Cyclomatic Complexity (estimated from the supplied source; excluding Java assert):
 * - buildShip(String, Compass, Position): 6
 * - Ship(String, Compass, IPosition, int): 1
 * - getCategory(): 1; getPositions(): 1; getPosition(): 1
 * - getBearing(): 1; getSize(): 1
 * - getAdjacentPositions(): 5
 * - stillFloating(): 3
 * - getTopMostPos(): 3; getBottomMostPos(): 3
 * - getLeftMostPos(): 3; getRightMostPos(): 3
 * - occupies(IPosition): 3
 * - tooCloseTo(IShip): 3; tooCloseTo(IPosition): 3
 * - isInsideBoard(): 5
 * - shoot(IPosition): 3; sink(): 2; toString(): 1
 *
 * Extra tests exercise exception contracts and boundary values.
 * Java assertion tests require -ea and are skipped otherwise.
 */
public class ShipTest {

    private Ship ship;
    private Ship shipBorder;

    /**
     * Minimal concrete implementation to control positions independently
     * of the placement rules of Barge, Frigate, Galleon, etc.
     */
    private static final class FixtureShip extends Ship {
        FixtureShip(String category, Compass bearing, IPosition anchor, IPosition... occupied) {
            super(category, bearing, anchor, occupied.length);
            for (IPosition cell : occupied) {
                positions.add(cell);
            }
        }
    }

    private static Position p(int row, int column) {
        return new Position(row, column);
    }

    private static Ship withPositions(Position... occupied) {
        if (occupied.length == 0) {
            return new FixtureShip("teste", Compass.NORTH, p(5, 5));
        }
        return new FixtureShip("teste", Compass.NORTH, occupied[0], occupied);
    }

    @BeforeEach
    void setUp() {
        ship = new Barge(Compass.NORTH, p(5, 5));
        shipBorder = new Barge(Compass.NORTH, p(0, 0));
    }

    @AfterEach
    void tearDown() {
        ship = null;
        shipBorder = null;
    }

    // ----- Factory: all five case labels and the default path -----

    @Test
    @DisplayName("buildShip 1: barca creates a Barge")
    void buildShip1() {
        assertInstanceOf(Barge.class, Ship.buildShip("barca", Compass.NORTH, p(5, 5)),
                "Error: 'barca' should create a Barge.");
    }

    @Test
    @DisplayName("buildShip 2: caravela creates a Caravel")
    void buildShip2() {
        assertInstanceOf(Caravel.class, Ship.buildShip("caravela", Compass.NORTH, p(5, 5)),
                "Error: 'caravela' should create a Caravel.");
    }

    @Test
    @DisplayName("buildShip 3: nau creates a Carrack")
    void buildShip3() {
        assertInstanceOf(Carrack.class, Ship.buildShip("nau", Compass.NORTH, p(5, 5)),
                "Error: 'nau' should create a Carrack.");
    }

    @Test
    @DisplayName("buildShip 4: fragata creates a Frigate")
    void buildShip4() {
        assertInstanceOf(Frigate.class, Ship.buildShip("fragata", Compass.NORTH, p(5, 5)),
                "Error: 'fragata' should create a Frigate.");
    }

    @Test
    @DisplayName("buildShip 5: galeao creates a Galleon")
    void buildShip5() {
        assertInstanceOf(Galleon.class, Ship.buildShip("galeao", Compass.NORTH, p(5, 5)),
                "Error: 'galeao' should create a Galleon.");
    }

    @Test
    @DisplayName("buildShip 6: unknown or incorrectly cased category returns null")
    void buildShip6() {
        assertAll(
                () -> assertNull(Ship.buildShip("desconhecido", Compass.NORTH, p(5, 5)),
                        "Error: an unknown ship category should return null."),
                () -> assertNull(Ship.buildShip("BARCA", Compass.NORTH, p(5, 5)),
                        "Error: factory category matching should be case-sensitive.")
        );
    }

    @Test
    @DisplayName("buildShip: null category triggers Java assertion when enabled")
    void buildShipNullCategory() {
        Assumptions.assumeTrue(Ship.class.desiredAssertionStatus(),
                "Run Maven/IntelliJ tests with -ea to exercise Java assert statements.");
        assertThrows(AssertionError.class, () -> Ship.buildShip(null, Compass.NORTH, p(5, 5)),
                "Error: null category should violate the factory's precondition.");
    }

    @Test
    @DisplayName("buildShip: null compass triggers Java assertion when enabled")
    void buildShipNullCompass() {
        Assumptions.assumeTrue(Ship.class.desiredAssertionStatus(),
                "Run Maven/IntelliJ tests with -ea to exercise Java assert statements.");
        assertThrows(AssertionError.class, () -> Ship.buildShip("barca", null, p(5, 5)),
                "Error: null compass should violate the factory's precondition.");
    }

    @Test
    @DisplayName("buildShip: null position triggers Java assertion when enabled")
    void buildShipNullPosition() {
        Assumptions.assumeTrue(Ship.class.desiredAssertionStatus(),
                "Run Maven/IntelliJ tests with -ea to exercise Java assert statements.");
        assertThrows(AssertionError.class, () -> Ship.buildShip("barca", Compass.NORTH, null),
                "Error: null position should violate the factory's precondition.");
    }

    // ----- Constructor / straightforward getters -----

    @Test
    @DisplayName("constructor: initializes category, compass, position, size and positions")
    void constructor() {
        Position origin = p(5, 5);
        Ship created = new FixtureShip("teste", Compass.NORTH, origin, origin);
        assertAll(
                () -> assertNotNull(created, "Error: the Ship object should exist."),
                () -> assertEquals("teste", created.getCategory(),
                        "Error: category should match the constructor argument."),
                () -> assertSame(origin, created.getPosition(),
                        "Error: the anchor position should be retained."),
                () -> assertEquals(Compass.NORTH, created.getBearing(),
                        "Error: compass should match the constructor argument."),
                () -> assertEquals(1, created.getSize(),
                        "Error: size should match the occupied-position count."),
                () -> assertEquals(1, created.getPositions().size(),
                        "Error: the fixture should contain one occupied position.")
        );
    }

    @Test
    @DisplayName("constructor: null category throws NullPointerException")
    void constructorNullCategory() {
        assertThrows(NullPointerException.class,
                () -> new FixtureShip(null, Compass.NORTH, p(5, 5)),
                "Error: a null ship category should be rejected.");
    }

    @Test
    @DisplayName("constructor: null compass throws NullPointerException")
    void constructorNullBearing() {
        assertThrows(NullPointerException.class,
                () -> new FixtureShip("teste", null, p(5, 5)),
                "Error: a null compass should be rejected.");
    }

    @Test
    @DisplayName("constructor: null anchor throws NullPointerException")
    void constructorNullPosition() {
        assertThrows(NullPointerException.class,
                () -> new FixtureShip("teste", Compass.NORTH, null),
                "Error: a null anchor should be rejected.");
    }

    @Test
    @DisplayName("getCategory: returns the category assigned by the constructor")
    void getCategory() {
        assertEquals("Barca", ship.getCategory(),
                "Error: Barge should report category 'Barca'.");
    }

    @Test
    @DisplayName("getPosition: returns the original anchor coordinates")
    void getPosition() {
        assertEquals(p(5, 5), ship.getPosition(),
                "Error: anchor should be position (5,5).");
    }

    @Test
    @DisplayName("getBearing: returns NORTH")
    void getBearing() {
        assertEquals(Compass.NORTH, ship.getBearing(),
                "Error: bearing should be NORTH.");
    }

    @Test
    @DisplayName("getSize: returns one for a barge")
    void getSize() {
        assertEquals(1, ship.getSize(),
                "Error: Barge should occupy exactly one cell.");
    }

    @Test
    @DisplayName("getPositions: returns the original mutable list of occupied cells")
    void getPositions() {
        List<IPosition> cells = ship.getPositions();
        assertAll(
                () -> assertNotNull(cells, "Error: occupied positions cannot be null."),
                () -> assertSame(cells, ship.getPositions(),
                        "Error: getPositions should return the stored list."),
                () -> assertEquals(1, cells.size(),
                        "Error: Barge should have one occupied cell."),
                () -> assertEquals(p(5, 5), cells.get(0),
                        "Error: the cell should be at (5,5).")
        );
    }

    // ----- Adjacent positions: zero cells, middle, edge and deduplication -----

    @Test
    @DisplayName("getAdjacentPositions 1: empty ship has no neighboring positions")
    void getAdjacentPositions1() {
        assertTrue(withPositions().getAdjacentPositions().isEmpty(),
                "Error: no occupied cells should produce no adjacent positions.");
    }

    @Test
    @DisplayName("getAdjacentPositions 2: central barge has eight unique neighbors")
    void getAdjacentPositions2() {
        List<IPosition> adjacent = ship.getAdjacentPositions();
        assertAll(
                () -> assertEquals(8, adjacent.size(),
                        "Error: a barge at (5,5) should have eight neighbors."),
                () -> assertEquals(adjacent.size(), new HashSet<>(adjacent).size(),
                        "Error: adjacent positions should not contain duplicates."),
                () -> assertFalse(adjacent.contains(p(5, 5)),
                        "Error: an occupied position must not be adjacent to itself."),
                () -> assertTrue(adjacent.contains(p(4, 4)),
                        "Error: diagonally adjacent cell (4,4) should be present.")
        );
    }

    @Test
    @DisplayName("getAdjacentPositions 3: corner barge has three neighbors")
    void getAdjacentPositions3() {
        List<IPosition> adjacent = shipBorder.getAdjacentPositions();
        assertAll(
                () -> assertEquals(3, adjacent.size(),
                        "Error: a barge in the top-left corner should have three neighbors."),
                () -> assertTrue(adjacent.containsAll(List.of(p(0, 1), p(1, 0), p(1, 1))),
                        "Error: the three legal corner neighbors are missing.")
        );
    }

    @Test
    @DisplayName("getAdjacentPositions 4: neighboring ship cells are excluded, duplicates removed")
    void getAdjacentPositions4() {
        Ship twoCells = withPositions(p(5, 5), p(5, 6));
        List<IPosition> adjacent = twoCells.getAdjacentPositions();
        assertAll(
                () -> assertEquals(10, adjacent.size(),
                        "Error: two horizontally adjacent cells should have ten external neighbors."),
                () -> assertEquals(10, new HashSet<>(adjacent).size(),
                        "Error: shared neighbors must be returned only once."),
                () -> assertFalse(adjacent.contains(p(5, 5)),
                        "Error: the first occupied cell should not appear in the adjacent list."),
                () -> assertFalse(adjacent.contains(p(5, 6)),
                        "Error: the second occupied cell should not appear in the adjacent list.")
        );
    }

    // ----- Floating state: first intact, later intact, no intact -----

    @Test
    @DisplayName("stillFloating 1: one untouched cell keeps the ship afloat")
    void stillFloating1() {
        assertTrue(ship.stillFloating(),
                "Error: a new undamaged barge should be floating.");
    }

    @Test
    @DisplayName("stillFloating 2: later intact cell keeps partially hit ship afloat")
    void stillFloating2() {
        Ship multi = withPositions(p(5, 5), p(5, 6), p(5, 7));
        multi.shoot(p(5, 5));
        assertTrue(multi.stillFloating(),
                "Error: the ship should be floating while cells (5,6) and (5,7) are intact.");
    }

    @Test
    @DisplayName("stillFloating 3: fully hit ship is sunk")
    void stillFloating3() {
        Ship multi = withPositions(p(5, 5), p(5, 6));
        multi.shoot(p(5, 5));
        multi.shoot(p(5, 6));
        assertFalse(multi.stillFloating(),
                "Error: a ship with all positions hit should no longer be floating.");
    }

    @Test
    @DisplayName("stillFloating: zero-length fixture is not floating")
    void stillFloatingEmpty() {
        assertFalse(withPositions().stillFloating(),
                "Error: a ship with zero occupied cells has no intact positions.");
    }

    // ----- Topmost: no comparison, smaller found, smaller not found -----

    @Test
    @DisplayName("getTopMostPos 1: one cell")
    void getTopMostPos1() {
        assertEquals(5, ship.getTopMostPos(),
                "Error: the only occupied row should be 5.");
    }

    @Test
    @DisplayName("getTopMostPos 2: a later row is smaller")
    void getTopMostPos2() {
        Ship multi = withPositions(p(7, 5), p(2, 5), p(4, 5));
        assertEquals(2, multi.getTopMostPos(),
                "Error: smallest occupied row should be 2.");
    }

    @Test
    @DisplayName("getTopMostPos 3: the first row remains smallest")
    void getTopMostPos3() {
        Ship multi = withPositions(p(2, 5), p(4, 5), p(7, 5));
        assertEquals(2, multi.getTopMostPos(),
                "Error: the initial smallest row should remain 2.");
    }

    // ----- Bottommost -----

    @Test
    @DisplayName("getBottomMostPos 1: one cell")
    void getBottomMostPos1() {
        assertEquals(5, ship.getBottomMostPos(),
                "Error: the only occupied row should be 5.");
    }

    @Test
    @DisplayName("getBottomMostPos 2: a later row is larger")
    void getBottomMostPos2() {
        Ship multi = withPositions(p(2, 5), p(7, 5), p(4, 5));
        assertEquals(7, multi.getBottomMostPos(),
                "Error: largest occupied row should be 7.");
    }

    @Test
    @DisplayName("getBottomMostPos 3: the first row remains largest")
    void getBottomMostPos3() {
        Ship multi = withPositions(p(7, 5), p(2, 5), p(4, 5));
        assertEquals(7, multi.getBottomMostPos(),
                "Error: the initial largest row should remain 7.");
    }

    // ----- Leftmost -----

    @Test
    @DisplayName("getLeftMostPos 1: one cell")
    void getLeftMostPos1() {
        assertEquals(5, ship.getLeftMostPos(),
                "Error: the only occupied column should be 5.");
    }

    @Test
    @DisplayName("getLeftMostPos 2: a later column is smaller")
    void getLeftMostPos2() {
        Ship multi = withPositions(p(5, 7), p(5, 2), p(5, 4));
        assertEquals(2, multi.getLeftMostPos(),
                "Error: smallest occupied column should be 2.");
    }

    @Test
    @DisplayName("getLeftMostPos 3: the first column remains smallest")
    void getLeftMostPos3() {
        Ship multi = withPositions(p(5, 2), p(5, 4), p(5, 7));
        assertEquals(2, multi.getLeftMostPos(),
                "Error: the initial smallest column should remain 2.");
    }

    // ----- Rightmost -----

    @Test
    @DisplayName("getRightMostPos 1: one cell")
    void getRightMostPos1() {
        assertEquals(5, ship.getRightMostPos(),
                "Error: the only occupied column should be 5.");
    }

    @Test
    @DisplayName("getRightMostPos 2: a later column is larger")
    void getRightMostPos2() {
        Ship multi = withPositions(p(5, 2), p(5, 7), p(5, 4));
        assertEquals(7, multi.getRightMostPos(),
                "Error: largest occupied column should be 7.");
    }

    @Test
    @DisplayName("getRightMostPos 3: the first column remains largest")
    void getRightMostPos3() {
        Ship multi = withPositions(p(5, 7), p(5, 2), p(5, 4));
        assertEquals(7, multi.getRightMostPos(),
                "Error: the initial largest column should remain 7.");
    }

    // ----- occupies: match at first, match later, no match -----

    @Test
    @DisplayName("occupies 1: first occupied position matches")
    void occupies1() {
        assertTrue(ship.occupies(p(5, 5)),
                "Error: the barge should occupy position (5,5).");
    }

    @Test
    @DisplayName("occupies 2: a later occupied position matches")
    void occupies2() {
        Ship multi = withPositions(p(5, 5), p(5, 6));
        assertTrue(multi.occupies(p(5, 6)),
                "Error: the second occupied position should be found.");
    }

    @Test
    @DisplayName("occupies 3: target is not occupied")
    void occupies3() {
        assertFalse(ship.occupies(p(1, 1)),
                "Error: the barge should not occupy position (1,1).");
    }

    @Test
    @DisplayName("occupies: empty fixture occupies no cells")
    void occupiesEmpty() {
        assertFalse(withPositions().occupies(p(5, 5)),
                "Error: a zero-length fixture cannot occupy a cell.");
    }

    // ----- tooCloseTo(IShip): first close, later close, all far/empty -----

    @Test
    @DisplayName("tooCloseTo(IShip) 1: another barge is adjacent")
    void tooCloseToShip1() {
        Ship adjacent = new Barge(Compass.NORTH, p(5, 6));
        assertTrue(ship.tooCloseTo(adjacent),
                "Error: another barge one column away should be too close.");
    }

    @Test
    @DisplayName("tooCloseTo(IShip) 2: later cell of other ship is adjacent")
    void tooCloseToShip2() {
        Ship other = withPositions(p(0, 0), p(5, 6));
        assertTrue(ship.tooCloseTo(other),
                "Error: the second cell of the other ship should trigger adjacency.");
    }

    @Test
    @DisplayName("tooCloseTo(IShip) 3: both ships are far apart")
    void tooCloseToShip3() {
        Ship far = new Barge(Compass.NORTH, p(9, 9));
        assertFalse(ship.tooCloseTo(far),
                "Error: ships at (5,5) and (9,9) should not be adjacent.");
    }

    @Test
    @DisplayName("tooCloseTo(IShip): an empty other ship is not adjacent")
    void tooCloseToShipEmpty() {
        assertFalse(ship.tooCloseTo(withPositions()),
                "Error: a ship with no occupied positions cannot be adjacent.");
    }

    // ----- tooCloseTo(IPosition): first close, later close, none close -----

    @Test
    @DisplayName("tooCloseTo(IPosition) 1: position is adjacent to first cell")
    void tooCloseToPosition1() {
        assertTrue(ship.tooCloseTo(p(5, 6)),
                "Error: position (5,6) should be adjacent to (5,5).");
    }

    @Test
    @DisplayName("tooCloseTo(IPosition) 2: position is adjacent to a later cell")
    void tooCloseToPosition2() {
        Ship multi = withPositions(p(5, 5), p(5, 8));
        assertTrue(multi.tooCloseTo(p(5, 9)),
                "Error: position (5,9) should be adjacent to the second cell.");
    }

    @Test
    @DisplayName("tooCloseTo(IPosition) 3: position is not adjacent")
    void tooCloseToPosition3() {
        assertFalse(ship.tooCloseTo(p(8, 8)),
                "Error: position (8,8) should not be adjacent to (5,5).");
    }

    // ----- Compound && in isInsideBoard: each short-circuit outcome -----

    @Test
    @DisplayName("isInsideBoard 1: all four bounds are valid")
    void isInsideBoard1() {
        assertAll(
                () -> assertTrue(ship.isInsideBoard(),
                        "Error: a barge at (5,5) should be inside the board."),
                () -> assertTrue(shipBorder.isInsideBoard(),
                        "Error: a barge at (0,0) should be inside the board."),
                () -> assertTrue(withPositions(p(9, 9)).isInsideBoard(),
                        "Error: (9,9) should be a valid border position.")
        );
    }

    @Test
    @DisplayName("isInsideBoard 2: leftmost column is negative")
    void isInsideBoard2() {
        assertFalse(withPositions(p(5, -1)).isInsideBoard(),
                "Error: a negative column should put the ship outside the board.");
    }

    @Test
    @DisplayName("isInsideBoard 3: rightmost column exceeds board width")
    void isInsideBoard3() {
        assertFalse(withPositions(p(5, Game.BOARD_SIZE)).isInsideBoard(),
                "Error: a column equal to BOARD_SIZE should be outside the board.");
    }

    @Test
    @DisplayName("isInsideBoard 4: topmost row is negative")
    void isInsideBoard4() {
        assertFalse(withPositions(p(-1, 5)).isInsideBoard(),
                "Error: a negative row should put the ship outside the board.");
    }

    @Test
    @DisplayName("isInsideBoard 5: bottommost row exceeds board height")
    void isInsideBoard5() {
        assertFalse(withPositions(p(Game.BOARD_SIZE, 5)).isInsideBoard(),
                "Error: a row equal to BOARD_SIZE should be outside the board.");
    }

    // ----- shoot: first-cell hit, later-cell hit, valid non-matching shot -----

    @Test
    @DisplayName("shoot 1: shooting the barge's cell hits and sinks it")
    void shoot1() {
        ship.shoot(p(5, 5));
        assertAll(
                () -> assertTrue(ship.getPositions().get(0).isHit(),
                        "Error: position (5,5) should be marked hit."),
                () -> assertFalse(ship.stillFloating(),
                        "Error: the one-cell barge should sink after one hit.")
        );
    }

    @Test
    @DisplayName("shoot 2: shooting the second cell hits only that cell")
    void shoot2() {
        Ship multi = withPositions(p(5, 5), p(5, 6));
        multi.shoot(p(5, 6));
        assertAll(
                () -> assertFalse(multi.getPositions().get(0).isHit(),
                        "Error: first position should remain intact."),
                () -> assertTrue(multi.getPositions().get(1).isHit(),
                        "Error: second position should be hit."),
                () -> assertTrue(multi.stillFloating(),
                        "Error: the ship should still be floating with the first cell intact.")
        );
    }

    @Test
    @DisplayName("shoot 3: valid target outside the ship does not mark any cell")
    void shoot3() {
        ship.shoot(p(0, 0));
        assertFalse(ship.getPositions().get(0).isHit(),
                "Error: an unrelated valid target must not damage the ship.");
    }

    @Test
    @DisplayName("shoot: null target triggers Java assertion when enabled")
    void shootNull() {
        Assumptions.assumeTrue(Ship.class.desiredAssertionStatus(),
                "Run Maven/IntelliJ tests with -ea to exercise Java assert statements.");
        assertThrows(AssertionError.class, () -> ship.shoot(null),
                "Error: shooting null should violate the shoot precondition.");
    }

    @Test
    @DisplayName("shoot: off-board target triggers Java assertion when enabled")
    void shootOutside() {
        Assumptions.assumeTrue(Ship.class.desiredAssertionStatus(),
                "Run Maven/IntelliJ tests with -ea to exercise Java assert statements.");
        assertThrows(AssertionError.class, () -> ship.shoot(p(-1, 5)),
                "Error: shooting an off-board position should violate the precondition.");
    }

    // ----- sink: one cell, multiple cells, and already sunk -----

    @Test
    @DisplayName("sink 1: sinking a barge hits its only cell")
    void sink1() {
        ship.sink();
        assertAll(
                () -> assertTrue(ship.getPositions().get(0).isHit(),
                        "Error: the barge's only position should be hit."),
                () -> assertFalse(ship.stillFloating(),
                        "Error: a sunk barge should not float.")
        );
    }

    @Test
    @DisplayName("sink 2: sinking a multi-cell ship hits every cell")
    void sink2() {
        Ship multi = withPositions(p(5, 5), p(5, 6), p(5, 7));
        multi.sink();
        assertAll(
                () -> assertTrue(multi.getPositions().stream().allMatch(IPosition::isHit),
                        "Error: all occupied positions should be marked hit after sink()."),
                () -> assertFalse(multi.stillFloating(),
                        "Error: the multi-cell ship should not be floating after sink().")
        );
    }

    @Test
    @DisplayName("sink: calling sink a second time remains sunk")
    void sinkAlreadySunk() {
        ship.sink();
        ship.sink();
        assertFalse(ship.stillFloating(),
                "Error: a ship should remain sunk after a second call.");
    }

    // ----- toString -----

    @Test
    @DisplayName("toString: describes category, compass and anchor")
    void toString1() {
        String expected = "[" + ship.getCategory()
                + " " + ship.getBearing()
                + " " + ship.getPosition() + "]";

        String actual = ship.toString();

        assertEquals(expected, actual,
                "Error: Expected '" + expected
                        + "' but got '" + actual + "'.");
    }

    // ----- Additional Java-assert preconditions (only if -ea) -----

    @Test
    @DisplayName("occupies: null target triggers Java assertion when enabled")
    void occupiesNull() {
        Assumptions.assumeTrue(Ship.class.desiredAssertionStatus(),
                "Run Maven/IntelliJ tests with -ea to exercise Java assert statements.");
        assertThrows(AssertionError.class, () -> ship.occupies(null),
                "Error: occupies(null) should violate the non-null precondition.");
    }

    @Test
    @DisplayName("tooCloseTo(IPosition): null target triggers Java assertion when enabled")
    void tooCloseToPositionNull() {
        Assumptions.assumeTrue(Ship.class.desiredAssertionStatus(),
                "Run Maven/IntelliJ tests with -ea to exercise Java assert statements.");
        assertThrows(AssertionError.class, () -> ship.tooCloseTo((IPosition) null),
                "Error: null position should violate the adjacency precondition.");
    }

    @Test
    @DisplayName("tooCloseTo(IShip): null target triggers Java assertion when enabled")
    void tooCloseToShipNull() {
        Assumptions.assumeTrue(Ship.class.desiredAssertionStatus(),
                "Run Maven/IntelliJ tests with -ea to exercise Java assert statements.");
        assertThrows(AssertionError.class, () -> ship.tooCloseTo((IShip) null),
                "Error: null ship should violate the adjacency precondition.");
    }
}
