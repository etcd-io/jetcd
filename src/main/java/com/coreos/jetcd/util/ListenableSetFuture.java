package com.coreos.jetcd.util;

import com.google.common.util.concurrent.ListenableFuture;
import javafx.util.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ListenableSetFuture, the result can be set by any thread. Before the setResult method called, the get method will be blocked.
 */
public class ListenableSetFuture<T> implements ListenableFuture<T> {

    private volatile T result = null;

    private List<Pair<Runnable, Executor>> listeners = new LinkedList<>();

    private AtomicBoolean canceled = new AtomicBoolean(false);
    private AtomicBoolean done = new AtomicBoolean(false);

    private CountDownLatch countDownLatch = new CountDownLatch(1);
    private final CancelCallable cancelCallable;

    public ListenableSetFuture(CancelCallable cancelCallable){
        this.cancelCallable = cancelCallable;
    }

    @Override
    public void addListener(Runnable runnable, Executor executor) {
        listeners.add(new Pair<>(runnable, executor));
    }

    /**
     * Attempts to cancel execution of this task.  This attempt will
     * fail if the task has already completed, has already been cancelled,
     * or could not be cancelled for some other reason. If successful,
     * and this task has not started when {@code cancel} is called,
     * this task should never run.  If the task has already started,
     * then the {@code mayInterruptIfRunning} parameter determines
     * whether the thread executing this task should be interrupted in
     * an attempt to stop the task.
     * <p>
     * <p>After this method returns, subsequent calls to {@link #isDone} will
     * always return {@code true}.  Subsequent calls to {@link #isCancelled}
     * will always return {@code true} if this method returned {@code true}.
     *
     * @param mayInterruptIfRunning {@code true} if the thread executing this
     *                              task should be interrupted; otherwise, in-progress tasks are allowed
     *                              to complete
     * @return {@code false} if the task could not be cancelled,
     * typically because it has already completed normally;
     * {@code true} otherwise
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if(cancelCallable == null || done.get()){
            return false;
        }
        if(cancelCallable.cancel()){
            canceled.set(true);
            done.set(true);
            return true;
        }else {
            return false;
        }
    }

    /**
     * Returns {@code true} if this task was cancelled before it completed
     * normally.
     *
     * @return {@code true} if this task was cancelled before it completed
     */
    @Override
    public boolean isCancelled() {
        return canceled.get();
    }

    /**
     * Returns {@code true} if this task completed.
     * <p>
     * Completion may be due to normal termination, an exception, or
     * cancellation -- in all of these cases, this method will return
     * {@code true}.
     *
     * @return {@code true} if this task completed
     */
    @Override
    public boolean isDone() {
        return done.get();
    }

    public void setResult(T result) {
        if (canceled.get() || done.get()) {
            throw new IllegalStateException();
        } else {
            synchronized (this){
                if (canceled.get() || done.get()) {
                    throw new IllegalStateException();
                }
                this.result = result;
                done.set(true);
                countDownLatch.countDown();
            }
            runCompleteListener();
        }

    }

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     *
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException    if the computation threw an
     *                               exception
     * @throws InterruptedException  if the current thread was interrupted
     *                               while waiting
     */
    @Override
    public T get() throws InterruptedException, ExecutionException {

        if (canceled.get()) {
            throw new CancellationException();
        }

        if (done.get()) {
            return result;
        }

        countDownLatch.await();
        if(canceled.get()){
            new CancellationException();
        }
        return result;
    }

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result, if available.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException    if the computation threw an
     *                               exception
     * @throws InterruptedException  if the current thread was interrupted
     *                               while waiting
     * @throws TimeoutException      if the wait timed out
     */
    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (canceled.get()) {
            throw new CancellationException();
        }

        if (done.get()) {
            return result;
        }

        if (!countDownLatch.await(timeout, unit)) {
            throw new TimeoutException();
        }

        return result;
    }

    private void runCompleteListener(){
        for(Pair<Runnable, Executor> runPair: listeners){
            runPair.getValue().execute(runPair.getKey());
        }
    }


    public interface CancelCallable{
        boolean cancel();
    }

}
