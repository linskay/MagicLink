import { useState, useEffect } from 'react'
import './styles.css'

const { ipcRenderer } = window.require ? window.require('electron') : { ipcRenderer: null }

export default function App() {
  const [nodes, setNodes] = useState([])
  const [vpnActive, setVpnActive] = useState(false)
  const [selectedCountry, setSelectedCountry] = useState('ALL')
  const [mode, setMode] = useState('FULL')

  const toggleMagic = async () => {
    if (vpnActive) {
      await ipcRenderer?.invoke('vpn-stop')
      setVpnActive(false)
    } else {
      const node = nodes[0] // Simplified selection
      if (node) {
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
            rules: mode === 'TG' ? [{ domain: ['telegram.org', 't.me'], outbound: 'proxy' }, { outbound: 'direct' }] : []
          }
        }
        await ipcRenderer?.invoke('vpn-start', config)
        setVpnActive(true)
      } else {
        alert('No nodes available. Please wait for core update.')
      }
    }
  }

  const countries = ['ALL', ...new Set(nodes.map(n => n.country).filter(Boolean))]

  return (
    <div className='app'>
      <h1>MagicLink</h1>

      <div className='magic-area'>
        <button
          className={`magic-btn ${vpnActive ? 'active' : ''}`}
          onClick={toggleMagic}
        >
          {vpnActive ? 'STOP MAGIC' : '🪄 ENABLE MAGIC'}
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
    </div>
  )
}