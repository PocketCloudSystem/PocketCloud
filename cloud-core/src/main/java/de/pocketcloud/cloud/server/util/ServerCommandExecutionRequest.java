package de.pocketcloud.cloud.server.util;

import de.pocketcloud.common.concurrent.Promise;
import de.pocketcloud.network.packet.type.ServerCommandExecutionResult;

public record ServerCommandExecutionRequest(String id, Promise<ServerCommandExecutionResult> promise, Long time) {}