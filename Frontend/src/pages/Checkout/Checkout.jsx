import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useCart } from "../../context/CartContext";
import { useAuth } from "../../context/AuthContext";
import { orderApiService, ORDER_PAYMENT_METHODS } from "../../services/orderService";
import "./Checkout.css";

const DELIVERY_THRESHOLD = 500;
const DELIVERY_CHARGE    = 49;

const STEPS = ["Delivery Address", "Payment"];

function Checkout() {
  const { cartItems, clearCart } = useCart();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [step, setStep]     = useState(0);
  const [address, setAddress] = useState({ name: "", phone: "", pincode: "", street: "", city: "", state: "" });
  const [payment, setPayment] = useState("UPI");
  const [addrErrors, setAddrErrors] = useState({});
  const [placing, setPlacing] = useState(false);
  const [orderError, setOrderError] = useState("");

  const priceTotal    = cartItems.reduce((s, i) => s + i.price * i.quantity, 0);
  const mrpTotal      = cartItems.reduce((s, i) => s + (i.mrp || i.price) * i.quantity, 0);
  const totalDiscount = mrpTotal - priceTotal;
  const delivery      = priceTotal >= DELIVERY_THRESHOLD ? 0 : DELIVERY_CHARGE;
  const grandTotal    = priceTotal + delivery;
  const totalItems    = cartItems.reduce((s, i) => s + i.quantity, 0);

  if (cartItems.length === 0) {
    navigate("/cart");
    return null;
  }

  const validateAddress = () => {
    const e = {};
    if (!address.name.trim())    e.name    = "Required";
    if (!/^\d{10}$/.test(address.phone)) e.phone = "Enter valid 10-digit number";
    if (!/^\d{6}$/.test(address.pincode)) e.pincode = "Enter valid 6-digit pincode";
    if (!address.street.trim())  e.street  = "Required";
    if (!address.city.trim())    e.city    = "Required";
    if (!address.state.trim())   e.state   = "Required";
    return e;
  };

  const handleAddressNext = (e) => {
    e.preventDefault();
    const e2 = validateAddress();
    if (Object.keys(e2).length > 0) { setAddrErrors(e2); return; }
    setStep(1);
  };

  const handlePlaceOrder = async () => {
    if (placing) return;
    setOrderError("");
    if (!user?.userId) {
      setOrderError("Please sign in again to place the order.");
      return;
    }
    setPlacing(true);
    try {
      const order = await orderApiService.createOrder({
        customerId: user.userId,
        paymentMethod: payment,
        products: cartItems,
      });
      const totalItems = cartItems.reduce((s, i) => s + i.quantity, 0);
      clearCart();
      navigate("/order-confirmation", {
        state: {
          orderNumber: order?.orderNumber,
          status: order?.status,
          address,
          payment,
          grandTotal: Number(order?.amount ?? grandTotal),
          totalItems,
        },
      });
    } catch (err) {
      setOrderError(err.message || "Order could not be placed.");
    } finally {
      setPlacing(false);
    }
  };

  const handleAddressChange = (field, value) => {
    setAddress({ ...address, [field]: value });
    setAddrErrors({ ...addrErrors, [field]: "" });
  };

  return (
    <div className="container checkout-wrapper">

      {/* Left */}
      <div className="checkout-main">

        {/* Stepper */}
        <div className="checkout-stepper">
          {STEPS.map((s, i) => (
            <div key={s} className={`checkout-step ${i === step ? "active" : ""} ${i < step ? "done" : ""}`}>
              <span className="step-num">{i < step ? "✓" : i + 1}</span>
              <span>{s}</span>
            </div>
          ))}
        </div>

        {/* Step 0 — Address */}
        {step === 0 && (
          <form className="checkout-section" onSubmit={handleAddressNext}>
            <h3 className="checkout-section-title">Delivery Address</h3>

            <div className="checkout-form-grid">
              {[
                { field: "name",    label: "Full Name",    type: "text",   placeholder: "John Doe" },
                { field: "phone",   label: "Mobile Number",type: "tel",    placeholder: "10-digit number" },
                { field: "pincode", label: "Pincode",      type: "text",   placeholder: "6-digit pincode" },
                { field: "city",    label: "City",         type: "text",   placeholder: "City" },
                { field: "state",   label: "State",        type: "text",   placeholder: "State" },
              ].map(({ field, label, type, placeholder }) => (
                <div className="checkout-field" key={field}>
                  <label>{label}</label>
                  <input
                    type={type}
                    placeholder={placeholder}
                    value={address[field]}
                    onChange={(e) => handleAddressChange(field, e.target.value)}
                  />
                  {addrErrors[field] && <span className="checkout-field-error">{addrErrors[field]}</span>}
                </div>
              ))}

              <div className="checkout-field checkout-field-full">
                <label>Street Address</label>
                <input
                  type="text"
                  placeholder="House no., Building, Street, Area"
                  value={address.street}
                  onChange={(e) => handleAddressChange("street", e.target.value)}
                />
                {addrErrors.street && <span className="checkout-field-error">{addrErrors.street}</span>}
              </div>
            </div>

            <button type="submit" className="checkout-next-btn">
              Continue to Payment →
            </button>
          </form>
        )}

        {/* Step 1 — Payment */}
        {step === 1 && (
          <div className="checkout-section">
            <h3 className="checkout-section-title">Payment Method</h3>

            <div className="payment-options">
              {ORDER_PAYMENT_METHODS.map((opt) => (
                <label key={opt.value} className={`payment-option ${payment === opt.value ? "selected" : ""}`}>
                  <input
                    type="radio"
                    name="payment"
                    value={opt.value}
                    checked={payment === opt.value}
                    onChange={() => setPayment(opt.value)}
                  />
                  <span className="payment-icon">{opt.icon}</span>
                  <span>{opt.label}</span>
                </label>
              ))}
            </div>

            {/* Delivery address recap */}
            <div className="checkout-addr-recap">
              <p className="recap-label">Delivering to</p>
              <p className="recap-name">{address.name} · {address.phone}</p>
              <p className="recap-addr">{address.street}, {address.city}, {address.state} — {address.pincode}</p>
              <button className="recap-change" onClick={() => setStep(0)}>Change</button>
            </div>

            <div className="checkout-payment-actions">
              <button className="checkout-back-btn" onClick={() => setStep(0)}>← Back</button>
              <button className="checkout-place-btn" onClick={handlePlaceOrder} disabled={placing}>
                {placing ? "Placing order..." : `Place Order · ₹${grandTotal.toLocaleString()}`}
              </button>
            </div>
            {orderError && <p className="checkout-field-error">{orderError}</p>}
          </div>
        )}
      </div>

      {/* Right — Summary */}
      <aside className="checkout-summary">
        <h3 className="checkout-summary-title">Order Summary</h3>
        <div className="checkout-summary-items">
          {cartItems.map((item) => (
            <div key={item.id} className="checkout-summary-item">
              <img src={item.image} alt={item.name} />
              <div>
                <p>{item.name}</p>
                <span>Qty: {item.quantity}</span>
              </div>
              <span>₹{(item.price * item.quantity).toLocaleString()}</span>
            </div>
          ))}
        </div>
        <hr className="checkout-divider" />
        <div className="checkout-summary-row"><span>Price ({totalItems} items)</span><span>₹{mrpTotal.toLocaleString()}</span></div>
        <div className="checkout-summary-row green"><span>Discount</span><span>− ₹{totalDiscount.toLocaleString()}</span></div>
        <div className="checkout-summary-row"><span>Delivery</span><span>{delivery === 0 ? <b style={{color:"#2e7d32"}}>FREE</b> : `₹${delivery}`}</span></div>
        <hr className="checkout-divider" />
        <div className="checkout-summary-row total"><span>Total</span><span>₹{grandTotal.toLocaleString()}</span></div>
      </aside>

    </div>
  );
}

export default Checkout;
