<?php

namespace pocketcloud\cloud\console\command;

interface ITabComplete {

    public function onTabComplete(array $args): array;
}