<?php

namespace pocketcloud\setup;

use Closure;

class Question {

    public function __construct(
        private readonly string   $key,
        private readonly string   $question,
        private readonly bool     $canSkipped,
        private readonly array    $possibleAnswers,
        private readonly ?string  $default,
        private readonly ?string  $recommendation,
        private readonly Closure  $parser,
        private readonly ?Closure $resultHandler
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