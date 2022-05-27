package fr.osallek.osaautoupdater;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GithubRelease(@JsonProperty("tag_name") String tagName, @JsonProperty("assets") List<GithubReleaseAsset> assets) {

    @Override
    public String tagName() {
        return tagName;
    }

    @Override
    public List<GithubReleaseAsset> assets() {
        return assets;
    }
}
