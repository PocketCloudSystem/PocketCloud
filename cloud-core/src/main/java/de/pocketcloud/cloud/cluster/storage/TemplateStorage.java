package de.pocketcloud.cloud.cluster.storage;

import de.pocketcloud.api.template.TemplateType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * Where the files of templates, server groups and global templates live.
 * <p>
 * Every node keeps a full local copy (see cluster spec), so {@link LocalTemplateStorage} is what
 * runs on each node. Replication between nodes happens on top of this interface. It also leaves
 * room for other backends (e.g. S3) later without touching the server code.
 */
public interface TemplateStorage {

    String name();

    /** Creates the folders for the global templates. */
    void init() throws IOException;

    void createTemplate(String template) throws IOException;

    void deleteTemplate(String template) throws IOException;

    boolean templateExists(String template);

    void createGroup(String group) throws IOException;

    void deleteGroup(String group) throws IOException;

    /** Copies the global template of a type into a server directory. */
    void copyGlobalTo(TemplateType type, Path target) throws IOException;

    /** Copies the files of a server group into a server directory. */
    void copyGroupTo(String group, Path target) throws IOException;

    /**
     * Copies the files of a template into a server directory.
     *
     * @param excludes relative paths to skip (directories end with {@code /})
     */
    void copyTemplateTo(String template, Path target, Set<String> excludes) throws IOException;

    /**
     * Stores a file or directory from a server back into its template (used by saves).
     *
     * @param relativePath path inside the template, e.g. {@code worlds/}
     */
    void storeInTemplate(String template, String relativePath, Path source) throws IOException;

    /** Archives a server log file in the template's {@code cloud_log_archive}. */
    void archiveLog(String template, String archiveFileName, Path source) throws IOException;

    /**
     * Local path of a template, for code that has to work on the files directly
     * (e.g. renewing server properties). Remote backends return their local cache.
     */
    Path localTemplatePath(String template);
}
