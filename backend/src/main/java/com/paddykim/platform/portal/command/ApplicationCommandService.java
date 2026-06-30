package com.paddykim.platform.portal.command;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ApplicationCommandService {

    private static final Set<String> PARAMETERIZED_INTENTS = Set.of(
            "OPEN_ARGOCD_APPLICATION_DETAIL",
            "SYNC_ARGOCD_APPLICATION"
    );

    private final ApplicationCommandRuleRepository applicationCommandRuleRepository;

    public ApplicationCommandService(ApplicationCommandRuleRepository applicationCommandRuleRepository) {
        this.applicationCommandRuleRepository = applicationCommandRuleRepository;
    }

    public ApplicationCommandResponse interpret(String command) {
        String normalizedCommand = normalize(command);

        return applicationCommandRuleRepository.findByEnabledTrueOrderByPriorityDescNameAsc().stream()
                .filter(rule -> matches(normalizedCommand, rule))
                .findFirst()
                .map(rule -> responseFor(normalizedCommand, rule))
                .orElseGet(this::unknownResponse);
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

    private ApplicationCommandResponse responseFor(String command, ApplicationCommandRule rule) {
        Map<String, String> parameters = Map.of();
        if (PARAMETERIZED_INTENTS.contains(rule.getIntent())) {
            String applicationName = extractApplicationName(command);
            if (applicationName == null) {
                return new ApplicationCommandResponse(
                        "UNKNOWN",
                        "NONE",
                        "application 이름을 명령문 앞에 입력해주세요. 예: platform-dev 상세 보여줘",
                        0.0,
                        null,
                        null,
                        Map.of()
                );
            }
            parameters = Map.of("applicationName", applicationName);
        }

        return new ApplicationCommandResponse(
                rule.getIntent(),
                rule.getView(),
                rule.getMessage(),
                rule.getConfidence(),
                rule.getResultApiMethod(),
                rule.getResultApiPath(),
                parameters
        );
    }

    private String extractApplicationName(String command) {
        if (command.isBlank()) {
            return null;
        }

        String candidate = command.split(" ")[0]
                .replaceAll("^[\"']|[\"',]$", "")
                .trim();

        return candidate.matches("[a-z0-9](?:[-a-z0-9.]*[a-z0-9])?") ? candidate : null;
    }

    private ApplicationCommandResponse unknownResponse() {
        return new ApplicationCommandResponse(
                "UNKNOWN",
                "NONE",
                "지원하지 않는 명령입니다. application 목록, 등록, 상세 조회 또는 sync 명령을 입력해주세요.",
                0.0,
                null,
                null,
                Map.of()
        );
    }
}
