package com.paddykim.platform.portal.cicd;

import com.paddykim.platform.portal.catalog.Application;
import com.paddykim.platform.portal.catalog.ApplicationComponent;
import com.paddykim.platform.portal.catalog.ApplicationEnvironment;
import com.paddykim.platform.portal.catalog.ApplicationNotFoundException;
import com.paddykim.platform.portal.catalog.ApplicationRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CicdRequestService {

    private static final String DISPATCH_TARGET = "platform-cicd-http";

    private final ApplicationRepository applicationRepository;
    private final CicdRequestRepository cicdRequestRepository;
    private final AuditEventRepository auditEventRepository;

    public CicdRequestService(
            ApplicationRepository applicationRepository,
            CicdRequestRepository cicdRequestRepository,
            AuditEventRepository auditEventRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.cicdRequestRepository = cicdRequestRepository;
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public CicdRequestResponse createRequest(CicdRequestCreateRequest request) {
        Application application = applicationRepository.findWithEnvironmentsById(request.applicationId())
                .orElseThrow(() -> new ApplicationNotFoundException(request.applicationId()));
        ApplicationEnvironment environment = findEnvironment(application, request.environment());
        ApplicationComponent component = findComponent(environment, request.componentId());

        CicdRequest cicdRequest = cicdRequestRepository.save(new CicdRequest(
                application,
                environment,
                component,
                request.requestType(),
                request.requestedValue().trim(),
                request.requestedBy().trim(),
                DISPATCH_TARGET,
                messageKey(application, environment, component)
        ));

        auditEventRepository.save(new AuditEvent(
                cicdRequest,
                AuditEventType.CICD_REQUEST_CREATED,
                cicdRequest.getRequestedBy(),
                messageKey(application, environment, component),
                "%s requested %s=%s".formatted(
                        cicdRequest.getRequestedBy(),
                        cicdRequest.getRequestType(),
                        cicdRequest.getRequestedValue()
                )
        ));

        return CicdRequestResponse.from(cicdRequest);
    }

    @Transactional(readOnly = true)
    public List<CicdRequestResponse> listRequests() {
        return cicdRequestRepository.findAll().stream()
                .sorted(Comparator.comparing(CicdRequest::getCreatedAt).reversed())
                .map(CicdRequestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CicdRequestResponse getRequest(Long id) {
        CicdRequest request = cicdRequestRepository.findById(id)
                .orElseThrow(() -> new CicdRequestNotFoundException(id));

        return CicdRequestResponse.from(request);
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> listAuditEvents() {
        return auditEventRepository.findAll().stream()
                .sorted(Comparator.comparing(AuditEvent::getCreatedAt).reversed())
                .map(AuditEventResponse::from)
                .toList();
    }

    private static ApplicationEnvironment findEnvironment(Application application, String environmentName) {
        return application.getEnvironments().stream()
                .filter(environment -> environment.getEnvironment().equalsIgnoreCase(environmentName))
                .findFirst()
                .orElseThrow(() -> new CicdRequestValidationException(
                        "Environment not found for application %s: %s".formatted(application.getId(), environmentName)
                ));
    }

    private static ApplicationComponent findComponent(ApplicationEnvironment environment, Long componentId) {
        return environment.getComponents().stream()
                .filter(component -> component.getId().equals(componentId))
                .findFirst()
                .orElseThrow(() -> new CicdRequestValidationException(
                        "Component not found for environment %s: %s".formatted(environment.getEnvironment(), componentId)
                ));
    }

    private static String messageKey(
            Application application,
            ApplicationEnvironment environment,
            ApplicationComponent component
    ) {
        return "%s:%s:%s".formatted(application.getName(), environment.getEnvironment(), component.getName());
    }
}
