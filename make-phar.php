<?php

if (file_exists(__DIR__ . "/PocketCloud.phar")) unlink(__DIR__ . "/PocketCloud.phar");

$phar = new Phar(__DIR__ . "/PocketCloud.phar", 0, "PocketCloud.phar");
$phar->setStub($phar->createDefaultStub("src/pocketcloud/PocketCloud.php"));
$phar->buildFromDirectory(__DIR__ . "/", "/\.php$/");
if (isset($phar["make-phar.php"])) unset($phar["make-phar.php"]);
$phar->compressFiles(Phar::GZ);