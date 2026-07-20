package de.pocketcloud.api.component.software;

public interface ISoftwareBridge {

    String url();

    String relativeServerPath();

    boolean checkForUpdates();

    String fileName();
}