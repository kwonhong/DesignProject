package de.yadrone.designProject;

import de.yadrone.base.ARDrone;
import de.yadrone.base.IARDrone;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.opencv.core.Core;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        BorderPane root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        root.setStyle("-fx-background-color: whitesmoke;");

        Scene scene = new Scene(root, 1000, 500);
        primaryStage.setTitle("Object Recognition");
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public static void main(String[] args) {
        // load the native OpenCV library
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        launch(args);
    }
}
