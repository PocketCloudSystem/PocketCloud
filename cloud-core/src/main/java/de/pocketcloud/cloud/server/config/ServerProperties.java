package de.pocketcloud.cloud.server.config;

import de.pocketcloud.cloud.server.config.overlay.PropertiesValueOverlay;
import de.pocketcloud.cloud.server.config.overlay.YamlNodeOverlay;
import de.pocketcloud.cloud.server.config.repo.RemoteConfigRepository;
import de.pocketcloud.common.util.FileUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public abstract class ServerProperties implements IServerProperties {

    @Override
    public final Map<String, Object> getDefaultContent() {
        RemoteConfigRepository.RemoteConfig remote = fetchRemoteConfig();
        return remote != null ? parseYaml(remote.rawContent()) : new LinkedHashMap<>();
    }

    private RemoteConfigRepository.RemoteConfig fetchRemoteConfig() {
        return RemoteConfigRepository.get(getServerSoftware().name(), getFileName());
    }

    private Map<String, Object> parseYaml(String content) {
        Yaml yaml = new Yaml();
        Map<String, Object> parsed = yaml.load(content);
        return parsed != null ? new LinkedHashMap<>(parsed) : new LinkedHashMap<>();
    }

    protected boolean modifyYaml(String filePath, Map<String, Object> updatedContent) {
        try {
            Map<String, Object> content = loadYaml(filePath, getDefaultContent());
            content.putAll(updatedContent);
            return saveYaml(filePath, content);
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean renewYaml(String filePath) {
        try {
            RemoteConfigRepository.RemoteConfig remote = fetchRemoteConfig();
            if (remote == null) return false;

            Path path = Path.of(filePath);
            String finalContent = Files.exists(path)
                    ? YamlNodeOverlay.overlay(remote.rawContent(), Files.readString(path))
                    : remote.rawContent();

            Files.writeString(path, finalContent);
            writeLocalVersion(filePath, remote.version());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean needsRenewalYaml(String filePath) {
        if (!Files.exists(Path.of(filePath))) return true;

        RemoteConfigRepository.RemoteConfig remote = fetchRemoteConfig();
        if (remote == null) return false;

        String localVersion = readLocalVersion(filePath);
        return localVersion == null || !localVersion.equals(remote.version());
    }

    protected boolean modifyProperties(String filePath, Map<String, Object> updatedContent) {
        try {
            Properties props = loadProperties(filePath);
            updatedContent.forEach((k, v) -> props.setProperty(k, String.valueOf(v)));
            return saveProperties(filePath, props);
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean renewProperties(String filePath) {
        try {
            RemoteConfigRepository.RemoteConfig remote = fetchRemoteConfig();
            if (remote == null) return false;

            Path path = Path.of(filePath);
            String finalContent = Files.exists(path)
                    ? PropertiesValueOverlay.overlay(remote.rawContent(), Files.readString(path))
                    : remote.rawContent();

            Files.writeString(path, finalContent);
            writeLocalVersion(filePath, remote.version());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean needsRenewalProperties(String filePath) {
        if (!Files.exists(Path.of(filePath))) return true;

        RemoteConfigRepository.RemoteConfig remote = fetchRemoteConfig();
        if (remote == null) return false;

        String localVersion = readLocalVersion(filePath);
        return localVersion == null || !localVersion.equals(remote.version());
    }

    private Path versionFilePath(String filePath) {
        Path path = Path.of(filePath);
        return path.resolveSibling("." + path.getFileName() + ".version");
    }

    private String readLocalVersion(String filePath) {
        Path versionPath = versionFilePath(filePath);
        if (!Files.exists(versionPath)) return null;
        try {
            return Files.readString(versionPath).strip();
        } catch (IOException e) {
            return null;
        }
    }

    private void writeLocalVersion(String filePath, String version) {
        try {
            Files.writeString(versionFilePath(filePath), version);
        } catch (IOException ignored) {}
    }

    private Map<String, Object> loadYaml(String filePath, Map<String, Object> defaultContent) {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) return new LinkedHashMap<>(defaultContent);
        Map<String, Object> loaded = FileUtils.parseYamlFile(path);
        return loaded != null ? loaded : new LinkedHashMap<>();
    }

    private boolean saveYaml(String filePath, Map<String, Object> content) {
        return FileUtils.emitYamlFile(Path.of(filePath), content);
    }

    private Properties loadProperties(String filePath) throws IOException {
        Properties props = new Properties();
        Path path = Path.of(filePath);
        if (Files.exists(path)) {
            List<String> lines = Files.readAllLines(path);
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                if (!line.startsWith("#")) sb.append(line).append("\n");
            }
            props.load(new StringReader(sb.toString()));
        }
        return props;
    }

    private boolean saveProperties(String filePath, Properties props) {
        try {
            StringBuilder sb = new StringBuilder();
            props.forEach((k, v) -> sb.append(k).append("=").append(v).append("\n"));
            Files.writeString(Path.of(filePath), sb.toString());
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}