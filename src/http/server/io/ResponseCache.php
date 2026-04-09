<?php

namespace pocketcloud\cloud\http\server\io;

use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\http\server\HttpServer;
use pocketcloud\cloud\http\util\RequestMethod;

final class ResponseCache {

    private static ?ThreadSafeArray $cache = null;

    public static function tick(): void {
        self::initCache();

        $cachingTime = HttpServer::getInstance()->getCachingTimeInSeconds();
        $now = time();

        self::$cache->synchronized(function () use ($now, $cachingTime): void {
            $keysToRemove = [];
            foreach (self::$cache as $key => $data) {
                [, $time] = (array)$data;
                if ($now >= ($time + $cachingTime)) {
                    $keysToRemove[] = $key;
                }
            }

            foreach ($keysToRemove as $key) {
                unset(self::$cache[$key]);
            }
        });
    }

    private static function initCache(): void {
        if (self::$cache === null) {
            self::$cache = new ThreadSafeArray();
        }
    }

    public static function cache(Request $request, Response $response): void {
        if (!HttpServer::getInstance()->isEnableResponseCaching()) return;
        if ($request->getMethod() !== RequestMethod::GET) return;

        self::initCache();
        $cacheKey = self::buildKey($request);

        self::$cache->synchronized(function () use ($cacheKey, $response): void {
            self::$cache[$cacheKey] = ThreadSafeArray::fromArray([$response, time()]);
        });
    }

    private static function buildKey(Request $request): string {
        $path = $request->getPath();
        $queries = $request->getQueries(true);
        $apiVersion = $path->getApiVersion() ?? "no-version";
        $fullPath = $path->getFullPath() . (count($queries) === 0 ? "" : "?" . http_build_query($queries));
        return $apiVersion . ":" . $path->getMethod()->name . ":" . $fullPath;
    }

    public static function check(Request $request): ?Response {
        if (!HttpServer::getInstance()->isEnableResponseCaching()) return null;
        if ($request->getMethod() != RequestMethod::GET) return null;

        self::initCache();

        $cacheKey = self::buildKey($request);
        $cachingTime = HttpServer::getInstance()->getCachingTimeInSeconds();

        return self::$cache->synchronized(function () use ($cacheKey, $cachingTime): ?Response {
            if (!isset(self::$cache[$cacheKey])) return null;

            [$response, $time] = (array)self::$cache[$cacheKey];

            if (time() >= ($time + $cachingTime)) {
                unset(self::$cache[$cacheKey]);
                return null;
            }

            return $response;
        });
    }
}