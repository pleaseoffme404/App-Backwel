import React, { useState, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useSession } from '../../shared/hooks/useSession';
import { useBusiness } from '../../shared/hooks/useBusiness';
import { Product, CartItem } from './types';
import { ProductCard } from './components/ProductCard';
import { ProductCardSkeleton } from './components/ProductCardSkeleton';

export default function POSModule() {
  const { session } = useSession();
  const { business } = useBusiness();
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

  const subtotal = useMemo(() => {
    return cart.reduce((acc, item) => acc + item.price * item.quantity, 0);
  }, [cart]);

  const [isCheckoutOpen, setIsCheckoutOpen] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState<'Efectivo' | 'Tarjeta' | 'Transferencia'>('Efectivo');
  const [amountReceived, setAmountReceived] = useState<string>('');
  const [discount, setDiscount] = useState<string>('');
  const [isSuccess, setIsSuccess] = useState(false);
  const [ticketNumber, setTicketNumber] = useState<string>('');

  const discountValue = Number(discount) || 0;
  const total = Math.max(0, subtotal - discountValue);
  const changeAmount = Math.max(0, Number(amountReceived) - total);
  const isPaymentValid = paymentMethod !== 'Efectivo' || Number(amountReceived) >= total;

  const handleProcessPayment = () => {
    setTicketNumber(Math.floor(Math.random() * 100000).toString().padStart(5, '0'));
    setIsSuccess(true);
  };

  const handleNewSale = () => {
    setCart([]);
    setIsSuccess(false);
    setIsCheckoutOpen(false);
    setAmountReceived('');
    setDiscount('');
    setPaymentMethod('Efectivo');
  };

  const handlePrintTicket = () => {
    const printWindow = window.open('', '_blank', 'width=400,height=600');
    if (!printWindow) return;

    const date = new Date().toLocaleString('es-MX', { dateStyle: 'long', timeStyle: 'short' });
    const itemsHtml = cart.map(item => `
      <div style="display: flex; justify-content: space-between; margin-bottom: 4px;">
        <span style="flex-basis: 70%;">${item.name} (x${item.quantity})</span>
        <span>$${(item.price * item.quantity).toFixed(2)}</span>
      </div>
    `).join('');

    printWindow.document.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>Ticket #${ticketNumber}</title>
          <style>
            body { font-family: 'Courier New', Courier, monospace; font-size: 14px; color: #000; padding: 20px; max-width: 300px; margin: 0 auto; }
            h1, h2, h3, p { margin: 0; text-align: center; }
            .divider { border-bottom: 1px dashed #000; margin: 15px 0; }
            .flex { display: flex; justify-content: space-between; margin-bottom: 5px; }
            .bold { font-weight: bold; }
            .text-sm { font-size: 12px; }
          </style>
        </head>
        <body>
          <h2>${business?.business_name || 'BACKWEL POS'}</h2>
          <p class="text-sm">Ticket #${ticketNumber}</p>
          <p class="text-sm">${date}</p>
          
          <div class="divider"></div>
          ${itemsHtml}
          <div class="divider"></div>
          
          <div class="flex"><span>Subtotal:</span><span>$${subtotal.toFixed(2)}</span></div>
          ${discountValue > 0 ? `<div class="flex"><span>Descuento:</span><span>-$${discountValue.toFixed(2)}</span></div>` : ''}
          <div class="flex bold" style="font-size: 16px;"><span>Total:</span><span>$${total.toFixed(2)}</span></div>
          
          <div class="divider"></div>
          <div class="flex"><span>Método:</span><span>${paymentMethod}</span></div>
          ${paymentMethod === 'Efectivo' ? `
            <div class="flex"><span>Recibido:</span><span>$${Number(amountReceived).toFixed(2)}</span></div>
            <div class="flex"><span>Cambio:</span><span>$${changeAmount.toFixed(2)}</span></div>
          ` : ''}
          
          <div class="divider"></div>
          <p class="text-sm bold">Soporte y Facturación</p>
          <p class="text-sm">${business?.support_email || 'soporte@empresa.com'}</p>
          <p class="text-sm">${business?.phone || 'Sin teléfono registrado'}</p>
          <p class="text-sm" style="margin-top: 15px;">¡Gracias por su compra!</p>
          
          <script>
            window.onload = () => { window.print(); window.close(); }
          </script>
        </body>
      </html>
    `);
    printWindow.document.close();
  };

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
            <span className="text-xl font-medium text-text-primary/70">Subtotal</span>
            <span className="text-4xl font-black text-text-primary">${subtotal.toFixed(2)}</span>
          </div>
          <button 
            disabled={cart.length === 0}
            onClick={() => setIsCheckoutOpen(true)}
            className="w-full py-5 rounded-xl bg-accent text-white text-xl font-bold uppercase tracking-wider active:scale-[0.98] transition-transform shadow-lg disabled:opacity-50 disabled:active:scale-100"
          >
            Cobrar
          </button>
        </footer>
      </aside>

      {isCheckoutOpen && (
        <div className="fixed inset-0 bg-bg-primary/80 backdrop-blur-sm z-[100] flex items-center justify-center p-4">
          <div className="bg-bg-secondary border border-brand-primary/30 rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden flex flex-col">
            {!isSuccess ? (
              <>
                <div className="p-6 border-b border-brand-primary/20 flex justify-between items-center bg-bg-secondary">
                  <h2 className="text-2xl font-black text-text-primary">Procesar Pago</h2>
                  <button onClick={() => setIsCheckoutOpen(false)} className="text-text-primary/50 hover:text-red-500 font-bold text-xl transition-colors">✕</button>
                </div>
                
                <div className="p-6 flex flex-col gap-6">
                  <div className="bg-bg-primary border border-brand-primary/10 rounded-xl p-6 flex flex-col items-center justify-center gap-2 shadow-inner">
                    <span className="text-sm font-bold text-text-primary/60 uppercase tracking-widest">Total a Cobrar</span>
                    <span className="text-5xl font-black text-brand-secondary">${total.toFixed(2)}</span>
                  </div>

                  <div className="flex flex-col gap-4">
                    <div className="flex flex-col gap-2">
                      <span className="text-sm font-bold text-text-primary/70 uppercase tracking-wider">Descuento Fijo (Opcional)</span>
                      <div className="relative">
                        <span className="absolute left-4 top-1/2 -translate-y-1/2 text-lg font-bold text-text-primary/50">$</span>
                        <input 
                          type="number" 
                          value={discount}
                          onChange={(e) => setDiscount(e.target.value)}
                          className="w-full bg-bg-secondary border border-brand-primary/30 p-3 pl-10 rounded-lg text-lg font-bold text-text-primary focus:border-brand-primary focus:ring-1 focus:ring-brand-primary focus:outline-none"
                          placeholder="0.00"
                        />
                      </div>
                    </div>

                    <div className="flex flex-col gap-2 mt-2">
                      <span className="text-sm font-bold text-text-primary/70 uppercase tracking-wider">Método de Pago</span>
                      <div className="grid grid-cols-3 gap-3">
                        {['Efectivo', 'Tarjeta', 'Transferencia'].map(method => (
                          <button
                            key={method}
                            onClick={() => setPaymentMethod(method as any)}
                            className={`py-3 rounded-lg font-bold text-sm transition-all border ${paymentMethod === method ? 'bg-brand-primary text-bg-primary border-brand-primary scale-105 shadow-lg' : 'bg-bg-primary text-text-primary/70 border-brand-primary/20 hover:border-brand-primary/50'}`}
                          >
                            {method}
                          </button>
                        ))}
                      </div>
                    </div>
                  </div>

                  {paymentMethod === 'Efectivo' && (
                    <div className="flex flex-col gap-4 bg-bg-primary p-4 rounded-xl border border-brand-primary/10">
                      <div className="flex flex-col gap-2">
                        <label className="text-sm font-bold text-text-primary/70 uppercase tracking-wider">Monto Recibido</label>
                        <div className="relative">
                          <span className="absolute left-4 top-1/2 -translate-y-1/2 text-xl font-bold text-text-primary/50">$</span>
                          <input 
                            type="number" 
                            value={amountReceived}
                            onChange={(e) => setAmountReceived(e.target.value)}
                            className="w-full bg-bg-secondary border border-brand-primary/30 p-4 pl-10 rounded-lg text-2xl font-black text-text-primary focus:border-brand-primary focus:ring-1 focus:ring-brand-primary focus:outline-none"
                            placeholder="0.00"
                            autoFocus
                          />
                        </div>
                      </div>
                      <div className="flex justify-between items-center pt-2 border-t border-brand-primary/10">
                        <span className="font-bold text-text-primary/70">Cambio:</span>
                        <span className={`text-2xl font-black ${changeAmount > 0 ? 'text-green-500' : 'text-text-primary'}`}>
                          ${changeAmount.toFixed(2)}
                        </span>
                      </div>
                    </div>
                  )}
                </div>

                <div className="p-6 border-t border-brand-primary/20 bg-bg-secondary flex gap-4">
                  <button 
                    onClick={() => setIsCheckoutOpen(false)}
                    className="flex-1 py-4 font-bold rounded-xl border border-brand-primary/20 text-text-primary hover:bg-bg-primary transition-colors"
                  >
                    Cancelar
                  </button>
                  <button 
                    disabled={!isPaymentValid}
                    onClick={handleProcessPayment}
                    className="flex-1 py-4 font-black rounded-xl bg-accent text-white uppercase tracking-wider hover:opacity-90 transition-opacity disabled:opacity-50 shadow-lg"
                  >
                    Confirmar
                  </button>
                </div>
              </>
            ) : (
              <>
                <div className="p-8 flex flex-col items-center gap-6 bg-bg-secondary text-center">
                  <div className="w-20 h-20 bg-green-500/20 text-green-500 rounded-full flex items-center justify-center border-4 border-green-500/30">
                    <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" /></svg>
                  </div>
                  <div className="flex flex-col gap-1">
                    <h2 className="text-3xl font-black text-text-primary">¡Pago Exitoso!</h2>
                    <p className="opacity-70 font-medium text-lg">Ticket #{ticketNumber}</p>
                  </div>
                  
                  <div className="w-full bg-[#f8fafc] text-[#0f172a] rounded-xl p-6 shadow-2xl flex flex-col gap-3 font-mono text-sm relative overflow-hidden">
                    <div className="absolute top-0 left-0 w-full h-2 bg-[radial-gradient(circle,transparent_4px,#f8fafc_4px)] bg-[length:16px_16px] -translate-y-1/2 drop-shadow-sm"></div>
                    
                    <div className="flex justify-between border-b border-[#0f172a]/10 pb-2">
                      <span className="opacity-60">Método</span>
                      <span className="font-bold">{paymentMethod}</span>
                    </div>
                    <div className="flex justify-between border-b border-[#0f172a]/10 pb-2">
                      <span className="opacity-60">Artículos</span>
                      <span className="font-bold">{cart.reduce((a, b) => a + b.quantity, 0)}</span>
                    </div>
                    <div className="flex justify-between border-b border-[#0f172a]/10 pb-2">
                      <span className="opacity-60">Subtotal</span>
                      <span className="font-bold">${subtotal.toFixed(2)}</span>
                    </div>
                    {discountValue > 0 && (
                      <div className="flex justify-between border-b border-[#0f172a]/10 pb-2">
                        <span className="opacity-60">Descuento</span>
                        <span className="font-bold text-red-600">-${discountValue.toFixed(2)}</span>
                      </div>
                    )}
                    <div className="flex justify-between border-b border-[#0f172a]/10 pb-2">
                      <span className="opacity-60 font-bold">Total</span>
                      <span className="font-black text-lg">${total.toFixed(2)}</span>
                    </div>
                    {paymentMethod === 'Efectivo' && (
                      <>
                        <div className="flex justify-between border-b border-[#0f172a]/10 pb-2 mt-2">
                          <span className="opacity-60">Recibido</span>
                          <span className="font-bold">${Number(amountReceived).toFixed(2)}</span>
                        </div>
                        <div className="flex justify-between mt-2">
                          <span className="opacity-60">Cambio</span>
                          <span className="font-black text-[#16a34a]">${changeAmount.toFixed(2)}</span>
                        </div>
                      </>
                    )}
                    
                    <div className="absolute bottom-0 left-0 w-full h-2 bg-[radial-gradient(circle,transparent_4px,#f8fafc_4px)] bg-[length:16px_16px] translate-y-1/2 rotate-180 drop-shadow-sm"></div>
                  </div>

                  <div className="w-full flex gap-4 mt-2">
                    <button 
                      onClick={handlePrintTicket}
                      className="flex-1 py-4 font-bold rounded-xl border border-brand-primary/20 text-text-primary hover:bg-bg-primary transition-colors flex items-center justify-center gap-2"
                    >
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 17h2a2 2 0 002-2v-4a2 2 0 00-2-2H5a2 2 0 00-2 2v4a2 2 0 002 2h2m2 4h6a2 2 0 002-2v-4a2 2 0 00-2-2H9a2 2 0 00-2 2v4a2 2 0 002 2zm8-12V5a2 2 0 00-2-2H9a2 2 0 00-2 2v4h10z" /></svg>
                      Imprimir Ticket
                    </button>
                    <button 
                      onClick={handleNewSale}
                      className="flex-1 py-4 font-black rounded-xl bg-brand-primary text-bg-primary uppercase tracking-wider hover:bg-brand-secondary transition-colors shadow-lg"
                    >
                      Nueva Venta
                    </button>
                  </div>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}