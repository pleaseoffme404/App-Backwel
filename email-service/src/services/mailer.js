import nodemailer from 'nodemailer';
import { logger } from '../utils/logger.js';

const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST,
  port: Number(process.env.SMTP_PORT),
  secure: false,
  auth: {
    user: process.env.SMTP_USER,
    pass: process.env.SMTP_PASSWORD
  },
  tls: {
    ciphers: 'SSLv3'
  }
});

export const verifyConnection = async () => {
  return await transporter.verify();
};

export const sendEmail = async ({ to, cc, bcc, subject, html }) => {
  try {
    await transporter.sendMail({
      from: process.env.EMAIL_FROM,
      to: to.join(', '),
      cc: cc ? cc.join(', ') : undefined,
      bcc: bcc ? bcc.join(', ') : undefined,
      subject,
      html
    });
  } catch (error) {
    logger.error('SMTP Delivery failed', { error: error.message });
    throw new Error('smtp_error');
  }
};