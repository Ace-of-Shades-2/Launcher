package nl.andrewl.aos2_launcher;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import nl.andrewl.aos2_launcher.util.FileUtils;
import nl.andrewl.aos2_launcher.util.FxUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The main starting point for the launcher app.
 */
public class Launcher extends Application {
	public static final Path BASE_DIR = Path.of(System.getProperty("user.home"), ".ace-of-shades");
	public static final Path VERSIONS_DIR = BASE_DIR.resolve("versions");
	public static final Path PROFILES_FILE = BASE_DIR.resolve("profiles.json");
	public static final Path PROFILES_DIR =  BASE_DIR.resolve("profiles");
	public static final Path JRE_PATH = BASE_DIR.resolve("jre");
	public static final String[] STANDARD_STYLESHEETS = {"/font/fonts.css", "/styles.css"};

	@Override
	public void start(Stage stage) throws IOException {
		if (!Files.exists(BASE_DIR)) Files.createDirectory(BASE_DIR);
		if (!Files.exists(VERSIONS_DIR)) Files.createDirectory(VERSIONS_DIR);
		if (!Files.exists(PROFILES_DIR)) Files.createDirectory(PROFILES_DIR);
		stage.setScene(FxUtils.loadScene("/main_view.fxml", STANDARD_STYLESHEETS));
		stage.setTitle("Ace of Shades - Launcher");
		stage.getIcons().add(FileUtils.loadImage("/icon.png"));
		stage.show();
	}

	private void addStylesheet(Scene scene, String resource) throws IOException {
		var url = Launcher.class.getResource(resource);
		if (url == null) throw new IOException("Could not load resource at " + resource);
		scene.getStylesheets().add(url.toExternalForm());
	}

	public static void main(String[] args) {
		launch(args);
	}
}
