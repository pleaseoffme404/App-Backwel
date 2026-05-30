import express from 'express';
import path from 'path';
import { fileURLToPath } from 'url';
import { createProxyMiddleware } from 'http-proxy-middleware';
import { configRouter } from './routes/config.routes.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const port = process.env.PORT || 3000;

app.use(
  '/api/v1',
  createProxyMiddleware({
    target: 'http://localhost:8080',
    changeOrigin: true
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
});