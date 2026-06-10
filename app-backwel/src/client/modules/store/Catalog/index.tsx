import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useSession } from '../../../shared/hooks/useSession';
import Swal from 'sweetalert2';

interface StoreProduct {
  id: string;
  productId?: string;
  name: string;
  price: number;
  image: string;
  category: string;
  stock: number;
}

const mockProducts: StoreProduct[] = [
  { id: '1', productId: 'p1', name: 'Silla Ergonómica Pro', price: 2500, image: '', category: 'Muebles', stock: 10 },
  { id: '2', productId: 'p2', name: 'Escritorio Gamer L', price: 4200, image: '', category: 'Muebles', stock: 5 },
  { id: '3', productId: 'p3', name: 'Monitor Ultrawide 27"', price: 6500, image: '', category: 'Tecnología', stock: 2 },
  { id: '4', productId: 'p4', name: 'Teclado Mecánico RGB', price: 1800, image: '', category: 'Tecnología', stock: 0 },
  { id: '5', productId: 'p5', name: 'Mouse Inalámbrico', price: 900, image: '', category: 'Tecnología', stock: 15 },
  { id: '6', productId: 'p6', name: 'Audífonos Noise Cancelling', price: 3200, image: '', category: 'Accesorios', stock: 8 },
];
import { useBusiness } from '../../../shared/hooks/useBusiness';

export default function StoreCatalog() {

  const [products, setProducts] = useState<StoreProduct[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [cart, setCart] = useState<(StoreProduct & { quantity: number })[]>([]);
  const [activeCategory, setActiveCategory] = useState<string>('Todos');
  const { business } = useBusiness();
  const bName = business?.businessName || business?.business_name;
  const bLogo = business?.logoUrl || business?.logo_url;

  const { session } = useSession();
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const fetchCatalog = () => {
      // Simulamos 600ms de retraso de red
      setTimeout(() => {
        setProducts(mockProducts);
        setIsLoading(false);
      }, 600);
    };

    fetchCatalog();
  }, []);

  const categories = ['Todos', ...Array.from(new Set(products.map(p => p.category)))];

  const filteredProducts = activeCategory === 'Todos' 
    ? products 
    : products.filter(p => p.category === activeCategory);

  const cartItemsCount = cart.reduce((acc, item) => acc + item.quantity, 0);

const [isCartOpen, setIsCartOpen] = useState(false);
  const [checkoutStep, setCheckoutStep] = useState<'cart' | 'checkout' | 'success'>('cart');

  const cartTotal = cart.reduce((acc, item) => acc + (item.price * item.quantity), 0);

const addToCart = (product: StoreProduct) => {
    if (!session?.active) {
      navigate('/login', { state: { from: location.pathname } });
      return;
    }

    if (product.stock <= 0) return;
    setCart(prev => {
      const existing = prev.find(p => p.id === product.id);
      if (existing) {
        if (existing.quantity >= product.stock) {
            Swal.fire({
              icon: 'warning',
              title: 'Stock Límite',
              text: `Solo hay ${product.stock} unidades disponibles apra este producto.`,
              confirmButtonColor: '#38BDF8'
            });
            return prev;
        }
        return prev.map(p => p.id === product.id ? { ...p, quantity: p.quantity + 1 } : p);
      }
      return [...prev, { ...product, quantity: 1 }];
    });
    setIsCartOpen(true);
  };

  const removeFromCart = (id: string) => {
    setCart(prev => prev.filter(p => p.id !== id));
  };

  const updateQuantity = (id: string, delta: number) => {
    setCart(prev => prev.map(item => {
      if (item.id === id) {
        const newQ = item.quantity + delta;
        if (newQ <= 0) return item;
        if (newQ > item.stock) {
          Swal.fire({
            icon: 'warning',
            title: 'Stock Límite',
            text: `Solo hay ${item.stock} unidades disponibles en inventario.`,
            confirmButtonColor: '#38BDF8'
          });
          return item;
        }
        return { ...item, quantity: newQ };
      }
      return item;
    }));
  };

  return (
    <div className="min-h-screen bg-bg-primary text-text-primary font-sans flex flex-col">
      <nav className="bg-bg-primary border-b border-brand-primary/10 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 h-20 flex items-center justify-between">
         <Link to="/" className="text-2xl font-black text-brand-primary tracking-tight flex items-center gap-3">
            {bLogo && (
              <img src={bLogo} alt="Logo" className="h-8 w-auto object-contain" />
            )}
            {bName ? (
              <span>
                {bName.split(' ')[0]} <span className="text-accent">{bName.split(' ').slice(1).join(' ')}</span>
              </span>
            ) : (
              <span>BACKWEL <span className="text-accent">STORE</span></span>
            )}
          </Link>
          <div className="flex items-center gap-6">
            <Link to={session?.active ? "/admin/dashboard" : "/login"} className="text-sm font-bold text-text-primary/70 hover:text-brand-primary transition-colors">
              {session?.active ? `Hola, ${session.user?.name || 'Usuario'}` : 'Mi Cuenta'}
            </Link>
            <button onClick={() => setIsCartOpen(true)} className="relative p-2 text-brand-primary hover:text-accent transition-colors">
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
        
        <aside className="w-full lg:w-64 shrink-0 flex flex-col gap-6">
          <div className="bg-bg-secondary p-5 rounded-xl border border-brand-primary/10">
            <h3 className="font-black text-brand-primary mb-4 uppercase tracking-wider text-sm">Categorías</h3>
            {isLoading ? (
              <div className="animate-pulse flex flex-col gap-2">
                <div className="h-8 bg-brand-primary/10 rounded-lg w-full"></div>
                <div className="h-8 bg-brand-primary/10 rounded-lg w-full"></div>
                <div className="h-8 bg-brand-primary/10 rounded-lg w-3/4"></div>
              </div>
            ) : (
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
            )}
          </div>
        </aside>

        <section className="flex-1">
          <div className="mb-6 flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-black text-brand-primary">Catálogo</h1>
              {!isLoading && <p className="text-text-primary/60">Mostrando {filteredProducts.length} productos</p>}
            </div>
          </div>

          {isLoading ? (
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
              {[1, 2, 3, 4, 5, 6].map(n => (
                <div key={n} className="bg-bg-secondary border border-brand-primary/10 rounded-xl overflow-hidden h-[320px] animate-pulse flex flex-col">
                  <div className="h-48 bg-brand-primary/5"></div>
                  <div className="p-5 flex flex-col gap-3">
                    <div className="h-4 bg-brand-primary/10 rounded w-1/3"></div>
                    <div className="h-6 bg-brand-primary/10 rounded w-3/4"></div>
                    <div className="mt-auto h-8 bg-brand-primary/10 rounded w-1/2"></div>
                  </div>
                </div>
              ))}
            </div>
          ) : filteredProducts.length === 0 ? (
            <div className="bg-bg-secondary border border-brand-primary/10 rounded-xl p-12 text-center flex flex-col items-center justify-center">
              <svg className="w-16 h-16 text-brand-primary/30 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" /></svg>
              <h3 className="text-xl font-bold text-text-primary">No hay productos disponibles</h3>
              <p className="text-text-primary/60 mt-2">Aún no se han agregado productos con stock a la tienda.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
              {filteredProducts.map(product => (
                <div key={product.id} className="bg-bg-secondary border border-brand-primary/10 rounded-xl overflow-hidden group flex flex-col hover:border-brand-primary/30 hover:shadow-lg transition-all duration-300">
                  <div className="h-48 bg-bg-primary flex items-center justify-center border-b border-brand-primary/5 overflow-hidden">
                    {product.image ? (
                      <img src={product.image} alt={product.name} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" />
                    ) : (
                      <span className="text-brand-primary/30 text-5xl font-black">{product.name.charAt(0)}</span>
                    )}
                  </div>
                  <div className="p-5 flex flex-col flex-1 gap-2">
                    <span className="text-xs font-bold text-text-primary/50 uppercase tracking-wider">{product.category}</span>
                    <h3 className="font-bold text-lg text-text-primary leading-tight line-clamp-2">{product.name}</h3>
                    <div className="mt-auto pt-4 flex items-center justify-between">
                      <span className="text-xl font-black text-brand-secondary">${product.price.toFixed(2)}</span>
                      <button 
                        onClick={() => addToCart(product)}
                        disabled={product.stock <= 0}
                        className={`px-4 py-2 rounded-lg font-bold text-sm transition-colors ${product.stock <= 0 ? 'bg-bg-primary text-text-primary/40 cursor-not-allowed' : 'bg-brand-primary text-bg-primary hover:opacity-90 active:scale-95'}`}
                      >
                        {product.stock <= 0 ? 'Agotado' : 'Añadir'}
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      </main>

      {isCartOpen && (
        <div className="fixed inset-0 z-50 flex justify-end">
          <div className="absolute inset-0 bg-bg-primary/80 backdrop-blur-sm" onClick={() => setIsCartOpen(false)}></div>
          <div className="relative w-full max-w-md bg-bg-secondary h-full shadow-2xl flex flex-col border-l border-brand-primary/20 transform transition-transform">
            <div className="flex items-center justify-between p-6 border-b border-brand-primary/10">
              <h2 className="text-2xl font-black text-brand-primary">
                {checkoutStep === 'cart' && 'Tu Carrito'}
                {checkoutStep === 'checkout' && 'Checkout'}
                {checkoutStep === 'success' && '¡Orden Confirmada!'}
              </h2>
              <button onClick={() => { setIsCartOpen(false); if(checkoutStep==='success') {setCart([]); setCheckoutStep('cart');} }} className="text-text-primary/50 hover:text-accent">
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-6 flex flex-col gap-4">
              {checkoutStep === 'cart' && cart.length === 0 && (
                <p className="text-text-primary/50 text-center mt-10">Tu carrito está vacío.</p>
              )}
              
              {checkoutStep === 'cart' && cart.map(item => (
                <div key={item.id} className="flex gap-4 bg-bg-primary p-3 rounded-xl border border-brand-primary/10">
                  <div className="w-20 h-20 bg-bg-secondary rounded-lg flex items-center justify-center overflow-hidden">
                    {item.image ? <img src={item.image} alt={item.name} className="w-full h-full object-cover" /> : <span className="font-bold text-brand-primary/30">{item.name.charAt(0)}</span>}
                  </div>
                  <div className="flex-1 flex flex-col justify-between">
                    <div>
                      <h4 className="font-bold text-text-primary leading-tight line-clamp-1">{item.name}</h4>
                      <span className="text-brand-secondary font-black">${item.price.toFixed(2)}</span>
                    </div>
                    <div className="flex items-center justify-between mt-2">
                      <div className="flex items-center bg-bg-secondary rounded-lg border border-brand-primary/10">
                        <button onClick={() => updateQuantity(item.id, -1)} className="px-3 py-1 text-text-primary hover:text-accent font-bold">-</button>
                        <span className="px-2 font-bold text-sm">{item.quantity}</span>
                        <button onClick={() => updateQuantity(item.id, 1)} className="px-3 py-1 text-text-primary hover:text-accent font-bold">+</button>
                      </div>
                      <button onClick={() => removeFromCart(item.id)} className="text-xs text-text-primary/40 hover:text-accent font-bold underline">Eliminar</button>
                    </div>
                  </div>
                </div>
              ))}

              {checkoutStep === 'checkout' && (
                <form id="checkout-form" onSubmit={(e) => { e.preventDefault(); setCheckoutStep('success'); }} className="flex flex-col gap-4">
                  <div className="flex flex-col gap-1">
                    <label className="text-xs font-bold text-text-primary/70 uppercase">Nombre Completo</label>
                    <input type="text" required className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary" />
                  </div>
                  <div className="flex flex-col gap-1">
                    <label className="text-xs font-bold text-text-primary/70 uppercase">Dirección de Envío</label>
                    <input type="text" required className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary" />
                  </div>
                  <div className="flex flex-col gap-1">
                    <label className="text-xs font-bold text-text-primary/70 uppercase">Tarjeta (Simulada)</label>
                    <input type="text" required placeholder="**** **** **** ****" maxLength={16} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary" />
                  </div>
                </form>
              )}

              {checkoutStep === 'success' && (
                <div className="flex flex-col items-center justify-center h-full text-center gap-4">
                  <div className="w-20 h-20 bg-brand-primary/10 text-brand-primary rounded-full flex items-center justify-center">
                    <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" /></svg>
                  </div>
                  <h3 className="text-xl font-bold text-text-primary">¡Pago exitoso!</h3>
                  <p className="text-text-primary/60">Tu orden ha sido registrada (simulación). Cuando el backend esté listo, esto creará la orden en la BD.</p>
                </div>
              )}
            </div>

            {checkoutStep !== 'success' && cart.length > 0 && (
              <div className="p-6 border-t border-brand-primary/10 bg-bg-primary flex flex-col gap-4">
                <div className="flex items-center justify-between font-black text-lg">
                  <span className="text-text-primary">Total:</span>
                  <span className="text-brand-secondary">${cartTotal.toFixed(2)}</span>
                </div>
                {checkoutStep === 'cart' ? (
                  <button onClick={() => setCheckoutStep('checkout')} className="w-full py-4 bg-brand-primary text-bg-primary font-black rounded-xl hover:opacity-90">
                    Proceder al Pago
                  </button>
                ) : (
                  <div className="flex gap-2">
                    <button onClick={() => setCheckoutStep('cart')} className="px-6 py-4 bg-bg-secondary text-text-primary font-bold rounded-xl hover:bg-brand-primary/10 border border-brand-primary/20">
                      Volver
                    </button>
                    <button form="checkout-form" type="submit" className="flex-1 py-4 bg-brand-primary text-bg-primary font-black rounded-xl hover:opacity-90">
                      Confirmar Pago
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}