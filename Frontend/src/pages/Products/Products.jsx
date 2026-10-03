import { useEffect, useState, useMemo } from "react";
import { useSearchParams } from "react-router-dom";

import ProductCard from "../../components/ProductCard/ProductCard";
import Loader from "../../components/Loader/Loader";
import EmptyState from "../../components/EmptyState/EmptyState";
import { fetchProducts, searchProducts } from "../../services/productService";
import "./Products.css";

const SORT_OPTIONS = [
  { value: "default",   label: "Relevance" },
  { value: "price_asc", label: "Price: Low to High" },
  { value: "price_desc",label: "Price: High to Low" },
  { value: "discount",  label: "Best Discount" },
];

function Products() {
  const [products, setProducts]     = useState([]);
  const [loading, setLoading]       = useState(true);
  const [sort, setSort]             = useState("default");
  const [maxPrice, setMaxPrice]     = useState("");
  const [searchParams]              = useSearchParams();
  const query                       = searchParams.get("q") || "";

  useEffect(() => {
    let mounted = true;
    const loader = query ? searchProducts(query).catch(() => fetchProducts()) : fetchProducts();
    loader
      .then((list) => {
        if (mounted) setProducts(list);
      })
      .catch(console.error)
      .finally(() => {
        if (mounted) setLoading(false);
      });
    return () => {
      mounted = false;
    };
  }, [query]);

  const filtered = useMemo(() => {
    let result = [...products];

    if (query) {
      const q = query.toLowerCase();
      result = result.filter(
        (p) =>
          p.name?.toLowerCase().includes(q) ||
          p.brand?.toLowerCase().includes(q) ||
          p.category?.toLowerCase().includes(q)
      );
    }

    if (maxPrice !== "") {
      result = result.filter((p) => p.price <= Number(maxPrice));
    }

    if (sort === "price_asc")  result.sort((a, b) => a.price - b.price);
    if (sort === "price_desc") result.sort((a, b) => b.price - a.price);
    if (sort === "discount")   result.sort((a, b) => (b.discount || 0) - (a.discount || 0));

    return result;
  }, [products, query, maxPrice, sort]);

  if (loading) return <Loader />;

  return (
    <div className="products-page container">

      {/* Sidebar */}
      <aside className="products-sidebar">
        <h3>Filters</h3>

        <div className="filter-group">
          <label className="filter-label">Sort By</label>
          {SORT_OPTIONS.map((opt) => (
            <label key={opt.value} className="filter-radio">
              <input
                type="radio"
                name="sort"
                value={opt.value}
                checked={sort === opt.value}
                onChange={() => setSort(opt.value)}
              />
              {opt.label}
            </label>
          ))}
        </div>

        <div className="filter-group">
          <label className="filter-label">Max Price (₹)</label>
          <input
            className="filter-price-input"
            type="number"
            placeholder="e.g. 5000"
            value={maxPrice}
            onChange={(e) => setMaxPrice(e.target.value)}
            min={0}
          />
          {maxPrice && (
            <button className="filter-clear" onClick={() => setMaxPrice("")}>
              Clear
            </button>
          )}
        </div>
      </aside>

      {/* Main */}
      <main className="products-main">

        <div className="products-header">
          <h2>
            {query ? `Results for "${query}"` : "All Products"}
            <span className="products-count"> ({filtered.length})</span>
          </h2>
        </div>

        {filtered.length === 0 ? (
          <EmptyState message={`No products found${query ? ` for "${query}"` : ""}`} />
        ) : (
          <div className="products-grid">
            {filtered.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        )}

      </main>

    </div>
  );
}

export default Products;
