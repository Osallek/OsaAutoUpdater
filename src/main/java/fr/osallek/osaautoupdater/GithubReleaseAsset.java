package fr.osallek.osaautoupdater;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GithubReleaseAsset {

    @JsonProperty("name")
    private String name;

    @JsonProperty("browser_download_url")
    private String url;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
