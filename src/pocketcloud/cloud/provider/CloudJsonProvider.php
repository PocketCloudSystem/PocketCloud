<?php

namespace pocketcloud\cloud\provider;

use pocketcloud\cloud\config\Config;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\util\promise\Promise;

final class CloudJsonProvider extends CloudProvider {

    private ?Config $templatesConfig = null;

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

    public function getTemplatesConfig(): ?Config {
        return $this->templatesConfig;
    }
}