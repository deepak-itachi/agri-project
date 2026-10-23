import axios from "axios";

// Backend address. For a deployed site set REACT_APP_API_BASE at build time.
export const API_BASE = process.env.REACT_APP_API_BASE || "http://localhost:8080";

export const api = axios.create({ baseURL: API_BASE });

let token = null;
let onUnauthorized = null;
export const setToken = (t) => { token = t; };
export const setUnauthorizedHandler = (fn) => { onUnauthorized = fn; };

api.interceptors.request.use((cfg) => {
  if (token) cfg.headers.Authorization = `Bearer ${token}`;
  return cfg;
});
api.interceptors.response.use(
  (r) => r,
  (err) => {
    const url = err.config?.url || "";
    if (err.response?.status === 401 && token && onUnauthorized && !url.includes("/api/auth/login")) onUnauthorized();
    return Promise.reject(err);
  }
);

/** Readable message from any failed request. */
export function errorMessage(err, fallback) {
  const fromServer = err?.response?.data?.message;
  if (fromServer) return fromServer;
  if (err?.code === "ERR_NETWORK") return "Can't reach the server. Check that the backend is running on port 8080.";
  return fallback || err?.message || "Something went wrong.";
}

/** Seller/admin uploads live on the backend (/uploads/...); bundled photos (/images/...) live in this app. */
export function assetUrl(u) {
  if (!u) return "";
  if (/^https?:/i.test(u)) return u;
  if (u.startsWith("/uploads/")) return API_BASE + u;
  return u;
}

const data = (p) => p.then((r) => r.data);

// ---- auth ----
export const authApi = {
  login: (body) => data(api.post("/api/auth/login", body)),
  register: (role, body) => data(api.post(`/api/auth/register/${role}`, body)),
  me: () => data(api.get("/api/auth/me")),
  updateMe: (body) => data(api.put("/api/auth/me", body)),
  logout: () => data(api.post("/api/auth/logout")),
};

// ---- public marketplace ----
export const marketApi = {
  listings: (q) => data(api.get("/api/market/listings", { params: q ? { q } : {} })),
  mandi: (state, commodity) => data(api.get("/api/market/mandi-prices", { params: { state, commodity } })),
};
export const cropImageApi = {
  all: () => data(api.get("/api/crop-images")),
  byName: (name) => data(api.get(`/api/crop-images/${encodeURIComponent(name)}`)),
};

// ---- buyer ----
export const buyerApi = {
  createOrder: (listingId, quantityKg) => data(api.post("/api/orders", { listingId, quantityKg })),
  orders: () => data(api.get("/api/buyer/orders")),
  cancel: (id) => data(api.post(`/api/buyer/orders/${id}/cancel`)),
  paymentConfig: () => data(api.get("/api/payments/config")),
  startPayment: (orderId) => data(api.post(`/api/payments/create/${orderId}`)),
  verify: (body) => data(api.post("/api/payments/verify", body)),
  demoPay: (orderId, method) => data(api.post("/api/payments/demo-confirm", { orderId, method })),
};

// ---- seller ----
export const sellerApi = {
  dashboard: () => data(api.get("/api/seller/dashboard")),
  orders: () => data(api.get("/api/seller/orders")),
  setOrderStatus: (id, status) => data(api.put(`/api/seller/orders/${id}/status`, null, { params: { status } })),
  listings: () => data(api.get("/api/seller/listings")),
  saveListing: (id, form) => data(api.post(id ? `/api/seller/listings/${id}` : "/api/seller/listings", form)),
  deleteListing: (id) => data(api.delete(`/api/seller/listings/${id}`)),
};

// ---- learner ----
export const learnerApi = {
  onboard: (body) => data(api.post("/api/farmers/onboard", body)),
  farmer: (id) => data(api.get(`/api/farmers/${id}`)),
  recommend: (id) => data(api.get(`/api/crops/recommend/${id}`)),
  guide: (id) => data(api.get(`/api/crops/${id}/guide`)),
  allGuides: () => data(api.get("/api/crops")),
  profit: (body) => data(api.post("/api/profit/estimate", body)),
  weather: (location) => data(api.get("/api/weather/current", { params: { location } })),
  uploadSoil: (form) => data(api.post("/api/farm-data/soil-report", form)),
  soilReports: (id) => data(api.get(`/api/farm-data/soil-report/farmer/${id}`)),
  saveWater: (body) => data(api.post("/api/farm-data/water-resource", body)),
  waterResources: (id) => data(api.get(`/api/farm-data/water-resource/farmer/${id}`)),
  uploadLand: (form) => data(api.post("/api/farm-data/land-photo", form)),
  landPhotos: (id) => data(api.get(`/api/farm-data/land-photo/farmer/${id}`)),
};

// ---- admin ----
export const adminApi = {
  stats: () => data(api.get("/api/admin/stats")),
  users: (role) => data(api.get("/api/admin/users", { params: role && role !== "ALL" ? { role } : {} })),
  updateUser: (id, body) => data(api.put(`/api/admin/users/${id}`, body)),
  resetPassword: (id, newPassword) => data(api.post(`/api/admin/users/${id}/reset-password`, { newPassword })),
  loginEvents: (role) => data(api.get("/api/admin/login-events", { params: role && role !== "ALL" ? { role } : {} })),
  listings: () => data(api.get("/api/admin/listings")),
  editListing: (id, body) => data(api.put(`/api/admin/listings/${id}`, body)),
  replaceListingImage: (id, form) => data(api.post(`/api/admin/listings/${id}/image`, form)),
  hideListing: (id) => data(api.delete(`/api/admin/listings/${id}`)),
  orders: () => data(api.get("/api/admin/orders")),
  cropImages: () => data(api.get("/api/admin/crop-images")),
  saveCropImage: (id, form) => data(api.post(id ? `/api/admin/crop-images/${id}` : "/api/admin/crop-images", form)),
  deleteCropImage: (id) => data(api.delete(`/api/admin/crop-images/${id}`)),
  updateGuide: (id, body) => data(api.put(`/api/admin/crop-guides/${id}`, body)),
  createGuide: (body) => data(api.post("/api/crops", body)),
  deleteGuide: (id) => data(api.delete(`/api/admin/crop-guides/${id}`)),
};

export default api;
