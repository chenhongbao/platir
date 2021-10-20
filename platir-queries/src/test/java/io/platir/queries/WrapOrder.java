package io.platir.queries;

import io.platir.service.DataQueryException;
import io.platir.service.Order;
import io.platir.service.Queries;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 *
 * @author Chen Hongbao
 */
public class WrapOrder extends OrderImpl {

    static boolean collectionEquals(Set<Order> c1, Set<Order> c2) {
        return Utils.collectionEquals(Order.class, c1, c2);
    }

    static boolean testOrder(Queries queries) throws DataQueryException {
        /* Step 1: Insert orders. */
        var order1 = new OrderImpl();
        order1.setOrderId(UUID.randomUUID().toString());
        var order2 = new OrderImpl();
        order2.setOrderId(UUID.randomUUID().toString());
        queries.insert(order1, order2);
        /* Step 2: Check runtime schema are the same. */
        var orders = new HashSet<Order>();
        orders.add(order1);
        orders.add(order2);
        if (!Utils.collectionEquals(Order.class, orders, queries.selectOrders())) {
            return false;
        }
        /* Load schema files into a new Queries and check equality. */
        var anotherQueries = new QueriesImpl();
        anotherQueries.initialize();
        return Utils.collectionEquals(Order.class, anotherQueries.selectOrders(), queries.selectOrders());
    }
}
