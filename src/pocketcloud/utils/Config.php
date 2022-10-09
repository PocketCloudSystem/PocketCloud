<?php

namespace pocketcloud\utils;

use Exception;

class Config {

    public const DETECT = -1;
    public const PROPERTIES = 0;
    public const CNF = Config::PROPERTIES;
    public const JSON = 1;
    public const YAML = 2;
    public const SERIALIZED = 4;
    public const ENUM = 5;
    public const ENUMERATION = Config::ENUM;

    private array $config = [];
    private array $nestedCache = [];

    private string $file;
    private int $type = Config::DETECT;
    private int $jsonOptions = JSON_PRETTY_PRINT | JSON_BIGINT_AS_STRING | JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE;

    private bool $changed = false;

    public static array $formats = [
        "properties" => Config::PROPERTIES,
        "cnf" => Config::CNF,
        "conf" => Config::CNF,
        "config" => Config::CNF,
        "json" => Config::JSON,
        "js" => Config::JSON,
        "yml" => Config::YAML,
        "yaml" => Config::YAML,
        "sl" => Config::SERIALIZED,
        "serialize" => Config::SERIALIZED,
        "txt" => Config::ENUM,
        "list" => Config::ENUM,
        "enum" => Config::ENUM
    ];

    public function __construct(string $file, int $type = Config::DETECT, array $default = []){
        $this->load($file, $type, $default);
    }

    public function reload() : void{
        $this->config = [];
        $this->nestedCache = [];
        $this->load($this->file, $this->type);
    }

    public function hasChanged() : bool{
        return $this->changed;
    }

    public function setChanged(bool $changed = true) : void{
        $this->changed = $changed;
    }

    public static function fixYAMLIndexes(string $str) : string{
        return preg_replace("#^( *)(y|Y|yes|Yes|YES|n|N|no|No|NO|true|True|TRUE|false|False|FALSE|on|On|ON|off|Off|OFF)( *)\:#m", "$1\"$2\"$3:", $str);
    }

    private function load(string $file, int $type = Config::DETECT, array $default = []) : void{
        $this->file = $file;

        $this->type = $type;
        if($this->type === Config::DETECT){
            $extension = strtolower(pathinfo($this->file, PATHINFO_EXTENSION));
            if(isset(Config::$formats[$extension])){
                $this->type = Config::$formats[$extension];
            }else{
                throw new \InvalidArgumentException("Cannot detect config type of " . $this->file);
            }
        }

        if(!file_exists($file)){
            $this->config = $default;
            $this->save();
        }else{
            $content = file_get_contents($this->file);
            if($content === false){
                throw new \RuntimeException("Unable to load config file");
            }
            switch($this->type){
                case Config::PROPERTIES:
                    $config = self::parseProperties($content);
                    break;
                case Config::JSON:
                    $config = json_decode($content, true, flags: JSON_THROW_ON_ERROR);
                    break;
                case Config::YAML:
                    $config = yaml_parse(self::fixYAMLIndexes($content));
                    break;
                case Config::SERIALIZED:
                    $config = unserialize($content);
                    break;
                case Config::ENUM:
                    $config = array_fill_keys(self::parseList($content), true);
                    break;
                default:
                    throw new \InvalidArgumentException("Invalid config type specified");
            }
            if(!is_array($config)){
                throw new \Exception("Failed to load config $this->file: Expected array for base type, but got " . get_debug_type($config));
            }
            $this->config = $config;
            if($this->fillDefaults($default, $this->config) > 0){
                $this->save();
            }
        }
    }

    public function getPath() : string{
        return $this->file;
    }

    public function save() : void{
        $content = null;
        switch($this->type){
            case Config::PROPERTIES:
                $content = self::writeProperties($this->config);
                break;
            case Config::JSON:
                $content = json_encode($this->config, $this->jsonOptions | JSON_THROW_ON_ERROR);
                break;
            case Config::YAML:
                $content = yaml_emit($this->config, YAML_UTF8_ENCODING);
                break;
            case Config::SERIALIZED:
                $content = serialize($this->config);
                break;
            case Config::ENUM:
                $content = self::writeList(array_keys($this->config));
                break;
            default:
                throw new Exception("Config type is unknown, has not been set or not detected");
        }

        file_put_contents($this->file, $content);

        $this->changed = false;
    }

    public function setJsonOptions(int $options) : Config{
        if($this->type !== Config::JSON){
            throw new \RuntimeException("Attempt to set JSON options for non-JSON config");
        }
        $this->jsonOptions = $options;
        $this->changed = true;

        return $this;
    }

    public function enableJsonOption(int $option) : Config{
        if($this->type !== Config::JSON){
            throw new \RuntimeException("Attempt to enable JSON option for non-JSON config");
        }
        $this->jsonOptions |= $option;
        $this->changed = true;

        return $this;
    }

    public function disableJsonOption(int $option) : Config{
        if($this->type !== Config::JSON){
            throw new \RuntimeException("Attempt to disable JSON option for non-JSON config");
        }
        $this->jsonOptions &= ~$option;
        $this->changed = true;

        return $this;
    }

    public function getJsonOptions() : int{
        if($this->type !== Config::JSON){
            throw new \RuntimeException("Attempt to get JSON options for non-JSON config");
        }
        return $this->jsonOptions;
    }

    public function __get($k){
        return $this->get($k);
    }

    public function __set($k, $v) : void{
        $this->set($k, $v);
    }

    public function __isset($k){
        return $this->exists($k);
    }

    public function __unset($k){
        $this->remove($k);
    }

    public function setNested($key, $value) : void{
        $vars = explode(".", $key);
        $base = array_shift($vars);

        if(!isset($this->config[$base])){
            $this->config[$base] = [];
        }

        $base = &$this->config[$base];

        while(count($vars) > 0){
            $baseKey = array_shift($vars);
            if(!isset($base[$baseKey])){
                $base[$baseKey] = [];
            }
            $base = &$base[$baseKey];
        }

        $base = $value;
        $this->nestedCache = [];
        $this->changed = true;
    }

    public function getNested($key, $default = null){
        if(isset($this->nestedCache[$key])){
            return $this->nestedCache[$key];
        }

        $vars = explode(".", $key);
        $base = array_shift($vars);
        if(isset($this->config[$base])){
            $base = $this->config[$base];
        }else{
            return $default;
        }

        while(count($vars) > 0){
            $baseKey = array_shift($vars);
            if(is_array($base) && isset($base[$baseKey])){
                $base = $base[$baseKey];
            }else{
                return $default;
            }
        }

        return $this->nestedCache[$key] = $base;
    }

    public function removeNested(string $key) : void{
        $this->nestedCache = [];
        $this->changed = true;

        $vars = explode(".", $key);

        $currentNode = &$this->config;
        while(count($vars) > 0){
            $nodeName = array_shift($vars);
            if(isset($currentNode[$nodeName])){
                if(count($vars) === 0){
                    unset($currentNode[$nodeName]);
                }elseif(is_array($currentNode[$nodeName])){
                    $currentNode = &$currentNode[$nodeName];
                }
            }else{
                break;
            }
        }
    }

    public function get($k, $default = false){
        return $this->config[$k] ?? $default;
    }

    public function set($k, $v = true) : void{
        $this->config[$k] = $v;
        $this->changed = true;
        foreach(Utils::stringifyKeys($this->nestedCache) as $nestedKey => $nvalue){
            if(substr($nestedKey, 0, strlen($k) + 1) === ($k . ".")){
                unset($this->nestedCache[$nestedKey]);
            }
        }
    }

    public function setAll(array $v) : void{
        $this->config = $v;
        $this->changed = true;
    }

    public function exists($k, bool $lowercase = false) : bool{
        if($lowercase){
            $k = strtolower($k);
            $array = array_change_key_case($this->config, CASE_LOWER);
            return isset($array[$k]);
        }else{
            return isset($this->config[$k]);
        }
    }

    public function remove($k) : void{
        unset($this->config[$k]);
        $this->changed = true;
    }

    public function getAll(bool $keys = false) : array{
        return ($keys ? array_keys($this->config) : $this->config);
    }

    public function setDefaults(array $defaults) : void{
        $this->fillDefaults($defaults, $this->config);
    }

    private function fillDefaults(array $default, &$data) : int{
        $changed = 0;
        foreach(Utils::stringifyKeys($default) as $k => $v){
            if(is_array($v)){
                if(!isset($data[$k]) || !is_array($data[$k])){
                    $data[$k] = [];
                }
                $changed += $this->fillDefaults($v, $data[$k]);
            }elseif(!isset($data[$k])){
                $data[$k] = $v;
                ++$changed;
            }
        }

        if($changed > 0){
            $this->changed = true;
        }

        return $changed;
    }

    public static function parseList(string $content) : array{
        $result = [];
        foreach(explode("\n", trim(str_replace("\r\n", "\n", $content))) as $v){
            $v = trim($v);
            if($v === ""){
                continue;
            }
            $result[] = $v;
        }
        return $result;
    }

    public static function writeList(array $entries) : string{
        return implode("\n", $entries);
    }

    public static function writeProperties(array $config) : string{
        $content = "#Properties Config file\r\n#" . date("D M j H:i:s T Y") . "\r\n";
        foreach(Utils::stringifyKeys($config) as $k => $v){
            if(is_bool($v)){
                $v = $v ? "on" : "off";
            }
            $content .= $k . "=" . $v . "\r\n";
        }

        return $content;
    }

    public static function parseProperties(string $content) : array{
        $result = [];
        if(preg_match_all('/^\s*([a-zA-Z0-9\-_\.]+)[ \t]*=([^\r\n]*)/um', $content, $matches) > 0){
            foreach($matches[1] as $i => $k){
                $v = trim($matches[2][$i]);
                switch(strtolower($v)){
                    case "on":
                    case "true":
                    case "yes":
                        $v = true;
                        break;
                    case "off":
                    case "false":
                    case "no":
                        $v = false;
                        break;
                    default:
                        $v = match($v){
                            (string) ((int) $v) => (int) $v,
                            (string) ((float) $v) => (float) $v,
                            default => $v,
                        };
                        break;
                }
                $result[(string) $k] = $v;
            }
        }

        return $result;
    }
}