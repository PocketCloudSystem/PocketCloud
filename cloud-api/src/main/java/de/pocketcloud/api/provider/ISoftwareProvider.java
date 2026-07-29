package de.pocketcloud.api.provider;

import de.pocketcloud.api.component.software.IServerSoftware;

import java.util.Collection;
import java.util.Optional;

public interface ISoftwareProvider {

    boolean check(String name);

    Optional<IServerSoftware> get(String name);

    Collection<IServerSoftware> getAll();
}