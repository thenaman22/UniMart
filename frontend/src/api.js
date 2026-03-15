const API_BASE = 'http://localhost:8080'

export async function api(path, options = {}) {
  const token = localStorage.getItem('unimart-token')
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { 'X-Auth-Token': token } : {}),
      ...(options.headers || {})
    },
    ...options
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Request failed' }))
    throw new Error(error.message || 'Request failed')
  }

  if (response.status === 204) {
    return null
  }

  return response.json()
}
