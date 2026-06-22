import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Dev proxy: the browser calls /api/* same-origin, Next forwards to Spring on :8080.
  // → no CORS needed in dev. (CORS on the backend is the alternative for separate deploys.)
  async rewrites() {
    return [
      { source: "/api/:path*", destination: "http://localhost:8080/api/:path*" },
    ];
  },
};

export default nextConfig;
