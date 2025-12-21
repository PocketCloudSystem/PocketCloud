<?php

namespace pocketcloud\cloud\util\misc;

use ArrayAccess;
use InvalidArgumentException;

final class Queue implements ArrayAccess {

    private array $queue = [];

    public function __construct(
        private readonly string $queueType,
        array $queue = []
    ) {
        foreach ($queue as $item) {
            $this->add($item);
        }
    }

    public function add(mixed $element): void {
        if ($this->queueType !== gettype($element)) {
            throw new InvalidArgumentException("Failed to add element to queue, value is of type " . gettype($element) . ", expected value to be type of " . $this->queueType);
        }

        $this->queue[] = $element;
    }

    public function next(): mixed {
        if (count($this->queue) === 0) return null;
        $next = array_shift($this->queue);
        $this->queue = array_values($this->queue);
        return $next;
    }

    public function clear(): void {
        $this->queue = [];
    }

    public function count(): int {
        return count($this->queue);
    }

    public function getAll(): array {
        return $this->queue;
    }

    public function offsetExists(mixed $offset): bool {
        return isset($this->queue[$offset]);
    }

    public function offsetGet(mixed $offset): mixed {
        return $this->queue[$offset];
    }

    public function offsetSet(mixed $offset, mixed $value): void {
        if ($this->queueType !== gettype($value)) {
            throw new InvalidArgumentException("Can't set value at offset $offset, value is of type " . gettype($value) . ", expected to be of type " . $this->queueType);
        }

        $this->queue[$offset] = $value;
    }

    public function offsetUnset(mixed $offset): void {
        if (isset($this->queue[$offset])) {
            unset($this->queue[$offset]);
        }
    }
}