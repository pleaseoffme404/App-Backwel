import React, { useEffect } from 'react';
import { useSession } from '../../../shared/hooks/useSession';
import { usePageConfig } from '../../../shared/hooks/usePageConfig';
import { HeroSection } from '../../store/components/HeroSection';
import { AboutSection } from '../../store/components/AboutSection';
import { FeaturedProductsSection } from '../../store/components/FeaturedProductsSection';
import { CatalogLinkSection } from '../../store/components/CatalogLinkSection';

export default function LandingBuilder() {
  const { session, isLoading: isSessionLoading } = useSession();
  const { 
    publishedConfig, 
    draftConfig, 
    isLoading: isConfigLoading,
    updateDraft,
    undoLastChange,
    clearAllDrafts,
    publishChanges
  } = usePageConfig();

  useEffect(() => {
    if (!isSessionLoading && !session?.active) {
      window.location.href = 'http://localhost:8080/login';
    }
  }, [session, isSessionLoading]);

  if (isSessionLoading || isConfigLoading) {
    return (
      <div className="flex h-screen items-center justify-center bg-bg-primary text-text-primary text-xl font-bold">
        Cargando constructor...
      </div>
    );
  }

  const activeConfig = draftConfig || publishedConfig || {};
  const hasChanges = !!draftConfig;

  const handlePublish = async () => {
    const success = await publishChanges();
    if (success) {
      alert('Cambios publicados exitosamente');
    } else {
      alert('Error al publicar los cambios');
    }
  };

  const renderInput = (section: string, field: string, label: string, type = 'text') => {
    const value = activeConfig[section]?.[field] ?? (type === 'checkbox' ? false : '');
    
    return (
      <div className="flex flex-col gap-2 mb-5">
        <label className="text-xs font-black text-text-primary/70 uppercase tracking-wider">
          {label}
        </label>
        {type === 'textarea' ? (
          <textarea 
            className="p-3 rounded-xl bg-bg-primary border border-brand-primary/20 text-text-primary text-sm focus:border-brand-primary focus:ring-1 focus:ring-brand-primary focus:outline-none transition-all resize-y min-h-[100px]"
            value={value}
            onChange={(e) => updateDraft(section, { [field]: e.target.value })}
          />
        ) : type === 'checkbox' ? (
          <label className="relative inline-flex items-center cursor-pointer">
            <input 
              type="checkbox" 
              className="sr-only peer"
              checked={value}
              onChange={(e) => updateDraft(section, { [field]: e.target.checked })}
            />
            <div className="w-11 h-6 bg-bg-primary peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-brand-primary rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-brand-primary/20 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-brand-primary"></div>
            <span className="ml-3 text-sm font-medium text-text-primary">Activar sección</span>
          </label>
        ) : (
          <input 
            type={type}
            className="p-3 rounded-xl bg-bg-primary border border-brand-primary/20 text-text-primary text-sm focus:border-brand-primary focus:ring-1 focus:ring-brand-primary focus:outline-none transition-all"
            value={value}
            onChange={(e) => updateDraft(section, { [field]: e.target.value })}
          />
        )}
      </div>
    );
  };

  return (
    <div className="flex h-screen w-full bg-bg-primary overflow-hidden font-sans">
      <aside className="w-[450px] bg-bg-secondary border-r border-brand-primary/20 flex flex-col h-full shrink-0 shadow-2xl relative z-20">
        <div className="p-6 border-b border-brand-primary/20 bg-bg-secondary flex flex-col gap-4 shadow-sm">
          <div className="flex justify-between items-center">
            <h1 className="text-2xl font-black text-text-primary tracking-tight">Landing Builder</h1>
            {hasChanges && <span className="w-3 h-3 bg-accent rounded-full animate-pulse" title="Cambios sin guardar"></span>}
          </div>
          
          <div className="flex gap-3 text-sm font-bold">
            <button 
              onClick={undoLastChange} 
              disabled={!hasChanges}
              className="px-4 py-2.5 bg-bg-primary border border-brand-primary/20 rounded-lg hover:border-brand-primary/50 disabled:opacity-50 flex-1 transition-all active:scale-95 text-text-primary"
            >
              Deshacer
            </button>
            <button 
              onClick={clearAllDrafts} 
              disabled={!hasChanges}
              className="px-4 py-2.5 bg-red-500/10 text-red-500 rounded-lg hover:bg-red-500/20 disabled:opacity-50 flex-1 transition-all active:scale-95"
            >
              Limpiar
            </button>
          </div>
          
          <button 
            onClick={handlePublish} 
            disabled={!hasChanges}
            className="w-full py-4 mt-2 bg-accent text-white rounded-xl font-black uppercase tracking-widest hover:bg-accent/90 disabled:opacity-50 disabled:cursor-not-allowed transition-all active:scale-[0.98] shadow-lg"
          >
            Publicar Cambios
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-6 flex flex-col gap-10">
          <section>
            <h2 className="text-xl font-black text-brand-primary mb-6 flex items-center gap-2">
              <span className="w-8 h-px bg-brand-primary/30"></span> Hero Section
            </h2>
            {renderInput('hero', 'visible', 'Visibilidad', 'checkbox')}
            {renderInput('hero', 'title', 'Título Principal')}
            {renderInput('hero', 'subtitle', 'Subtítulo', 'textarea')}
            {renderInput('hero', 'cta_text', 'Texto Botón Acción')}
            {renderInput('hero', 'bg_image_url', 'URL Imagen de Fondo')}
          </section>

          <section>
            <h2 className="text-xl font-black text-brand-primary mb-6 flex items-center gap-2">
              <span className="w-8 h-px bg-brand-primary/30"></span> Sección Nosotros
            </h2>
            {renderInput('about', 'visible', 'Visibilidad', 'checkbox')}
            {renderInput('about', 'title', 'Título')}
            {renderInput('about', 'description', 'Descripción Completa', 'textarea')}
            {renderInput('about', 'image_url', 'URL Imagen Lateral')}
          </section>

          <section>
            <h2 className="text-xl font-black text-brand-primary mb-6 flex items-center gap-2">
              <span className="w-8 h-px bg-brand-primary/30"></span> Productos Destacados
            </h2>
            {renderInput('featured_products', 'visible', 'Visibilidad', 'checkbox')}
            {renderInput('featured_products', 'title', 'Título de Bloque')}
            {renderInput('featured_products', 'subtitle', 'Subtítulo Corto')}
          </section>

          <section>
            <h2 className="text-xl font-black text-brand-primary mb-6 flex items-center gap-2">
              <span className="w-8 h-px bg-brand-primary/30"></span> Banner Catálogo
            </h2>
            {renderInput('catalog_link', 'visible', 'Visibilidad', 'checkbox')}
            {renderInput('catalog_link', 'banner_text', 'Llamada a la Acción')}
            {renderInput('catalog_link', 'button_text', 'Texto del Botón')}
            {renderInput('catalog_link', 'banner_image_url', 'URL Imagen de Fondo')}
          </section>
        </div>
      </aside>

      <main className="flex-1 relative bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAiIGhlaWdodD0iMjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iMSIgY3k9IjEiIHI9IjEiIGZpbGw9InJnYmEoMCwwLDAsMC4wNSkiLz48L3N2Zz4=')]">
        <div className="absolute top-6 left-1/2 -translate-x-1/2 bg-bg-secondary px-6 py-2 rounded-full border border-brand-primary/20 shadow-lg z-30 font-mono text-sm font-bold text-brand-secondary">
          PREVIEW EN VIVO (75%)
        </div>
        
        <div className="w-full h-full overflow-hidden flex items-center justify-center p-12">
          <div className="w-[1280px] h-[800px] origin-center scale-[0.75] bg-bg-primary rounded-2xl shadow-[0_0_50px_rgba(0,0,0,0.15)] border border-brand-primary/30 overflow-y-auto pointer-events-none transition-all duration-300">
            <HeroSection config={activeConfig?.hero} isLoading={false} />
            <AboutSection config={activeConfig?.about} isLoading={false} />
            <FeaturedProductsSection config={activeConfig?.featured_products} isLoading={false} />
            <CatalogLinkSection config={activeConfig?.catalog_link} isLoading={false} />
          </div>
        </div>
      </main>
    </div>
  );
}