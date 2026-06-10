import React, { useState, useEffect } from 'react';

export default function ConfiguracionAdmin() {
  const [config, setConfig] = useState({
    businessName: '',
    logoUrl: '',
    contactEmail: '',
    contactPhone: '',
    taxId: '',
    address: '',
    currency: 'MXN'
  });
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [status, setStatus] = useState<{ type: 'success' | 'error', msg: string } | null>(null);

  useEffect(() => {
    // Hidratar la configuración desde la BD de Node
    const fetchConfig = async () => {
      try {
        const res = await fetch('/api/config/business');
        if (res.ok) {
          const data = await res.json();
          if (Object.keys(data).length > 0) {
            setConfig(prev => ({ ...prev, ...data }));
          }
        }
      } catch (error) {
        console.error("Error al cargar la configuración:", error);
      } finally {
        setIsLoading(false);
      }
    };
    fetchConfig();
  }, []);

  const handleChange = (field: string, value: string) => {
    setConfig(prev => ({ ...prev, [field]: value }));
    setStatus(null); // Limpiar mensaje al editar
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    setStatus(null);

    try {
      const res = await fetch('/api/config/page', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ section: 'business', config })
      });

      if (res.ok) {
        setStatus({ type: 'success', msg: 'Identidad del negocio actualizada exitosamente.' });
      } else {
        setStatus({ type: 'error', msg: 'Error al guardar la configuración en la base de datos.' });
      }
    } catch (error) {
      setStatus({ type: 'error', msg: 'Error de red al intentar guardar los cambios.' });
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return <div className="p-8 text-text-primary/50 font-bold animate-pulse">Cargando configuración...</div>;
  }

  return (
    <div className="w-full max-w-5xl mx-auto flex flex-col gap-6 pb-12">
      <div className="flex flex-col gap-2 border-b border-brand-primary/10 pb-6">
        <h1 className="text-3xl font-black text-brand-primary tracking-tight">Identidad del Negocio</h1>
        <p className="text-text-primary/70">
          Configura la información global de tu tienda. Estos datos aparecerán en tu página pública, correos y tickets de compra.
        </p>
      </div>

      {status && (
        <div className={`p-4 rounded-xl border font-bold ${status.type === 'success' ? 'bg-brand-primary/10 border-brand-primary text-brand-primary' : 'bg-red-500/10 border-red-500 text-red-500'}`}>
          {status.msg}
        </div>
      )}

      <form onSubmit={handleSave} className="flex flex-col gap-8">
        {/* Sección: Marca */}
        <section className="bg-bg-secondary p-8 rounded-xl border border-brand-primary/20 shadow-sm flex flex-col gap-6">
          <h2 className="text-xl font-bold text-text-primary flex items-center gap-2 border-b border-brand-primary/10 pb-4">
            <span className="w-6 h-px bg-brand-primary/30"></span> Marca y Visuales
          </h2>
          
          <div className="flex gap-8 items-start">
            <div className="flex-1 flex flex-col gap-6">
              <div className="flex flex-col gap-1">
                <label className="text-xs font-bold text-text-primary/70 uppercase tracking-wide">Nombre de la Empresa</label>
                <input 
                  type="text" 
                  value={config.businessName} 
                  onChange={e => handleChange('businessName', e.target.value)} 
                  className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary transition-colors" 
                  placeholder="Ej: Backwel Solutions S.A. de C.V." 
                />
              </div>
              <div className="flex flex-col gap-1">
                <label className="text-xs font-bold text-text-primary/70 uppercase tracking-wide">URL del Logotipo</label>
                <input 
                  type="url" 
                  value={config.logoUrl} 
                  onChange={e => handleChange('logoUrl', e.target.value)} 
                  className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary transition-colors" 
                  placeholder="https://tudominio.com/logo.png" 
                />
              </div>
            </div>

            {/* Preview del Logo */}
            <div className="w-40 h-40 bg-bg-primary rounded-xl border border-brand-primary/20 border-dashed flex flex-col items-center justify-center shrink-0 overflow-hidden relative group">
              <span className="text-xs font-bold text-text-primary/40 uppercase absolute top-2">Preview</span>
              {config.logoUrl ? (
                <img src={config.logoUrl} alt="Logo" className="max-w-[80%] max-h-[80%] object-contain mt-4" />
              ) : (
                <svg className="w-12 h-12 text-brand-primary/20 mt-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
              )}
            </div>
          </div>
        </section>

        {/* Sección: Contacto y Ubicación */}
        <section className="bg-bg-secondary p-8 rounded-xl border border-brand-primary/20 shadow-sm flex flex-col gap-6">
          <h2 className="text-xl font-bold text-text-primary flex items-center gap-2 border-b border-brand-primary/10 pb-4">
            <span className="w-6 h-px bg-brand-primary/30"></span> Contacto y Fiscal
          </h2>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="flex flex-col gap-1">
              <label className="text-xs font-bold text-text-primary/70 uppercase tracking-wide">Correo Público (Soporte)</label>
              <input 
                type="email" 
                value={config.contactEmail} 
                onChange={e => handleChange('contactEmail', e.target.value)} 
                className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary transition-colors" 
                placeholder="soporte@tuempresa.com" 
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs font-bold text-text-primary/70 uppercase tracking-wide">Teléfono de Atención</label>
              <input 
                type="tel" 
                value={config.contactPhone} 
                onChange={e => handleChange('contactPhone', e.target.value)} 
                className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary transition-colors" 
                placeholder="+52 55 1234 5678" 
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs font-bold text-text-primary/70 uppercase tracking-wide">RFC / ID Fiscal</label>
              <input 
                type="text" 
                value={config.taxId} 
                onChange={e => handleChange('taxId', e.target.value)} 
                className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary transition-colors uppercase font-mono" 
                placeholder="XAXX010101000" 
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs font-bold text-text-primary/70 uppercase tracking-wide">Moneda Local</label>
              <select 
                value={config.currency} 
                onChange={e => handleChange('currency', e.target.value)} 
                className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary transition-colors cursor-pointer"
              >
                <option value="MXN">Peso Mexicano (MXN)</option>
                <option value="USD">Dólar Estadounidense (USD)</option>
                <option value="EUR">Euro (EUR)</option>
              </select>
            </div>
            <div className="flex flex-col gap-1 md:col-span-2">
              <label className="text-xs font-bold text-text-primary/70 uppercase tracking-wide">Dirección Física o Fiscal</label>
              <textarea 
                value={config.address} 
                onChange={e => handleChange('address', e.target.value)} 
                className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary transition-colors resize-y min-h-[80px]" 
                placeholder="Calle Principal #123, Colonia Centro..." 
              />
            </div>
          </div>
        </section>

        <button 
          type="submit" 
          disabled={isSaving}
          className="w-full py-5 bg-brand-primary text-bg-primary font-black text-lg rounded-xl hover:opacity-90 transition-all disabled:opacity-50 shadow-xl shadow-brand-primary/20 uppercase tracking-widest active:scale-[0.98]"
        >
          {isSaving ? 'Guardando...' : 'Guardar Identidad del Negocio'}
        </button>
      </form>
    </div>
  );
}