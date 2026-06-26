package com.paddykim.platform.portal.catalog;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {

    private final ApplicationRepository applicationRepository;

    public CatalogService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Transactional(readOnly = true)
    public List<CatalogApplicationResponse> listApplications() {
        return applicationRepository.findAll().stream()
                .map(application -> CatalogApplicationResponse.from(application, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public CatalogApplicationResponse getApplication(Long id) {
        Application application = applicationRepository.findWithEnvironmentsById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));

        return CatalogApplicationResponse.from(application, true);
    }

    @Transactional
    public CatalogApplicationResponse createApplication(ApplicationRequest request) {
        String name = trim(request.name());
        if (applicationRepository.existsByName(name)) {
            throw new CatalogValidationException("Application already exists: " + name);
        }

        Application application = applicationRepository.save(new Application(
                name,
                trim(request.description()),
                trim(request.owner()),
                trim(request.repositoryUrl())
        ));

        return CatalogApplicationResponse.from(application, true);
    }

    @Transactional
    public CatalogApplicationResponse updateApplication(Long applicationId, ApplicationRequest request) {
        Application application = findApplication(applicationId);
        String name = trim(request.name());
        applicationRepository.findByName(name)
                .filter(candidate -> !candidate.getId().equals(applicationId))
                .ifPresent(candidate -> {
                    throw new CatalogValidationException("Application already exists: " + name);
                });

        application.update(
                name,
                trim(request.description()),
                trim(request.owner()),
                trim(request.repositoryUrl())
        );

        return CatalogApplicationResponse.from(application, true);
    }

    @Transactional
    public void deleteApplication(Long applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ApplicationNotFoundException(applicationId);
        }

        applicationRepository.deleteById(applicationId);
    }

    @Transactional
    public CatalogApplicationResponse createEnvironment(Long applicationId, ApplicationEnvironmentRequest request) {
        Application application = findApplication(applicationId);
        application.addEnvironment(new ApplicationEnvironment(
                trim(request.environment()),
                trim(request.namespace()),
                trim(request.argocdApplicationName()),
                trim(request.helmValuesPath()),
                trim(request.serviceUrl())
        ));
        applicationRepository.flush();

        return CatalogApplicationResponse.from(application, true);
    }

    @Transactional
    public CatalogApplicationResponse updateEnvironment(
            Long applicationId,
            Long environmentId,
            ApplicationEnvironmentRequest request
    ) {
        Application application = findApplication(applicationId);
        ApplicationEnvironment environment = findEnvironment(application, environmentId);
        environment.update(
                trim(request.environment()),
                trim(request.namespace()),
                trim(request.argocdApplicationName()),
                trim(request.helmValuesPath()),
                trim(request.serviceUrl())
        );

        return CatalogApplicationResponse.from(application, true);
    }

    @Transactional
    public CatalogApplicationResponse deleteEnvironment(Long applicationId, Long environmentId) {
        Application application = findApplication(applicationId);
        ApplicationEnvironment environment = findEnvironment(application, environmentId);
        application.removeEnvironment(environment);

        return CatalogApplicationResponse.from(application, true);
    }

    @Transactional
    public CatalogApplicationResponse createComponent(
            Long applicationId,
            Long environmentId,
            ApplicationComponentRequest request
    ) {
        Application application = findApplication(applicationId);
        ApplicationEnvironment environment = findEnvironment(application, environmentId);
        environment.addComponent(new ApplicationComponent(
                trim(request.name()),
                trim(request.kind()),
                trim(request.deploymentName()),
                trim(request.serviceName()),
                trim(request.imageRepository())
        ));
        applicationRepository.flush();

        return CatalogApplicationResponse.from(application, true);
    }

    @Transactional
    public CatalogApplicationResponse updateComponent(
            Long applicationId,
            Long environmentId,
            Long componentId,
            ApplicationComponentRequest request
    ) {
        Application application = findApplication(applicationId);
        ApplicationComponent component = findComponent(application, environmentId, componentId);
        component.update(
                trim(request.name()),
                trim(request.kind()),
                trim(request.deploymentName()),
                trim(request.serviceName()),
                trim(request.imageRepository())
        );

        return CatalogApplicationResponse.from(application, true);
    }

    @Transactional
    public CatalogApplicationResponse deleteComponent(Long applicationId, Long environmentId, Long componentId) {
        Application application = findApplication(applicationId);
        ApplicationEnvironment environment = findEnvironment(application, environmentId);
        ApplicationComponent component = findComponent(environment, componentId);
        environment.removeComponent(component);

        return CatalogApplicationResponse.from(application, true);
    }

    @Transactional
    public CatalogApplicationResponse upsertManifestMapping(
            Long applicationId,
            Long environmentId,
            Long componentId,
            ApplicationManifestMappingRequest request
    ) {
        Application application = findApplication(applicationId);
        ApplicationComponent component = findComponent(application, environmentId, componentId);
        component.upsertManifestMapping(
                trim(request.manifestRepositoryUrl()),
                trim(request.manifestBranch()),
                trim(request.valuesPath()),
                trim(request.imageTagKey())
        );
        applicationRepository.flush();

        return CatalogApplicationResponse.from(application, true);
    }

    @Transactional
    public CatalogApplicationResponse deleteManifestMapping(Long applicationId, Long environmentId, Long componentId) {
        Application application = findApplication(applicationId);
        ApplicationComponent component = findComponent(application, environmentId, componentId);
        component.removeManifestMapping();

        return CatalogApplicationResponse.from(application, true);
    }

    private Application findApplication(Long applicationId) {
        return applicationRepository.findWithEnvironmentsById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
    }

    private static ApplicationEnvironment findEnvironment(Application application, Long environmentId) {
        return application.getEnvironments().stream()
                .filter(environment -> environment.getId().equals(environmentId))
                .findFirst()
                .orElseThrow(() -> new CatalogResourceNotFoundException("Environment not found: " + environmentId));
    }

    private static ApplicationComponent findComponent(
            Application application,
            Long environmentId,
            Long componentId
    ) {
        return findComponent(findEnvironment(application, environmentId), componentId);
    }

    private static ApplicationComponent findComponent(ApplicationEnvironment environment, Long componentId) {
        return environment.getComponents().stream()
                .filter(component -> component.getId().equals(componentId))
                .findFirst()
                .orElseThrow(() -> new CatalogResourceNotFoundException("Component not found: " + componentId));
    }

    private static String trim(String value) {
        return value.trim();
    }
}
