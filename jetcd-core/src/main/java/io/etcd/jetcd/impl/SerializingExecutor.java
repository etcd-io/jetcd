package io.etcd.jetcd.impl;

import java.util.ArrayDeque;
import java.util.Queue;

/*
 * An executor that ensures tasks are executed serially.
 * WARNING: Not thread-safe. This executor must only be used from a single Vert.x context.
 */
public class SerializingExecutor {
    private final Queue<Runnable> queue = new ArrayDeque<>();
    private boolean running = false;

    public void execute(Runnable task) {
        queue.offer(task);
        if (!running) {
            running = true;
            scheduleNext();
        }
    }

    private void scheduleNext() {
        Runnable next;
        next = queue.poll();
        if (next == null) {
            running = false;
            return;
        }
        try {
            next.run();
        } finally {
            scheduleNext();
        }
    }
}

