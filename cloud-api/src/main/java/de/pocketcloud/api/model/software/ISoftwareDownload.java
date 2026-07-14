package de.pocketcloud.api.model.software;

public interface ISoftwareDownload {

    String url();

    String filename();

    String startCommand();

    boolean checkForUpdates();
}