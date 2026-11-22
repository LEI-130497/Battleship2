package battleship;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Game (JUnit 6, without Mockito).
 * Original author: britoeabreu
 * Original date: 2024-03-19 15:30
 * Updated: 2026-11-22 17:14 (Europe/Lisbon)
 *
 * CC supplied in the original exercise (some names/counts differ from the
 * actual Game.java supplied):
 * - Game(): 1
 * - fireSingleShot(): 7 (named "fire" in the original exercise)
 * - getRepeatedShots(): 1; getInvalidShots(): 1
 * - getHits(): 1; getSunkShips(): 1; getRemainingShips(): 1
 * - repeatedShot(): 2 (original exercise)
 * - Simple getters: 1 each
 *
 * Additional methods present in the supplied Game.java and tested here:
 * fireShots, jsonShots, readEnemyFire, randomEnemyFire, printBoard,
 * printMyBoard, printAlienBoard, over.
 *
 * NOTE: CC varies with how loops, short-circuit conditions and exceptions
 * are counted. This test class is not a proof of 100% branch coverage.
 * JavaFX UI tests use the real toolkit and inspect the generated board; they
 * are skipped when a graphical JavaFX runtime is unavailable.
 * Tests invoking audio use the real SoundManager, not a mock.
 * Shot results are the nested record IGame.ShotResult.
 */
public class GameTest {

    private Game game;
    private IFleet fleet;

    /** Captures the generated map without creating a Stage or showing a window. */
    private static final class CapturingGraphicInterface extends GraphicInterface {
        private final CountDownLatch boardReady = new CountDownLatch(1);
        private volatile char[][] capturedBoard;

        @Override
        public void showBoard(char[][] map) {
            capturedBoard = map;
            boardReady.countDown();
        }

        char[][] awaitBoard() throws InterruptedException {
            assertTrue(boardReady.await(10, TimeUnit.SECONDS),
                "Error: expected printUIBoard to invoke showBoard on the JavaFX thread");
            assertNotNull(capturedBoard,
                "Error: expected showBoard to receive a non-null character matrix");
            return capturedBoard;
        }
    }

    // UI tests use the shared JavaFX JUnit extension on each test method.
    // The JavaFX toolkit must not be shut down by this class's @AfterAll.

    @BeforeEach
    void setUp() {
        fleet = new Fleet();
        game = new Game(fleet);
    }

    @AfterEach
    void tearDown() {
        game = null;
        fleet = null;
    }

    // ------ Helpers ------

    private static List<IPosition> threeShots() {
        return List.of(new Position(2, 3), new Position(2, 4), new Position(2, 5));
    }

    /** Creates a real Move. Its results are placeholders: only getShots() is used. */
    private static Move recordedMove(List<IPosition> shots) {
        List<IGame.ShotResult> results = new ArrayList<>();
        for (IPosition shot : shots) {
            results.add(new IGame.ShotResult(shot.isInside(), false, null, false));
        }
        return new Move(1, shots, results);
    }

    /** Captures console output; JUnit test execution should not run in parallel. */
    private static String captureOutput(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            action.run();
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    /** Creates recorded moves so that exactly `remaining` squares are available. */
    private void recordAllButLastPositions(int remaining) {
        int boardCells = Game.BOARD_SIZE * Game.BOARD_SIZE;
        assertTrue(remaining >= 0 && remaining <= boardCells,
            "Error: the number of available squares must be within the board");

        List<IPosition> batch = new ArrayList<>(Game.NUMBER_SHOTS);
        int registered = 0;
        for (int row = 0; row < Game.BOARD_SIZE && registered < boardCells - remaining; row++) {
            for (int col = 0; col < Game.BOARD_SIZE && registered < boardCells - remaining; col++) {
                batch.add(new Position(row, col));
                registered++;
                if (batch.size() == Game.NUMBER_SHOTS) {
                    game.getAlienMoves().add(recordedMove(List.copyOf(batch)));
                    batch.clear();
                }
            }
        }
        if (!batch.isEmpty()) {
            // Constructing a Move directly is intentional for 98/100 recorded cells.
            game.getAlienMoves().add(recordedMove(List.copyOf(batch)));
        }
        assertEquals(boardCells - remaining,
            game.getAlienMoves().stream().mapToInt(move -> move.getShots().size()).sum(),
            "Error: expected all occupied squares to be present in recorded moves");
    }

    /** Extracts a cell from the formatted console board (not the optional legend). */
    private static char printedCellAt(String output, int row, int col) {
        String prefix = " " + new Position(row, 0).getClassicRow() + " |";
        String boardLine = output.lines()
            .filter(line -> line.startsWith(prefix))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Error: board row not found: " + prefix));
        return boardLine.charAt(prefix.length() + 1 + col * 2);
    }

    // ------ Construction and getters ------

    @Test
    @DisplayName("Constructor: initializes fleets, move lists and counters")
    void constructor() {
        assertAll("Initial state",
            () -> assertNotNull(game, "Error: expected a constructed Game instance"),
            () -> assertSame(fleet, game.getMyFleet(), "Error: expected constructor to preserve the supplied fleet"),
            () -> assertNotNull(game.getAlienFleet(), "Error: expected an initialized enemy fleet"),
            () -> assertNotNull(game.getAlienMoves(), "Error: expected a non-null enemy-moves list"),
            () -> assertTrue(game.getAlienMoves().isEmpty(), "Error: expected no enemy moves initially"),
            () -> assertTrue(game.getMyMoves().isEmpty(), "Error: expected no own moves initially"),
            () -> assertEquals(0, game.getInvalidShots(), "Error: expected zero invalid shots initially"),
            () -> assertEquals(0, game.getRepeatedShots(), "Error: expected zero repeated shots initially"),
            () -> assertEquals(0, game.getHits(), "Error: expected zero hits initially"),
            () -> assertEquals(0, game.getSunkShips(), "Error: expected zero sunk ships initially")
        );
    }

    @Test
    @DisplayName("getMyFleet: returns the fleet passed to the constructor")
    void getMyFleet() {
        assertSame(fleet, game.getMyFleet(), "Error: expected the same fleet reference as supplied");
    }

    @Test
    @DisplayName("getAlienFleet: returns an initially empty enemy fleet")
    void getAlienFleet() {
        assertAll("Enemy fleet",
            () -> assertNotNull(game.getAlienFleet(), "Error: expected a non-null enemy fleet"),
            () -> assertTrue(game.getAlienFleet().getShips().isEmpty(), "Error: expected no enemy ships initially")
        );
    }

    @Test
    @DisplayName("getMyMoves: returns an initially empty list")
    void getMyMoves() {
        assertAll("Own moves",
            () -> assertNotNull(game.getMyMoves(), "Error: expected a non-null own-moves list"),
            () -> assertEquals(0, game.getMyMoves().size(), "Error: expected zero own moves")
        );
    }

    @Test
    @DisplayName("getAlienMoves: stores one move containing three shots")
    void getAlienMoves() {
        game.fireShots(threeShots());
        assertAll("Enemy moves",
            () -> assertEquals(1, game.getAlienMoves().size(), "Error: expected one move, not three moves"),
            () -> assertEquals(3, game.getAlienMoves().get(0).getShots().size(),
                "Error: expected three shots in the first move")
        );
    }

    // ------ fireSingleShot: original assignment's fire1..fire7 ------

    @Test
    @DisplayName("fire1: a valid shot into water is a miss")
    void fire1() {
        IGame.ShotResult result = game.fireSingleShot(new Position(2, 3), false);
        assertAll("Valid miss",
            () -> assertNotNull(result, "Error: expected a ShotResult for a valid miss"),
            () -> assertTrue(result.valid(), "Error: expected a valid shot inside the board"),
            () -> assertFalse(result.repeated(), "Error: expected a new shot, not a repetition"),
            () -> assertNull(result.ship(), "Error: expected no ship at a water square"),
            () -> assertFalse(result.sunk(), "Error: a water shot cannot sink a ship"),
            () -> assertEquals(0, game.getInvalidShots(), "Error: expected zero invalid shots"),
            () -> assertEquals(0, game.getRepeatedShots(), "Error: expected zero repeated shots"),
            () -> assertEquals(0, game.getHits(), "Error: expected zero hits on an empty board")
        );
    }

    @Test
    @DisplayName("fire2: an out-of-bounds shot increments invalid-shot counter")
    void fire2() {
        IGame.ShotResult result = game.fireSingleShot(new Position(-1, 5), false);
        assertAll("Invalid shot",
            () -> assertNotNull(result, "Error: expected an invalid-shot result"),
            () -> assertFalse(result.valid(), "Error: expected outside-board shot to be invalid"),
            () -> assertFalse(result.repeated(), "Error: invalid shot must not count as a repetition"),
            () -> assertNull(result.ship(), "Error: invalid shot should have no ship"),
            () -> assertFalse(result.sunk(), "Error: invalid shot cannot sink a ship"),
            () -> assertEquals(1, game.getInvalidShots(), "Error: expected one invalid shot"),
            () -> assertEquals(0, game.getRepeatedShots(), "Error: invalid shots must not count as repeated")
        );
    }

    @Test
    @DisplayName("fire3: the explicit repeated flag increments repeated-shot counter")
    void fire3() {
        IGame.ShotResult result = game.fireSingleShot(new Position(2, 3), true);
        assertAll("Explicit repetition",
            () -> assertTrue(result.valid(), "Error: expected the repeated position to be inside the board"),
            () -> assertTrue(result.repeated(), "Error: expected the explicit repeat flag to be honored"),
            () -> assertNull(result.ship(), "Error: repeat result must not report a newly hit ship"),
            () -> assertFalse(result.sunk(), "Error: repeat must not sink a ship"),
            () -> assertEquals(1, game.getRepeatedShots(), "Error: expected one repeated shot"),
            () -> assertEquals(0, game.getInvalidShots(), "Error: expected zero invalid shots"),
            () -> assertEquals(0, game.getHits(), "Error: a repeated shot must not create a new hit")
        );
    }

    @Test
    @DisplayName("fire4: a shot in a previous move is repeated even with flag false")
    void fire4() {
        game.fireShots(threeShots());
        IGame.ShotResult result = game.fireSingleShot(new Position(2, 3), false);
        assertAll("Repeated from prior move",
            () -> assertTrue(result.valid(), "Error: expected the coordinate to be in the board"),
            () -> assertTrue(result.repeated(), "Error: expected the prior move to mark the shot repeated"),
            () -> assertEquals(1, game.getRepeatedShots(),
                "Error: expected a previously recorded shot to be counted as repeated")
        );
    }

    @Test
    @DisplayName("fire5: a hit on a floating ship increments hits but not sinks")
    void fire5() {
        Ship ship = new Frigate(Compass.EAST, new Position(5, 5));
        fleet.addShip(ship);
        assertTrue(ship.getPositions().size() > 1,
            "Error: expected the Frigate to occupy more than one position");
        IGame.ShotResult result = game.fireSingleShot(ship.getPositions().iterator().next(), false);
        assertAll("Hit, still floating",
            () -> assertTrue(result.valid(), "Error: expected a valid hit"),
            () -> assertFalse(result.repeated(), "Error: expected a first hit at this position"),
            () -> assertSame(ship, result.ship(), "Error: expected the hit ship in the result"),
            () -> assertFalse(result.sunk(), "Error: expected the Frigate still floating"),
            () -> assertEquals(1, game.getHits(), "Error: expected one successful hit"),
            () -> assertEquals(0, game.getSunkShips(), "Error: expected no sunk ships after one hit"),
            () -> assertTrue(ship.stillFloating(), "Error: expected Frigate to remain afloat")
        );
    }

    @Test
    @DisplayName("fire6: hitting the final unhit part of a ship sinks it")
    void fire6() {
        Ship ship = new Barge(Compass.NORTH, new Position(1, 1));
        fleet.addShip(ship);
        List<IPosition> positions = List.copyOf(ship.getPositions());
        List<IGame.ShotResult> results = new ArrayList<>();
        for (IPosition position : positions) {
            results.add(game.fireSingleShot(position, false));
        }
        IGame.ShotResult lastResult = results.get(results.size() - 1);
        assertAll("Ship sunk",
            () -> assertTrue(lastResult.valid(), "Error: expected a valid final hit"),
            () -> assertSame(ship, lastResult.ship(), "Error: expected the sunk ship in the result"),
            () -> assertTrue(lastResult.sunk(), "Error: expected final hit to report the ship sunk"),
            () -> assertEquals(positions.size(), game.getHits(),
                "Error: expected exactly one hit per ship position"),
            () -> assertEquals(1, game.getSunkShips(), "Error: expected exactly one sunk ship"),
            () -> assertFalse(ship.stillFloating(), "Error: expected the ship to have sunk"),
            () -> assertEquals(0, game.getRemainingShips(), "Error: expected no floating ships")
        );
    }

    @Test
    @DisplayName("fire7: an invalid position takes precedence over the repeated flag")
    void fire7() {
        IGame.ShotResult result = game.fireSingleShot(new Position(Game.BOARD_SIZE, Game.BOARD_SIZE), true);
        assertAll("Invalid repeated shot",
            () -> assertFalse(result.valid(), "Error: expected out-of-bounds to take priority over repetition"),
            () -> assertFalse(result.repeated(), "Error: invalid position must not be treated as repeat"),
            () -> assertEquals(1, game.getInvalidShots(), "Error: expected the invalid counter to increment"),
            () -> assertEquals(0, game.getRepeatedShots(), "Error: invalid shot must not reach repetition check")
        );
    }

    // ------ Public counters ------

    @Test
    @DisplayName("getRepeatedShots: reflects repeated shots")
    void getRepeatedShots() {
        game.fireSingleShot(new Position(0, 0), true);
        assertEquals(1, game.getRepeatedShots(), "Error: expected one repeated shot");
    }

    @Test
    @DisplayName("getInvalidShots: reflects invalid shots")
    void getInvalidShots() {
        game.fireSingleShot(new Position(-1, 0), false);
        assertEquals(1, game.getInvalidShots(), "Error: expected one invalid shot");
    }

    @Test
    @DisplayName("getHits: reports zero on an empty fleet")
    void getHits() {
        assertEquals(0, game.getHits(), "Error: expected zero hits before any shot");
    }

    @Test
    @DisplayName("getSunkShips: reports zero initially")
    void getSunkShips() {
        assertEquals(0, game.getSunkShips(), "Error: expected zero sunk ships before play");
    }

    @Test
    @DisplayName("getRemainingShips: reflects additions and a sinking")
    void getRemainingShips() {
        Ship ship1 = new Barge(Compass.NORTH, new Position(1, 1));
        Ship ship2 = new Frigate(Compass.EAST, new Position(5, 5));
        assertEquals(0, game.getRemainingShips(), "Error: expected an empty initial fleet");
        fleet.addShip(ship1);
        assertEquals(1, game.getRemainingShips(), "Error: expected one floating ship after first addition");
        fleet.addShip(ship2);
        assertEquals(2, game.getRemainingShips(), "Error: expected two floating ships after second addition");
        ship2.sink();
        assertEquals(1, game.getRemainingShips(), "Error: expected one ship left after sinking second ship");
    }

    // ------ repeatedShot ------

    @Test
    @DisplayName("repeatedShot1: identifies a position from an existing move")
    void repeatedShot1() {
        game.fireShots(threeShots());
        assertTrue(game.repeatedShot(new Position(2, 3)),
            "Error: expected (2,3) to be recognized from the recorded move");
    }

    @Test
    @DisplayName("repeatedShot2: a position is not repeated before a move")
    void repeatedShot2() {
        assertFalse(game.repeatedShot(new Position(2, 3)),
            "Error: expected (2,3) not to be repeated before any move");
    }

    @Test
    @DisplayName("repeatedShot3: a different coordinate remains new after a move")
    void repeatedShot3() {
        game.fireShots(threeShots());
        assertFalse(game.repeatedShot(new Position(9, 9)),
            "Error: expected an unrecorded coordinate to remain non-repeated");
    }

    // ------ fireShots ------

    @Test
    @DisplayName("fireShots1: accepts exactly three valid positions")
    void fireShots1() {
        game.fireShots(threeShots());
        IMove firstMove = game.getAlienMoves().get(0);
        assertAll("Valid three-shot move",
            () -> assertEquals(1, firstMove.getNumber(), "Error: expected the first move to have number 1"),
            () -> assertEquals(3, firstMove.getShotResults().size(), "Error: expected three shot results"),
            () -> assertTrue(firstMove.getShotResults().stream().allMatch(IGame.ShotResult::valid),
                "Error: expected all three positions to be valid"),
            () -> assertEquals(1, game.getAlienMoves().size(), "Error: expected one recorded move"),
            () -> assertEquals(3, game.getAlienMoves().get(0).getShots().size(),
                "Error: expected three positions in the move"),
            () -> assertEquals(0, game.getInvalidShots(), "Error: expected no invalid shots")
        );
    }

    @Test
    @DisplayName("fireShots2: rejects fewer than three positions")
    void fireShots2() {
        assertThrows(IllegalArgumentException.class,
            () -> game.fireShots(List.of(new Position(0, 0), new Position(0, 1))),
            "Error: expected IllegalArgumentException for fewer than three shots");
        assertEquals(0, game.getAlienMoves().size(), "Error: an invalid-size move must not be recorded");
    }

    @Test
    @DisplayName("fireShots3: rejects more than three positions")
    void fireShots3() {
        assertThrows(IllegalArgumentException.class,
            () -> game.fireShots(List.of(new Position(0, 0), new Position(0, 1),
                new Position(0, 2), new Position(0, 3))),
            "Error: expected IllegalArgumentException for more than three shots");
    }

    @Test
    @DisplayName("fireShots4: counts duplicate positions inside one move")
    void fireShots4() {
        game.fireShots(List.of(new Position(2, 3), new Position(2, 3), new Position(2, 4)));
        assertAll("Duplicate in same move",
            () -> assertEquals(1, game.getRepeatedShots(), "Error: expected one repeated shot in the same move"),
            () -> assertEquals(1, game.getAlienMoves().size(), "Error: expected duplicates not to abort move recording")
        );
    }

    @Test
    @DisplayName("fireShots5: records an invalid position as an invalid shot")
    void fireShots5() {
        game.fireShots(List.of(new Position(-1, 0), new Position(0, 1), new Position(0, 2)));
        assertAll("Invalid position within move",
            () -> assertEquals(1, game.getInvalidShots(), "Error: expected exactly one invalid shot"),
            () -> assertEquals(1, game.getAlienMoves().size(), "Error: expected the move to be recorded")
        );
    }

    @Test
    @DisplayName("fireShots6: detects a shot repeated in a later move")
    void fireShots6() {
        game.fireShots(threeShots());
        game.fireShots(List.of(new Position(2, 3), new Position(0, 0), new Position(0, 1)));
        IMove secondMove = game.getAlienMoves().get(1);
        assertAll("Repeated position across moves",
            () -> assertEquals(2, game.getAlienMoves().size(), "Error: expected two recorded moves"),
            () -> assertEquals(2, secondMove.getNumber(), "Error: expected the second move to have number 2"),
            () -> assertTrue(secondMove.getShotResults().get(0).repeated(),
                "Error: expected the first shot of the second move to be flagged repeated"),
            () -> assertEquals(1, game.getRepeatedShots(), "Error: expected one repeated shot across moves")
        );
    }

    // ------ JSON serialization ------

    @Test
    @DisplayName("jsonShots1: empty input produces an empty JSON array")
    void jsonShots1() {
        assertEquals("[ ]", Game.jsonShots(List.of()), "Error: expected Jackson pretty-printed empty JSON array [ ]");
    }

    @Test
    @DisplayName("jsonShots2: converts internal coordinates to classic positions")
    void jsonShots2() throws Exception {
        JsonNode json = new ObjectMapper().readTree(
            Game.jsonShots(List.of(new Position(0, 0), new Position(1, 2))));
        assertAll("Serialized coordinates",
            () -> assertEquals(2, json.size(), "Error: expected two JSON positions"),
            () -> assertEquals("A", json.get(0).get("row").asText(), "Error: expected row A for index 0"),
            () -> assertEquals(1, json.get(0).get("column").asInt(), "Error: expected column 1 for index 0"),
            () -> assertEquals("B", json.get(1).get("row").asText(), "Error: expected row B for index 1"),
            () -> assertEquals(3, json.get(1).get("column").asInt(), "Error: expected column 3 for index 2")
        );
    }

    // ------ readEnemyFire ------

    @Test
    @DisplayName("readEnemyFire1: accepts three separate letter-number pairs")
    void readEnemyFire1() throws Exception {
        String json = game.readEnemyFire(new Scanner("A 1 B 2 C 3\n"));
        JsonNode shots = new ObjectMapper().readTree(json);
        assertAll("Three valid input positions",
            () -> assertEquals(3, shots.size(), "Error: expected three serialized positions"),
            () -> assertEquals("A", shots.get(0).get("row").asText(), "Error: expected first row A"),
            () -> assertEquals(1, game.getAlienMoves().size(), "Error: expected one recorded move")
        );
    }

    @Test
    @DisplayName("readEnemyFire2: normalizes lowercase letters")
    void readEnemyFire2() throws Exception {
        JsonNode shots = new ObjectMapper().readTree(game.readEnemyFire(new Scanner("a 1 b 2 c 3\n")));
        assertAll("Case insensitive letters",
            () -> assertEquals("A", shots.get(0).get("row").asText(), "Error: expected uppercase A"),
            () -> assertEquals("B", shots.get(1).get("row").asText(), "Error: expected uppercase B"),
            () -> assertEquals("C", shots.get(2).get("row").asText(), "Error: expected uppercase C")
        );
    }

    @Test
    @DisplayName("readEnemyFire3: rejects a letter not followed by a number")
    void readEnemyFire3() {
        assertThrows(IllegalArgumentException.class,
            () -> game.readEnemyFire(new Scanner("A B 2 C 3\n")),
            "Error: expected an exception for an incomplete coordinate");
    }

    @Test
    @DisplayName("readEnemyFire4: rejects fewer than three coordinates")
    void readEnemyFire4() {
        assertThrows(IllegalArgumentException.class,
            () -> game.readEnemyFire(new Scanner("A 1 B 2\n")),
            "Error: expected an exception when fewer than three positions are provided");
    }

    @Test
    @DisplayName("readEnemyFire5: stops reading after the first three positions")
    void readEnemyFire5() throws Exception {
        JsonNode shots = new ObjectMapper().readTree(
            game.readEnemyFire(new Scanner("A 1 B 2 C 3 D 4\n")));
        assertAll("Additional input",
            () -> assertEquals(3, shots.size(), "Error: expected only the first three coordinates to be used"),
            () -> assertEquals(1, game.getAlienMoves().size(), "Error: expected one recorded move")
        );
    }

    @Test
    @DisplayName("readEnemyFire6: accepts compact coordinates such as A1")
    void readEnemyFire6() throws Exception {
        JsonNode shots = new ObjectMapper().readTree(
            game.readEnemyFire(new Scanner("A1 B2 C3\n")));
        assertEquals(3, shots.size(), "Error: expected three parsed compact coordinates");
    }

    // ------ randomEnemyFire ------

    @Test
    @DisplayName("randomEnemyFire1: chooses three unique positions from a free board")
    void randomEnemyFire1() throws Exception {
        JsonNode json = new ObjectMapper().readTree(game.randomEnemyFire());
        List<IPosition> fired = game.getAlienMoves().get(0).getShots();
        assertAll("Random move on free board",
            () -> assertEquals(3, json.size(), "Error: expected three positions in JSON"),
            () -> assertEquals(1, game.getAlienMoves().size(), "Error: expected one recorded random move"),
            () -> assertEquals(3, fired.size(), "Error: expected three random shots"),
            () -> assertEquals(3, new HashSet<>(fired).size(),
                "Error: expected three distinct random positions on a free board")
        );
    }

    @Test
    @DisplayName("randomEnemyFire2: repeats the last remaining position when fewer than three remain")
    void randomEnemyFire2() throws Exception {
        recordAllButLastPositions(1);
        int movesBefore = game.getAlienMoves().size();
        JsonNode json = new ObjectMapper().readTree(game.randomEnemyFire());
        assertAll("Only one position remains",
            () -> assertEquals(3, json.size(), "Error: expected JSON to contain three shots"),
            () -> assertEquals(movesBefore + 1, game.getAlienMoves().size(),
                "Error: expected one extra move after random fire"),
            () -> assertEquals(2, game.getRepeatedShots(),
                "Error: expected two repeats of the last available position")
        );
    }

    @Test
    @DisplayName("randomEnemyFire3: two free squares produce two distinct shots and one repetition")
    void randomEnemyFire3() throws Exception {
        recordAllButLastPositions(2);
        int previousMoves = game.getAlienMoves().size();
        JsonNode json = new ObjectMapper().readTree(game.randomEnemyFire());
        List<IPosition> shots = game.getAlienMoves().get(previousMoves).getShots();
        assertAll("Two free squares",
            () -> assertEquals(3, json.size(), "Error: expected exactly three serialized shots"),
            () -> assertEquals(3, shots.size(), "Error: expected three submitted shots"),
            () -> assertEquals(2, new HashSet<>(shots).size(),
                "Error: expected two distinct available squares"),
            () -> assertEquals(1, game.getRepeatedShots(),
                "Error: expected exactly one repeated shot when only two squares remain"),
            () -> assertEquals(previousMoves + 1, game.getAlienMoves().size(),
                "Error: expected the generated move to be recorded")
        );
    }

    @Test
    @DisplayName("randomEnemyFire4: avoids adjacent squares around sunken ships")
    void randomEnemyFire4() throws Exception {
        Ship sunk = new Barge(Compass.NORTH, new Position(4, 4));
        fleet.addShip(sunk);
        sunk.sink();
        game.randomEnemyFire();
        List<IPosition> fired = game.getAlienMoves().get(0).getShots();
        assertAll("Exclude sunk-ship adjacent squares",
            () -> assertEquals(3, fired.size(), "Error: expected three enemy shots"),
            () -> assertTrue(fired.stream().noneMatch(sunk.getAdjacentPositions()::contains),
                "Error: expected no generated shot in a square excluded by a sunk ship")
        );
    }

    @Test
    @DisplayName("randomEnemyFire5: documents failure when no squares remain")
    void randomEnemyFire5() {
        recordAllButLastPositions(0);
        Throwable exception = assertThrows(Throwable.class, game::randomEnemyFire,
            "Error: expected the current implementation to reject null shots when no squares remain");
        assertTrue(exception instanceof AssertionError || exception instanceof NullPointerException,
            "Error: expected AssertionError with -ea or NullPointerException without -ea, but got " + exception);
    }

    @Test
    @DisplayName("jsonShots3: a null entry cannot be converted to classic coordinates")
    void jsonShots3() {
        assertThrows(NullPointerException.class,
            () -> Game.jsonShots(Collections.singletonList(null)),
            "Error: expected an exception when an input shot position is null");
    }

    @Test
    @DisplayName("readEnemyFire7: empty input cannot form three coordinates")
    void readEnemyFire7() {
        assertThrows(IllegalArgumentException.class,
            () -> game.readEnemyFire(new Scanner("\n")),
            "Error: expected an exception for zero input positions");
        assertEquals(0, game.getAlienMoves().size(),
            "Error: expected no move to be recorded for empty input");
    }

    @Test
    @DisplayName("fireShots7: null list fails the input contract")
    void fireShots7() {
        Throwable exception = assertThrows(Throwable.class, () -> game.fireShots(null),
            "Error: expected a null shots list to fail");
        assertTrue(exception instanceof AssertionError || exception instanceof NullPointerException,
            "Error: expected AssertionError (-ea) or NullPointerException, but got " + exception);
    }

    @Test
    @DisplayName("fire8: null position fails the input contract")
    void fire8() {
        Throwable exception = assertThrows(Throwable.class,
            () -> game.fireSingleShot(null, false),
            "Error: expected a null shot position to fail");
        assertTrue(exception instanceof AssertionError || exception instanceof NullPointerException,
            "Error: expected AssertionError (-ea) or NullPointerException, but got " + exception);
    }

    @Test
    @DisplayName("repeatedShot4: null position fails the method contract when assertions are enabled")
    void repeatedShot4() {
        // With -ea: the Java assert fires; without -ea an empty move list returns false.
        boolean assertionsEnabled = false;
        assert assertionsEnabled = true;
        if (assertionsEnabled) {
            assertThrows(AssertionError.class, () -> game.repeatedShot(null),
                "Error: expected AssertionError when null violates the asserted precondition");
        } else {
            assertFalse(game.repeatedShot(null),
                "Error: expected no match for null when JVM assertions are disabled");
        }
    }

    @Test
    @DisplayName("jsonShots4: null shot list fails the input contract")
    void jsonShots4() {
        Throwable error = assertThrows(Throwable.class, () -> Game.jsonShots(null),
            "Error: expected null input to be rejected");
        assertTrue(error instanceof AssertionError || error instanceof NullPointerException,
            "Error: expected AssertionError (-ea) or NullPointerException, but got " + error);
    }

    @Test
    @DisplayName("readEnemyFire8: null Scanner fails the input contract")
    void readEnemyFire8() {
        Throwable error = assertThrows(Throwable.class, () -> game.readEnemyFire(null),
            "Error: expected a null Scanner to be rejected");
        assertTrue(error instanceof AssertionError || error instanceof NullPointerException,
            "Error: expected AssertionError (-ea) or NullPointerException, but got " + error);
    }

    // ------ Printing and CLI output ------

    @Test
    @DisplayName("printBoard1: empty board without legend or shown shots")
    void printBoard1() {
        String output = captureOutput(() -> Game.printBoard(fleet, List.of(), false, false));
        assertAll("Empty board",
            () -> assertTrue(output.contains(String.valueOf(Game.EMPTY_MARKER)),
                "Error: expected empty-water marker on an empty board"),
            () -> assertFalse(output.contains("LEGENDA"), "Error: expected no legend when disabled")
        );
    }

    @Test
    @DisplayName("printBoard2: shows the legend when requested")
    void printBoard2() {
        String output = captureOutput(() -> Game.printBoard(fleet, List.of(), false, true));
        assertTrue(output.contains("LEGENDA"), "Error: expected the legend to be displayed");
    }

    @Test
    @DisplayName("printBoard3: renders hits, misses, and skips outside-board shots")
    void printBoard3() {
        Ship ship = new Barge(Compass.NORTH, new Position(1, 1));
        fleet.addShip(ship);
        IPosition shipPosition = ship.getPositions().iterator().next();
        List<IPosition> shots = List.of(shipPosition, new Position(9, 9), new Position(-1, 5));
        String output = captureOutput(() -> Game.printBoard(
            fleet, List.of(recordedMove(shots)), true, false));
        assertAll("Rendered shots",
            () -> assertTrue(output.contains(" " + Game.SHOT_SHIP_MARKER),
                "Error: expected a hit marker on the board"),
            () -> assertTrue(output.contains(" " + Game.SHOT_WATER_MARKER),
                "Error: expected a water-shot marker on the board"),
            () -> assertFalse(output.contains("LEGENDA"), "Error: expected no legend")
        );
    }

    @Test
    @DisplayName("printBoard4: hides recorded shots when show_shots is false")
    void printBoard4() {
        Ship ship = new Barge(Compass.NORTH, new Position(1, 1));
        fleet.addShip(ship);
        IPosition shipPosition = ship.getPositions().iterator().next();
        String output = captureOutput(() -> Game.printBoard(
            fleet, List.of(recordedMove(List.of(shipPosition, new Position(9, 9)))), false, false));
        assertAll("Hidden shots",
            () -> assertTrue(output.contains(" " + Game.SHIP_MARKER), "Error: expected ship marker to stay visible"),
            () -> assertFalse(output.contains(" " + Game.SHOT_SHIP_MARKER),
                "Error: expected no hit markers when shots are hidden"),
            () -> assertFalse(output.contains(" " + Game.SHOT_WATER_MARKER),
                "Error: expected no miss markers when shots are hidden")
        );
    }

    @Test
    @DisplayName("printBoard5: sunk ship adjacent squares are rendered")
    void printBoard5() {
        Ship ship = new Barge(Compass.NORTH, new Position(4, 4));
        fleet.addShip(ship);
        ship.sink();
        String output = captureOutput(() -> Game.printBoard(fleet, List.of(), false, false));
        assertTrue(output.contains(" " + Game.SHIP_ADJACENT_MARKER),
            "Error: expected adjacent markers around a sunk ship");
    }

    @Test
    @DisplayName("printBoard6: can display shots and the legend together")
    void printBoard6() {
        String output = captureOutput(() -> Game.printBoard(
            fleet, List.of(recordedMove(List.of(new Position(9, 9)))), true, true));
        assertAll("Shot and legend together",
            () -> assertTrue(output.contains("LEGENDA"),
                "Error: expected a legend when showLegend is true"),
            () -> assertTrue(output.contains(" " + Game.SHOT_WATER_MARKER),
                "Error: expected the visible water shot when show_shots is true")
        );
    }

    @Test
    @DisplayName("printBoard7: shooting a marked adjacent square produces a water marker")
    void printBoard7() {
        Ship ship = new Barge(Compass.NORTH, new Position(4, 4));
        fleet.addShip(ship);
        ship.sink();
        IPosition adjacent = ship.getAdjacentPositions().iterator().next();
        assertTrue(adjacent.isInside(), "Error: expected a valid adjacent position for the central ship");
        String output = captureOutput(() -> Game.printBoard(
            fleet, List.of(recordedMove(List.of(adjacent))), true, false));
        assertTrue(output.contains(" " + Game.SHOT_WATER_MARKER),
            "Error: expected a water marker after shooting an adjacent square");
    }

    @Test
    @DisplayName("printBoardNullFleet: rejects null fleet")
    void printBoardNullFleet() {
        Throwable error = assertThrows(Throwable.class,
            () -> Game.printBoard(null, List.of(), false, false),
            "Error: expected a null fleet to fail");
        assertTrue(error instanceof AssertionError || error instanceof NullPointerException,
            "Error: expected AssertionError (-ea) or NullPointerException, but got " + error);
    }

    @Test
    @DisplayName("printBoardNullMoves: rejects null moves if shots would be drawn")
    void printBoardNullMoves() {
        Throwable error = assertThrows(Throwable.class,
            () -> Game.printBoard(fleet, null, true, false),
            "Error: expected a null move list to fail");
        assertTrue(error instanceof AssertionError || error instanceof NullPointerException,
            "Error: expected AssertionError (-ea) or NullPointerException, but got " + error);
    }

    @Test
    @DisplayName("printUIBoardNullFleet: rejects null fleet before queueing JavaFX work")
    void printUIBoardNullFleet() {
        Throwable error = assertThrows(Throwable.class,
            () -> Game.printUIBoard(null, List.of(), false, false),
            "Error: expected a null fleet to fail");
        assertTrue(error instanceof AssertionError || error instanceof NullPointerException,
            "Error: expected AssertionError (-ea) or NullPointerException, but got " + error);
    }

    @Test
    @DisplayName("printUIBoardNullMoves: rejects null moves before queueing JavaFX work")
    void printUIBoardNullMoves() {
        Throwable error = assertThrows(Throwable.class,
            () -> Game.printUIBoard(fleet, null, true, false),
            "Error: expected a null move list to fail");
        assertTrue(error instanceof AssertionError || error instanceof NullPointerException,
            "Error: expected AssertionError (-ea) or NullPointerException, but got " + error);
    }

    @Test
    @DisplayName("printBoard8: exact cells distinguish ship, hit, miss and open water")
    void printBoard8() {
        Ship ship = new Barge(Compass.NORTH, new Position(4, 4));
        fleet.addShip(ship);
        IPosition hit = ship.getPositions().iterator().next();
        Position water = new Position(9, 9);
        String printed = captureOutput(() -> Game.printBoard(
            fleet, List.of(recordedMove(List.of(hit, water))), true, false));
        assertAll("Exact board cells",
            () -> assertEquals(Game.SHOT_SHIP_MARKER,
                printedCellAt(printed, hit.getRow(), hit.getColumn()),
                "Error: expected the exact hit location to contain the hit marker"),
            () -> assertEquals(Game.SHOT_WATER_MARKER,
                printedCellAt(printed, water.getRow(), water.getColumn()),
                "Error: expected the exact miss location to contain the water-shot marker"),
            () -> assertEquals(Game.EMPTY_MARKER, printedCellAt(printed, 0, 0),
                "Error: expected an untouched corner to remain water")
        );
    }

    @Test
    @DisplayName("printBoard9: repeated rendering of the same shot preserves its marker")
    void printBoard9() {
        Position water = new Position(2, 2);
        Move first = recordedMove(List.of(water));
        Move second = recordedMove(List.of(water));
        String printed = captureOutput(() -> Game.printBoard(fleet, List.of(first, second), true, false));
        assertEquals(Game.SHOT_WATER_MARKER,
            printedCellAt(printed, water.getRow(), water.getColumn()),
            "Error: expected a repeated water shot to remain marked as a miss");
    }

    @Test
    @DisplayName("printBoard10: a sunken ship's adjacent square turns into a water shot")
    void printBoard10() {
        Ship ship = new Barge(Compass.NORTH, new Position(4, 4));
        fleet.addShip(ship);
        ship.sink();
        IPosition adjacent = ship.getAdjacentPositions().iterator().next();
        String printed = captureOutput(() -> Game.printBoard(
            fleet, List.of(recordedMove(List.of(adjacent))), true, false));
        assertEquals(Game.SHOT_WATER_MARKER,
            printedCellAt(printed, adjacent.getRow(), adjacent.getColumn()),
            "Error: expected the exact adjacent square to be replaced by a miss marker");
    }

    @Test
    @DisplayName("printMyBoard: prints the player's board")
    void printMyBoard() {
        String output = captureOutput(() -> game.printMyBoard(false, false));
        assertTrue(output.contains(String.valueOf(Game.EMPTY_MARKER)),
            "Error: expected empty cells on the player's board");
    }

    @Test
    @DisplayName("printAlienBoard: prints the enemy board")
    void printAlienBoard() {
        String output = captureOutput(() -> game.printAlienBoard(false, false));
        assertTrue(output.contains(String.valueOf(Game.EMPTY_MARKER)),
            "Error: expected empty cells on the enemy board");
    }

    // The JavaFX toolkit must be initialized, even when a Stage is not opened.
    // The subclass captures showBoard's argument on the JavaFX Application Thread.
    @Test
    @ExtendWith(JavaFxTestExtension.class)
    @DisplayName("printUIBoard1: JavaFX receives hit, miss and ship markers")
    void printUIBoard1() throws Exception {
        CapturingGraphicInterface gui = new CapturingGraphicInterface();
        Ship ship = new Barge(Compass.NORTH, new Position(4, 4));
        fleet.addShip(ship);
        IPosition hit = ship.getPositions().iterator().next();
        Position miss = new Position(9, 9);

        Game.printUIBoard(fleet, List.of(recordedMove(List.of(hit, miss))), true, false);
        char[][] board = gui.awaitBoard();
        assertAll("Generated GUI board",
            () -> assertSame(gui, GraphicInterface.getInstance(),
                "Error: expected the capturing JavaFX interface to be the active instance"),
            () -> assertEquals(Game.BOARD_SIZE, board.length,
                "Error: expected the correct number of rows"),
            () -> assertEquals(Game.BOARD_SIZE, board[0].length,
                "Error: expected the correct number of columns"),
            () -> assertEquals(Game.SHOT_SHIP_MARKER, board[hit.getRow()][hit.getColumn()],
                "Error: expected a hit marker at the ship position"),
            () -> assertEquals(Game.SHOT_WATER_MARKER, board[miss.getRow()][miss.getColumn()],
                "Error: expected a miss marker at the water position"),
            () -> assertEquals(Game.EMPTY_MARKER, board[0][0],
                "Error: expected untouched water to remain empty")
        );
    }

    @Test
    @ExtendWith(JavaFxTestExtension.class)
    @DisplayName("printUIBoard2: hidden shots preserve the original ship and water markers")
    void printUIBoard2() throws Exception {
        CapturingGraphicInterface gui = new CapturingGraphicInterface();
        Ship ship = new Barge(Compass.NORTH, new Position(4, 4));
        fleet.addShip(ship);
        IPosition shipPosition = ship.getPositions().iterator().next();

        Game.printUIBoard(fleet,
            List.of(recordedMove(List.of(shipPosition, new Position(9, 9)))), false, true);
        char[][] board = gui.awaitBoard();
        assertAll("Hidden GUI shots",
            () -> assertEquals(Game.SHIP_MARKER,
                board[shipPosition.getRow()][shipPosition.getColumn()],
                "Error: expected the ship marker to remain when shots are hidden"),
            () -> assertEquals(Game.EMPTY_MARKER, board[9][9],
                "Error: expected a hidden water shot to leave an empty cell")
        );
    }

    @Test
    @ExtendWith(JavaFxTestExtension.class)
    @DisplayName("printUIBoard3: skips out-of-bounds shots and draws sunken-ship adjacency")
    void printUIBoard3() throws Exception {
        CapturingGraphicInterface gui = new CapturingGraphicInterface();
        Ship ship = new Barge(Compass.NORTH, new Position(4, 4));
        fleet.addShip(ship);
        ship.sink();
        IPosition adjacent = ship.getAdjacentPositions().iterator().next();
        Game.printUIBoard(fleet,
            List.of(recordedMove(List.of(new Position(-1, 2), adjacent))), true, false);
        char[][] board = gui.awaitBoard();
        assertEquals(Game.SHOT_WATER_MARKER,
            board[adjacent.getRow()][adjacent.getColumn()],
            "Error: expected a hit on a sunk ship's adjacent marker to be drawn as water");
    }

    @Test
    @ExtendWith(JavaFxTestExtension.class)
    @DisplayName("printMyUIBoard: forwards the player's board to JavaFX")
    void printMyUIBoard() throws Exception {
        CapturingGraphicInterface gui = new CapturingGraphicInterface();
        game.fireShots(threeShots());

        game.printMyUIBoard(true, true);
        char[][] board = gui.awaitBoard();
        assertAll("Player UI board",
            () -> assertEquals(Game.BOARD_SIZE, board.length,
                "Error: expected a complete player board"),
            () -> assertEquals(Game.SHOT_WATER_MARKER, board[2][3],
                "Error: expected the recorded shot to appear on the player board")
        );
    }

    @Test
    @DisplayName("over: prints the end-of-game message and closes the sound manager")
    void over() {
        String output = captureOutput(() -> game.over());
        assertTrue(output.contains("Java Sparrow"),
            "Error: expected the end-of-game message in console output");
    }
}
