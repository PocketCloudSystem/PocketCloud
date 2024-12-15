<?php

namespace pocketcloud\cloud\util\tick;

interface Tickable {

    public function tick(int $currentTick): void;
}