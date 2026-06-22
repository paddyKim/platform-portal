import { useEffect, useMemo, useState } from 'react'
import './App.css'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'

async function fetchJson(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, options)
  if (!response.ok) {
    throw new Error(`API returned ${response.status}`)
  }

  return response.json()
}

function App() {
  const [apiStatus, setApiStatus] = useState('Checking API')
  const [lastCheckedAt, setLastCheckedAt] = useState('')
  const [applications, setApplications] = useState([])
  const [selectedApplicationId, setSelectedApplicationId] = useState(null)
  const [activeSection, setActiveSection] = useState('cicd')
  const [applicationDetail, setApplicationDetail] = useState(null)
  const [sourceRepositories, setSourceRepositories] = useState([])
  const [sourceRepositoryView, setSourceRepositoryView] = useState('list')
  const [environmentStatuses, setEnvironmentStatuses] = useState({})
  const [runtimeStatuses, setRuntimeStatuses] = useState({})
  const [cicdRequests, setCicdRequests] = useState([])
  const [auditEvents, setAuditEvents] = useState([])
  const [sourceRepositoryForm, setSourceRepositoryForm] = useState({
    name: '',
    provider: 'GITHUB',
    visibility: 'PUBLIC',
    repositoryUrl: '',
    apiBaseUrl: 'https://api.github.com',
    accountName: '',
    accessToken: '',
    description: '',
  })
  const [cicdForm, setCicdForm] = useState({
    environment: '',
    componentId: '',
    requestType: 'BUILD_IMAGE',
    requestedValue: '',
    requestedBy: 'platform-operator',
  })
  const [catalogState, setCatalogState] = useState('loading')
  const [detailState, setDetailState] = useState('idle')
  const [statusState, setStatusState] = useState('idle')
  const [runtimeState, setRuntimeState] = useState('idle')
  const [cicdState, setCicdState] = useState('idle')
  const [sourceRepositoryState, setSourceRepositoryState] = useState('loading')
  const [sourceRepositoryMessage, setSourceRepositoryMessage] = useState('')
  const [cicdMessage, setCicdMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    let ignore = false

    async function loadHealth() {
      try {
        const health = await fetchJson('/api/health')
        if (!ignore) {
          setApiStatus(`${health.service}: ${health.status}`)
          setLastCheckedAt(new Date(health.timestamp).toLocaleString())
        }
      } catch (error) {
        if (!ignore) {
          setApiStatus(error.message)
          setLastCheckedAt(new Date().toLocaleString())
        }
      }
    }

    loadHealth()

    return () => {
      ignore = true
    }
  }, [])

  useEffect(() => {
    let ignore = false

    async function loadApplications() {
      setCatalogState('loading')
      setErrorMessage('')

      try {
        const data = await fetchJson('/api/applications')
        if (!ignore) {
          setApplications(data)
          setSelectedApplicationId(data[0]?.id ?? null)
          setCatalogState(data.length > 0 ? 'ready' : 'empty')
        }
      } catch (error) {
        if (!ignore) {
          setErrorMessage(error.message)
          setCatalogState('error')
        }
      }
    }

    loadApplications()

    return () => {
      ignore = true
    }
  }, [])

  useEffect(() => {
    if (!selectedApplicationId) {
      setApplicationDetail(null)
      setEnvironmentStatuses({})
      setRuntimeStatuses({})
      setDetailState('idle')
      return
    }

    let ignore = false

    async function loadApplicationDetail() {
      setDetailState('loading')
      setErrorMessage('')

      try {
        const data = await fetchJson(`/api/applications/${selectedApplicationId}`)
        if (!ignore) {
          setApplicationDetail(data)
          setEnvironmentStatuses({})
          setRuntimeStatuses({})
          setDetailState('ready')
        }
      } catch (error) {
        if (!ignore) {
          setApplicationDetail(null)
          setErrorMessage(error.message)
          setDetailState('error')
        }
      }
    }

    loadApplicationDetail()

    return () => {
      ignore = true
    }
  }, [selectedApplicationId])

  useEffect(() => {
    let ignore = false

    async function loadSourceRepositories() {
      setSourceRepositoryState('loading')
      setSourceRepositoryMessage('')

      try {
        const data = await fetchJson('/api/source-repositories')
        if (!ignore) {
          setSourceRepositories(data)
          setSourceRepositoryState('ready')
        }
      } catch (error) {
        if (!ignore) {
          setSourceRepositoryMessage(error.message)
          setSourceRepositoryState('error')
        }
      }
    }

    loadSourceRepositories()

    return () => {
      ignore = true
    }
  }, [])

  useEffect(() => {
    let ignore = false

    async function loadCicdData() {
      setCicdState('loading')

      try {
        const [requests, events] = await Promise.all([
          fetchJson('/api/cicd/requests'),
          fetchJson('/api/audit-events'),
        ])

        if (!ignore) {
          setCicdRequests(requests)
          setAuditEvents(events)
          setCicdState('ready')
        }
      } catch (error) {
        if (!ignore) {
          setCicdMessage(error.message)
          setCicdState('error')
        }
      }
    }

    loadCicdData()

    return () => {
      ignore = true
    }
  }, [])

  useEffect(() => {
    if (!applicationDetail) {
      setEnvironmentStatuses({})
      setStatusState('idle')
      return
    }

    let ignore = false

    async function loadEnvironmentStatuses() {
      setStatusState('loading')

      const statusEntries = await Promise.all(
        applicationDetail.environments.map(async (environment) => {
          try {
            const status = await fetchJson(
              `/api/applications/${applicationDetail.id}/environments/${environment.environment}/status`,
            )
            return [environment.environment, status]
          } catch (error) {
            return [
              environment.environment,
              {
                applicationId: applicationDetail.id,
                applicationName: applicationDetail.name,
                environment: environment.environment,
                argocdApplicationName: environment.argocdApplicationName,
                connectionStatus: 'UNAVAILABLE',
                syncStatus: 'Unknown',
                healthStatus: 'Unknown',
                operationPhase: 'Unknown',
                reconciledAt: null,
                images: [],
                message: error.message,
              },
            ]
          }
        }),
      )

      if (!ignore) {
        setEnvironmentStatuses(Object.fromEntries(statusEntries))
        setStatusState('ready')
      }
    }

    loadEnvironmentStatuses()

    return () => {
      ignore = true
    }
  }, [applicationDetail])

  useEffect(() => {
    if (!applicationDetail) {
      setCicdForm((current) => ({
        ...current,
        environment: '',
        componentId: '',
      }))
      return
    }

    const environment = applicationDetail.environments[0]
    setCicdForm((current) => ({
      ...current,
      environment: environment?.environment ?? '',
      componentId: environment?.components[0]?.id?.toString() ?? '',
    }))
  }, [applicationDetail])

  useEffect(() => {
    if (!applicationDetail) {
      setRuntimeStatuses({})
      setRuntimeState('idle')
      return
    }

    let ignore = false

    async function loadRuntimeStatuses() {
      setRuntimeState('loading')

      const runtimeEntries = await Promise.all(
        applicationDetail.environments.map(async (environment) => {
          try {
            const runtime = await fetchJson(
              `/api/applications/${applicationDetail.id}/environments/${environment.environment}/runtime`,
            )
            return [environment.environment, runtime]
          } catch (error) {
            return [
              environment.environment,
              {
                applicationId: applicationDetail.id,
                applicationName: applicationDetail.name,
                environment: environment.environment,
                namespace: environment.namespace,
                connectionStatus: 'UNAVAILABLE',
                summary: {
                  desiredReplicas: 0,
                  readyReplicas: 0,
                  availableReplicas: 0,
                  warningEvents: 0,
                },
                components: [],
                message: error.message,
              },
            ]
          }
        }),
      )

      if (!ignore) {
        setRuntimeStatuses(Object.fromEntries(runtimeEntries))
        setRuntimeState('ready')
      }
    }

    loadRuntimeStatuses()

    return () => {
      ignore = true
    }
  }, [applicationDetail])

  const selectedApplication = useMemo(
    () => applications.find((application) => application.id === selectedApplicationId),
    [applications, selectedApplicationId],
  )

  async function reloadCicdData() {
    const [requests, events] = await Promise.all([
      fetchJson('/api/cicd/requests'),
      fetchJson('/api/audit-events'),
    ])
    setCicdRequests(requests)
    setAuditEvents(events)
  }

  async function reloadSourceRepositories() {
    const repositories = await fetchJson('/api/source-repositories')
    setSourceRepositories(repositories)
  }

  async function handleCreateSourceRepository(event) {
    event.preventDefault()
    setSourceRepositoryState('submitting')
    setSourceRepositoryMessage('')

    try {
      await fetchJson('/api/source-repositories', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(sourceRepositoryForm),
      })
      setSourceRepositoryForm({
        name: '',
        provider: 'GITHUB',
        visibility: 'PUBLIC',
        repositoryUrl: '',
        apiBaseUrl: 'https://api.github.com',
        accountName: '',
        accessToken: '',
        description: '',
      })
      setSourceRepositoryView('list')
      await reloadSourceRepositories()
      setSourceRepositoryMessage('Source repository registered')
      setSourceRepositoryState('ready')
    } catch (error) {
      setSourceRepositoryMessage(error.message)
      setSourceRepositoryState('error')
    }
  }

  async function handleDeleteSourceRepository(repositoryId) {
    const confirmed = window.confirm('Delete this source repository?')
    if (!confirmed) {
      return
    }

    setSourceRepositoryState('submitting')
    setSourceRepositoryMessage('')

    try {
      const response = await fetch(`${API_BASE_URL}/api/source-repositories/${repositoryId}`, {
        method: 'DELETE',
      })
      if (!response.ok) {
        throw new Error(`API returned ${response.status}`)
      }
      await reloadSourceRepositories()
      setSourceRepositoryMessage('Source repository deleted')
      setSourceRepositoryState('ready')
    } catch (error) {
      setSourceRepositoryMessage(error.message)
      setSourceRepositoryState('error')
    }
  }

  async function handleCreateCicdRequest(event) {
    event.preventDefault()
    if (!applicationDetail) {
      return
    }

    setCicdState('submitting')
    setCicdMessage('')

    try {
      await fetchJson('/api/cicd/requests', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          applicationId: applicationDetail.id,
          environment: cicdForm.environment,
          componentId: Number(cicdForm.componentId),
          requestType: cicdForm.requestType,
          requestedValue: cicdForm.requestedValue,
          requestedBy: cicdForm.requestedBy,
        }),
      })
      setCicdForm((current) => ({
        ...current,
        requestedValue: '',
      }))
      await reloadCicdData()
      setCicdMessage('CI/CD request recorded')
      setCicdState('ready')
    } catch (error) {
      setCicdMessage(error.message)
      setCicdState('error')
    }
  }

  return (
    <main className="portal-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">PaaS Application Management</p>
          <h1>Platform Portal</h1>
        </div>
        <div className="api-state">
          <span className="status-dot" />
          <span>{apiStatus}</span>
        </div>
      </header>

      <section className="overview">
        <div>
          <h2>Application Catalog</h2>
          <p>
            Registered workloads, CI/CD requests, and runtime state managed by
            the portal. Build and deployment execution will be delegated to the
            platform-cicd backend.
          </p>
        </div>
        <dl className="runtime-facts">
          <div>
            <dt>API</dt>
            <dd>{API_BASE_URL}</dd>
          </div>
          <div>
            <dt>Last check</dt>
            <dd>{lastCheckedAt || 'Pending'}</dd>
          </div>
        </dl>
      </section>

      {errorMessage && (
        <section className="notice" role="alert">
          {errorMessage}
        </section>
      )}

      <nav className="portal-nav" aria-label="Portal sections">
        <button
          className={activeSection === 'cicd' ? 'active' : ''}
          onClick={() => setActiveSection('cicd')}
          type="button"
        >
          CI/CD
        </button>
        <button
          className={activeSection === 'apps' ? 'active' : ''}
          onClick={() => setActiveSection('apps')}
          type="button"
        >
          App Management
        </button>
      </nav>

      {activeSection === 'cicd' && (
        <section className="cicd-workspace" aria-label="CI/CD workspace">
          {renderSourceRepositoryPanel(
            sourceRepositories,
            sourceRepositoryState,
            sourceRepositoryMessage,
            sourceRepositoryView,
            setSourceRepositoryView,
            sourceRepositoryForm,
            setSourceRepositoryForm,
            handleCreateSourceRepository,
            handleDeleteSourceRepository,
          )}

          {detailState === 'loading' && <p className="muted">Loading CI/CD context...</p>}
          {detailState === 'ready' && applicationDetail && (
            <section className="detail-panel cicd-detail-panel" aria-label="CI/CD request detail">
              <header className="detail-header">
                <div>
                  <p className="eyebrow">Pipeline Requests</p>
                  <h2>{applicationDetail.name}</h2>
                  <p>
                    Build and deployment requests for the selected application.
                    The portal records requests and audit history; execution is
                    delegated to platform-cicd in the next integration step.
                  </p>
                </div>
                <a href={applicationDetail.repositoryUrl} rel="noreferrer" target="_blank">
                  Repository
                </a>
              </header>

              <div className="metadata-grid compact">
                {renderMetadata('Owner', applicationDetail.owner)}
                {renderMetadata('Repository', applicationDetail.repositoryUrl)}
                {renderMetadata('Selected App', selectedApplication?.name || applicationDetail.name)}
                {renderMetadata('Request Target', 'platform-cicd')}
              </div>

              {renderCicdControlPanel(
                applicationDetail,
                cicdForm,
                setCicdForm,
                handleCreateCicdRequest,
                cicdRequests,
                auditEvents,
                cicdState,
                cicdMessage,
              )}
            </section>
          )}
        </section>
      )}

      {activeSection === 'apps' && (
        <section className="catalog-layout">
          <aside className="catalog-panel" aria-label="Applications">
            {renderApplicationPanel(
            applications,
            selectedApplicationId,
            setSelectedApplicationId,
            catalogState,
          )}
          </aside>

          <section className="detail-panel" aria-label="Application detail">
            {detailState === 'idle' && <p className="muted">Select an application.</p>}
            {detailState === 'loading' && <p className="muted">Loading application detail...</p>}
            {detailState === 'error' && <p className="muted">Application detail is unavailable.</p>}

            {detailState === 'ready' && applicationDetail && (
            <>
              <header className="detail-header">
                <div>
                  <p className="eyebrow">Application Management</p>
                  <h2>{applicationDetail.name}</h2>
                  <p>
                    Live cluster view for the selected application. Sync, health,
                    runtime, and component details are read from ArgoCD and
                    Kubernetes status APIs.
                  </p>
                </div>
                <a href={applicationDetail.repositoryUrl} rel="noreferrer" target="_blank">
                  Repository
                </a>
              </header>

              <div className="metadata-grid">
                {renderMetadata('Owner', applicationDetail.owner)}
                {renderMetadata('Repository', applicationDetail.repositoryUrl)}
                {renderMetadata('Environments', applicationDetail.environments.length)}
                {renderMetadata('Selected', selectedApplication?.name || applicationDetail.name)}
              </div>

              <div className="environment-stack">
                {applicationDetail.environments.map((environment) => (
                  <article className="environment-card" key={environment.id}>
                    <div className="environment-heading">
                      <div>
                        <p className="eyebrow">Environment</p>
                        <h3>{environment.environment}</h3>
                      </div>
                      <span>{environment.namespace}</span>
                    </div>

                    <div className="metadata-grid compact">
                      {renderMetadata('Namespace', environment.namespace)}
                      {renderMetadata('ArgoCD App', environment.argocdApplicationName)}
                      {renderMetadata('Values Path', environment.helmValuesPath)}
                      {renderMetadata('Service URL', environment.serviceUrl)}
                    </div>

                    {renderArgoCdStatus(environmentStatuses[environment.environment], statusState)}

                    {renderRuntimeStatus(runtimeStatuses[environment.environment], runtimeState)}

                    <div className="component-table" role="table" aria-label={`${environment.environment} components`}>
                      <div className="component-row component-head" role="row">
                        <span role="columnheader">Component</span>
                        <span role="columnheader">Kind</span>
                        <span role="columnheader">Deployment</span>
                        <span role="columnheader">Service</span>
                        <span role="columnheader">Image</span>
                      </div>
                      {environment.components.map((component) => (
                        <div className="component-row" key={component.id} role="row">
                          <span role="cell">{component.name}</span>
                          <span role="cell">{component.kind}</span>
                          <span role="cell">{component.deploymentName}</span>
                          <span role="cell">{component.serviceName}</span>
                          <span role="cell">{component.imageRepository}</span>
                        </div>
                      ))}
                    </div>
                  </article>
                ))}
              </div>
            </>
          )}
          </section>
        </section>
      )}
    </main>
  )
}

function renderApplicationPanel(applications, selectedApplicationId, setSelectedApplicationId, catalogState) {
  return (
    <>
      <div className="panel-heading">
        <h3>Live Applications</h3>
        <span>{applications.length}</span>
      </div>

      {catalogState === 'loading' && <p className="muted">Loading live applications...</p>}
      {catalogState === 'empty' && <p className="muted">No applications discovered.</p>}

      {catalogState === 'ready' && (
        <div className="application-list">
          {applications.map((application) => (
            <button
              className={application.id === selectedApplicationId ? 'application-item active' : 'application-item'}
              key={application.id}
              onClick={() => setSelectedApplicationId(application.id)}
              type="button"
            >
              <span>{application.name}</span>
              <small>{application.environments.length} environment</small>
            </button>
          ))}
        </div>
      )}
    </>
  )
}

function renderSourceRepositoryPanel(
  sourceRepositories,
  sourceRepositoryState,
  sourceRepositoryMessage,
  sourceRepositoryView,
  setSourceRepositoryView,
  sourceRepositoryForm,
  setSourceRepositoryForm,
  handleCreateSourceRepository,
  handleDeleteSourceRepository,
) {
  const isGitLab = sourceRepositoryForm.provider === 'GITLAB'
  const tokenLabel = isGitLab ? 'GitLab password / access token' : 'GitHub password / access token'
  const apiBaseLabel = isGitLab ? 'GitLab API URL' : 'GitHub API URL'

  return (
    <section className="source-repository-panel">
      <div className="source-repository-header">
        <div>
          <p className="eyebrow">CI/CD Sources</p>
          <h2>Source repositories</h2>
          <p>
            Repositories registered for build and deployment workflows. Each
            source becomes the entry point for future pipeline execution.
          </p>
        </div>
        <div className="source-repository-actions">
          <span>{sourceRepositories.length} repos</span>
          <button
            className="panel-action"
            onClick={() => setSourceRepositoryView(sourceRepositoryView === 'register' ? 'list' : 'register')}
            type="button"
          >
            {sourceRepositoryView === 'register' ? '목록' : '등록'}
          </button>
        </div>
      </div>

      {sourceRepositoryMessage && (
        <p className={sourceRepositoryState === 'error' ? 'status-message' : 'success-message'}>
          {sourceRepositoryMessage}
        </p>
      )}

      {sourceRepositoryView === 'register' && (
        <form className="source-repo-form" onSubmit={handleCreateSourceRepository}>
          <label>
            <span>Repository host</span>
            <select
              onChange={(event) => {
                const provider = event.target.value
                setSourceRepositoryForm((current) => ({
                  ...current,
                  provider,
                  apiBaseUrl: provider === 'GITLAB' ? 'https://gitlab.com/api/v4' : 'https://api.github.com',
                }))
              }}
              value={sourceRepositoryForm.provider}
            >
              <option value="GITHUB">GitHub</option>
              <option value="GITLAB">GitLab</option>
            </select>
          </label>
          <label>
            <span>Visibility</span>
            <select
              onChange={(event) => setSourceRepositoryForm((current) => ({
                ...current,
                visibility: event.target.value,
              }))}
              value={sourceRepositoryForm.visibility}
            >
              <option value="PUBLIC">Public</option>
              <option value="PRIVATE">Private</option>
            </select>
          </label>
          <label>
            <span>Name</span>
            <input
              onChange={(event) => setSourceRepositoryForm((current) => ({ ...current, name: event.target.value }))}
              required
              type="text"
              value={sourceRepositoryForm.name}
            />
          </label>
          <label className="wide-field">
            <span>Repository URL</span>
            <input
              onChange={(event) => setSourceRepositoryForm((current) => ({
                ...current,
                repositoryUrl: event.target.value,
              }))}
              required
              type="url"
              value={sourceRepositoryForm.repositoryUrl}
            />
          </label>
          <label className="wide-field">
            <span>{apiBaseLabel}</span>
            <input
              onChange={(event) => setSourceRepositoryForm((current) => ({
                ...current,
                apiBaseUrl: event.target.value,
              }))}
              required
              type="url"
              value={sourceRepositoryForm.apiBaseUrl}
            />
          </label>
          <label>
            <span>Account</span>
            <input
              onChange={(event) => setSourceRepositoryForm((current) => ({
                ...current,
                accountName: event.target.value,
              }))}
              required
              type="text"
              value={sourceRepositoryForm.accountName}
            />
          </label>
          <label className="wide-field">
            <span>{tokenLabel}</span>
            <input
              onChange={(event) => setSourceRepositoryForm((current) => ({
                ...current,
                accessToken: event.target.value,
              }))}
              required
              type="password"
              value={sourceRepositoryForm.accessToken}
            />
          </label>
          <label className="wide-field">
            <span>Description</span>
            <input
              onChange={(event) => setSourceRepositoryForm((current) => ({
                ...current,
                description: event.target.value,
              }))}
              required
              type="text"
              value={sourceRepositoryForm.description}
            />
          </label>
          <button disabled={sourceRepositoryState === 'submitting'} type="submit">Register source</button>
        </form>
      )}

      {sourceRepositoryView === 'list' && (
        <>
          {sourceRepositoryState === 'loading' && <p className="muted">Loading source repositories...</p>}
          {sourceRepositoryState !== 'loading' && sourceRepositories.length === 0 && (
            <p className="muted">No source repositories registered.</p>
          )}

          <div className="source-repository-list">
            {sourceRepositories.map((repository) => (
              <article className="source-repository-card" key={repository.id}>
                <div className="source-repository-main">
                  <div>
                    <strong>{repository.name}</strong>
                    <a href={repository.repositoryUrl} rel="noreferrer" target="_blank">
                      {repository.repositoryUrl}
                    </a>
                  </div>
                  <span className="status-value neutral">{repository.provider || 'UNKNOWN'}</span>
                </div>
                <p>{repository.description}</p>
                <div className="source-repository-stats">
                  {renderStatusMetric('Clones', repository.cloneCount ?? 0)}
                  {renderStatusMetric('Builds', repository.buildCount ?? 0)}
                  {renderStatusMetric(
                    'Last clone',
                    repository.lastClonedAt ? new Date(repository.lastClonedAt).toLocaleString() : 'None',
                  )}
                </div>
                <div className="source-repository-meta">
                  <small>{repository.visibility}</small>
                  <small>{repository.accountName || 'No account'}</small>
                  <small>{repository.credentialConfigured ? 'Credential configured' : 'Credential missing'}</small>
                  <button
                    className="danger-action"
                    onClick={() => handleDeleteSourceRepository(repository.id)}
                    type="button"
                  >
                    제거
                  </button>
                </div>
              </article>
            ))}
          </div>
        </>
      )}
    </section>
  )
}

function renderCicdControlPanel(
  applicationDetail,
  cicdForm,
  setCicdForm,
  handleCreateCicdRequest,
  cicdRequests,
  auditEvents,
  cicdState,
  cicdMessage,
) {
  const selectedEnvironment = applicationDetail.environments.find(
    (environment) => environment.environment === cicdForm.environment,
  ) ?? applicationDetail.environments[0]
  const applicationRequests = cicdRequests.filter((request) => request.applicationId === applicationDetail.id)
  const applicationAuditEvents = auditEvents.filter((event) => (
    applicationRequests.some((request) => request.id === event.cicdRequestId)
  ))

  return (
    <section className="cicd-panel" aria-label="CI/CD requests">
      <div className="status-heading">
        <div>
          <p className="eyebrow">CI/CD Control</p>
          <h3>Requests and audit</h3>
        </div>
        <span className="connection-badge">PORTAL OWNED</span>
      </div>

      <form className="cicd-form" onSubmit={handleCreateCicdRequest}>
        <label>
          <span>Environment</span>
          <select
            onChange={(event) => {
              const environment = applicationDetail.environments.find(
                (candidate) => candidate.environment === event.target.value,
              )
              setCicdForm((current) => ({
                ...current,
                environment: event.target.value,
                componentId: environment?.components[0]?.id?.toString() ?? '',
              }))
            }}
            value={cicdForm.environment}
          >
            {applicationDetail.environments.map((environment) => (
              <option key={environment.id} value={environment.environment}>
                {environment.environment}
              </option>
            ))}
          </select>
        </label>

        <label>
          <span>Component</span>
          <select
            onChange={(event) => setCicdForm((current) => ({ ...current, componentId: event.target.value }))}
            value={cicdForm.componentId}
          >
            {(selectedEnvironment?.components ?? []).map((component) => (
              <option key={component.id} value={component.id}>
                {component.name}
              </option>
            ))}
          </select>
        </label>

        <label>
          <span>Request type</span>
          <select
            onChange={(event) => setCicdForm((current) => ({ ...current, requestType: event.target.value }))}
            value={cicdForm.requestType}
          >
            <option value="BUILD_IMAGE">Build image</option>
            <option value="DEPLOY_IMAGE">Deploy image</option>
            <option value="CHANGE_REPLICAS">Change replicas</option>
          </select>
        </label>

        <label>
          <span>Requested value</span>
          <input
            onChange={(event) => setCicdForm((current) => ({ ...current, requestedValue: event.target.value }))}
            placeholder={requestValuePlaceholder(cicdForm.requestType)}
            required
            type="text"
            value={cicdForm.requestedValue}
          />
        </label>

        <label>
          <span>Requested by</span>
          <input
            onChange={(event) => setCicdForm((current) => ({ ...current, requestedBy: event.target.value }))}
            required
            type="text"
            value={cicdForm.requestedBy}
          />
        </label>

        <button disabled={cicdState === 'submitting' || !cicdForm.componentId} type="submit">
          Record request
        </button>
      </form>

      {cicdMessage && (
        <p className={cicdState === 'error' ? 'status-message' : 'success-message'}>{cicdMessage}</p>
      )}

      <div className="cicd-columns">
        <div className="request-list">
          <div className="panel-heading">
            <h4>Requests</h4>
            <span>{applicationRequests.length}</span>
          </div>
          {applicationRequests.length === 0 && <p className="muted">No CI/CD requests recorded.</p>}
          {applicationRequests.map((request) => (
            <article className="request-card" key={request.id}>
              <div>
                <strong>{request.requestType}</strong>
                <small>{request.componentName} / {request.environment}</small>
              </div>
              <code>{request.requestedValue}</code>
              <span className={`status-value ${cicdStatusTone(request.status)}`}>{request.status}</span>
            </article>
          ))}
        </div>

        <div className="request-list">
          <div className="panel-heading">
            <h4>Audit events</h4>
            <span>{applicationAuditEvents.length}</span>
          </div>
          {applicationAuditEvents.length === 0 && <p className="muted">No audit events recorded.</p>}
          {applicationAuditEvents.map((event) => (
            <article className="request-card" key={event.id}>
              <div>
                <strong>{event.eventType}</strong>
                <small>{new Date(event.createdAt).toLocaleString()}</small>
              </div>
              <p>{event.description}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}

function requestValuePlaceholder(requestType) {
  if (requestType === 'CHANGE_REPLICAS') {
    return '2'
  }
  if (requestType === 'DEPLOY_IMAGE') {
    return '1fd847c'
  }
  return 'main'
}

function cicdStatusTone(status) {
  if (['SUCCEEDED'].includes(status)) {
    return 'good'
  }
  if (['REQUESTED', 'DISPATCHED', 'RUNNING'].includes(status)) {
    return 'pending'
  }
  if (['FAILED'].includes(status)) {
    return 'bad'
  }
  return 'neutral'
}

function renderMetadata(label, value) {
  return (
    <div className="metadata-item" key={label}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function renderRuntimeStatus(runtime, runtimeState) {
  if (runtimeState === 'loading' || !runtime) {
    return (
      <section className="status-panel" aria-label="Kubernetes runtime status">
        <p className="muted">Loading Kubernetes runtime...</p>
      </section>
    )
  }

  const isUnavailable = runtime.connectionStatus !== 'AVAILABLE'

  return (
    <section className={isUnavailable ? 'status-panel unavailable' : 'status-panel'} aria-label="Kubernetes runtime status">
      <div className="status-heading">
        <div>
          <p className="eyebrow">Kubernetes Runtime</p>
          <h4>{runtime.namespace}</h4>
        </div>
        <span className={isUnavailable ? 'connection-badge disconnected' : 'connection-badge'}>
          {runtime.connectionStatus}
        </span>
      </div>

      <div className="status-grid">
        {renderStatusMetric(
          'Ready',
          `${runtime.summary.readyReplicas}/${runtime.summary.desiredReplicas}`,
          runtime.summary.readyReplicas === runtime.summary.desiredReplicas ? 'good' : 'pending',
        )}
        {renderStatusMetric('Available', runtime.summary.availableReplicas, 'neutral')}
        {renderStatusMetric(
          'Warnings',
          runtime.summary.warningEvents,
          runtime.summary.warningEvents > 0 ? 'bad' : 'good',
        )}
        {renderStatusMetric('Components', runtime.components.length, 'neutral')}
      </div>

      {isUnavailable && <p className="status-message">{runtime.message || 'Kubernetes runtime is unavailable.'}</p>}

      {runtime.components.length > 0 && (
        <div className="runtime-table" role="table" aria-label={`${runtime.environment} Kubernetes runtime`}>
          <div className="runtime-row runtime-head" role="row">
            <span role="columnheader">Component</span>
            <span role="columnheader">Status</span>
            <span role="columnheader">Deployment</span>
            <span role="columnheader">Pods</span>
            <span role="columnheader">Restarts</span>
            <span role="columnheader">Service</span>
            <span role="columnheader">Recent event</span>
          </div>

          {runtime.components.map((component) => (
            <div className="runtime-row" key={component.componentId} role="row">
              <span role="cell">
                <strong>{component.componentName}</strong>
                <small>{component.kind}</small>
              </span>
              <span role="cell">
                <strong className={`status-value ${statusTone(component.status)}`}>{component.status}</strong>
              </span>
              <span role="cell">
                {component.readyReplicas}/{component.desiredReplicas} ready
              </span>
              <span role="cell">{renderPodSummary(component.pods)}</span>
              <span role="cell">{component.restartCount}</span>
              <span role="cell">
                <strong>{component.serviceName}</strong>
                <small>{component.serviceType} {component.clusterIp}</small>
              </span>
              <span role="cell">{renderRecentEvent(component)}</span>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

function renderArgoCdStatus(status, statusState) {
  if (statusState === 'loading' || !status) {
    return (
      <section className="status-panel" aria-label="ArgoCD status">
        <p className="muted">Loading ArgoCD status...</p>
      </section>
    )
  }

  const isUnavailable = status.connectionStatus !== 'AVAILABLE'

  return (
    <section className={isUnavailable ? 'status-panel unavailable' : 'status-panel'} aria-label="ArgoCD status">
      <div className="status-heading">
        <div>
          <p className="eyebrow">ArgoCD Status</p>
          <h4>{status.argocdApplicationName}</h4>
        </div>
        <span className={isUnavailable ? 'connection-badge disconnected' : 'connection-badge'}>
          {status.connectionStatus}
        </span>
      </div>

      <div className="status-grid">
        {renderStatusMetric('Sync', status.syncStatus, statusTone(status.syncStatus))}
        {renderStatusMetric('Health', status.healthStatus, statusTone(status.healthStatus))}
        {renderStatusMetric('Operation', status.operationPhase, statusTone(status.operationPhase))}
        {renderStatusMetric('Reconciled', status.reconciledAt ? new Date(status.reconciledAt).toLocaleString() : 'Unknown')}
      </div>

      {isUnavailable && <p className="status-message">{status.message || 'ArgoCD status is unavailable.'}</p>}

      {status.images.length > 0 && (
        <div className="image-list" aria-label="ArgoCD image summary">
          {status.images.map((image) => (
            <code key={image}>{image}</code>
          ))}
        </div>
      )}
    </section>
  )
}

function renderStatusMetric(label, value, tone = 'neutral') {
  return (
    <div className="status-metric" key={label}>
      <span>{label}</span>
      <strong className={`status-value ${tone}`}>{value}</strong>
    </div>
  )
}

function statusTone(value) {
  if (['Synced', 'Healthy', 'Succeeded', 'READY'].includes(value)) {
    return 'good'
  }

  if (['OutOfSync', 'Progressing', 'Running', 'PROGRESSING', 'WARNING'].includes(value)) {
    return 'pending'
  }

  if (['Degraded', 'Failed', 'Error', 'Missing', 'MISSING'].includes(value)) {
    return 'bad'
  }

  return 'neutral'
}

function renderPodSummary(pods) {
  if (pods.length === 0) {
    return 'No pods'
  }

  return pods.map((pod) => `${pod.name} ${pod.readyContainers}/${pod.totalContainers} ${pod.phase}`).join(', ')
}

function renderRecentEvent(component) {
  if (component.recentEvents.length === 0) {
    return component.message || 'No recent events'
  }

  const event = component.recentEvents[0]
  return `${event.type} ${event.reason}: ${event.message}`
}

export default App
