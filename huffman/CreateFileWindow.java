package huffman;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;

public class CreateFileWindow extends BorderPane implements SubWindow {
    @FXML
    private TextField source;

    @FXML
    private TextField destination;

    private boolean cancelled = false;

    public static class FileSelection {
        public final String source;
        public final String destination;

        public FileSelection(String source, String destination) {
            this.source = source;
            this.destination = destination;
        }
    }

    @FXML
    public void initialize() {

    }

    public CreateFileWindow() {
        final String pathway = "create_file_window.fxml";
        final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(pathway));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private boolean isValid() {
        if (cancelled) {
            return false;
        }
        final String pathway = source.getText();
        final String destiny = destination.getText();
        final int extension = destiny.lastIndexOf('.');
        if (extension < 0) {
            return false;
        }
        final int directory = destiny.lastIndexOf('\\');
        if (directory < 0) {
            return false;
        }

        final String fixed = destiny.substring(0, directory);
        final File file = new File(pathway);
        final File out = new File(fixed);
        return file.exists() && file.isFile() && out.isDirectory();
    }

    @FXML
    public void confirm() {
        if (isValid()) {
            App.STAGE_STACK.peek().close();
        }
    }

    @FXML
    public void browseSource() {
        chooseFieldFile(source);
    }

    @FXML
    public void browseDestination() {
        chooseFieldSave(destination);
    }

    @FXML
    public void close() {
        cancelled = true;
        App.STAGE_STACK.peek().close();
    }

    @Override
    public Scene buildScene() {
        return new Scene(this);
    }

    @Override
    public String getTitle() {
        return "HuffmanEncoder - Select File";
    }

    @Override
    public double getWindowWidth() {
        return 400;
    }

    @Override
    public double getWindowHeight() {
        return 200;
    }

    public FileSelection getFilePaths() {
        display(App.STAGE_STACK.peek());
        final String pathway = source.getText();
        final String destiny = destination.getText();
        return isValid() ? new FileSelection(pathway, destiny) : null;
    }

    private void chooseFieldSave(TextField field) {
        final File file = chooseSave();

        if (file != null) {
            field.setText(file.getPath());
        }
    }

    private void chooseFieldFile(TextField field) {
        final File file = chooseFile();

        if (file != null) {
            field.setText(file.getPath());
        }
    }

    static public File chooseSave() {
        final FileChooser fileChooser = new FileChooser();

        return fileChooser.showSaveDialog(App.STAGE_STACK.peek());
    }

    static public File chooseFile() {
        final FileChooser fileChooser = new FileChooser();

        return fileChooser.showOpenDialog(App.STAGE_STACK.peek());
    }
}
