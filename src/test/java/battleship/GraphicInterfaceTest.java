package battleship;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GraphicInterface (JUnit 6, without Mockito).
 * Author: ${user.name} (replace using the IntelliJ IDEA file template variable)
 * Generated: 2026-11-22 16:13 (Europe/Lisbon)
 * Estimated cyclomatic complexity (CC):
 * - GraphicInterface(): 1
 * - mainLaunch(): 1
 * - getInstance(): 1
 * - start(Stage): 1
 * - showScene(): 1
 * - showBoard(char[][]): 8 (two loops, five switch cases, unmatched path)
 *
 * Requires a JavaFX display (or configured headless environment).
 * JavaFxTestExtension launches the application once for both this class and
 * GameTest, then closes it after the JUnit test plan finishes. Run these tests
 * sequentially because they share the singleton and static GridPane.
 */
@ExtendWith(JavaFxTestExtension.class)
class GraphicInterfaceTest {

    private TestableGraphicInterface instance;
    private Stage testStage;
    private static GraphicInterface launchedInstance;

    /** Keeps showBoard's real logic, but prevents it from displaying a window. */
    private static final class TestableGraphicInterface extends GraphicInterface {
        boolean sceneRequested;

        @Override
        public void showScene() {
            sceneRequested = true;
        }

        void showRealScene() {
            super.showScene();
        }
    }

    @BeforeAll
    static void launchJavaFxOnce() {
        // The shared extension starts JavaFX once across all test classes.
        // It also arranges for the JavaFX toolkit to close at the end of the test plan.
        launchedInstance = JavaFxTestExtension.getLaunchedInstance();
        assertNotNull(launchedInstance,
            "Error: expected the shared JavaFX session to have an application instance");
    }

    @BeforeEach
    void setUp() {
        instance = new TestableGraphicInterface();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (testStage != null) {
            onFxThread(() -> testStage.hide());
            testStage = null;
        }
        GraphicInterface.grid = null;

        // Restore the real launched application instead of leaving the singleton
        // null. Later tests (for example TasksTest) may use printUIBoard().
        Field singleton = GraphicInterface.class.getDeclaredField("instance");
        singleton.setAccessible(true);
        singleton.set(null, launchedInstance);
        instance = null;
    }

    @Test
    @DisplayName("Constructor: the new instance becomes the singleton")
    void constructor() {
        assertAll("Constructor",
            () -> assertNotNull(instance,
                "Error: expected a constructed GraphicInterface but got null"),
            () -> assertSame(instance, GraphicInterface.getInstance(),
                "Error: expected the newly constructed object as the singleton, but got a different instance"));
    }

    @Test
    @DisplayName("mainLaunch: creates and starts the JavaFX application")
    void mainLaunch() throws Exception {
        // mainLaunch() has already been invoked by @BeforeAll, exactly once.
        Field stageField = GraphicInterface.class.getDeclaredField("stage");
        stageField.setAccessible(true);
        assertAll("Application launch",
            () -> assertNotNull(launchedInstance,
                "Error: expected a launched GraphicInterface but got null"),
            () -> assertNotNull(stageField.get(launchedInstance),
                "Error: expected start(Stage) to receive a real Stage but got null"));
    }

    @Test
    @DisplayName("getInstance: returns the instance created during setup")
    void getInstance() {
        assertSame(instance, GraphicInterface.getInstance(),
            "Error: expected getInstance() to return the setup instance, but it returned another object");
    }

    @Test
    @DisplayName("start: a close request is consumed and the window is hidden")
    void start() {
        onFxThread(() -> {
            testStage = new Stage();
            instance.start(testStage);
            testStage.setScene(new Scene(new GridPane(), 100, 100));
            testStage.show();

            EventHandler<WindowEvent> closeHandler = testStage.getOnCloseRequest();
            assertNotNull(closeHandler,
                "Error: expected a registered close handler but got null");
            assertTrue(testStage.isShowing(),
                "Error: expected the stage to be visible before closing but it was hidden");

            WindowEvent event = new WindowEvent(testStage, WindowEvent.WINDOW_CLOSE_REQUEST);
            closeHandler.handle(event);
            assertAll("Close handler",
                () -> assertTrue(event.isConsumed(),
                    "Error: expected the close request to be consumed but it was not"),
                () -> assertFalse(testStage.isShowing(),
                    "Error: expected the stage to be hidden but it was still visible"));
        });
    }

    @Test
    @DisplayName("showScene: shows the board with the expected title and usable size")
    void showScene() {
        onFxThread(() -> {
            testStage = new Stage();
            instance.start(testStage);
            GraphicInterface.grid = new GridPane();
            instance.showRealScene();

            Scene scene = testStage.getScene();
            assertNotNull(scene,
                "Error: expected a Scene attached to the stage but got null");
            assertAll("Scene configuration",
                () -> assertSame(GraphicInterface.grid, scene.getRoot(),
                    "Error: expected the board GridPane as root but got a different root"),
                // A platform may shrink a 750 x 750 scene to fit the available
                // desktop area when Stage.show() is called (e.g. macOS CI).
                () -> assertTrue(scene.getWidth() > 0 && scene.getWidth() <= 750.01,
                    "Error: expected scene width in (0, 750] but got " + scene.getWidth()),
                () -> assertTrue(scene.getHeight() > 0 && scene.getHeight() <= 750.01,
                    "Error: expected scene height in (0, 750] but got " + scene.getHeight()),
                () -> assertEquals("Visualização", testStage.getTitle(),
                    "Error: expected title 'Visualização' but got '" + testStage.getTitle() + "'"),
                () -> assertTrue(testStage.isShowing(),
                    "Error: expected the stage to be displayed but it was hidden"));
        });
    }

    @Test
    @DisplayName("showBoard path 1: empty cells become white circles")
    void showBoard1() {
        onFxThread(() -> {
            instance.showBoard(emptyBoard());
            assertCell(0, 0, Color.WHITE);
        });
    }

    @Test
    @DisplayName("showBoard path 2: ship cells become gray circles")
    void showBoard2() {
        onFxThread(() -> {
            char[][] board = emptyBoard();
            board[0][0] = Game.SHIP_MARKER;
            instance.showBoard(board);
            assertCell(0, 0, Color.GRAY);
        });
    }

    @Test
    @DisplayName("showBoard path 3: a ship hit at the last position is red")
    void showBoard3() {
        onFxThread(() -> {
            char[][] board = emptyBoard();
            int last = Game.BOARD_SIZE - 1;
            board[last][last] = Game.SHOT_SHIP_MARKER;
            instance.showBoard(board);
            assertCell(last, last, Color.RED);
        });
    }

    @Test
    @DisplayName("showBoard path 4: missed shots become blue circles")
    void showBoard4() {
        onFxThread(() -> {
            char[][] board = emptyBoard();
            board[0][0] = Game.SHOT_WATER_MARKER;
            instance.showBoard(board);
            assertCell(0, 0, Color.BLUE);
        });
    }

    @Test
    @DisplayName("showBoard path 5: cells adjacent to ships become yellow")
    void showBoard5() {
        onFxThread(() -> {
            char[][] board = emptyBoard();
            board[0][0] = Game.SHIP_ADJACENT_MARKER;
            instance.showBoard(board);
            assertCell(0, 0, Color.YELLOW);
        });
    }

    @Test
    @DisplayName("showBoard path 6: unrecognized markers retain the default fill")
    void showBoard6() {
        onFxThread(() -> {
            char[][] board = emptyBoard();
            board[0][0] = '\uFFFF';
            Paint initialFill = new Circle(30).getFill();
            instance.showBoard(board);
            assertCell(0, 0, initialFill);
        });
    }

    @Test
    @DisplayName("showBoard path 7: missing rows throw ArrayIndexOutOfBoundsException")
    void showBoard7() {
        onFxThread(() -> {
            char[][] shortBoard = new char[Game.BOARD_SIZE - 1][Game.BOARD_SIZE];
            assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> instance.showBoard(shortBoard),
                "Error: expected ArrayIndexOutOfBoundsException for a board with missing rows");
        });
    }

    @Test
    @DisplayName("showBoard path 8: null boards throw NullPointerException")
    void showBoard8() {
        onFxThread(() -> assertThrows(NullPointerException.class,
            () -> instance.showBoard(null),
            "Error: expected NullPointerException for a null board"));
    }

    private static char[][] emptyBoard() {
        char[][] board = new char[Game.BOARD_SIZE][Game.BOARD_SIZE];
        for (char[] row : board) {
            Arrays.fill(row, Game.EMPTY_MARKER);
        }
        return board;
    }

    private void assertCell(int row, int column, Paint expectedFill) {
        GridPane board = GraphicInterface.grid;
        assertNotNull(board,
            "Error: expected a GridPane containing the board but got null");
        int expectedCells = Game.BOARD_SIZE * Game.BOARD_SIZE;
        assertEquals(expectedCells, board.getChildren().size(),
            "Error: expected " + expectedCells + " cells but got " + board.getChildren().size());

        Circle circle = (Circle) board.getChildren().get(row * Game.BOARD_SIZE + column);
        assertAll("Cell (" + row + ", " + column + ")",
            () -> assertEquals(expectedFill, circle.getFill(),
                "Error: expected fill " + expectedFill + " but got " + circle.getFill()),
            () -> assertEquals(Color.BLACK, circle.getStroke(),
                "Error: expected a black stroke but got " + circle.getStroke()),
            () -> assertEquals(30.0, circle.getRadius(),
                "Error: expected radius 30 but got " + circle.getRadius()),
            () -> assertEquals(Integer.valueOf(row), GridPane.getRowIndex(circle),
                "Error: expected row " + row + " but got " + GridPane.getRowIndex(circle)),
            () -> assertEquals(Integer.valueOf(column), GridPane.getColumnIndex(circle),
                "Error: expected column " + column + " but got " + GridPane.getColumnIndex(circle)),
            () -> assertEquals(10.0, board.getHgap(),
                "Error: expected horizontal gap 10 but got " + board.getHgap()),
            () -> assertEquals(10.0, board.getVgap(),
                "Error: expected vertical gap 10 but got " + board.getVgap()),
            () -> assertTrue(instance.sceneRequested,
                "Error: expected showScene() to be requested but it was not"));
    }

    /** Runs JavaFX operations on the actual JavaFX Application Thread. */
    private static void onFxThread(Executable testAction) {
        if (Platform.isFxApplicationThread()) {
            execute(testAction);
            return;
        }
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Throwable> problem = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                testAction.execute();
            } catch (Throwable error) {
                problem.set(error);
            } finally {
                finished.countDown();
            }
        });

        try {
            assertTrue(finished.await(10, TimeUnit.SECONDS),
                "Error: expected the JavaFX action to finish within 10 seconds but it timed out");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Error: expected uninterrupted JavaFX test execution", error);
        }
        if (problem.get() != null) {
            Throwable error = problem.get();
            if (error instanceof AssertionError) throw (AssertionError) error;
            if (error instanceof RuntimeException) throw (RuntimeException) error;
            throw new AssertionError("Error: unexpected failure on the JavaFX thread", error);
        }
    }

    private static void execute(Executable action) {
        try {
            action.execute();
        } catch (Throwable error) {
            if (error instanceof AssertionError) throw (AssertionError) error;
            if (error instanceof RuntimeException) throw (RuntimeException) error;
            throw new AssertionError("Error: unexpected failure on the JavaFX thread", error);
        }
    }
}
