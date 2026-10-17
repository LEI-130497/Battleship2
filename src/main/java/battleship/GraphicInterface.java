package battleship;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.application.Platform;

public class GraphicInterface extends Application {

    public static GridPane grid;
    private Stage stage;
    private static GraphicInterface instance;

    public GraphicInterface() {
        super();
        instance = this;
    }

    public static void mainLaunch()
    {
        Thread thread = new Thread(() -> {
            Platform.setImplicitExit(false);
            Application.launch(GraphicInterface.class);
        });

        thread.start();
    }

    public static GraphicInterface getInstance() {
        return instance;
    }


    @Override
    public void start(Stage stage) {
        this.stage = stage;

        stage.setOnCloseRequest(event -> {
            event.consume();
            this.stage.hide();
        });
    }

    public void showScene()
    {
        Scene scene = new Scene(GraphicInterface.grid, 750, 750);

        this.stage.setScene(scene);
        this.stage.setTitle("Visualização");
        this.stage.show();
    }

    public void showBoard(char[][] map)
    {
        GraphicInterface.grid = new GridPane();
        GraphicInterface.grid.setHgap(10);
        GraphicInterface.grid.setVgap(10);

        Circle circle;
        for (int row = 0; row < Game.BOARD_SIZE; row++) {
            for (int col = 0; col < Game.BOARD_SIZE; col++) {
                circle = new Circle(30);
                switch (map[row][col]) {
                    case Game.EMPTY_MARKER:
                        circle.setFill(Color.WHITE);
                        break;
                    case Game.SHIP_MARKER:
                        circle.setFill(Color.GRAY);
                        break;
                    case Game.SHOT_SHIP_MARKER:
                        circle.setFill(Color.RED);
                        break;
                    case Game.SHOT_WATER_MARKER:
                        circle.setFill(Color.BLUE);
                        break;
                    case Game.SHIP_ADJACENT_MARKER:
                        circle.setFill(Color.YELLOW);
                        break;
                }
                circle.setStroke(Color.BLACK);
                GraphicInterface.grid.add(circle, col, row);
            }
        }
        this.showScene();
    }
}