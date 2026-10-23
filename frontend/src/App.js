import React from "react";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import Navbar from "./components/Navbar";
import Footer from "./components/Footer";
import Guard from "./components/Guard";
import PortalShell from "./components/PortalShell";

import Landing from "./pages/Landing";
import LearnMore from "./pages/LearnMore";
import ChooseRole from "./pages/ChooseRole";
import Auth from "./pages/Auth";
import Market from "./pages/Market";
import Checkout from "./pages/Checkout";
import BuyerOrders from "./pages/BuyerOrders";
import NotFound from "./pages/NotFound";

import SellerOverview from "./pages/seller/Overview";
import SellerProducts from "./pages/seller/Products";
import SellerOrders from "./pages/seller/Orders";
import SellerCustomers from "./pages/seller/Customers";
import SellerProfile from "./pages/seller/Profile";

import LearnerHome from "./pages/learner/LearnerHome";
import Onboard from "./pages/learner/Onboard";
import CropGuidePage from "./pages/learner/CropGuidePage";
import Profit from "./pages/learner/Profit";
import FarmData from "./pages/learner/FarmData";

import AdminOverview from "./pages/admin/Overview";
import AdminActivity from "./pages/admin/Activity";
import AdminUsers from "./pages/admin/Users";
import AdminListings from "./pages/admin/Listings";
import AdminCropImages from "./pages/admin/CropImages";
import AdminGuides from "./pages/admin/Guides";
import AdminOrders from "./pages/admin/Orders";

const SELLER_NAV = [
  { to: "/seller", label: "Overview", end: true },
  { to: "/seller/products", label: "My products" },
  { to: "/seller/orders", label: "Orders" },
  { to: "/seller/customers", label: "Customers" },
  { to: "/seller/profile", label: "Profile & payout" },
];
const ADMIN_NAV = [
  { to: "/admin", label: "Overview", end: true },
  { to: "/admin/activity", label: "Login activity" },
  { to: "/admin/users", label: "Users" },
  { to: "/admin/listings", label: "Listings" },
  { to: "/admin/crop-images", label: "Crop images" },
  { to: "/admin/guides", label: "Crop guides" },
  { to: "/admin/orders", label: "Orders" },
];

export default function App() {
  const { pathname } = useLocation();
  const showFooter = !/^\/(seller|admin|signin\/)/.test(pathname);

  return (
    <>
      <Navbar />
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/learn-more" element={<LearnMore />} />
        <Route path="/signin" element={<ChooseRole />} />
        <Route path="/signin/:role" element={<Auth />} />
        <Route path="/market" element={<Market />} />

        <Route path="/checkout/:orderId" element={<Guard roles={["BUYER"]}><Checkout /></Guard>} />
        <Route path="/buyer/orders" element={<Guard roles={["BUYER"]}><BuyerOrders /></Guard>} />

        <Route path="/learn" element={<Guard roles={["FARMER"]}><LearnerHome /></Guard>} />
        <Route path="/learn/start" element={<Guard roles={["FARMER"]}><Onboard /></Guard>} />
        <Route path="/learn/crop/:id" element={<Guard roles={["FARMER"]}><CropGuidePage /></Guard>} />
        <Route path="/learn/profit/:id" element={<Guard roles={["FARMER"]}><Profit /></Guard>} />
        <Route path="/learn/farm-data" element={<Guard roles={["FARMER"]}><FarmData /></Guard>} />

        <Route path="/seller" element={<Guard roles={["SELLER"]}><PortalShell role="SELLER" title="Seller workspace" subtitle="Your products, orders and customers." items={SELLER_NAV} /></Guard>}>
          <Route index element={<SellerOverview />} />
          <Route path="products" element={<SellerProducts />} />
          <Route path="orders" element={<SellerOrders />} />
          <Route path="customers" element={<SellerCustomers />} />
          <Route path="profile" element={<SellerProfile />} />
        </Route>

        <Route path="/admin" element={<Guard roles={["ADMIN"]}><PortalShell role="ADMIN" title="Admin console" subtitle="People, products and activity across the site." items={ADMIN_NAV} /></Guard>}>
          <Route index element={<AdminOverview />} />
          <Route path="activity" element={<AdminActivity />} />
          <Route path="users" element={<AdminUsers />} />
          <Route path="listings" element={<AdminListings />} />
          <Route path="crop-images" element={<AdminCropImages />} />
          <Route path="guides" element={<AdminGuides />} />
          <Route path="orders" element={<AdminOrders />} />
        </Route>

        <Route path="/home" element={<Navigate to="/" replace />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
      {showFooter && <Footer />}
    </>
  );
}
