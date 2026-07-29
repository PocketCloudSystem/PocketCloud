package de.pocketcloud.api.network.traffic;

import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.List;

@Accessors(fluent = true)
public record TrafficDirection(String name) {

    public static final TrafficDirection IN = new TrafficDirection("IN");
    public static final TrafficDirection OUT = new TrafficDirection("OUT");

    public static List<TrafficDirection> values() {
        return Arrays.asList(IN, OUT);
    }
}