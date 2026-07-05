package de.pocketcloud.cloud.console.screen;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.CloudConsole;
import de.pocketcloud.cloud.console.log.cache.LogMessagesCache;

public abstract class Screen {

    abstract public void initialize(CloudConsole console);

    abstract public void handleInput(String input);

    abstract public void tick(long currentTick);

    abstract public void onRemove(long currentTick);

    /**
     * This is being called when the user presses CTRL + C-
     */
    abstract public void onCancel(long currentTick);

    final public Screen printLogCache() {
        LogMessagesCache.print();
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

    final public Screen setPrompt(String prompt) {
        PocketCloud.instance().console().setPrompt(prompt);
        return this;
    }
}