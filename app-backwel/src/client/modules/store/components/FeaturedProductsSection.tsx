import React from 'react';
import { Skeleton } from '../../../shared/ui/Skeleton';
import { PageConfigSection } from '../../../shared/hooks/usePageConfig';

interface FeaturedProductsSectionProps {
  config?: PageConfigSection;
  isLoading: boolean;
}

export function FeaturedProductsSection({ config, isLoading }: FeaturedProductsSectionProps) {
  if (isLoading) {
    return (
      <section className="w-full py-24 bg-bg-primary px-6">
        <div className="max-w-6xl mx-auto flex flex-col items-center gap-12">
          <div className="text-center space-y-4 w-full flex flex-col items-center">
            <Skeleton className="w-1/3 h-10 rounded-lg" />
            <Skeleton className="w-1/2 h-6 rounded" />
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8 w-full">
            {[1, 2, 3].map((i) => (
              <div key={i} className="flex flex-col gap-4 bg-bg-secondary p-4 rounded-2xl border border-brand-primary/10">
                <Skeleton className="w-full aspect-square rounded-xl" />
                <Skeleton className="w-3/4 h-6" />
                <Skeleton className="w-full h-4" />
                <Skeleton className="w-1/4 h-6 mt-2" />
              </div>
            ))}
          </div>
        </div>
      </section>
    );
  }

  if (config?.visible === false) return null;

  const title = config?.title || 'Productos Destacados';
  const subtitle = config?.subtitle || 'Nuestra selección de artículos top para ti';

  const placeholderProducts = [
    { id: '1', name: 'Auriculares Inalámbricos Pro', description: 'Sonido de alta fidelidad con cancelación de ruido activa.', price: 2499.00 },
    { id: '2', name: 'Teclado Mecánico RGB 60%', description: 'Switches rojos ultra rápidos ideales para gaming.', price: 1299.50 },
    { id: '3', name: 'Monitor Ultrawide 34"', description: 'Resolución 4K y 144Hz para la mejor experiencia.', price: 8500.00 },
  ];

  return (
    <section className="w-full py-24 bg-bg-primary px-6">
      <div className="max-w-6xl mx-auto flex flex-col items-center gap-16">
        <div className="text-center space-y-4">
          <h2 className="text-4xl md:text-5xl font-black text-text-primary tracking-tight">
            {title}
          </h2>
          {subtitle && (
            <p className="text-xl text-text-primary/70 font-medium">
              {subtitle}
            </p>
          )}
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-8 w-full">
          {placeholderProducts.map((product) => (
            <div 
              key={product.id} 
              className="group flex flex-col bg-bg-secondary rounded-2xl border border-brand-primary/10 overflow-hidden hover:shadow-2xl hover:shadow-brand-primary/20 transition-all duration-300 hover:-translate-y-2"
            >
              <div className="w-full aspect-square bg-bg-primary relative overflow-hidden flex items-center justify-center">
                <div className="w-full h-full flex items-center justify-center text-6xl font-black text-brand-primary/10 bg-brand-primary/5 group-hover:scale-110 transition-transform duration-500">
                  {product.name.charAt(0)}
                </div>
              </div>
              <div className="p-6 flex flex-col flex-1 gap-2">
                <h3 className="text-xl font-bold text-text-primary line-clamp-1">{product.name}</h3>
                <p className="text-sm text-text-primary/70 line-clamp-2 flex-1">{product.description}</p>
                <div className="mt-4 flex items-center justify-between">
                  <span className="text-2xl font-black text-brand-secondary">
                    ${product.price.toFixed(2)}
                  </span>
                  <button className="bg-brand-primary/10 text-brand-primary hover:bg-brand-primary hover:text-bg-primary px-4 py-2 rounded-lg font-bold text-sm transition-colors">
                    Ver más
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}