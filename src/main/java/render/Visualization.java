package render;

import assets.Debug;
import assets.Tools;
import contract.datastructure.DataStructure;
import contract.datastructure.Element;
import contract.datastructure.VisualType;
import contract.wrapper.Locator;
import contract.wrapper.Operation;
import contract.operation.OP_ReadWrite;
import contract.operation.OP_Swap;
import contract.operation.OP_ToggleScope;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import model.ExecutionModel;
import render.assets.ARenderManager;
import render.assets.Const;

import java.util.Collection;
import java.util.HashMap;

/**
 * Handler class for rendering an ExecutionModel.
 *
 * @author Richard Sundqvist
 */
public class Visualization extends StackPane {

    // ============================================================= //
    /*
     *
     * Field variables
     *
     */
    // ============================================================= //

    /**
     * Pane for drawing of animated elements.
     */
    private final Pane animationPane = new Pane();

    /**
     * Animation time in milliseconds.
     */
    private long animationTime = Const.DEFAULT_ANIMATION_TIME;
    /**
     * Determines whether operations are animated on the animated_nodes canvas.
     */
    private boolean animate;
    /**
     * The ExecutionModel to visualise.
     */
    private final ExecutionModel executionModel;
    /**
     * A list of render managers for the data structures.
     */
    private final Pane managerPane = new Pane();
    /**
     * A mapping of renders and their managers.
     */
    private final HashMap<String, ARenderManager> managerMap = new HashMap<>();

    // ============================================================= //
    /*
     *
     * Constructors
     *
     */
    // ============================================================= //

    /**
     * Create a new Visualization.
     *
     * @param executionModel The ExecutionModel to render.
     */
    public Visualization (ExecutionModel executionModel) {
        this.executionModel = executionModel;

        // Shared animation space
        animationPane.setMouseTransparent(true);
        animationPane.maxWidth(Double.MAX_VALUE);
        animationPane.maxHeight(Double.MAX_VALUE);
        animate = true;

        // Add stacked canvases
        getChildren().addAll(Tools.HINT_PANE, managerPane, animationPane);
    }

    // ============================================================= //
    /*
     *
     * Control
     *
     */
    // ============================================================= //

    /**
     * Clear the visualization.
     */
    public void clear () {
        managerMap.clear();
        managerPane.getChildren().clear();
        animationPane.getChildren().clear();
        Tools.HINT_PANE.setVisible(true);
    }

    public void clearAndCreateVisuals () {
        clear();
        for (DataStructure struct : executionModel.getDataStructures().values()) {
            ARenderManager arm = new ARenderManager(struct, animationPane);
            managerPane.getChildren().add(arm);
            managerMap.put(struct.identifier, arm);
            if (arm.getDataStructure().resolveVisual() == VisualType.single) {
                arm.toFront();
            }
        }
        Tools.HINT_PANE.setVisible(managerPane.getChildren().isEmpty());
        autoPosition();
    }

    /**
     * Command the Visualization to update its renders and animate the given operation.
     *
     * @param op An operation to animate.
     */
    public void render (Operation op) {
        for (Object rm : managerPane.getChildren()) {
            ((ARenderManager) rm).getRender().render();
        }
        if (animate && op != null) {
            animate(op);
        }
    }

    /**
     * Force Render initialisation.
     */
    public void init () {
        for (Object rm : managerPane.getChildren()) {
            ((ARenderManager) rm).getRender().repaintAll();
        }

    }

    /**
     * Animate an operation.
     *
     * @param op The operation to animate.
     */
    public void animate (Operation op) {
        if (op == null) {
            return;
        }
        switch (op.operation) {
            case read:
            case write:
                animateReadWrite((OP_ReadWrite) op);
                break;
            case remove:
                animateToggleScope((OP_ToggleScope) op);
                break;
            case swap:
                animateSwap((OP_Swap) op);
                break;
            default:
                // Do nothing.
                break;
        }
    }

    /**
     * Create a render which shows live updating statistics for the ExecutionModel.
     */
    public void showLiveStats () {
        System.err.println("Visualization.showLiveStats() not implemented yet.");
    }

    /**
     * Reset the renders' states.
     */
    public void reset () {
        managerMap.values().forEach(ARenderManager::reset);
    }

    /**
     * Attempt to place visuals with minimal overlap. Will return {@code false} if
     * placement failed. Note that {@code true} does not guarantee that there is no
     * overlap between renders.
     *
     * @return {@code false} if placement failed.
     */
    public boolean autoPosition () {
        boolean successful = true;

        ARenderManager arm;
        double padding = Const.DEFAULT_RENDER_PADDING;
        double xPos;
        double yPos;

        //@formatter:off
        int northWest = 0;
        int nWExpand = 0; // Default.
        int southWest = 0;
        int sWExpand = 0; // Bar Chart.
        int northEast = 0;
        int nEExpand = 0; // Single Elements.
        //@formatter:on

        for (Node node : managerPane.getChildren()) {
            arm = (ARenderManager) node;

            switch (arm.getDataStructure().visual) {
                case single:
                    yPos = northEast * 120 + padding;
                    xPos = getWidth() - (150 + padding) * (nEExpand + 1);
                    if (!(checkXPos(xPos) && checkYPos(yPos))) {
                        northEast = 0;
                        nEExpand++;
                        yPos = northEast * 120 + padding;
                        xPos = getWidth() - 150 * (nEExpand + 1) - padding;
                    }
                    northEast++;
                    break;
                case bar:
                    xPos = padding + getWidth() * sWExpand;
                    yPos = getHeight() - 125 - render.assets.Const.DEFAULT_RENDER_HEIGHT * southWest - padding * 2;
                    southWest++;
                    break;
                default:
                    xPos = padding + getWidth() * nWExpand;
                    yPos = (padding + render.assets.Const.DEFAULT_RENDER_HEIGHT) * northWest + padding;
                    northWest++;
                    break;

            }

            // Make sure users can see the render.
            if (!checkPositions(xPos, yPos)) {
                if (Debug.ERR) {
                    System.err.println("Using default placement for \"" + arm.getDataStructure() + "\".");
                }
                yPos = padding;
                xPos = padding;
                successful = false;
            }

            arm.getRender().setTranslateX(xPos);
            arm.getRender().setTranslateY(yPos);
            arm.getRender().updateInfoLabels();
        }

        return successful;
    }

    /**
     * Set the relative node size for the all renders. If {@code factor == 2}, the largest
     * element will be twice as large as the smallest. Relation is inversed for
     * {@code 0 < factor < 1}.<br>
     * <br>
     * Will disable for {@code factor <= 0} and {@code factor == 1}
     *
     * @param factor The min-max size factor for this render.
     */
    public void setRelativeNodeSize (double factor) {
        for (ARenderManager manager : managerMap.values()) {
            manager.setRelativeNodeSize(factor);
        }
    }

    // ============================================================= //
    /*
     *
     * Getters and Setters
     *
     */
    // ============================================================= //

    /**
     * Returns the managers used by this Visualization.
     *
     * @return An ARenderManager collection.
     */
    public Collection<ARenderManager> getManagers () {
        return managerMap.values();
    }

    /**
     * Set the animation time in milliseconds for all animations. Actual animation time
     * will be {@code millis * 0.65} to allow rest time after the animation.
     *
     * @param animationTime The new animation time in milliseconds.
     */
    public void setAnimationTime (long animationTime) {
        this.animationTime = (long) (animationTime * 0.65000);
    }

    /**
     * Set animation on or off.
     *
     * @param animate The new animation option.
     */
    public void setAnimate (boolean animate) {
        this.animate = animate;
    }

    /**
     * Returns the animation setting for the visualization.
     *
     * @return {@code true} if animation is enabled, {@code false} otherwise.
     */
    public boolean getAnimate () {
        return animate;
    }

    private void animateToggleScope (OP_ToggleScope toggleScope) {
        Locator tar = toggleScope.getTarget();
        Element e;

        /**
         * Var1 params
         */
        for (DataStructure struct : executionModel.getDataStructures().values()) {
            e = struct.getElement(tar);
            if (e != null) {
                ARender render = managerMap.get(struct.identifier).getRender();
                render.animateToggleScope(e, animationTime);
                if (Debug.ERR) {
                    System.err.println("\nVisualization.animateRemove():");
                }
                return;
            }
        }
    }

    private void animateReadWrite (OP_ReadWrite rw) {
        Locator source = rw.getSource();
        Locator target = rw.getTarget();
        if (source == null && target == null) {
            return;
        }
        Element src_e = null, tar_e = null;
        ARender src_render = null, tar_render = null;
        /**
         * Source parameters
         */
        for (DataStructure struct : executionModel.getDataStructures().values()) {
            src_e = struct.getElement(source);
            if (src_e != null) {
                src_render = managerMap.get(struct.identifier).getRender();
                break; // Source found
            }
        }
        /**
         * Target parameters
         */
        for (DataStructure struct : executionModel.getDataStructures().values()) {
            tar_e = struct.getElement(target);
            if (tar_e != null) {
                tar_render = managerMap.get(struct.identifier).getRender();
                break; // Target found
            }
        }

        if (Debug.ERR) {
            System.err.println("\nVisualization.animateReadWrite():");
            System.err.println("Has target: " + (tar_e == null ? "false" : tar_render.getDataStructure()));
            System.err.println("Has source: " + (src_e == null ? "false" : src_render.getDataStructure()));
        }

        /**
         * Start animations
         */
        if (src_e != null && tar_e != null) {
            // Render data transfer between two known structures.
            tar_render.animateReadWrite(src_e, src_render, tar_e, tar_render, animationTime);
        } else if (src_e != null && tar_e == null) {
            // Render read without target.
            src_render.animateReadWrite(src_e, src_render, null, null, animationTime);
        } else if (tar_e != null) {
            // Render write without source.
            tar_render.animateReadWrite(null, null, tar_e, tar_render, animationTime);
        }
    }

    /**
     * Trigger an animation of a swap.
     *
     * @param swap The swap to animate.
     */
    private void animateSwap (OP_Swap swap) {
        Locator var1 = swap.getVar1();
        Locator var2 = swap.getVar2();
        if (var1 == null || var2 == null) {
            return;
        }
        Element v1_e = null, v2_e = null;
        ARender v1_render = null, v2_render = null;
        /**
         * Var1 params
         */
        for (DataStructure struct : executionModel.getDataStructures().values()) {
            v1_e = struct.getElement(var1);
            if (v1_e != null) {
                v1_render = managerMap.get(struct.identifier).getRender();
                break;
            }
        }
        /**
         * Var2 params
         */
        for (DataStructure struct : executionModel.getDataStructures().values()) {
            v2_e = struct.getElement(var2);
            if (v2_e != null) {
                v2_render = managerMap.get(struct.identifier).getRender();
                break;
            }
        }
        /**
         * Start animations
         */

        if (Debug.ERR) {
            System.err.println("\nVisualization.animateSwap():");
        }

        if (v1_render == null || v2_render == null) {
            System.err.println("Error in Visualization.animateSwap(): Failed to resolve render: " +
                    "v1_render = " + v1_render + ", v2_render = " + v2_render);
            return;
        }

        v1_render.animateSwap(v1_e, v1_render, v2_e, v2_render, animationTime);
        v2_render.animateSwap(v2_e, v2_render, v1_e, v1_render, animationTime);
    }

    private boolean checkPositions (double xPos, double yPos) {
        boolean result = true;

        if (!checkXPos(xPos)) {
            if (Debug.OUT) {
                System.out.println("Bad X-Coordinate: " + xPos + " not in " + xRange() + ".");
            }
            result = false;
        }
        if (!checkYPos(yPos)) {
            if (Debug.OUT) {
                System.out.println("Bad Y-Coordinate: " + yPos + " not in " + yRange() + ".");
            }
            result = false;
        }

        return result;
    }

    /**
     * Check to see if an X-Coordinate is in the acceptable range.
     *
     * @param xPos An x-coordinate.
     * @return True if the coordinate good, false otherwise.
     */
    private boolean checkXPos (double xPos) {
        return !(xPos < getXMin() || xPos > getXMax());
    }

    /**
     * Returns the maximum acceptable X-Coordinate.
     *
     * @return The maximum acceptable X-Coordinate.
     */
    private double getXMax () {
        return getWidth() - 50;
    }

    /**
     * Returns the minimum acceptable X-Coordinate.
     *
     * @return The minimum acceptable X-Coordinate.
     */
    private double getXMin () {
        return render.assets.Const.DEFAULT_RENDER_PADDING;
    }

    /**
     * Check to see if an Y-Coordinate is in the acceptable range.
     *
     * @param yPos An y-coordinate.
     * @return True if the coordinate good, false otherwise.
     */

    private boolean checkYPos (double yPos) {
        return !(yPos < getYMin() || yPos > getYMax());
    }

    /**
     * Returns the maximum acceptable Y-Coordinate.
     *
     * @return The maximum acceptable Y-Coordinate.
     */
    private double getYMax () {
        return getHeight() - 50;
    }

    /**
     * Returns the minimum acceptable Y-Coordinate.
     *
     * @return The minimum acceptable Y-Coordinate.
     */
    private double getYMin () {
        return render.assets.Const.DEFAULT_RENDER_PADDING;
    }

    /**
     * Returns a String representing the range of acceptable X-Coordinates
     *
     * @return A String representing the range
     */
    private String xRange () {
        return "[" + getXMin() + ", " + getXMax() + "]";
    }

    /**
     * Returns a String representing the range of acceptable Y-Coordinates
     *
     * @return A String representing the range
     */
    private String yRange () {
        return "[" + getYMin() + ", " + getYMax() + "]";
    }
}
