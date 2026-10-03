import { productApi } from "../api/api";
import dummyProducts from "../data/products";
import { extractErrorMessage } from "./authService";

/**
 * Product-service (:8050) storefront wrapper.
 * Backend ProductResponse: { id, name, description, availableQuantity, price,
 *   categoryId, categoryName, categoryDescription } — list endpoints return a
 *   bare PageResponse { content, page, size, totalElements, ... } (no ApiResponse wrapper).
 * Single-product endpoints return ApiResponse { data }.
 * Mapper below converts to the UI shape the cards expect.
 */

const PLACEHOLDER_IMAGES = [
  "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400&q=80",
  "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400&q=80",
  "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=400&q=80",
  "https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=400&q=80",
  "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=400&q=80",
  "https://images.unsplash.com/photo-1585386959984-a4155224a1ad?w=400&q=80",
];

function placeholderFor(id) {
  const n = Number(id);
  return PLACEHOLDER_IMAGES[Number.isFinite(n) ? n % PLACEHOLDER_IMAGES.length : 0];
}

/** Backend ProductResponse -> UI product card shape. */
export function mapProduct(p) {
  if (!p) return null;
  const price = Number(p.price ?? 0);
  return {
    id: p.id,
    productId: p.id,
    name: p.name,
    description: p.description,
    price,
    mrp: price, // backend has no MRP/discount yet — no strikethrough rendered
    discount: 0,
    brand: p.categoryName || "",
    category: p.categoryName || "",
    categoryId: p.categoryId,
    image: p.image || placeholderFor(p.id),
    rating: null,
    ratingCount: 0,
    availableQuantity: p.availableQuantity ?? 0,
    inStock: (p.availableQuantity ?? 0) > 0,
  };
}

function pageContent(res) {
  const body = res.data;
  const list = Array.isArray(body?.content) ? body.content : body?.data?.content || [];
  return { items: list.map(mapProduct).filter(Boolean), total: body?.totalElements ?? list.length };
}

export const fetchProducts = async (page = 0, size = 24) => {
  try {
    const res = await productApi.get("/api/v1/products", { params: { page, size } });
    return pageContent(res).items;
  } catch {
    return dummyProducts; // backend down — demo fallback (checkout will validate server-side)
  }
};

export const fetchProductsPage = async (page = 0, size = 24) => {
  const res = await productApi.get("/api/v1/products", { params: { page, size } });
  return pageContent(res);
};

export const fetchProductById = async (id) => {
  try {
    const res = await productApi.get(`/api/v1/products/${id}`);
    const mapped = mapProduct(res.data?.data);
    if (mapped) return mapped;
    throw new Error("not found");
  } catch (err) {
    const fallback = dummyProducts.find((p) => String(p.id) === String(id)) || null;
    if (fallback) return fallback;
    throw err;
  }
};

export const searchProducts = async (keyword, page = 0, size = 24) => {
  const res = await productApi.get("/api/v1/products/search", {
    params: { keyword, page, size },
  });
  return pageContent(res).items;
};

export const fetchCategories = async () => {
  try {
    const res = await productApi.get("/api/v1/categories");
    const body = res.data;
    return body?.data ?? body ?? [];
  } catch (err) {
    throw new Error(extractErrorMessage(err, "Could not load categories."));
  }
};
