package com.paddykim.platform.portal.command;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ApplicationCommandService {

    private final ApplicationCommandRuleRepository applicationCommandRuleRepository;

    public ApplicationCommandService(ApplicationCommandRuleRepository applicationCommandRuleRepository) {
        this.applicationCommandRuleRepository = applicationCommandRuleRepository;
    }

    public ApplicationCommandResponse interpret(String command) {
        String normalizedCommand = normalize(command);

        return applicationCommandRuleRepository.findByEnabledTrueOrderByPriorityDescNameAsc().stream()
                .filter(rule -> matches(normalizedCommand, rule))
                .findFirst()
                .map(rule -> new ApplicationCommandResponse(
                        rule.getIntent(),
                        rule.getView(),
                        rule.getMessage(),
                        rule.getConfidence(),
                        rule.getResultApiMethod(),
                        rule.getResultApiPath()
                ))
                .orElseGet(() -> new ApplicationCommandResponse(
                        "UNKNOWN",
                        "NONE",
                        "지원하지 않는 명령입니다. application 목록 조회 또는 등록 명령을 입력해주세요.",
                        0.0,
                        null,
                        null
                ));
    }

    private boolean matches(String command, ApplicationCommandRule rule) {
        return containsAny(command, parseTerms(rule.getTargetTerms()))
                && containsAny(command, parseTerms(rule.getActionTerms()));
    }

    private String normalize(String command) {
        return command == null
                ? ""
                : command.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private List<String> parseTerms(String terms) {
        if (terms == null || terms.isBlank()) {
            return List.of();
        }

        return List.of(terms.split(",")).stream()
                .map(this::normalize)
                .filter(term -> !term.isBlank())
                .toList();
    }

    private boolean containsAny(String command, List<String> terms) {
        return terms.stream().anyMatch(command::contains);
    }
}
