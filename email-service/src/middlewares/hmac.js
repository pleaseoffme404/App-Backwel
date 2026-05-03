import crypto from 'crypto';
import { logger } from '../utils/logger.js';

export const hmacMiddleware = (req, res, next) => {
  const signature = req.headers['x-hmac-signature'];
  const timestamp = req.headers['x-timestamp'];

  if (!signature || !timestamp) {
    return res.status(401).json({ error: 'invalid_secret' });
  }

  const payload = timestamp + JSON.stringify(req.body);
  const expected = crypto.createHmac('sha256', process.env.SERVICE_EMAIL_SECRET).update(payload).digest('hex');

  if (signature !== expected) {
    logger.error('HMAC mismatch', { expected, signature });
    return res.status(401).json({ error: 'invalid_secret' });
  }

  next();
};