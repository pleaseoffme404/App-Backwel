import React, { useState, useEffect } from 'react';
import { Product } from './types';
import { ProductCard } from './components/ProductCard';
import { ProductCardSkeleton } from './components/ProductCardSkeleton';

export default function POSModule() {
  const [products, setProducts] = useState<Product[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      setProducts([
        { id: 'p1', name: 'Cable UTP Cat 6', price: 15.00, stock: 150, image: '' },
        { id: 'p2', name: 'Switch Gigabit 8 Puertos', price: 450.00, stock: 0, image: '' },
        { id: 'p3', name: 'Router WiFi 6', price: 1200.00, stock: 12, image: '' },
        { id: 'p4', name: 'Patch Panel 24 Puertos', price: 850.00, stock: 5, image: '' },
        { id: 'p5', name: 'Gabinete Rack 12U', price: 3200.00, stock: 2, image: '' },
      ]);
      setIsLoading(false);
    }, 1500);
    return () => clearTimeout(timer);
  }, []);

  return (
    <div className="flex h-screen w-full bg-bg-primary overflow-hidden">
      <section className="flex-1 flex flex-col min-w-0">
        <header className="h-20 px-6 flex items-center justify-between border-b border-brand-primary/20 bg-bg-secondary">
          <h1 className="text-2xl font-bold text-text-primary tracking-tight">Catálogo</h1>
          <div className="flex gap-4">
            <input
              type="text"
              placeholder="Buscar productos..."
              className="w-72 px-4 py-3 rounded-lg bg-bg-primary border border-brand-primary/30 focus:outline-none focus:border-brand-primary focus:ring-1 focus:ring-brand-primary text-text-primary placeholder:text-text-primary/50 text-lg"
            />
          </div>
        </header>
        
        <main className="flex-1 overflow-y-auto p-6">
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
            {isLoading
              ? Array.from({ length: 10 }).map((_, i) => (
                  <ProductCardSkeleton key={`skel-${i}`} />
                ))
              : products.map((product) => (
                  <ProductCard
                    key={product.id}
                    product={product}
                    onClick={(p) => console.log('Añadir al carrito:', p.id)}
                  />
                ))}
          </div>
        </main>
      </section>
      <aside className="w-[400px] xl:w-[450px] flex flex-col bg-bg-secondary border-l border-brand-primary/20 shrink-0">
        <header className="h-20 px-6 flex items-center border-b border-brand-primary/20">
          <h2 className="text-2xl font-bold text-text-primary tracking-tight">Orden Activa</h2>
        </header>
        
        <div className="flex-1 overflow-y-auto p-4 flex flex-col gap-2">
          
        </div>

        <footer className="p-6 border-t border-brand-primary/20 bg-bg-secondary">
          <div className="flex justify-between items-center mb-6">
            <span className="text-xl font-medium text-text-primary/70">Total</span>
            <span className="text-4xl font-black text-text-primary">$0.00</span>
          </div>
          <button className="w-full py-5 rounded-xl bg-accent text-white text-xl font-bold uppercase tracking-wider active:scale-[0.98] transition-transform shadow-lg">
            Cobrar
          </button>
        </footer>
      </aside>
    </div>
  );
}