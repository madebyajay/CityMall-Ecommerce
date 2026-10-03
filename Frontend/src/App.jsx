import { Suspense, lazy } from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";

import CustomerLayout from "./modules/customer/layout/CustomerLayout";
import RequireAuth from "./shared/routes/RequireAuth";
import Forbidden from "./pages/Forbidden";
import Home from "./pages/Home/Home";
import Products from "./pages/Products/Products";
import Cart from "./pages/Cart/Cart";
import ProductDetails from "./pages/ProductDetails/ProductDetails";
import Login from "./pages/Auth/Login";
import Register from "./pages/Auth/Register";
import ForgotPassword from "./pages/Auth/ForgotPassword";
import Profile from "./pages/Profile/Profile";
import Checkout from "./pages/Checkout/Checkout";
import OrderConfirmation from "./pages/Checkout/OrderConfirmation";
import Orders from "./pages/Orders/Orders";

import "./styles/global.css";

// Lazy-load the whole admin tree as one chunk: normal customers never download it.
const AdminApp = lazy(() => import("./modules/admin/AdminRoutes"));

function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={<div style={{ padding: 24 }}>Loading...</div>}>
        <Routes>
          {/* Customer tree — always inside CustomerLayout, never AdminLayout */}
          <Route element={<CustomerLayout />}>
            <Route path="/" element={<Home />} />
            <Route path="/products" element={<Products />} />
            <Route path="/product/:id" element={<ProductDetails />} />
            <Route path="/cart" element={<Cart />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/profile" element={<RequireAuth><Profile /></RequireAuth>} />
            <Route path="/checkout" element={<RequireAuth><Checkout /></RequireAuth>} />
            <Route path="/order-confirmation" element={<RequireAuth><OrderConfirmation /></RequireAuth>} />
            <Route path="/orders" element={<RequireAuth><Orders /></RequireAuth>} />
            <Route path="/403" element={<Forbidden />} />
          </Route>

          {/* Admin tree — guarded by RequireAdmin/RequireSuperAdmin inside, rendered in AdminLayout */}
          <Route path="/admin/*" element={<AdminApp />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}

export default App;
