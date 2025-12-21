<?php

namespace pocketcloud\cloud\util\misc;

interface Tickable {

    public function tick(int $currentTick): void;
}