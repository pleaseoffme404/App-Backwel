import React, { useState, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useSession } from '../../shared/hooks/useSession';
import { Product, CartItem } from './types';
import { ProductCard } from './components/ProductCard';
import { ProductCardSkeleton } from './components/ProductCardSkeleton';

export default function POSModule() {
  const { session } = useSession();
  const [products, setProducts] = useState<Product[]>([]);
  const [cart, setCart] = useState<CartItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isMenuCollapsed, setIsMenuCollapsed] = useState(true);

  useEffect(() => {
    const roles = session?.user?.roles || [];
    if (roles.includes('ADMIN') || roles.includes('OWNER')) {
      localStorage.setItem('backwel_admin_locked', 'true');
    }
  }, [session]);
  useEffect(() => {
    const fetchCatalog = async () => {
      try {
        const response = await fetch('/api/v1/catalog/products');
        if (response.ok) {
          const data = await response.json();
          setProducts(data);
        }
      } catch (error) {
        console.error("Error cargando catálogo mock:", error);
      } finally {
        setIsLoading(false);
      }
    };
    
    fetchCatalog();
  }, []);

  const addToCart = (product: Product) => {
    setCart((prev) => {
      const existing = prev.find((item) => item.id === product.id);
      if (existing) {
        if (existing.quantity >= product.stock) return prev;
        return prev.map((item) =>
          item.id === product.id ? { ...item, quantity: item.quantity + 1 } : item
        );
      }
      return [...prev, { ...product, quantity: 1 }];
    });
  };

  const updateQuantity = (id: string, delta: number) => {
    setCart((prev) => 
      prev.map((item) => {
        if (item.id === id) {
          const newQuantity = item.quantity + delta;
          if (newQuantity <= 0 || newQuantity > item.stock) return item;
          return { ...item, quantity: newQuantity };
        }
        return item;
      })
    );
  };

  const removeFromCart = (id: string) => {
    setCart((prev) => prev.filter((item) => item.id !== id));
  };

  const total = useMemo(() => {
    return cart.reduce((acc, item) => acc + item.price * item.quantity, 0);
  }, [cart]);

 return (
    <div className="flex h-screen w-full bg-bg-primary overflow-hidden">
      
      <aside className={`${isMenuCollapsed ? 'w-20' : 'w-64'} transition-all duration-300 border-r border-brand-primary/20 bg-bg-secondary flex flex-col shrink-0 z-50`}>
        <div className="h-20 border-b border-brand-primary/20 flex items-center justify-center overflow-hidden">
          <button 
            onClick={() => setIsMenuCollapsed(!isMenuCollapsed)}
            className="p-2 rounded-lg hover:bg-bg-primary text-text-primary/70 hover:text-brand-primary transition-colors shrink-0"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
        </div>
        <nav className="flex-1 p-4">
          <Link 
            to="/admin/dashboard" 
            className="p-3 rounded-md transition-all flex items-center gap-3 overflow-hidden whitespace-nowrap hover:bg-bg-primary text-text-primary"
            title="Volver al Admin"
          >
            <svg className="w-6 h-6 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            {!isMenuCollapsed && <span className="font-medium">Volver al Admin</span>}
          </Link>
        </nav>
      </aside>

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
                    onClick={addToCart}
                  />
                ))}
          </div>
        </main>
      </section>

      <aside className="w-[400px] xl:w-[450px] flex flex-col bg-bg-secondary border-l border-brand-primary/20 shrink-0">
        <header className="h-20 px-6 flex items-center border-b border-brand-primary/20">
          <h2 className="text-2xl font-bold text-text-primary tracking-tight">Orden Activa</h2>
        </header>
        
        <div className="flex-1 overflow-y-auto p-4 flex flex-col gap-3">
          {cart.length === 0 ? (
            <div className="flex-1 flex items-center justify-center text-text-primary/50 font-medium">
              El carrito está vacío
            </div>
          ) : (
            cart.map((item) => (
              <div key={item.id} className="bg-bg-primary border border-brand-primary/10 rounded-lg p-3 flex flex-col gap-2">
                <div className="flex justify-between items-start">
                  <span className="font-medium text-text-primary leading-tight pr-4">{item.name}</span>
                  <button onClick={() => removeFromCart(item.id)} className="text-red-500 font-bold hover:bg-red-500/10 px-2 rounded">
                    ✕
                  </button>
                </div>
                <div className="flex justify-between items-center">
                  <span className="font-bold text-brand-secondary">${(item.price * item.quantity).toFixed(2)}</span>
                  <div className="flex items-center gap-3 bg-bg-secondary border border-brand-primary/20 rounded-md px-1">
                    <button 
                      onClick={() => updateQuantity(item.id, -1)}
                      className="w-8 h-8 flex items-center justify-center font-black text-lg text-text-primary hover:text-brand-primary"
                    >-</button>
                    <span className="font-bold w-4 text-center">{item.quantity}</span>
                    <button 
                      onClick={() => updateQuantity(item.id, 1)}
                      disabled={item.quantity >= item.stock}
                      className="w-8 h-8 flex items-center justify-center font-black text-lg text-text-primary hover:text-brand-primary disabled:opacity-30"
                    >+</button>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>

        <footer className="p-6 border-t border-brand-primary/20 bg-bg-secondary">
          <div className="flex justify-between items-center mb-6">
            <span className="text-xl font-medium text-text-primary/70">Total</span>
            <span className="text-4xl font-black text-text-primary">${total.toFixed(2)}</span>
          </div>
          <button 
            disabled={cart.length === 0}
            className="w-full py-5 rounded-xl bg-accent text-white text-xl font-bold uppercase tracking-wider active:scale-[0.98] transition-transform shadow-lg disabled:opacity-50 disabled:active:scale-100"
          >
            Cobrar
          </button>
        </footer>
      </aside>
    </div>
  );
}