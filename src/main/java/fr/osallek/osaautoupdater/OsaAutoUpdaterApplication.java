package fr.osallek.osaautoupdater;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RegisterReflectionForBinding({GithubRelease.class, GithubReleaseAsset.class})
public class OsaAutoUpdaterApplication {

    public static void main(String[] args) {
        System.setProperty("OSALLEK_DOCUMENTS", UpdateUtils.getDocumentsPath().toString());
        SpringApplication.run(OsaAutoUpdaterApplication.class, args);
    }
}
