package com.citymall.dto.admin;

/**
 * Payload for {@code GET /api/v1/admin/dashboard/summary}.
 *
 * <p>Customer counts are live (local users table). Order / product / revenue
 * fields are STUBBED — customer-service owns no Order/Product tables. Wire
 * them to order-service / product-service (Feign) when those admin
 * read-models are available; fields and names are already frontend-stable.
 */
public record DashboardSummaryResponse(
        long totalCustomers,
        long blockedCustomers,
        long totalOrdersToday,
        long pendingOrders,
        long lowStockProductCount,
        double revenueToday
) {
    public static DashboardSummaryResponse stubbed(
            long totalCustomers, long blockedCustomers) {
        return new DashboardSummaryResponse(
                totalCustomers,
                blockedCustomers,
                0L,   // STUB: order-service GET /admin/orders?status + date filter
                0L,   // STUB: order-service pending-order count
                0L,   // STUB: product-service low-stock count
                0.0); // STUB: payment/order-service revenue-today sum
    }
}
