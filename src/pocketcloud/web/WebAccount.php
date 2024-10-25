<?php

namespace pocketcloud\web;

use pocketcloud\util\Utils;

final class WebAccount {

    public function __construct(
        private readonly string $name,
        private string $password,
        private bool $initialPassword,
        private WebAccountRoles $role
    ) {}

    public function setPassword(string $password): void {
        $this->password = $password;
    }

    public function setInitialPassword(bool $initialPassword): void {
        $this->initialPassword = $initialPassword;
    }

    public function setRole(WebAccountRoles $role): void {
        $this->role = $role;
    }

    public function getName(): string {
        return $this->name;
    }

    public function getPassword(): string {
        return $this->password;
    }

    public function isInitialPassword(): bool {
        return $this->initialPassword;
    }

    public function getRole(): WebAccountRoles {
        return $this->role;
    }

    public function toArray(): array {
        return [
            "username" => $this->name,
            "password" => $this->password,
            "initialPassword" => $this->initialPassword,
            "role" => $this->role->roleName()
        ];
    }

    public static function create(string $name, string $password, bool $initialPassword, WebAccountRoles $role): self {
        return new self($name, $password, $initialPassword, $role);
    }

    public static function fromArray(array $data): ?self {
        if (!Utils::containKeys($data, "username", "password", "initialPassword", "role")) return null;
        if (($role = WebAccountRoles::from($data["role"])) === null) return null;
        if (!is_bool($data["initialPassword"])) return null;
        return self::create($data["username"], $data["password"], $data["initialPassword"], $role);
    }
}