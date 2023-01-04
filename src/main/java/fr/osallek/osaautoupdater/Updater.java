package fr.osallek.osaautoupdater;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

@Component
public class Updater implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Updater.class);

    private static final String VERSION_FILE = "version.txt";

    private final OsaAutoUpdaterProperties properties;

    public Updater(OsaAutoUpdaterProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        LOGGER.info("Checking {} for updates!", this.properties.getRepoName());

        File currentExecutable = new File(this.properties.getExecutableName());

        String releasesUrl = "https://api.github.com/repos/Osallek/" + this.properties.getRepoName() + "/releases/latest";

        try {
            ResponseEntity<GithubRelease> response = new RestTemplate().getForEntity(releasesUrl, GithubRelease.class);

            if (!HttpStatus.OK.equals(response.getStatusCode())) {
                LOGGER.error("An error occurred while getting release from Github: {}", response.getStatusCode());
                runExecutable();
                return;
            }

            if (response.getBody() == null) {
                LOGGER.error("No body returned from Github!");
                runExecutable();
                return;
            }

            File versionFile = new File(VERSION_FILE);

            if (currentExecutable.exists() && versionFile.exists() && (!versionFile.canRead() || !versionFile.canWrite())) {
                LOGGER.warn("{} file exists but cannot be read! Ignoring update!", versionFile);
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
                DefaultArtifactVersion currentVersion = new DefaultArtifactVersion(version);
                DefaultArtifactVersion releaseVersion = new DefaultArtifactVersion(release.tagName());

                if (currentVersion.compareTo(releaseVersion) >= 0) {
                    LOGGER.info("No new version found!");
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
                LOGGER.info("Downloading: {}!", finalReleaseAsset.get().name());
                File file = new File("tmp_" + finalReleaseAsset.get().name());

                try (FileOutputStream outputStream = new FileOutputStream(file)) {
                    byte[] buffer = new byte[1_000_000];
                    int length;
                    while ((length = clientHttpResponse.getBody().read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                }

                LOGGER.info("Downloaded: {}!", finalReleaseAsset.get().name());
                return file;
            });

            if (newExecutable == null) {
                LOGGER.error("Could not download {}!", releaseAsset.get().name());
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

            LOGGER.info("Downloaded new version {}!", newVersion);

            runExecutable();
        } catch (Exception e) {
            runExecutable();
        }
    }

    private void runExecutable() throws IOException {
        File executableFile = new File(this.properties.getExecutableName());

        if (!executableFile.exists() || !executableFile.canExecute()) {
            LOGGER.error("{} does not exists or is not executable!", this.properties.getExecutableName());
            return;
        }

        Process process;

        if (this.properties.getExecutableName().endsWith(".jar")) {
            process = new ProcessBuilder("java", "-jar", this.properties.getExecutableName()).start();
        } else {
            process = new ProcessBuilder(this.properties.getExecutableName()).start();
        }

        if (!process.isAlive()) {
            LOGGER.error("{} does not seems to have started!", this.properties.getExecutableName());
        } else {
            LOGGER.info("{} started!", this.properties.getExecutableName());
        }

        LOGGER.info("Closing Auto updater!");
    }
}
