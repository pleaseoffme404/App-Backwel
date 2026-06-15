import React, { useState, useEffect } from 'react';
import Swal from 'sweetalert2';

interface ItemVariant {
  itemAttributes: Record<string, string>;
  baseSalePrice: number;
  initialStock: number;
  redundancyStock: number;
  logicalLimit: number;
  images: string[];
}

interface Category {
  id: string;
  name: string;
  description?: string;
  parentId?: string | null;
  children?: Category[];
}

const buildCategoryTree = (flatCategories: any[]): Category[] => {
  const tree: Category[] = [];
  const lookup: Record<string, Category> = {};

  flatCategories.forEach(cat => {
    const id = cat.categoryId || cat.id;
    lookup[id] = { ...cat, id, children: [] };
  });

  flatCategories.forEach(cat => {
    const id = cat.categoryId || cat.id;
    const item = lookup[id];
    const parentId = cat.parentId;

    if (parentId && lookup[parentId]) {
      lookup[parentId].children?.push(item);
    } else {
      tree.push(item);
    }
  });

  return tree;
};

export default function ProductosAdmin() {
  const [activeTab, setActiveTab] = useState('productos');
  const [categories, setCategories] = useState<any[]>([]);
  const [products, setProducts] = useState<any[]>([]);

  // States for Discount Management
  const [discounts, setDiscounts] = useState<any[]>([]);
  const [isCreatingDiscount, setIsCreatingDiscount] = useState(false);
  const [descName, setDescName] = useState('');
  const [descPercent, setDescPercent] = useState<number>(10);
  const [descStackable, setDescStackable] = useState(false);
  const [descStartDate, setDescStartDate] = useState('');
  const [descEndDate, setDescEndDate] = useState('');
  const [descTargetCategory, setDescTargetCategory] = useState('');
  
  // States for Product Management
  const [isCreating, setIsCreating] = useState(false);
  const [name, setName] = useState('');
  const [brand, setBrand] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [description, setDescription] = useState('');
const [attributes, setAttributes] = useState<string[]>([]);
  const [newAttr, setNewAttr] = useState('');
 const [items, setItems] = useState<ItemVariant[]>([]);
  const [editingProduct, setEditingProduct] = useState<any | null>(null);
  const [isEditingInfo, setIsEditingInfo] = useState(false);
  const [inventorySearch, setInventorySearch] = useState('');
  const [selectedInventoryItem, setSelectedInventoryItem] = useState<any | null>(null);
  const [stockChangeAmount, setStockChangeAmount] = useState<number>(0);
  const [stockChangeType, setStockChangeType] = useState<'ADD' | 'REMOVE'>('ADD');
  // States for Category Management
  const [isCreatingCategory, setIsCreatingCategory] = useState(false);
  const [catName, setCatName] = useState('');
  const [catDescription, setCatDescription] = useState('');
  const [catParentId, setCatParentId] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<Category | null>(null);

  const fetchCategories = async () => {
    try {
      const [catRes, metaRes] = await Promise.all([
        fetch('/api/v1/categories/'),
        fetch('/api/config/category-metadata').catch(() => null)
      ]);

      if (catRes.ok) {
        const data = await catRes.json();
        let metadataMap: Record<string, string> = {};

        if (metaRes && metaRes.ok) {
          const metaData = await metaRes.json();
          metaData.forEach((m: any) => {
            metadataMap[m.category_id] = m.description;
          });
        }

        const normalized = data.map((c: any) => ({
          ...c,
          name: c.categoryName || c.name,
          description: metadataMap[c.categoryId || c.id] || ''
        }));
        setCategories(normalized);
      }
    } catch (error) {
      console.error('Error fetching categories:', error);
    }
  };

  
const fetchProducts = async () => {
    try {
      const meiliUrl = import.meta.env.VITE_MEILISEARCH_URL || 'http://localhost:7700';
      const meiliKey = import.meta.env.VITE_MEILISEARCH_API_KEY || 'C8ntQZrkHab2NuAOJlZtzi2399jOplWfL1YeOJ3Oq2l'; 
      
      const res = await fetch(`${meiliUrl}/indexes/PRODUCTS/search`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${meiliKey}`
        },
        body: JSON.stringify({
          q: '', 
          limit: 100 
        })
      });

      if (res.ok) {
        const data = await res.json();
        setProducts(data.hits || []);
      } else {
        console.warn(await res.text());
      }
    } catch (error) {
      console.warn(error);
    }
  };
  const fetchDiscounts = async () => {
    try {
      const res = await fetch('/api/v1/discounts/search');
      if (res.ok) {
        const data = await res.json();
        setDiscounts(data.content || []);
      }
    } catch (error) {
      console.error(error);
    }
  };

  const handleAdjustInventory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedInventoryItem || stockChangeAmount <= 0) return;

    const multiplier = stockChangeType === 'ADD' ? 1 : -1;
    const delta = stockChangeAmount * multiplier;

    const payload = {
        itemId: selectedInventoryItem.id || selectedInventoryItem.itemId,
        availableDelta: delta,
        reservedDelta: 0,
        redundancyDelta: 0,
        physicalDelta: delta
    };

    try {
        const res = await fetch(`/api/v1/inventory/adjust`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            import('sweetalert2').then(({ default: Swal }) => Swal.fire('¡Stock Actualizado!', `Se han ${stockChangeType === 'ADD' ? 'añadido' : 'retirado'} ${stockChangeAmount} unidades físicamente.`, 'success'));
            setSelectedInventoryItem(null);
            setStockChangeAmount(0);
        } else {
            import('sweetalert2').then(({ default: Swal }) => Swal.fire('Endpoint faltante', 'Pide a tu Dev de Java que termine el endpoint POST /inventory/adjust recibiendo InventoryDeltaRequest', 'info'));
        }
    } catch(err) {
        console.error(err);
    }
  };


useEffect(() => {
    fetchCategories();
    fetchProducts();
    fetchDiscounts();
  }, []);

  const fetchProductDetails = async (id: string) => {
    try {
      const res = await fetch(`/api/v1/products/?productId=${id}`);
      if (res.ok) {
        const fullProduct = await res.json();
        setEditingProduct(fullProduct);
      } else {
        import('sweetalert2').then(({ default: Swal }) => Swal.fire('Error', 'No se pudo cargar el detalle del producto desde la base principal', 'error'));
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleUpdateProductBasicInfo = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingProduct) return;
    try {
      const res = await fetch(`/api/v1/products/?productId=${editingProduct.productId || editingProduct.id}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: editingProduct.name,
          brand: editingProduct.brand,
          description: editingProduct.description
        })
      });
      if (res.ok) {
        import('sweetalert2').then(({ default: Swal }) => Swal.fire('Actualizado', 'Información base guardada.', 'success'));
        setIsEditingInfo(false);
        fetchProducts(); // Refrescar catálogo indexado
        const updated = await res.json();
        setEditingProduct(updated);
      } else {
        import('sweetalert2').then(({ default: Swal }) => Swal.fire('Error', 'No se pudo actualizar', 'error'));
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleCreateDiscount = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!descTargetCategory) {
      import('sweetalert2').then(({ default: Swal }) => Swal.fire('Error', 'Selecciona una categoría objetivo', 'error'));
      return;
    }

    const payload = {
      discountName: descName,
      stackable: descStackable,
      discountDecimal: descPercent / 100,
      startDate: new Date(descStartDate).toISOString(),
      endDate: new Date(descEndDate).toISOString(),
      targets: {
        itemTargets: [],
        productTargets: [],
        categoryTargets: [descTargetCategory]
      }
    };

    try {
      const res = await fetch('/api/v1/discounts/', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (res.ok) {
        import('sweetalert2').then(({ default: Swal }) => Swal.fire('¡Éxito!', 'Descuento promocional activado.', 'success'));
        setIsCreatingDiscount(false);
        setDescName('');
        setDescPercent(10);
        setDescStackable(false);
        setDescStartDate('');
        setDescEndDate('');
        setDescTargetCategory('');
        fetchDiscounts();
      } else {
        const err = await res.json();
        import('sweetalert2').then(({ default: Swal }) => Swal.fire('Error de validación', err.message || 'Verifica las fechas y datos', 'error'));
      }
    } catch (error) {
      import('sweetalert2').then(({ default: Swal }) => Swal.fire('Error de red', 'No se pudo contactar al servidor', 'error'));
    }
  };

  const handleCreateCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch('/api/v1/categories/', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          categoryName: catName,
          parentId: catParentId || null
        })
      });
      
      if (res.ok) {
        const newCat = await res.json();
        const newCatId = newCat.categoryId || newCat.id;

        if (newCatId && catDescription.trim()) {
          await fetch('/api/config/category-metadata', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ categoryId: newCatId, description: catDescription })
          }).catch(() => {});
        }

        Swal.fire({
          title: '¡Éxito!',
          text: 'Categoría creada correctamente.',
          icon: 'success',
          confirmButtonColor: '#38BDF8'
        });

        setIsCreatingCategory(false);
        setCatName('');
        setCatDescription('');
        setCatParentId('');
        fetchCategories();
      } else {
        Swal.fire({
          title: 'Error',
          text: 'Error al crear la categoría. Revisa los datos.',
          icon: 'error',
          confirmButtonColor: '#38BDF8'
        });
      }
    } catch (err) {
      Swal.fire({
        title: 'Error de red',
        text: 'Error al conectar con el servidor.',
        icon: 'error',
        confirmButtonColor: '#38BDF8'
      });
    }
  };

  const handleNavigateToProducts = (catId: string) => {
    setCategoryId(catId);
    setSelectedCategory(null);
    setActiveTab('productos');
  };

  const handleAddAttribute = (e: React.KeyboardEvent | React.MouseEvent) => {
    if (('key' in e && e.key === 'Enter') || e.type === 'click') {
      e.preventDefault();
      if (newAttr.trim() && !attributes.includes(newAttr.trim())) {
        setAttributes([...attributes, newAttr.trim()]);
        setNewAttr('');
      }
    }
  };

  const handleAddItem = () => {
    const defaultAttrs: Record<string, string> = {};
    attributes.forEach(attr => defaultAttrs[attr] = '');
    setItems([...items, {
      itemAttributes: defaultAttrs,
      baseSalePrice: 0.01,
      initialStock: 0,
      redundancyStock: 0,
      logicalLimit: 1,
      images: ['']
    }]);
  };

  const updateItem = (index: number, field: keyof ItemVariant, value: any) => {
    const updated = [...items];
    updated[index] = { ...updated[index], [field]: value };
    setItems(updated);
  };

  const updateItemAttribute = (itemIndex: number, attrName: string, value: string) => {
    const updated = [...items];
    updated[itemIndex].itemAttributes = { ...updated[itemIndex].itemAttributes, [attrName]: value };
    setItems(updated);
  };

  const updateItemImage = (itemIndex: number, imgIndex: number, url: string) => {
    const updated = [...items];
    const newImages = [...updated[itemIndex].images];
    newImages[imgIndex] = url;
    updated[itemIndex].images = newImages;
    setItems(updated);
  };

  

const addImageField = (itemIndex: number) => {
    const updated = [...items];
    updated[itemIndex].images.push('');
    setItems(updated);
  };

  const removeImageField = (itemIndex: number, imgIndex: number) => {
    const updated = [...items];
    const newImages = [...updated[itemIndex].images];
    newImages.splice(imgIndex, 1);
    updated[itemIndex].images = newImages;
    setItems(updated);
  };

 const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const payload = { name, description, categoryId, brand, attributes, items };
    try {
      const res = await fetch('/api/v1/products/', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      if (res.ok) {
        Swal.fire({
          title: '¡Producto Creado!',
          text: 'El producto y sus variantes han sido guardados.',
          icon: 'success',
          confirmButtonColor: '#38BDF8'
        });
        setIsCreating(false);
        setItems([]);
        setAttributes([]);
        setName('');
        setBrand('');
        setDescription('');
        setCategoryId('');
      } else {
        const error = await res.json();
        let errorMsg = 'Error al crear producto.';
        if (error.validationErrors) {
           const details = Object.entries(error.validationErrors).map(([_, val]) => `• ${val}`).join('<br/>');
           errorMsg = `Verifica los campos:<br/><div style="text-align: left; font-size: 0.9em; color: #ef4444;">${details}</div>`;
        }
        Swal.fire({
          title: 'Error de Validación',
          html: errorMsg,
          icon: 'error',
          confirmButtonColor: '#38BDF8'
        });
      }
    } catch (err) {
      Swal.fire({
        title: 'Error de red',
        text: 'No se pudo contactar con el servidor.',
        icon: 'error',
        confirmButtonColor: '#38BDF8'
      });
    }
  };
  const CategoryNodeItem = ({ category, level = 0 }: { category: Category, level?: number }) => (    <div className="flex flex-col w-full relative">
      <div 
        onClick={() => setSelectedCategory(category)}
        className="bg-bg-secondary p-4 rounded-xl border border-brand-primary/10 flex items-center justify-between hover:border-brand-primary cursor-pointer transition-colors my-1 relative z-10 shadow-sm"
        style={{ marginLeft: `${level * 2.5}rem` }}
      >
        {level > 0 && (
          <>
            <div className="absolute -left-6 top-1/2 w-6 h-px bg-brand-primary/20"></div>
            <div className="absolute -left-6 -top-6 w-px h-[calc(100%+1.5rem)] bg-brand-primary/20"></div>
          </>
        )}
        
        <div className="flex flex-col">
          <span className="font-bold text-lg text-brand-primary">{category.name}</span>
          <span className="text-sm text-text-primary/60 line-clamp-1">{category.description || 'Categoría base del sistema'}</span>
        </div>
        <span className="text-xs font-mono bg-bg-primary px-2 py-1 rounded text-text-primary/40 border border-brand-primary/10 hidden md:block">
          {category.id}
        </span>
      </div>
      
      {category.children && category.children.length > 0 && (
        <div className="flex flex-col w-full">
          {category.children.map(child => (
            <CategoryNodeItem key={child.id} category={child} level={level + 1} />
          ))}
        </div>
      )}
    </div>
  );

  const renderCategorias = () => {
    if (isCreatingCategory) {
      return (
        <div className="w-full max-w-3xl mx-auto flex flex-col gap-6 pb-12">
          <div className="flex items-center justify-between border-b border-brand-primary/10 pb-4">
            <h1 className="text-3xl font-black text-brand-primary tracking-tight">Nueva Categoría</h1>
            <button onClick={() => setIsCreatingCategory(false)} className="text-text-primary/60 hover:text-accent font-medium">Cancelar</button>
          </div>
          <form onSubmit={handleCreateCategory} className="bg-bg-secondary p-6 rounded-xl border border-brand-primary/20 flex flex-col gap-4 shadow-sm">
            <div className="flex flex-col gap-1">
              <label className="text-xs font-bold text-text-primary/70 uppercase">Nombre</label>
              <input type="text" required value={catName} onChange={e => setCatName(e.target.value)} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary" placeholder="Ej: Laptops, Ropa, Herramientas..." />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs font-bold text-text-primary/70 uppercase">Categoría Padre (Opcional)</label>
              <select value={catParentId} onChange={e => setCatParentId(e.target.value)} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary">
                <option value="">Ninguna (Categoría Raíz)</option>
                {categories.map((cat: any) => (
                  <option key={cat.categoryId || cat.id} value={cat.categoryId || cat.id}>{cat.name}</option>
                ))}
              </select>
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs font-bold text-text-primary/70 uppercase">Descripción</label>
              <textarea required value={catDescription} onChange={e => setCatDescription(e.target.value)} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary min-h-[100px] resize-y" placeholder="Descripción de la categoría..."></textarea>
            </div>
            <button type="submit" className="mt-4 py-4 bg-brand-primary text-bg-primary font-black text-lg rounded-xl hover:opacity-90 transition-all shadow-xl shadow-brand-primary/20">
              Guardar Categoría
            </button>
          </form>
        </div>
      );
    }

    return (
      <div className="w-full flex flex-col gap-6 relative">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-black text-brand-primary tracking-tight">Categorías</h1>
            <p className="text-text-primary/70">Selecciona una categoría para ver sus opciones.</p>
          </div>
          <button onClick={() => setIsCreatingCategory(true)} className="bg-brand-primary text-bg-primary px-6 py-3 rounded-lg font-bold hover:opacity-90 transition-opacity flex items-center gap-2">
            + Nueva Categoría
          </button>
        </div>
        
        <div className="flex flex-col gap-3 pb-8">
          {categories.length === 0 ? (
            <div className="bg-bg-secondary border border-brand-primary/20 rounded-xl p-8 text-center flex flex-col items-center justify-center min-h-[200px]">
              <h3 className="text-xl font-bold text-text-primary mb-2">Aún no hay categorías</h3>
              <p className="text-text-primary/60">Crea tu primera categoría raíz para empezar a ordenar tus productos.</p>
            </div>
          ) : (
            buildCategoryTree(categories).map(cat => (
              <CategoryNodeItem key={cat.id} category={cat} />
            ))
          )}
        </div>

        {selectedCategory && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
            <div className="bg-bg-secondary w-full max-w-lg rounded-2xl shadow-2xl border border-brand-primary/20 overflow-hidden flex flex-col">
              <div className="p-6 border-b border-brand-primary/10 flex justify-between items-center bg-bg-primary">
                <div>
                  <h3 className="text-2xl font-black text-brand-primary">{selectedCategory.name}</h3>
                  <p className="text-xs font-mono text-text-primary/40 mt-1">ID: {selectedCategory.id}</p>
                </div>
                <button onClick={() => setSelectedCategory(null)} className="text-text-primary/40 hover:text-accent font-black text-xl w-8 h-8 flex items-center justify-center rounded-full hover:bg-accent/10 transition-colors">&times;</button>
              </div>
              
              <div className="p-6 flex flex-col gap-4">
                <p className="text-text-primary/70 text-sm">
                  {selectedCategory.description || 'Esta categoría está activa y disponible en el catálogo.'}
                </p>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mt-4">
                  <button 
                    onClick={() => handleNavigateToProducts(selectedCategory.id)}
                    className="flex flex-col items-center justify-center p-4 bg-brand-primary/10 border border-brand-primary/30 rounded-xl hover:bg-brand-primary hover:text-bg-primary text-brand-primary transition-all group"
                  >
                    <span className="font-bold text-lg mb-1">Ver Productos</span>
                    <span className="text-xs opacity-70 group-hover:opacity-100">Gestionar catálogo</span>
                  </button>
                  
                  <button 
                    onClick={() => {
                      setIsCreatingCategory(true);
                      setCatParentId(selectedCategory.id);
                      setSelectedCategory(null);
                    }}
                    className="flex flex-col items-center justify-center p-4 bg-bg-primary border border-brand-primary/20 rounded-xl hover:border-brand-primary text-text-primary transition-all"
                  >
                    <span className="font-bold text-lg mb-1 text-brand-secondary">Añadir Subcategoría</span>
                    <span className="text-xs opacity-70">Crear nodo hijo</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    );
  };

const renderDescuentos = () => {
    if (isCreatingDiscount) {
      return (
        <div className="w-full max-w-4xl mx-auto flex flex-col gap-6 pb-12">
          <div className="flex items-center justify-between border-b border-brand-primary/10 pb-4">
            <h1 className="text-3xl font-black text-brand-primary tracking-tight">Nuevo Descuento Promocional</h1>
            <button onClick={() => setIsCreatingDiscount(false)} className="text-text-primary/60 hover:text-accent font-medium">Cancelar</button>
          </div>

          <form onSubmit={handleCreateDiscount} className="bg-bg-secondary p-6 rounded-xl border border-brand-primary/20 flex flex-col gap-6 shadow-sm">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="flex flex-col gap-1">
                <label className="text-xs font-bold text-text-primary/70 uppercase">Nombre de la Promoción</label>
                <input type="text" required value={descName} onChange={e => setDescName(e.target.value)} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary" placeholder="Ej: Hot Sale 2026" />
              </div>
              
              <div className="flex flex-col gap-1">
                <label className="text-xs font-bold text-text-primary/70 uppercase">Porcentaje de Descuento (%)</label>
                <input type="number" min="1" max="99" required value={descPercent} onChange={e => setDescPercent(Number(e.target.value))} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary" />
              </div>

              <div className="flex flex-col gap-1">
                <label className="text-xs font-bold text-text-primary/70 uppercase">Fecha de Inicio</label>
                <input type="datetime-local" required value={descStartDate} onChange={e => setDescStartDate(e.target.value)} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary" />
              </div>

              <div className="flex flex-col gap-1">
                <label className="text-xs font-bold text-text-primary/70 uppercase">Fecha de Fin</label>
                <input type="datetime-local" required value={descEndDate} onChange={e => setDescEndDate(e.target.value)} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary" />
              </div>

              <div className="flex flex-col gap-1 md:col-span-2">
                <label className="text-xs font-bold text-text-primary/70 uppercase">Categoría Objetivo (Se aplica a todos sus productos)</label>
                <select required value={descTargetCategory} onChange={e => setDescTargetCategory(e.target.value)} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary">
                  <option value="" disabled>Selecciona la categoría a descontar...</option>
                  {categories.map((cat: any) => (
                    <option key={cat.categoryId || cat.id} value={cat.categoryId || cat.id}>{cat.name}</option>
                  ))}
                </select>
              </div>

              <div className="flex items-center gap-3 md:col-span-2 mt-2 bg-brand-primary/5 p-4 rounded-lg border border-brand-primary/10">
                <input type="checkbox" id="stackable" checked={descStackable} onChange={e => setDescStackable(e.target.checked)} className="w-5 h-5 accent-brand-primary rounded" />
                <label htmlFor="stackable" className="text-sm font-bold text-brand-primary cursor-pointer select-none">
                  Descuento Apilable (Permitir que el cliente use cupones extra sobre este descuento)
                </label>
              </div>
            </div>

            <button type="submit" className="mt-4 w-full py-4 bg-brand-primary text-bg-primary font-black text-lg rounded-xl hover:opacity-90 transition-all shadow-xl shadow-brand-primary/20">
              Crear Regla de Descuento
            </button>
          </form>
        </div>
      );
    }

    return (
      <div className="w-full flex flex-col gap-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-black text-brand-primary tracking-tight">Gestión de Descuentos</h1>
            <p className="text-text-primary/70">Visualiza y configura reducciones de precio programadas.</p>
          </div>
          <button onClick={() => setIsCreatingDiscount(true)} className="bg-brand-primary text-bg-primary px-6 py-3 rounded-lg font-bold hover:opacity-90 transition-opacity flex items-center gap-2 shadow-sm">
            + Nuevo Descuento
          </button>
        </div>

        {discounts.length === 0 ? (
          <div className="bg-bg-secondary border border-brand-primary/20 rounded-xl p-8 text-center flex flex-col items-center justify-center min-h-[400px]">
            <h3 className="text-xl font-bold text-text-primary mb-2">Aún no hay descuentos</h3>
            <p className="text-text-primary/60 max-w-md">Crea tu primera promoción temporal para impulsar las ventas en una categoría.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {discounts.map((disc, idx) => (
              <div key={idx} className="bg-bg-secondary border border-brand-primary/10 rounded-xl p-5 flex flex-col gap-3 relative overflow-hidden group hover:border-brand-primary/40 transition-colors">
                <div className={`absolute top-0 right-0 text-[10px] font-black px-2 py-1 rounded-bl-lg ${disc.active ? 'bg-green-500 text-white' : 'bg-red-500 text-white'}`}>
                  {disc.active ? 'ACTIVO' : 'INACTIVO'}
                </div>
                <h3 className="font-black text-xl text-brand-primary pr-12 truncate">{disc.name}</h3>
                
                <div className="flex items-center gap-2">
                  <span className="text-3xl font-black text-accent">-{(disc.decimalValue * 100).toFixed(0)}%</span>
                  {disc.stackable && <span className="bg-brand-primary/10 text-brand-primary text-[10px] font-black px-2 py-1 rounded">APILABLE</span>}
                </div>
                
                <div className="flex flex-col gap-1 text-xs text-text-primary/70 bg-bg-primary p-3 rounded-lg border border-brand-primary/5 mt-2">
                  <div className="flex justify-between items-center">
                    <span className="font-bold">Inicia:</span>
                    <span>{new Date(disc.startDate).toLocaleString()}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="font-bold">Termina:</span>
                    <span>{new Date(disc.endDate).toLocaleString()}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    );
  };
  const renderInventario = () => {
    const searchTerm = inventorySearch.toLowerCase().trim();
    let matchingItems: any[] = [];

    if (searchTerm) {
      products.forEach(prod => {
        if (prod.items) {
          prod.items.forEach((item: any) => {
            if (
              item.sku?.toLowerCase().includes(searchTerm) || 
              prod.name?.toLowerCase().includes(searchTerm) || 
              item.id?.toLowerCase().includes(searchTerm)
            ) {
              matchingItems.push({
                ...item,
                productName: prod.name,
                brand: prod.brand
              });
            }
          });
        }
      });
    }

    return (
      <div className="w-full flex flex-col gap-6">
        <div className="flex items-center justify-between border-b border-brand-primary/10 pb-4">
          <div>
            <h1 className="text-3xl font-black text-brand-primary tracking-tight">Ajustes de Inventario Físico</h1>
            <p className="text-text-primary/70">Registra entradas manuales o mermas de stock.</p>
          </div>
        </div>

        {!selectedInventoryItem ? (
          <div className="bg-bg-secondary border border-brand-primary/20 rounded-xl p-6 shadow-sm flex flex-col gap-6">
            <div className="flex flex-col gap-2">
              <label className="text-xs font-bold text-text-primary/70 uppercase">Buscar Variante</label>
              <div className="flex gap-2">
                <input 
                  type="text" 
                  value={inventorySearch} 
                  onChange={e => setInventorySearch(e.target.value)} 
                  className="flex-1 p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary" 
                  placeholder="Escribe el SKU, ID o nombre del producto..." 
                />
              </div>
            </div>

            {searchTerm && matchingItems.length === 0 && (
              <p className="text-center text-text-primary/50 py-4 font-bold">No se encontraron variantes con esa búsqueda.</p>
            )}

            {matchingItems.length > 0 && (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {matchingItems.map((item, idx) => (
                  <div key={idx} onClick={() => setSelectedInventoryItem(item)} className="bg-bg-primary border border-brand-primary/10 rounded-xl p-4 flex flex-col gap-2 hover:border-brand-primary/50 transition-colors shadow-sm cursor-pointer group">
                    <div className="flex justify-between items-start">
                      <div>
                        <h3 className="font-bold text-brand-primary truncate">{item.productName}</h3>
                        <p className="text-xs font-bold text-text-primary/50 uppercase">{item.brand}</p>
                      </div>
                      <span className="bg-brand-primary/10 text-brand-primary px-2 py-1 rounded text-xs font-mono font-bold">SKU: {item.sku}</span>
                    </div>
                    <div className="flex flex-wrap gap-2 mt-2">
                      {item.attributes && Object.entries(item.attributes).map(([k, v]) => (
                        <span key={k} className="text-[10px] bg-bg-secondary border border-brand-primary/10 px-2 py-0.5 rounded text-text-primary/70">{k}: {String(v)}</span>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            )}
            
            {!searchTerm && (
               <div className="text-center text-text-primary/40 py-12 border-dashed border-2 border-brand-primary/10 rounded-xl">
                 <p className="font-bold text-lg">Usa el buscador de arriba</p>
                 <p className="text-sm">Encuentra la variante exacta a la que quieres sumarle o restarle stock físico.</p>
               </div>
            )}
          </div>
        ) : (
          <div className="bg-bg-secondary border border-brand-primary/20 rounded-xl p-6 shadow-sm flex flex-col gap-6 max-w-2xl mx-auto w-full">
            <div className="flex justify-between items-start border-b border-brand-primary/10 pb-4">
              <div>
                <h2 className="text-xl font-black text-brand-primary">Ajustar Stock</h2>
                <p className="font-mono text-sm text-text-primary/60 mt-1">SKU: {selectedInventoryItem.sku}</p>
              </div>
              <button onClick={() => setSelectedInventoryItem(null)} className="text-sm font-bold text-text-primary/40 hover:text-accent transition-colors hover:bg-bg-primary px-3 py-1 rounded">&times; Cancelar</button>
            </div>

            <div className="bg-bg-primary p-4 rounded-lg border border-brand-primary/10">
              <p className="font-bold text-text-primary">{selectedInventoryItem.productName}</p>
              <div className="flex gap-2 mt-2">
                {selectedInventoryItem.attributes && Object.entries(selectedInventoryItem.attributes).map(([k, v]) => (
                  <span key={k} className="text-[10px] bg-bg-secondary px-2 py-1 rounded text-text-primary/70 font-bold">{k}: {String(v)}</span>
                ))}
              </div>
            </div>

            <form onSubmit={handleAdjustInventory} className="flex flex-col gap-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="flex flex-col gap-1">
                  <label className="text-xs font-bold text-text-primary/70 uppercase">Tipo de Ajuste</label>
                  <select value={stockChangeType} onChange={(e: any) => setStockChangeType(e.target.value)} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary font-bold">
                    <option value="ADD">Ingreso (Surtir +)</option>
                    <option value="REMOVE">Merma / Pérdida (-)</option>
                  </select>
                </div>
                <div className="flex flex-col gap-1">
                  <label className="text-xs font-bold text-text-primary/70 uppercase">Cantidad Física</label>
                  <input type="number" min="1" required value={stockChangeAmount} onChange={e => setStockChangeAmount(Number(e.target.value))} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary text-xl font-black" />
                </div>
              </div>

              <div className="bg-brand-primary/5 p-4 rounded-lg border border-brand-primary/10 mt-2">
                <p className="text-xs text-brand-primary/80 font-bold">Resumen del movimiento contable</p>
                <ul className="text-sm text-text-primary/80 mt-2 flex flex-col gap-1 font-mono">
                  <li>• physicalDelta: <span className="font-black text-text-primary">{stockChangeType === 'ADD' ? '+' : '-'}{stockChangeAmount}</span></li>
                  <li>• availableDelta: <span className="font-black text-text-primary">{stockChangeType === 'ADD' ? '+' : '-'}{stockChangeAmount}</span></li>
                  <li>• reserved/redundancy: <span className="font-black text-text-primary">0</span></li>
                </ul>
              </div>

              <button type="submit" disabled={stockChangeAmount <= 0} className={`mt-4 w-full py-4 font-black text-lg rounded-xl transition-all shadow-xl text-white ${stockChangeType === 'ADD' ? 'bg-green-600 hover:bg-green-500 shadow-green-600/20' : 'bg-red-600 hover:bg-red-500 shadow-red-600/20'} disabled:opacity-50`}>
                {stockChangeType === 'ADD' ? 'Registrar Ingreso a Inventario' : 'Registrar Pérdida de Inventario'}
              </button>
            </form>
          </div>
        )}
      </div>
    );
  };

  const renderProductos = () => {
    if (!isCreating) {
      // Filtrar productos si se seleccionó una categoría (Meilisearch guarda el categoryId)
      const displayedProducts = categoryId 
        ? products.filter(p => p.categoryId === categoryId)
        : products;

      return (
        <div className="w-full flex flex-col gap-6">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-black text-brand-primary tracking-tight">Catálogo de Productos</h1>
              <p className="text-text-primary/70">Gestiona los productos base y sus variantes.</p>
            </div>
            <button 
              onClick={() => setIsCreating(true)}
              className="bg-brand-primary text-bg-primary px-6 py-3 rounded-lg font-bold hover:opacity-90 transition-opacity flex items-center gap-2 shadow-sm"
            >
              + Nuevo Producto
            </button>
          </div>

          {categoryId && (
            <div className="bg-brand-secondary/10 border border-brand-secondary/30 text-brand-secondary px-4 py-3 rounded-lg flex items-center justify-between">
              <span className="font-bold text-sm">Filtrando por categoría ID: <span className="font-mono bg-bg-primary px-2 py-0.5 rounded ml-2 text-xs">{categoryId}</span></span>
              <button onClick={() => setCategoryId('')} className="text-xs hover:underline font-bold">Limpiar Filtro</button>
            </div>
          )}

          {displayedProducts.length === 0 ? (
            <div className="bg-bg-secondary border border-brand-primary/20 rounded-xl p-8 text-center flex flex-col items-center justify-center min-h-[400px]">
              <h3 className="text-xl font-bold text-text-primary mb-2">
                {products.length === 0 ? 'Aún no hay productos indexados' : 'No hay productos en esta categoría'}
              </h3>
              <p className="text-text-primary/60 max-w-md">Si acabas de crear un producto, puede tardar un par de segundos en aparecer mientras Meilisearch lo indexa.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {displayedProducts.map((prod) => (
                <div key={prod.id || prod.productId} className="bg-bg-secondary border border-brand-primary/10 rounded-xl p-4 flex flex-col gap-2 hover:border-brand-primary/30 transition-colors shadow-sm relative group overflow-hidden">
                  <div className="absolute top-0 right-0 bg-brand-primary text-bg-primary text-[10px] font-black px-2 py-1 rounded-bl-lg opacity-0 group-hover:opacity-100 transition-opacity">
                    INDEXADO
                  </div>
                  <h3 className="font-bold text-lg text-text-primary truncate">{prod.name}</h3>
                  <span className="text-xs text-brand-primary font-bold uppercase tracking-wide">{prod.brand}</span>
                  <p className="text-sm text-text-primary/70 line-clamp-2">{prod.description}</p>
                  <div className="mt-2 text-[10px] font-mono bg-bg-primary p-2 rounded text-text-primary/50 break-all border border-brand-primary/5">
                    ID: {prod.id || prod.productId}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      );
    }

    return (
      <div className="w-full max-w-5xl mx-auto flex flex-col gap-6 pb-12">
        <div className="flex items-center justify-between border-b border-brand-primary/10 pb-4">
          <h1 className="text-3xl font-black text-brand-primary tracking-tight">Crear Nuevo Producto</h1>
          <button onClick={() => setIsCreating(false)} className="text-text-primary/60 hover:text-accent font-medium">Cancelar</button>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-8">
          <section className="bg-bg-secondary p-6 rounded-xl border border-brand-primary/20 flex flex-col gap-4 shadow-sm">
            <h2 className="text-xl font-bold text-brand-primary flex items-center gap-2">
              <span className="w-6 h-px bg-brand-primary/30"></span> Información Base
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="flex flex-col gap-1">
                <label className="text-xs font-bold text-text-primary/70 uppercase">Nombre del Producto</label>
                <input type="text" required value={name} onChange={e => setName(e.target.value)} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary" placeholder="Ej: Laptop ThinkPad L14" />
              </div>
              <div className="flex flex-col gap-1">
                <label className="text-xs font-bold text-text-primary/70 uppercase">Marca</label>
                <input type="text" required value={brand} onChange={e => setBrand(e.target.value)} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary" placeholder="Ej: Lenovo" />
              </div>
              <div className="flex flex-col gap-1 md:col-span-2">
                <label className="text-xs font-bold text-text-primary/70 uppercase">Categoría</label>
                <select required value={categoryId} onChange={e => setCategoryId(e.target.value)} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary">
                  <option value="" disabled>Selecciona una categoría...</option>
                  {categories.map((cat: any) => (
                    <option key={cat.categoryId || cat.id} value={cat.categoryId || cat.id}>{cat.name}</option>
                  ))}
                </select>
              </div>
              <div className="flex flex-col gap-1 md:col-span-2">
                <label className="text-xs font-bold text-text-primary/70 uppercase">Descripción</label>
                <textarea required value={description} onChange={e => setDescription(e.target.value)} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary min-h-[100px] resize-y" placeholder="Descripción detallada del producto..."></textarea>
              </div>
            </div>
          </section>

          <section className="bg-bg-secondary p-6 rounded-xl border border-brand-primary/20 flex flex-col gap-4 shadow-sm">
            <h2 className="text-xl font-bold text-brand-primary flex items-center gap-2">
              <span className="w-6 h-px bg-brand-primary/30"></span> Estructura de Atributos
            </h2>
            <div className="flex gap-2">
              <input type="text" value={newAttr} onKeyDown={handleAddAttribute} onChange={e => setNewAttr(e.target.value)} className="flex-1 p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary" placeholder="Nuevo atributo (Ej: Talla, Color)..." />
              <button type="button" onClick={handleAddAttribute} className="bg-brand-secondary text-bg-primary px-6 rounded-lg font-bold hover:opacity-90">Agregar</button>
            </div>
            {attributes.length > 0 && (
              <div className="flex flex-wrap gap-2 mt-2">
                {attributes.map(attr => (
                  <span key={attr} className="bg-brand-primary/10 text-brand-primary px-3 py-1 rounded-full text-sm font-bold flex items-center gap-2 border border-brand-primary/20">
                    {attr}
                    <button type="button" onClick={() => setAttributes(attributes.filter(a => a !== attr))} className="hover:text-accent">&times;</button>
                  </span>
                ))}
              </div>
            )}
          </section>

          <section className="bg-bg-secondary p-6 rounded-xl border border-brand-primary/20 flex flex-col gap-6 shadow-sm">
            <div className="flex items-center justify-between">
              <h2 className="text-xl font-bold text-brand-primary flex items-center gap-2">
                <span className="w-6 h-px bg-brand-primary/30"></span> Variantes (Items)
              </h2>
              <button type="button" onClick={handleAddItem} disabled={attributes.length === 0} className="text-sm bg-accent text-white px-4 py-2 rounded-lg font-bold disabled:opacity-50 hover:opacity-90 transition-opacity">
                + Añadir Variante
              </button>
            </div>

            {items.map((item, idx) => (
              <div key={idx} className="bg-bg-primary p-5 rounded-lg border border-brand-primary/10 flex flex-col gap-4 relative">
                <button type="button" onClick={() => setItems(items.filter((_, i) => i !== idx))} className="absolute top-4 right-4 text-text-primary/40 hover:text-accent font-bold">&times; Eliminar</button>
                
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  {attributes.map(attr => (
                    <div key={attr} className="flex flex-col gap-1">
                      <label className="text-xs font-bold text-brand-secondary uppercase">{attr}</label>
                      <input type="text" required value={item.itemAttributes[attr] || ''} onChange={e => updateItemAttribute(idx, attr, e.target.value)} className="p-2 bg-bg-secondary border border-brand-primary/20 rounded text-sm text-text-primary outline-none focus:border-brand-primary" placeholder="Valor..." />
                    </div>
                  ))}
                </div>

                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 border-t border-brand-primary/10 pt-4">
                  <div className="flex flex-col gap-1">
                    <label className="text-xs font-bold text-text-primary/70 uppercase">Precio Base ($)</label>
                    <input type="number" step="0.01" min="0.01" required value={item.baseSalePrice} onChange={e => updateItem(idx, 'baseSalePrice', parseFloat(e.target.value))} className="p-2 bg-bg-secondary border border-brand-primary/20 rounded text-sm text-text-primary outline-none focus:border-brand-primary" />
                  </div>
                  <div className="flex flex-col gap-1">
                    <label className="text-xs font-bold text-text-primary/70 uppercase">Stock Inicial</label>
                    <input type="number" min="0" required value={item.initialStock} onChange={e => updateItem(idx, 'initialStock', parseInt(e.target.value, 10))} className="p-2 bg-bg-secondary border border-brand-primary/20 rounded text-sm text-text-primary outline-none focus:border-brand-primary" />
                  </div>
                  <div className="flex flex-col gap-1">
                    <label className="text-xs font-bold text-text-primary/70 uppercase">Stock Redundancia</label>
                    <input type="number" min="0" required value={item.redundancyStock} onChange={e => updateItem(idx, 'redundancyStock', parseInt(e.target.value, 10))} className="p-2 bg-bg-secondary border border-brand-primary/20 rounded text-sm text-text-primary outline-none focus:border-brand-primary" />
                  </div>
                  <div className="flex flex-col gap-1">
                    <label className="text-xs font-bold text-text-primary/70 uppercase">Límite Lógico</label>
                    <input type="number" min="1" required value={item.logicalLimit} onChange={e => updateItem(idx, 'logicalLimit', parseInt(e.target.value, 10))} className="p-2 bg-bg-secondary border border-brand-primary/20 rounded text-sm text-text-primary outline-none focus:border-brand-primary" />
                  </div>
                </div>

                <div className="border-t border-brand-primary/10 pt-4 flex flex-col gap-3">
                  <div className="flex items-center justify-between">
                    <label className="text-xs font-bold text-text-primary/70 uppercase">Imágenes de la variante</label>
                    <button type="button" onClick={() => addImageField(idx)} className="text-xs text-brand-primary font-bold hover:underline">+ Añadir Otra Imagen</button>
                  </div>
                  
                  <div className="grid grid-cols-1 gap-3">
                    {item.images.map((img, imgIdx) => (
                      <div key={imgIdx} className="flex items-start gap-4 p-3 bg-bg-secondary border border-brand-primary/10 rounded-lg">
                        <div className="w-20 h-20 shrink-0 bg-bg-primary rounded border border-brand-primary/20 flex items-center justify-center overflow-hidden">
                          {img ? (
                            <img src={img} alt="Preview" className="w-full h-full object-cover" onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />
                          ) : (
                            <span className="text-xs text-text-primary/30">Preview</span>
                          )}
                        </div>
                        <div className="flex-1 flex flex-col gap-2">
                          <input type="url" required value={img} onChange={e => updateItemImage(idx, imgIdx, e.target.value)} className="p-2 bg-bg-primary border border-brand-primary/20 rounded text-sm text-text-primary outline-none focus:border-brand-primary w-full" placeholder="https://ejemplo.com/imagen.jpg" />
                          {item.images.length > 1 && (
                            <button type="button" onClick={() => removeImageField(idx, imgIdx)} className="text-xs text-accent font-bold self-start hover:underline">
                              Quitar Imagen
                            </button>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            ))}
            {items.length === 0 && <p className="text-sm text-text-primary/50 text-center py-4">Define primero los atributos arriba y luego añade variantes.</p>}
          </section>

          <button type="submit" disabled={items.length === 0} className="w-full py-4 bg-brand-primary text-bg-primary font-black text-lg rounded-xl hover:opacity-90 transition-all disabled:opacity-50 shadow-xl shadow-brand-primary/20">
            Guardar Producto en Base de Datos
          </button>
        </form>
      </div>
    );
  };

  const navClasses = (tabName: string) => 
    `w-full text-left px-4 py-3 rounded-lg font-bold transition-all ${activeTab === tabName ? 'bg-brand-primary text-bg-primary shadow-md' : 'text-text-primary/70 hover:bg-bg-primary hover:text-brand-primary'}`;

  return (
    <div className="flex h-[calc(100vh-10rem)] gap-6">
      <aside className="w-64 shrink-0 flex flex-col gap-2 bg-bg-secondary p-4 rounded-xl border border-brand-primary/20">
        <h2 className="text-sm font-black text-text-primary/40 uppercase tracking-widest mb-2 px-2">Operaciones</h2>
        <button onClick={() => setActiveTab('productos')} className={navClasses('productos')}>
          Productos
        </button>
        <button onClick={() => setActiveTab('categorias')} className={navClasses('categorias')}>
          Categorías
        </button>
        <button onClick={() => setActiveTab('descuentos')} className={navClasses('descuentos')}>
          Descuentos
        </button>
        <button onClick={() => setActiveTab('inventario')} className={navClasses('inventario')}>
          Inventario
        </button>
      </aside>

      <main className="flex-1 bg-bg-primary rounded-xl overflow-y-auto pr-2">
        {activeTab === 'productos' && renderProductos()}
        {activeTab === 'categorias' && renderCategorias()}
        {activeTab === 'descuentos' && renderDescuentos()}
        {activeTab === 'inventario' && renderInventario()}
      </main>
    </div>
  );
}