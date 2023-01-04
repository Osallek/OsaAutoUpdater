package fr.osallek.osaautoupdater;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RegisterReflectionForBinding({GithubRelease.class, GithubReleaseAsset.class})
public class OsaAutoUpdaterApplication {

    public static void main(String[] args) {
        SpringApplication.run(OsaAutoUpdaterApplication.class, args);
    }
}
