import { logger } from '../utils/logger.js';

export const authMiddleware = (req, res, next) => {
  if (req.headers['content-type'] !== 'application/json') {
    logger.error('Invalid content type', { ip: req.ip });
    return res.status(400).json({ error: 'validation_error' });
  }

  const secret = req.headers['x-service-secret'];
  
  if (!secret || secret !== process.env.SERVICE_EMAIL_SECRET) {
    logger.error('Invalid secret attempt', { ip: req.ip });
    return res.status(401).json({ error: 'invalid_secret' });
  }
  
  next();
};