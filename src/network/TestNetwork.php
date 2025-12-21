<?php

namespace pocketcloud\cloud\network;

use pocketcloud\cloud\thread\Thread;

final class TestNetwork extends Thread {

    private bool $shutdown = false;

    protected function onRun(): void {
        while (!$this->shutdown && $this->isAlive()) {
            usleep(50000 * 20);
            #CloudLogger::get()->info("Hallo!");
        }
    }

    public function requestShutdown(): void {
        $this->shutdown = true;
    }
}