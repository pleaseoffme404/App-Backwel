import cron from 'node-cron';
import { config } from './config/environment.js';
import { processOrdersStateCycle, createNewMockOrder } from './simulator.js';

console.log('=== Iniciando Motor de Simulación Backwel CronJob ===');
console.log(`Configuración: Expresión Cron [${config.cronSchedule}]`);
console.log(`Configuración: Apuntando a Email Service en [${config.emailServiceUrl}]`);

cron.schedule(config.cronSchedule, async () => {
    try {
        await processOrdersStateCycle();
    } catch (error) {
        console.error(`[Fatal Cron] Error crítico en la ejecución del hilo principal: ${error.message}`);
    }
});

import express from 'express';
const app = express();
app.use(express.json());

app.post('/mock-orders', (req, res) => {
    const { customer, email, total } = req.body;
    if (!customer || !email || !total) {
        return res.status(400).json({ error: 'Faltan campos mandatorios (customer, email, total)' });
    }
    const order = createNewMockOrder({ customer, email, total });
    res.status(201).json({ message: 'Pedido inyectado al flujo del simulador', order });
});

app.listen(config.port, () => {
    console.log(`Servidor de control del simulador escuchando en puerto local: ${config.port}`);
});