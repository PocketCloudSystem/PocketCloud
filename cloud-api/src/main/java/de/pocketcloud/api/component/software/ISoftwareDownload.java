package de.pocketcloud.api.component.software;

public interface ISoftwareDownload {

    String url();

    String filename();

    String startCommand();

    boolean checkForUpdates();
}