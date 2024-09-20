package fr.osallek.osaautoupdater;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class Updater implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Updater.class);

    private static final String VERSION_FILE = "version.txt";

    private static final Pattern JAVA_VERSION_PATTERN = Pattern.compile("\"(.*?)\"");

    private static final String JAVA_DOWNLOAD_URL = "https://download.oracle.com/java/21/archive/jdk-21.0.3_windows-x64_bin.zip";

    private final OsaAutoUpdaterProperties properties;

    private Optional<Path> javaFolder = Optional.empty();

    public Updater(OsaAutoUpdaterProperties properties) {
        this.properties = properties;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            if (this.properties.getJavaVersion() != null) {
                LOGGER.info("Checking for Java {}", this.properties.getJavaVersion());

                boolean hasGlobalJava = checkJavaVersion(null);

                if (!hasGlobalJava) {
                    LOGGER.warn("Could not get right Java version from global");
                    Path osallekDocumentsFolder = UpdateUtils.getDocumentsPath();
                    this.javaFolder = Optional.of(osallekDocumentsFolder.resolve("java"));

                    if (!checkJavaVersion(this.javaFolder.orElse(null))) {
                        FileSystemUtils.deleteRecursively(this.javaFolder.get());

                        Path javaZip = new RestTemplate().execute(JAVA_DOWNLOAD_URL, HttpMethod.GET, null, clientHttpResponse -> {
                            Path path = osallekDocumentsFolder.resolve("java_tmp.zip");
                            LOGGER.info("Downloading Java to {}", path);
                            UpdateUtils.copyToFile(clientHttpResponse.getBody(), path.toFile());

                            LOGGER.info("Downloaded Java");
                            return path;
                        });

                        UpdateUtils.unzip(javaZip, osallekDocumentsFolder);
                        Files.move(osallekDocumentsFolder.resolve("jdk-21.0.3"), this.javaFolder.get(), StandardCopyOption.REPLACE_EXISTING,
                                   StandardCopyOption.ATOMIC_MOVE);
                        Files.deleteIfExists(javaZip);
                    }
                }
            } else {
                LOGGER.error("Could not check Java version");
                return;
            }

            LOGGER.info("Checking {} for updates", this.properties.getRepoName());

            File currentExecutable = new File(this.properties.getExecutableName());

            String releasesUrl = "https://api.github.com/repos/Osallek/" + this.properties.getRepoName() + "/releases/latest";

            ResponseEntity<GithubRelease> response = new RestTemplate().getForEntity(releasesUrl, GithubRelease.class);

            if (!HttpStatus.OK.equals(response.getStatusCode())) {
                LOGGER.error("An error occurred while getting release from Github: {}", response.getStatusCode());
                runExecutable();
                return;
            }

            if (response.getBody() == null) {
                LOGGER.error("No body returned from Github");
                runExecutable();
                return;
            }

            File versionFile = new File(VERSION_FILE);

            if (currentExecutable.exists() && versionFile.exists() && (!versionFile.canRead() || !versionFile.canWrite())) {
                LOGGER.warn("{} file exists but cannot be read! Ignoring update", versionFile);
                runExecutable();
                return;
            }

            String version = null;

            if (versionFile.exists()) {
                List<String> lines = Files.readAllLines(versionFile.toPath());

                if (!CollectionUtils.isEmpty(lines)) {
                    version = lines.get(0);
                }
            }

            GithubRelease release = response.getBody();

            if (currentExecutable.exists() && version != null) {
                ComparableVersion currentVersion = new ComparableVersion(version);
                ComparableVersion releaseVersion = new ComparableVersion(release.tagName());

                if (currentVersion.compareTo(releaseVersion) >= 0) {
                    LOGGER.info("No new version found");
                    runExecutable();
                    return;
                }
            }

            Optional<GithubReleaseAsset> releaseAsset = Optional.empty();

            if (!CollectionUtils.isEmpty(release.assets())) {
                releaseAsset = release.assets()
                                      .stream()
                                      .filter(asset -> this.properties.getExecutableName().equalsIgnoreCase(asset.name()))
                                      .findFirst();
            }

            if (releaseAsset.isEmpty()) {
                LOGGER.info("Not asset");
                //No valid asset
                runExecutable();
                return;
            }

            //Download
            Optional<GithubReleaseAsset> finalReleaseAsset = releaseAsset;
            File newExecutable = new RestTemplate().execute(releaseAsset.get().url(), HttpMethod.GET, null, clientHttpResponse -> {
                LOGGER.info("Downloading: {}", finalReleaseAsset.get().name());
                File file = new File("tmp_" + finalReleaseAsset.get().name());
                UpdateUtils.copyToFile(clientHttpResponse.getBody(), file);

                LOGGER.info("Downloaded: {}", finalReleaseAsset.get().name());
                return file;
            });

            if (newExecutable == null) {
                LOGGER.error("Could not download {}", releaseAsset.get().name());
                runExecutable();
                return;
            }

            try {
                Files.deleteIfExists(currentExecutable.toPath());
            } catch (IOException e) {
                LOGGER.error("Could not delete {} because: {}", currentExecutable, e.getMessage());
                runExecutable();
                return;
            }

            if (!newExecutable.renameTo(currentExecutable)) {
                LOGGER.error("Could not rename {} to {}", newExecutable, currentExecutable);
                runExecutable();
                return;
            }

            String newVersion = release.tagName();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(versionFile))) {
                writer.write(newVersion);
            } catch (IOException e) {
                LOGGER.error("Could not write version to {} because: {}", versionFile, e.getMessage());
                runExecutable();
                return;
            }

            LOGGER.info("Downloaded new version {}", newVersion);

            runExecutable();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);

            try {
                runExecutable();
            } catch (IOException ex) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    private void runExecutable() throws IOException {
        File executableFile = new File(this.properties.getExecutableName());

        if (!executableFile.exists() || !executableFile.canExecute()) {
            LOGGER.error("{} does not exists or is not executable", this.properties.getExecutableName());
            return;
        }

        Process process;

        if (this.properties.getExecutableName().endsWith(".jar")) {
            process = new ProcessBuilder(
                    this.javaFolder.map(path -> path.resolve("bin"))
                                   .filter(Files::exists)
                                   .map(Path::toAbsolutePath)
                                   .map(Path::toString)
                                   .map(s -> s + "\\")
                                   .orElse("") + "java",
                    "-jar", this.properties.getExecutableName()).start();
        } else {
            process = new ProcessBuilder(this.properties.getExecutableName()).start();
        }

        if (!process.isAlive()) {
            LOGGER.error("{} does not seems to have started", this.properties.getExecutableName());
        } else {
            LOGGER.info("{} started", this.properties.getExecutableName());
        }

        LOGGER.info("Closing Auto updater");
    }

    private boolean checkJavaVersion(Path path) throws InterruptedException {
        try {
            Process process = new ProcessBuilder(
                    Optional.ofNullable(path)
                            .map(p -> p.resolve("bin"))
                            .filter(Files::exists)
                            .map(Path::toAbsolutePath)
                            .map(Path::toString)
                            .map(s -> s + "\\")
                            .orElse("") + "java",
                    "-version").start();

            if (!process.isAlive()) {
                LOGGER.error("Could not check Java version");
                return false;
            } else {
                process.waitFor();

                String text = StringUtils.firstNonBlank(new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8),
                                                        new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));

                if (StringUtils.isNotBlank(text)) {
                    Matcher matcher = JAVA_VERSION_PATTERN.matcher(text);

                    if (matcher.find()) {
                        String version = matcher.group().replace("\"", "");
                        ComparableVersion foundVersion = new ComparableVersion(version);
                        ComparableVersion neededVersion = new ComparableVersion(this.properties.getJavaVersion());

                        if (foundVersion.compareTo(neededVersion) < 0) {
                            LOGGER.error("Found Java version {} in {}, need at least {}", version, Optional.ofNullable(path).map(Path::toString).orElse("Global"), this.properties.getJavaVersion());
                        } else {
                            LOGGER.info("Found Java version: {} in {}", version, Optional.ofNullable(path).map(Path::toString).orElse("Global"));
                            return true;
                        }
                    }
                }
            }

            return false;
        } catch (IOException e) {
            LOGGER.error("Could not check Java version");
            return false;
        }
    }
}
