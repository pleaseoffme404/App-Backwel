import dotenv from 'dotenv';
dotenv.config();

export const config = {
    port: process.env.SIMULATOR_PORT || 3005,
    emailServiceUrl: process.env.EMAIL_SERVICE_URL || 'http://email-service:3001/emails',
    cronSchedule: process.env.CRON_SCHEDULE || '*/1 * * * *', // Por defecto cada minuto
    serviceSecret: process.env.SERVICE_EMAIL_SECRET || 'backwel-internal-key'
};

if (!process.env.EMAIL_SERVICE_URL && process.env.NODE_ENV === 'production') {
    console.warn("Advertencia: EMAIL_SERVICE_URL no está definida, usando valor por defecto de Docker.");
}