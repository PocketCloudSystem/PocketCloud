<?php

namespace pocketcloud\cloud\web;

enum WebAccountRoles: string {

    case ADMIN = "admin";
    case DEFAULT = "default";

    public function roleName(): string {
        return $this->value;
    }

    public static function get(string $name): ?self {
        return match (strtolower($name)) {
            "admin" => self::ADMIN,
            "default" => self::DEFAULT,
            default => null
        };
    }
}