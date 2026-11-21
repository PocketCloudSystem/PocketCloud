package de.pocketcloud.api.cluster;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * The nodes of the cloud ({@code CloudAPI.instance().nodes()}).
 */
public interface INodeProvider {

    /** Used when nothing is known about the nodes (e.g. an older cloud that does not send them). */
    INodeProvider EMPTY = new INodeProvider() {
        @Override
        public Collection<INode> getAll() {
            return List.of();
        }

        @Override
        public Optional<INode> get(String name) {
            return Optional.empty();
        }
    };

    Collection<INode> getAll();

    Optional<INode> get(String name);

    default Optional<INode> head() {
        return getAll().stream().filter(INode::head).findFirst();
    }
}
