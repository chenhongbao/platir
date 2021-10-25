package io.platir.engine.broker;

import io.platir.broker.MarketDataService;
import io.platir.broker.TradingService;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public class BrokerServiceFinder {

    private final Set<URI> jarUris = new HashSet<>();

    public void addServiceJar(URI jarUri) {
        if (jarUri != null) {
            jarUris.add(jarUri);
        }
    }

    public Set<TradingService> getTradingServices() {
        return loadServices(TradingService.class);
    }

    public Set<MarketDataService> getMarketDataServices() {
        return loadServices(MarketDataService.class);
    }

    private <T> Set<T> loadServices(Class<T> clazz) {
        return ServiceLoader.load(clazz, new URLClassLoader(toUrls(), this.getClass().getClassLoader()))
                .stream()
                .map(provider -> {
                    return provider.get();
                })
                .collect(Collectors.toSet());
    }

    private URL[] toUrls() {
        return jarUris.stream().map(uri -> {
            try {
                return uri.toURL();
            } catch (MalformedURLException ignored) {
                return null;
            }
        }).filter(uri -> {
            return uri != null;
        }).collect(Collectors.toSet()).toArray(new URL[0]);
    }
}
