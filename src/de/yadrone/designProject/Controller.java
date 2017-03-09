package de.yadrone.designProject;

import de.yadrone.base.ARDrone;
import de.yadrone.base.IARDrone;
import de.yadrone.base.video.ImageListener;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static de.yadrone.designProject.Utility.bufferedImageToMat;
import static de.yadrone.designProject.Utility.mat2Image;

public class Controller {
    private static final double SENSITIVITY = 15.0;
    private static final int FRAME_PERIOD = 60;
    private static final float MIN_CONTOUR_AREA = 2000;
    private static final int DILATION_SIZE = 16;
    private static final int EROSION_SIZE = 6;
    
    // Buttons
    @FXML private Button connectDroneButton;
    @FXML private Button startCameraButton;
    @FXML private Button takeOffButton;
    @FXML private Button landButton;
    @FXML private Button increaseAltitudeButton;
    @FXML private Button decreaseAltitudeButton;
    @FXML private Button forwardButton;
    @FXML private Button backButton;
    @FXML private Button rotateLeftButton;
    @FXML private Button rotateRightButton;
    @FXML private Button emergencyCutOffButton;
    
    @FXML private Button redButton;
    @FXML private Button greenButton;
    
    // Sliders to set HSV min/max values.
    @FXML private Slider hueStartSlider;
    @FXML private Slider hueStopSlider;
    @FXML private Slider saturationStartSlider;
    @FXML private Slider saturationStopSlider;
    @FXML private Slider valueStartSlider;
    @FXML private Slider valueStopSlider;

    // ImageView to display frames
    @FXML private ImageView originalFrameImageView;
    @FXML private ImageView hsvFrameImageView;
    @FXML private ImageView filteredFrameImageView;
    @FXML private ImageView morphFrameImageView;

    // TextArea to display relative position information
    @FXML private TextArea relativePositionTxtArea;

    private IARDrone drone;

	public Controller() {
	}

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
            
            // enable the take-off and camera buttons
            connectDroneButton.setDisable(true);
            startCameraButton.setDisable(false);
            takeOffButton.setDisable(false);
            
        } catch (Exception exc) {
            System.err.println("Failed to connect to the drone...");
            exc.printStackTrace();
        }
    }

    @FXML
    private void startCamera() {
        startDroneVideoStream();
        startCameraButton.setDisable(true);
    }

    @FXML
	private void takeOff() {
		drone.takeOff();
		takeOffButton.setDisable(true);
		landButton.setDisable(false);
        increaseAltitudeButton.setDisable(false);
        decreaseAltitudeButton.setDisable(false);
        forwardButton.setDisable(false);
        backButton.setDisable(false);
        rotateLeftButton.setDisable(false);
        rotateRightButton.setDisable(false);
        emergencyCutOffButton.setDisable(false);
	}

    @FXML
    private void land() {
    	drone.landing();
		takeOffButton.setDisable(false);
		landButton.setDisable(true);
        increaseAltitudeButton.setDisable(true);
        decreaseAltitudeButton.setDisable(true);
        forwardButton.setDisable(true);
        backButton.setDisable(true);
        rotateLeftButton.setDisable(true);
        rotateRightButton.setDisable(true);
        emergencyCutOffButton.setDisable(true);
    }
    
    @FXML
    private void increaseAltitude() {
    	drone.up();
    }
    
    @FXML
    private void decreaseAltitude() {
    	drone.down();
    }
    
    @FXML
    private void forward() {
    	drone.forward();
    }
    
    @FXML
    private void back() {
    	drone.backward();
    }
    
    @FXML
    private void rotateLeft() {
    	drone.spinLeft();
    }
    
    @FXML
    private void rotateRight() {
    	drone.spinRight();
    }
    
    @FXML
    private void emergencyCutOff() {
    	drone.reset();
    	
        takeOffButton.setDisable(false);
        landButton.setDisable(true);
        increaseAltitudeButton.setDisable(true);
        decreaseAltitudeButton.setDisable(true);
        forwardButton.setDisable(true);
        backButton.setDisable(true);
        rotateLeftButton.setDisable(true);
        rotateRightButton.setDisable(true);
        emergencyCutOffButton.setDisable(true);
        
    }
    
    private void startDroneVideoStream() {
        // Checking the drone connection before starting the video steaming
        if (drone == null || !drone.getConfigurationManager().isConnected()) {
            System.err.println("Not connected to a drone properly...");
            return;
        }

        drone.getVideoManager().addImageListener(new ImageListener() {
            @Override
            public void imageUpdated(BufferedImage image) {
                updateFrames(bufferedImageToMat(image));
            }
        });
    }

    private void startComputerVideoStream() {
        // TODO: Remove this after putting dependency injection
        final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        VideoCapture videoCapture = new VideoCapture();


        // Attempts to open the front camera (if there is one).
        // Can access different camera by changing the index number
        videoCapture.open(0);
        if (!videoCapture.isOpened()) {
            System.err.printf("Camera Access Denied...");
            return;
        }

        final Runnable frameGrabberRunnable = () -> {
            // TODO: Handle the case of camera being suddenly inaccessible.
            if (!videoCapture.isOpened()) {
                System.err.printf("Camera Access Denied...");
                return;
            }

            // 1. Attempt to read the current frame from the opened camera.
            Mat rgbFrame = new Mat();
            videoCapture.read(rgbFrame);
            if (rgbFrame.empty()) {
                System.err.printf("Captured Frame is empty...");
                return;
            }

            updateFrames(rgbFrame);
        };

        // Grabbing the frame every 33 ms using the runnable.
        scheduledExecutorService.scheduleAtFixedRate(
                frameGrabberRunnable, 0, FRAME_PERIOD, TimeUnit.MILLISECONDS);
    }

    private void updateFrames(Mat rgbFrame) {
        // 1. Displaying the original frame
        originalFrameImageView.setImage(mat2Image(rgbFrame));

        // 2. Attempt to convert frame from BGR to HSV color-space
        Mat hsvFrame = new Mat();
        Imgproc.cvtColor(rgbFrame, hsvFrame, Imgproc.COLOR_BGR2HSV);
        if (hsvFrame.empty()) {
            System.err.printf("Converting from BGR to HSV color-space failed...");
            return;
        }

        // Displaying the hsv converted frame
        hsvFrameImageView.setImage(mat2Image(hsvFrame));

        // 3. Attempt to filter HSV image between the specified HSV values
        Mat filteredFrame = new Mat();
        Core.inRange(
                hsvFrame,
                new Scalar(
                        hueStartSlider.getValue(),
                        saturationStartSlider.getValue(),
                        valueStartSlider.getValue()),
                new Scalar(
                        hueStopSlider.getValue(),
                        saturationStopSlider.getValue(),
                        valueStopSlider.getValue()),
                filteredFrame);

        // Displaying the filtered frame
        filteredFrameImageView.setImage(mat2Image(filteredFrame));

        // 4. Attempt to erode the frame
        Mat erodedFrame = new Mat();
        Mat erodeElement = Imgproc.getStructuringElement(
                Imgproc.MORPH_RECT, new Size(EROSION_SIZE, EROSION_SIZE));
        Imgproc.erode(filteredFrame, erodedFrame, erodeElement);
        Imgproc.erode(erodedFrame, erodedFrame, erodeElement);

        // 5. Attempt to dilate the frame
        Mat dilatedFrame = new Mat();
        Mat dilatedElement = Imgproc.getStructuringElement(
                Imgproc.MORPH_RECT, new Size(DILATION_SIZE, DILATION_SIZE)); // Dilate with larger element so make sure object is nicely visible
        Imgproc.dilate(erodedFrame, dilatedFrame, dilatedElement);
        Imgproc.dilate(dilatedFrame, dilatedFrame, dilatedElement);

        // Displaying the frame after morphological operation
        morphFrameImageView.setImage(mat2Image(dilatedFrame));
        findRelativePosition(dilatedFrame);
    }

    private void findRelativePosition(Mat morphFrame) {
        // 6. Finding the coordinate of the detected object.
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(
                morphFrame,
                contours,
                hierarchy,
                Imgproc.RETR_CCOMP,
                Imgproc.CHAIN_APPROX_SIMPLE);

        // Checking if a contour exists
        if (hierarchy.size().height > 0 && hierarchy.size().width > 0) {
            for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {

                // Finding the area of the contour
                Double contourArea = Imgproc.contourArea(contours.get(idx));
                if (contourArea < MIN_CONTOUR_AREA)
                    return;

                // Finding the center point
                float[] radius = new float[1];
                final Point center = new Point();
                Imgproc.minEnclosingCircle(
                        new MatOfPoint2f(contours.get(idx).toArray()), center, radius);

                final String appendingData =
                        String.format("Contour Id: %1$d, " +
                                        "Area: %2$.1f, " +
                                        "Center Point: (x: %3$.1f, y: %4$.1f)\n",
                                idx, contourArea, center.x, center.y);
                Platform.runLater(() -> relativePositionTxtArea.appendText(appendingData));
            }
        }
    }

    @FXML
    public void setRedHsvRange(ActionEvent event) {
        setHsvRange(170 - SENSITIVITY, 70, 50,
                180 + SENSITIVITY, 255, 255);
    }

    @FXML
    public void setGreenHsvRange(ActionEvent event) {
        setHsvRange(50 - SENSITIVITY, 100, 50,
                60 + SENSITIVITY, 255, 255);
    }

    private void setHsvRange(double hueStart, double satStart, double valueStart,
                             double hueStop, double satStop, double valueStop) {

        hueStartSlider.setValue(hueStart);
        hueStopSlider.setValue(hueStop);

        saturationStartSlider.setValue(satStart);
        saturationStopSlider.setValue(satStop);

        valueStartSlider.setValue(valueStart);
        valueStopSlider.setValue(valueStop);
    }
}
