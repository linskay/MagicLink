const { app, BrowserWindow, ipcMain } = require('electron')
const path = require('path')
const fs = require('fs')
const vpnManager = require('./VpnManager.js')
const { exec } = require('child_process')

function createWindow() {
  const win = new BrowserWindow({
    width: 1000,
    height: 700,
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false
    }
  })
  win.loadURL('http://127.0.0.1:5173')
}

// IPC Handlers
ipcMain.handle('nodes-list', async () => {
  const nodesPath = path.join(app.getAppPath(), 'data', 'nodes.json')
  if (fs.existsSync(nodesPath)) {
    return JSON.parse(fs.readFileSync(nodesPath, 'utf8'))
  }
  return []
})

ipcMain.handle('nodes-refresh', async () => {
  return new Promise((resolve, reject) => {
    // Assuming jar is in a known location or we run via mvn for dev
    const jarPath = path.join(app.getAppPath(), '..', 'core', 'target', 'core-1.0-SNAPSHOT.jar')
    exec(`java -jar ${jarPath} --export`, (error, stdout, stderr) => {
      if (error) {
        console.error(`exec error: ${error}`)
        reject(error)
        return
      }
      resolve(true)
    })
  })
})

ipcMain.handle('vpn-start', async (event, config) => {
  return await vpnManager.start(config)
})

ipcMain.handle('vpn-stop', async () => {
  return await vpnManager.stop()
})

app.whenReady().then(createWindow)