package nl.andrewl.aos2_launcher;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import nl.andrewl.aos2_launcher.model.ClientVersionRelease;
import nl.andrewl.aos2_launcher.model.ProgressReporter;
import nl.andrewl.aos2_launcher.util.FileUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionFetcher {
	private static final String BASE_GITHUB_URL = "https://api.github.com/repos/Ace-of-Shades-2/Game";
	private static final Pattern OFFICIAL_VERSION_REGEX = Pattern.compile("aos2-client-v\\d+\\.\\d+\\.\\d+-" + SystemVersionValidator.getPreferredVersionSuffix() + "\\.jar");
	private static final Pattern VERSION_REGEX = Pattern.compile("v\\d+\\.\\d+\\.\\d+");

	public static final VersionFetcher INSTANCE = new VersionFetcher();

	private final List<ClientVersionRelease> availableReleases;

	private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
	private boolean loaded = false;
	private CompletableFuture<List<ClientVersionRelease>> activeReleaseFetchFuture;

	public VersionFetcher() {
		this.availableReleases = new ArrayList<>();
	}

	/**
	 * Gets all possible client versions that the client could choose, including
	 * both custom local versions, and versions which are available online.
	 * Local versions which don't match any online release are simply added to
	 * the list with the ".jar" removed.
	 * @return A future that returns the list of available versions.
	 */
	public CompletableFuture<List<String>> getAvailableClientVersions() {
		return getAvailableReleases().thenApplyAsync(releases -> {
			List<String> versions = new ArrayList<>(releases.size());
			for (var r : releases) versions.add(r.tag());
			try (var s = Files.list(Launcher.VERSIONS_DIR)) {
				s.filter(this::isVersionFile)
					.map(path -> {
						String filename = path.getFileName().toString();
						if (isOfficialVersionFile(path)) return extractVersion(path);
						return filename.substring(0, filename.length() - 4);
					})
					.sorted()
					.forEachOrdered(name -> {
						if (!versions.contains(name)) versions.add(name);
					});
			} catch (IOException e) {
				e.printStackTrace();
			}
			return versions;
		});
	}

	public CompletableFuture<ClientVersionRelease> getRelease(String versionTag) {
		return getAvailableReleases().thenApply(releases -> releases.stream()
				.filter(r -> r.tag().equals(versionTag))
				.findFirst().orElse(null));
	}

	public CompletableFuture<List<ClientVersionRelease>> getAvailableReleases() {
		if (loaded) {
			return CompletableFuture.completedFuture(Collections.unmodifiableList(availableReleases));
		}
		return fetchReleasesFromGitHub();
	}

	private CompletableFuture<List<ClientVersionRelease>> fetchReleasesFromGitHub() {
		if (activeReleaseFetchFuture != null) return activeReleaseFetchFuture;
		HttpRequest req = HttpRequest.newBuilder(URI.create(BASE_GITHUB_URL + "/releases"))
				.timeout(Duration.ofSeconds(3))
				.GET()
				.build();
		activeReleaseFetchFuture = httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofInputStream())
				.thenApplyAsync(resp -> {
					if (resp.statusCode() == 200) {
						JsonArray releasesArray = new Gson().fromJson(new InputStreamReader(resp.body()), JsonArray.class);
						availableReleases.clear();
						for (var element : releasesArray) {
							if (element.isJsonObject()) {
								JsonObject obj = element.getAsJsonObject();
								String tag = obj.get("tag_name").getAsString();
								String apiUrl = obj.get("url").getAsString();
								String assetsUrl = obj.get("assets_url").getAsString();
								OffsetDateTime publishedAt = OffsetDateTime.parse(obj.get("published_at").getAsString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
								LocalDateTime localPublishedAt = publishedAt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
								availableReleases.add(new ClientVersionRelease(tag, apiUrl, assetsUrl, localPublishedAt));
							}
						}
						availableReleases.sort(Comparator.comparing(ClientVersionRelease::publishedAt).reversed());
						loaded = true;
						return availableReleases;
					} else {
						throw new RuntimeException("Error while requesting releases.");
					}
				});
		return activeReleaseFetchFuture;
	}

	/**
	 * Gets the path to the executable JAR client file that can be used to run
	 * the game.
	 * @param versionTag A string that's either a version tag, such as v1.2.3,
	 *                   or a plain file name (without .jar suffix). If it's a
	 *                   version tag, we will try to find (or download) an
	 *                   official version with that tag. Otherwise, we just look
	 *                   for a jar file with a matching name.
	 * @param progressReporter A progress reporter to visualize the progress.
	 * @return A future that completes when the file is found and available.
	 */
	public CompletableFuture<Path> getVersionFile(String versionTag, ProgressReporter progressReporter) {
		// Check if we're trying to get a normal version.
		if (VERSION_REGEX.matcher(versionTag).matches()) {
			try (var s = Files.list(Launcher.VERSIONS_DIR)) {
				Optional<Path> optionalFile = s.filter(f -> isOfficialVersionFile(f) && versionTag.equals(extractVersion(f)))
						.findFirst();
				if (optionalFile.isPresent()) return CompletableFuture.completedFuture(optionalFile.get());
			} catch (IOException e) {
				return CompletableFuture.failedFuture(e);
			}
			progressReporter.enableProgress();
			progressReporter.setActionText("Downloading client " + versionTag + "...");
			var future = getRelease(versionTag)
					.thenComposeAsync(release -> downloadVersion(release, progressReporter));
			future.thenRun(progressReporter::disableProgress);
			return future;
		} else {
			// Otherwise, the user just wants to select an exact jar file.
			Optional<Path> optionalFile = FileUtils.getFirstExisting(
					Launcher.VERSIONS_DIR.resolve(versionTag),
					Launcher.VERSIONS_DIR.resolve(versionTag + ".jar")
			);
			return optionalFile.map(CompletableFuture::completedFuture)
					.orElseGet(() -> CompletableFuture.failedFuture(new IOException("Version " + versionTag + " not found.")));
		}

	}

	private CompletableFuture<Path> downloadVersion(ClientVersionRelease release, ProgressReporter progressReporter) {
		HttpRequest req = HttpRequest.newBuilder(URI.create(release.assetsUrl()))
				.GET().timeout(Duration.ofSeconds(3)).build();
		CompletableFuture<JsonObject> downloadUrlFuture = httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofInputStream())
			.thenApplyAsync(resp -> {
				if (resp.statusCode() == 200) {
					JsonArray assetsArray = new Gson().fromJson(new InputStreamReader(resp.body()), JsonArray.class);
					for (var asset : assetsArray) {
						JsonObject assetObj = asset.getAsJsonObject();
						String name = assetObj.get("name").getAsString();
						if (OFFICIAL_VERSION_REGEX.matcher(name).matches()) {
							return assetObj;
						}
					}
					throw new RuntimeException("Couldn't find a matching release asset for this system.");
				} else {
					throw new RuntimeException("Error while requesting release assets from GitHub: " + resp.statusCode());
				}
			});
		return downloadUrlFuture.thenComposeAsync(asset -> {
			String url = asset.get("browser_download_url").getAsString();
			String fileName = asset.get("name").getAsString();
			HttpRequest downloadRequest = HttpRequest.newBuilder(URI.create(url))
					.GET().timeout(Duration.ofMinutes(5)).build();
			Path file = Launcher.VERSIONS_DIR.resolve(fileName);
			return httpClient.sendAsync(downloadRequest, HttpResponse.BodyHandlers.ofInputStream())
				.thenApplyAsync(resp -> {
					if (resp.statusCode() == 200) {
						// Download sequentially, and update the progress.
						try {
							FileUtils.downloadWithProgress(file, resp, progressReporter);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
						return file;
					} else {
						throw new RuntimeException("Error while downloading release asset from GitHub: " + resp.statusCode());
					}
				});
		});
	}

	private boolean isVersionFile(Path p) {
		return Files.isRegularFile(p) && p.getFileName().toString().endsWith(".jar");
	}

	private boolean isOfficialVersionFile(Path p) {
		return isVersionFile(p) && OFFICIAL_VERSION_REGEX.matcher(p.getFileName().toString()).matches();
	}

	private String extractVersion(Path file) {
		Matcher matcher = VERSION_REGEX.matcher(file.getFileName().toString());
		if (matcher.find()) {
			return matcher.group();
		}
		throw new IllegalArgumentException("File doesn't contain a valid version pattern.");
	}
}
