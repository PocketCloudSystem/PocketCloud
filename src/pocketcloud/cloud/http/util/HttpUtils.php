<?php

namespace pocketcloud\cloud\http\util;

use pocketcloud\cloud\http\io\data\Queries;
use pocketcloud\cloud\http\io\data\RequestData;
use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\util\net\Address;
use UnexpectedValueException;

final class HttpUtils {
	
	private const string LOCALHOST_PREFIX = "http://localhost";
	
	public static function parseRequest(Address $address, string $request): ?Request {
		[$headers, $bodyLines] = self::splitData(explode("\r\n", $request));
        if (empty($headers)) return null;
		$req = explode(" ", trim(rtrim(array_shift($headers))));
		$method = array_shift($req);
		if (!in_array($method, Request::SUPPORTED_REQUEST_METHODS)) return null;
		if (empty($req)) return null;
		$path = "/" . trim(array_shift($req), "/");
		if (filter_var(self::LOCALHOST_PREFIX . $path, FILTER_VALIDATE_URL) !== self::LOCALHOST_PREFIX . $path) return null;
		$queries = "";
		if (str_contains($path, "?")) $queries = substr($path, strpos($path, "?") + 1);
		if (empty($req)) return null;
		return new Request(self::decodeHeaders($headers), new RequestData($address, strtoupper($method), urldecode($path), new Queries(self::remapQueries($queries))), implode(PHP_EOL, array_filter($bodyLines, fn($v) => $v)));
	}
	
	private static function decodeHeaders(array $headers): array {
		$h = [];
		foreach ($headers as $header) {
			if (!str_contains($header, ": ")) continue;
			$h[($d = explode(": ", $header))[0]] = $d[1];
		}
		return $h;
	}
	
	public static function encodeHeaders(array $headers): array {
		$h = [];
		foreach ($headers as $k => $v) $h[] = "$k: $v";
		return $h;
	}
	
	private static function splitData(array $lines): array {
		[$headers, $bodyLines] = [$lines, $lines];
		foreach ($lines as $i => $line) {
			if (!$line) return [array_slice($lines, 0, $i), array_slice($lines, $i)];
		}
		return [$headers, $bodyLines];
	}
	
	private static function remapQueries(string $queries): array {
		if (!str_contains($queries, "=")) return [];
		$mapped = [];
		foreach (explode("&", $queries) as $pair) {
			if (substr_count($pair, "=") !== 1 or str_starts_with($pair, "=") or str_ends_with($pair, "=")) continue;
			$mapped[urldecode(($d = explode("=", $pair))[0])] = urldecode($d[1]);
		}
		return $mapped;
	}

    public static function matchPath(string $base, string $match): bool {
        if (str_contains($base, "?")) {
            if (!str_contains($match, "?")) return false;
            if (!self::matchQueries(substr($base, strpos($base, "?") + 1), substr($match, strpos($match, "?") + 1))) return false;
            $base = substr($base, 0, strpos($base, "?"));
        }
        if (str_contains($match, "?")) $match = substr($match, 0, strpos($match, "?"));
        [$base, $match] = [rtrim(str_replace("\\", "/", $base), "/ "), rtrim(str_replace("\\", "/", $match), "/ ")];
        $baseParameters = explode("/", $base);
        $matchParameters = explode("/", $match);
        if (count($baseParameters) !== count($matchParameters)) return false;
        foreach ($baseParameters as $k => $v) {
            if (str_starts_with($v, "#")) continue;
            if (str_starts_with($v, "{") and str_ends_with($v, "}") and !Pattern::isValid($matchParameters[$k], json_decode($v, true))) return false;
            if ($v !== $matchParameters[$k]) return false;
        }
        return true;
    }
	
	public static function matchQueries(string $base, string $match): bool {
		return !count(array_diff(array_keys(self::remapQueries($base)), array_keys(self::remapQueries($match))));
	}
	
	public static function fillRequest(Request $request, string $baseUrl): void {
		[$base, $match] = [rtrim(str_replace("\\", "/", $baseUrl), "/ "), rtrim(str_replace("\\", "/", $request->data()->path()), "/ ")];
		if (str_contains($base, "?")) {
			$matchQueries = self::remapQueries(substr($match, strpos($match, "?") + 1));
			foreach (self::remapQueries(substr($base, strpos($base, "?") + 1)) as $k => $name) {
				$request->{$name} = $matchQueries[$k];
			}
			[$base, $match] = [substr($base, 0, strpos($base, "?")), substr($match, 0, strpos($match, "?"))];
		}
		$baseParameters = explode("/", $base);
		$matchParameters = explode("/", $match);
		foreach ($baseParameters as $k => $v) {
			if (str_starts_with($v, "#")) {
				$request->{substr($v, 1)} = $matchParameters[$k];
			} else if (str_starts_with($v, "{") and str_ends_with($v, "}")) {
				$d = json_decode($v, true);
				if (!isset($d["name"])) throw new UnexpectedValueException("Parameter name for $baseUrl (" . $request->data()->method() . ") is missing!");
				$request->{$d["name"]} = $matchParameters[$k];
			}
		}
	}
}
