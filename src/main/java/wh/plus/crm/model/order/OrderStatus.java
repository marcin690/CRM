package wh.plus.crm.model.order;

/** Status realizacji zamówienia. */
public enum OrderStatus {
    DRAFT,          // szkic
    PLACED,         // złożone
    CONFIRMED,      // potwierdzone
    IN_PRODUCTION,  // w produkcji
    SHIPPED,        // wysłane
    DELIVERED,      // dostarczone
    CANCELLED       // anulowane
}
