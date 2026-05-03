import { Router } from 'express';
import { z } from 'zod';
import fs from 'fs/promises';
import path from 'path';
import { randomUUID } from 'crypto';
import { sendEmail } from '../services/mailer.js';
import { logger } from '../utils/logger.js';

const router = Router();

const emailSchema = z.object({
  template_id: z.string(),
  recipients: z.object({
    to: z.array(z.string().email()).min(1),
    cc: z.array(z.string().email()).optional(),
    bcc: z.array(z.string().email()).optional()
  }),
  data: z.record(z.any()).default({}),
  meta: z.object({
    locale: z.string().optional(),
    priority: z.enum(['low', 'normal', 'high']).optional()
  }).optional(),
  overrides: z.object({
    subject: z.string().optional()
  }).optional()
});

router.get('/templates/:template_id', async (req, res) => {
  try {
    const { template_id } = req.params;
    const configPath = path.join(process.cwd(), 'templates', template_id, 'config.json');
    const configData = await fs.readFile(configPath, 'utf-8');
    res.status(200).json(JSON.parse(configData));
  } catch (error) {
    res.status(404).json({ error: 'template_not_found' });
  }
});

router.post('/emails', async (req, res) => {
  try {
    const parsed = emailSchema.safeParse(req.body);
    
    if (!parsed.success) {
      logger.error('Validation failed', { errors: parsed.error.issues });
      const isRecipientError = parsed.error.issues.some(i => i.path.includes('recipients'));
      return res.status(400).json({ error: isRecipientError ? 'invalid_recipient' : 'validation_error' });
    }

    const payload = parsed.data;
    const configPath = path.join(process.cwd(), 'templates', payload.template_id, 'config.json');
    const htmlPath = path.join(process.cwd(), 'templates', payload.template_id, 'template.html');

    let configData;
    let htmlContent;

    try {
      configData = JSON.parse(await fs.readFile(configPath, 'utf-8'));
      htmlContent = await fs.readFile(htmlPath, 'utf-8');
    } catch (err) {
      return res.status(404).json({ error: 'template_not_found' });
    }

    const providedKeys = Object.keys(payload.data);
    const missing = configData.required_data.filter(k => !providedKeys.includes(k));
    
    if (missing.length > 0) {
      logger.error('Missing required fields', { missing });
      return res.status(400).json({ error: 'missing_required_field' });
    }

    const allowedKeys = [...configData.required_data, ...(configData.optional_data || [])];
    const extraKeys = providedKeys.filter(k => !allowedKeys.includes(k));
    
    if (extraKeys.length > 0) {
      logger.info('Ignored extra fields', { extraKeys, template_id: payload.template_id });
    }

    let finalHtml = htmlContent;
    for (const key of allowedKeys) {
      if (payload.data[key] !== undefined) {
        const regex = new RegExp(`\\{\\{${key}\\}\\}`, 'g');
        finalHtml = finalHtml.replace(regex, String(payload.data[key]));
      }
    }

    const finalSubject = payload.overrides?.subject || configData.subject;

    await sendEmail({
      to: payload.recipients.to,
      cc: payload.recipients.cc,
      bcc: payload.recipients.bcc,
      subject: finalSubject,
      html: finalHtml
    });

    logger.info('Email sent successfully', { template_id: payload.template_id, to: payload.recipients.to });
    
    res.status(202).json({
      status: 'accepted',
      id: randomUUID()
    });

  } catch (error) {
    if (error.message === 'smtp_error') {
      return res.status(500).json({ error: 'smtp_error' });
    }
    logger.error('Internal error', { error: error.message });
    res.status(500).json({ error: 'validation_error' });
  }
});

export default router;