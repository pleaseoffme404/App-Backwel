import React from 'react';
import { Skeleton } from '../../../shared/ui/Skeleton';
import { PageConfigSection } from '../../../shared/hooks/usePageConfig';

interface AboutSectionProps {
  config?: PageConfigSection;
  isLoading: boolean;
}

export function AboutSection({ config, isLoading }: AboutSectionProps) {
  if (isLoading) {
    return (
      <section className="w-full py-24 bg-bg-secondary px-6">
        <div className="max-w-6xl mx-auto flex flex-col md:flex-row items-center gap-12">
          <div className="flex-1 w-full space-y-6">
            <Skeleton className="w-1/3 h-12 rounded-lg" />
            <Skeleton className="w-full h-6 rounded" />
            <Skeleton className="w-full h-6 rounded" />
            <Skeleton className="w-4/5 h-6 rounded" />
          </div>
          <div className="flex-1 w-full">
            <Skeleton className="w-full aspect-video md:aspect-square lg:aspect-video rounded-2xl" />
          </div>
        </div>
      </section>
    );
  }

  if (!config || !config.visible) return null;

  return (
    <section className="w-full py-24 bg-bg-secondary px-6">
      <div className="max-w-6xl mx-auto flex flex-col md:flex-row items-center gap-12">
        <div className="flex-1 space-y-6">
          <h2 className="text-4xl md:text-5xl font-black text-text-primary tracking-tight">
            {config.title}
          </h2>
          <p className="text-xl text-text-primary/70 leading-relaxed font-medium">
            {config.description}
          </p>
        </div>
        
        {config.image_url && (
          <div className="flex-1 w-full relative">
            <div className="absolute inset-0 bg-brand-primary/20 transform translate-x-4 translate-y-4 rounded-2xl"></div>
            <img 
              src={config.image_url} 
              alt={config.title} 
              className="relative z-10 w-full aspect-video md:aspect-square lg:aspect-video object-cover rounded-2xl shadow-xl border border-brand-primary/10"
            />
          </div>
        )}
      </div>
    </section>
  );
}