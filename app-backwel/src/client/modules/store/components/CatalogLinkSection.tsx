import React from 'react';
import { Link } from 'react-router-dom';
import { Skeleton } from '../../../shared/ui/Skeleton';
import { PageConfigSection } from '../../../shared/hooks/usePageConfig';

interface CatalogLinkSectionProps {
  config?: PageConfigSection;
  isLoading: boolean;
}

export function CatalogLinkSection({ config, isLoading }: CatalogLinkSectionProps) {
  if (isLoading) {
    return (
      <section className="w-full py-24 bg-bg-secondary px-6">
        <div className="max-w-6xl mx-auto rounded-3xl overflow-hidden relative min-h-[400px] flex items-center justify-center">
          <Skeleton className="absolute inset-0 w-full h-full opacity-50" />
          <div className="relative z-10 flex flex-col items-center gap-8 w-full max-w-2xl px-6">
            <Skeleton className="w-full h-16 md:h-20 rounded-xl" />
            <Skeleton className="w-64 h-16 rounded-xl" />
          </div>
        </div>
      </section>
    );
  }

  if (!config || !config.visible) return null;

  return (
    <section className="w-full py-24 bg-bg-secondary px-6">
      <div className="max-w-6xl mx-auto rounded-3xl overflow-hidden relative min-h-[400px] flex items-center justify-center shadow-2xl border border-brand-primary/20 group">
        {config.banner_image_url ? (
          <div 
            className="absolute inset-0 bg-cover bg-center bg-no-repeat transition-transform duration-1000 group-hover:scale-105"
            style={{ backgroundImage: `url(${config.banner_image_url})` }}
          >
            <div className="absolute inset-0 bg-brand-primary/90 mix-blend-multiply" />
            <div className="absolute inset-0 bg-gradient-to-t from-bg-primary/90 to-transparent" />
          </div>
        ) : (
          <div className="absolute inset-0 bg-brand-primary/10" />
        )}

        <div className="relative z-10 flex flex-col items-center text-center px-6 max-w-3xl">
          <h2 className="text-4xl md:text-6xl font-black text-white tracking-tight mb-10 drop-shadow-2xl">
            {config.banner_text}
          </h2>
          <Link 
            to="/store" 
            className="bg-accent text-white px-12 py-5 rounded-xl text-xl font-bold uppercase tracking-wider hover:bg-white hover:text-accent transition-colors active:scale-95 shadow-xl"
          >
            {config.button_text}
          </Link>
        </div>
      </div>
    </section>
  );
}