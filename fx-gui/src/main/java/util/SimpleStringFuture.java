package util;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A very lightweight, thread-safe implementation of {@link Future} for a single String result.
 * <p>
 * Supports setting a value exactly once or cancelling the future.
 * Provides blocking {@code get()} methods but no chaining or async execution like {@link CompletableFuture}.
 */
public class SimpleStringFuture implements Future<String> {
    private final CountDownLatch latch = new CountDownLatch(1);
    private volatile String value;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicBoolean completed = new AtomicBoolean(false);
    
    /**
     * Completes this future with the given value, if it has not already been completed or cancelled.
     *
     * @param value the result value
     * @return true if this call successfully completed the future, false if it was already completed or cancelled
     */
    public boolean complete(String value) {
        if (completed.compareAndSet(false, true)) {
            this.value = value;
            latch.countDown();
            return true;
        }
        return false;
    }
    
    /**
     * Cancels this future, if it has not already been completed or cancelled.
     *
     * @param mayInterruptIfRunning ignored (no execution thread is associated with this future)
     * @return true if this call successfully cancelled the future, false if it was already completed or cancelled
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (completed.compareAndSet(false, true)) {
            cancelled.set(true);
            latch.countDown();
            return true;
        }
        return false;
    }
    
    /**
     * Returns whether this future was cancelled.
     *
     * @return true if cancelled, false otherwise
     */
    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }
    
    /**
     * Returns whether this future has been completed (either successfully or via cancellation).
     *
     * @return true if completed or cancelled, false otherwise
     */
    @Override
    public boolean isDone() {
        return completed.get();
    }
    
    /**
     * Waits if necessary for the computation to complete, and then retrieves the result.
     *
     * @return the computed string value
     * @throws InterruptedException  if the current thread was interrupted while waiting
     * @throws CancellationException if the future was cancelled before completion
     */
    @Override
    public String get() throws InterruptedException {
        latch.await();
        if (cancelled.get()) throw new CancellationException();
        return value;
    }
    
    /**
     * Waits if necessary for at most the given time for the computation to complete, and then retrieves the result.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @return the computed string value
     * @throws InterruptedException  if the current thread was interrupted while waiting
     * @throws TimeoutException      if the wait timed out
     * @throws CancellationException if the future was cancelled before completion
     */
    @Override
    @SuppressWarnings("NullableProblems")
    public String get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        if (unit == null) throw new NullPointerException();
        if (!latch.await(timeout, unit)) throw new TimeoutException();
        if (cancelled.get()) throw new CancellationException();
        return value;
    }
}
