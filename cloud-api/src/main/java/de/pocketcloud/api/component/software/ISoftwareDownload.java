package de.pocketcloud.api.component.software;

public interface ISoftwareDownload {

    String url();

    String filename();

    String[] startCommand();

    default String realStartCommand() {
        return String.join(" ", startCommand());
    }

    boolean checkForUpdates();
}