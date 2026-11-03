/**
 * HESTORA Frontend Static Web Server (Node.js)
 * Automatically finds an available port if 3000 is occupied (e.g., by nginx)
 */

const http = require('http');
const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');

let requestedPort = parseInt(process.env.PORT, 10) || 3000;
const BASE_DIR = path.resolve(__dirname);

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.woff2': 'font/woff2',
  '.woff': 'font/woff',
  '.ttf': 'font/ttf'
};

function startServer(port) {
  const server = http.createServer((req, res) => {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', '*');

    if (req.method === 'OPTIONS') {
      res.writeHead(204);
      res.end();
      return;
    }

    let urlPath = req.url.split('?')[0];
    if (urlPath === '/' || urlPath === '') {
      urlPath = '/index.html';
    }

    // Decode URL
    let safePath = path.normalize(decodeURIComponent(urlPath)).replace(/^(\.\.[\/\\])+/, '');
    let filePath = path.join(BASE_DIR, safePath);

    fs.stat(filePath, (err, stats) => {
      if (err || !stats.isFile()) {
        res.writeHead(404, { 'Content-Type': 'text/html; charset=utf-8' });
        res.end(`
          <!doctype html>
          <html>
          <head><meta charset="utf-8"><title>404 Not Found</title></head>
          <body style="font-family: sans-serif; text-align: center; padding: 50px;">
            <h2>404 - Không tìm thấy trang</h2>
            <p>Đường dẫn <code>${urlPath}</code> không tồn tại trên máy chủ HESTORA.</p>
            <p><a href="/">← Quay lại Trang chủ</a> | <a href="/hotel-management.html">Trang Quản trị</a></p>
          </body>
          </html>
        `);
        return;
      }

      const ext = path.extname(filePath).toLowerCase();
      const contentType = MIME_TYPES[ext] || 'application/octet-stream';

      res.writeHead(200, { 'Content-Type': contentType });
      fs.createReadStream(filePath).pipe(res);
    });
  });

  server.on('error', (err) => {
    if (err.code === 'EADDRINUSE') {
      console.log(`[THÔNG BÁO] Cổng ${port} đang bị chiếm (bởi nginx hoặc dịch vụ khác). Đang thử cổng ${port + 1}...`);
      startServer(port + 1);
    } else {
      console.error('[LỖI]', err);
    }
  });

  server.listen(port, () => {
    const url = `http://localhost:${port}/`;
    console.log('==========================================================');
    console.log(`   HESTORA - Khởi chạy Website trên cổng Port ${port}`);
    console.log('==========================================================');
    console.log(` Trang chủ khách hàng: ${url}index.html`);
    console.log(` Trang quản trị hệ thống: ${url}hotel-management.html`);
    console.log('==========================================================');

    // Auto open browser on Windows
    const startCmd = process.platform === 'win32' ? `start ${url}` : `open ${url}`;
    exec(startCmd, () => {});
  });
}

startServer(requestedPort);

