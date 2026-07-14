package de.pocketcloud.api.model.software;

public interface ISoftwareBridge {

    String url();

    String relativeServerPath();

    boolean checkForUpdates();

    String fileName();
}