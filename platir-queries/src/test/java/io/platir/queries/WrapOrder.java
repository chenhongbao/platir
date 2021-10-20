package io.platir.queries;

import io.platir.service.Order;
import java.util.Set;

/**
 *
 * @author Chen Hongbao
 */
public class WrapOrder extends OrderImpl {

    static boolean collectionEquals(Set<Order> c1, Set<Order> c2) {
        return TestUtils.collectionEquals(Order.class, c1, c2);
    }
}
