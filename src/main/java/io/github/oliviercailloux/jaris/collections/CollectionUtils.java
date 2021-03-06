package io.github.oliviercailloux.jaris.collections;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.github.oliviercailloux.jaris.exceptions.Throwing;
import io.github.oliviercailloux.jaris.exceptions.Unchecker;
import java.util.Set;
import java.util.function.Function;

/**
 * A few helper methods generally useful when dealing with collections, felt to miss from the JDK
 * and Guava.
 */
public class CollectionUtils {
  @SuppressWarnings("serial")
  private static class InternalException extends RuntimeException {
    public InternalException(Exception e) {
      super(e);
    }

    /**
     * Guaranteed to be an X, if only X’s are given to the constructor.
     */
    @Override
    public synchronized Exception getCause() {
      return (Exception) super.getCause();
    }
  }

  /**
   * Wraps any checked exceptions into an InternalException with the checked exception as its cause.
   */
  private static final Unchecker<Exception, InternalException> UNCHECKER =
      Unchecker.wrappingWith(InternalException::new);

  /**
   * Returns an immutable map with the given {@code keys} and whose value for each key was computed
   * by {@code valueFunction}. The map's iteration order is the order of {@code keys}.
   *
   * @param <K> the key type of the returned map
   * @param <V> the value type of the returned map
   * @param <X> a sort of exception that the provided function may throw
   * @param keys the keys to use as the map keys
   * @param valueFunction the function producing the values
   * @return an immutable map
   * @throws X iff the given function throws a checked exception
   */
  public static <K, V, X extends Exception> ImmutableMap<K, V> toMap(Set<K> keys,
      Throwing.Function<? super K, V, X> valueFunction) throws X {
    final Function<? super K, V> wrapped = UNCHECKER.wrapFunction(valueFunction);
    try {
      return Maps.toMap(keys, wrapped::apply);
    } catch (InternalException e) {
      @SuppressWarnings("unchecked")
      final X cause = (X) e.getCause();
      throw cause;
    }
  }
}
