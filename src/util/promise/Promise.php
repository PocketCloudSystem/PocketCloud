<?php

namespace pocketcloud\cloud\util\promise;

use Closure;
use Throwable;

final class Promise {

    private bool $resolved = false;
    private bool $rejected = false;
    private mixed $result = null;

    /** @var Closure[] */
    private array $success = [];

    /** @var Closure[] */
    private array $failure = [];

    public function resolve(mixed $value = null): void {
        if ($this->resolved || $this->rejected) return;

        $this->resolved = true;
        $this->result = $value;

        foreach ($this->success as $handler) {
            $handler($value);
        }

        $this->clear();
    }

    public function reject(mixed $reason = null): void {
        if ($this->resolved || $this->rejected) return;

        $this->rejected = true;
        $this->result = $reason;

        foreach ($this->failure as $handler) {
            $handler($reason);
        }

        $this->clear();
    }

    public function then(Closure $onSuccess): Promise {
        $next = new Promise();

        $handler = function ($value) use ($onSuccess, $next) {
            try {
                $result = $onSuccess($value);

                if ($result instanceof Promise) {
                    $result->then($next->resolve(...))
                        ->failure($next->reject(...));
                } else {
                    $next->resolve($result);
                }
            } catch (Throwable $e) {
                $next->reject($e);
            }
        };

        if ($this->resolved) {
            $handler($this->result);
        } elseif (!$this->rejected) {
            $this->success[] = $handler;
            $this->failure[] = fn($r) => $next->reject($r);
        }

        return $next;
    }

    public function failure(Closure $onFailure): self {
        if ($this->rejected) {
            $onFailure($this->result);
        } elseif (!$this->resolved) {
            $this->failure[] = $onFailure;
        }

        return $this;
    }

    private function clear(): void {
        $this->success = [];
        $this->failure = [];
    }

    public static function all(array $promises): Promise {
        $all = new Promise();
        $results = [];
        $remaining = count($promises);

        if ($remaining === 0) {
            $all->resolve([]);
            return $all;
        }

        foreach ($promises as $i => $promise) {
            $promise
                ->then(function ($value) use (&$results, &$remaining, $i, $all) {
                    $results[$i] = $value;
                    $remaining--;

                    if ($remaining === 0) {
                        ksort($results);
                        $all->resolve($results);
                    }
                })
                ->failure(fn($reason) => $all->reject($reason));
        }

        return $all;
    }
}
