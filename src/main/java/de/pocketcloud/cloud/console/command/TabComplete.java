package de.pocketcloud.cloud.console.command;

import java.util.Collection;
import java.util.List;

public interface TabComplete {

    Collection<? extends String> onTabComplete(List<String> args);
}