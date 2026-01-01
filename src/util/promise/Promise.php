<?php

namespace pocketcloud\cloud\util\promise;

use Closure;

/** @template TValue */
final class Promise {

    private bool $resolved = false;
    private bool $rejected = false;
    private mixed $result = null;

    /** @var Closure[] */
    private array $success = [];

    /** @var Closure[] */
    private array $failure = [];

    /**
     * @param TValue $value
     * @return $this|self
     */
    public function resolve(mixed $value = null): self {
        if ($this->resolved || $this->rejected) return $this;

        $this->resolved = true;
        $this->result = $value;

        foreach ($this->success as $handler) {
            $handler($value);
        }

        return $this->clear();
    }

    public function reject(mixed $reason = null): self {
        if ($this->resolved || $this->rejected) return $this;

        $this->rejected = true;
        $this->result = $reason;

        foreach ($this->failure as $handler) {
            $handler($reason);
        }

        return $this->clear();
    }

    /**
     * @param Closure(TValue $result): void $onSuccess
     * @return Promise
     */
    public function then(Closure $onSuccess): self {
        if ($this->resolved) {
            $onSuccess($this->result);
        } else if (!$this->rejected) {
            $this->success[] = $onSuccess;
        }

        return $this;
    }

    public function failure(Closure $onFailure): self {
        if ($this->rejected) {
            $onFailure($this->result);
        } else if (!$this->resolved) {
            $this->failure[] = $onFailure;
        }

        return $this;
    }

    private function clear(): self {
        $this->success = [];
        $this->failure = [];
        return $this;
    }

    public static function resolved(mixed $result = null): Promise {
        return new Promise()->resolve($result);
    }

    public static function rejected(mixed $reason = null): Promise {
        return new Promise()->reject($reason);
    }

    public static function all(array $promises): Promise {
        $all = new Promise();
        $results = [];
        $remaining = count($promises);

        if ($remaining === 0) {
            $all->resolve([]);
            return $all;
        }

        /**
         * @var int $i
         * @var Promise $promise
         */
        foreach ($promises as $i => $promise) {
            $promise->then(function (mixed $value) use (&$results, &$remaining, $i, $all) {
                $results[$i] = $value;
                $remaining--;

                if ($remaining === 0) {
                    ksort($results);
                    $all->resolve($results);
                }
            })->failure(fn(mixed $reason) => $all->reject($reason));
        }

        return $all;
    }
}