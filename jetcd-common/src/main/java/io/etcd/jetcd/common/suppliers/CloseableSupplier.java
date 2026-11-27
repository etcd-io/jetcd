package io.etcd.jetcd.common.suppliers;

import java.util.function.Supplier;

/**
 * A supplier that provides AutoCloseable instances and can itself be closed
 * to clean up the supplied resource.
 *
 * @param <T> the type of AutoCloseable results supplied
 */
public interface CloseableSupplier<T extends AutoCloseable> extends Supplier<T>, AutoCloseable {
}
