import { logger } from '../utils/logger.js';

export const initQueue = async () => {
  const host = process.env.QUEUE_HOST;
  if (!host) return;
};

export const sendToQueue = async (payload) => {
  logger.info('Queued message', { payload });
};