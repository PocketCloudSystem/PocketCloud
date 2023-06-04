<?php

namespace pocketcloud\setup;

use JetBrains\PhpStorm\Pure;

class QuestionBuilder {

    private ?string $key = null;
    private ?string $question = null;
    private bool $canSkipped = false;
    private array $possibleAnswers = [];
    private mixed $default = null;
    private ?string $recommendation = null;
    private ?\Closure $parser = null;
    private ?\Closure $resultHandler = null;

    #[Pure] public static function builder(): QuestionBuilder {
        return new self();
    }

    public function key(string $key): self {
        $this->key = $key;
        return $this;
    }

    public function question(string $question): self {
        $this->question = $question;
        return $this;
    }

    public function canSkipped(bool $value): self {
        $this->canSkipped = $value;
        return $this;
    }

    public function possibleAnswers(string|int|float|bool ...$answers): self {
        $this->possibleAnswers = $answers;
        return $this;
    }

    public function default(string $default): self {
        $this->default = $default;
        return $this;
    }

    public function recommendation(string $recommendation): self {
        $this->recommendation = $recommendation;
        return $this;
    }

    public function parser(\Closure $parser): self {
        $this->parser = $parser;
        return $this;
    }

    public function resultHandler(\Closure $handler): self {
        $this->resultHandler = $handler;
        return $this;
    }

    public function build(): Question {
        assert($this->key !== null);
        assert($this->question !== null);
        assert($this->parser !== null);
        return new Question(
            $this->key,
            $this->question,
            $this->canSkipped,
            $this->possibleAnswers,
            $this->default,
            $this->recommendation,
            $this->parser,
            $this->resultHandler
        );
    }
}