package fr.osallek.osaautoupdater;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@SpringBootApplication
public class OsaAutoUpdaterApplication implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(OsaAutoUpdaterApplication.class);

    private static final String VERSION_FILE = "version.txt";

    private final OsaAutoUpdaterProperties properties;

    public OsaAutoUpdaterApplication(OsaAutoUpdaterProperties properties) {
        this.properties = properties;
    }

    public static void main(String[] args) {
        SpringApplication.run(OsaAutoUpdaterApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        LOGGER.info("Checking {} for updates!", this.properties.getJarName());

        File currentJar = new File(this.properties.getJarName() + ".jar");

        String releasesUrl = "https://api.github.com/repos/Osallek/" + this.properties.getRepoName() + "/releases";

        try {
            ResponseEntity<List<GithubRelease>> response = new RestTemplate().exchange(releasesUrl, HttpMethod.GET, null,
                                                                                       new ParameterizedTypeReference<>() {});

            if (!HttpStatus.OK.equals(response.getStatusCode())) {
                LOGGER.error("An error occurred while getting releases from Github: {}", response.getStatusCode());
                runJar();
                return;
            }

            if (CollectionUtils.isEmpty(response.getBody())) {
                LOGGER.warn("No release found for {}", this.properties.getJarName());
                runJar();
                return;
            }

            response.getBody().sort(Comparator.comparing(GithubRelease::getTagName).reversed());

            File versionFile = new File(VERSION_FILE);

            if (currentJar.exists() && versionFile.exists() && (!versionFile.canRead() || !versionFile.canWrite())) {
                LOGGER.warn("{} file exists but cannot be read! Ignoring update!", versionFile);
                runJar();
                return;
            }

            String version = null;

            if (versionFile.exists()) {
                List<String> lines = Files.readAllLines(versionFile.toPath());

                if (!CollectionUtils.isEmpty(lines)) {
                    version = lines.get(0);
                }
            }

            GithubRelease release = response.getBody().get(0); //Get the newest

            if (currentJar.exists() && version != null) {
                DefaultArtifactVersion currentVersion = new DefaultArtifactVersion(version);
                DefaultArtifactVersion releaseVersion = new DefaultArtifactVersion(release.getTagName());

                if (currentVersion.compareTo(releaseVersion) >= 0) {
                    LOGGER.info("No new version found!");
                    runJar();
                    return;
                }
            }

            Optional<GithubReleaseAsset> releaseAsset = Optional.empty();

            if (!CollectionUtils.isEmpty(release.getAssets())) {
                releaseAsset = release.getAssets()
                                      .stream()
                                      .filter(asset -> asset.getName().matches(this.properties.getJarName() + ".*.jar"))
                                      .findFirst();
            }

            if (releaseAsset.isEmpty()) {
                LOGGER.info("Not asset");
                //No valid asset
                runJar();
                return;
            }

            //Download
            Optional<GithubReleaseAsset> finalReleaseAsset = releaseAsset;
            File newJar = new RestTemplate().execute(releaseAsset.get().getUrl(), HttpMethod.GET, null, clientHttpResponse -> {
                LOGGER.info("Downloading: {}!", finalReleaseAsset.get().getName());
                File file = new File(finalReleaseAsset.get().getName());

                try (FileOutputStream outputStream = new FileOutputStream(file)) {
                    StreamUtils.copy(clientHttpResponse.getBody(), outputStream);
                }

                LOGGER.info("Downloaded: {}!", finalReleaseAsset.get().getName());
                return file;
            });

            if (newJar == null) {
                LOGGER.error("Could not download {}!", releaseAsset.get().getName());
                runJar();
                return;
            }

            try {
                Files.deleteIfExists(currentJar.toPath());
            } catch (IOException e) {
                LOGGER.error("Could not delete {} because: {}", currentJar, e.getMessage());
                runJar();
                return;
            }

            if (!newJar.renameTo(currentJar)) {
                LOGGER.error("Could not rename {} to {}", newJar, currentJar);
                runJar();
                return;
            }

            String newVersion = releaseAsset.get().getName().replace(this.properties.getJarName(), "").replace(".jar", "").substring(1);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(versionFile))) {
                writer.write(newVersion);
            } catch (IOException e) {
                LOGGER.error("Could not write version to {} because: {}", versionFile, e.getMessage());
                runJar();
                return;
            }

            LOGGER.info("Downloaded new version {}!", newVersion);

            runJar();
        } catch (Exception e) {
            runJar();
        }
    }

    private void runJar() throws IOException {
        File jarFile = new File(this.properties.getJarName() + ".jar");

        if (!jarFile.exists() || !jarFile.canExecute()) {
            LOGGER.error("{} does not exists or is not executable!", this.properties.getJarName());
            return;
        }

        Process process = new ProcessBuilder("java", "-jar", this.properties.getJarName() + ".jar").start();

        if (!process.isAlive()) {
            LOGGER.error("{} does not seems to have started!", this.properties.getJarName());
        } else {
            LOGGER.info("{} started!", this.properties.getJarName());
        }

        LOGGER.info("Closing Auto updater!");
    }
}
