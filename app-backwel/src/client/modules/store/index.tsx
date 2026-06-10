import React from 'react';
import { Header } from '../../shared/ui/Header';
import { usePageConfig } from '../../shared/hooks/usePageConfig';
import { HeroSection } from './components/HeroSection';
import { AboutSection } from './components/AboutSection';
import { FeaturedProductsSection } from './components/FeaturedProductsSection';
import { CatalogLinkSection } from './components/CatalogLinkSection';
import { FooterSection } from './components/FooterSection';

export default function StoreLanding() {
  const { publishedConfig, isLoading } = usePageConfig();

  return (
    <div className="min-h-screen bg-bg-primary flex flex-col font-sans overflow-x-hidden">
      <Header />
      <main className="flex-1 flex flex-col w-full">
        <HeroSection config={publishedConfig?.hero} isLoading={isLoading} />
        <AboutSection config={publishedConfig?.about} isLoading={isLoading} />
        <FeaturedProductsSection config={publishedConfig?.featured_products} isLoading={isLoading} />
        <CatalogLinkSection config={publishedConfig?.catalog_link} isLoading={isLoading} />
        <FooterSection />
      </main>
    </div>
  );
}