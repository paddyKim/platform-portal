import { useEffect, useMemo, useState } from 'react'
import './App.css'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'

const BUILD_PROFILE_TEMPLATES = {
  SHELL: {
    workingDirectory: '.',
    script: `#!/usr/bin/env bash
set -euo pipefail

./gradlew test
docker build -t ghcr.io/paddykim/platform-api:\${IMAGE_TAG} backend
docker push ghcr.io/paddykim/platform-api:\${IMAGE_TAG}

mkdir -p .platform
cat > .platform/build-output.json <<JSON
{
  "imageRepository": "ghcr.io/paddykim/platform-api",
  "imageTag": "\${IMAGE_TAG}",
  "imageReference": "ghcr.io/paddykim/platform-api:\${IMAGE_TAG}"
}
JSON`,
  },
  JENKINS: {
    workingDirectory: '.',
    script: `pipeline {
  agent any
  stages {
    stage('Test') {
      steps {
        sh './gradlew test'
      }
    }
    stage('Build image') {
      steps {
        sh 'docker build -t ghcr.io/paddykim/platform-api:\${IMAGE_TAG} backend'
        sh 'docker push ghcr.io/paddykim/platform-api:\${IMAGE_TAG}'
      }
    }
  }
}`,
  },
  GITHUB_ACTIONS: {
    workingDirectory: '.',
    script: `name: build
on:
  workflow_dispatch:
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: ./gradlew test
      - run: docker build -t ghcr.io/paddykim/platform-api:\${IMAGE_TAG} backend
      - run: docker push ghcr.io/paddykim/platform-api:\${IMAGE_TAG}`,
  },
  GITLAB_CI: {
    workingDirectory: '.',
    script: `stages:
  - test
  - build

test:
  stage: test
  script:
    - ./gradlew test

build:
  stage: build
  script:
    - docker build -t ghcr.io/paddykim/platform-api:\${IMAGE_TAG} backend
    - docker push ghcr.io/paddykim/platform-api:\${IMAGE_TAG}`,
  },
}

const PIPELINE_PREVIEWS = {
  SHELL: {
    label: 'Shell runner',
    unit: 'Template flow',
    stages: [
      { name: 'Clone', detail: 'Fetch source' },
      { name: 'Test', detail: './gradlew test' },
      { name: 'Build', detail: 'docker build' },
      { name: 'Publish', detail: 'image tag' },
    ],
  },
  JENKINS: {
    label: 'Jenkins Pipeline',
    unit: 'Template flow',
    stages: [
      { name: 'Checkout', detail: 'SCM' },
      { name: 'Test', detail: 'sh step' },
      { name: 'Build image', detail: 'Docker' },
      { name: 'Post', detail: 'archive/logs' },
    ],
  },
  GITHUB_ACTIONS: {
    label: 'GitHub Actions',
    unit: 'Template flow',
    stages: [
      { name: 'workflow_dispatch', detail: 'trigger' },
      { name: 'checkout', detail: 'action' },
      { name: 'test', detail: 'run step' },
      { name: 'build', detail: 'run step' },
    ],
  },
  GITLAB_CI: {
    label: 'GitLab CI',
    unit: 'Template flow',
    stages: [
      { name: 'test', detail: 'job' },
      { name: 'build', detail: 'job' },
      { name: 'publish', detail: 'job' },
      { name: 'deploy-ready', detail: 'artifact' },
    ],
  },
}

async function fetchJson(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, options)
  if (!response.ok) {
    throw new Error(`API returned ${response.status}`)
  }

  return response.json()
}

function base64ToArrayBuffer(value) {
  const binary = window.atob(value)
  const bytes = new Uint8Array(binary.length)

  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index)
  }

  return bytes.buffer
}

function arrayBufferToBase64(buffer) {
  const bytes = new Uint8Array(buffer)
  let binary = ''

  for (let index = 0; index < bytes.byteLength; index += 1) {
    binary += String.fromCharCode(bytes[index])
  }

  return window.btoa(binary)
}

async function encryptSourceRepositoryCredential(accessToken) {
  const { publicKey } = await fetchJson('/api/source-repositories/credential-public-key')
  const key = await window.crypto.subtle.importKey(
    'spki',
    base64ToArrayBuffer(publicKey),
    {
      name: 'RSA-OAEP',
      hash: 'SHA-256',
    },
    false,
    ['encrypt'],
  )
  const encrypted = await window.crypto.subtle.encrypt(
    { name: 'RSA-OAEP' },
    key,
    new TextEncoder().encode(accessToken),
  )

  return arrayBufferToBase64(encrypted)
}

function App() {
  const [apiStatus, setApiStatus] = useState('Checking API')
  const [lastCheckedAt, setLastCheckedAt] = useState('')
  const [applications, setApplications] = useState([])
  const [argoCdApplications, setArgoCdApplications] = useState([])
  const [selectedApplicationId, setSelectedApplicationId] = useState(null)
  const [activeSection, setActiveSection] = useState('cicd')
  const [applicationDetail, setApplicationDetail] = useState(null)
  const [sourceRepositories, setSourceRepositories] = useState([])
  const [sourceRepositoryView, setSourceRepositoryView] = useState('list')
  const [selectedSourceRepositoryId, setSelectedSourceRepositoryId] = useState(null)
  const [buildProfiles, setBuildProfiles] = useState([])
  const [selectedBuildProfileId, setSelectedBuildProfileId] = useState(null)
  const [lastBuildExecution, setLastBuildExecution] = useState(null)
  const [buildExecutions, setBuildExecutions] = useState([])
  const [selectedBuildExecutionId, setSelectedBuildExecutionId] = useState(null)
  const [environmentStatuses, setEnvironmentStatuses] = useState({})
  const [runtimeStatuses, setRuntimeStatuses] = useState({})
  const [sourceRepositoryForm, setSourceRepositoryForm] = useState({
    name: '',
    provider: 'GITHUB',
    visibility: 'PUBLIC',
    repositoryUrl: '',
    accountName: '',
    accessToken: '',
    description: '',
  })
  const [buildProfileForm, setBuildProfileForm] = useState({
    name: '',
    ciTool: 'SHELL',
    workingDirectory: BUILD_PROFILE_TEMPLATES.SHELL.workingDirectory,
    script: BUILD_PROFILE_TEMPLATES.SHELL.script,
    description: '',
    requestedBy: 'platform-operator',
    imageTag: '',
    branch: 'main',
    targetApplicationId: '',
    targetEnvironmentId: '',
    targetComponentId: '',
  })
  const [environmentForm, setEnvironmentForm] = useState({
    environment: 'dev',
    namespace: 'dev',
    argocdApplicationName: '',
    helmValuesPath: '',
    serviceUrl: '',
  })
  const [componentForm, setComponentForm] = useState({
    name: '',
    kind: 'api',
    deploymentName: '',
    serviceName: '',
    imageRepository: '',
  })
  const [manifestForm, setManifestForm] = useState({
    manifestRepositoryUrl: '',
    manifestBranch: 'main',
    valuesPath: '',
    imageTagKey: '',
  })
  const [argoCdApplicationForm, setArgoCdApplicationForm] = useState({
    name: '',
    project: 'default',
    sourceRepoUrl: '',
    sourcePath: '.',
    targetRevision: 'HEAD',
    destinationServer: 'https://kubernetes.default.svc',
    destinationNamespace: 'default',
    automated: false,
    prune: false,
    selfHeal: false,
  })
  const [appCommandText, setAppCommandText] = useState('')
  const [appCommandView, setAppCommandView] = useState('idle')
  const [editingBuildProfileId, setEditingBuildProfileId] = useState(null)
  const [editingEnvironmentId, setEditingEnvironmentId] = useState(null)
  const [componentFormEnvironmentId, setComponentFormEnvironmentId] = useState(null)
  const [editingComponentId, setEditingComponentId] = useState(null)
  const [editingManifestComponentId, setEditingManifestComponentId] = useState(null)
  const [catalogState, setCatalogState] = useState('idle')
  const [catalogManagementState, setCatalogManagementState] = useState('idle')
  const [appCommandState, setAppCommandState] = useState('idle')
  const [detailState, setDetailState] = useState('idle')
  const [statusState, setStatusState] = useState('idle')
  const [runtimeState, setRuntimeState] = useState('idle')
  const [sourceRepositoryState, setSourceRepositoryState] = useState('loading')
  const [buildProfileState, setBuildProfileState] = useState('idle')
  const [buildExecutionState, setBuildExecutionState] = useState('idle')
  const [sourceRepositoryMessage, setSourceRepositoryMessage] = useState('')
  const [buildProfileMessage, setBuildProfileMessage] = useState('')
  const [buildExecutionMessage, setBuildExecutionMessage] = useState('')
  const [catalogManagementMessage, setCatalogManagementMessage] = useState('')
  const [appCommandMessage, setAppCommandMessage] = useState('')
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
    if (!selectedSourceRepositoryId) {
      setBuildProfiles([])
      setSelectedBuildProfileId(null)
      setLastBuildExecution(null)
      setBuildExecutions([])
      setSelectedBuildExecutionId(null)
      setBuildProfileState('idle')
      setBuildProfileMessage('')
      setBuildExecutionState('idle')
      setBuildExecutionMessage('')
      return
    }

    let ignore = false

    async function loadBuildProfiles() {
      setBuildProfileState('loading')
      setBuildProfileMessage('')
      setLastBuildExecution(null)
      setBuildExecutions([])
      setSelectedBuildExecutionId(null)

      try {
        const data = await fetchJson(`/api/source-repositories/${selectedSourceRepositoryId}/build-profiles`)
        if (!ignore) {
          setBuildProfiles(data)
          setBuildProfileState('ready')
        }
      } catch (error) {
        if (!ignore) {
          setBuildProfileMessage(error.message)
          setBuildProfileState('error')
        }
      }
    }

    loadBuildProfiles()

    return () => {
      ignore = true
    }
  }, [selectedSourceRepositoryId])

  useEffect(() => {
    if (buildProfiles.length === 0) {
      setSelectedBuildProfileId(null)
      return
    }

    if (!buildProfiles.some((profile) => profile.id === selectedBuildProfileId)) {
      setSelectedBuildProfileId(buildProfiles[0].id)
    }
  }, [buildProfiles, selectedBuildProfileId])

  useEffect(() => {
    if (!selectedSourceRepositoryId || !selectedBuildProfileId) {
      setBuildExecutions([])
      setSelectedBuildExecutionId(null)
      setBuildExecutionState('idle')
      setBuildExecutionMessage('')
      return
    }

    let ignore = false

    async function loadBuildExecutions() {
      setBuildExecutionState('loading')
      setBuildExecutionMessage('')

      try {
        const data = await fetchJson(
          `/api/source-repositories/${selectedSourceRepositoryId}/build-profiles/${selectedBuildProfileId}/executions`,
        )
        if (!ignore) {
          setBuildExecutions(data)
          setSelectedBuildExecutionId((current) => (
            data.some((execution) => execution.id === current) ? current : data[0]?.id ?? null
          ))
          setBuildExecutionState('ready')
        }
      } catch (error) {
        if (!ignore) {
          setBuildExecutionMessage(error.message)
          setBuildExecutionState('error')
        }
      }
    }

    loadBuildExecutions()

    return () => {
      ignore = true
    }
  }, [selectedSourceRepositoryId, selectedBuildProfileId])

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
      return
    }

    setBuildProfileForm((current) => {
      const applicationId = String(applicationDetail.id)
      const environments = applicationDetail.environments || []
      const selectedEnvironment = environments.find(
        (environment) => String(environment.id) === String(current.targetEnvironmentId),
      ) || environments[0]
      const components = selectedEnvironment?.components || []
      const selectedComponent = components.find(
        (component) => String(component.id) === String(current.targetComponentId),
      ) || components[0]

      return {
        ...current,
        targetApplicationId: current.targetApplicationId || applicationId,
        targetEnvironmentId: selectedEnvironment ? String(selectedEnvironment.id) : '',
        targetComponentId: selectedComponent ? String(selectedComponent.id) : '',
      }
    })
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

  const selectedSourceRepository = useMemo(
    () => sourceRepositories.find((repository) => repository.id === selectedSourceRepositoryId),
    [sourceRepositories, selectedSourceRepositoryId],
  )

  const selectedBuildProfile = useMemo(
    () => buildProfiles.find((profile) => profile.id === selectedBuildProfileId),
    [buildProfiles, selectedBuildProfileId],
  )

  const selectedBuildExecution = useMemo(
    () => buildExecutions.find((execution) => execution.id === selectedBuildExecutionId),
    [buildExecutions, selectedBuildExecutionId],
  )

  async function reloadSourceRepositories() {
    const repositories = await fetchJson('/api/source-repositories')
    setSourceRepositories(repositories)
  }

  async function reloadApplications(nextSelectedApplicationId = selectedApplicationId) {
    const data = await fetchJson('/api/applications')
    setApplications(data)
    setCatalogState(data.length > 0 ? 'ready' : 'empty')
    if (nextSelectedApplicationId && data.some((application) => application.id === nextSelectedApplicationId)) {
      setSelectedApplicationId(nextSelectedApplicationId)
      return
    }
    setSelectedApplicationId(data[0]?.id ?? null)
  }

  async function handleAppCommandSubmit(event) {
    event.preventDefault()
    const command = appCommandText.trim()
    if (!command) {
      return
    }

    setAppCommandState('submitting')
    setAppCommandMessage('')

    try {
      const interpretation = await fetchJson('/api/application-commands/interpret', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ command }),
      })

      if (interpretation.intent === 'OPEN_APPLICATION_CREATE_FORM') {
        resetArgoCdApplicationForm()
        setAppCommandView('create')
        setCatalogState('idle')
        setSelectedApplicationId(null)
        setApplicationDetail(null)
        setAppCommandMessage('어플리케이션 등록 화면으로 이동했습니다.')
        setAppCommandState('ready')
        return
      }

      if (interpretation.intent !== 'LIST_APPLICATIONS') {
        setAppCommandMessage(interpretation.message)
        setAppCommandState('error')
        return
      }

      const data = await fetchJson(interpretation.resultApiPath || '/api/argocd/applications')
      setArgoCdApplications(data)
      setCatalogState(data.length > 0 ? 'ready' : 'empty')
      setAppCommandView('list')
      setSelectedApplicationId(null)
      setApplicationDetail(null)

      if (data.length === 0) {
        setAppCommandMessage('등록된 application이 없습니다.')
        setAppCommandState('ready')
        return
      }

      setAppCommandMessage(`${data.length}개 application을 불러왔습니다.`)
      setAppCommandState('ready')
    } catch (error) {
      setAppCommandMessage(error.message)
      setAppCommandState('error')
    }
  }

  async function reloadBuildProfiles(repositoryId = selectedSourceRepositoryId) {
    if (!repositoryId) {
      setBuildProfiles([])
      return
    }

    const profiles = await fetchJson(`/api/source-repositories/${repositoryId}/build-profiles`)
    setBuildProfiles(profiles)
  }

  async function reloadBuildExecutions(
    repositoryId = selectedSourceRepositoryId,
    profileId = selectedBuildProfileId,
    options = {},
  ) {
    if (!repositoryId || !profileId) {
      setBuildExecutions([])
      setSelectedBuildExecutionId(null)
      return
    }

    const executions = await fetchJson(`/api/source-repositories/${repositoryId}/build-profiles/${profileId}/executions`)
    setBuildExecutions(executions)
    if (options.selectLatest) {
      setSelectedBuildExecutionId(executions[0]?.id ?? null)
      return
    }

    setSelectedBuildExecutionId((current) => (
      executions.some((execution) => execution.id === current) ? current : executions[0]?.id ?? null
    ))
  }

  function resetBuildProfileForm() {
    setEditingBuildProfileId(null)
    setBuildProfileForm({
      name: '',
      ciTool: 'SHELL',
      workingDirectory: BUILD_PROFILE_TEMPLATES.SHELL.workingDirectory,
      script: BUILD_PROFILE_TEMPLATES.SHELL.script,
      description: '',
      requestedBy: 'platform-operator',
      imageTag: '',
      branch: 'main',
      targetApplicationId: applicationDetail ? String(applicationDetail.id) : '',
      targetEnvironmentId: applicationDetail?.environments?.[0]?.id
        ? String(applicationDetail.environments[0].id)
        : '',
      targetComponentId: applicationDetail?.environments?.[0]?.components?.[0]?.id
        ? String(applicationDetail.environments[0].components[0].id)
        : '',
    })
  }

  function handleSelectBuildProfileTool(ciTool) {
    const template = BUILD_PROFILE_TEMPLATES[ciTool]
    setBuildProfileForm((current) => ({
      ...current,
      ciTool,
      workingDirectory: template.workingDirectory,
      script: template.script,
    }))
  }

  function handleEditBuildProfile(profile) {
    setEditingBuildProfileId(profile.id)
    setSelectedBuildProfileId(profile.id)
    if (profile.targetApplicationId) {
      setSelectedApplicationId(profile.targetApplicationId)
    }
    setBuildProfileForm((current) => ({
      ...current,
      name: profile.name,
      ciTool: profile.ciTool,
      workingDirectory: profile.workingDirectory,
      script: profile.script,
      description: profile.description,
      targetApplicationId: profile.targetApplicationId ? String(profile.targetApplicationId) : '',
      targetEnvironmentId: profile.targetEnvironmentId ? String(profile.targetEnvironmentId) : '',
      targetComponentId: profile.targetComponentId ? String(profile.targetComponentId) : '',
    }))
  }

  function resetArgoCdApplicationForm() {
    setArgoCdApplicationForm({
      name: '',
      project: 'default',
      sourceRepoUrl: '',
      sourcePath: '.',
      targetRevision: 'HEAD',
      destinationServer: 'https://kubernetes.default.svc',
      destinationNamespace: 'default',
      automated: false,
      prune: false,
      selfHeal: false,
    })
  }

  async function handleCreateArgoCdApplication(event) {
    event.preventDefault()
    setCatalogManagementState('submitting')
    setCatalogManagementMessage('')

    try {
      const createdApplication = await fetchJson('/api/argocd/applications', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(argoCdApplicationForm),
      })
      setArgoCdApplications((current) => [createdApplication, ...current])
      setCatalogManagementMessage(`${createdApplication.name} ArgoCD application을 등록했습니다.`)
      setCatalogManagementState('ready')
      resetArgoCdApplicationForm()
    } catch (error) {
      setCatalogManagementMessage(error.message)
      setCatalogManagementState('error')
    }
  }

  function resetEnvironmentForm() {
    setEditingEnvironmentId(null)
    setEnvironmentForm({
      environment: 'dev',
      namespace: 'dev',
      argocdApplicationName: '',
      helmValuesPath: '',
      serviceUrl: '',
    })
  }

  function handleEditEnvironment(environment) {
    setEditingEnvironmentId(environment.id)
    setEnvironmentForm({
      environment: environment.environment,
      namespace: environment.namespace,
      argocdApplicationName: environment.argocdApplicationName,
      helmValuesPath: environment.helmValuesPath,
      serviceUrl: environment.serviceUrl,
    })
  }

  async function handleSaveEnvironment(event) {
    event.preventDefault()
    if (!selectedApplicationId) {
      return
    }

    setCatalogManagementState('submitting')
    setCatalogManagementMessage('')
    const path = editingEnvironmentId
      ? `/api/applications/${selectedApplicationId}/environments/${editingEnvironmentId}`
      : `/api/applications/${selectedApplicationId}/environments`
    const method = editingEnvironmentId ? 'PUT' : 'POST'

    try {
      const detail = await fetchJson(path, {
        method,
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(environmentForm),
      })
      setApplicationDetail(detail)
      await reloadApplications(selectedApplicationId)
      resetEnvironmentForm()
      setCatalogManagementMessage(editingEnvironmentId ? 'Environment updated' : 'Environment created')
      setCatalogManagementState('ready')
    } catch (error) {
      setCatalogManagementMessage(error.message)
      setCatalogManagementState('error')
    }
  }

  async function handleDeleteEnvironment(environmentId) {
    if (!selectedApplicationId) {
      return
    }
    const confirmed = window.confirm('Delete this environment and all components?')
    if (!confirmed) {
      return
    }

    setCatalogManagementState('submitting')
    setCatalogManagementMessage('')

    try {
      const detail = await fetchJson(`/api/applications/${selectedApplicationId}/environments/${environmentId}`, {
        method: 'DELETE',
      })
      setApplicationDetail(detail)
      await reloadApplications(selectedApplicationId)
      resetEnvironmentForm()
      setCatalogManagementMessage('Environment deleted')
      setCatalogManagementState('ready')
    } catch (error) {
      setCatalogManagementMessage(error.message)
      setCatalogManagementState('error')
    }
  }

  function resetComponentForm(environmentId = null) {
    setComponentFormEnvironmentId(environmentId)
    setEditingComponentId(null)
    setComponentForm({
      name: '',
      kind: 'api',
      deploymentName: '',
      serviceName: '',
      imageRepository: '',
    })
  }

  function handleEditComponent(environmentId, component) {
    setComponentFormEnvironmentId(environmentId)
    setEditingComponentId(component.id)
    setComponentForm({
      name: component.name,
      kind: component.kind,
      deploymentName: component.deploymentName,
      serviceName: component.serviceName,
      imageRepository: component.imageRepository,
    })
  }

  async function handleSaveComponent(event, environmentId) {
    event.preventDefault()
    if (!selectedApplicationId) {
      return
    }

    setCatalogManagementState('submitting')
    setCatalogManagementMessage('')
    const path = editingComponentId
      ? `/api/applications/${selectedApplicationId}/environments/${environmentId}/components/${editingComponentId}`
      : `/api/applications/${selectedApplicationId}/environments/${environmentId}/components`
    const method = editingComponentId ? 'PUT' : 'POST'

    try {
      const detail = await fetchJson(path, {
        method,
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(componentForm),
      })
      setApplicationDetail(detail)
      resetComponentForm(environmentId)
      setCatalogManagementMessage(editingComponentId ? 'Component updated' : 'Component created')
      setCatalogManagementState('ready')
    } catch (error) {
      setCatalogManagementMessage(error.message)
      setCatalogManagementState('error')
    }
  }

  async function handleDeleteComponent(environmentId, componentId) {
    if (!selectedApplicationId) {
      return
    }
    const confirmed = window.confirm('Delete this component?')
    if (!confirmed) {
      return
    }

    setCatalogManagementState('submitting')
    setCatalogManagementMessage('')

    try {
      const detail = await fetchJson(
        `/api/applications/${selectedApplicationId}/environments/${environmentId}/components/${componentId}`,
        { method: 'DELETE' },
      )
      setApplicationDetail(detail)
      resetComponentForm(environmentId)
      setCatalogManagementMessage('Component deleted')
      setCatalogManagementState('ready')
    } catch (error) {
      setCatalogManagementMessage(error.message)
      setCatalogManagementState('error')
    }
  }

  function resetManifestForm() {
    setEditingManifestComponentId(null)
    setManifestForm({
      manifestRepositoryUrl: '',
      manifestBranch: 'main',
      valuesPath: '',
      imageTagKey: '',
    })
  }

  function handleEditManifestMapping(component) {
    setEditingManifestComponentId(component.id)
    setManifestForm({
      manifestRepositoryUrl: component.manifestMapping?.manifestRepositoryUrl || '',
      manifestBranch: component.manifestMapping?.manifestBranch || 'main',
      valuesPath: component.manifestMapping?.valuesPath || '',
      imageTagKey: component.manifestMapping?.imageTagKey || '',
    })
  }

  async function handleSaveManifestMapping(event, environmentId, componentId) {
    event.preventDefault()
    if (!selectedApplicationId) {
      return
    }

    setCatalogManagementState('submitting')
    setCatalogManagementMessage('')
    const component = applicationDetail?.environments
      ?.find((environment) => environment.id === environmentId)
      ?.components
      ?.find((candidate) => candidate.id === componentId)
    const method = component?.manifestMapping ? 'PUT' : 'POST'

    try {
      const detail = await fetchJson(
        `/api/applications/${selectedApplicationId}/environments/${environmentId}/components/${componentId}/manifest-mapping`,
        {
          method,
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(manifestForm),
        },
      )
      setApplicationDetail(detail)
      resetManifestForm()
      setCatalogManagementMessage(component?.manifestMapping ? 'Manifest mapping updated' : 'Manifest mapping created')
      setCatalogManagementState('ready')
    } catch (error) {
      setCatalogManagementMessage(error.message)
      setCatalogManagementState('error')
    }
  }

  async function handleDeleteManifestMapping(environmentId, componentId) {
    if (!selectedApplicationId) {
      return
    }
    const confirmed = window.confirm('Delete this manifest mapping?')
    if (!confirmed) {
      return
    }

    setCatalogManagementState('submitting')
    setCatalogManagementMessage('')

    try {
      const detail = await fetchJson(
        `/api/applications/${selectedApplicationId}/environments/${environmentId}/components/${componentId}/manifest-mapping`,
        { method: 'DELETE' },
      )
      setApplicationDetail(detail)
      resetManifestForm()
      setCatalogManagementMessage('Manifest mapping deleted')
      setCatalogManagementState('ready')
    } catch (error) {
      setCatalogManagementMessage(error.message)
      setCatalogManagementState('error')
    }
  }

  async function handleCreateSourceRepository(event) {
    event.preventDefault()
    setSourceRepositoryState('submitting')
    setSourceRepositoryMessage('')

    try {
      const encryptedAccessToken = await encryptSourceRepositoryCredential(sourceRepositoryForm.accessToken)
      await fetchJson('/api/source-repositories', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          name: sourceRepositoryForm.name,
          provider: sourceRepositoryForm.provider,
          visibility: sourceRepositoryForm.visibility,
          repositoryUrl: sourceRepositoryForm.repositoryUrl,
          accountName: sourceRepositoryForm.accountName,
          description: sourceRepositoryForm.description,
          encryptedAccessToken,
        }),
      })
      setSourceRepositoryForm({
        name: '',
        provider: 'GITHUB',
        visibility: 'PUBLIC',
        repositoryUrl: '',
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
      if (selectedSourceRepositoryId === repositoryId) {
        setSelectedSourceRepositoryId(null)
        setSourceRepositoryView('list')
        setBuildProfiles([])
        setBuildExecutions([])
        setSelectedBuildExecutionId(null)
        resetBuildProfileForm()
      }
      await reloadSourceRepositories()
      setSourceRepositoryMessage('Source repository deleted')
      setSourceRepositoryState('ready')
    } catch (error) {
      setSourceRepositoryMessage(error.message)
      setSourceRepositoryState('error')
    }
  }

  async function handleSaveBuildProfile(event) {
    event.preventDefault()
    if (!selectedSourceRepositoryId) {
      return
    }

    setBuildProfileState('submitting')
    setBuildProfileMessage('')

    const path = editingBuildProfileId
      ? `/api/source-repositories/${selectedSourceRepositoryId}/build-profiles/${editingBuildProfileId}`
      : `/api/source-repositories/${selectedSourceRepositoryId}/build-profiles`
    const method = editingBuildProfileId ? 'PUT' : 'POST'

    try {
      const savedProfile = await fetchJson(path, {
        method,
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          name: buildProfileForm.name,
          ciTool: buildProfileForm.ciTool,
          workingDirectory: buildProfileForm.workingDirectory,
          script: buildProfileForm.script,
          description: buildProfileForm.description,
          targetApplicationId: Number(buildProfileForm.targetApplicationId),
          targetEnvironmentId: Number(buildProfileForm.targetEnvironmentId),
          targetComponentId: Number(buildProfileForm.targetComponentId),
        }),
      })
      await reloadBuildProfiles(selectedSourceRepositoryId)
      setSelectedBuildProfileId(savedProfile.id)
      resetBuildProfileForm()
      setBuildProfileMessage(editingBuildProfileId ? 'Build profile updated' : 'Build profile saved')
      setBuildProfileState('ready')
    } catch (error) {
      setBuildProfileMessage(error.message)
      setBuildProfileState('error')
    }
  }

  async function handleDeleteBuildProfile(profileId) {
    if (!selectedSourceRepositoryId) {
      return
    }

    const confirmed = window.confirm('Delete this build profile?')
    if (!confirmed) {
      return
    }

    setBuildProfileState('submitting')
    setBuildProfileMessage('')

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/source-repositories/${selectedSourceRepositoryId}/build-profiles/${profileId}`,
        { method: 'DELETE' },
      )
      if (!response.ok) {
        throw new Error(`API returned ${response.status}`)
      }
      await reloadBuildProfiles(selectedSourceRepositoryId)
      if (editingBuildProfileId === profileId) {
        resetBuildProfileForm()
      }
      if (selectedBuildProfileId === profileId) {
        setBuildExecutions([])
        setSelectedBuildExecutionId(null)
      }
      setBuildProfileMessage('Build profile deleted')
      setBuildProfileState('ready')
    } catch (error) {
      setBuildProfileMessage(error.message)
      setBuildProfileState('error')
    }
  }

  async function handleRunBuildProfile(profile) {
    if (!selectedSourceRepositoryId) {
      return
    }

    setBuildProfileState('submitting')
    setBuildProfileMessage('')
    setLastBuildExecution(null)

    try {
      const result = await fetchJson(
        `/api/source-repositories/${selectedSourceRepositoryId}/build-profiles/${profile.id}/run`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            requestedBy: buildProfileForm.requestedBy,
            branch: buildProfileForm.branch,
          }),
        },
      )
      setBuildProfileMessage(`${result.status}: ${result.statusMessage}`)
      setLastBuildExecution(result)
      await reloadSourceRepositories()
      await reloadBuildExecutions(selectedSourceRepositoryId, profile.id, { selectLatest: true })
      setBuildProfileState('ready')
    } catch (error) {
      setBuildProfileMessage(error.message)
      setBuildProfileState('error')
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
            selectedSourceRepository,
            setSelectedSourceRepositoryId,
            applications,
            applicationDetail,
            selectedApplicationId,
            setSelectedApplicationId,
            sourceRepositoryForm,
            setSourceRepositoryForm,
            handleCreateSourceRepository,
            handleDeleteSourceRepository,
            buildProfiles,
            selectedBuildProfile,
            selectedBuildProfileId,
            setSelectedBuildProfileId,
            buildProfileForm,
            setBuildProfileForm,
            buildProfileState,
            buildProfileMessage,
            lastBuildExecution,
            buildExecutions,
            selectedBuildExecution,
            setSelectedBuildExecutionId,
            buildExecutionState,
            buildExecutionMessage,
            editingBuildProfileId,
            handleSelectBuildProfileTool,
            handleSaveBuildProfile,
            handleEditBuildProfile,
            handleDeleteBuildProfile,
            handleRunBuildProfile,
            resetBuildProfileForm,
          )}

        </section>
      )}

      {activeSection === 'apps' && (
        <section className="app-management-workspace" aria-label="Application management workspace">
          {renderAppCommandEntry(
            appCommandText,
            setAppCommandText,
            handleAppCommandSubmit,
            appCommandState,
            appCommandMessage,
          )}

          {(appCommandView === 'list' || appCommandView === 'create' || appCommandView === 'detail') && (
            <div className={appCommandView === 'detail' ? 'catalog-layout' : 'catalog-layout single'}>
              {appCommandView === 'list' && (
              <section className="catalog-panel" aria-label="ArgoCD applications">
                {renderArgoCdApplicationList(argoCdApplications, catalogState)}
              </section>
              )}

              {appCommandView === 'create' && (
              <section className="detail-panel app-registration-panel" aria-label="Application registration">
                {catalogManagementMessage && (
                  <p className={catalogManagementState === 'error' ? 'status-message' : 'success-message'}>
                    {catalogManagementMessage}
                  </p>
                )}
                {renderArgoCdApplicationCreateForm(
                  argoCdApplicationForm,
                  setArgoCdApplicationForm,
                  handleCreateArgoCdApplication,
                  resetArgoCdApplicationForm,
                  catalogManagementState,
                )}
              </section>
              )}

              {appCommandView === 'detail' && (
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

              {catalogManagementMessage && (
                <p className={catalogManagementState === 'error' ? 'status-message' : 'success-message'}>
                  {catalogManagementMessage}
                </p>
              )}

              <div className="metadata-grid">
                {renderMetadata('Owner', applicationDetail.owner)}
                {renderMetadata('Repository', applicationDetail.repositoryUrl)}
                {renderMetadata('Environments', applicationDetail.environments.length)}
                {renderMetadata('Selected', selectedApplication?.name || applicationDetail.name)}
              </div>

              {renderEnvironmentForm(
                environmentForm,
                setEnvironmentForm,
                editingEnvironmentId,
                handleSaveEnvironment,
                resetEnvironmentForm,
                catalogManagementState,
              )}

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

                    <div className="management-actions">
                      <button onClick={() => handleEditEnvironment(environment)} type="button">
                        환경 수정
                      </button>
                      <button
                        className="danger-action"
                        onClick={() => handleDeleteEnvironment(environment.id)}
                        type="button"
                      >
                        환경 삭제
                      </button>
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
                        <span role="columnheader">Actions</span>
                      </div>
                      {environment.components.map((component) => (
                        <div className="component-row" key={component.id} role="row">
                          <span role="cell">{component.name}</span>
                          <span role="cell">{component.kind}</span>
                          <span role="cell">{component.deploymentName}</span>
                          <span role="cell">{component.serviceName}</span>
                          <span role="cell">
                            <strong>{component.imageRepository}</strong>
                            <small>
                              {component.manifestMapping
                                ? `${component.manifestMapping.valuesPath} / ${component.manifestMapping.imageTagKey}`
                                : 'No manifest mapping'}
                            </small>
                          </span>
                          <span role="cell" className="component-actions">
                            <button onClick={() => handleEditComponent(environment.id, component)} type="button">
                              수정
                            </button>
                            <button onClick={() => handleEditManifestMapping(component)} type="button">
                              Manifest
                            </button>
                            <button
                              className="danger-action"
                              onClick={() => handleDeleteComponent(environment.id, component.id)}
                              type="button"
                            >
                              삭제
                            </button>
                          </span>
                        </div>
                      ))}
                    </div>

                    {renderComponentForm(
                      environment,
                      componentForm,
                      setComponentForm,
                      componentFormEnvironmentId,
                      editingComponentId,
                      handleSaveComponent,
                      resetComponentForm,
                      catalogManagementState,
                    )}

                    {editingManifestComponentId && environment.components.some(
                      (component) => component.id === editingManifestComponentId,
                    ) && renderManifestMappingForm(
                      environment,
                      environment.components.find((component) => component.id === editingManifestComponentId),
                      manifestForm,
                      setManifestForm,
                      handleSaveManifestMapping,
                      handleDeleteManifestMapping,
                      resetManifestForm,
                      catalogManagementState,
                    )}
                  </article>
                ))}
              </div>
              </>
            )}
              </section>
              )}
            </div>
          )}
        </section>
      )}
    </main>
  )
}

function renderArgoCdApplicationList(applications, catalogState) {
  return (
    <>
      <div className="panel-heading">
        <div>
          <p className="eyebrow">Live Cluster</p>
          <h3>ArgoCD Applications</h3>
        </div>
        <span>{applications.length}</span>
      </div>

      {catalogState === 'loading' && <p className="muted">Loading ArgoCD applications...</p>}
      {catalogState === 'empty' && <p className="muted">No ArgoCD applications discovered.</p>}

      {catalogState === 'ready' && (
        <div className="argocd-application-list">
          {applications.map((application) => (
            <article className="argocd-application-card" key={`${application.argocdNamespace}-${application.name}`}>
              <div className="argocd-application-main">
                <div>
                  <p className="eyebrow">{application.project}</p>
                  <h3>{application.name}</h3>
                </div>
                <div className="argocd-status-pills">
                  <strong className={`status-value ${statusTone(application.syncStatus)}`}>
                    {application.syncStatus}
                  </strong>
                  <strong className={`status-value ${statusTone(application.healthStatus)}`}>
                    {application.healthStatus}
                  </strong>
                </div>
              </div>

              <div className="argocd-application-grid">
                {renderMetadata('ArgoCD Namespace', application.argocdNamespace)}
                {renderMetadata('Destination Namespace', application.destinationNamespace)}
                {renderMetadata('Cluster', application.destinationServer)}
                {renderMetadata('Revision', application.targetRevision)}
                {renderMetadata('Repository', application.sourceRepoUrl)}
                {renderMetadata('Path', application.sourcePath)}
                {renderMetadata('Operation', application.operationPhase)}
                {renderMetadata('Reconciled', formatDateTime(application.reconciledAt))}
              </div>
            </article>
          ))}
        </div>
      )}
    </>
  )
}

function renderArgoCdApplicationCreateForm(
  argoCdApplicationForm,
  setArgoCdApplicationForm,
  handleCreateArgoCdApplication,
  resetArgoCdApplicationForm,
  catalogManagementState,
) {
  return (
    <form className="management-form argocd-create-form" onSubmit={handleCreateArgoCdApplication}>
      <div className="panel-heading">
        <div>
          <p className="eyebrow">ArgoCD Application</p>
          <h3>Register application</h3>
        </div>
        <button className="secondary-action" onClick={resetArgoCdApplicationForm} type="button">
          초기화
        </button>
      </div>

      <div className="management-form-grid argocd-create-grid">
        <label>
          <span>Name</span>
          <input
            onChange={(event) => setArgoCdApplicationForm((current) => ({
              ...current,
              name: event.target.value,
            }))}
            required
            type="text"
            value={argoCdApplicationForm.name}
          />
        </label>
        <label>
          <span>Project</span>
          <input
            onChange={(event) => setArgoCdApplicationForm((current) => ({
              ...current,
              project: event.target.value,
            }))}
            required
            type="text"
            value={argoCdApplicationForm.project}
          />
        </label>
        <label className="wide-field">
          <span>Source Repository URL</span>
          <input
            onChange={(event) => setArgoCdApplicationForm((current) => ({
              ...current,
              sourceRepoUrl: event.target.value,
            }))}
            required
            type="url"
            value={argoCdApplicationForm.sourceRepoUrl}
          />
        </label>
        <label>
          <span>Source Path</span>
          <input
            onChange={(event) => setArgoCdApplicationForm((current) => ({
              ...current,
              sourcePath: event.target.value,
            }))}
            required
            type="text"
            value={argoCdApplicationForm.sourcePath}
          />
        </label>
        <label>
          <span>Target Revision</span>
          <input
            onChange={(event) => setArgoCdApplicationForm((current) => ({
              ...current,
              targetRevision: event.target.value,
            }))}
            required
            type="text"
            value={argoCdApplicationForm.targetRevision}
          />
        </label>
        <label className="wide-field">
          <span>Destination Server</span>
          <input
            onChange={(event) => setArgoCdApplicationForm((current) => ({
              ...current,
              destinationServer: event.target.value,
            }))}
            required
            type="text"
            value={argoCdApplicationForm.destinationServer}
          />
        </label>
        <label>
          <span>Destination Namespace</span>
          <input
            onChange={(event) => setArgoCdApplicationForm((current) => ({
              ...current,
              destinationNamespace: event.target.value,
            }))}
            required
            type="text"
            value={argoCdApplicationForm.destinationNamespace}
          />
        </label>
      </div>

      <div className="argocd-sync-options">
        {renderCheckbox('Automated sync', argoCdApplicationForm.automated, (checked) => (
          setArgoCdApplicationForm((current) => ({ ...current, automated: checked }))
        ))}
        {renderCheckbox('Prune', argoCdApplicationForm.prune, (checked) => (
          setArgoCdApplicationForm((current) => ({ ...current, prune: checked }))
        ))}
        {renderCheckbox('Self heal', argoCdApplicationForm.selfHeal, (checked) => (
          setArgoCdApplicationForm((current) => ({ ...current, selfHeal: checked }))
        ))}
      </div>

      <button disabled={catalogManagementState === 'submitting'} type="submit">
        {catalogManagementState === 'submitting' ? 'Registering...' : 'Register ArgoCD application'}
      </button>
    </form>
  )
}

function renderCheckbox(label, checked, onChange) {
  return (
    <label className="checkbox-field">
      <input
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
        type="checkbox"
      />
      <span>{label}</span>
    </label>
  )
}

function renderAppCommandEntry(
  appCommandText,
  setAppCommandText,
  handleAppCommandSubmit,
  appCommandState,
  appCommandMessage,
) {
  return (
    <section className="app-command-panel" aria-label="Application command entry">
      <form className="app-command-form" onSubmit={handleAppCommandSubmit}>
        <span aria-hidden="true">+</span>
        <input
          aria-label="Application command"
          onChange={(event) => setAppCommandText(event.target.value)}
          placeholder="등록된 어플리케이션 목록 보여줘"
          type="text"
          value={appCommandText}
        />
        <button disabled={appCommandState === 'submitting'} type="submit">
          {appCommandState === 'submitting' ? 'Loading...' : 'Run'}
        </button>
      </form>
      {appCommandMessage && (
        <p className={appCommandState === 'error' ? 'status-message' : 'success-message'}>
          {appCommandMessage}
        </p>
      )}
    </section>
  )
}

function renderEnvironmentForm(
  environmentForm,
  setEnvironmentForm,
  editingEnvironmentId,
  handleSaveEnvironment,
  resetEnvironmentForm,
  catalogManagementState,
) {
  return (
    <form className="management-form" onSubmit={handleSaveEnvironment}>
      <div className="panel-heading">
        <h3>{editingEnvironmentId ? 'Edit environment' : 'New environment'}</h3>
        {editingEnvironmentId && (
          <button className="secondary-action" onClick={resetEnvironmentForm} type="button">
            새 환경
          </button>
        )}
      </div>
      <div className="management-form-grid">
        <label>
          <span>Environment</span>
          <input
            onChange={(event) => setEnvironmentForm((current) => ({
              ...current,
              environment: event.target.value,
            }))}
            required
            type="text"
            value={environmentForm.environment}
          />
        </label>
        <label>
          <span>Namespace</span>
          <input
            onChange={(event) => setEnvironmentForm((current) => ({
              ...current,
              namespace: event.target.value,
            }))}
            required
            type="text"
            value={environmentForm.namespace}
          />
        </label>
        <label>
          <span>ArgoCD app</span>
          <input
            onChange={(event) => setEnvironmentForm((current) => ({
              ...current,
              argocdApplicationName: event.target.value,
            }))}
            required
            type="text"
            value={environmentForm.argocdApplicationName}
          />
        </label>
        <label>
          <span>Service URL</span>
          <input
            onChange={(event) => setEnvironmentForm((current) => ({
              ...current,
              serviceUrl: event.target.value,
            }))}
            required
            type="text"
            value={environmentForm.serviceUrl}
          />
        </label>
        <label className="wide-field">
          <span>Helm values path</span>
          <input
            onChange={(event) => setEnvironmentForm((current) => ({
              ...current,
              helmValuesPath: event.target.value,
            }))}
            required
            type="text"
            value={environmentForm.helmValuesPath}
          />
        </label>
      </div>
      <button disabled={catalogManagementState === 'submitting'} type="submit">
        {editingEnvironmentId ? 'Update environment' : 'Create environment'}
      </button>
    </form>
  )
}

function renderComponentForm(
  environment,
  componentForm,
  setComponentForm,
  componentFormEnvironmentId,
  editingComponentId,
  handleSaveComponent,
  resetComponentForm,
  catalogManagementState,
) {
  const isActive = componentFormEnvironmentId === environment.id

  if (!isActive) {
    return (
      <button className="secondary-action" onClick={() => resetComponentForm(environment.id)} type="button">
        컴포넌트 추가
      </button>
    )
  }

  return (
    <form className="management-form" onSubmit={(event) => handleSaveComponent(event, environment.id)}>
      <div className="panel-heading">
        <h3>{editingComponentId ? 'Edit component' : 'New component'}</h3>
        <button className="secondary-action" onClick={() => resetComponentForm(null)} type="button">
          닫기
        </button>
      </div>
      <div className="management-form-grid">
        <label>
          <span>Name</span>
          <input
            onChange={(event) => setComponentForm((current) => ({
              ...current,
              name: event.target.value,
            }))}
            required
            type="text"
            value={componentForm.name}
          />
        </label>
        <label>
          <span>Kind</span>
          <input
            onChange={(event) => setComponentForm((current) => ({
              ...current,
              kind: event.target.value,
            }))}
            required
            type="text"
            value={componentForm.kind}
          />
        </label>
        <label>
          <span>Deployment</span>
          <input
            onChange={(event) => setComponentForm((current) => ({
              ...current,
              deploymentName: event.target.value,
            }))}
            required
            type="text"
            value={componentForm.deploymentName}
          />
        </label>
        <label>
          <span>Service</span>
          <input
            onChange={(event) => setComponentForm((current) => ({
              ...current,
              serviceName: event.target.value,
            }))}
            required
            type="text"
            value={componentForm.serviceName}
          />
        </label>
        <label className="wide-field">
          <span>Image repository</span>
          <input
            onChange={(event) => setComponentForm((current) => ({
              ...current,
              imageRepository: event.target.value,
            }))}
            required
            type="text"
            value={componentForm.imageRepository}
          />
        </label>
      </div>
      <button disabled={catalogManagementState === 'submitting'} type="submit">
        {editingComponentId ? 'Update component' : 'Create component'}
      </button>
    </form>
  )
}

function renderManifestMappingForm(
  environment,
  component,
  manifestForm,
  setManifestForm,
  handleSaveManifestMapping,
  handleDeleteManifestMapping,
  resetManifestForm,
  catalogManagementState,
) {
  return (
    <form
      className="management-form manifest-form"
      onSubmit={(event) => handleSaveManifestMapping(event, environment.id, component.id)}
    >
      <div className="panel-heading">
        <div>
          <p className="eyebrow">Manifest Mapping</p>
          <h3>{component.name}</h3>
        </div>
        <button className="secondary-action" onClick={resetManifestForm} type="button">
          닫기
        </button>
      </div>
      <div className="management-form-grid">
        <label className="wide-field">
          <span>Manifest repository</span>
          <input
            onChange={(event) => setManifestForm((current) => ({
              ...current,
              manifestRepositoryUrl: event.target.value,
            }))}
            required
            type="url"
            value={manifestForm.manifestRepositoryUrl}
          />
        </label>
        <label>
          <span>Branch</span>
          <input
            onChange={(event) => setManifestForm((current) => ({
              ...current,
              manifestBranch: event.target.value,
            }))}
            required
            type="text"
            value={manifestForm.manifestBranch}
          />
        </label>
        <label>
          <span>Values path</span>
          <input
            onChange={(event) => setManifestForm((current) => ({
              ...current,
              valuesPath: event.target.value,
            }))}
            required
            type="text"
            value={manifestForm.valuesPath}
          />
        </label>
        <label>
          <span>Image tag key</span>
          <input
            onChange={(event) => setManifestForm((current) => ({
              ...current,
              imageTagKey: event.target.value,
            }))}
            required
            type="text"
            value={manifestForm.imageTagKey}
          />
        </label>
      </div>
      <div className="management-actions">
        <button disabled={catalogManagementState === 'submitting'} type="submit">
          {component.manifestMapping ? 'Update mapping' : 'Create mapping'}
        </button>
        {component.manifestMapping && (
          <button
            className="danger-action"
            onClick={() => handleDeleteManifestMapping(environment.id, component.id)}
            type="button"
          >
            Mapping 삭제
          </button>
        )}
      </div>
    </form>
  )
}

function renderSourceRepositoryPanel(
  sourceRepositories,
  sourceRepositoryState,
  sourceRepositoryMessage,
  sourceRepositoryView,
  setSourceRepositoryView,
  selectedSourceRepository,
  setSelectedSourceRepositoryId,
  applications,
  applicationDetail,
  selectedApplicationId,
  setSelectedApplicationId,
  sourceRepositoryForm,
  setSourceRepositoryForm,
  handleCreateSourceRepository,
  handleDeleteSourceRepository,
  buildProfiles,
  selectedBuildProfile,
  selectedBuildProfileId,
  setSelectedBuildProfileId,
  buildProfileForm,
  setBuildProfileForm,
  buildProfileState,
  buildProfileMessage,
  lastBuildExecution,
  buildExecutions,
  selectedBuildExecution,
  setSelectedBuildExecutionId,
  buildExecutionState,
  buildExecutionMessage,
  editingBuildProfileId,
  handleSelectBuildProfileTool,
  handleSaveBuildProfile,
  handleEditBuildProfile,
  handleDeleteBuildProfile,
  handleRunBuildProfile,
  resetBuildProfileForm,
) {
  const isGitLab = sourceRepositoryForm.provider === 'GITLAB'
  const tokenLabel = isGitLab ? 'GitLab password / access token' : 'GitHub password / access token'
  const isRegisterView = sourceRepositoryView === 'register'
  const isDetailView = sourceRepositoryView === 'detail'
  const targetEnvironments = applicationDetail?.environments || []
  const selectedTargetEnvironment = targetEnvironments.find(
    (environment) => String(environment.id) === String(buildProfileForm.targetEnvironmentId),
  )
  const targetComponents = selectedTargetEnvironment?.components || []
  const selectedTargetComponent = targetComponents.find(
    (component) => String(component.id) === String(buildProfileForm.targetComponentId),
  )

  return (
    <section className="source-repository-panel">
      <div className="source-repository-header">
        <div>
          <p className="eyebrow">CI/CD Workspace</p>
          <h2>Pipeline designer</h2>
          <p>
            Select a source, define the build profile, preview the CI flow, and
            prepare an execution request for platform-cicd.
          </p>
        </div>
        <div className="source-repository-actions">
          <span>{sourceRepositories.length} repos</span>
          <button
            className="panel-action"
            onClick={() => {
              setSourceRepositoryView(isRegisterView || isDetailView ? 'list' : 'register')
              if (!isRegisterView && !isDetailView) {
                resetBuildProfileForm()
              }
            }}
            type="button"
          >
            {isRegisterView || isDetailView ? '목록' : '등록'}
          </button>
        </div>
      </div>

      {sourceRepositoryMessage && (
        <p className={sourceRepositoryState === 'error' ? 'status-message' : 'success-message'}>
          {sourceRepositoryMessage}
        </p>
      )}

      {isRegisterView ? (
        renderSourceRepositoryRegistrationForm(
          sourceRepositoryForm,
          setSourceRepositoryForm,
          handleCreateSourceRepository,
          sourceRepositoryState,
          tokenLabel,
        )
      ) : (
        <div className={isDetailView ? 'cicd-designer-layout detail' : 'cicd-designer-layout list'}>
          {!isDetailView && (
          <aside className="cicd-sources-column" aria-label="Source repositories">
            <div className="panel-heading">
              <h3>Sources</h3>
              <span>{sourceRepositories.length}</span>
            </div>

            {sourceRepositoryState === 'loading' && <p className="muted">Loading source repositories...</p>}
            {sourceRepositoryState !== 'loading' && sourceRepositories.length === 0 && (
              <p className="muted">No source repositories registered.</p>
            )}

            <div className="source-repository-list">
              {sourceRepositories.map((repository) => (
                <article
                  className={
                    selectedSourceRepository?.id === repository.id
                      ? 'source-repository-card compact active'
                      : 'source-repository-card compact'
                  }
                  key={repository.id}
                  onClick={() => {
                    setSelectedSourceRepositoryId(repository.id)
                    setSourceRepositoryView('detail')
                  }}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                      setSelectedSourceRepositoryId(repository.id)
                      setSourceRepositoryView('detail')
                    }
                  }}
                  role="button"
                  tabIndex="0"
                >
                  <div className="source-repository-main">
                    <strong>{repository.name}</strong>
                    <a
                      href={repository.repositoryUrl}
                      onClick={(event) => event.stopPropagation()}
                      rel="noreferrer"
                      target="_blank"
                    >
                      {repository.repositoryUrl}
                    </a>
                  </div>

                  <div className="source-compact-meta">
                    <span>{repository.provider || 'UNKNOWN'}</span>
                    <span>{repository.visibility}</span>
                    <span>{repository.accountName || 'No account'}</span>
                    <span>{repository.credentialConfigured ? 'Auth saved' : 'Auth missing'}</span>
                  </div>

                  <div className="source-compact-stats">
                    <span>{repository.cloneCount ?? 0} clones</span>
                    <span>{repository.buildCount ?? 0} builds</span>
                  </div>

                  <button
                    className="danger-action"
                    onClick={(event) => {
                      event.stopPropagation()
                      handleDeleteSourceRepository(repository.id)
                    }}
                    type="button"
                  >
                    제거
                  </button>
                </article>
              ))}
            </div>
          </aside>
          )}

          {isDetailView && !selectedSourceRepository && (
            <div className="empty-designer-state">
              <p className="eyebrow">Repository Detail</p>
              <h3>No source repository selected</h3>
              <p className="muted">Return to the source repository list and select a repository.</p>
            </div>
          )}

          {isDetailView && selectedSourceRepository && (
          <section className="pipeline-designer-panel" aria-label="Pipeline designer">
              <>
                <div className="designer-summary">
                  <div>
                    <p className="eyebrow">Selected Source</p>
                    <h3>{selectedSourceRepository.name}</h3>
                    <a href={selectedSourceRepository.repositoryUrl} rel="noreferrer" target="_blank">
                      {selectedSourceRepository.repositoryUrl}
                    </a>
                  </div>
                  <div className="designer-badges">
                    <span>{selectedSourceRepository.provider}</span>
                    <span>{selectedSourceRepository.visibility}</span>
                    <span>{selectedSourceRepository.credentialConfigured ? 'Password/token saved' : 'Auth missing'}</span>
                    <span>{selectedSourceRepository.cloneCount ?? 0} clones</span>
                    <span>{selectedSourceRepository.buildCount ?? 0} builds</span>
                  </div>
                </div>

                <div className="pipeline-designer-grid">
                  <section className="build-profile-list">
                    <div className="panel-heading">
                      <h3>Build profiles</h3>
                      <span>{buildProfiles.length}</span>
                    </div>

                    {buildProfileState === 'loading' && <p className="muted">Loading build profiles...</p>}
                    {buildProfileState !== 'loading' && buildProfiles.length === 0 && (
                      <p className="muted">No build profiles saved.</p>
                    )}

                    {buildProfiles.map((profile) => (
                      <article
                        className={
                          selectedBuildProfileId === profile.id
                            ? 'build-profile-card active'
                            : 'build-profile-card'
                        }
                        key={profile.id}
                      >
                        <div>
                          <strong>{profile.name}</strong>
                          <small>{profile.workingDirectory}</small>
                        </div>
                        <span className="status-value neutral">{profile.ciTool}</span>
                        <small>{profile.targetApplicationName || 'No target'} / {profile.targetComponentName || 'No component'}</small>
                        <p>{profile.description}</p>
                        <code>{profile.script.split('\n')[0]}</code>
                        <div className="profile-actions">
                          <button onClick={() => setSelectedBuildProfileId(profile.id)} type="button">
                            Select
                          </button>
                          <button onClick={() => handleEditBuildProfile(profile)} type="button">
                            수정
                          </button>
                          <button
                            className="danger-action"
                            onClick={() => handleDeleteBuildProfile(profile.id)}
                            type="button"
                          >
                            삭제
                          </button>
                        </div>
                      </article>
                    ))}
                  </section>

                  <section className="pipeline-editor-stack">
                    {renderPipelinePreview(buildProfileForm.ciTool)}

                    <form className="build-profile-form" onSubmit={handleSaveBuildProfile}>
                      <div className="panel-heading">
                        <h3>{editingBuildProfileId ? 'Edit build profile' : 'New build profile'}</h3>
                        {editingBuildProfileId && (
                          <button className="secondary-action" onClick={resetBuildProfileForm} type="button">
                            새 profile
                          </button>
                        )}
                      </div>

                      {buildProfileMessage && (
                        <p className={buildProfileState === 'error' ? 'status-message' : 'success-message'}>
                          {buildProfileMessage}
                        </p>
                      )}

                      <div className="profile-form-grid">
                        <label>
                          <span>Name</span>
                          <input
                            onChange={(event) => setBuildProfileForm((current) => ({
                              ...current,
                              name: event.target.value,
                            }))}
                            required
                            type="text"
                            value={buildProfileForm.name}
                          />
                        </label>
                        <label>
                          <span>CI tool</span>
                          <select
                            onChange={(event) => handleSelectBuildProfileTool(event.target.value)}
                            value={buildProfileForm.ciTool}
                          >
                            <option value="SHELL">Shell</option>
                            <option value="JENKINS">Jenkins</option>
                            <option value="GITHUB_ACTIONS">GitHub Actions</option>
                            <option value="GITLAB_CI">GitLab CI</option>
                          </select>
                        </label>
                        <label>
                          <span>Working directory</span>
                          <input
                            onChange={(event) => setBuildProfileForm((current) => ({
                              ...current,
                              workingDirectory: event.target.value,
                            }))}
                            required
                            type="text"
                            value={buildProfileForm.workingDirectory}
                          />
                        </label>
                        <label>
                          <span>Description optional</span>
                          <input
                            onChange={(event) => setBuildProfileForm((current) => ({
                              ...current,
                              description: event.target.value,
                            }))}
                            type="text"
                            value={buildProfileForm.description}
                          />
                        </label>
                      </div>

                      <div className="profile-target-panel">
                        <div>
                          <p className="eyebrow">Manifest Target</p>
                          <strong>
                            {selectedTargetComponent
                              ? selectedTargetComponent.imageRepository
                              : 'Select target component'}
                          </strong>
                        </div>

                        <div className="profile-form-grid">
                          <label>
                            <span>Application</span>
                            <select
                              onChange={(event) => {
                                setSelectedApplicationId(Number(event.target.value))
                                setBuildProfileForm((current) => ({
                                  ...current,
                                  targetApplicationId: event.target.value,
                                  targetEnvironmentId: '',
                                  targetComponentId: '',
                                }))
                              }}
                              required
                              value={buildProfileForm.targetApplicationId || selectedApplicationId || ''}
                            >
                              {applications.map((application) => (
                                <option key={application.id} value={application.id}>
                                  {application.name}
                                </option>
                              ))}
                            </select>
                          </label>

                          <label>
                            <span>Environment</span>
                            <select
                              onChange={(event) => {
                                const environment = targetEnvironments.find(
                                  (candidate) => String(candidate.id) === event.target.value,
                                )
                                setBuildProfileForm((current) => ({
                                  ...current,
                                  targetEnvironmentId: event.target.value,
                                  targetComponentId: environment?.components?.[0]?.id
                                    ? String(environment.components[0].id)
                                    : '',
                                }))
                              }}
                              required
                              value={buildProfileForm.targetEnvironmentId}
                            >
                              {targetEnvironments.map((environment) => (
                                <option key={environment.id} value={environment.id}>
                                  {environment.environment}
                                </option>
                              ))}
                            </select>
                          </label>

                          <label>
                            <span>Component</span>
                            <select
                              onChange={(event) => setBuildProfileForm((current) => ({
                                ...current,
                                targetComponentId: event.target.value,
                              }))}
                              required
                              value={buildProfileForm.targetComponentId}
                            >
                              {targetComponents.map((component) => (
                                <option key={component.id} value={component.id}>
                                  {component.name}
                                </option>
                              ))}
                            </select>
                          </label>
                        </div>
                      </div>

                      <label className="script-editor">
                        <span>Build script</span>
                        <textarea
                          onChange={(event) => setBuildProfileForm((current) => ({
                            ...current,
                            script: event.target.value,
                          }))}
                          required
                          rows="14"
                          value={buildProfileForm.script}
                        />
                      </label>

                      <button disabled={buildProfileState === 'submitting'} type="submit">
                        {editingBuildProfileId ? 'Update profile' : 'Save profile'}
                      </button>
                    </form>
                  </section>
                </div>

                {renderBuildExecutionHistory(
                  selectedBuildProfile,
                  buildExecutions,
                  selectedBuildExecution,
                  setSelectedBuildExecutionId,
                  buildExecutionState,
                  buildExecutionMessage,
                )}
              </>
          </section>
          )}

          {isDetailView && selectedSourceRepository && (
          <aside className="run-panel" aria-label="Run build profile">
            <p className="eyebrow">Run Panel</p>
            <h3>Build execution</h3>

            {!selectedSourceRepository && <p className="muted">Select a source repository first.</p>}

            {selectedSourceRepository && !selectedBuildProfile && (
              <p className="muted">Save a build profile before running a build.</p>
            )}

            {selectedSourceRepository && selectedBuildProfile && (
              <>
                <div className="run-target">
                  <span>Selected profile</span>
                  <strong>{selectedBuildProfile.name}</strong>
                  <small>{selectedBuildProfile.ciTool} / {selectedBuildProfile.workingDirectory}</small>
                  <small>
                    {selectedBuildProfile.targetApplicationName || 'No target'}
                    {' / '}
                    {selectedBuildProfile.targetEnvironment || 'No environment'}
                    {' / '}
                    {selectedBuildProfile.targetComponentName || 'No component'}
                  </small>
                </div>

                {renderPipelinePreview(selectedBuildProfile.ciTool, 'compact')}

                <div className="run-parameter-grid">
                  <label>
                    <span>Requested by</span>
                    <input
                      onChange={(event) => setBuildProfileForm((current) => ({
                        ...current,
                        requestedBy: event.target.value,
                      }))}
                      required
                      type="text"
                      value={buildProfileForm.requestedBy}
                    />
                  </label>
                  <label>
                    <span>Branch</span>
                    <input
                      onChange={(event) => setBuildProfileForm((current) => ({
                        ...current,
                        branch: event.target.value,
                      }))}
                      required
                      type="text"
                      value={buildProfileForm.branch}
                    />
                  </label>
                </div>

                <button
                  className="run-action"
                  disabled={buildProfileState === 'submitting'}
                  onClick={() => handleRunBuildProfile(selectedBuildProfile)}
                  type="button"
                >
                  {buildProfileState === 'submitting' && <span className="button-spinner" aria-hidden="true" />}
                  <span>{buildProfileState === 'submitting' ? 'Running...' : 'Run build'}</span>
                </button>

                {lastBuildExecution && (
                  <section className="execution-result" aria-label="Last build execution result">
                    <div className="execution-result-heading">
                      <div>
                        <p className="eyebrow">Latest Execution</p>
                        <h4>#{lastBuildExecution.historyId || lastBuildExecution.executionId || 'Pending'}</h4>
                      </div>
                      <strong className={`status-value ${statusTone(lastBuildExecution.status)}`}>
                        {lastBuildExecution.status}
                      </strong>
                    </div>

                    <dl className="execution-facts latest-summary">
                      <div>
                        <dt>Profile</dt>
                        <dd>{lastBuildExecution.buildProfileName || selectedBuildProfile.name}</dd>
                      </div>
                      <div>
                        <dt>Image</dt>
                        <dd>{lastBuildExecution.imageReference || lastBuildExecution.imageTag || 'None'}</dd>
                      </div>
                      <div>
                        <dt>Manifest</dt>
                        <dd>{lastBuildExecution.manifestUpdateStatus || 'Not requested'}</dd>
                      </div>
                      <div>
                        <dt>Finished</dt>
                        <dd>{formatDateTime(lastBuildExecution.finishedAt || lastBuildExecution.createdAt)}</dd>
                      </div>
                    </dl>
                  </section>
                )}
              </>
            )}
          </aside>
          )}
        </div>
      )}
    </section>
  )
}

function renderBuildExecutionHistory(
  selectedBuildProfile,
  buildExecutions,
  selectedBuildExecution,
  setSelectedBuildExecutionId,
  buildExecutionState,
  buildExecutionMessage,
) {
  return (
    <section className="execution-history-panel" aria-label="Build execution history">
      <div className="panel-heading">
        <div>
          <p className="eyebrow">Execution History</p>
          <h3>{selectedBuildProfile ? selectedBuildProfile.name : 'Select a build profile'}</h3>
        </div>
        <span>{buildExecutions.length}</span>
      </div>

      {!selectedBuildProfile && <p className="muted">Select a build profile to view execution history.</p>}
      {selectedBuildProfile && buildExecutionState === 'loading' && <p className="muted">Loading executions...</p>}
      {buildExecutionMessage && (
        <p className={buildExecutionState === 'error' ? 'status-message' : 'success-message'}>
          {buildExecutionMessage}
        </p>
      )}
      {selectedBuildProfile && buildExecutionState !== 'loading' && buildExecutions.length === 0 && (
        <p className="muted">No executions recorded for this build profile.</p>
      )}

      {buildExecutions.length > 0 && (
        <div className="execution-history-table" role="table" aria-label="Build profile executions">
          <div className="execution-history-row execution-history-head" role="row">
            <span role="columnheader">Status</span>
            <span role="columnheader">Runner</span>
            <span role="columnheader">Branch</span>
            <span role="columnheader">Image</span>
            <span role="columnheader">Manifest</span>
            <span role="columnheader">Requested by</span>
            <span role="columnheader">Finished</span>
          </div>

          {buildExecutions.map((execution) => (
            <button
              className={
                selectedBuildExecution?.id === execution.id
                  ? 'execution-history-row active'
                  : 'execution-history-row'
              }
              key={execution.id}
              onClick={() => setSelectedBuildExecutionId(execution.id)}
              role="row"
              type="button"
            >
              <span role="cell" className={`status-value ${statusTone(execution.status)}`}>{execution.status}</span>
              <span role="cell">{execution.runnerType}</span>
              <span role="cell">{execution.branch}</span>
              <span role="cell">{execution.imageReference || execution.imageTag || 'None'}</span>
              <span role="cell">{execution.manifestUpdateStatus || 'None'}</span>
              <span role="cell">{execution.requestedBy}</span>
              <span role="cell">{formatDateTime(execution.finishedAt || execution.createdAt)}</span>
            </button>
          ))}
        </div>
      )}

      {selectedBuildExecution && (
        <section className="execution-history-detail" aria-label="Selected execution detail">
          <div className="execution-detail-grid">
            <div>
              <p className="eyebrow">Request Spec</p>
              <dl>
                <div><dt>Runner</dt><dd>{selectedBuildExecution.runnerType}</dd></div>
                <div><dt>Branch</dt><dd>{selectedBuildExecution.branch}</dd></div>
                <div><dt>Requested value</dt><dd>{selectedBuildExecution.requestedValue}</dd></div>
                <div><dt>Requested by</dt><dd>{selectedBuildExecution.requestedBy}</dd></div>
                <div><dt>External execution</dt><dd>{selectedBuildExecution.externalExecutionId ?? 'None'}</dd></div>
              </dl>
            </div>

            <div>
              <p className="eyebrow">Clone Result</p>
              <dl>
                <div><dt>Status</dt><dd>{selectedBuildExecution.cloneStatus || 'None'}</dd></div>
                <div><dt>Message</dt><dd>{selectedBuildExecution.cloneMessage || 'None'}</dd></div>
                <div><dt>Checkout</dt><dd>{selectedBuildExecution.checkoutPath || 'None'}</dd></div>
              </dl>
            </div>

            <div>
              <p className="eyebrow">Image Artifact</p>
              <dl>
                <div><dt>Repository</dt><dd>{selectedBuildExecution.imageRepository || 'None'}</dd></div>
                <div><dt>Tag</dt><dd>{selectedBuildExecution.imageTag || 'None'}</dd></div>
                <div><dt>Digest</dt><dd>{selectedBuildExecution.imageDigest || 'None'}</dd></div>
                <div><dt>Reference</dt><dd>{selectedBuildExecution.imageReference || 'None'}</dd></div>
              </dl>
            </div>

            <div>
              <p className="eyebrow">Manifest Update</p>
              <dl>
                <div><dt>Status</dt><dd>{selectedBuildExecution.manifestUpdateStatus || 'None'}</dd></div>
                <div><dt>Message</dt><dd>{selectedBuildExecution.manifestUpdateMessage || 'None'}</dd></div>
                <div><dt>Changed file</dt><dd>{selectedBuildExecution.manifestChangedFilePath || 'None'}</dd></div>
                <div><dt>Updated</dt><dd>{formatDateTime(selectedBuildExecution.manifestUpdatedAt)}</dd></div>
              </dl>
            </div>

            <div>
              <p className="eyebrow">Build Result</p>
              <dl>
                <div><dt>Status</dt><dd>{selectedBuildExecution.status}</dd></div>
                <div><dt>Message</dt><dd>{selectedBuildExecution.statusMessage || 'None'}</dd></div>
                <div><dt>Started</dt><dd>{formatDateTime(selectedBuildExecution.startedAt)}</dd></div>
                <div><dt>Finished</dt><dd>{formatDateTime(selectedBuildExecution.finishedAt)}</dd></div>
                <div><dt>Exit code</dt><dd>{selectedBuildExecution.exitCode ?? 'None'}</dd></div>
              </dl>
            </div>
          </div>

          {selectedBuildExecution.externalRunUrl && (
            <a href={selectedBuildExecution.externalRunUrl} rel="noreferrer" target="_blank">
              Open external run
            </a>
          )}

          <pre>{selectedBuildExecution.logSummary || 'No log output captured.'}</pre>
        </section>
      )}
    </section>
  )
}

function renderSourceRepositoryRegistrationForm(
  sourceRepositoryForm,
  setSourceRepositoryForm,
  handleCreateSourceRepository,
  sourceRepositoryState,
  tokenLabel,
) {
  return (
    <form className="source-repo-form" onSubmit={handleCreateSourceRepository}>
      <label>
        <span>Repository host</span>
        <select
          onChange={(event) => {
            const provider = event.target.value
            setSourceRepositoryForm((current) => ({
              ...current,
              provider,
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
        <span>Description optional</span>
        <input
          onChange={(event) => setSourceRepositoryForm((current) => ({
            ...current,
            description: event.target.value,
          }))}
          type="text"
          value={sourceRepositoryForm.description}
        />
      </label>
      <button disabled={sourceRepositoryState === 'submitting'} type="submit">Register source</button>
    </form>
  )
}

function renderPipelinePreview(ciTool, density = 'default') {
  const preview = PIPELINE_PREVIEWS[ciTool] ?? PIPELINE_PREVIEWS.SHELL

  return (
    <section className={density === 'compact' ? 'pipeline-preview compact' : 'pipeline-preview'}>
      <div className="panel-heading">
        <div>
          <p className="eyebrow">{preview.label}</p>
          <h3>{preview.unit}</h3>
        </div>
      </div>
      <div className="pipeline-stage-flow">
        {preview.stages.map((stage, index) => (
          <div className="pipeline-stage" key={`${stage.name}-${index}`}>
            <span>{index + 1}</span>
            <strong>{stage.name}</strong>
            <small>{stage.detail}</small>
          </div>
        ))}
      </div>
      <p className="pipeline-preview-note">Template preview only. It is not parsed from the saved script.</p>
    </section>
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
  if (['Synced', 'Healthy', 'Succeeded', 'SUCCEEDED', 'READY'].includes(value)) {
    return 'good'
  }

  if (['OutOfSync', 'Progressing', 'Running', 'RUNNING', 'REQUESTED', 'DISPATCHED', 'PROGRESSING', 'WARNING'].includes(value)) {
    return 'pending'
  }

  if (['Degraded', 'Failed', 'FAILED', 'Error', 'Missing', 'MISSING'].includes(value)) {
    return 'bad'
  }

  return 'neutral'
}

function formatDateTime(value) {
  if (!value) {
    return 'None'
  }

  return new Date(value).toLocaleString()
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
