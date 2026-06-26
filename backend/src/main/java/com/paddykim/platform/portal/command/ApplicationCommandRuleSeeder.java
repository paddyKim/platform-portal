package com.paddykim.platform.portal.command;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ApplicationCommandRuleSeeder implements CommandLineRunner {

    private final ApplicationCommandRuleRepository applicationCommandRuleRepository;

    public ApplicationCommandRuleSeeder(ApplicationCommandRuleRepository applicationCommandRuleRepository) {
        this.applicationCommandRuleRepository = applicationCommandRuleRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seed(new ApplicationCommandRule(
                "application-list",
                "LIST_APPLICATIONS",
                "APPLICATION_LIST",
                "등록된 application 목록을 조회합니다.",
                "어플리케이션,애플리케이션,앱,app,application,applications",
                "목록,리스트,조회,보여,알려,list,show",
                "GET",
                "/api/argocd/applications",
                0.9,
                100
        ));

        seed(new ApplicationCommandRule(
                "application-create",
                "OPEN_APPLICATION_CREATE_FORM",
                "APPLICATION_CREATE",
                "application 등록 화면으로 이동합니다.",
                "어플리케이션,애플리케이션,앱,app,application",
                "등록,생성,추가,신규,새로운,create,new,add",
                null,
                null,
                0.9,
                90
        ));
    }

    private void seed(ApplicationCommandRule rule) {
        applicationCommandRuleRepository.findByName(rule.getName())
                .ifPresentOrElse(
                        existingRule -> existingRule.updateFrom(rule),
                        () -> applicationCommandRuleRepository.save(rule)
                );
    }
}
