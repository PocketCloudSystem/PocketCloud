package de.pocketcloud.cloud.cluster.sync;

/**
 * A version of a replicated unit: number, manifest and whether the unit was deleted (tombstone).
 * On the head this is the valid version; on other nodes the version last applied from the head.
 */
public record UnitVersion(long version, Manifest manifest, boolean deleted) {

    public String hash() {
        return deleted ? "deleted" : manifest.hash();
    }
}
