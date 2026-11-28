package de.pocketcloud.cloud.console.command.parameter.def;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/** Free text with tab completion from a list that is read on every completion (ids, file names, nodes, ...). */
public class SuggestingParameter extends StringParameter {

    private final Supplier<Collection<String>> suggestions;

    public SuggestingParameter(String name, boolean optional, Supplier<Collection<String>> suggestions) {
        super(name, optional);
        this.suggestions = suggestions;
    }

    @Override
    public List<String> onTabCompleteMatch(String currentArg) {
        String needle = currentArg.toLowerCase();
        try {
            return suggestions.get().stream().filter(s -> s.toLowerCase().startsWith(needle)).sorted().toList();
        } catch (RuntimeException e) {
            return List.of();
        }
    }
}
