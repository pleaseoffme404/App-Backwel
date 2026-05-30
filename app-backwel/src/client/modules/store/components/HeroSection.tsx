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

  if (!config || !config.visible) return null;

  return (
    <section className="relative w-full h-[80vh] min-h-[600px] flex items-center justify-center overflow-hidden bg-bg-primary">
      {config.bg_image_url && (
        <div 
          className="absolute inset-0 bg-cover bg-center bg-no-repeat transition-opacity duration-700"
          style={{ backgroundImage: `url(${config.bg_image_url})` }}
        >
          <div className="absolute inset-0 bg-bg-primary/80 backdrop-blur-sm" />
        </div>
      )}

      <div className="relative z-10 flex flex-col items-center text-center p-6 max-w-4xl mx-auto">
        <h1 className="text-5xl md:text-7xl font-black text-text-primary tracking-tighter mb-6 drop-shadow-2xl">
          {config.title}
        </h1>
        <p className="text-xl md:text-2xl text-text-primary/80 font-medium mb-12 max-w-2xl">
          {config.subtitle}
        </p>
        <Link 
          to="/store" 
          className="bg-brand-primary text-text-primary px-10 py-5 rounded-xl text-xl font-bold uppercase tracking-wider hover:bg-brand-secondary transition-all active:scale-95 shadow-xl shadow-brand-primary/20 border border-brand-primary/50"
        >
          {config.cta_text}
        </Link>
      </div>
    </section>
  );
}