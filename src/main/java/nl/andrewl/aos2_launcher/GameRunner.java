package nl.andrewl.aos2_launcher;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Window;
import nl.andrewl.aos2_launcher.model.Profile;
import nl.andrewl.aos2_launcher.model.ProgressReporter;
import nl.andrewl.aos2_launcher.model.Server;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameRunner {
	public void run(Profile profile, Server server, ProgressReporter progressReporter, Window owner) {
		SystemVersionValidator.getJreExecutablePath(progressReporter)
				.whenCompleteAsync((jrePath, throwable) -> {
					if (throwable != null) {
						showPopup(
								owner,
								Alert.AlertType.ERROR,
								"An error occurred while ensuring that you've got the latest Java runtime: " + throwable.getMessage()
						);
					} else {
						VersionFetcher.INSTANCE.getVersionFile(profile.getClientVersion(), progressReporter)
								.whenCompleteAsync((clientJarPath, throwable2) -> {
									progressReporter.disableProgress();
									if (throwable2 != null) {
										showPopup(
												owner,
												Alert.AlertType.ERROR,
												"An error occurred while ensuring you've got the correct client version: " + throwable2.getMessage()
										);
									} else {
										startGame(owner, profile, server, jrePath, clientJarPath);
									}
								});
					}
				});
	}

	private void startGame(Window owner, Profile profile, Server server, Path jrePath, Path clientJarPath) {
		List<String> command = new ArrayList<>();
		command.add(jrePath.toAbsolutePath().toString());
		if (profile.getJvmArgs() != null && !profile.getJvmArgs().isBlank()) {
			command.addAll(Arrays.asList(profile.getJvmArgs().split("\\s+")));
		}
		command.add("-jar");
		command.add(clientJarPath.toAbsolutePath().toString());
		command.add(server.getHost());
		command.add(Integer.toString(server.getPort()));
		command.add(profile.getUsername());
		String[] cmd = command.toArray(new String[0]);
		try {
			Process p = new ProcessBuilder()
					.command(cmd)
					.directory(profile.getDir().toFile())
					.inheritIO()
					.start();
			int result = p.waitFor();
			if (result != 0) {
				showPopup(owner, Alert.AlertType.ERROR, "The game exited with error code: " + result);
			}
		} catch (IOException e) {
			showPopup(owner, Alert.AlertType.ERROR, "An error occurred while starting the game: " + e.getMessage());
		} catch (InterruptedException e) {
			showPopup(owner, Alert.AlertType.ERROR, "The game was interrupted: " + e.getMessage());
		}
	}

	private void showPopup(Window owner, Alert.AlertType type, String text) {
		Platform.runLater(() -> {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.initOwner(owner);
			alert.setContentText(text);
			alert.show();
		});
	}
}
