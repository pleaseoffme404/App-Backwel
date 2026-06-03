import React from 'react';
import { Link } from 'react-router-dom';
import { Skeleton } from '../../../shared/ui/Skeleton';
import { PageConfigSection } from '../../../shared/hooks/usePageConfig';

interface HeroSectionProps {
  config?: PageConfigSection;
  isLoading: boolean;
}

export function HeroSection({ config, isLoading }: HeroSectionProps) {
  if (isLoading) {
    return (
      <div className="w-full h-[80vh] min-h-[600px] relative flex items-center justify-center p-6 bg-bg-primary">
        <Skeleton className="absolute inset-0 w-full h-full opacity-50" />
        <div className="relative z-10 flex flex-col items-center gap-6 text-center w-full max-w-4xl">
          <Skeleton className="w-4/5 h-20 md:h-24 rounded-lg" />
          <Skeleton className="w-2/3 h-8 md:h-10 rounded-lg" />
          <Skeleton className="w-56 h-16 rounded-xl mt-8" />
        </div>
      </div>
    );
  }

  if (config?.visible === false) return null;

  const bgImage = config?.bg_image_url || 'https://images.unsplash.com/photo-1550751827-4bd374c3f58b?q=80&w=2070';
  const title = config?.title || 'Construyendo el futuro de tus compras';
  const subtitle = config?.subtitle || 'Bienvenido a la plataforma más rápida y segura. Descubre nuestro catálogo.';
  const ctaText = config?.cta_text || 'Explorar ahora';

  return (
    <section className="relative w-full h-[80vh] min-h-[600px] flex items-center justify-center overflow-hidden bg-bg-primary">
      <div 
        className="absolute inset-0 bg-cover bg-center bg-no-repeat transition-opacity duration-700"
        style={{ backgroundImage: `url(${bgImage})` }}
      >
        <div className="absolute inset-0 bg-bg-primary/80 backdrop-blur-sm" />
      </div>

      <div className="relative z-10 flex flex-col items-center text-center p-6 max-w-4xl mx-auto">
        <h1 className="text-5xl md:text-7xl font-black text-text-primary tracking-tighter mb-6 drop-shadow-2xl">
          {title}
        </h1>
        <p className="text-xl md:text-2xl text-text-primary/80 font-medium mb-12 max-w-2xl">
          {subtitle}
        </p>
        <Link 
          to="/pos" 
          className="bg-brand-primary text-text-primary px-10 py-5 rounded-xl text-xl font-bold uppercase tracking-wider hover:bg-brand-secondary transition-all active:scale-95 shadow-xl shadow-brand-primary/20 border border-brand-primary/50"
        >
          {ctaText}
        </Link>
      </div>
    </section>
  );
}