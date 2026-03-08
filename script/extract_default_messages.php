<?php

$deConf = yaml_parse_file(__DIR__ . "/lang/en.yml");
echo "public static const MESSAGES = [";
foreach ($deConf as $key => $value) {
    echo "\n\r\t\"" . $key . "\" => \"" . $value . "\",";
}
echo "\n];";
echo "\n\n";

$deConf = yaml_parse_file(__DIR__ . "/lang/de.yml");
echo "public static const MESSAGES_DE = [";
foreach ($deConf as $key => $value) {
    echo "\n\r\t\"" . $key . "\" => \"" . $value . "\",";
}
echo "\n];";