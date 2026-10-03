import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import Loader from "../../components/Loader/Loader";
import StarRating from "../../components/product/StarRating";
import { fetchProductById } from "../../services/productService";
import { useCart } from "../../context/CartContext";
import "./ProductDetails.css";

function ProductDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToCart } = useCart();
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [added, setAdded] = useState(false);

  useEffect(() => {
    let mounted = true;
    fetchProductById(id)
      .then((p) => {
        if (mounted) {
          setProduct(p);
          setAdded(false);
          setError(null);
        }
      })
      .catch(() => {
        if (mounted) setError("Product not found.");
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });
    return () => {
      mounted = false;
    };
  }, [id]);

  const handleAddToCart = () => {
    addToCart(product);
    setAdded(true);
  };

  const handleBuyNow = () => {
    addToCart(product);
    navigate("/cart");
  };

  if (loading) return <Loader />;

  if (error || !product) {
    return <div className="container"><p>{error || "Product not found."}</p></div>;
  }

  const savings = product.mrp ? product.mrp - product.price : null;

  return (
    <div className="container pd-wrapper">

      {/* Left — Image */}
      <div className="pd-image-section">
        <img src={product.image} alt={product.name} className="pd-image" />
      </div>

      {/* Right — Info */}
      <div className="pd-info-section">

        {product.brand && <p className="pd-brand">{product.brand}</p>}
        <h1 className="pd-name">{product.name}</h1>

        {product.rating && (
          <div className="pd-rating">
            <StarRating rating={product.rating} count={product.ratingCount} />
          </div>
        )}

        <hr className="pd-divider" />

        {/* Price */}
        <div className="pd-price-block">
          <span className="pd-price">₹{product.price.toLocaleString()}</span>
          {product.mrp && (
            <>
              <span className="pd-mrp">M.R.P: ₹{product.mrp.toLocaleString()}</span>
              {product.discount && (
                <span className="pd-discount">({product.discount}% off)</span>
              )}
            </>
          )}
        </div>
        {savings && (
          <p className="pd-savings">You save: ₹{savings.toLocaleString()}</p>
        )}

        <hr className="pd-divider" />

        {/* Stock */}
        {typeof product.availableQuantity === "number" && (
          <p className="pd-stock" style={{ color: product.availableQuantity > 0 ? "#2e7d32" : "#c62828" }}>
            {product.availableQuantity > 0
              ? product.availableQuantity < 10
                ? `Only ${product.availableQuantity} left in stock`
                : "In stock"
              : "Out of stock"}
          </p>
        )}

        {/* Description */}
        <p className="pd-description">{product.description}</p>

        {/* Actions */}
        <div className="pd-actions">
          <button
            className={`pd-btn pd-btn-cart ${added ? "pd-btn-added" : ""}`}
            onClick={handleAddToCart}
            disabled={added || product.availableQuantity === 0}
          >
            {added ? "✓ Added to Cart" : "Add to Cart"}
          </button>
          <button className="pd-btn pd-btn-buy" onClick={handleBuyNow}>
            Buy Now
          </button>
        </div>

      </div>
    </div>
  );
}

export default ProductDetails;
