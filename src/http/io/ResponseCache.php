<?php

namespace pocketcloud\cloud\http\io;

use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\http\HttpServer;

final class ResponseCache {

    private static ?ThreadSafeArray $cache = null;

    private static function initCache(): void {
        if (self::$cache === null) {
            self::$cache = new ThreadSafeArray();
        }
    }

    public static function tick(): void {
        self::initCache();

        $cachingTime = HttpServer::getInstance()->getCachingTimeInSeconds();
        $now = time();

        self::$cache->synchronized(function() use ($now, $cachingTime) {
            $keysToRemove = [];

            foreach (self::$cache as $pathString => $data) {
                [, $time] = $data;

                if ($now >= ($time + $cachingTime)) {
                    $keysToRemove[] = $pathString;
                }
            }

            foreach ($keysToRemove as $key) {
                unset(self::$cache[$key]);
            }
        });
    }

    public static function cache(Request $request, Response $response): void {
        if (!HttpServer::getInstance()->isEnableResponseCaching()) return;

        self::initCache();

        $path = $request->getPath();
        $queries = $request->getQueries(true);

        $apiVersion = $path->getApiVersion() ?? "no-version";
        $fullPath = $path->getFullPath() . (count($queries) == 0 ? "" : "?" . http_build_query($queries));
        $cacheKey = $apiVersion . ":" . $path->getMethod() . ":" . $fullPath;

        self::$cache->synchronized(function() use ($cacheKey, $response) {
            self::$cache[$cacheKey] = serialize([$response, time()]);
        });
    }

    public static function check(Request $request): ?Response {
        if (!HttpServer::getInstance()->isEnableResponseCaching()) return null;

        self::initCache();

        $path = $request->getPath();
        $queries = $request->getQueries(true);

        $apiVersion = $path->getApiVersion() ?? "no-version";
        $fullPath = $path->getFullPath() . (count($queries) == 0 ? "" : "?" . http_build_query($queries));
        $cacheKey = $apiVersion . ":" . $path->getMethod() . ":" . $fullPath;

        return self::$cache->synchronized(function() use ($cacheKey, $fullPath, $request) {
            if (!isset(self::$cache[$cacheKey])) return null;

            $entry = unserialize(self::$cache[$cacheKey]);
            [$response, $time] = $entry;

            if (time() >= ($time + HttpServer::getInstance()->getCachingTimeInSeconds())) {
                unset(self::$cache[$cacheKey]);
                return null;
            }

            return $response;
        });
    }
}