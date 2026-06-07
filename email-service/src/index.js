import 'dotenv/config';
import express from 'express';
import { authMiddleware } from './middlewares/auth.js';
import routes from './api/routes.js';
import { verifyConnection } from './services/mailer.js';

console.log("--- CONFIGURACIÓN EMAIL-SERVICE ---");
console.log("Secret definido en memoria:", process.env.SERVICE_EMAIL_SECRET ? "OK" : "NO DEFINIDO");
// OJO: Imprime solo los primeros 3 caracteres para no exponer tu clave completa en los logs
console.log("Primeros 3 caracteres del secret:", process.env.SERVICE_EMAIL_SECRET?.substring(0, 3));
const app = express();

app.use(express.json());

app.get('/health', async (req, res) => {
  try {
    await verifyConnection();
    res.status(200).json({ status: 'ok', smtp: 'connected' });
  } catch (error) {
    res.status(503).json({ status: 'error', smtp: 'unreachable' });
  }
});

app.use(authMiddleware);
app.use('/', routes);

app.listen(3001, '0.0.0.0');