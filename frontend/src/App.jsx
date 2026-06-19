import { useEffect, useMemo, useState } from 'react'
import './App.css'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'

async function fetchJson(path) {
  const response = await fetch(`${API_BASE_URL}${path}`)
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
  const [applicationDetail, setApplicationDetail] = useState(null)
  const [environmentStatuses, setEnvironmentStatuses] = useState({})
  const [catalogState, setCatalogState] = useState('loading')
  const [detailState, setDetailState] = useState('idle')
  const [statusState, setStatusState] = useState('idle')
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

  const selectedApplication = useMemo(
    () => applications.find((application) => application.id === selectedApplicationId),
    [applications, selectedApplicationId],
  )

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
            Registered workloads and environment metadata managed by the portal.
            Live ArgoCD, Kubernetes, and Prometheus status will be added in later
            milestones.
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

      <section className="catalog-layout">
        <aside className="catalog-panel" aria-label="Applications">
          <div className="panel-heading">
            <h3>Applications</h3>
            <span>{applications.length}</span>
          </div>

          {catalogState === 'loading' && <p className="muted">Loading catalog...</p>}
          {catalogState === 'empty' && <p className="muted">No applications registered.</p>}

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
        </aside>

        <section className="detail-panel" aria-label="Application detail">
          {detailState === 'idle' && <p className="muted">Select an application.</p>}
          {detailState === 'loading' && <p className="muted">Loading application detail...</p>}
          {detailState === 'error' && <p className="muted">Application detail is unavailable.</p>}

          {detailState === 'ready' && applicationDetail && (
            <>
              <header className="detail-header">
                <div>
                  <p className="eyebrow">Managed Application</p>
                  <h2>{applicationDetail.name}</h2>
                  <p>{applicationDetail.description}</p>
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
    </main>
  )
}

function renderMetadata(label, value) {
  return (
    <div className="metadata-item" key={label}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
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
  if (['Synced', 'Healthy', 'Succeeded'].includes(value)) {
    return 'good'
  }

  if (['OutOfSync', 'Progressing', 'Running'].includes(value)) {
    return 'pending'
  }

  if (['Degraded', 'Failed', 'Error', 'Missing'].includes(value)) {
    return 'bad'
  }

  return 'neutral'
}

export default App
