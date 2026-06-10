export interface BackendItemDTO {
  itemId: string;
  itemAttributes: Record<string, string>;
  pictures: string[];
  basePrice: number;
  availableStock: number;
}

export interface BackendProductDTO {
  productId: string;
  productName: string;
  brand: string;
  items: BackendItemDTO[];
}

export interface Product {
  id: string;
  productId?: string;
  name: string;
  price: number;
  stock: number;
  image: string;
}

export interface CartItem extends Product {
  quantity: number;
}