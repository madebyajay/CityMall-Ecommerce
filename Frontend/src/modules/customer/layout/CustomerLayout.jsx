import { Outlet } from "react-router-dom";
import Navbar from "../../../components/layout/Navbar/Navbar";
import Footer from "../../../components/layout/Footer/Footer";

/**
 * Customer-only shell: header + footer + cart drawer context.
 * Admin screens must never render inside this layout (and vice versa).
 */
function CustomerLayout() {
  return (
    <>
      <Navbar />
      <Outlet />
      <Footer />
    </>
  );
}

export default CustomerLayout;
