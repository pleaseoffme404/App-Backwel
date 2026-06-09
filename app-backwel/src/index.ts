import express from 'express';
import path from 'path';
import { fileURLToPath } from 'url';
import { createProxyMiddleware } from 'http-proxy-middleware';
import { configRouter } from './routes/config.routes.js'; // <- Restaurado

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const port = (process.env.PORT || 3000) as number;

const apiGatewayUrl = (process.env.API_GATEWAY_URL || 'http://localhost:8080') as string;

app.post('/api/v1/auth/unlock', express.json(), (req, res) => {
  console.log(`[MOCK] Bypass Java -> Verificando password para unlock`);
  if (req.body.password === 'admin123') {
    res.json({ success: true });
  } else {
    res.status(401).json({ error: 'invalid_password' });
  }
});

app.use(
  '/api/v1',
  createProxyMiddleware({
    target: apiGatewayUrl,
    changeOrigin: true,
   pathRewrite: (path, req) => (req as any).originalUrl,
    on: {
      proxyReq: (proxyReq, req) => {
        console.log(`\n--- [DEBUG PROXY REQ START] ---`);
        console.log(`[REQ] ${req.method} ${(req as any).originalUrl} -> ${apiGatewayUrl}${proxyReq.path}`);
        
        const cookies = req.headers.cookie;
        console.log(`[REQ COOKIES IN]:`, cookies ? cookies : 'None');
        
        if (cookies && req.method && ['POST', 'PUT', 'DELETE', 'PATCH'].includes(req.method)) {
          const xsrfCookie = cookies.split(';').find(c => c.trim().startsWith('XSRF-TOKEN='));
          if (xsrfCookie) {
            const tokenValue = xsrfCookie.split('=')[1].trim();
            proxyReq.setHeader('X-XSRF-TOKEN', tokenValue);
            console.log(`[REQ CSRF INJECTED]:`, tokenValue.substring(0, 10) + '...');
          } else {
            console.log(`[REQ CSRF MISSING]: No se encontró XSRF-TOKEN en las cookies.`);
          }
        }
        
        console.log(`[REQ HEADERS OUT]:`, proxyReq.getHeaders());
        console.log(`--- [DEBUG PROXY REQ END] ---\n`);
      },
      proxyRes: (proxyRes, req) => {
        console.log(`\n--- [DEBUG PROXY RES START] ---`);
        console.log(`[RES] ${req.method} ${(req as any).originalUrl} | Status: ${proxyRes.statusCode}`);
        console.log(`[RES HEADERS]:`, proxyRes.headers);
        console.log(`--- [DEBUG PROXY RES END] ---\n`);
      },
      error: (err, req, res) => {
        console.error(`[PROXY ERR] ${req.method} ${(req as any).originalUrl} | Error:`, err.message);
        const response = res as any;
        if (response.writeHead && response.headersSent !== undefined) {
          if (!response.headersSent) {
            response.writeHead(504, { 'Content-Type': 'application/json' });
            response.end(JSON.stringify({ error: 'Proxy Error', details: err.message }));
          }
        }
      }
    }
  })
);

app.use(express.json());

// Tus rutas nativas de frontend (BFF) siguen vivas aquí
app.use('/api/config', configRouter);

app.use(express.static(path.join(__dirname, '../dist/client')));

app.get(/.*/, (req, res) => {
  res.sendFile(path.join(__dirname, '../dist/client/index.html'));
});

app.listen(port, () => {
  console.log(`BFF Server running on port ${port}`);
  console.log(`Proxy target mapped to: ${apiGatewayUrl}`);
});