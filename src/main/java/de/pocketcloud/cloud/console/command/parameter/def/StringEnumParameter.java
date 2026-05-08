package de.pocketcloud.cloud.console.command.parameter.def;

import de.pocketcloud.cloud.console.command.parameter.BaseCommandParameter;
import de.pocketcloud.cloud.console.command.exception.ArgumentParseException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StringEnumParameter extends BaseCommandParameter {

    private final List<String> allowedStrings;
    private final boolean caseSensitive;
    private final String typeName;

    public StringEnumParameter(String name, List<String> allowedStrings, boolean caseSensitive, boolean optional, String typeName, String customErrorMessage) {
        super(name, optional, customErrorMessage);
        this.caseSensitive = caseSensitive;
        this.typeName = typeName;
        this.allowedStrings = allowedStrings.stream()
                .map(s -> caseSensitive ? s : s.toLowerCase())
                .collect(Collectors.toList());
    }

    public StringEnumParameter(String name, List<String> allowedStrings, boolean caseSensitive, boolean optional) {
        this(name, allowedStrings, caseSensitive, optional, null, null);
    }

    public StringEnumParameter(String name, boolean optional, String... allowedStrings) {
        this(name, Arrays.asList(allowedStrings), false, optional, null, null);
    }

    @Override
    public String parseValue(String input) throws ArgumentParseException {
        String normalised = caseSensitive ? input : input.toLowerCase();
        if (!allowedStrings.contains(normalised)) throw new ArgumentParseException("Given string is not allowed");
        return normalised;
    }

    @Override
    public List<String> onTabCompleteMatch(String currentArg) {
        if (currentArg.isEmpty()) return List.copyOf(allowedStrings);
        String needle = caseSensitive ? currentArg : currentArg.toLowerCase();
        return allowedStrings.stream().filter(s -> s.contains(needle)).collect(Collectors.toList());
    }

    @Override
    public String getType() {
        if (typeName != null) return typeName;
        if (allowedStrings.size() > 2)
            return allowedStrings.get(0) + "|" + allowedStrings.get(1) + "|...";
        return String.join("|", allowedStrings);
    }

    public List<String> getAllowedStrings() {
        return List.copyOf(allowedStrings);
    }

    public boolean isAllowedString(String s) {
        return allowedStrings.contains(caseSensitive ? s : s.toLowerCase());
    }
}