<?php

namespace pocketcloud\cloud\setup;

use Closure;
use pocketcloud\cloud\console\Console;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\console\log\logger\ILogger;
use pocketcloud\cloud\console\log\output\OutputManager;
use pocketcloud\cloud\console\log\output\SetupOutputHandler;
use pocketcloud\cloud\console\screen\impl\SetupScreen;
use pocketcloud\cloud\console\screen\ScreenManager;
use pocketcloud\cloud\util\TerminalUtils;
use RuntimeException;

abstract class Setup {

    private const string COMMAND_CANCEL = "cancel";
    private const string COMMAND_BACK = "back";

    private static ?Setup $currentSetup = null;

    private string $prefix = "";
    private ?ILogger $logger = null;
    private ?SetupOutputHandler $outputHandler = null;
    private ?Question $currentQuestion = null;
    private int $currentQuestionIndex = -1;
    protected bool $cancelled = false;
    private array $results = [];
    private array $inputs = [];
    private array $questions = [];
    private ?Closure $completionHandler = null;

    private function __construct() {}

    final public function startSetup(): void {
        if (self::$currentSetup !== null) throw new RuntimeException("Another setup is already running");

        self::$currentSetup = $this;
        ScreenManager::getInstance()->setCurrentScreen(new SetupScreen($this));
        TerminalUtils::clearConsole();

        Console::getInstance()->disableHistory();
        Console::getInstance()->setControlCHandler(fn() => $this->cancel());
        Console::getInstance()->setCompletionHandler(function (array $tokens, string $current): array {
            $recommendations = $this->currentQuestion?->getPossibleAnswers() ?? [];
            if (empty($recommendations) || !empty($tokens)) return [];
            $matches = [];
            foreach ($recommendations as $recommendation) {
                if (str_starts_with(strtolower($recommendation), strtolower($current))) {
                    $matches[] = $recommendation;
                }
            }

            return $matches;
        });

        $this->setupOutputHandler();
        $this->initializeLogger();
        $this->onStart($this->logger);
        $this->logSetupInstructions();

        $this->questions = array_values($this->applyQuestions());

        if (empty($this->questions)) {
            $this->endSetup();
            return;
        }

        $this->navigateToQuestion(0);
    }

    private function setupOutputHandler(): void {
        $this->outputHandler = new SetupOutputHandler();
        OutputManager::setHandler($this->outputHandler);
    }

    private function initializeLogger(): void {
        $this->logger = CloudLogger::tmp();
        $this->logger->setFormat("§r{message}");

        $this->outputHandler->addAuthorizedLogger($this->logger);
    }

    final public function onCompletion(Closure $closure): self {
        $this->completionHandler = $closure;
        return $this;
    }

    private function logSetupInstructions(): void {
        $this->logger->info(
            "Type §8'§c" . self::COMMAND_CANCEL . "§8' §rto cancel the setup or " .
            "§8'§e" . self::COMMAND_BACK . "§8' §rto modify previous answers!"
        );
    }

    private function endSetup(): void {
        $this->cleanupOutputHandler();

        ScreenManager::getInstance()->resetScreen();

        $this->currentQuestion = null;
        $this->currentQuestionIndex = -1;
        self::$currentSetup = null;

        $this->handleResults($this->results);

        if ($this->completionHandler !== null) ($this->completionHandler)($this->results);
    }

    private function cleanupOutputHandler(): void {
        if ($this->outputHandler !== null && $this->logger !== null) $this->outputHandler->removeAuthorizedLogger($this->logger);
        OutputManager::reset();
    }

    private function navigateToQuestion(int $targetIndex): void {
        if ($targetIndex < 0) {
            $targetIndex = 0;
        }

        if ($targetIndex >= count($this->questions)) {
            $this->endSetup();
            return;
        }

        $this->currentQuestionIndex = $targetIndex;
        $this->currentQuestion = $this->questions[$targetIndex];
        $this->displayCurrentQuestion();
    }

    private function nextQuestion(bool $back = false): void {
        if ($this->cancelled) return;

        $nextIndex = $this->currentQuestionIndex + ($back ? -1 : 1);
        $this->navigateToQuestion($nextIndex);
    }

    private function displayCurrentQuestion(): void {
        TerminalUtils::clearConsole();

        $this->displayQuestionHeader();
        $this->displayPossibleAnswers();
        $this->displayDefaultValue();
        $this->displayPreviousAnswer();
        $this->displayHelp();

        Console::getInstance()->setPrompt("§8» §b");
    }

    private function displayQuestionHeader(): void {
        $prefix = trim($this->prefix) === "" ? "" : $this->prefix . " §8- ";
        $counter = "§8(§7" . ($this->currentQuestionIndex + 1) . "§8/§7" . count($this->questions) . "§8)";

        $this->logger->info($prefix . "§rQuestion {}: §r{}", $counter, $this->currentQuestion->getQuestion());
    }

    private function displayPossibleAnswers(): void {
        $answers = $this->currentQuestion->getPossibleAnswers();
        if (empty($answers)) return;

        $this->logger->info("Possible answers: §b{}", implode("§8, §b", $answers));

        if (($recommendation = $this->currentQuestion->getRecommendation()) !== null) {
            $this->logger->info("Recommendation: §b{}", $recommendation);
        }
    }

    private function displayDefaultValue(): void {
        if (($default = $this->currentQuestion->getDefaultValueMessage()) !== null) {
            $this->logger->info("Default: §b{}", $default);
        }
    }

    private function displayPreviousAnswer(): void {
        $key = $this->currentQuestion->getId();
        if (!isset($this->results[$key]) || !isset($this->inputs[$key])) return;

        $value = $this->results[$key];
        $displayValue = match (gettype($value)) {
            "boolean" => $value ? "Yes" : "No",
            default => $value
        };

        $this->logger->info("Previous answer: §b{}", $displayValue);
        Console::getInstance()->setInput($this->inputs[$key]);
    }

    private function displayHelp(): void {
        $this->logger->emptyLine();
        $this->logSetupInstructions();
    }

    final public function handleInput(string $input): void {
        if ($this->cancelled) return;
        $command = strtolower(trim($input));

        match ($command) {
            self::COMMAND_CANCEL => $this->cancel(),
            self::COMMAND_BACK => $this->back(),
            default => $this->processAnswer($input)
        };
    }

    private function processAnswer(string $input): void {
        if ($this->validateAndProcessInput($input)) {
            $this->inputs[$this->currentQuestion->getId()] = $input;
            $this->nextQuestion();
        }
    }

    private function validateAndProcessInput(string $input): bool {
        if ($this->canSkipCurrentQuestion() && $input === "") {
            if ($this->currentQuestion->isCanSkipped() && !isset($this->results[$this->currentQuestion->getId()])) $this->storeResult($this->currentQuestion->getDefaultValue());
            return true;
        }

        if ($input === "") return false;

        if (!$this->isValidAnswer($input)) {
            $this->logger->error("Please provide a valid answer!");
            return false;
        }

        $result = $this->parseInput($input, $error);
        if ($result === null) {
            $this->logger->error($error ?? "Please provide a valid answer!");
            return false;
        }

        $this->storeResult($result);
        return true;
    }

    private function canSkipCurrentQuestion(): bool {
        return $this->currentQuestion->isCanSkipped() || isset($this->results[$this->currentQuestion->getId()]);
    }

    private function isValidAnswer(string $input): bool {
        $possibleAnswers = $this->currentQuestion->getPossibleAnswers();
        return empty($possibleAnswers) || in_array($input, $possibleAnswers, true);
    }

    private function parseInput(string $input, ?string &$error = null): mixed {
        return $this->currentQuestion->getParser()($input, $error);
    }

    private function storeResult(mixed $result): void {
        $this->results[$this->currentQuestion->getId()] = $result;
        if (($resultHandler = $this->currentQuestion->getResultHandler()) !== null) $resultHandler($result);
    }

    public function onStart(ILogger $logger): void {}

    public function onCancel(): void {}

    final public function back(): void {
        $this->nextQuestion(true);
    }

    final public function cancel(): void {
        if ($this->cancelled) return;

        $this->logger?->close();
        self::$currentSetup = null;
        $this->cancelled = true;

        $this->onCancel();
        ScreenManager::getInstance()->resetScreen();
        if ($this->completionHandler !== null) ($this->completionHandler)($this->results);
    }

    public function getLogger(): ?ILogger {
        return $this->logger;
    }

    public function getCurrentQuestion(): ?Question {
        return $this->currentQuestion;
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

    public static function new(): static {
        return new static();
    }
}