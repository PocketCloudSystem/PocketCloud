package de.pocketcloud.cloud.cluster.storage;

import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.common.util.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;

/**
 * {@link TemplateStorage} on the local file system ({@code templates/}, {@code groups/}).
 * Same layout and behaviour as before the abstraction existed.
 */
public final class LocalTemplateStorage implements TemplateStorage {

    public static final String LOG_ARCHIVE = "cloud_log_archive";

    @Override
    public String name() {
        return "local";
    }

    @Override
    public void init() {
        for (TemplateType type : TemplateType.values()) FileUtils.createDir(globalPath(type));
    }

    @Override
    public void createTemplate(String template) throws IOException {
        Files.createDirectories(localTemplatePath(template));
    }

    @Override
    public void deleteTemplate(String template) {
        Path path = localTemplatePath(template);
        if (Files.isDirectory(path)) FileUtils.removeDirectory(path);
    }

    @Override
    public boolean templateExists(String template) {
        return Files.isDirectory(localTemplatePath(template));
    }

    @Override
    public void createGroup(String group) throws IOException {
        Files.createDirectories(groupPath(group));
    }

    @Override
    public void deleteGroup(String group) {
        Path path = groupPath(group);
        if (Files.isDirectory(path)) FileUtils.removeDirectory(path);
    }

    @Override
    public void copyGlobalTo(TemplateType type, Path target) throws IOException {
        if (pausedSnapshot("templates/global/" + type.name().toLowerCase(), target, Set.of())) return;
        TemplateLocks.read(globalPath(type), () -> FileUtils.copyDirectory(globalPath(type), target, Set.of()));
    }

    @Override
    public void copyGroupTo(String group, Path target) throws IOException {
        if (pausedSnapshot("groups/" + group, target, Set.of())) return;
        TemplateLocks.read(groupPath(group), () -> FileUtils.copyDirectory(groupPath(group), target, Set.of()));
    }

    @Override
    public void copyTemplateTo(String template, Path target, Set<String> excludes) throws IOException {
        if (pausedSnapshot("templates/" + template, target, excludes)) return;
        TemplateLocks.read(localTemplatePath(template), () -> FileUtils.copyDirectory(localTemplatePath(template), target, excludes));
    }

    /** While the template sync of the folder is paused on this node, servers get the last distributed version. */
    private static boolean pausedSnapshot(String unit, Path target, Set<String> excludes) throws IOException {
        var replicator = de.pocketcloud.cloud.cluster.sync.TemplateReplicator.get();
        return replicator.isPresent() && replicator.get().copyPausedSnapshot(new de.pocketcloud.cloud.cluster.sync.SyncUnit(unit), target, excludes);
    }

    @Override
    public void storeInTemplate(String template, String relativePath, Path source) throws IOException {
        Path dst = localTemplatePath(template).resolve(relativePath);
        TemplateLocks.<IOException>read(localTemplatePath(template), () -> {
            if (Files.isRegularFile(source)) {
                Files.copy(source, dst, StandardCopyOption.REPLACE_EXISTING);
            } else if (Files.isDirectory(source)) {
                FileUtils.copyDirectory(source, dst, Set.of());
            }
        });
    }

    @Override
    public void archiveLog(String template, String archiveFileName, Path source) throws IOException {
        Path archive = localTemplatePath(template).resolve(LOG_ARCHIVE);
        FileUtils.createDir(archive);
        Files.copy(source, archive.resolve(archiveFileName), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public Path localTemplatePath(String template) {
        return PocketCloudPaths.templates().with(template).asPath();
    }

    private static Path groupPath(String group) {
        return PocketCloudPaths.groups().with(group).asPath();
    }

    private static Path globalPath(TemplateType type) {
        return PocketCloudPaths.templates().global().with(type.name().toLowerCase()).asPath();
    }
}
