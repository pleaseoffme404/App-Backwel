import express from 'express';
import path from 'path';
import { fileURLToPath } from 'url';
import { createProxyMiddleware } from 'http-proxy-middleware';
import { configRouter } from './routes/config.routes.js'; // <- Restaurado

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
app.get('/api/v1/catalog/products', (req, res) => {
  console.log(`[MOCK] Bypass de Java -> Entregando catálogo POS`);
  res.json([
    { id: 'p1', name: 'Cable UTP Cat 6', price: 15.00, stock: 150, image: '' },
    { id: 'p2', name: 'Switch Gigabit 8 Puertos', price: 450.00, stock: 0, image: '' },
    { id: 'p3', name: 'Router WiFi 6', price: 1200.00, stock: 12, image: '' },
    { id: 'p4', name: 'Patch Panel 24 Puertos', price: 850.00, stock: 5, image: '' },
    { id: 'p5', name: 'Gabinete Rack 12U', price: 3200.00, stock: 2, image: '' },
    { id: 'p6', name: 'Bobina Fibra Óptica 1000m', price: 4500.00, stock: 4, image: '' },
    { id: 'p7', name: 'Conectores RJ45 (Caja 100)', price: 120.00, stock: 30, image: '' },
    { id: 'p8', name: 'Cámara IP Domo PTZ', price: 1800.00, stock: 8, image: '' }
  ]);
});

app.post('/api/v1/auth/unlock', express.json(), (req, res) => {
  console.log(`[MOCK] Verificando contraseña para desbloquear Admin`);
  const { password } = req.body;
  // Contraseña temporal por defecto para el mock
  if (password === 'admin123') {
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