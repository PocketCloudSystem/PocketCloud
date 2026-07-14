package de.pocketcloud.api.model.group;

import java.util.Collection;

public interface IServerGroup {

    String name();

    Collection<String> templates();
}