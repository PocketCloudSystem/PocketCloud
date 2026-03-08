<?php

namespace pocketcloud\cloud\cache;

interface Cache
{

    public static function syncOut(): void;
    public static function getAll(): array;

}