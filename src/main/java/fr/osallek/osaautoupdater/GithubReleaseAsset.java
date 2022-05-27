package fr.osallek.osaautoupdater;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubReleaseAsset(@JsonProperty("name") String name, @JsonProperty("browser_download_url") String url) {

    @Override
    public String name() {
        return name;
    }

    @Override
    public String url() {
        return url;
    }
}
