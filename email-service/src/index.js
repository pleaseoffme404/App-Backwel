import 'dotenv/config';
import express from 'express';
import { authMiddleware } from './middlewares/auth.js';
import routes from './api/routes.js';
import { verifyConnection } from './services/mailer.js';

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