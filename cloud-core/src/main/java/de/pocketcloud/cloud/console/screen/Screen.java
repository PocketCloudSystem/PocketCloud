package de.pocketcloud.cloud.console.screen;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.CloudConsole;
import de.pocketcloud.cloud.console.log.cache.LogMessagesCache;
import de.pocketcloud.cloud.console.output.OutputHandler;
import de.pocketcloud.cloud.console.output.OutputManager;
import de.pocketcloud.cloud.console.util.InterruptionResult;

import java.util.function.Supplier;

public abstract class Screen {

    abstract public void initialize(CloudConsole console);

    abstract public void handleInput(String input);

    abstract public void tick(long currentTick);

    abstract public void onRemove(long currentTick);

    /**
     * This is being called when the user presses CTRL + C
     */
    abstract public InterruptionResult onCancel(long currentTick);

    final public Screen printLogCache() {
        LogMessagesCache.print();
        return this;
    }

    final public Screen clear() {
        PocketCloud.instance().console().clear();
        return this;
    }

    final public Screen enableCompletion() {
        PocketCloud.instance().console().enableCompletion();
        return this;
    }

    final public Screen disableCompletion() {
        PocketCloud.instance().console().disableCompletion();
        return this;
    }

    final public Screen enableHistory() {
        PocketCloud.instance().console().enableHistory(true);
        return this;
    }

    final public Screen disableHistory() {
        PocketCloud.instance().console().disableHistory();
        return this;
    }

    final public Screen showCursor() {
        PocketCloud.instance().console().showCursor();
        return this;
    }

    final public Screen hideCursor() {
        PocketCloud.instance().console().hideCursor();
        return this;
    }

    final public Screen showTyping() {
        PocketCloud.instance().console().showTyping();
        return this;
    }

    final public Screen hideTyping() {
        PocketCloud.instance().console().hideTyping();
        return this;
    }

    final public Screen showStatus(String... lines) {
        PocketCloud.instance().console().showStatus(lines);
        return this;
    }

    final public Screen hideStatus() {
        PocketCloud.instance().console().hideStatus();
        return this;
    }

    final public Screen setOutputHandler(OutputHandler handler) {
        OutputManager.set(handler);
        return this;
    }

    final public void setInput(String input) {
        PocketCloud.instance().console().setInput(input);
    }

    final public Screen setPrompt(String prompt) {
        PocketCloud.instance().console().setPrompt(prompt);
        return this;
    }

    final public Screen setInterruptionHandler(Supplier<InterruptionResult> handler) {
        PocketCloud.instance().console().setInterruptionHandler(handler);
        return this;
    }

    final public Screen resetOutputManager() {
        OutputManager.reset();
        return this;
    }

    final public Screen resetPrompt() {
        PocketCloud.instance().console().resetPrompt();
        return this;
    }

    final public Screen resetInterruptionHandler() {
        PocketCloud.instance().console().resetInterruptionHandler();
        return this;
    }

    final public Screen restoreAll() {
        resetOutputManager();
        resetPrompt();
        resetInterruptionHandler();
        hideStatus();
        enableCompletion();
        enableHistory();
        return this;
    }
}