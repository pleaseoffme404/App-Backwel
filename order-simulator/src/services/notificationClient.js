import axios from 'axios';
import crypto from 'crypto';
import { config } from '../config/environment.js';

export async function triggerEmailWebhook(templateId, customerName, orderId, additionalData = {}) {
    const payload = {
        template_id: templateId,
        recipients: {
            to: [additionalData.customer_email || "cliente@backwel.com"]
        },
        data: {
            customer_name: customerName,
            order_id: orderId,
            ...additionalData
        }
    };

    const timestamp = Date.now().toString();
    const payloadString = JSON.stringify(payload);

    const dataToSign = timestamp + payloadString;


    const signature = crypto
        .createHmac('sha256', config.serviceSecret)
        .update(dataToSign)
        .digest('hex');

    //console.log("--- DEBUG WEBHOOK ---");
    //console.log("Secret enviado:", config.serviceSecret ? "OK" : "NO DEFINIDO");
    try {
        console.log(`[Webhook] Enviando evento '${templateId}' para el pedido ${orderId}...`);
        const response = await axios.post(config.emailServiceUrl, payload, {
            headers: {
                'Content-Type': 'application/json',
                'x-hmac-signature': signature,
                'x-timestamp':timestamp,
                'x-service-secret': config.serviceSecret
            }
        });
        console.log(`[Webhook] Notificación aceptada por el servicio de correo: ${response.status}`);
    } catch (error) {
        if (error.response) {

            console.error(`[Error Webhook] Rechazado por el servidor de correos (Status ${error.response.status}):`, error.response.data);
        } else {
            // Fallo de red: Node.js no encuentra el servidor (Ej. ECONNREFUSED)
            console.error(`[Error Webhook] Fallo de red (¿Está encendido el email-service?): ${error.message}`);
        }
    }
}