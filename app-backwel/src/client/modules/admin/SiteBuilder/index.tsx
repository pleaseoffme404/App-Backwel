import React, { useEffect, useState } from 'react';
import { useSession } from '../../../shared/hooks/useSession';
import { usePageConfig } from '../../../shared/hooks/usePageConfig';
import { useTheme } from '../../../shared/hooks/useTheme';
import { HeroSection } from '../../store/components/HeroSection';
import { AboutSection } from '../../store/components/AboutSection';
import { FeaturedProductsSection } from '../../store/components/FeaturedProductsSection';
import { CatalogLinkSection } from '../../store/components/CatalogLinkSection';
import { NotFound } from '../../../shared/ui/NotFound';

const defaultColors = {
  dark_bg_primary: '#0F172A', dark_bg_secondary: '#1E293B',
  dark_brand_primary: '#38BDF8', dark_brand_secondary: '#7DD3FC',
  dark_accent: '#F97316', dark_text_primary: '#F8FAFC',
  light_bg_primary: '#F4F7FE', light_bg_secondary: '#FFFFFF',
  light_brand_primary: '#0A3C51', light_brand_secondary: '#126385',
  light_accent: '#E85D04', light_text_primary: '#1A202C',
};

export default function SiteBuilder() {
  const [activePage, setActivePage] = useState<'landing' | 'not_found' | 'theme' | null>(null);
  const { session, isLoading: isSessionLoading } = useSession();
  const { theme, toggleTheme } = useTheme();
  
const { 
    publishedConfig, draftConfig, isLoading: isConfigLoading,
    updateDraft, undoLastChange, clearAllDrafts, publishChanges,
    createSavepoint, restoreSavepoint
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

const activeConfig: Record<string, any> = draftConfig || publishedConfig || {};
  const hasChanges = !!draftConfig;

  const handlePublish = async () => {
    const success = await publishChanges();
    if (success) {
      alert(activePage === 'theme' 
        ? 'Tema publicado. Los usuarios lo verán de inmediato. Recarga tu página para aplicarlo en el panel de Admin.' 
        : 'Cambios publicados exitosamente');
    } else {
      alert('Error al publicar los cambios');
    }
  };

 const renderInput = (section: string, field: string, label: string, type = 'text', options: any[] = []) => {
    let value = activeConfig[section]?.[field];
    if (value === undefined) {
      if (type === 'checkbox') value = false;
      else if (type === 'color') value = defaultColors[field as keyof typeof defaultColors];
      else if (type === 'range') value = 8;
      else if (type === 'select') value = options[0]?.value;
      else value = '';
    }
    
    if (type === 'color') {
      return (
        <div className="flex flex-col gap-2 mb-5">
          <label className="text-xs font-black text-text-primary/70 uppercase tracking-wider">{label}</label>
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg overflow-hidden border border-brand-primary/20 shrink-0 relative">
              <input
                type="color"
                className="absolute -top-2 -left-2 w-16 h-16 cursor-pointer"
                defaultValue={value}
                onBlur={(e) => updateDraft(section, { [field]: e.target.value })}
                onChange={(e) => {
                  const root = document.getElementById('preview-container');
                  if (root) {
                    const cssVarName = `--${field.replace('dark_', '').replace('light_', '').replace('_', '-')}`;
                    root.style.setProperty(cssVarName, e.target.value);
                  }
                }}
              />
            </div>
            <span className="text-sm font-mono opacity-70 uppercase">{value}</span>
          </div>
        </div>
      );
    }

    if (type === 'select') {
      return (
        <div className="flex flex-col gap-2 mb-5">
          <label className="text-xs font-black text-text-primary/70 uppercase tracking-wider">{label}</label>
          <select
            className="p-3 rounded-xl bg-bg-primary border border-brand-primary/20 text-text-primary text-sm focus:border-brand-primary focus:outline-none transition-all"
            value={value}
            onChange={(e) => {
              updateDraft(section, { [field]: e.target.value });
              const root = document.getElementById('preview-container');
              if (root && field === 'font_family') root.style.setProperty('--font-family-global', e.target.value);
            }}
          >
            {options.map(opt => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
          </select>
        </div>
      );
    }

    if (type === 'range') {
      return (
        <div className="flex flex-col gap-2 mb-5">
          <label className="text-xs font-black text-text-primary/70 uppercase tracking-wider">{label} ({value}px)</label>
          <input 
            type="range"
            min="0"
            max="32"
            className="w-full h-2 bg-brand-primary/20 rounded-lg appearance-none cursor-pointer accent-brand-primary"
            value={value}
            onChange={(e) => {
              updateDraft(section, { [field]: parseInt(e.target.value) });
              const root = document.getElementById('preview-container');
              if (root && field === 'border_radius') root.style.setProperty('--radius-global', `${e.target.value}px`);
            }}
          />
        </div>
      );
    }

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
              type="checkbox" className="sr-only peer" checked={value}
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

  if (!activePage) {
    return (
      <div className="h-full w-full p-8 overflow-y-auto bg-bg-primary text-text-primary">
        <h1 className="text-3xl font-black mb-2">Site Builder</h1>
        <p className="opacity-80 mb-8">Selecciona la página o sección global que deseas editar.</p>
        
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <button 
            onClick={() => setActivePage('landing')}
            className="bg-bg-secondary border border-brand-primary/20 p-6 rounded-xl hover:-translate-y-1 hover:shadow-lg hover:border-brand-primary/50 transition-all text-left flex flex-col gap-2 group"
          >
            <span className="text-2xl font-bold text-text-primary group-hover:text-brand-primary transition-colors">Landing Page</span>
            <span className="text-sm opacity-70">Página principal de la tienda, hero, nosotros y productos destacados.</span>
          </button>

          <button 
            onClick={() => setActivePage('not_found')}
            className="bg-bg-secondary border border-brand-primary/20 p-6 rounded-xl hover:-translate-y-1 hover:shadow-lg hover:border-brand-primary/50 transition-all text-left flex flex-col gap-2 group"
          >
            <span className="text-2xl font-bold text-text-primary group-hover:text-brand-primary transition-colors">Página 404</span>
            <span className="text-sm opacity-70">Pantalla de error cuando una ruta no existe o está restringida.</span>
          </button>

          <button 
            onClick={() => setActivePage('theme')}
            className="bg-bg-secondary border border-brand-primary/20 p-6 rounded-xl hover:-translate-y-1 hover:shadow-lg hover:border-brand-primary/50 transition-all text-left flex flex-col gap-2 group"
          >
            <span className="text-2xl font-bold text-text-primary group-hover:text-brand-primary transition-colors">Tema Global (CSS)</span>
            <span className="text-sm opacity-70">Constructor visual de colores globales para modo claro y oscuro.</span>
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-screen w-full bg-bg-primary overflow-hidden font-sans">
      
      {}
      <style>
        {`
          #preview-container {
            --bg-primary: ${activeConfig.theme?.[`${theme}_bg_primary`] || defaultColors[`${theme}_bg_primary` as keyof typeof defaultColors]};
            --bg-secondary: ${activeConfig.theme?.[`${theme}_bg_secondary`] || defaultColors[`${theme}_bg_secondary` as keyof typeof defaultColors]};
            --brand-primary: ${activeConfig.theme?.[`${theme}_brand_primary`] || defaultColors[`${theme}_brand_primary` as keyof typeof defaultColors]};
            --brand-secondary: ${activeConfig.theme?.[`${theme}_brand_secondary`] || defaultColors[`${theme}_brand_secondary` as keyof typeof defaultColors]};
            --accent: ${activeConfig.theme?.[`${theme}_accent`] || defaultColors[`${theme}_accent` as keyof typeof defaultColors]};
            --text-primary: ${activeConfig.theme?.[`${theme}_text_primary`] || defaultColors[`${theme}_text_primary` as keyof typeof defaultColors]};
          }
        `}
      </style>

      <aside className="w-[450px] bg-bg-secondary border-r border-brand-primary/20 flex flex-col h-full shrink-0 shadow-2xl relative z-20">
        <div className="p-6 border-b border-brand-primary/20 bg-bg-secondary flex flex-col gap-4 shadow-sm">
          <div className="flex justify-between items-center mb-1">
            <button 
              onClick={() => setActivePage(null)}
              className="text-xs font-black text-text-primary/50 hover:text-brand-primary uppercase tracking-wider transition-colors"
            >
              ← Volver al menú
            </button>
          </div>
          <div className="flex justify-between items-center">
            <h1 className="text-2xl font-black text-text-primary tracking-tight">
              {activePage === 'landing' ? 'Landing Page' : activePage === 'not_found' ? 'Página 404' : 'Tema Global'}
            </h1>
            {hasChanges && <span className="w-3 h-3 bg-accent rounded-full animate-pulse" title="Cambios sin guardar"></span>}
          </div>
          
          <div className="flex gap-3 text-sm font-bold mt-2">
            <button 
              onClick={undoLastChange} disabled={!hasChanges}
              className="px-4 py-2.5 bg-bg-primary border border-brand-primary/20 rounded-lg hover:border-brand-primary/50 disabled:opacity-50 flex-1 transition-all active:scale-95 text-text-primary"
            >
              Deshacer
            </button>
            <button 
              onClick={clearAllDrafts} disabled={!hasChanges}
              className="px-4 py-2.5 bg-red-500/10 text-red-500 rounded-lg hover:bg-red-500/20 disabled:opacity-50 flex-1 transition-all active:scale-95"
            >
              Limpiar
            </button>
          </div>
          
          <div className="flex gap-3 text-xs font-bold">
            <button 
              onClick={createSavepoint}
              className="px-4 py-2 bg-brand-primary/10 text-brand-primary border border-brand-primary/20 rounded-lg hover:bg-brand-primary/20 flex-1 transition-all active:scale-95"
            >
              Crear Savepoint
            </button>
            <button 
              onClick={restoreSavepoint}
              className="px-4 py-2 bg-bg-primary text-text-primary border border-text-primary/10 rounded-lg hover:border-brand-primary/50 flex-1 transition-all active:scale-95"
            >
              Cargar Savepoint
            </button>
          </div>
          
          <button 
            onClick={handlePublish} disabled={!hasChanges}
            className="w-full py-4 mt-2 bg-accent text-white rounded-xl font-black uppercase tracking-widest hover:bg-accent/90 disabled:opacity-50 disabled:cursor-not-allowed transition-all active:scale-[0.98] shadow-lg"
          >
            Publicar Cambios
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-6 flex flex-col gap-10">
          {activePage === 'landing' ? (
            <>
              <section>
                <h2 className="text-xl font-black text-brand-primary mb-6 flex items-center gap-2"><span className="w-8 h-px bg-brand-primary/30"></span> Hero Section</h2>
                {renderInput('hero', 'visible', 'Visibilidad', 'checkbox')}
                {renderInput('hero', 'title', 'Título Principal')}
                {renderInput('hero', 'subtitle', 'Subtítulo', 'textarea')}
                {renderInput('hero', 'cta_text', 'Texto Botón Acción')}
                {renderInput('hero', 'bg_image_url', 'URL Imagen de Fondo')}
              </section>

              <section>
                <h2 className="text-xl font-black text-brand-primary mb-6 flex items-center gap-2"><span className="w-8 h-px bg-brand-primary/30"></span> Sección Nosotros</h2>
                {renderInput('about', 'visible', 'Visibilidad', 'checkbox')}
                {renderInput('about', 'title', 'Título')}
                {renderInput('about', 'description', 'Descripción Completa', 'textarea')}
                {renderInput('about', 'image_url', 'URL Imagen Lateral')}
              </section>

              <section>
                <h2 className="text-xl font-black text-brand-primary mb-6 flex items-center gap-2"><span className="w-8 h-px bg-brand-primary/30"></span> Productos Destacados</h2>
                {renderInput('featured_products', 'visible', 'Visibilidad', 'checkbox')}
                {renderInput('featured_products', 'title', 'Título de Bloque')}
                {renderInput('featured_products', 'subtitle', 'Subtítulo Corto')}
              </section>

              <section>
                <h2 className="text-xl font-black text-brand-primary mb-6 flex items-center gap-2"><span className="w-8 h-px bg-brand-primary/30"></span> Banner Catálogo</h2>
                {renderInput('catalog_link', 'visible', 'Visibilidad', 'checkbox')}
                {renderInput('catalog_link', 'banner_text', 'Llamada a la Acción')}
                {renderInput('catalog_link', 'button_text', 'Texto del Botón')}
                {renderInput('catalog_link', 'banner_image_url', 'URL Imagen de Fondo')}
              </section>
            </>
          ) : activePage === 'not_found' ? (
            <>
              <section>
                <h2 className="text-xl font-black text-brand-primary mb-6 flex items-center gap-2"><span className="w-8 h-px bg-brand-primary/30"></span> Configuración 404</h2>
                {renderInput('not_found', 'title', 'Título Principal (Ej: 404)')}
                {renderInput('not_found', 'message', 'Mensaje de Error')}
                {renderInput('not_found', 'description', 'Descripción', 'textarea')}
                {renderInput('not_found', 'button_text', 'Texto del Botón')}
                {renderInput('not_found', 'support_email', 'Correo de Soporte')}
              </section>
            </>
          ) : (
            <>
              <section>
                <h2 className="text-xl font-black text-brand-primary mb-6 flex items-center gap-2"><span className="w-8 h-px bg-brand-primary/30"></span> Paleta: Modo Oscuro</h2>
                <div className="grid grid-cols-2 gap-4">
                  {renderInput('theme', 'dark_bg_primary', 'Fondo Princ.', 'color')}
                  {renderInput('theme', 'dark_bg_secondary', 'Fondo Sec.', 'color')}
                  {renderInput('theme', 'dark_brand_primary', 'Marca Princ.', 'color')}
                  {renderInput('theme', 'dark_brand_secondary', 'Marca Sec.', 'color')}
                  {renderInput('theme', 'dark_accent', 'Acento/Botón', 'color')}
                  {renderInput('theme', 'dark_text_primary', 'Texto', 'color')}
                </div>
              </section>
              <section>
                <h2 className="text-xl font-black text-brand-primary mb-6 flex items-center gap-2"><span className="w-8 h-px bg-brand-primary/30"></span> Paleta: Modo Claro</h2>
                <div className="grid grid-cols-2 gap-4">
                  {renderInput('theme', 'light_bg_primary', 'Fondo Princ.', 'color')}
                  {renderInput('theme', 'light_bg_secondary', 'Fondo Sec.', 'color')}
                  {renderInput('theme', 'light_brand_primary', 'Marca Princ.', 'color')}
                  {renderInput('theme', 'light_brand_secondary', 'Marca Sec.', 'color')}
                   {renderInput('theme', 'light_accent', 'Acento/Botón', 'color')}
                  {renderInput('theme', 'light_text_primary', 'Texto', 'color')}
                </div>
              </section>

              <section>
                <h2 className="text-xl font-black text-brand-primary mb-6 flex items-center gap-2"><span className="w-8 h-px bg-brand-primary/30"></span> Estructura Global</h2>
                {renderInput('theme', 'border_radius', 'Redondez de Contenedores', 'range')}
                {renderInput('theme', 'font_family', 'Tipografía Global', 'select', [
                  { value: 'Inter, sans-serif', label: 'Inter (Moderna)' },
                  { value: 'Roboto, sans-serif', label: 'Roboto (Elegante)' },
                  { value: 'Space Grotesk, sans-serif', label: 'Space Grotesk (Tecnológica)' },
                  { value: 'Merriweather, serif', label: 'Merriweather (Clásica)' }
                ])}
              </section>
            </>
          )}
        </div>
      </aside>

      <main className="flex-1 relative bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAiIGhlaWdodD0iMjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iMSIgY3k9IjEiIHI9IjEiIGZpbGw9InJnYmEoMCwwLDAsMC4wNSkiLz48L3N2Zz4=')]">
        
        <div className="absolute top-6 left-1/2 -translate-x-1/2 bg-bg-secondary px-6 py-2 rounded-full border border-brand-primary/20 shadow-lg z-30 font-mono text-sm font-bold text-brand-secondary flex items-center gap-4">
          <span>VISTA PREVIA (Escala 75%)</span>
          <div className="w-px h-4 bg-brand-primary/20"></div>
          <button 
            onClick={toggleTheme} 
            className="hover:text-brand-primary uppercase transition-colors pointer-events-auto cursor-pointer"
          >
            Modo: {theme === 'dark' ? 'Oscuro 🌙' : 'Claro ☀️'}
          </button>
        </div>
        
        <div className="w-full h-full overflow-hidden flex items-center justify-center p-12">
          <div 
            id="preview-container"
            className="w-[1280px] h-[800px] origin-center scale-[0.75] bg-bg-primary text-text-primary rounded-2xl shadow-[0_0_50px_rgba(0,0,0,0.15)] border border-brand-primary/30 overflow-y-auto transition-all duration-300"
          >
            {activePage === 'landing' || activePage === 'theme' ? (
              <>
                <HeroSection config={activeConfig?.hero} isLoading={false} />
                <AboutSection config={activeConfig?.about} isLoading={false} />
                <FeaturedProductsSection config={activeConfig?.featured_products} isLoading={false} />
                <CatalogLinkSection config={activeConfig?.catalog_link} isLoading={false} />
              </>
            ) : (
              <NotFound previewConfig={activeConfig?.not_found} />
            )}
          </div>
        </div>
      </main>
    </div>
  );
}