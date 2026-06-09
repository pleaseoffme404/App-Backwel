import React, { useState } from 'react';
import { Link } from 'react-router-dom';

interface StoreProduct {
  id: string;
  name: string;
  price: number;
  image: string;
  category: string;
  stock: number;
}

// Datos falsos para no depender del backend roto temporalmente
const mockProducts: StoreProduct[] = [
  { id: '1', name: 'Silla Ergonómica', price: 2500, image: '', category: 'Muebles', stock: 10 },
  { id: '2', name: 'Escritorio L', price: 4200, image: '', category: 'Muebles', stock: 5 },
  { id: '3', name: 'Monitor 27"', price: 6500, image: '', category: 'Tecnología', stock: 2 },
  { id: '4', name: 'Teclado Mecánico', price: 1800, image: '', category: 'Tecnología', stock: 0 },
];

export default function StoreCatalog() {
  const [cart, setCart] = useState<(StoreProduct & { quantity: number })[]>([]);
  const [activeCategory, setActiveCategory] = useState<string>('Todos');

  const categories = ['Todos', ...Array.from(new Set(mockProducts.map(p => p.category)))];
  
  const filteredProducts = activeCategory === 'Todos' 
    ? mockProducts 
    : mockProducts.filter(p => p.category === activeCategory);

  const totalCart = cart.reduce((acc, item) => acc + (item.price * item.quantity), 0);
  const cartItemsCount = cart.reduce((acc, item) => acc + item.quantity, 0);

  const addToCart = (product: StoreProduct) => {
    if (product.stock <= 0) return;
    setCart(prev => {
      const existing = prev.find(p => p.id === product.id);
      if (existing) {
        if (existing.quantity >= product.stock) {
            alert(`Solo hay ${product.stock} unidades en stock.`);
            return prev;
        }
        return prev.map(p => p.id === product.id ? { ...p, quantity: p.quantity + 1 } : p);
      }
      return [...prev, { ...product, quantity: 1 }];
    });
  };

  return (
    <div className="min-h-screen bg-bg-primary text-text-primary font-sans flex flex-col">
      {/* Navbar Minimalista */}
      <nav className="bg-bg-primary border-b border-brand-primary/10 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 h-20 flex items-center justify-between">
          <Link to="/" className="text-2xl font-black text-brand-primary tracking-tight">
            BACKWEL <span className="text-accent">STORE</span>
          </Link>
          <div className="flex items-center gap-6">
            <Link to="/login" className="text-sm font-bold text-text-primary/70 hover:text-brand-primary transition-colors">
              Mi Cuenta
            </Link>
            <button className="relative p-2 text-brand-primary hover:text-accent transition-colors">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" /></svg>
              {cartItemsCount > 0 && (
                <span className="absolute top-0 right-0 bg-accent text-white text-[10px] font-bold w-5 h-5 flex items-center justify-center rounded-full border-2 border-bg-primary">
                  {cartItemsCount}
                </span>
              )}
            </button>
          </div>
        </div>
      </nav>

      <main className="flex-1 max-w-7xl mx-auto w-full px-4 py-8 flex flex-col lg:flex-row gap-8">
        
        {/* Sidebar Filtros */}
        <aside className="w-full lg:w-64 shrink-0 flex flex-col gap-6">
          <div className="bg-bg-secondary p-5 rounded-xl border border-brand-primary/10">
            <h3 className="font-black text-brand-primary mb-4 uppercase tracking-wider text-sm">Categorías</h3>
            <div className="flex flex-col gap-2">
              {categories.map(cat => (
                <button 
                  key={cat} 
                  onClick={() => setActiveCategory(cat)}
                  className={`text-left text-sm font-bold py-2 px-3 rounded-lg transition-colors ${activeCategory === cat ? 'bg-brand-primary text-bg-primary' : 'text-text-primary/70 hover:bg-bg-primary'}`}
                >
                  {cat}
                </button>
              ))}
            </div>
          </div>
        </aside>

        {/* Grid de Productos */}
        <section className="flex-1">
          <div className="mb-6">
            <h1 className="text-3xl font-black text-brand-primary">Catálogo</h1>
            <p className="text-text-primary/60">Mostrando {filteredProducts.length} productos</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
            {filteredProducts.map(product => (
              <div key={product.id} className="bg-bg-secondary border border-brand-primary/10 rounded-xl overflow-hidden group flex flex-col hover:border-brand-primary/30 hover:shadow-lg transition-all duration-300">
                <div className="h-48 bg-bg-primary flex items-center justify-center border-b border-brand-primary/5">
                  {product.image ? (
                    <img src={product.image} alt={product.name} className="w-full h-full object-cover" />
                  ) : (
                    <span className="text-brand-primary/30 text-5xl font-black">{product.name.charAt(0)}</span>
                  )}
                </div>
                <div className="p-5 flex flex-col flex-1 gap-2">
                  <span className="text-xs font-bold text-text-primary/50 uppercase tracking-wider">{product.category}</span>
                  <h3 className="font-bold text-lg text-text-primary leading-tight">{product.name}</h3>
                  <div className="mt-auto pt-4 flex items-center justify-between">
                    <span className="text-xl font-black text-brand-secondary">${product.price.toFixed(2)}</span>
                    <button 
                      onClick={() => addToCart(product)}
                      disabled={product.stock <= 0}
                      className={`px-4 py-2 rounded-lg font-bold text-sm transition-colors ${product.stock <= 0 ? 'bg-bg-primary text-text-primary/40 cursor-not-allowed' : 'bg-brand-primary text-bg-primary hover:opacity-90'}`}
                    >
                      {product.stock <= 0 ? 'Agotado' : 'Añadir'}
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>

      </main>
    </div>
  );
}