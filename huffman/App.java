package huffman;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Stack;


public class App extends Application {
    private static Scene scene;
    public static final Stack<Stage> STAGE_STACK = new Stack<>();

    @Override
    public void start(Stage primaryStage) throws IOException {
        final FXMLLoader loader = new FXMLLoader(getClass().getResource("primary.fxml"));
        final Parent root = loader.load();
        primaryStage.setTitle("Huffman Encoder");
        primaryStage.getIcons().addAll(new Image(getClass().getResourceAsStream("icon64x64.png")),
                new Image(getClass().getResourceAsStream("icon32x32.png")));
        scene = new Scene(root, 600, 600);
        STAGE_STACK.push(primaryStage);
        primaryStage.setScene(scene);
        primaryStage.show();

    }


    public static void main(String[] args) {
        launch();
    }

}