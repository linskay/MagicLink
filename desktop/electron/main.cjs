const { app, BrowserWindow, ipcMain } = require('electron')
const path = require('path')
const vpnManager = require('./VpnManager.js')

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

ipcMain.handle('vpn-start', async (event, config) => {
  return await vpnManager.start(config)
})

ipcMain.handle('vpn-stop', async () => {
  return await vpnManager.stop()
})

app.whenReady().then(createWindow)