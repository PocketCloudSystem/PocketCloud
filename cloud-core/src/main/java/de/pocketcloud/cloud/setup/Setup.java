package de.pocketcloud.cloud.setup;

import de.pocketcloud.api.logging.ILogger;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.console.screen.impl.SetupScreen;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class Setup {

    private static final String COMMAND_CANCEL = "cancel";
    private static final String COMMAND_BACK = "back";

    private static Setup currentSetup = null;

    @Setter
    private String prefix = "";
    @Getter
    private ILogger logger;
    @Getter
    private Question<?> currentQuestion;
    private int currentQuestionIndex = -1;
    @Getter
    protected boolean cancelled = false;

    private final Map<String, Object> results = new LinkedHashMap<>();
    private final Map<String, String> inputs = new LinkedHashMap<>();
    private List<Question<?>> questions = List.of();
    private Consumer<Map<String, Object>> completionHandler;

    protected Setup() {}

    public final void startSetup() {
        synchronized (Setup.class) {
            if (currentSetup != null) throw new IllegalStateException("Another setup is already running");
            currentSetup = this;
        }

        PocketCloud.instance().screens().set(new SetupScreen(this));

        this.logger = CloudLogger.tmp();

        onStart(logger);
        logSetupInstructions();

        this.questions = List.copyOf(applyQuestions());

        if (questions.isEmpty()) {
            endSetup();
            return;
        }

        PocketCloud.instance().console().setPrompt("§8» §r");

        navigateToQuestion(0);
    }

    public final Setup onCompletion(Consumer<Map<String, Object>> handler) {
        this.completionHandler = handler;
        return this;
    }

    private void logSetupInstructions() {
        logger.withoutFormat(
                "Type §8'§c" + COMMAND_CANCEL + "§8' §rto cancel the setup or " +
                        "§8'§e" + COMMAND_BACK + "§8' §rto modify previous answers!"
        );
    }

    private void endSetup() {
        PocketCloud.instance().screens().reset();

        this.currentQuestion = null;
        this.currentQuestionIndex = -1;
        synchronized (Setup.class) {
            currentSetup = null;
        }

        handleResults(Map.copyOf(results));

        if (completionHandler != null) completionHandler.accept(Map.copyOf(results));
    }

    private void navigateToQuestion(int targetIndex) {
        if (targetIndex < 0) targetIndex = 0;

        if (targetIndex >= questions.size()) {
            endSetup();
            return;
        }

        this.currentQuestionIndex = targetIndex;
        this.currentQuestion = questions.get(targetIndex);
        displayCurrentQuestion();
    }

    private void nextQuestion(boolean back) {
        if (cancelled) return;
        navigateToQuestion(currentQuestionIndex + (back ? -1 : 1));
    }

    private void displayCurrentQuestion() {
        SetupScreen screen = currentScreen();
        if (screen != null) screen.clear();

        displayQuestionHeader();
        displayPossibleAnswers();
        displayDefaultValue();
        displayPreviousAnswer();
        displayHelp();
    }

    private SetupScreen currentScreen() {
        return PocketCloud.instance().screens().get() instanceof SetupScreen screen ? screen : null;
    }

    private void displayQuestionHeader() {
        String prefixPart = prefix.trim().isEmpty() ? "" : prefix + " §8- ";
        String counter = "§8(§7" + (currentQuestionIndex + 1) + "§8/§7" + questions.size() + "§8)";

        logger.withoutFormat(prefixPart + "§rQuestion {}: §r{}", counter, currentQuestion.getQuestion());
    }

    private void displayPossibleAnswers() {
        List<String> answers = currentQuestion.getPossibleAnswers();
        if (answers.isEmpty()) return;

        logger.withoutFormat("Possible answers: §b{}", String.join("§8, §b", answers));

        if (currentQuestion.getRecommendation() != null) {
            logger.withoutFormat("Recommendation: §b{}", currentQuestion.getRecommendation());
        }
    }

    private void displayDefaultValue() {
        if (currentQuestion.getDefaultValueMessage() != null) {
            logger.withoutFormat("Default: §b{}", currentQuestion.getDefaultValueMessage());
        }
    }

    private void displayPreviousAnswer() {
        String key = currentQuestion.getId();
        if (!results.containsKey(key) || !inputs.containsKey(key)) return;

        Object value = results.get(key);
        String displayValue = value instanceof Boolean bool ? (bool ? "Yes" : "No") : String.valueOf(value);

        logger.withoutFormat("Previous answer: §b{}", displayValue);

        SetupScreen screen = currentScreen();
        if (screen != null) screen.setInput(inputs.get(key));
    }

    private void displayHelp() {
        logger.emptyLine();
        logSetupInstructions();
    }

    public final void handleInput(String input) {
        if (cancelled) return;
        String command = input.trim().toLowerCase();

        switch (command) {
            case COMMAND_CANCEL -> cancel();
            case COMMAND_BACK -> back();
            default -> processAnswer(input);
        }
    }

    private void processAnswer(String input) {
        if (validateAndProcessInput(input)) {
            inputs.put(currentQuestion.getId(), input);
            nextQuestion(false);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean validateAndProcessInput(String input) {
        Question<Object> question = (Question<Object>) currentQuestion;

        if (canSkipCurrentQuestion() && input.isEmpty()) {
            if (question.isCanSkipped() && !results.containsKey(question.getId())) {
                storeResult(question, question.getDefaultValue());
            }
            return true;
        }

        if (input.isEmpty()) return false;

        if (!isValidAnswer(input)) {
            logger.withoutFormat("Please provide a valid answer!");
            return false;
        }

        ErrorHolder error = new ErrorHolder();
        Object result = question.getParser().parse(input, error);
        if (result == null) {
            logger.withoutFormat(error.isPresent() ? error.get() : "Please provide a valid answer!");
            return false;
        }

        storeResult(question, result);
        return true;
    }

    private boolean canSkipCurrentQuestion() {
        return currentQuestion.isCanSkipped() || results.containsKey(currentQuestion.getId());
    }

    private boolean isValidAnswer(String input) {
        List<String> possibleAnswers = currentQuestion.getPossibleAnswers();
        return possibleAnswers.isEmpty() || possibleAnswers.contains(input);
    }

    private void storeResult(Question<Object> question, Object result) {
        results.put(question.getId(), result);
        if (question.getResultHandler() != null) question.getResultHandler().accept(result);
    }

    public void onStart(ILogger logger) {
    }

    public void onCancel() {
    }

    public final void back() {
        nextQuestion(true);
    }

    public final void cancel() {
        if (cancelled) return;

        synchronized (Setup.class) {
            currentSetup = null;
        }

        cancelled = true;

        onCancel();
        PocketCloud.instance().screens().reset();
        if (completionHandler != null) completionHandler.accept(Map.copyOf(results));
    }

    public abstract List<Question<?>> applyQuestions();

    public abstract void handleResults(Map<String, Object> results);

    public static synchronized Setup currentSetup() {
        return currentSetup;
    }
}