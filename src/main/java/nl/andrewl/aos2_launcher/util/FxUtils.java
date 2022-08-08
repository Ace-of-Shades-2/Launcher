package nl.andrewl.aos2_launcher.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import nl.andrewl.aos2_launcher.Launcher;

import java.io.IOException;

public class FxUtils {
	public static Parent loadNode(String fxml, Object controller) {
		FXMLLoader loader = new FXMLLoader(FxUtils.class.getResource(fxml));
		if (controller != null) loader.setController(controller);
		try {
			return loader.load();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Scene loadScene(String fxml, Object controller) {
		return new Scene(loadNode(fxml, controller));
	}

	public static Scene loadScene(String fxml, Object controller, String... stylesheets) {
		Scene s = loadScene(fxml, controller);
		addStylesheet(s, stylesheets);
		return s;
	}

	public static Scene loadScene(String fxml) {
		return loadScene(fxml, (Object) null);
	}

	public static Scene loadScene(String fxml, String... stylesheets) {
		Scene s = loadScene(fxml);
		addStylesheet(s, stylesheets);
		return s;
	}

	public static void addStylesheet(Scene scene, String... resources) {
		for (var resource : resources) {
			var url = Launcher.class.getResource(resource);
			if (url == null) throw new RuntimeException("Could not load resource at " + resource);
			scene.getStylesheets().add(url.toExternalForm());
		}
	}
}
