<?php

namespace pocketcloud\setup;

use Closure;
use pocketcloud\console\log\CloudLogSaver;
use pocketcloud\console\log\Logger;
use pocketcloud\language\Language;
use pocketcloud\util\Utils;

abstract class Setup {

    private static ?Setup $currentSetup = null;

    private ?Logger $logger = null;
    private ?Question $currentQuestion = null;
    private int $currentQuestionIndex = -1;
    protected bool $cancelled = false;
    private array $results = [];
    private array $questions = [];
    private ?Closure $completionHandler = null;

    final public function startSetup(): void {
        if (self::$currentSetup !== null) return;
        self::$currentSetup = $this;
        Utils::clearConsole();
        $this->logger = new Logger(setupMode: true);
        $this->onStart();
        $this->logger->info(Language::current()->translate("setup.cancel"));
        $this->questions = array_values($this->applyQuestions());
        if (count($this->questions) > 0) $this->nextQuestion();
        else $this->endSetup();
    }

    final public function completion(Closure $closure): self {
        $this->completionHandler = $closure;
        return $this;
    }

    private function endSetup(): void {
        Utils::clearConsole();
        CloudLogSaver::print();
        $this->currentQuestion = null;
        $this->currentQuestionIndex = -1;
        self::$currentSetup = null;
        $this->handleResults($this->results);
        if ($this->completionHandler !== null) ($this->completionHandler)($this->results);
    }

    private function nextQuestion(): void {
        if ($this->cancelled) return;
        if ($this->currentQuestion === null) {
            $this->currentQuestion = $this->questions[0];
            $this->currentQuestionIndex = 0;
        } else {
            $this->currentQuestionIndex++;
            if (isset($this->questions[$this->currentQuestionIndex])) {
                $this->currentQuestion = $this->questions[$this->currentQuestionIndex];
            } else {
                $this->endSetup();
                return;
            }
        }

        $this->logger->info(Language::current()->translate($this->currentQuestion->getQuestion()) . ($this->currentQuestion->getDefault() !== null ? " " . Language::current()->translate("setup.default.value", $this->currentQuestion->getDefault()) : ""));
        if (count($this->currentQuestion->getPossibleAnswers()) > 0) {
            $this->logger->info(Language::current()->translate("setup.possible.answers", implode(", ", $this->currentQuestion->getPossibleAnswers())) . ($this->currentQuestion->getRecommendation() !== null ? " " . Language::current()->translate("setup.possible.answers.recommendation", $this->currentQuestion->getRecommendation()) : ""));
        }
    }

    final public function handleInput(string $input): void {
        if ($this->cancelled) return;

        if (strtolower($input) == "cancel") {
            $this->cancel();
            return;
        }

        if ($this->currentQuestion->isCanSkipped() && $input == "") {
            $this->nextQuestion();
            return;
        }

        if ($input == "") return;

        if (($result = $this->checkResult($this->currentQuestion, $input)) !== null) {
            $this->results[$this->currentQuestion->getKey()] = $result;
            $this->nextQuestion();
        }
    }

    public function onStart(): void {}

    public function onCancel(): void {}

    private function checkResult(Question $question, string $line): mixed {
        if (count($question->getPossibleAnswers()) > 0) {
            if (!in_array($line, $question->getPossibleAnswers())) {
                $this->logger->error(Language::current()->translate("setup.input.invalid"));
                return null;
            }
        }

        $result = $question->getParser()($line);
        if ($result === null) {
            $this->logger->error(Language::current()->translate("setup.input.invalid"));
            return null;
        }

        if ($question->getResultHandler() !== null) ($question->getResultHandler())($result);
        return $result;
    }

    final public function cancel(): void {
        if ($this->cancelled) return;
        $this->logger?->setSetupMode(false);
        $this->logger?->setSaveMode(true);
        self::$currentSetup = null;
        $this->cancelled = true;
        $this->onCancel();
        Utils::clearConsole();
        CloudLogSaver::print();
        if ($this->completionHandler !== null) ($this->completionHandler)($this->results);
    }

    public function getLogger(): ?Logger {
        return $this->logger;
    }

    public function isCancelled(): bool {
        return $this->cancelled;
    }

    /** @return array<Question> */
    abstract public function applyQuestions(): array;

    abstract public function handleResults(array $results): void;

    public static function getCurrentSetup(): ?Setup {
        return self::$currentSetup;
    }
}