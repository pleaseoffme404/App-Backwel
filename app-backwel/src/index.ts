import express from 'express';
import path from 'path';
import { fileURLToPath } from 'url';
import { createProxyMiddleware } from 'http-proxy-middleware';
import { configRouter } from './routes/config.routes.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const port = process.env.PORT || 3000;

const apiGatewayUrl = process.env.API_GATEWAY_URL || 'http://localhost:8080';

app.get('/api/v1/user/userinfo', (req, res) => {
  console.log(`[MOCK] Bypass Java 500 Error -> Entregando perfil ADMIN falso`);
  res.json({
    id: "00000000-0000-0000-0000-000000000000",
    email: "admin@local.dev",
    role: "OWNER",
    name: "Admin",
    surname: "Temporal",
    phoneNumber: "0000000000",
    pictureUrl: "",
    countryCode: "MX",
    currencyCode: "MXN"
  });
});

app.post('/api/v1/user/complete-account', (req, res) => {
  console.log(`[MOCK] Bypass Java 403 Error -> Simulando creación de cuenta`);
  res.json({
    id: "00000000-0000-0000-0000-000000000000",
    email: "admin@local.dev",
    role: "OWNER",
    name: "Admin",
    surname: "Temporal"
  });
});


app.use(
  '/api/v1',
  createProxyMiddleware({
    target: apiGatewayUrl,
    changeOrigin: true,
    pathRewrite: (path, req) => (req as any).originalUrl,
    on: {
      proxyReq: (proxyReq, req) => {
        console.log(`[PROXY REQ] ${req.method} ${(req as any).originalUrl} -> ${apiGatewayUrl}${proxyReq.path}`);
      },
      proxyRes: (proxyRes, req) => {
        console.log(`[PROXY RES] ${req.method} ${(req as any).originalUrl} | Status: ${proxyRes.statusCode}`);
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

app.use('/api/config', configRouter);

app.use(express.static(path.join(__dirname, '../dist/client')));

app.get(/.*/, (req, res) => {
  res.sendFile(path.join(__dirname, '../dist/client/index.html'));
});

app.listen(port, () => {
  console.log(`BFF Server running on port ${port}`);
  console.log(`Proxy target mapped to: ${apiGatewayUrl}`);
});