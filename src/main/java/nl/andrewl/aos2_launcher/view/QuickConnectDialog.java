package nl.andrewl.aos2_launcher.view;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import nl.andrewl.aos2_launcher.GameRunner;
import nl.andrewl.aos2_launcher.Launcher;
import nl.andrewl.aos2_launcher.model.Profile;
import nl.andrewl.aos2_launcher.model.ProgressReporter;
import nl.andrewl.aos2_launcher.model.Server;
import nl.andrewl.aos2_launcher.util.FxUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class QuickConnectDialog {
	@FXML public TextField hostField;
	@FXML public TextField portField;
	@FXML public Button connectButton;

	private final BooleanProperty fieldsValid = new SimpleBooleanProperty(false);

	private final Profile profile;
	private final ProgressReporter progressReporter;
	private final Window owner;
	private final Stage stage;

	public QuickConnectDialog(Window owner, Profile profile, ProgressReporter progressReporter) {
		this.profile = profile;
		this.progressReporter = progressReporter;
		this.owner = owner;
		Scene scene = FxUtils.loadScene("/dialog/quick_connect.fxml", this, Launcher.STANDARD_STYLESHEETS);
		connectButton.disableProperty().bind(fieldsValid.not());
		hostField.textProperty().addListener((observableValue, s, t1) -> validateFields());
		portField.textProperty().addListener((observableValue, s, t1) -> validateFields());
		this.stage = new Stage();
		stage.setScene(scene);
		stage.initOwner(owner);
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.setTitle("Quick Connect");
		stage.show();
	}

	@FXML
	public void onConnect() {
		new GameRunner().run(
				profile,
				new Server(hostField.getText(), Integer.parseInt(portField.getText()), null, null, 1, 1, 1),
				progressReporter,
				owner
		);
		stage.close();
	}

	private void validateFields() {
		boolean valid = hostValid();
		try {
			int port = Integer.parseInt(portField.getText());
			if (port < 0 || port > 65535) valid = false;
		} catch (NumberFormatException e) {
			valid = false;
		}
		fieldsValid.set(valid);
	}

	private boolean hostValid() {
		String hostText = hostField.getText();
		if (hostText == null || hostText.isBlank()) return false;
		try {
			InetAddress.getByName(hostText);
			return true;
		} catch (UnknownHostException e) {
			return false;
		}
	}
}
