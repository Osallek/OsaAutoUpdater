package fr.osallek.osaautoupdater;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "updater")
public class OsaAutoUpdaterProperties {

    private String executableName;

    private String repoName;

    private String javaVersion;

    public String getExecutableName() {
        return executableName;
    }

    public void setExecutableName(String executableName) {
        this.executableName = executableName;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }
}
