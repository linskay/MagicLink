import { useState, useEffect } from 'react'
import './styles.css'

const { ipcRenderer } = window.require ? window.require('electron') : { ipcRenderer: null }

export default function App() {
  const [nodes, setNodes] = useState([])
  const [vpnActive, setVpnActive] = useState(false)
  const [selectedCountry, setSelectedCountry] = useState('ALL')
  const [mode, setMode] = useState('FULL')
  const [status, setStatus] = useState('INITIALIZING')
  const [connectedNode, setConnectedNode] = useState(null)

  const fetchNodes = async () => {
    if (!ipcRenderer) return
    try {
      const nodeList = await ipcRenderer.invoke('nodes-list')
      setNodes(nodeList || [])
      if (status === 'INITIALIZING' && nodeList?.length === 0) {
        setStatus('FIRST_RUN')
      } else if (status === 'INITIALIZING' || status === 'FIRST_RUN') {
        setStatus('READY')
      }
    } catch (e) {
      console.error('Failed to fetch nodes', e)
    }
  }

  const refreshNodes = async () => {
    if (!ipcRenderer) return
    const prevStatus = status
    setStatus('UPDATING')
    try {
      await ipcRenderer.invoke('nodes-refresh')
      await fetchNodes()
    } catch (e) {
      console.error('Refresh failed', e)
      alert('Failed to sync. Make sure java is installed.')
    } finally {
      setStatus('READY')
    }
  }

  useEffect(() => {
    fetchNodes()
    const interval = setInterval(fetchNodes, 30000)
    return () => clearInterval(interval)
  }, [])

  const toggleMagic = async () => {
    if (vpnActive) {
      setStatus('READY')
      await ipcRenderer?.invoke('vpn-stop')
      setVpnActive(false)
      setConnectedNode(null)
    } else {
      let filteredNodes = nodes
      if (selectedCountry !== 'ALL') {
        filteredNodes = nodes.filter(n => n.country === selectedCountry)
      }

      filteredNodes.sort((a, b) => (a.latency || 999) - (b.latency || 999))

      const node = filteredNodes[0]
      if (node) {
        setStatus('CONNECTING')
        const config = {
          outbounds: [
            {
              type: node.type,
              tag: `proxy-${node.id}`,
              server: node.host,
              server_port: node.port,
              ...node.params
            },
            { type: 'direct', tag: 'direct' }
          ],
          route: {
            rules: mode === 'TG' ? [
              { domain: ['telegram.org', 't.me', 'telegram.me', 'tdesktop.com', 'telegra.ph'], outbound: `proxy-${node.id}` },
              { ip_cidr: ["91.108.4.0/22", "91.108.8.0/22", "91.108.12.0/22", "91.108.16.0/22", "91.108.20.0/22", "91.108.56.0/22", "149.154.160.0/20"], outbound: `proxy-${node.id}` },
              { outbound: 'direct' }
            ] : (mode === 'PROXY' ? [
              { outbound: `proxy-${node.id}` }
            ] : [])
          }
        }

        try {
          await ipcRenderer?.invoke('vpn-start', config)
          setVpnActive(true)
          setConnectedNode(node)
          setStatus('CONNECTED')
        } catch (e) {
          alert(`Error: ${e.message}`)
          setStatus('READY')
        }
      } else {
        alert('No suitable nodes found. Try to Force Sync.')
      }
    }
  }

  const countries = ['ALL', ...new Set(nodes.map(n => n.country).filter(Boolean))]

  return (
    <div className='app'>
      <div className='status-bar'>
        <span className={`status-dot ${status}`}></span>
        {status === 'FIRST_RUN' ? 'INITIAL MAGIC SEARCH...' :
          status === 'UPDATING' ? 'SYNCING SOURCES...' :
            status === 'CONNECTING' ? 'WARPING...' :
              status === 'CONNECTED' ? `SECURED: ${connectedNode?.country} (${connectedNode?.latency}ms)` : `FOUND ${nodes.length} MAGICAL NODES`}
      </div>

      <h1>MagicLink</h1>

      <div className='magic-area'>
        <motion.button
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
          className={`magic-btn ${vpnActive ? 'active' : ''}`}
          onClick={toggleMagic}
        >
          {vpnActive ? 'TERMINATE MAGIC' : '🪄 INITIATE MAGIC'}
        </motion.button>
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

      <div className='footer'>
        <button className='refresh-btn' onClick={refreshNodes}>🔄 Force Sync</button>
      </div>
    </div>
  )
}