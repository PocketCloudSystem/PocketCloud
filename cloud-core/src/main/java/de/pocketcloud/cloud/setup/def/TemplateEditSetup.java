package de.pocketcloud.cloud.setup.def;

import de.pocketcloud.api.logging.ILogger;
import de.pocketcloud.api.template.util.TemplateEditData;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.setup.Question;
import de.pocketcloud.cloud.setup.QuestionBuilder;
import de.pocketcloud.cloud.setup.Setup;
import de.pocketcloud.cloud.template.Template;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
public final class TemplateEditSetup extends Setup {

    private final Template template;

    @Override
    public void onStart(ILogger logger) {
        setPrefix("§bTemplate-Editing-Setup");
        logger.withoutFormat("Welcome to the Template-Editing-Setup!");
    }

    @Override
    public void onCancel() {
        CloudLogger.get().warn("The template editing was cancelled!");
    }

    @Override
    public List<Question<?>> applyQuestions() {
        return List.of(
                QuestionBuilder.builder("lobby", "Is your template a lobby?")
                        .parser((input, _) -> input.equalsIgnoreCase("yes"))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue(template.settings().lobby() ? "Yes" : "No", template.settings().lobby())
                        .build(),

                QuestionBuilder.builder("maintenance", "Should your template be in maintenance?")
                        .parser((input, _) -> input.equalsIgnoreCase("yes"))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue(template.settings().maintenance() ? "Yes" : "No", template.settings().maintenance())
                        .build(),

                QuestionBuilder.builder("static", "Should your template be static, meaning the servers of that template just have their own data?")
                        .parser((input, _) -> input.equalsIgnoreCase("yes"))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue(template.settings().staticServers() ? "Yes" : "No", template.settings().staticServers())
                        .build(),

                QuestionBuilder.builder("alwaysCopyToStaticServers", "Should your static servers always copy data from their template?")
                        .parser((input, _) -> input.equalsIgnoreCase("yes"))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue(template.settings().alwaysCopyToStaticServers() ? "Yes" : "No", template.settings().alwaysCopyToStaticServers())
                        .build(),

                QuestionBuilder.builder("saveOnShutdown", "Should your static servers always copy data from their template?")
                        .parser((input, _) -> input.equalsIgnoreCase("yes"))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .defaultValue(template.settings().saveOnShutdown() ? "Yes" : "No", template.settings().saveOnShutdown())
                        .build(),

                QuestionBuilder.builder("maxPlayerCount", "How many players are allowed on that template's servers?")
                        .parser((input, _) -> input.matches("\\d+") ? Integer.parseInt(input) : null)
                        .defaultValue(template.settings().maxPlayerCount() + " player" + (template.settings().maxPlayerCount() == 1 ? "" : "s"), template.settings().maxPlayerCount())
                        .canSkipped(true)
                        .build(),

                QuestionBuilder.builder("minServerCount", "How many servers should always be running?")
                        .parser((input, _) -> input.matches("\\d+") ? Integer.parseInt(input) : null)
                        .defaultValue(template.settings().minServerCount() + " server" + (template.settings().minServerCount() == 1 ? "" : "s"), template.settings().minServerCount())
                        .canSkipped(true)
                        .build(),

                QuestionBuilder.builder("maxServerCount", "How many servers can be running in total?")
                        .parser((input, _) -> input.matches("\\d+") ? Integer.parseInt(input) : null)
                        .defaultValue(template.settings().maxServerCount() + " server" + (template.settings().maxServerCount() == 1 ? "" : "s"), template.settings().maxServerCount())
                        .canSkipped(true)
                        .build(),

                QuestionBuilder.builder("startNewPercentage", "How many players are required to start a new server? (in %, 0-100, 0 = none)")
                        .parser((input, _) -> {
                            if (!input.matches("\\d+(\\.\\d+)?")) return null;
                            double value = Double.parseDouble(input);
                            return (value < 0 || value > 100) ? null : value / 100;
                        })
                        .canSkipped(true)
                        .recommendation("75%")
                        .defaultValue((template.settings().startNewPercentage() * 100) + "%", template.settings().startNewPercentage() * 100)
                        .build(),

                QuestionBuilder.builder("autoStart", "Should your template start servers automatically?")
                        .parser((input, _) -> input.equalsIgnoreCase("yes"))
                        .canSkipped(true)
                        .possibleAnswers("yes", "no")
                        .recommendation("yes")
                        .defaultValue(template.settings().autoStart() ? "Yes" : "No", template.settings().autoStart())
                        .build(),

                QuestionBuilder.builder("maxMemory", "How much memory do servers from this template have? §8(§bin Megabytes§8)")
                        .parser((input, _) -> {
                            if (!input.matches("\\d+")) return null;
                            int value = Integer.parseInt(input);
                            return (value < 0) ? null : value;
                        })
                        .canSkipped(true)
                        .recommendation("1024 Megabytes")
                        .defaultValue(String.valueOf(template.settings().maxMemory()), template.settings().maxMemory())
                        .build()
        );
    }

    @Override
    public void handleResults(Map<String, Object> results) {
        PocketCloud.instance().templates().edit(template, TemplateEditData.create()
                .lobby((Boolean) results.get("lobby"))
                .maintenance((Boolean) results.get("maintenance"))
                .staticServers((Boolean) results.get("static"))
                .alwaysCopyToStaticServers((Boolean) results.get("alwaysCopyToStaticServers"))
                .saveOnShutdown((Boolean) results.get("saveOnShutdown"))
                .maxPlayerCount((Integer) results.get("maxPlayerCount"))
                .minServerCount((Integer) results.get("minServerCount"))
                .maxServerCount((Integer) results.get("maxServerCount"))
                .startNewPercentage((Double) results.get("startNewPercentage"))
                .autoStart((Boolean) results.get("autoStart"))
                .maxMemory((Integer) results.get("maxMemory"))
        );
    }
}