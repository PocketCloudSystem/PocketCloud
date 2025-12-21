<?php

$buildCloud = false;
$buildBridgeServer = false;
$options = getopt("", ["cloud::", "bridge-server::"]);
if (empty($options) || (!($buildCloud = isset($options["cloud"])) && !($buildBridgeServer = isset($options["bridge-server"])))) {
    error("Please select the project that you want to build. (--cloud OR --bridge-server)");
    exit(1);
}

if (!file_exists(__DIR__ . "/output/")) mkdir(__DIR__ . "/output/");

if ($buildCloud) {
    info("Building cloud...");
    buildCloud();
    info("Built cloud.");
}

if ($buildBridgeServer) {
    info("Building cloud bridge...");
    buildCloudBridge();
    info("Built cloud bridge.");
}

exit(0);

function info(string $message): void {
    echo "[" . date("H:i:s") . "/INFO] " . $message . "\n";
}

function error(string $message): void {
    echo "[" . date("H:i:s") . "/ERROR] " . $message . "\n";
}

function buildCloud(): void {
    if (file_exists(__DIR__ . "/output/PocketCloud.phar")) unlink(__DIR__ . "/output/PocketCloud.phar");
    info("Copying files...");
    if (!file_exists(__DIR__ . "/output/pocketcloud-tmp-source/")) mkdir(__DIR__ . "/output/pocketcloud-tmp-source/");
    if (!file_exists(__DIR__ . "/output/pocketcloud-tmp-source/vendor/")) mkdir(__DIR__ . "/output/pocketcloud-tmp-source/vendor/");
    if (!file_exists(__DIR__ . "/output/pocketcloud-tmp-source/src/")) mkdir(__DIR__ . "/output/pocketcloud-tmp-source/src/");
    copyDirectory(__DIR__ . "/../src/", __DIR__ . "/output/pocketcloud-tmp-source/src/");
    copyDirectory(__DIR__ . "/../vendor/", __DIR__ . "/output/pocketcloud-tmp-source/vendor/");

    $phar = new Phar(__DIR__ . "/output/PocketCloud.phar", 0, "PocketCloud.phar");
    $phar->setStub($phar->createDefaultStub("src/PocketCloud.php"));
    $phar->buildFromDirectory(__DIR__ . "/output/pocketcloud-tmp-source/");
    $phar->compressFiles(Phar::GZ);
    removeDirectory(__DIR__ . "/output/pocketcloud-tmp-source/");
}

function buildCloudBridge(): void {
    if (file_exists(__DIR__ . "/output/CloudBridge.phar")) unlink(__DIR__ . "/output/CloudBridge.phar");

    $phar = new Phar(__DIR__ . "/output/CloudBridge.phar", 0, "CloudBridge.phar");
    $phar->buildFromDirectory(__DIR__ . "/../../bridge-server/", "/\.php$/");
    $phar["plugin.yml"] = file_get_contents(__DIR__ . "/../../bridge-server/plugin.yml");
    $phar->compressFiles(Phar::GZ);
}

function copyDirectory(string $source, string $destination): void {
    if (!file_exists($destination)) mkdir($destination);
    foreach (scandir($source) as $file) {
        if ($file == "." || $file == "..") continue;
        if (is_dir($source . "/" . $file)) {
            copyDirectory($source . "/" . $file . "/", $destination . "/" . $file . "/");
        } else {
            copy($source . "/" . $file, $destination . "/" . $file);
        }
    }
}

function removeDirectory(string $directory): void {
    if (is_dir($directory)) {
        $files = array_diff(scandir($directory), [".", ".."]);
        if (empty($files)) {
            rmdir($directory);
        } else {
            foreach (scandir($directory) as $file) {
                if ($file == "." || $file == "..") continue;
                if (is_dir($directory . "/" . $file)) {
                    removeDirectory($directory . "/" . $file . "/");
                } else {
                    unlink($directory . "/" . $file);
                }
            }

            rmdir($directory);
        }
    }
}