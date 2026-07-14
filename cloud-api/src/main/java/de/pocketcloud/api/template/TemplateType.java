package de.pocketcloud.api.template;

public enum TemplateType {

    SERVER,
    PROXY;

    public boolean isProxy() {
        return this == PROXY;
    }

    public boolean isServer() {
        return this == SERVER;
    }
}