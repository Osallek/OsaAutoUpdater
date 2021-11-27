package fr.osallek.osaautoupdater;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class GithubRelease {

    @JsonProperty("tag_name")
    private String tagName;

    @JsonProperty("assets")
    private List<GithubReleaseAsset> assets;

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public List<GithubReleaseAsset> getAssets() {
        return assets;
    }

    public void setAssets(List<GithubReleaseAsset> assets) {
        this.assets = assets;
    }
}
