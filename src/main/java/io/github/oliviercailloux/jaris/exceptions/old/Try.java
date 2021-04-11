package io.github.oliviercailloux.jaris.exceptions.old;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.oliviercailloux.jaris.exceptions.Throwing;
import io.github.oliviercailloux.jaris.exceptions.Throwing.Consumer;
import io.github.oliviercailloux.jaris.exceptions.Throwing.Function;
import io.github.oliviercailloux.jaris.exceptions.Throwing.Runnable;
import java.util.Optional;


/**
 * <p>
 * An instance of this class contains either a result, in which case it is called a “success”; or a
 * cause of type {@link Exception}, in which case it is called a “failure”.
 * </p>
 * <p>
 * Instances of this class are immutable.
 * </p>
 * <p>
 * Heavily inspired by <a href="https://github.com/vavr-io/vavr">Vavr</a>. One notable difference is
 * that this class (and this library) does not sneaky throw (see the contract of Vavr’s
 * <code>Try#<a href=
 * "https://github.com/vavr-io/vavr/blob/9a40af5cec2622a8ce068d5833a2bf07671f5eed/src/main/java/io/vavr/control/Try.java#L629">get()</a></code>
 * and its <a href=
 * "https://github.com/vavr-io/vavr/blob/9a40af5cec2622a8ce068d5833a2bf07671f5eed/src/main/java/io/vavr/control/Try.java#L1305">implementation</a>).
 *
 * @param <T> the type of result possibly kept in the instance.
 * @param <X> the type of cause possibly kept in the instance.
 */
public abstract class Try<T, X extends Exception> extends TryOptionalUnsafe<T, X>
    implements TryGeneral<T, X> {

  /**
   * Returns a builder of {@code Try} failures, which can be useful for syntactic reason.
   *
   * @param <T> the type of result declared to be possibly (but effectively not) kept in the built
   *        instances.
   * @return a builder.
   */
  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  /**
   * A builder of Try failures, provided only for syntactic sugar.
   *
   * @param <T> the type of result declared to be possibly (but effectively not) kept in the built
   *        instances.
   */
  public static class Builder<T> {
    private Builder() {
      /* Reducing visibility. */
    }

    /**
     * Returns a failure containing the given cause and that will catch only checked exceptions when
     * provided a functional (this matters for the or-based methods).
     *
     * @param <X> the type of cause declared to be possibly (and effectively) kept in the returned
     *        instance.
     * @param cause the cause to contain
     */
    public <X extends Exception> Try<T, X> failure(X cause) {
      return new Failure<>(cause);
    }
  }

  /**
   * Returns a success containing the given result.
   *
   * @param <T> the type of result declared to be possibly (and effectively) kept in the returned
   *        instance.
   * @param <X> the type of cause declared to be possibly (but effectively not) kept in the returned
   *        instance.
   * @param t the result to contain
   */
  public static <T, X extends Exception> Try<T, X> success(T t) {
    return new Success<>(t);
  }

  /**
   * Returns a failure containing the given cause.
   *
   * @param <T> the type of result declared to be possibly (but effectively not) kept in the
   *        returned instance.
   * @param <X> the type of cause declared to be possibly (and effectively) kept in the returned
   *        instance.
   * @param cause the cause to contain
   */
  public static <T, X extends Exception> Try<T, X> failure(X cause) {
    return new Failure<>(cause);
  }

  /**
   * Attempts to wrap a result from the given supplier.
   * <p>
   * This method returns a failure iff the given supplier throws a checked exception and rethrows
   * the throwable thrown by the supplier if it throws anything else than a checked exception.
   *
   * @param <T> the type of result possibly kept in the returned instance.
   * @param <X> the type of cause possibly kept in the returned instance.
   * @param supplier the supplier to get a result from
   * @return a success containing the result if the supplier returns a result; a failure containing
   *         the throwable if the supplier throws a checked exception
   */
  public static <T, X extends Exception> Try<T, X> get(
      Throwing.Supplier<? extends T, ? extends Exception> supplier) {
    try {
      return success(supplier.get());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      @SuppressWarnings("unchecked")
      final X exc = (X) e;
      return failure(exc);
    }
  }

  /**
   * Conceptually safe cast. Currently unused.
   */
  @SuppressWarnings("unused")
  private static <T, X extends Exception, U extends T, Y extends X> Try<T, X> cast(Try<U, Y> t) {
    @SuppressWarnings("unchecked")
    final Try<T, X> casted = (Try<T, X>) t;
    return casted;
  }

  private static class Success<T, X extends Exception> extends Try<T, X> {

    private final T result;

    private Success(T result) {
      this.result = checkNotNull(result);
    }

    @Override
    public boolean isSuccess() {
      return true;
    }

    @Override
    public boolean isFailure() {
      return false;
    }

    @Override
    Optional<T> getResult() {
      return Optional.of(result);
    }

    @Override
    Optional<X> getCause() {
      return Optional.empty();
    }

    private <Y extends Exception> Try<T, Y> cast() {
      @SuppressWarnings("unchecked")
      final Try<T, Y> casted = (Try<T, Y>) this;
      return casted;
    }

    @Override
    public <U, Y extends Exception> U map(
        Throwing.Function<? super T, ? extends U, ? extends Y> transformation,
        Throwing.Function<? super X, ? extends U, ? extends Y> causeTransformation) throws Y {
      return transformation.apply(result);
    }

    @Override
    public <Y extends Exception> T orMapCause(
        Function<? super X, ? extends T, Y> causeTransformation) throws Y {
      return result;
    }

    @Override
    public <Y extends Exception> Optional<T> orConsumeCause(Consumer<? super X, Y> consumer)
        throws Y {
      return Optional.of(result);
    }

    @Override
    public T orThrow() throws X {
      return result;
    }

    @Override
    public <Y extends Exception, Z extends Exception, W extends Exception> Try<T, Z> or(
        Throwing.Supplier<? extends T, Y> supplier,
        Throwing.BiFunction<? super X, ? super Y, ? extends Z, W> exceptionsMerger) throws W {
      return cast();
    }

    @Override
    public Try<T, X> andRun(Runnable<? extends X> runnable) {
      final TryVoid<? extends X> ran = TryVoid.run(runnable);
      return ran.map(() -> this, Try::failure);
    }

    @Override
    public Try<T, X> andConsume(Consumer<? super T, ? extends X> consumer) {
      return andRun(() -> consumer.accept(result));
    }

    @Override
    public <U, V, Y extends Exception> Try<V, X> and(Try<U, ? extends X> t2,
        Throwing.BiFunction<? super T, ? super U, ? extends V, Y> merger) throws Y {
      return t2.map(u -> success(merger.apply(result, u)), Try::failure);
    }

    @Override
    public <U> Try<U, X> flatMap(Function<? super T, ? extends U, ? extends X> mapper) {
      return Try.get(() -> mapper.apply(result));
    }
  }

  private static class Failure<T, X extends Exception> extends Try<T, X> {
    private final X cause;

    private Failure(X cause) {
      this.cause = checkNotNull(cause);
    }

    @Override
    public boolean isSuccess() {
      return false;
    }

    @Override
    public boolean isFailure() {
      return true;
    }

    @Override
    Optional<T> getResult() {
      return Optional.empty();
    }

    @Override
    Optional<X> getCause() {
      return Optional.of(cause);
    }

    private <U> Try<U, X> cast() {
      @SuppressWarnings("unchecked")
      final Try<U, X> casted = (Try<U, X>) this;
      return casted;
    }

    @Override
    public <U, Y extends Exception> U map(
        Throwing.Function<? super T, ? extends U, ? extends Y> transformation,
        Throwing.Function<? super X, ? extends U, ? extends Y> causeTransformation) throws Y {
      return causeTransformation.apply(cause);
    }

    @Override
    public <Y extends Exception> T orMapCause(
        Function<? super X, ? extends T, Y> causeTransformation) throws Y {
      return causeTransformation.apply(cause);
    }

    @Override
    public <Y extends Exception> Optional<T> orConsumeCause(Consumer<? super X, Y> consumer)
        throws Y {
      consumer.accept(cause);
      return Optional.empty();
    }

    @Override
    public T orThrow() throws X {
      throw cause;
    }

    @Override
    public <Y extends Exception, Z extends Exception, W extends Exception> Try<T, Z> or(
        Throwing.Supplier<? extends T, Y> supplier,
        Throwing.BiFunction<? super X, ? super Y, ? extends Z, W> exceptionsMerger) throws W {
      final Try<T, Y> t2 = Try.get(supplier);
      return t2.map(Try::success, y -> failure(exceptionsMerger.apply(cause, y)));
    }

    @Override
    public Try<T, X> andRun(Runnable<? extends X> runnable) {
      return this;
    }

    @Override
    public Try<T, X> andConsume(Consumer<? super T, ? extends X> consumer) {
      return this;
    }

    @Override
    public <U, V, Y extends Exception> Try<V, X> and(Try<U, ? extends X> t2,
        Throwing.BiFunction<? super T, ? super U, ? extends V, Y> merger) throws Y {
      return cast();
    }

    @Override
    public <U> Try<U, X> flatMap(Function<? super T, ? extends U, ? extends X> mapper) {
      return cast();
    }
  }

  protected Try() {
    /* Reducing visibility. */
  }

  /**
   * Returns <code>true</code> iff this object contains a result (and not a cause).
   */
  @Override
  public abstract boolean isSuccess();

  /**
   * Return <code>true</code> iff this object contains a cause (and not a result).
   */
  @Override
  public abstract boolean isFailure();

  /**
   * Returns this instance if it is a success. Otherwise, attempts to get a result from the given
   * supplier. If this succeeds, that is, if the supplier returns a result, returns a success
   * containing that result. Otherwise, if the supplier throws a checked exception, merges both
   * exceptions using the given {@code exceptionMerger} and returns a failure containing that merged
   * cause.
   *
   * @param <Y> a type of exception that the provided supplier may throw
   * @param <Z> the type of cause that the returned try will be declared to contain
   * @param <W> a type of exception that the provided merger may throw
   * @param supplier the supplier that is invoked if this try is a failure
   * @param exceptionsMerger the function invoked to merge both exceptions if this try is a failure
   *        and the given supplier threw a checked exception
   * @return a success if this instance is a success or the given supplier returned a result
   * @throws W iff the merger was applied and threw a checked exception
   */
  public abstract <Y extends Exception, Z extends Exception, W extends Exception> Try<T, Z> or(
      Throwing.Supplier<? extends T, Y> supplier,
      Throwing.BiFunction<? super X, ? super Y, ? extends Z, W> exceptionsMerger) throws W;

  /**
   * Returns a failure containing this cause if this instance is a failure, a failure containing the
   * checked exception that the provided runnable threw if it did throw one, and a success containg
   * the result contained in this instance if this instance is a success and the provided runnable
   * does not throw.
   * <p>
   * If this instance is a failure, returns this instance without running the provided runnable.
   * Otherwise, if the runnable succeeds (that is, does not throw), returns this instance.
   * Otherwise, if the runnable throws a checked exception, returns a failure containing the cause
   * it threw.
   *
   * @param runnable the function to run if this instance is a success
   * @return a success iff this instance is a success and the provided runnable terminated without
   *         throwing
   */
  public abstract Try<T, X> andRun(Throwing.Runnable<? extends X> runnable);

  /**
   * Returns a failure containing this cause if this instance is a failure, a failure containing the
   * checked exception that the provided consumer threw if it did throw one, and a success containg
   * the result contained in this instance otherwise.
   * <p>
   * If this instance is a failure, returns this instance without running the provided consumer.
   * Otherwise, if the consumer succeeds (that is, does not throw), returns this instance.
   * Otherwise, if the consumer throws a checked exception, returns a failure containing the cause
   * it threw.
   *
   * @param consumer the function to run if this instance is a success
   * @return a success iff this instance is a success and the provided consumer terminated without
   *         throwing
   */
  public abstract Try<T, X> andConsume(Throwing.Consumer<? super T, ? extends X> consumer);

  /**
   * Returns this failure if this instance is a failure; the provided failure if it is a failure and
   * this instance is a success; and a success containing the merge of the result contained in this
   * instance and the one contained in {@code t2}, if they both are successes.
   *
   * @param <U> the type of result that the provided try is declared to contain
   * @param <V> the type of result that the returned try will be declared to contain
   * @param <Y> a type of exception that the provided merger may throw
   * @param t2 the try to consider if this try is a success
   * @param merger the function invoked to merge the results if both this and the given try are
   *        successes
   * @return a success if this instance and the given try are two successes
   * @throws Y iff the merger was applied and threw a checked exception
   */
  public abstract <U, V, Y extends Exception> Try<V, X> and(Try<U, ? extends X> t2,
      Throwing.BiFunction<? super T, ? super U, ? extends V, Y> merger) throws Y;

  /**
   * Returns this failure if this instance is a failure; a failure containing the cause thrown by
   * the given function if it threw a checked exception; or a success containing the result of
   * applying the provided mapper to the result contained in this instance if it is a success and
   * the mapper did not throw.
   *
   * @param <U> the type of result that the returned try will be declared to contain
   * @param mapper the mapper to apply to the result contained in this instance if it is a success
   * @return a success iff this instance is a success and the provided mapper does not throw
   */
  public abstract <U> Try<U, X> flatMap(
      Throwing.Function<? super T, ? extends U, ? extends X> mapper);

  @Override
  public String toString() {
    final ToStringHelper stringHelper = MoreObjects.toStringHelper(this);
    orConsumeCause(e -> stringHelper.add("cause", e)).ifPresent(r -> stringHelper.add("result", r));
    return stringHelper.toString();
  }

}