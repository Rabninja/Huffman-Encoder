package huffman;

import java.io.File;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class PrimaryController {
    @FXML
    private StackPane encodePane;

    @FXML
    private StackPane decodePane;

    @FXML
    private Rectangle decodeBox;

    @FXML
    private Rectangle encodeBox;

    @FXML
    private GridPane dragDrop;

    @FXML
    protected ObservableList<String> counts;

    @FXML
    protected ObservableList<String> encodings;

    @FXML
    protected Label status;

    @FXML
    protected ProgressBar progress;

    @FXML
    protected CheckMenuItem forceCheck;

    private Thread process;

    private interface Encodable {
        void begin(String source, String destination);
    }

    @FXML
    protected void initialize() {
        progress.setProgress(0.);

        final int OUTLINE_THICKNESS = 1;
        decodeBox.heightProperty().bind(dragDrop.heightProperty().divide(2).subtract(OUTLINE_THICKNESS));
        decodeBox.widthProperty().bind(dragDrop.widthProperty().subtract(OUTLINE_THICKNESS));
        encodeBox.heightProperty().bind(dragDrop.heightProperty().divide(2).subtract(OUTLINE_THICKNESS));
        encodeBox.widthProperty().bind(dragDrop.widthProperty().subtract(OUTLINE_THICKNESS));

        encodePane.setOnDragOver(this::dragOver);
        encodePane.setOnDragEntered(this::dragEntered);
        encodePane.setOnDragExited(this::dragExited);
        encodePane.setOnDragDropped((DragEvent event) -> {
            dragDropped(event, "_decoded", "_encoded", this::encode);
        });

        decodePane.setOnDragOver(this::dragOver);
        decodePane.setOnDragEntered(this::dragEntered);
        decodePane.setOnDragExited(this::dragExited);
        decodePane.setOnDragDropped((DragEvent event) -> {
            dragDropped(event, "_encoded", "_decoded", this::decode);
        });
    }

    @FXML
    private void encodeSelector() {
        final CreateFileWindow create = new CreateFileWindow();
        final var paths = create.getFilePaths();
        if (paths == null) {
            return;
        }
        encode(paths.source, paths.destination);
    }

    @FXML
    private void decodeSelector() {
        final CreateFileWindow create = new CreateFileWindow();
        final var paths = create.getFilePaths();
        if (paths == null) {
            return;
        }
        decode(paths.source, paths.destination);
    }

    @FXML
    public void displayInformation(ActionEvent actionEvent) {
        final String BULLET_POINT = String.valueOf((char)0x2022);
        final String HYPHEN_POINT = " ".repeat(4) + String.valueOf((char)0x2010);
        final String usage = BULLET_POINT + "File\n"
                                + HYPHEN_POINT + "Encode: Select a source file to encode.\n"
                                + HYPHEN_POINT + "Decode: Select a source file to decode.\n"
                                + HYPHEN_POINT + "Read Count: Count the frequency of byte signatures in a file.\n"
                           + BULLET_POINT + "Tabs\n"
                                + HYPHEN_POINT + "Drag & Drop: Drag a file to the ENCODE area to encode the file or drag it to the DECODE area to decode the encoded file.\n"
                                + HYPHEN_POINT + "Character Count: Displays the frequency/count of byte signatures in a file.\n"
                                + HYPHEN_POINT + "Character Encoding: Displays the encoded byte signature for all original byte signatures.\n";
        final Alert alert = new Alert(Alert.AlertType.INFORMATION, usage, ButtonType.CLOSE);
        final Stage stage = (Stage)alert.getDialogPane().getScene().getWindow();
        stage.getIcons().addAll(App.STAGE_STACK.peek().getIcons());
        alert.showAndWait();
    }

    @FXML
    public void readCount(ActionEvent actionEvent) {
        final File file = CreateFileWindow.chooseFile();
        if (file == null || (process != null && process.isAlive())) {
            return;
        }
        process = new Thread(new EncodeFile(true, forceCheck, null, counts, encodings, progress, status, file));
        process.start();
    }

    private void encode(String source, String out) {
        if (process != null && process.isAlive()) {
            return;
        }
        process = new Thread(new EncodeFile(false, forceCheck, out, counts, encodings, progress, status, new File(source)));
        process.start();
    }

    private void decode(String source, String out) {
        if (process != null && process.isAlive()) {
            return;
        }
        process = new Thread(new DecodeFile(out, counts, encodings, progress, status, new File(source)));
        process.start();
    }

    private void dragOver(DragEvent drag) {
        final StackPane button = (StackPane)drag.getSource();
        if (drag.getGestureSource() != button && drag.getDragboard().hasFiles()) {
            drag.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        }
        drag.consume();
    }

    private void dragEntered(DragEvent drag) {
        final StackPane pane = (StackPane)drag.getSource();
        if (drag.getGestureSource() != pane && drag.getDragboard().hasFiles()) {
            final Stop[] stops = new Stop[] { new Stop(0, Color.WHITE), new Stop(1, Color.LIGHTGRAY)};
            final LinearGradient gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops);
            final Rectangle rectangle = (Rectangle)pane.getChildren().get(0);
            rectangle.setFill(gradient);
        }
        drag.consume();
    }

    private void dragExited(DragEvent drag) {
        final StackPane pane = (StackPane) drag.getSource();
        final Rectangle rectangle = (Rectangle)pane.getChildren().get(0);
        rectangle.setFill(Color.TRANSPARENT);
        drag.consume();
    }

    private void dragDropped(DragEvent drag, String ending, String replacement, Encodable encodable) {
        final Dragboard dragboard = drag.getDragboard();
        final boolean success = dragboard.hasFiles();

        if (success) {
            final String source = dragboard.getFiles().get(0).getPath();
            final String extension = source.substring(source.lastIndexOf('.'));
            final String split = source.substring(0, source.lastIndexOf('.'));
            if (split.endsWith(ending)) {
                final String fixed = split.substring(0, split.lastIndexOf('_')) + replacement + extension;
                encodable.begin(source, fixed);
            }
            else {
                final String fixed = split + replacement + extension;
                encodable.begin(source, fixed);
            }
        }
        drag.setDropCompleted(success);
        drag.consume();
    }
}
