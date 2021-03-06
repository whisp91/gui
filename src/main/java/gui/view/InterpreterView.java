package gui.view;

import contract.wrapper.Operation;
import contract.operation.OperationType;
import gui.Main;
import interpreter.Interpreter;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controller and model for the Interpreter view.
 *
 * @author Richard Sundqvist
 */
public class InterpreterView implements InvalidationListener {

    // ============================================================= //
    /*
     *
     * Field variables
     *
     */
    // ============================================================= //

    private final ObservableList<Operation> beforeItems, afterItems;
    private final Map<String, Object> namespace;
    private final Stage root;
    private final TextField beforeCount, afterCount;
    private final Interpreter interpreter;
    private final Stage parent;
    /**
     * Items received from the caller of show ().
     */
    private List<Operation> receivedItems;
    private final Button interpretButton;
    private final Button moveToBeforeButton;

    // ============================================================= //
    /*
     *
     * Constructors
     *
     */
    // ============================================================= //

    @SuppressWarnings("unchecked")
    public InterpreterView (Stage parent) {
        this.parent = parent;
        interpreter = new Interpreter();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/InterpreterView.fxml"));
        fxmlLoader.setController(this);
        root = new Stage();
        root.getIcons().add(new Image(getClass().getResourceAsStream("/assets/icon_cogwheel.png")));
        root.initModality(Modality.APPLICATION_MODAL);
        root.setTitle("Interpreter");
        root.initOwner(parent);
        GridPane p = null;
        try {
            p = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Buttons
        root.setOnCloseRequest(event -> {
            event.consume(); // Better to do this now than missing it later.
            discardInterpreted();
        });
        namespace = fxmlLoader.getNamespace();
        // Lists
        ListView<Operation> interpreterBefore = (ListView<Operation>) namespace.get("interpreterBefore");
        ListView<Operation> interpreterAfter = (ListView<Operation>) namespace.get("interpreterAfter");
        beforeItems = interpreterBefore.getItems();
        afterItems = interpreterAfter.getItems();
        beforeItems.addListener(this);
        afterItems.addListener(this);

        receivedItems = new ArrayList<>();

        // Counters
        beforeCount = (TextField) namespace.get("beforeCount");
        afterCount = (TextField) namespace.get("afterCount");

        // Button enabling
        interpretButton = (Button) namespace.get("interpretButton");
        moveToBeforeButton = (Button) namespace.get("moveToBeforeButton");
        Button keepButton = (Button) namespace.get("keepButton");
        keepButton.disableProperty().bind(moveToBeforeButton.disabledProperty());

        // Size and build
        p.setPrefWidth(this.parent.getWidth() * 0.75);
        p.setPrefHeight(this.parent.getHeight() * 0.75);
        Scene dialogScene = new Scene(p, this.parent.getWidth() * 0.75, this.parent.getHeight() * 0.75);
        root.setScene(dialogScene);
    }

    // ============================================================= //
    /*
     *
     * FControl
     *
     */
    // ============================================================= //

    /**
     * Show the Interpreter View.
     *
     * @param ops The list of operations to use.
     * @return A list of interpreted operations, or {@code null} if the user cancelled.
     */
    public List<Operation> show (List<Operation> ops) {
        interpretButton.setDisable(ops.isEmpty());
        moveToBeforeButton.setDisable(true);
        receivedItems = new ArrayList<>(ops);
        beforeItems.setAll(receivedItems);
        afterItems.clear();
        loadTestCases();
        // Set size and show
        root.setWidth(parent.getWidth() * 0.75);
        root.setHeight(parent.getHeight() * 0.75);
        root.showAndWait();
        return receivedItems;
    }

    /**
     * Listener for the "Keep" button.
     */
    public void keepInterpreted () {
        if (!afterItems.isEmpty()) {
            receivedItems.clear();
            receivedItems.addAll(afterItems);
            root.close();
        } else {
            receivedItems = null;
        }
    }

    /**
     * Listener for the "Discard" button.
     */
    public void discardInterpreted () {
        receivedItems = null;
        root.close();
    }

    /**
     * onAction for the "{@literal<}--" button.
     */
    public void moveToBefore () {
        moveToBeforeButton.setDisable(true);
        interpretButton.setDisable(false);
        if (!afterItems.isEmpty()) {
            beforeItems.setAll(afterItems);
            afterItems.clear();
        }
    }

    /**
     * onAction for the "Interpret" button.
     */
    public void interpret () {
        interpretButton.setDisable(true);
        moveToBeforeButton.setDisable(false);

        List<Operation> interpretedItems = interpreter.interpret(beforeItems);

        int n = beforeItems.size() - interpretedItems.size();
        afterItems.setAll(interpretedItems);
        if (n == 0) {
            Main.console.info("Interpretation did not return any new operations.");
        } else {
            Main.console.info("List size reduced by " + n + ", going from " + beforeItems.size() + " to "
                    + afterItems.size() + ".");
        }
        afterCount.setText("" + afterItems.size());
    }

    // ============================================================= //
    /*
     *
     * Utility
     *
     */
    // ============================================================= //

    private void loadTestCases () {
        VBox casesBox = (VBox) namespace.get("casesBox");
        casesBox.getChildren().clear();
        List<OperationType> selectedTypes = interpreter.getTestCases();
        Insets insets = new Insets(2, 0, 2, 5);
        // Create CheckBoxes for all Consolidate operation types
        for (OperationType type : OperationType.values()) {
            if (type.numAtomicOperations < 2) {
                continue;
            }
            CheckBox cb = new CheckBox(type.toString());
            cb.setOnAction(event -> {
                if (cb.isSelected()) {
                    interpreter.addTestCase(type);
                } else {
                    interpreter.removeTestCase(type);
                }
            });
            if (selectedTypes.contains(type)) {
                cb.setSelected(true);
            } else {
                cb.setSelected(false);
            }
            cb.setPadding(insets);
            casesBox.getChildren().add(cb);
        }
    }

    // ============================================================= //
    /*
     *
     * Interface
     *
     */
    // ============================================================= //

    @Override
    public void invalidated (Observable o) {
        beforeCount.setText("" + beforeItems.size());
        afterCount.setText("" + afterItems.size());
    }
}
