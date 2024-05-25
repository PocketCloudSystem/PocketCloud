<?php

namespace pocketcloud\web;

use pocketcloud\config\Config;
use pocketcloud\config\impl\DefaultConfig;
use pocketcloud\config\type\ConfigTypes;
use pocketcloud\http\endpoint\EndpointRegistry;
use pocketcloud\http\endpoint\impl\web\WebAccountCreateEndPoint;
use pocketcloud\http\endpoint\impl\web\WebAccountGetEndPoint;
use pocketcloud\http\endpoint\impl\web\WebAccountListEndPoint;
use pocketcloud\http\endpoint\impl\web\WebAccountRemoveEndPoint;
use pocketcloud\http\endpoint\impl\web\WebAccountUpdateEndPoint;
use pocketcloud\util\Reloadable;
use pocketcloud\util\SingletonTrait;

class WebAccountManager implements Reloadable {
    use SingletonTrait;

    /** @var array<WebAccount> */
    private array $accounts = [];
    private Config $accountsConfig;

    public function __construct() {
        self::setInstance($this);
        $this->accountsConfig = new Config(WEB_PATH . "accounts.json", ConfigTypes::JSON());
    }

    public function loadAccounts(): void {
        if (!DefaultConfig::getInstance()->isWebEnabled()) return;
        foreach ($this->accountsConfig->getAll() as $data) {
            if (($account = WebAccount::fromArray($data)) !== null) {
                $this->accounts[$account->getName()] = $account;
            }
        }

        foreach ([new WebAccountCreateEndPoint(), new WebAccountRemoveEndPoint(), new WebAccountGetEndPoint(), new WebAccountUpdateEndPoint(), new WebAccountListEndPoint()] as $endPoint) EndpointRegistry::addEndPoint($endPoint);
    }

    public function createAccount(WebAccount $account): void {
        if (!DefaultConfig::getInstance()->isWebEnabled()) return;
        $this->accounts[$account->getName()] = $account;
        $this->accountsConfig->set($account->getName(), $account->toArray());
        $this->accountsConfig->save();
    }

    public function updateAccount(WebAccount $account, ?string $password, ?WebAccountRoles $role): void {
        if (!DefaultConfig::getInstance()->isWebEnabled()) return;
        if ($password !== null) {
            $account->setPassword($password);
            $account->setInitialPassword(false);
        }

        if ($role !== null) $account->setRole($role);

        $this->accountsConfig->set($account->getName(), $account->toArray());
        $this->accountsConfig->save();
    }

    public function removeAccount(WebAccount $account): void {
        if (!DefaultConfig::getInstance()->isWebEnabled()) return;

        if ($this->checkAccount($account->getName())) unset($this->accounts[$account->getName()]);

        $this->accountsConfig->remove($account->getName());
        $this->accountsConfig->save();
    }

    public function reload(): bool {
        if (DefaultConfig::getInstance()->isWebEnabled()) {
            $this->accounts = [];
            $this->accountsConfig->reload();
            $this->loadAccounts();
            foreach ([new WebAccountCreateEndPoint(), new WebAccountRemoveEndPoint(), new WebAccountGetEndPoint(), new WebAccountUpdateEndPoint(), new WebAccountListEndPoint()] as $endPoint) EndpointRegistry::addEndPoint($endPoint);
        } else {
            $this->accounts = [];
            unset($this->accountsConfig);
            foreach ([new WebAccountCreateEndPoint(), new WebAccountRemoveEndPoint(), new WebAccountGetEndPoint(), new WebAccountUpdateEndPoint(), new WebAccountListEndPoint()] as $endPoint) EndpointRegistry::removeEndPoint($endPoint);
        }

        return true;
    }

    public function checkAccount(string $name): bool {
        return isset($this->accounts[$name]);
    }

    public function getAccount(string $name): ?WebAccount {
        if (!DefaultConfig::getInstance()->isWebEnabled()) return null;
        return $this->accounts[$name] ?? null;
    }

    public function getAccounts(): array {
        if (!DefaultConfig::getInstance()->isWebEnabled()) return [];
        return $this->accounts;
    }

    public static function getInstance(): self {
        return self::$instance ??= new self;
    }
}