<?php

declare(strict_types=1);

namespace pocketcloud\lib\snooze;

use function assert;

class SleeperNotifier extends \Threaded{

	private \Threaded $sharedObject;
	private int $sleeperId;

	final public function attachSleeper(\Threaded $sharedObject, int $id): void {
		$this->sharedObject = $sharedObject;
		$this->sleeperId = $id;
	}

	final public function getSleeperId(): int {
		return $this->sleeperId;
	}

	final public function wakeupSleeper(): void {
		$shared = $this->sharedObject;
		assert($shared !== null);
		$sleeperId = $this->sleeperId;
		$shared->synchronized(function() use ($shared, $sleeperId): void {
			if(!isset($shared[$sleeperId])) {
				$shared[$sleeperId] = $sleeperId;
				$shared->notify();
			}
		});
	}
}
