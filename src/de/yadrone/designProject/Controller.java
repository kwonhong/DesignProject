package de.yadrone.designProject;

import de.yadrone.base.ARDrone;
import de.yadrone.base.IARDrone;
import de.yadrone.base.video.ImageListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;

import java.awt.image.BufferedImage;

public class Controller {

    private IARDrone drone;

    @FXML
    private ImageView droneVideoStreamImageView;

    @FXML
    private void connectDrone() {
        try {
            // Connecting the application with the Parrot 2.0 Drone.
            // If the application is not connected the drone, it
            // will throw ConnectException.
            drone = new ARDrone();

            // Upon calling the start method,
            // all managers (i.e. Command-, Configuration, NavData- and VideoManager)
            // will begin their work
            drone.start();

        } catch (Exception exc) {
            System.err.println("Failed to connect to the drone...");
            exc.printStackTrace();
        }
    }

    @FXML
    private void startVideoStream() {
        // Checking the drone connection before starting the video steaming
        if (drone == null || !drone.getConfigurationManager().isConnected()) {
            System.err.println("Not connected to a drone properly...");
            return;
        }

        drone.getVideoManager().addImageListener(new ImageListener() {
            @Override
            public void imageUpdated(BufferedImage image) {
                droneVideoStreamImageView.setImage(SwingFXUtils.toFXImage(image, null));
            }
        });
    }
}
