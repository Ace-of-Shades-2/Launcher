package nl.andrewl.aos2_launcher.view;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import nl.andrewl.aos2_launcher.model.Profile;
import nl.andrewl.aos2_launcher.util.FxUtils;

public class ProfileView extends Pane {
	private final Profile profile;

	@FXML public Label nameLabel;
	@FXML public Label clientVersionLabel;
	@FXML public Label usernameLabel;

	public ProfileView(Profile profile) {
		this.profile = profile;
		Node node = FxUtils.loadNode("/profile_view.fxml", this);
		getChildren().add(node);
		nameLabel.textProperty().bind(profile.nameProperty());
		clientVersionLabel.textProperty().bind(profile.clientVersionProperty());
		usernameLabel.textProperty().bind(profile.usernameProperty());
	}

	public Profile getProfile() {
		return this.profile;
	}
}
