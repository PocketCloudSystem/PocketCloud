<?php

namespace pocketcloud\setup;

use Closure;

readonly class Question {

    public function __construct(
        private string $key,
        private string $question,
        private bool $canSkipped,
        private array $possibleAnswers,
        private ?string $default,
        private ?string $recommendation,
        private Closure $parser,
        private ?Closure $resultHandler
    ) {}

    public function getKey(): string {
        return $this->key;
    }

    public function getQuestion(): string {
        return $this->question;
    }

    public function isCanSkipped(): bool {
        return $this->canSkipped;
    }

    public function getPossibleAnswers(): array {
        return $this->possibleAnswers;
    }

    public function getDefault(): ?string {
        return $this->default;
    }

    public function getRecommendation(): ?string {
        return $this->recommendation;
    }

    public function getParser(): Closure {
        return $this->parser;
    }

    public function getResultHandler(): ?Closure {
        return $this->resultHandler;
    }
}