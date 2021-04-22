package io.github.oliviercailloux.jaris.exceptions;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * An equivalent to Java stream which allows for functional interfaces that may throw checked
 * exceptions; designed for people who do not like sneaky-throws.
 * <p>
 * The following popular SO questions mention several libraries that deal with the “streams and
 * checked exceptions” issue, but (at the time of writing) all those I found there sneaky throw,
 * apart from <a href= "https://github.com/JeffreyFalgout/ThrowingStream/">ThrowingStream</a>.
 * </p>
 * <ul>
 * <li><a href="https://stackoverflow.com/questions/23548589">Java 8: How do I work with exception
 * throwing methods in streams?</a></li>
 * <li><a href="https://stackoverflow.com/questions/19757300">Java 8: Lambda-Streams, Filter by
 * Method with Exception</a></li>
 * <li><a href="https://stackoverflow.com/questions/30117134">Aggregate runtime exceptions in Java 8
 * streams</a></li>
 * <li><a href="https://stackoverflow.com/questions/27644361">How can I throw CHECKED exceptions
 * from inside Java 8 streams?</a></li>
 * </ul>
 * <p>
 * This approach is heavily inspired by
 * <a href= "https://github.com/JeffreyFalgout/ThrowingStream/">ThrowingStream</a>; some differences
 * are discussed <a href="https://github.com/JeffreyFalgout/ThrowingStream/issues/3">here</a>.
 * </p>
 *
 * @param <T> the type of the stream elements
 * @param <X> an exception type that functionals used with this stream may throw, and that terminal
 *        operations on this stream may throw
 * @see Stream
 */
public class CheckedStream<T, X extends Exception> {
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
   * Returns a checked stream wrapping the given stream.
   * <p>
   * The returned stream will behave as the delegate one except that it accepts functionals that
   * declare checked exceptions. The returned stream throws, on terminal operations, any exception
   * thrown by a functional operation during the stream operations.
   * </p>
   *
   * @param <T> the type of the stream elements
   * @param <X> an exception type that functionals used with the returned stream may throw, and
   *        therefore, that terminal operations on the returned stream may throw
   * @param delegate the stream executing operations
   * @return a checked stream delegating to the given stream
   */
  public static <T, X extends Exception> CheckedStream<T, X> wrapping(Stream<T> delegate) {
    return new CheckedStream<>(delegate);
  }

  /**
   * Returns a checked stream wrapping the stream produced by {@code collection.stream()}.
   * <p>
   * The returned stream will behave as the delegate one except that it accepts functionals that
   * declare checked exceptions. The returned stream throws, on terminal operations, any exception
   * thrown by a functional operation during the stream operations.
   * </p>
   *
   * @param <T> the type of the stream elements
   * @param <X> an exception type that functionals used with the returned stream may throw, and
   *        therefore, that terminal operations on the returned stream may throw
   * @param collection the source
   * @return a checked stream delegating to the stream produced by the given collection
   */
  public static <T, X extends Exception> CheckedStream<T, X> from(Collection<T> collection) {
    return new CheckedStream<>(collection.stream());
  }

  /**
   * Returns an infinite sequential unordered checked stream where each element is generated by the
   * provided {@code Throwing.Supplier}. This is suitable for generating constant streams, streams
   * of random elements, etc.
   * <p>
   * The returned stream accepts functionals that declare checked exceptions. The returned stream
   * throws, on terminal operations, any exception thrown by a functional operation during the
   * stream operations (including by the given generator).
   * </p>
   *
   * @param <T> the type of stream elements
   * @param <X> an exception type that functionals used with the returned stream may throw, and
   *        therefore, that terminal operations on the returned stream may throw
   * @param generator the {@code Throwing.Supplier} of generated elements
   * @return a new infinite sequential unordered {@code CheckedStream}
   */
  public static <T, X extends Exception> CheckedStream<T, X>
      generate(Throwing.Supplier<? extends T, ? extends X> generator) {
    final Supplier<? extends T> wrapped = UNCHECKER.wrapSupplier(generator);
    return new CheckedStream<>(Stream.generate(wrapped));
  }

  private final Stream<T> delegate;

  private CheckedStream(Stream<T> delegate) {
    this.delegate = checkNotNull(delegate);
  }

  /**
   * @see Stream#distinct()
   */
  public CheckedStream<T, X> distinct() {
    return new CheckedStream<>(delegate.distinct());
  }

  /**
   * @see Stream#dropWhile(Predicate)
   */
  public CheckedStream<T, X> dropWhile(Throwing.Predicate<? super T, ? extends X> predicate) {
    final Predicate<? super T> wrapped = UNCHECKER.wrapPredicate(predicate);
    return new CheckedStream<>(delegate.dropWhile(wrapped));
  }

  /**
   * @see Stream#takeWhile(Predicate)
   */
  public CheckedStream<T, X> takeWhile(Throwing.Predicate<? super T, ? extends X> predicate) {
    final Predicate<? super T> wrapped = UNCHECKER.wrapPredicate(predicate);
    return new CheckedStream<>(delegate.takeWhile(wrapped));
  }

  /**
   * @see Stream#filter(Predicate)
   */
  public CheckedStream<T, X> filter(Throwing.Predicate<? super T, ? extends X> predicate) {
    final Predicate<? super T> wrapped = UNCHECKER.wrapPredicate(predicate);
    return new CheckedStream<>(delegate.filter(wrapped));
  }

  /**
   * @see Stream#flatMap(Function)
   */
  public <R> CheckedStream<R, X>
      flatMap(Throwing.Function<? super T, ? extends Stream<? extends R>, ? extends X> mapper) {
    final Function<? super T, ? extends Stream<? extends R>> wrapped =
        UNCHECKER.wrapFunction(mapper);
    return new CheckedStream<>(delegate.flatMap(wrapped));
  }

  /**
   * @see Stream#limit(long)
   */
  public CheckedStream<T, X> limit(long maxSize) {
    return new CheckedStream<>(delegate.limit(maxSize));
  }

  /**
   * @see Stream#map(Function)
   */
  public <R> CheckedStream<R, X>
      map(Throwing.Function<? super T, ? extends R, ? extends X> mapper) {
    final Function<? super T, ? extends R> wrapped = UNCHECKER.wrapFunction(mapper);
    return new CheckedStream<>(delegate.map(wrapped));
  }

  /**
   * @see Stream#skip(long)
   */
  public CheckedStream<T, X> skip(long n) {
    return new CheckedStream<>(delegate.skip(n));
  }

  /**
   * @see Stream#sorted()
   */
  public CheckedStream<T, X> sorted() {
    return new CheckedStream<>(delegate.sorted());
  }

  /**
   * @see Stream#sorted(Comparator)
   */
  public CheckedStream<T, X> sorted(Throwing.Comparator<? super T, ? extends X> comparator) {
    final Comparator<? super T> wrapped = UNCHECKER.wrapComparator(comparator);
    return new CheckedStream<>(delegate.sorted(wrapped));
  }

  /**
   * @see Stream#reduce(Object, BinaryOperator) <code>Stream.reduce(T, BinaryOperator)</code>
   */
  public T reduce(T identity, Throwing.BinaryOperator<T, ? extends X> accumulator) throws X {
    final BinaryOperator<T> wrapped = UNCHECKER.wrapBinaryOperator(accumulator);
    try {
      return delegate.reduce(identity, wrapped);
    } catch (InternalException e) {
      final Exception cause = e.getCause();
      @SuppressWarnings("unchecked")
      final X castedCause = (X) cause;
      throw castedCause;
    }
  }

  /**
   * @see Stream#reduce(BinaryOperator)
   */
  public Optional<T> reduce(Throwing.BinaryOperator<T, ? extends X> accumulator) throws X {
    final BinaryOperator<T> wrapped = UNCHECKER.wrapBinaryOperator(accumulator);
    try {
      return delegate.reduce(wrapped);
    } catch (InternalException e) {
      final Exception cause = e.getCause();
      @SuppressWarnings("unchecked")
      final X castedCause = (X) cause;
      throw castedCause;
    }
  }

  /**
   * @see Stream#reduce(Object, BiFunction, BinaryOperator)
   *      <code>Stream.reduce(U, BiFunction, BinaryOperator)</code>
   */
  public <U> U reduce(U identity, Throwing.BiFunction<U, ? super T, U, ? extends X> accumulator,
      Throwing.BinaryOperator<U, ? extends X> combiner) throws X {
    final BiFunction<U, ? super T, U> wrappedAccumulator = UNCHECKER.wrapBiFunction(accumulator);
    final BinaryOperator<U> wrappedCombiner = UNCHECKER.wrapBinaryOperator(combiner);
    try {
      return delegate.reduce(identity, wrappedAccumulator, wrappedCombiner);
    } catch (InternalException e) {
      final Exception cause = e.getCause();
      @SuppressWarnings("unchecked")
      final X castedCause = (X) cause;
      throw castedCause;
    }
  }

  /**
   * @see Stream#collect(Supplier, BiConsumer, BiConsumer)
   */
  public <R> R collect(Throwing.Supplier<R, ? extends X> supplier,
      Throwing.BiConsumer<R, ? super T, ? extends X> accumulator,
      Throwing.BiConsumer<R, R, ? extends X> combiner) throws X {
    final Supplier<R> wrappedSupplier = UNCHECKER.wrapSupplier(supplier);
    final BiConsumer<R, ? super T> wrappedAccumulator = UNCHECKER.wrapBiConsumer(accumulator);
    final BiConsumer<R, R> wrappedCombiner = UNCHECKER.wrapBiConsumer(combiner);
    try {
      return delegate.collect(wrappedSupplier, wrappedAccumulator, wrappedCombiner);
    } catch (InternalException e) {
      final Exception cause = e.getCause();
      @SuppressWarnings("unchecked")
      final X castedCause = (X) cause;
      throw castedCause;
    }
  }

  /**
   * @see Stream#collect(Collector)
   */
  public <R, A> R collect(Collector<? super T, A, R> collector) throws X {
    try {
      return delegate.collect(collector);
    } catch (InternalException e) {
      final Exception cause = e.getCause();
      @SuppressWarnings("unchecked")
      final X castedCause = (X) cause;
      throw castedCause;
    }
  }

  /**
   * @see Stream#allMatch(Predicate)
   */
  public boolean allMatch(Throwing.Predicate<? super T, ? extends X> predicate) throws X {
    /*
     * Any checked exception thrown by predicate is supposed to extend X, by its header. Only such
     * exceptions are wrapped into an InternalException instance by the UNCHECKER. Thus, any
     * InternalException thrown by the wrapped predicate has a Y as its cause.
     */
    final Predicate<? super T> wrapped = UNCHECKER.wrapPredicate(predicate);
    try {
      return delegate.allMatch(wrapped);
    } catch (InternalException e) {
      final Exception cause = e.getCause();
      @SuppressWarnings("unchecked")
      final X castedCause = (X) cause;
      throw castedCause;
    }
  }

  /**
   * @see Stream#anyMatch(Predicate)
   */
  public boolean anyMatch(Throwing.Predicate<? super T, ? extends X> predicate) throws X {
    final Predicate<? super T> wrapped = UNCHECKER.wrapPredicate(predicate);
    try {
      return delegate.anyMatch(wrapped);
    } catch (InternalException e) {
      final Exception cause = e.getCause();
      @SuppressWarnings("unchecked")
      final X castedCause = (X) cause;
      throw castedCause;
    }
  }

  /**
   * @see Stream#noneMatch(Predicate)
   */
  public boolean noneMatch(Throwing.Predicate<? super T, ? extends X> predicate) throws X {
    final Predicate<? super T> wrapped = UNCHECKER.wrapPredicate(predicate);
    try {
      return delegate.noneMatch(wrapped);
    } catch (InternalException e) {
      final Exception cause = e.getCause();
      @SuppressWarnings("unchecked")
      final X castedCause = (X) cause;
      throw castedCause;
    }
  }

  /**
   * @see Stream#peek(Consumer)
   */
  public CheckedStream<T, X> peek(Throwing.Consumer<? super T, ? extends X> action) throws X {
    final Consumer<? super T> wrapped = UNCHECKER.wrapConsumer(action);
    try {
      return new CheckedStream<>(delegate.peek(wrapped));
    } catch (InternalException e) {
      final Exception cause = e.getCause();
      @SuppressWarnings("unchecked")
      final X castedCause = (X) cause;
      throw castedCause;
    }
  }

  /**
   * @see Stream#count()
   */
  public long count() throws X {
    try {
      return delegate.count();
    } catch (InternalException e) {
      final Exception cause = e.getCause();
      @SuppressWarnings("unchecked")
      final X castedCause = (X) cause;
      throw castedCause;
    }
  }

  /**
   * @see Stream#findAny()
   */
  public Optional<T> findAny() throws X {
    try {
      return delegate.findAny();
    } catch (InternalException e) {
      final Exception cause = e.getCause();
      @SuppressWarnings("unchecked")
      final X castedCause = (X) cause;
      throw castedCause;
    }
  }

  /**
   * @see Stream#findFirst()
   */
  public Optional<T> findFirst() throws X {
    try {
      return delegate.findFirst();
    } catch (InternalException e) {
      final Exception cause = e.getCause();
      @SuppressWarnings("unchecked")
      final X castedCause = (X) cause;
      throw castedCause;
    }
  }

  /**
   * @see Stream#forEach(Consumer)
   */
  public void forEach(Throwing.Consumer<? super T, ? extends X> action) throws X {
    final Consumer<? super T> wrapped = UNCHECKER.wrapConsumer(action);
    try {
      delegate.forEach(wrapped);
    } catch (InternalException e) {
      final Exception cause = e.getCause();
      @SuppressWarnings("unchecked")
      final X castedCause = (X) cause;
      throw castedCause;
    }
  }

  /**
   * @see Stream#forEachOrdered(Consumer)
   */
  public void forEachOrdered(Throwing.Consumer<? super T, ? extends X> action) throws X {
    final Consumer<? super T> wrapped = UNCHECKER.wrapConsumer(action);
    try {
      delegate.forEachOrdered(wrapped);
    } catch (InternalException e) {
      final Exception cause = e.getCause();
      @SuppressWarnings("unchecked")
      final X castedCause = (X) cause;
      throw castedCause;
    }
  }

  /**
   * @see Stream#max(Comparator)
   */
  public Optional<T> max(Throwing.Comparator<? super T, ? extends X> comparator) throws X {
    final Comparator<? super T> wrapped = UNCHECKER.wrapComparator(comparator);
    try {
      return delegate.max(wrapped);
    } catch (InternalException e) {
      final Exception cause = e.getCause();
      @SuppressWarnings("unchecked")
      final X castedCause = (X) cause;
      throw castedCause;
    }
  }

  /**
   * @see Stream#min(Comparator)
   */
  public Optional<T> min(Comparator<? super T> comparator) throws X {
    try {
      return delegate.min(comparator);
    } catch (InternalException e) {
      final Exception cause = e.getCause();
      @SuppressWarnings("unchecked")
      final X castedCause = (X) cause;
      throw castedCause;
    }
  }

  /**
   * Accumulates the elements of this stream into an immutable list.
   *
   * @return an immutable list
   * @throws X if any functional interface operating on this stream throws a checked exception
   */
  public ImmutableList<T> toList() throws X {
    try {
      return delegate.collect(ImmutableList.toImmutableList());
    } catch (InternalException e) {
      final Exception cause = e.getCause();
      @SuppressWarnings("unchecked")
      final X castedCause = (X) cause;
      throw castedCause;
    }
  }
}
