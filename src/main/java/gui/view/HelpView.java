package gui.view;

import assets.Const;
import contract.operation.OperationType;
import javafx.animation.Animation;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point3D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;

public class HelpView {

    private final Stage stage = new Stage();
    private final Window owner;
    private BorderPane root;

    /**
     * Create a new HelpView.
     */
    public HelpView (Window owner) {
        this.owner = owner;
        init();
    }

    private void init () {
        FXMLLoader fxmlLoader = new FXMLLoader(this.getClass().getResource("/view/HelpView.fxml"));
        fxmlLoader.setController(this);

        stage.getIcons().add(new Image(getClass().getResourceAsStream("/assets/icon.png")));
        stage.initModality(Modality.NONE);
        stage.setTitle(Const.PROGRAM_NAME + ": Help");
        stage.initOwner(owner);
        try {
            root = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        stage.setOnCloseRequest(event -> {
            event.consume(); // Better to do this now than missing it later.
            stopBoxRotation();
            stage.close();
        });

        createCubes(fxmlLoader);

        Scene dialogScene = new Scene(root, owner.getWidth() * 0.75, owner.getHeight() * 0.75);
        stage.setScene(dialogScene);
    }

    public void show () {
        startBoxRotation();
        stage.show();
        stage.setWidth(owner.getWidth() * 0.75);
        stage.setHeight(owner.getHeight() * 0.75);
    }

    private void createCubes (FXMLLoader fxmlLoader) {

        Label label;

        fxmlLoader.getNamespace().get("box");
        GridPane labels = (GridPane) fxmlLoader.getNamespace().get("box_label");

        int column = 0;
        for (OperationType ot : OperationType.values()) {
            // Create box
            final Box box = new Box();
            box.setMaterial(new PhongMaterial(ot.color));
            box.setWidth(render.assets.Const.DEFAULT_ELEMENT_WIDTH);
            box.setHeight(render.assets.Const.DEFAULT_ELEMENT_HEIGHT);
            box.setDepth(render.assets.Const.DEFAULT_ELEMENT_WIDTH + render.assets.Const.DEFAULT_ELEMENT_HEIGHT);
            box.setOnMouseClicked(event -> {
                about(ot);
                boxClicked(box);
            });

            // Randomise rotation
            double[] random = new double[3];
            for (int i = 0; i < 2; i++) {
                int sign = Math.random() < 0.5 ? -1 : 1;
                random[i] = Math.random() * sign;
            }
            Point3D axis = new Point3D(random[0], random[1], random[2]);
            box.setRotationAxis(axis);
            box.setRotate(Math.random() * 180);

            // Rotation
            RotateTransition rt = new RotateTransition(Duration.seconds(10), box);
            rt.setByAngle(360);
            rt.setCycleCount(Animation.INDEFINITE);
            rotTransitions.add(rt);

            /*
             * Create label
             */
            label = new Label(" " + ot.name().toUpperCase() + " ");
            label.setFont(Font.font("consolas", 15));
            label.setStyle("-fx-background-color: rgba(255, 255, 255, 0.8);");

            // Add to panels
            labels.add(label, column, 0);
            // ((BorderPane) boxes.getChildren().get(column)).setCenter(box);
            column++;
        }
    }

    private void boxClicked (final Box box) {
        if (box.getScaleX() == 1) {
            ScaleTransition st = new ScaleTransition(Duration.millis(1500), box);
            st.setByX(-Math.random());
            st.setByY(-Math.random());
            st.setByZ(-Math.random());
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.play();
        }
    }

    private void about (OperationType ot) {
        root.setCenter(new Label(ot.name()));
    }

    private final ArrayList<RotateTransition> rotTransitions = new ArrayList<RotateTransition>();

    private void stopBoxRotation () {
        for (RotateTransition rt : rotTransitions) {
            rt.stop();
        }
    }

    private void startBoxRotation () {
        for (RotateTransition rt : rotTransitions) {
            rt.play();
        }
    }

    public void aboutArray () {
        root.setCenter(new Label("Arrays r gud"));
    }

    public void aboutOrphan () {
        root.setCenter(new Label("orphans r gudder"));
    }

    public void aboutTree () {
        root.setCenter(new Label("tress r guddest"));
    }

    public void onMouseClicked (Event me) {

    }

    public void onMouseEntered (Event me) {
        ((Pane) me.getSource()).setBorder(render.assets.Const.BORDER_MOUSEOVER);
    }

    public void onMouseExited (Event me) {
        ((Pane) me.getSource()).setBorder(null);
    }
}
