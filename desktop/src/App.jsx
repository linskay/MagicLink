import { motion } from 'framer-motion'

export default function App(){
  return (
    <div className='app'>
      <h1>MagicLink</h1>
      <div className='cards'>
        <button className='card'>🌍 Full Magic</button>
        <button className='card'>📡 Telegram Only</button>
        <button className='card'>🔌 Proxy Only</button>
      </div>
    </div>
  )
}