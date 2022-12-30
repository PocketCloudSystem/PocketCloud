<?php

namespace pocketcloud\lib\express\utils;

use ArrayIterator;
use Closure;
use JetBrains\PhpStorm\Pure;
use function array_filter;
use function array_keys;
use function array_map;

/**
 * @template T
 * @extends ArrayIterator<T>
 */
class Collection extends ArrayIterator {
	
	public function has(mixed $key): bool {
		return $this->offsetExists($key);
	}
	
	/**
	 * @param mixed $key
	 * @param mixed|null $default
	 *
	 * @return T
	 */
	public function get(mixed $key, mixed $default = null) {
		if (!$this->has($key)) return $default;
		return $this->offsetGet($key) ?? $default;
	}
	
	/**
	 * @param mixed $key
	 * @param mixed|null $default
	 *
	 * @return T
	 */
	public function getAndUnset(mixed $key, mixed $default = null) {
		if ($this->has($key)) {
			$r = $this->get($key);
			$this->unset($key);
			return $r;
		}
		return $default;
	}
	
	/**
	 * @param mixed $key
	 * @param T $value
	 */
	public function set(mixed $key, mixed $value): void {
		$this->offsetSet($key, $value);
	}
	
	public function unset(mixed $key): bool {
		if (!$this->has($key)) return false;
		$this->offsetUnset($key);
		return true;
	}
	
	/**
	 * @return array<T>
	 */
	public function asArray(): array {
		return (array) $this;
	}
	
	public function map(Closure $closure): array {
		return array_map($closure, $this->asArray());
	}
	
	public function filter(Closure $closure): array {
		return array_filter($this->asArray(), $closure);
	}
	
	#[Pure] public function keys(): array {
		return array_keys($this->asArray());
	}
}