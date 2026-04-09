<?php

namespace pocketcloud\cloud\http\client\thread\misc;

use CURLFile;
use CurlHandle;
use InvalidArgumentException;
use pmmp\thread\ThreadSafe;
use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\http\client\io\ClientRequestContext;
use pocketcloud\cloud\http\client\util\MultipartBody;
use pocketcloud\cloud\http\util\HttpUtils;
use RuntimeException;

final class PendingRequest extends ThreadSafe {

    public function __construct(
        public readonly string $requestId,
        public readonly string $url,
        public readonly string $method,
        public ThreadSafeArray $queries,
        public ThreadSafeArray $headers,
        public readonly ?string $body,
        public readonly bool $multipartBody,
        public readonly int $timeout,
        public readonly int $retries
    ) {}

    public function prepareCurlHandle(CurlHandle $curlHandle, ?string &$finalUrl = null): void {
        $queryString = empty($this->queries) ? "" : "?" . http_build_query((array) $this->queries);
        $body = $this->body === null ? null : unserialize($this->body);
        $opts = [
            CURLOPT_URL => $finalUrl = $this->url . $queryString,
            CURLOPT_SSL_VERIFYPEER => false,
            CURLOPT_SSL_VERIFYHOST => false,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HEADER => true,
            CURLOPT_TIMEOUT => $this->timeout
        ];

        if ($body !== null) {
            if ($this->multipartBody) {
                $fields = $body["fields"];
                $files = array_map(fn (array $value) => new CURLFile(...$value), $body["files"]);
                $opts[CURLOPT_POSTFIELDS] = array_merge($fields, $files);
            } else {
                $contentType = $this->headers["Content-Type"] ?? null;
                if (is_array($body) || is_object($body)) {
                    if ($contentType === null || str_contains($contentType, "application/json")) {
                        $opts[CURLOPT_POSTFIELDS] = json_encode($body);
                        $this->headers["Content-Type"] = "application/json";
                    }
                } elseif (is_string($body)) {
                    $opts[CURLOPT_POSTFIELDS] = $body;
                    if ($contentType === null) $this->headers["Content-Type"] = "text/plain";
                } elseif (is_int($body) || is_float($body) || is_bool($body)) {
                    $opts[CURLOPT_POSTFIELDS] = (string) $body;
                    $this->headers["Content-Type"] = $contentType ?? "text/plain";
                } else {
                    throw new InvalidArgumentException("Unsupported body type: " . gettype($body));
                }

                $this->headers["Content-Length"] = strlen($opts[CURLOPT_POSTFIELDS]);
            }
        }

        if ($this->method == "POST") {
            if (!$this->multipartBody) {
                $opts[CURLOPT_POST] = true;
                if ($body === null && !empty($this->queries)) {
                    $opts[CURLOPT_POSTFIELDS] = substr($queryString, 1);
                    $this->headers["Content-Type"] = "application/x-www-form-urlencoded";
                }
            }
        } else if (in_array($this->method, ["PUT", "PATCH", "DELETE"])) {
            $opts[CURLOPT_CUSTOMREQUEST] = $this->method;
        }

        $opts[CURLOPT_HTTPHEADER] = HttpUtils::encodeHeaders((array) $this->headers);
        curl_setopt_array($curlHandle, $opts);
    }

    public function execute(CurlHandle $curlHandle): FinishedRequest {
        $this->prepareCurlHandle($curlHandle, $finalUrl);
        $result = curl_exec($curlHandle);
        $retries = $this->retries;
        if ($result === false) {
            while ($retries > 0) {
                $retries--;
                $result = curl_exec($curlHandle);
                if ($result !== false) break;
            }

            if ($result === false) {
                $errno = curl_errno($curlHandle);
                $error = curl_error($curlHandle);
                return new FinishedRequest(
                    $this->requestId,
                    $finalUrl,
                    $this->method,
                    -1,
                    new ThreadSafeArray(),
                    null,
                    null,
                    $this->retries - $retries,
                    RequestExceptionData::fromException(new RuntimeException("cURL Error ($errno): $error"))
                );
            }
        }

        $code = curl_getinfo($curlHandle, CURLINFO_HTTP_CODE);
        $headers = explode("\r\n", substr($result, 0, ($len = curl_getinfo($curlHandle, CURLINFO_HEADER_SIZE))));
        $body = substr($result, $len);
        $parsedBody = null;
        foreach ($headers as $line) {
            if (str_contains($line, ":")) {
                [$name, $val] = explode(":", $line, 2);
                if (strtolower(trim($name)) === "content-type" && str_contains($val, "application/json")) {
                    $parsedBody = json_decode($body, true);
                }
            }
        }

        return new FinishedRequest(
            $this->requestId,
            $finalUrl,
            $this->method,
            $code,
            ThreadSafeArray::fromArray($headers),
            $body,
            serialize($parsedBody),
            $this->retries - $retries,
            null
        );
    }
    
    public static function fromContext(string $requestId, ClientRequestContext $context): self {
        $body = $context->body();
        if ($body !== null) {
            if ($body instanceof MultipartBody) {
                $data = ["fields" => $body->fields(), "files" => []];
                /** @var CURLFile $file */
                foreach ($body->files() as $name => $file) {
                    $data["files"][$name] = [$file->getFilename(), $file->getMimeType(), $file->getPostFilename()];
                }

                $body = $data;
            }

            $body = serialize($body);
        }

        return new self(
            $requestId,
            $context->url(),
            $context->method()->name,
            ThreadSafeArray::fromArray($context->queries()),
            ThreadSafeArray::fromArray($context->headers()),
            $body,
            $context->body() instanceof MultipartBody,
            $context->timeout(),
            $context->retries()
        );
    }
}