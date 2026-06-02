import { useState, useEffect } from 'react';
import { Skeleton } from '../../../shared/ui/Skeleton';

interface DashboardMetrics {
  totalUsers: number;
  totalProducts: number;
  totalOrders: number;
  totalRevenue: number;
}

export default function Dashboard() {
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      setMetrics({
        totalUsers: 142,
        totalProducts: 56,
        totalOrders: 890,
        totalRevenue: 125400.50
      });
      setIsLoading(false);
    }, 1500);

    return () => clearTimeout(timer);
  }, []);

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('es-MX', {
      style: 'currency',
      currency: 'MXN'
    }).format(amount);
  };

  const MetricCard = ({ title, value, isCurrency = false }: { title: string, value?: number, isCurrency?: boolean }) => (
    <div className="bg-bg-secondary p-6 rounded-lg border border-text-primary/10 flex flex-col gap-2">
      <h3 className="text-sm font-medium opacity-80">{title}</h3>
      {isLoading || value === undefined ? (
        <Skeleton className="h-8 w-24" />
      ) : (
        <p className="text-3xl font-bold">
          {isCurrency ? formatCurrency(value) : value}
        </p>
      )}
    </div>
  );

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="text-2xl font-bold">Resumen General</h2>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard title="Usuarios Totales" value={metrics?.totalUsers} />
        <MetricCard title="Productos" value={metrics?.totalProducts} />
        <MetricCard title="Órdenes" value={metrics?.totalOrders} />
        <MetricCard title="Ingresos" value={metrics?.totalRevenue} isCurrency />
      </div>
    </div>
  );
}