package ru.ifmo.ctddev.poperechnyi.parallelmapper;


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Function;


public class ParallelMapperImpl implements info.kgeorgiy.java.advanced.mapper.ParallelMapper {
    final private Thread[] threads;
    final private Queue<Consumer<Void>> queue;
    private boolean interrupted = false;

    public ParallelMapperImpl(int threads) {
        queue = new ArrayDeque<>();
        this.threads = new Thread[threads];
        for (int i = 0; i < threads; ++i) {
            this.threads[i] = new Thread(() -> {
                while (!interrupted) {
                    Consumer<Void> data = null;
                    synchronized (queue) {
                        if (!queue.isEmpty()) {
                            data = queue.poll();
                        }
                    }
                    if (data != null) {
                        data.accept(null);
                        synchronized (queue) {
                            queue.notifyAll();
                        }
                    } else
                    if (interrupted) {
                        return;
                    } else
                        synchronized (queue) {
                            try {
                                queue.wait();
                            } catch (InterruptedException e) {
                                return;
                            }
                        }
                }
            });
            this.threads[i].start();
        }
    }

    /**
     *
     * @param f function mapping member of type T to member of type R
     * @param args List of arguments of type T to map
     * @return mapped list of type R
     * @throws InterruptedException
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {

        List<R> result = new ArrayList<>();
        for (int i = 0; i < args.size(); ++i) {
            result.add(null);
        }
        final int[] counter = {0};

        for (int i = 0; i < args.size(); ++i) {
            final int current = i;
            synchronized (queue) {
                queue.add(aVoid ->  {
                    result.set(current, f.apply(args.get(current)));
                    synchronized (counter) {
                        counter[0]++;
                    }
                });

            }
        }
        synchronized (queue) {
            queue.notifyAll();
            while (counter[0] < args.size()) {
                queue.wait();
            }
        }
        return result;
    }

    @Override
    public void close() throws InterruptedException {
        interrupted = true;
        for (Thread thread : threads) {
            thread.interrupt();
            thread.join();
        }
    }
}
