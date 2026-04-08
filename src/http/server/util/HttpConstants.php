<?php

namespace pocketcloud\cloud\http\server\util;

final class HttpConstants {

    public const int MAX_REQUEST_SIZE = (1024 * 1024) * 10;
    public const int CHUNK_SIZE = 8192;
    public const int MAX_HEADERS = 100;

    public const string GET = "GET";
    public const string POST = "POST";
    public const string PUT = "PUT";
    public const string DELETE = "DELETE";
    public const string PATCH = "PATCH";

    public const array SUPPORTED_REQUEST_METHODS = ["GET", "POST", "PUT", "DELETE", "PATCH"];
}