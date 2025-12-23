<?php

namespace pocketcloud\cloud\util\misc;

use ArrayAccess;
use InvalidArgumentException;
use SplQueue;

/**
 * @template T
 * @implements ArrayAccess<int, T>
 */
final class Queue implements ArrayAccess {

    private const int QUEUE_TYPE_DATA_TYPE = 0;
    private const int QUEUE_TYPE_CLASS = 1;

    private SplQueue $queue;

    private function __construct(
        private readonly string $typeOrClass,
        array $queue = [],
        private readonly int $queueType = self::QUEUE_TYPE_DATA_TYPE
    ) {
        $this->queue = new SplQueue();
        foreach ($queue as $item) {
            $this->add($item);
        }
    }

    /**
     * @param T $element
     */
    public function add(mixed $element): void {
        $this->checkElement($element);
        $this->queue->enqueue($element);
    }

    /**
     * @return T|null
     */
    public function next(): mixed {
        if (count($this->queue) === 0) return null;
        return $this->queue->dequeue();
    }

    public function clear(): void {
        $this->queue = new SplQueue();
    }

    public function count(): int {
        return $this->queue->count();
    }

    public function isEmpty(): bool {
        return $this->queue->isEmpty();
    }

    /**
     * @return list<T>
     */
    public function getAll(): array {
        return iterator_to_array($this->queue);
    }

    public function offsetExists(mixed $offset): bool {
        return isset($this->queue[$offset]);
    }

    public function offsetGet(mixed $offset): mixed {
        return $this->queue[$offset];
    }

    public function offsetSet(mixed $offset, mixed $value): void {
        $this->checkElement($value);
        $this->queue->add($offset, $value);
    }

    public function offsetUnset(mixed $offset): void {
        if (isset($this->queue[$offset])) {
            unset($this->queue[$offset]);
        }
    }

    /** @throws InvalidArgumentException */
    private function checkElement(mixed $element): void {
        if ($this->queueType == self::QUEUE_TYPE_DATA_TYPE) {
            if ($this->typeOrClass !== ($elementDataType = gettype($element))) {
                throw new InvalidArgumentException("Failed to add element to queue, value is of type " . $elementDataType . ", expected value to be type of " . $this->typeOrClass);
            }
        } else if ($this->queueType == self::QUEUE_TYPE_CLASS) {
            if (!is_a($element, $this->typeOrClass)) {
                throw new InvalidArgumentException("Failed to add element to queue, value is not a subclass of " . $this->typeOrClass);
            }
        }
    }

    public static function fromType(mixed $sample, array $queue = []): self {
        return new self(gettype($sample), $queue);
    }

    public static function fromClass(string $class, array $queue = []): self {
        return new self($class, $queue, self::QUEUE_TYPE_CLASS);
    }
}