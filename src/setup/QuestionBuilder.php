<?php

namespace pocketcloud\cloud\setup;

use Closure;

final class QuestionBuilder {

    private Closure $parser;
    private bool $canSkipped = false;
    private array $possibleAnswers = [];
    private ?string $defaultValueMessage = null;
    private mixed $actualDefaultValue = null;
    private ?string $recommendation = null;
    private ?Closure $resultHandler = null;

    public static function builder(string $id, string $question): QuestionBuilder {
        return new self($id, $question);
    }

    public function __construct(
        private readonly string $id,
        private readonly string $question
    ) {
        $this->parser = self::defaultParser();
    }

    public function canSkipped(bool $value): self {
        $this->canSkipped = $value;
        return $this;
    }

    public function possibleAnswers(string|int|float|bool ...$answers): self {
        $this->possibleAnswers = $answers;
        return $this;
    }

    public function default(string $displayDefault, mixed $value): self {
        $this->defaultValueMessage = $displayDefault;
        $this->actualDefaultValue = $value;
        return $this;
    }

    public function recommendation(string $recommendation): self {
        $this->recommendation = $recommendation;
        return $this;
    }

    /**
     * @param Closure(string $input, ?string &$error): mixed $parser
     * @return $this
     * @required
     */
    public function parser(Closure $parser): self {
        $this->parser = $parser;
        return $this;
    }

    public function resultHandler(Closure $handler): self {
        $this->resultHandler = $handler;
        return $this;
    }

    public function build(): Question {
        return new Question(
            $this->id,
            $this->question,
            $this->canSkipped,
            $this->possibleAnswers,
            $this->defaultValueMessage,
            $this->actualDefaultValue,
            $this->recommendation,
            $this->parser,
            $this->resultHandler
        );
    }

    public static function defaultParser(): Closure {
        return fn(string $input): string => $input;
    }
}