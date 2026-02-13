<?php

namespace pocketcloud\cloud\update;

use pocketcloud\cloud\util\promise\Promise;

interface IUpdateChecker {

    /**
     * @return Promise<array{needsUpdate: bool, extraData: mixed}>
     */
    public function needsUpdate(): Promise;

    /**
     * @return Promise<bool>
     */
    public function update(mixed $extraData): Promise;

    public function id(): string;
}