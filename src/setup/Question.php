<?php

namespace pocketcloud\cloud\setup;

use Closure;

readonly class Question {

    public function __construct(
        private string $id,
        private string $question,
        private bool $canSkipped,
        private array $possibleAnswers,
        private ?string $defaultValueMessage,
        private mixed $defaultValue,
        private ?string $recommendation,
        private Closure $parser,
        private ?Closure $resultHandler
    ) {}

    public function getId(): string {
        return $this->id;
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

    public function getDefaultValueMessage(): ?string {
        return $this->defaultValueMessage;
    }

    public function getDefaultValue(): mixed {
        return $this->defaultValue;
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