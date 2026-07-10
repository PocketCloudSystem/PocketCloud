package de.pocketcloud.cloud.server.util;

import de.pocketcloud.network.packet.type.ServerCommandExecutionResult;
import de.pocketcloud.cloud.util.concurrent.Promise;

public record ServerCommandExecutionRequest(String id, Promise<ServerCommandExecutionResult> promise, Long time) {}