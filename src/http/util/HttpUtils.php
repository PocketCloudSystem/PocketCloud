<?php

namespace pocketcloud\cloud\http\util;

use pocketcloud\cloud\http\server\HttpServer;
use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\util\HttpConstants;
use pocketcloud\cloud\http\server\util\StatusCode;
use pocketcloud\cloud\util\net\Address;

final class HttpUtils {

    public static function parseHttpRequest(Address $address, string $buffer): StatusCode|Request {
        if (strlen($buffer) > HttpConstants::MAX_REQUEST_SIZE) return StatusCode::PAYLOAD_TOO_LARGE;

        $lines = explode("\r\n", $buffer);
        if (empty($lines)) return StatusCode::BAD_REQUEST;

        $requestLine = array_shift($lines);
        $parts = explode(" ", trim($requestLine), 3);
        if (count($parts) !== 3) return StatusCode::BAD_REQUEST;

        [$method, $fullPath, $protocol] = $parts;

        if (($method = RequestMethod::fromName($method)) === null) return StatusCode::METHOD_NOT_ALLOWED;
        if (!preg_match("/^HTTP\/1\.[01]$/", $protocol)) return StatusCode::HTTP_VERSION_NOT_SUPPORTED;

        $fullPath = "/" . trim($fullPath, "/");

        if (str_contains($fullPath, "\0") || str_contains($fullPath, "..")) {
            return StatusCode::BAD_REQUEST;
        }

        if (!preg_match("/^\/[a-zA-Z0-9\/_\-.~!$&\"()*+,;=:@%?#\[\]]*$/", $fullPath)) {
            return StatusCode::BAD_REQUEST;
        }

        $headers = [];
        $body = "";
        $isBody = false;
        $queryParams = [];
        $headerCount = 0;

        if (str_contains($fullPath, "?")) {
            [$path, $queryString] = explode("?", $fullPath, 2);
            parse_str($queryString, $queryParams);
        } else {
            $path = $fullPath;
        }

        foreach ($lines as $line) {
            if (trim($line) === "") {
                $isBody = true;
                continue;
            }

            if ($isBody) {
                $body .= $line . "\r\n";
            } else {
                if (!str_contains($line, ": ")) continue;

                $parts = explode(": ", $line, 2);
                if (count($parts) !== 2) continue;

                [$headerName, $headerValue] = $parts;
                if (!preg_match("/^[a-zA-Z0-9\-]+$/", $headerName)) continue;

                $headerValue = str_replace(["\r", "\n"], "", trim($headerValue));
                if (++$headerCount > HttpConstants::MAX_HEADERS) return StatusCode::REQUEST_HEADER_FIELDS_TOO_LARGE;

                $headers[$headerName] = $headerValue;
            }
        }

        $body = rtrim($body, "\r\n");
        if (isset($headers["Content-Length"])) {
            $contentLength = (int)$headers["Content-Length"];
            if ($contentLength > HttpConstants::MAX_REQUEST_SIZE) {
                return StatusCode::PAYLOAD_TOO_LARGE;
            }
        }

        $match = HttpServer::getInstance()->findPath($method, $path);
        if ($match === null) return StatusCode::NOT_FOUND;
        [$path, $parameters] = $match;
        return new Request($address, $method, $path, $queryParams, $headers, $body, $parameters);
    }

    public static function encodeHeaders(array $headers): array {
        $tmp = [];
        foreach ($headers as $k => $v) {
            $k = str_replace(["\r", "\n", ":"], "", $k);
            $v = str_replace(["\r", "\n"], "", $v);
            $tmp[] = "$k: $v";
        }
        return $tmp;
    }
}