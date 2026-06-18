import { useEffect, useState } from 'react'
import './App.css'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'

function App() {
  const [apiStatus, setApiStatus] = useState('Checking API')
  const [lastCheckedAt, setLastCheckedAt] = useState('')

  useEffect(() => {
    let ignore = false

    async function loadHealth() {
      try {
        const response = await fetch(`${API_BASE_URL}/api/health`)
        if (!response.ok) {
          throw new Error(`API returned ${response.status}`)
        }

        const health = await response.json()
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

      <section className="hero">
        <div>
          <h2>Manage deployed applications after CI/CD</h2>
          <p>
            Day 13 establishes the portal runtime. Application catalog, runtime
            status, deployment requests, and audit trails will build on this
            shell in the next milestones.
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

      <section className="workspace-grid" aria-label="Portal milestone areas">
        <article>
          <span>01</span>
          <h3>Application catalog</h3>
          <p>Registered applications, owners, repositories, and environments.</p>
        </article>
        <article>
          <span>02</span>
          <h3>Runtime status</h3>
          <p>ArgoCD health, Kubernetes readiness, pods, and recent events.</p>
        </article>
        <article>
          <span>03</span>
          <h3>GitOps requests</h3>
          <p>Image tag and replica changes handled through desired state.</p>
        </article>
        <article>
          <span>04</span>
          <h3>Audit and metrics</h3>
          <p>Write action history and Prometheus operational summaries.</p>
        </article>
      </section>
    </main>
  )
}

export default App
