const { app, BrowserWindow } = require('electron')
function createWindow(){
  const win = new BrowserWindow({width:1000,height:700})
  win.loadURL('http://127.0.0.1:5173')
}
app.whenReady().then(createWindow)