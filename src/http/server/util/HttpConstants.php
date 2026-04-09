<?php

namespace pocketcloud\cloud\http\server\util;

final class HttpConstants {

    public const int MAX_REQUEST_SIZE = (1024 * 1024) * 10;
    public const int CHUNK_SIZE = 8192;
    public const int MAX_HEADERS = 100;

    public const array SUPPORTED_REQUEST_METHODS = ["GET", "POST", "PUT", "DELETE", "PATCH"];
}