package battleship;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;

public class GraphicInterface extends Application {

    private GridPane grid;
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        grid.setHgap(30);
        grid.setVgap(30);
    }

    public void showPositions(char[][] map)
    {
        this.grid = new GridPane();
        Circle circle;
        for (int row = 0; row < Game.BOARD_SIZE; row++) {
            for (int col = 0; col < Game.BOARD_SIZE; col++) {
                circle = new Circle();
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
                this.grid.add(circle, col, row);
            }
        }

        Scene scene = new Scene(grid, 350, 350);

        stage.setScene(scene);
        stage.setTitle("Visualização");
        stage.show();
    }
}