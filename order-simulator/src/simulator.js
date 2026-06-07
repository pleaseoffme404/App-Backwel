import { triggerEmailWebhook } from './services/notificationClient.js';

const mockOrders = [
    { id: 'BKW-1001', customer: 'Luis Martínez', email: 'luis@example.com', status: 'processing', total: '850.00' },
    { id: 'BKW-1002', customer: 'Daniel Prado', email: 'daniel@example.com', status: 'shipped', total: '1,200.00' }
];

const STATUS_FLOW = {
    'processing': { next: 'shipped', template: 'order_status_updated', label: 'Enviado' },
    'shipped': { next: 'in_transit', template: 'order_status_updated', label: 'En Reparto' },
    'in_transit': { next: 'delivered', template: 'order_status_updated', label: 'Entregado' },
    'delivered': { next: null }
};

export async function processOrdersStateCycle() {
    console.log(`\n[CronJob] Ejecutando escaneo cíclico de pedidos transaccionales...`);

    let updatesCount = 0;

    for (const order of mockOrders) {
        const currentState = order.status;
        const flow = STATUS_FLOW[currentState];

        if (flow && flow.next) {
            const oldState = order.status;
            order.status = flow.next;
            updatesCount++;

            console.log(`[Simulador] Pedido ${order.id} cambia de [${oldState}] -> [${order.status}]`);

            await triggerEmailWebhook(
                flow.template,
                order.customer,
                order.id,
                {
                    new_status: flow.label,
                    total_amount: order.total,
                    customer_email: order.email
                }
            );
        }
    }

    if (updatesCount === 0) {
        console.log(`[Simulador] Sin pedidos pendientes de avance. Reiniciando ciclo de prueba para flujo constante...`);
        mockOrders.forEach(o => o.status = 'processing');
    }
}

export function createNewMockOrder(orderData) {
    const newOrder = {
        id: `BKW-${Math.floor(1000 + Math.random() * 9000)}`,
        status: 'processing',
        ...orderData
    };
    mockOrders.push(newOrder);
    console.log(`[Simulador] Nuevo pedido registrado manualmente para flujo cron: ${newOrder.id}`);

    triggerEmailWebhook('order_completed', newOrder.customer, newOrder.id, {
        total_amount: newOrder.total,
        customer_email: newOrder.email
    });

    return newOrder;
}