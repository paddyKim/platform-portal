package com.paddykim.platform.portal.command;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationCommandRuleRepository extends JpaRepository<ApplicationCommandRule, Long> {

    boolean existsByName(String name);

    Optional<ApplicationCommandRule> findByName(String name);

    List<ApplicationCommandRule> findByEnabledTrueOrderByPriorityDescNameAsc();
}
