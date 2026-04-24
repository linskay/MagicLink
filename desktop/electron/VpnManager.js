const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');
const { app } = require('electron');

class VpnManager {
    constructor() {
        this.process = null;
    }

    getBinaryPath() {
        const resourcesPath = app.isPackaged ? process.resourcesPath : app.getAppPath();
        if (app.isPackaged) {
            return path.join(resourcesPath, 'bin', 'sing-box.exe');
        }
        return path.join(app.getAppPath(), 'bin', 'sing-box.exe'); // In dev, we keep it in desktop/bin
    }

    async start(config) {
        if (this.process) {
            await this.stop();
        }

        const binaryPath = this.getBinaryPath();
        if (!fs.existsSync(binaryPath)) {
            throw new Error(`Sing-box binary not found at ${binaryPath}`);
        }

        const configPath = path.join(app.getPath('userData'), 'vpn_config.json');
        fs.writeFileSync(configPath, JSON.stringify(config, null, 2));

        this.process = spawn(binaryPath, ['run', '-c', configPath]);

        this.process.stdout.on('data', (data) => {
            console.log(`sing-box: ${data}`);
        });

        this.process.stderr.on('data', (data) => {
            console.error(`sing-box error: ${data}`);
        });

        this.process.on('close', (code) => {
            console.log(`sing-box exited with code ${code}`);
            this.process = null;
        });
    }

    async stop() {
        if (this.process) {
            this.process.kill();
            this.process = null;
        }
    }
}

module.exports = new VpnManager();
