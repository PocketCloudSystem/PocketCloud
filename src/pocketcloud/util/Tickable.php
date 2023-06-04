<?php

namespace pocketcloud\util;

interface Tickable {

    public function tick(int $currentTick): void;
}