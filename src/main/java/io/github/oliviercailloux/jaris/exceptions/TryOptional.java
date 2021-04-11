package io.github.oliviercailloux.jaris.exceptions;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import io.github.oliviercailloux.jaris.exceptions.Throwing.BiFunction;
import io.github.oliviercailloux.jaris.exceptions.Throwing.Consumer;
import io.github.oliviercailloux.jaris.exceptions.Throwing.Runnable;
import io.github.oliviercailloux.jaris.exceptions.Throwing.Supplier;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * The root of the Try contract hierarchy, defining in the most general way the concepts of success,
 * failure, catching, and equality.
 * <p>
 * This is not a public part of the contract of Try because cathesAll(), for example, should not be
 * exposed: different catching behaviors are publicly viewed as unrelated types.
 * <p>
 * Is homeomorphic to an {@code Optional<T>} xor {@code X} (plus indication of catching all or
 * checked): either is a success, and then <em>may</em> contain a result of type {@code T}, or is a
 * failure, and then <em>does</em> contain a cause of type {@code X}.
 *
 * @param <T> the type of result possibly kept in this object.
 * @param <X> the type of cause kept in this object if it is a failure.
 */
abstract class TryOptional<T, X extends Throwable> {
  /**
   * A sort of try optional such that a success has an associated value. Suitable for Try and
   * TrySafe, depending on the catching strategy. The name indicates that this interface applies to
   * both catching strategies.
   *
   * @param <T> the type of result kept in this object if it is a success.
   * @param <X> the type of cause kept in this object if it is a failure.
   */
  public interface TryVariableCatchInterface<T, X extends Z, Z extends Throwable> {

    /**
     * Returns <code>true</code> iff this object contains a result (and not a cause).
     *
     * @return <code>true</code> iff {@link #isFailure()} returns <code>false</code>
     */
    public boolean isSuccess();

    /**
     * Return <code>true</code> iff this object contains a cause (and not a result).
     *
     * @return <code>true</code> iff {@link #isSuccess()} returns <code>false</code>
     */
    public boolean isFailure();

    /**
     * Returns the transformed result contained in this instance if it is a success, using the
     * provided {@code transformation}; or the transformed cause contained in this instance if it is
     * a failure, using the provided {@code causeTransformation}.
     * <p>
     * This method necessarily applies exactly one of the provided functions.
     *
     * @param <U> the type of transformed result to return
     * @param <Y> a type of exception that the provided functions may throw
     * @param transformation a function to apply to the result if this instance is a success
     * @param causeTransformation a function to apply to the cause if this instance is a failure
     * @return the transformed result or cause
     * @throws Y iff the function that was applied threw a checked exception
     */
    public <U, Y extends Exception> U map(
        Throwing.Function<? super T, ? extends U, ? extends Y> transformation,
        Throwing.Function<? super X, ? extends U, ? extends Y> causeTransformation) throws Y;

    /**
     * Returns the result contained in this instance if it is a success, without applying the
     * provided function; or returns the transformed cause contained in this instance if it is a
     * failure, using the provided {@code causeTransformation}.
     * <p>
     * Equivalent to: {@code map(Function#identity(), causeTransformation)}.
     *
     * @param <Y> a type of exception that the provided function may throw
     * @param causeTransformation the function to apply if this instance is a failure
     * @return the result, or the transformed cause
     * @throws Y iff the function was applied and threw a checked exception
     */
    public <Y extends Exception> T orMapCause(
        Throwing.Function<? super X, ? extends T, Y> causeTransformation) throws Y;

    /**
     * Returns an optional containing the result of this instance, without invoking the given
     * consumer, if this try is a success; otherwise, invokes the given consumer and returns an
     * empty optional.
     *
     * @param <Y> a type of exception that the provided consumer may throw
     * @param consumer the consumer to invoke if this instance is a failure
     * @return an optional, containing the result if this instance is a success, empty otherwise
     * @throws Y iff the consumer was invoked and threw a checked exception
     */
    public <Y extends Exception> Optional<T> orConsumeCause(
        Throwing.Consumer<? super X, Y> consumer) throws Y;

    /**
     * Returns the result contained in this instance if this instance is a success, or throws the
     * cause contained in this instance.
     *
     * @return the result that this success contains
     * @throws X iff this instance is a failure
     */
    public T orThrow() throws X;

    public <Y extends Z> T orThrow(Function<X, Y> causeTransformation) throws Y;

    /**
     * Runs the runnable iff this instance is a success, and returns the result; otherwise, returns
     * this instance.
     *
     * @param runnable the functional interface to run if this instance is a success
     * @return a success iff this instance is a success and the provided runnable terminated without
     *         throwing
     */
    public TryVariableCatchInterface<T, ?, Z> andRun(Throwing.Runnable<? extends X> runnable);

    /**
     * Runs the consumer iff this instance is a success, and returns this instance if it succeeds
     * and the cause of failure if it throws a caught throwable; otherwise, returns this instance.
     *
     * @param consumer the functional interface to run if this instance is a success
     * @return a success iff this instance is a success and the provided consumer terminated without
     *         throwing
     */
    public TryVariableCatchInterface<T, ?, Z> andConsume(
        Throwing.Consumer<? super T, ? extends X> consumer);

    /**
     * Applies the given mapper iff this instance is a success, and returns the transformed success
     * if it succeeds, or the cause of the failure if it throws a catchable throwable; otherwise,
     * returns this instance.
     *
     * @param <U> the type of result that the returned try will be declared to contain
     * @param mapper the mapper to apply to the result contained in this instance if it is a success
     * @return a success iff this instance is a success and the provided mapper does not throw
     */
    public <U> TryVariableCatchInterface<U, X, Z> flatMap(
        Throwing.Function<? super T, ? extends U, ? extends X> mapper);

    /**
     * Returns a string representation of this object, suitable for debug.
     */
    @Override
    public String toString();

  }
  /**
   * A sort of try optional such that a success has no associated value. Suitable for TryVoid and
   * TryVoidCatchAll, depending on the catching strategy. The name indicates that this interface
   * applies to both catching strategies.
   *
   * @param <X> the type of cause kept in this object if it is a failure.
   */
  public interface TryVoidVariableCatchInterface<X extends Z, Z extends Throwable> {

    /**
     * Returns <code>true</code> iff this instance is a success, hence, contains no cause.
     *
     * @return <code>true</code> iff {@link #isFailure()} returns <code>false</code>
     */
    public boolean isSuccess();

    /**
     * Return <code>true</code> iff this object contains a cause.
     *
     * @return <code>true</code> iff {@link #isSuccess()} returns <code>false</code>
     */
    public boolean isFailure();

    /**
     * Returns the supplied result if this instance is a success, using the provided
     * {@code supplier}; or the transformed cause contained in this instance if it is a failure,
     * using the provided {@code causeTransformation}.
     * <p>
     * This method necessarily invokes exactly one of the provided functional interfaces.
     *
     * @param <T> the type of (supplied or transformed) result to return
     * @param <Y> a type of exception that the provided functions may throw
     * @param supplier a supplier to get a result from if this instance is a success
     * @param causeTransformation a function to apply to the cause if this instance is a failure
     * @return the supplied result or transformed cause
     * @throws Y iff the functional interface that was invoked threw a checked exception
     */
    public <T, Y extends Exception> T map(Throwing.Supplier<? extends T, ? extends Y> supplier,
        Throwing.Function<? super X, ? extends T, ? extends Y> causeTransformation) throws Y;

    /**
     * If this instance is a failure, invokes the given consumer using the cause contained in this
     * instance. If this instance is a success, do nothing.
     *
     * @param <Y> a type of exception that the provided consumer may throw
     * @param consumer the consumer to invoke if this instance is a failure
     * @throws Y iff the consumer was invoked and threw a checked exception
     */
    public <Y extends Exception> void ifFailed(Throwing.Consumer<? super X, Y> consumer) throws Y;

    /**
     * If this instance is a failure, throws the cause it contains. Otherwise, do nothing.
     *
     * @throws X iff this instance contains a cause
     */
    public void orThrow() throws X;

    public <Y extends Z> void orThrow(Function<X, Y> causeTransformation) throws Y;

    /**
     * If this instance is a success, returns a try representing the result of invoking the given
     * supplier; otherwise, returns this failure.
     * <p>
     * If this instance is a failure, it is returned, without invoking the given supplier.
     * Otherwise, the given supplier is invoked. If it terminates without throwing, a success is
     * returned, containing the result just supplied by the supplier. If the supplier throws a
     * checked exception, a failure is returned, containing the cause it threw.
     *
     * @param <T> the type of result that the returned try will be declared to contain
     * @param <X> the type of cause that the returned try will be declared to contain
     * @param supplier the supplier to attempt to get a result from if this instance is a success.
     * @return a success iff this instance is a success and the given supplier terminated without
     *         throwing.
     * @see Try#get(Supplier)
     */
    public <T> TryVariableCatchInterface<T, X, Z> andGet(
        Throwing.Supplier<? extends T, ? extends X> supplier);

    /**
     * If this instance is a success, returns a {@code TryVoid} instance representing the result of
     * invoking the given runnable; otherwise, returns this failure.
     * <p>
     * If this instance is a failure, it is returned, without invoking the given runnable.
     * Otherwise, the given runnable is invoked. If it terminates without throwing, a success is
     * returned. If the runnable throws a checked exception, a failure is returned, containing the
     * cause it threw.
     *
     * @param <X> the type of cause that the returned instance will be declared to contain
     * @param runnable the runnable to attempt to run if this instance is a success.
     * @return a success iff this instance is a success and the given runnable terminated without
     *         throwing.
     * @see #run(Runnable)
     */
    public TryVoidVariableCatchInterface<X, Z> andRun(Throwing.Runnable<? extends X> runnable);

    /**
     * If this instance is a failure, returns a {@code TryVoid} instance representing the result of
     * invoking the given runnable; otherwise, returns this success.
     * <p>
     * If this instance is a success, it is returned, without invoking the given runnable.
     * Otherwise, the given runnable is invoked. If it terminates without throwing, a success is
     * returned. If the runnable throws a checked exception, a failure is returned, containing the
     * cause it threw.
     *
     * @param <X> the type of cause that the returned instance will be declared to contain
     * @param runnable the runnable to attempt to run if this instance is a failure.
     * @return a success iff this instance is a success or the given runnable terminated without
     *         throwing.
     * @see #run(Runnable)
     */
    public TryVoidVariableCatchInterface<X, Z> or(Throwing.Runnable<? extends X> runnable);

    /**
     * Returns a string representation of this object, suitable for debug.
     */
    @Override
    public String toString();

  }

  public static abstract class TryVariableCatch<T, X extends Z, Z extends Throwable>
      extends TryOptional<T, X> implements TryVariableCatchInterface<T, X, Z> {
    @Override
    public T orThrow() throws X {
      return orThrow(Function.identity());
    }

    @Override
    public String toString() {
      final ToStringHelper stringHelper = MoreObjects.toStringHelper(this);
      orConsumeCause(e -> stringHelper.add("cause", e))
          .ifPresent(r -> stringHelper.add("result", r));
      return stringHelper.toString();
    }
  }

  public static abstract class TryVariableCatchSuccess<T, X extends Z, Z extends Throwable>
      extends TryVariableCatch<T, X, Z> {

    protected final T result;

    protected TryVariableCatchSuccess(T result) {
      this.result = checkNotNull(result);
    }

    @Override
    Optional<T> getResult() {
      return Optional.of(result);
    }

    @Override
    Optional<X> getCause() {
      return Optional.empty();
    }

    @Override
    public <U, Y extends Exception> U map(
        Throwing.Function<? super T, ? extends U, ? extends Y> transformation,
        Throwing.Function<? super X, ? extends U, ? extends Y> causeTransformation) throws Y {
      return transformation.apply(result);
    }

    @Override
    public <Y extends Exception> T orMapCause(
        Throwing.Function<? super X, ? extends T, Y> causeTransformation) throws Y {
      return result;
    }

    @Override
    public <Y extends Exception> Optional<T> orConsumeCause(
        Throwing.Consumer<? super X, Y> consumer) throws Y {
      return Optional.of(result);
    }

    @Override
    public <Y extends Z> T orThrow(Function<X, Y> causeTransformation) {
      return result;
    }
  }

  public static abstract class TryVariableCatchFailure<X extends Z, Z extends Throwable>
      extends TryVariableCatch<Object, X, Z> {

    protected final X cause;

    protected TryVariableCatchFailure(X cause) {
      this.cause = checkNotNull(cause);
    }

    @Override
    Optional<Object> getResult() {
      return Optional.empty();
    }

    @Override
    Optional<X> getCause() {
      return Optional.of(cause);
    }

    @Override
    public <U, Y extends Exception> U map(
        Throwing.Function<? super Object, ? extends U, ? extends Y> transformation,
        Throwing.Function<? super X, ? extends U, ? extends Y> causeTransformation) throws Y {
      return causeTransformation.apply(cause);
    }

    @Override
    public <Y extends Exception> Object orMapCause(
        Throwing.Function<? super X, ? extends Object, Y> causeTransformation) throws Y {
      return causeTransformation.apply(cause);
    }

    @Override
    public <Y extends Exception> Optional<Object> orConsumeCause(
        Throwing.Consumer<? super X, Y> consumer) throws Y {
      consumer.accept(cause);
      return Optional.empty();
    }

    @Override
    public <Y extends Z> Object orThrow(Function<X, Y> causeTransformation) throws Y {
      throw causeTransformation.apply(cause);
    }
  }

  public static abstract class TryVoidVariableCatch<X extends Z, Z extends Throwable>
      extends TryOptional<Object, X> implements TryVoidVariableCatchInterface<X, Z> {
    @Override
    public void orThrow() throws X {
      orThrow(Function.identity());
    }


    @Override
    public String toString() {
      final ToStringHelper stringHelper = MoreObjects.toStringHelper(this);
      andRun(() -> stringHelper.addValue("success"));
      ifFailed(e -> stringHelper.add("cause", e));
      return stringHelper.toString();
    }
  }

  public static abstract class TryVoidVariableCatchSuccess<X extends Z, Z extends Throwable>
      extends TryVoidVariableCatch<X, Z> {

    protected TryVoidVariableCatchSuccess() {
      /* Reducing visibility. */
    }

    @Override
    Optional<Object> getResult() {
      return Optional.empty();
    }

    @Override
    Optional<X> getCause() {
      return Optional.empty();
    }

    @Override
    public <T, Y extends Exception> T map(Throwing.Supplier<? extends T, ? extends Y> supplier,
        Throwing.Function<? super X, ? extends T, ? extends Y> causeTransformation) throws Y {
      return supplier.get();
    }

    @Override
    public <Y extends Exception> void ifFailed(Throwing.Consumer<? super X, Y> consumer) throws Y {
      /* Nothing to do. */
    }

    @Override
    public <Y extends Z> void orThrow(Function<X, Y> causeTransformation) {
      /* Nothing to do. */
    }
  }

  public static abstract class TryVoidVariableCatchFailure<X extends Z, Z extends Throwable>
      extends TryVoidVariableCatch<X, Z> {

    protected final X cause;

    protected TryVoidVariableCatchFailure(X cause) {
      this.cause = checkNotNull(cause);
    }

    @Override
    Optional<Object> getResult() {
      return Optional.empty();
    }

    @Override
    Optional<X> getCause() {
      return Optional.of(cause);
    }

    @Override
    public <T, Y extends Exception> T map(Throwing.Supplier<? extends T, ? extends Y> supplier,
        Throwing.Function<? super X, ? extends T, ? extends Y> causeTransformation) throws Y {
      return causeTransformation.apply(cause);
    }

    @Override
    public <Y extends Exception> void ifFailed(Throwing.Consumer<? super X, Y> consumer) throws Y {
      consumer.accept(cause);
    }

    @Override
    public <Y extends Z> void orThrow(Function<X, Y> causeTransformation) throws Y {
      throw causeTransformation.apply(cause);
    }
  }

  public static class TrySuccess<T> extends TryVariableCatchSuccess<T, Exception, Exception>
      implements Try<T, Exception> {
    public static <T, X extends Exception> Try<T, X> given(T result) {
      return new TrySuccess<>(result).cast();
    }

    private TrySuccess(T result) {
      super(result);
    }

    @Override
    boolean catchesAll() {
      return false;
    }

    private <Y extends Exception> Try<T, Y> cast() {
      @SuppressWarnings("unchecked")
      final Try<T, Y> casted = (Try<T, Y>) this;
      return casted;
    }

    @Override
    public <Y extends Exception, Z extends Exception, W extends Exception> Try<T, Z> or(
        Supplier<? extends T, Y> supplier,
        BiFunction<? super Exception, ? super Y, ? extends Z, W> exceptionsMerger) throws W {
      return cast();
    }

    @Override
    public Try<T, Exception> andRun(Runnable<? extends Exception> runnable) {
      final TryVoid<? extends Exception> ran = TryVoid.run(runnable);
      return ran.map(() -> this, Try::failure);
    }

    @Override
    public Try<T, Exception> andConsume(Consumer<? super T, ? extends Exception> consumer) {
      return andRun(() -> consumer.accept(result));
    }

    @Override
    public <U, V, Y extends Exception> Try<V, Exception> and(Try<U, ? extends Exception> t2,
        BiFunction<? super T, ? super U, ? extends V, Y> merger) throws Y {
      return t2.map(u -> Try.success(merger.apply(result, u)), Try::failure);
    }

    @Override
    public <U> Try<U, Exception> flatMap(
        Throwing.Function<? super T, ? extends U, ? extends Exception> mapper) {
      return Try.get(() -> mapper.apply(result));
    }
  }
  public static class TryFailure<X extends Exception>
      extends TryOptional.TryVariableCatchFailure<X, Exception> implements Try<Object, X> {
    public static <T, X extends Exception> Try<T, X> given(X cause) {
      return new TryFailure<>(cause).cast();
    }

    private TryFailure(X cause) {
      super(cause);
    }

    @Override
    boolean catchesAll() {
      return false;
    }

    private <U> Try<U, X> cast() {
      @SuppressWarnings("unchecked")
      final Try<U, X> casted = (Try<U, X>) this;
      return casted;
    }

    @Override
    public <Y extends Exception, Z extends Exception, W extends Exception> Try<Object, Z> or(
        Throwing.Supplier<? extends Object, Y> supplier,
        Throwing.BiFunction<? super X, ? super Y, ? extends Z, W> exceptionsMerger) throws W {
      final Try<Object, Y> t2 = Try.get(supplier);
      return t2.map(Try::success, y -> Try.failure(exceptionsMerger.apply(cause, y)));
    }

    @Override
    public Try<Object, X> andRun(Throwing.Runnable<? extends X> runnable) {
      return this;
    }

    @Override
    public Try<Object, X> andConsume(Throwing.Consumer<? super Object, ? extends X> consumer) {
      return this;
    }

    @Override
    public <U, V, Y extends Exception> Try<V, X> and(Try<U, ? extends X> t2,
        BiFunction<? super Object, ? super U, ? extends V, Y> merger) throws Y {
      return cast();
    }

    @Override
    public <U> Try<U, X> flatMap(
        Throwing.Function<? super Object, ? extends U, ? extends X> mapper) {
      return cast();
    }

  }

  public static class TryVoidSuccess extends TryVoidVariableCatchSuccess<Exception, Exception>
      implements TryVoid<Exception> {
    public static <X extends Exception> TryVoid<X> given() {
      return new TryVoidSuccess().cast();
    }

    private TryVoidSuccess() {
      /* Reducing visibility. */
    }

    @Override
    boolean catchesAll() {
      return false;
    }

    private <Y extends Exception> TryVoid<Y> cast() {
      @SuppressWarnings("unchecked")
      final TryVoid<Y> casted = (TryVoid<Y>) this;
      return casted;
    }

    @Override
    public <T> Try<T, Exception> andGet(Supplier<? extends T, ? extends Exception> supplier) {
      return Try.get(supplier);
    }

    @Override
    public TryVoid<Exception> andRun(Runnable<? extends Exception> runnable) {
      return TryVoid.run(runnable);
    }

    @Override
    public TryVoid<Exception> or(Runnable<? extends Exception> runnable) {
      return this;
    }
  }

  public static class TryVoidFailure<X extends Exception>
      extends TryVoidVariableCatchFailure<X, Exception> implements TryVoid<X> {
    public static <X extends Exception> TryVoid<X> given(X cause) {
      return new TryVoidFailure<>(cause);
    }

    private TryVoidFailure(X cause) {
      super(cause);
    }

    @Override
    boolean catchesAll() {
      return false;
    }

    @Override
    public <T> Try<T, X> andGet(Supplier<? extends T, ? extends X> supplier) {
      return Try.failure(cause);
    }

    @Override
    public TryVoid<X> andRun(Runnable<? extends X> runnable) {
      return this;
    }

    @Override
    public TryVoid<X> or(Runnable<? extends X> runnable) {
      return TryVoid.run(runnable);
    }

  }


  protected TryOptional() {
    /* Reducing visibility. */
  }

  /**
   * Returns <code>true</code> iff this instance represents a success.
   *
   * @return <code>true</code> iff {@link #isFailure()} returns <code>false</code>
   */
  public boolean isSuccess() {
    return getCause().isEmpty();
  }

  /**
   * Return <code>true</code> iff this instance contains a cause.
   *
   * @return <code>true</code> iff {@link #isSuccess()} returns <code>false</code>
   */
  public boolean isFailure() {
    return getCause().isPresent();
  }

  abstract boolean catchesAll();

  abstract Optional<T> getResult();

  abstract Optional<X> getCause();

  /**
   * Returns <code>true</code> iff both instances have the same “catching” behavior, and either:
   * <ul>
   * <li>the given object is a {@link Try} and this object and the given one are both successes and
   * hold equal results;
   * <li>the given object is a {@link Try} or a {@link TryVoid} and this object and the given one
   * are both failures and hold equal causes.
   * </ul>
   */
  @Override
  public boolean equals(Object o2) {
    if (!(o2 instanceof TryOptional)) {
      return false;
    }

    final TryOptional<?, ?> t2 = (TryOptional<?, ?>) o2;
    return getResult().equals(t2.getResult()) && getCause().equals(t2.getCause())
        && (catchesAll() == t2.catchesAll());
  }

  @Override
  public int hashCode() {
    return Objects.hash(catchesAll(), getResult(), getCause());
  }

  /**
   * Returns a string representation of this object, suitable for debug.
   */
  @Override
  public abstract String toString();
}