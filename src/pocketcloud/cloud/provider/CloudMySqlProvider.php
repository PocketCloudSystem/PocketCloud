<?php

namespace pocketcloud\cloud\provider;

use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\util\promise\Promise;
use r3pt1s\mysql\ConnectionPool;

final class CloudMySqlProvider extends CloudProvider {

    private ?ConnectionPool $connectionPool = null;

    public function __construct() {
    }

    public function createTemplate(Template $template): Promise {
        $promise = new Promise();
        $promise->reject();
        return $promise;
    }

    public function removeTemplate(Template $template): Promise {
        $promise = new Promise();
        $promise->reject();
        return $promise;
    }

    public function getTemplate(string $template): Promise {
        $promise = new Promise();
        $promise->reject();
        return $promise;
    }

    public function checkTemplate(string $template): Promise {
        $promise = new Promise();
        $promise->reject();
        return $promise;
    }

    public function getTemplates(): Promise {
        $promise = new Promise();
        $promise->reject();
        return $promise;
    }

    public function getConnectionPool(): ?ConnectionPool {
        return $this->connectionPool;
    }
}