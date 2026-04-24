import { useState, useEffect } from 'react'
import './styles.css'

const { ipcRenderer } = window.require ? window.require('electron') : { ipcRenderer: null }

export default function App() {
  const [nodes, setNodes] = useState([])
  const [vpnActive, setVpnActive] = useState(false)
  const [selectedCountry, setSelectedCountry] = useState('ALL')
  const [mode, setMode] = useState('FULL')
  const [status, setStatus] = useState('READY') // READY, UPDATING, CONNECTING, CONNECTED

  const fetchNodes = async () => {
    if (!ipcRenderer) return
    try {
      const nodeList = await ipcRenderer.invoke('nodes-list')
      setNodes(nodeList || [])
    } catch (e) {
      console.error('Failed to fetch nodes', e)
    }
  }

  const refreshNodes = async () => {
    if (!ipcRenderer) return
    setStatus('UPDATING')
    try {
      await ipcRenderer.invoke('nodes-refresh')
      await fetchNodes()
    } catch (e) {
      console.error('Refresh failed', e)
    } finally {
      setStatus('READY')
    }
  }

  useEffect(() => {
    fetchNodes()
    const interval = setInterval(fetchNodes, 30000) // Refresh local list every 30s
    return () => clearInterval(interval)
  }, [])

  const toggleMagic = async () => {
    if (vpnActive) {
      setStatus('READY')
      await ipcRenderer?.invoke('vpn-stop')
      setVpnActive(false)
    } else {
      // Filtering logic
      let filteredNodes = nodes
      if (selectedCountry !== 'ALL') {
        filteredNodes = nodes.filter(n => n.country === selectedCountry)
      }

      // Sort by latency (lowest first)
      filteredNodes.sort((a, b) => (a.latency || 999) - (b.latency || 999))

      const node = filteredNodes[0]
      if (node) {
        setStatus('CONNECTING')
        const config = {
          outbounds: [
            {
              type: node.type,
              tag: 'proxy',
              server: node.host,
              server_port: node.port,
              ...node.params
            },
            { type: 'direct', tag: 'direct' }
          ],
          route: {
            rules: mode === 'TG' ? [
              { domain: ['telegram.org', 't.me', 'telegram.me', 'tdesktop.com'], outbound: 'proxy' },
              { outbound: 'direct' }
            ] : (mode === 'PROXY' ? [
              { outbound: 'proxy' }
            ] : [])
          }
        }

        try {
          await ipcRenderer?.invoke('vpn-start', config)
          setVpnActive(true)
          setStatus('CONNECTED')
        } catch (e) {
          console.error('VPN Start failed', e)
          setStatus('READY')
        }
      } else {
        alert('No suitable nodes found for selection.')
      }
    }
  }

  const countries = ['ALL', ...new Set(nodes.map(n => n.country).filter(Boolean))]

  return (
    <div className='app'>
      <div className='status-bar'>
        <span className={`status-dot ${status}`}></span>
        {status === 'UPDATING' ? 'MAGIC SYNCING...' :
          status === 'CONNECTING' ? 'WARPING...' :
            status === 'CONNECTED' ? 'SECURED' : `FOUND ${nodes.length} NODES`}
      </div>

      <h1>MagicLink</h1>

      <div className='magic-area'>
        <button
          className={`magic-btn ${vpnActive ? 'active' : ''}`}
          onClick={toggleMagic}
        >
          {vpnActive ? 'TERMINATE MAGIC' : '🪄 INITIATE MAGIC'}
        </button>
      </div>

      <div className='modes'>
        <button
          className={`card ${mode === 'FULL' ? 'selected' : ''}`}
          onClick={() => setMode('FULL')}
        >🌍 Full Magic</button>
        <button
          className={`card ${mode === 'TG' ? 'selected' : ''}`}
          onClick={() => setMode('TG')}
        >📡 Telegram Only</button>
        <button
          className={`card ${mode === 'PROXY' ? 'selected' : ''}`}
          onClick={() => setMode('PROXY')}
        >🔌 Proxy Only</button>
      </div>

      <div className='country-selector'>
        {countries.map(c => (
          <button
            key={c}
            className={`country-tag ${selectedCountry === c ? 'selected' : ''}`}
            onClick={() => setSelectedCountry(c)}
          >
            {c}
          </button>
        ))}
      </div>

      <button className='refresh-btn' onClick={refreshNodes}>🔄 Force Sync</button>
    </div>
  )
}