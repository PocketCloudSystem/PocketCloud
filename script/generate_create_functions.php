<?php

$baseDir = __DIR__ . '/src/'; // <-- Ordner mit Klassen

foreach (
    [
        __DIR__ . "/../src/network/packet/impl/" => "cloud",
        __DIR__ . "/../../bridge-server/src/network/packet/impl/" => "bridge",
    ] as $orgPath => $newPath
) {
    copydir($orgPath, $baseDir . $newPath);
}

$rii = new RecursiveIteratorIterator(new RecursiveDirectoryIterator($baseDir));

foreach ($rii as $file) {
    if (!$file->isFile() || $file->getExtension() !== 'php') {
        continue;
    }

    $code = file_get_contents($file->getPathname());

    // Skip if create() already exists
    if (preg_match('/(public|protected|private)\s+static\s+function\s+create\s*\(/', $code)) {
        continue;
    }

    // Match public constructor
    if (!preg_match('/public\s+function\s+__construct\s*\(([^)]*)\)/s', $code, $match)) {
        continue;
    }

    $rawParams = trim($match[1]);
    if ($rawParams === '') {
        continue;
    }

    // 1. Remove default values
    $params = preg_replace('/\s*=\s*[^,\r\n]+/', '', $rawParams);

    // 2. Remove nullable marker '?'
    $params = preg_replace('/\?(\s*[A-Za-z_\\\\][A-Za-z0-9_\\\\]*)/', '$1', $params);

    // 3. Remove constructor property promotion keywords
    $params = preg_replace(
        '/\b(public|protected|private|readonly)\b\s*/',
        '',
        $params
    );

    // 4. Normalize whitespace
    $params = preg_replace('/\s+/', ' ', trim($params));

    // Extract variable names
    preg_match_all('/\$(\w+)/', $params, $paramMatches);
    $paramList = implode(', ', array_map(fn($p) => '$' . $p, $paramMatches[1]));

    $createMethod =
        "\n    public static function create($params): self {\n" .
        "        return new self($paramList);\n" .
        "    }";

    // Insert before last closing brace
    $code = preg_replace('/}\s*$/', $createMethod . "\n}", $code, 1);

    file_put_contents($file->getPathname(), $code);

    echo "Updated: {$file->getPathname()}\n";
}

echo "Done.\n";

function copydir(string $src, string $dst): bool {
    $src = rtrim($src, DIRECTORY_SEPARATOR);
    $dst = rtrim($dst, DIRECTORY_SEPARATOR);
    mkdir($dst, 0777, true);

    foreach (array_diff(scandir($src), [".", ".."]) as $file) {
        try {
            if (filetype($src . DIRECTORY_SEPARATOR . $file) == "dir") {
                copydir($src . DIRECTORY_SEPARATOR . $file, $dst . DIRECTORY_SEPARATOR . $file);
            } else {
                copy($src . DIRECTORY_SEPARATOR . $file, $dst . DIRECTORY_SEPARATOR . $file);
            }
        } catch (Throwable) {}
    }
    return false;
}