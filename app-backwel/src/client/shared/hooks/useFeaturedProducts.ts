import { useState, useEffect } from 'react';

export interface FeaturedProduct {
  id: string;
  name: string;
  description: string;
  price: number;
  image_url: string;
}

export function useFeaturedProducts() {
  const [products, setProducts] = useState<FeaturedProduct[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // TODO: reemplazar con GET /api/v1/catalog/products cuando el endpoint esté disponible
    fetch('/api/config/featured-products')
      .then((res) => {
        if (!res.ok) throw new Error();
        return res.json();
      })
      .then((data) => {
        setProducts(data);
        setIsLoading(false);
      })
      .catch(() => {
        setIsLoading(false);
      });
  }, []);

  return { products, isLoading };
}