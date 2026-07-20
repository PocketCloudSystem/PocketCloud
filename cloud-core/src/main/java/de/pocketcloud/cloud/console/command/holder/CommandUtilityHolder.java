package de.pocketcloud.cloud.console.command.holder;

import de.pocketcloud.cloud.console.command.exception.ArgumentParseException;
import de.pocketcloud.cloud.console.command.exception.FlagParseException;
import de.pocketcloud.cloud.console.command.exception.NoArgumentFoundException;
import de.pocketcloud.cloud.console.command.flag.CommandFlag;
import de.pocketcloud.cloud.console.command.flag.CommandLongFlag;
import de.pocketcloud.cloud.console.command.flag.CommandShortFlag;
import de.pocketcloud.cloud.console.command.parameter.BaseCommandParameter;
import de.pocketcloud.cloud.console.command.parameter.def.StringParameter;

import java.util.*;
import java.util.stream.Collectors;

public class CommandUtilityHolder {

    protected final Map<String, CommandFlag> flags = new LinkedHashMap<>();

    public CommandUtilityHolder addFlag(CommandFlag flag) {
        flags.put(flag.getFullFlag(), flag);
        return this;
    }

    public CommandUtilityHolder addFlags(CommandFlag... flags) {
        for (CommandFlag f : flags) addFlag(f);
        return this;
    }

    public Optional<CommandFlag> getFlag(String fullFlag) {
        return Optional.ofNullable(flags.get(fullFlag));
    }

    public Collection<CommandFlag> getFlags() {
        return Collections.unmodifiableCollection(flags.values());
    }

    public ScanResult scanAndCleanFlags(List<String> args, boolean mergeGlobalAndRegularFlags) throws FlagParseException {
        if (flags.isEmpty()) return ScanResult.empty();
        Map<String, Object> globalFlags = new LinkedHashMap<>();
        Map<String, Object> regularFlags = new LinkedHashMap<>();

        Iterator<String> it = args.iterator();
        while (it.hasNext()) {

            String arg = it.next();
            boolean removed = false;

            String[] parts = arg.split("=", 2);
            String flagKey = parts[0];
            if (new CommandLongFlag("x", false, false).isLikelyFlag(arg)) {
                String valuePart = parts.length > 1 ? parts[1] : null;
                String flagName = flagKey.substring(CommandLongFlag.PREFIX.length());
                CommandFlag flag = flags.get(flagKey);
                if (flag != null) {
                    if (flag.isExpectValue()) {
                        if (valuePart == null) throw new FlagParseException("Flag " + flagKey + " expects a value");
                        storeFlag(flag, flagName, valuePart, mergeGlobalAndRegularFlags, globalFlags, regularFlags);
                    } else {
                        storeFlag(flag, flagName, true, mergeGlobalAndRegularFlags, globalFlags, regularFlags);
                    }

                    removed = true;
                }
            } else if (new CommandShortFlag("x", false, false).isLikelyFlag(arg)) {
                String valuePart = parts.length > 1 ? parts[1] : null;
                String flagName = flagKey.substring(CommandShortFlag.PREFIX.length());
                CommandFlag flag = flags.get(flagKey);
                if (flag != null) {
                    if (flag.isExpectValue()) {
                        if (valuePart == null) throw new FlagParseException("Flag " + flagKey + " expects a value");
                        storeFlag(flag, flagName, valuePart, mergeGlobalAndRegularFlags, globalFlags, regularFlags);
                    } else {
                        storeFlag(flag, flagName, true, mergeGlobalAndRegularFlags, globalFlags, regularFlags);
                    }

                    removed = true;
                } else if (valuePart == null && flagName.length() > 1) {
                    // -abc -> -a -b -c
                    boolean anyMatched = false;
                    for (char c : flagName.toCharArray()) {
                        CommandFlag charFlag = flags.get(CommandShortFlag.PREFIX + c);
                        if (charFlag != null) {
                            storeFlag(charFlag, String.valueOf(c), true, mergeGlobalAndRegularFlags, globalFlags, regularFlags);
                            anyMatched = true;
                        }
                    }

                    removed = anyMatched;
                }
            }

            if (removed) {
                it.remove();
            }
        }

        return new ScanResult(globalFlags, regularFlags);
    }

    public ScanResult scanAndCleanFlags(List<String> args) throws FlagParseException {
        return scanAndCleanFlags(args, false);
    }

    private void storeFlag(CommandFlag flag, String name, Object value, boolean merge, Map<String, Object> global, Map<String, Object> regular) {
        if (flag.isGlobal() && !merge) {
            global.put(name, value);
        } else {
            regular.put(name, value);
        }
    }

    protected final List<BaseCommandParameter> parameters = new ArrayList<>();

    public CommandUtilityHolder addParameter(BaseCommandParameter parameter) {
        parameters.add(parameter);
        return this;
    }

    public CommandUtilityHolder addParameter(BaseCommandParameter parameter, int position) {
        parameters.add(position, parameter);
        return this;
    }

    public CommandUtilityHolder addParameters(BaseCommandParameter... params) {
        Collections.addAll(parameters, params);
        return this;
    }

    public Optional<BaseCommandParameter> getParameter(int index) {
        try {
            return Optional.of(parameters.get(index));
        } catch (IndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }

    public List<BaseCommandParameter> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    public boolean isEmpty() {
        return parameters.isEmpty() && flags.isEmpty();
    }

    public long getRequiredCount() {
        return parameters.stream()
                .filter(p -> !p.isOptional())
                .count();
    }

    public long getOptionalCount() {
        return parameters.stream()
                .filter(BaseCommandParameter::isOptional)
                .count();
    }

    public Map<String, Object> parseArgs(List<String> args, LastParsed lastParsed) throws ArgumentParseException, NoArgumentFoundException {
        if (parameters.isEmpty()) return Map.of();
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < parameters.size(); i++) {
            BaseCommandParameter param = parameters.get(i);
            lastParsed.value = param;
            boolean isMultiString = (param instanceof StringParameter sp) && sp.isMultiString();
            if (i < args.size()) {
                String token = isMultiString ? String.join(" ", args.subList(i, args.size())) : args.get(i);
                result.put(param.getName(), param.parseValue(token));
                if (isMultiString) break;
            } else {
                if (!param.isOptional()) throw new NoArgumentFoundException();
            }
        }

        return result;
    }

    public List<String> tabCompleteAt(int argIndex, String currentArg) {
        if (argIndex >= parameters.size()) return List.of();
        return parameters.get(argIndex).onTabCompleteMatch(currentArg);
    }

    public String buildUsageFragment() {
        return parameters.stream()
                .map(p ->
                        p.isOptional()
                                ? "[" + p.getName() + ": " + p.getType() + "]"
                                : "<" + p.getName() + ": " + p.getType() + ">"
                )
                .collect(Collectors.joining(" "));
    }

    public record ScanResult(Map<String, Object> globalFlags, Map<String, Object> regularFlags) {

        static ScanResult empty() {
            return new ScanResult(Map.of(), Map.of());
        }

        public Map<String, Object> merged() {
            Map<String, Object> merged = new LinkedHashMap<>(globalFlags);
            merged.putAll(regularFlags);
            return Collections.unmodifiableMap(merged);
        }
    }

    public static final class LastParsed {
        public BaseCommandParameter value;
    }
}