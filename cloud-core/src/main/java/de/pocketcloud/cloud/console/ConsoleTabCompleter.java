package de.pocketcloud.cloud.console;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.CommandManager;
import de.pocketcloud.cloud.console.command.TabComplete;
import de.pocketcloud.cloud.console.command.holder.CommandUtilityHolder;
import de.pocketcloud.cloud.console.command.sub.LambdaSubCommand;
import de.pocketcloud.cloud.console.command.sub.SubCommand;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ConsoleTabCompleter implements Completer {

    @Override
    public void complete(LineReader lineReader, ParsedLine parsedLine, List<Candidate> list) {
        final List<String> words = new ArrayList<>(parsedLine.words());
        String current = words.isEmpty() ? "" : words.getLast();

        if (words.isEmpty() || words.size() == 1) {
            list.addAll(completeCommandNames(current).stream().map(Candidate::new).toList());
            return;
        }

        words.removeLast();
        list.addAll(completeCommandArguments(words, current).stream().map(Candidate::new).toList());
    }

    private List<String> completeCommandNames(String current) {
        List<String> matches = new ArrayList<>();
        CommandManager commandManager = PocketCloud.instance().commandManager();

        for (Command cmd : commandManager.getAll()) {
            if (startsWith(cmd.getName(), current)) matches.add(cmd.getName());
        }

        return matches;
    }

    private List<String> completeCommandArguments(List<String> tokens, String current) {
        List<String> copiedTokens = new ArrayList<>(tokens);
        String commandName = copiedTokens.removeFirst();
        Object command = PocketCloud.instance().commandManager().get(commandName).orElse(null);
        if (command == null) return List.of();
        List<String> matches = new ArrayList<>();
        Object originalCommand = command;
        List<String> actualTokens = new ArrayList<>(copiedTokens);

        command = resolveSubCommand(command, commandName, copiedTokens, current, matches);

        ((CommandUtilityHolder) command).getParameter(copiedTokens.size()).ifPresent(param -> matches.addAll(param.onTabCompleteMatch(current)));

        Object usedForCustomTabCompletion = command instanceof LambdaSubCommand ? originalCommand : command;

        if (usedForCustomTabCompletion instanceof TabComplete tabComplete) {
            List<String> customArgs = new ArrayList<>(actualTokens);
            customArgs.add(current);
            matches.addAll(tabComplete.onTabComplete(customArgs));
        }

        return filterMatches(
                matches.stream().distinct().toList(),
                current
        );
    }

    private Object resolveSubCommand(Object originCommand, String commandName, List<String> tokens, String current, List<String> matches) {
        Command command = (Command) originCommand;
        Map<String, SubCommand> subCommands = command.getSubCommands();
        if (subCommands.isEmpty()) return command;
        SubCommand subCommand = command.getSubCommand(commandName);
        if (subCommand != null) return subCommand;

        if (!tokens.isEmpty()) {
            subCommand = command.getSubCommand(tokens.getFirst());
            if (subCommand != null) {
                tokens.removeFirst();
                return subCommand;
            }
        } else {
            for (SubCommand cmd : subCommands.values()) {
                if (startsWith(cmd.getName(), current)) {
                    matches.add(cmd.getName());
                }
            }
        }

        return command;
    }

    private List<String> filterMatches(List<String> matches, String current) {
        return matches.stream()
                .filter(match -> startsWith(match.replace("\"", "").replace("'", ""), current))
                .toList();
    }

    private boolean startsWith(String haystack, String needle) {
        return haystack.toLowerCase().startsWith(needle.toLowerCase());
    }
}