<?php

namespace pocketcloud\cloud\setup;

use Closure;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\terminal\log\color\CloudColor;
use pocketcloud\cloud\terminal\log\logger\Logger;
use pocketcloud\cloud\terminal\log\logger\LoggingCache;
use pocketcloud\cloud\util\terminal\TerminalUtils;

abstract class Setup {

    private static ?Setup $currentSetup = null;

    private string $prefix = "";
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
        TerminalUtils::clear();
        $this->logger = CloudLogger::temp(false);
        $this->logger->setUsePrefix(false);
        $this->onStart($this->logger);
        $this->logger->info("Type §8'§ccancel§8' §rto cancel the setup or §8'§eback§8' §rto modify previous answers!");
        $this->questions = array_values($this->applyQuestions());
        if (count($this->questions) > 0) $this->nextQuestion();
        else $this->endSetup();
    }

    final public function completion(Closure $closure): self {
        $this->completionHandler = $closure;
        return $this;
    }

    private function endSetup(): void {
        TerminalUtils::clear();
        LoggingCache::print();
        $this->currentQuestion = null;
        $this->currentQuestionIndex = -1;
        self::$currentSetup = null;
        $this->handleResults($this->results);
        if ($this->completionHandler !== null) ($this->completionHandler)($this->results);
    }

    private function nextQuestion(bool $back = false): void {
        if ($this->cancelled) return;
        if ($this->currentQuestion === null) {
            $this->currentQuestion = $this->questions[0];
            $this->currentQuestionIndex = 0;
        } else {
            if ($back && $this->currentQuestionIndex > 0) $this->currentQuestionIndex--;
            else if ($back && $this->currentQuestionIndex == 0) $this->currentQuestionIndex = 0;
            else $this->currentQuestionIndex++;

            if (isset($this->questions[$this->currentQuestionIndex])) {
                $this->currentQuestion = $this->questions[$this->currentQuestionIndex];
            } else {
                $this->endSetup();
                return;
            }
        }

        TerminalUtils::clear();
        $this->logger->info($this->prefix . (trim($this->prefix) == "" ? "" : " §8- ") . "§rQuestion §8(§7" . ($this->currentQuestionIndex + 1) . "§8/§7" . count($this->questions) . "§8): §r" . $this->currentQuestion->getQuestion());
        if (count($this->currentQuestion->getPossibleAnswers()) > 0) {
            $this->logger->info("Possible answers: §b" . implode("§8, §b", $this->currentQuestion->getPossibleAnswers()));
            if ($this->currentQuestion->getRecommendation() !== null) $this->logger->info("Recommendation: §b" . $this->currentQuestion->getRecommendation());
        }

        if ($this->currentQuestion->getDefault() !== null) $this->logger->info("Default: §b" . $this->currentQuestion->getDefault());
        if (isset($this->results[$this->currentQuestion->getKey()])) $this->logger->info("Previous answer: §b" . match (gettype($this->results[$this->currentQuestion->getKey()])) {
            "boolean" => $this->results[$this->currentQuestion->getKey()] ? "Yes" : "No",
            default => $this->results[$this->currentQuestion->getKey()]
        });

        $this->logger->emptyLine();
        $this->logger->info("Type §8'§ccancel§8' §rto cancel the setup or §8'§eback§8' §rto modify previous answers!");
        echo CloudColor::toColoredString("§8» §b");
    }

    final public function handleInput(string $input): void {
        if ($this->cancelled) return;

        if (strtolower($input) == "cancel") {
            $this->cancel();
            return;
        }

        if (strtolower($input) == "back") {
            $this->back();
            return;
        }

        $canBeSkipped = $this->currentQuestion->isCanSkipped() || isset($this->results[$this->currentQuestion->getKey()]);
        if ($canBeSkipped && $input == "") {
            $this->nextQuestion();
            return;
        }

        if ($input == "") {
            echo CloudColor::toColoredString("§8» §b");
            return;
        }

        if (($result = $this->checkResult($this->currentQuestion, $input)) !== null) {
            $this->results[$this->currentQuestion->getKey()] = $result;
            $this->nextQuestion();
        }
    }

    public function onStart(Logger $logger): void {}

    public function onCancel(): void {}

    private function checkResult(Question $question, string $line): mixed {
        if (count($question->getPossibleAnswers()) > 0) {
            if (!in_array($line, $question->getPossibleAnswers())) {
                $this->logger->error("Please provide a valid answer!");
                return null;
            }
        }

        $result = $question->getParser()($line);
        if ($result === null) {
            $this->logger->error("Please provide a valid answer!");
            return null;
        }

        if ($question->getResultHandler() !== null) ($question->getResultHandler())($result);
        return $result;
    }

    final public function back(): void {
        $this->nextQuestion(true);
    }

    final public function cancel(): void {
        if ($this->cancelled) return;
        $this->logger?->close();
        self::$currentSetup = null;
        $this->cancelled = true;
        $this->onCancel();
        TerminalUtils::clear();
        LoggingCache::print();
        if ($this->completionHandler !== null) ($this->completionHandler)($this->results);
    }

    public function getLogger(): ?Logger {
        return $this->logger;
    }

    public function isCancelled(): bool {
        return $this->cancelled;
    }

    public function setPrefix(string $prefix): void {
        $this->prefix = $prefix;
    }

    /** @return array<Question> */
    abstract public function applyQuestions(): array;

    abstract public function handleResults(array $results): void;

    public static function getCurrentSetup(): ?Setup {
        return self::$currentSetup;
    }
}