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
                "application-sync",
                "SYNC_ARGOCD_APPLICATION",
                "APPLICATION_DETAIL",
                "ArgoCD application sync를 요청합니다.",
                "sync,동기화",
                "sync,동기화,실행,요청,해줘,해주세요",
                "POST",
                "/api/argocd/applications/{applicationName}/sync",
                0.9,
                130
        ));

        seed(new ApplicationCommandRule(
                "application-detail",
                "OPEN_ARGOCD_APPLICATION_DETAIL",
                "APPLICATION_DETAIL",
                "ArgoCD application 상세 정보를 조회합니다.",
                "상세,정보,detail",
                "상세,정보,조회,보여,열어,알려,detail,show,open",
                "GET",
                "/api/argocd/applications/{applicationName}",
                0.9,
                120
        ));

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
