<?php

namespace pocketcloud\cloud\provider;

use pocketcloud\cloud\config\Config;
use pocketcloud\cloud\config\type\ConfigTypes;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\util\promise\Promise;

final class CloudJsonProvider extends CloudProvider {

    private Config $templatesConfig;

    public function __construct() {
        $this->templatesConfig = new Config(TEMPLATES_PATH . "templates.json", ConfigTypes::JSON());
    }

    public function addTemplate(Template $template): void {
        $this->templatesConfig->set($template->getName(), $template->toArray());
        $this->templatesConfig->save();
    }

    public function removeTemplate(Template $template): void {
        $this->templatesConfig->remove($template->getName());
        $this->templatesConfig->save();
    }

    public function getTemplate(string $template): Promise {
        $promise = new Promise();

        $data = $this->templatesConfig->get($template);
        if (($template = Template::fromArray($data)) !== null) {
            $promise->resolve($template);
        } else $promise->reject();

        return $promise;
    }

    public function checkTemplate(string $template): Promise {
        $promise = new Promise();
        $promise->resolve($this->templatesConfig->has($template));
        return $promise;
    }

    public function getTemplates(): Promise {
        $promise = new Promise();

        $templates = [];
        $data = $this->templatesConfig->getAll();
        foreach ($data as $template) {
            if (($template = Template::fromArray($template)) !== null) $templates[$template->getName()] = $template;
        }

        $promise->resolve($templates);
        return $promise;
    }

    public function getTemplatesConfig(): ?Config {
        return $this->templatesConfig;
    }
}