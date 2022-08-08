package nl.andrewl.aos2_launcher.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import nl.andrewl.aos2_launcher.model.Server;

import java.io.IOException;

public class ServerView extends Pane {
	private final Server server;

	@FXML public Label nameLabel;
	@FXML public Label descriptionLabel;
	@FXML public Label hostLabel;
	@FXML public Label portLabel;
	@FXML public Label maxPlayersLabel;
	@FXML public Label currentPlayersLabel;

	public ServerView(Server server) {
		this.server = server;

		try {
			FXMLLoader loader = new FXMLLoader(ServerView.class.getResource("/server_view.fxml"));
			loader.setController(this);
			Node node = loader.load();
			getChildren().add(node);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		nameLabel.textProperty().bind(server.nameProperty());
		descriptionLabel.textProperty().bind(server.descriptionProperty());
		hostLabel.textProperty().bind(server.hostProperty());
		portLabel.textProperty().bind(server.portProperty().asString());
		maxPlayersLabel.textProperty().bind(server.maxPlayersProperty().asString());
		currentPlayersLabel.textProperty().bind(server.currentPlayersProperty().asString());
	}

	public Server getServer() {
		return server;
	}
}
