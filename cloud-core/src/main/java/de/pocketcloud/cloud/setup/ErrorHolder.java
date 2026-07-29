package de.pocketcloud.cloud.setup;

public final class ErrorHolder {

    private String error = null;

    public void set(String error) {
        this.error = error;
    }

    public boolean isPresent() {
        return error != null;
    }

    public String get() {
        return error;
    }
}