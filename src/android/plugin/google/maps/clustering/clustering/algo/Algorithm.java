package plugin.google.maps.clustering.clustering.algo;

import java.util.Collection;
import java.util.Set;

import plugin.google.maps.clustering.clustering.Cluster;
import plugin.google.maps.clustering.clustering.ClusterItem;

/**
 * Logic for computing clusters
 */
public interface Algorithm<T extends ClusterItem> {
    void addItem(T item);

    void addItems(Collection<T> items);

    void clearItems();

    void removeItem(T item);

    Set<? extends Cluster<T>> getClusters(double zoom);

    Collection<T> getItems();
}