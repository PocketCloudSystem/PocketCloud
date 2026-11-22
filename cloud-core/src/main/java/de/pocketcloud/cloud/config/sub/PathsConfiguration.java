package de.pocketcloud.cloud.config.sub;

import de.pocketcloud.api.config.ICloudConfig;
import de.pocketcloud.api.logging.CloudLogLevel;
import de.pocketcloud.cloud.PocketCloud;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Exclude;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Custom locations of some storage folders. Relative paths are resolved against the cloud folder,
 * absolute paths (e.g. {@code /srv/pocketcloud/software/}) can point anywhere, e.g. to a folder shared by
 * several clouds. Changes need a restart.
 */
@Getter
@Setter
@Accessors(fluent = true)
public final class PathsConfiguration extends OkaeriConfig implements ICloudConfig {

    @Exclude
    public static final String DEFAULT_SOFTWARE = "storage/software/";
    @Exclude
    public static final String DEFAULT_IN_GAME = "storage/inGame/";
    @Exclude
    public static final String DEFAULT_LIBRARIES = "storage/libraries/";
    @Exclude
    public static final String DEFAULT_PLUGINS = "storage/plugins/";

    @Comment({"Folder of the server software (jars, binaries, default configs, bridge)."})
    private String software = DEFAULT_SOFTWARE;

    @Comment({"Folder of the in-game files (languages, whitelist, notification list)."})
    private String inGame = DEFAULT_IN_GAME;

    @Comment({"Folder of the libraries."})
    private String libraries = DEFAULT_LIBRARIES;

    @Comment({"Folder of the cloud plugins."})
    private String plugins = DEFAULT_PLUGINS;

    @Override
    public void validate() {
        software = orDefault(software, DEFAULT_SOFTWARE, "software");
        inGame = orDefault(inGame, DEFAULT_IN_GAME, "inGame");
        libraries = orDefault(libraries, DEFAULT_LIBRARIES, "libraries");
        plugins = orDefault(plugins, DEFAULT_PLUGINS, "plugins");
    }

    private static String orDefault(String value, String fallback, String key) {
        if (value != null && !value.isBlank()) return value.trim();
        PocketCloud.instance().appendStartNotification("Empty path for paths." + key + " in config.yml, reset to default... (" + fallback + ")", CloudLogLevel.WARN);
        return fallback;
    }
}
