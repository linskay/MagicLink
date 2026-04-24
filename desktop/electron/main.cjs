const { app, BrowserWindow, ipcMain } = require('electron')
const path = require('path')
const fs = require('fs')
const { exec } = require('child_process')
const vpnManager = require('./VpnManager.js')

let mainWindow;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1000,
    height: 700,
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false
    }
  })

  if (app.isPackaged) {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'))
  } else {
    mainWindow.loadURL('http://127.0.0.1:5173')
  }
}

// IPC Handlers
ipcMain.handle('nodes-list', async () => {
  const nodesPath = path.join(app.getPath('userData'), 'nodes.json')
  if (fs.existsSync(nodesPath)) {
    try {
      return JSON.parse(fs.readFileSync(nodesPath, 'utf8'))
    } catch (e) {
      console.error('Failed to parse nodes.json', e)
      return []
    }
  }
  return []
})

ipcMain.handle('nodes-refresh', async () => {
  return new Promise((resolve, reject) => {
    const resourcesPath = app.isPackaged ? process.resourcesPath : app.getAppPath()

    // In dev, jar is in ../core/target/. In prod, we'll put it in resources/bin/ or root
    let jarPath;
    if (app.isPackaged) {
      jarPath = path.join(resourcesPath, 'bin', 'core.jar')
    } else {
      jarPath = path.join(app.getAppPath(), '..', 'core', 'target', 'core-1.0-SNAPSHOT.jar')
    }

    const exportPath = path.join(app.getPath('userData'), 'nodes.json')
    const command = `java -jar "${jarPath}" --refresh --export "${exportPath}"`

    exec(command, (error, stdout, stderr) => {
      if (error) {
        console.error(`core error: ${error}`)
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

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})