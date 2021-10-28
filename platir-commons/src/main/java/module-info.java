module io.platir.commons {
    requires transitive io.platir.api;
    requires com.google.gson;
    exports io.platir.commons;
    exports io.platir.utils;
    opens io.platir.commons;
}
