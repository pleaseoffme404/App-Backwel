import { useState, useEffect, useCallback } from 'react';

export interface BusinessConfig {
  business_name?: string;
  legal_name?: string;
  logo_url?: string;
  support_email?: string;
  phone?: string;
  address?: string;
  tax_id?: string;
}

let cachedBusinessConfig: BusinessConfig | null = null;
let isFetching = false;
const listeners = new Set<() => void>();

export function useBusiness() {
  const [business, setBusiness] = useState<BusinessConfig | null>(cachedBusinessConfig);
  const [isLoading, setIsLoading] = useState(!cachedBusinessConfig);

  const fetchBusiness = useCallback(async () => {
    if (isFetching) return;
    isFetching = true;
    try {
      // Pedimos la configuración a la API
      const res = await fetch('/api/config/business');
      
      if (res.ok) {
        const data = await res.json();
        console.log(" Datos de negocio recibidos del backend:", data);

        cachedBusinessConfig = data.business || data || {};
        listeners.forEach(listener => listener());
      } else {
        console.error("El backend respondió con error:", res.status); 
      }
    } catch (error) {
      console.error("Error de red al pedir configuración de negocio:", error);
    } finally {
      isFetching = false;
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    const update = () => {
      setBusiness(cachedBusinessConfig);
      setIsLoading(false);
    };
    listeners.add(update);

    if (!cachedBusinessConfig) fetchBusiness();
    else update();

    return () => { listeners.delete(update); };
  }, [fetchBusiness]);

  return { business, isLoading, refreshBusiness: fetchBusiness };
}