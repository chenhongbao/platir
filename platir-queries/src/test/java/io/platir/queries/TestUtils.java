package io.platir.queries;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Chen Hongbao
 */
class TestUtils {

    static <T> boolean beanEquals(Class<T> clazz, Object o1, Object o2) {
        if (o1.getClass() != clazz || o2.getClass() != clazz) {
            return false;
        }
        for (var method : clazz.getMethods()) {
            if (!method.getName().startsWith("get")) {
                continue;
            }
            try {
                if (!method.invoke(o1).equals(method.invoke(o2))) {
                    return false;
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                Logger.getLogger(QueriesImplTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return true;
    }

    static <T> boolean collectionEquals(Class<T> clazz, Set<T> c1, Set<T> c2) {
        for (var o1 : c1) {
            boolean hit = false;
            for (var o2 : c2) {
                if (beanEquals(clazz, o1, o2)) {
                    hit = true;
                    break;
                }
            }
            if (!hit) {
                return false;
            }
        }
        return true;
    }
}
