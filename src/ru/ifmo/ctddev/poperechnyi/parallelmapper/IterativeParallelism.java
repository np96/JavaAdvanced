package ru.ifmo.ctddev.poperechnyi.parallelmapper;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;


public class IterativeParallelism implements ListIP {
    private ParallelMapper mapper;

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }
    /**
     *
     * @param threads
     * @param values
     * @return
     * @throws InterruptedException
     */
    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return String.join("", "", parallelWork(threads, values,
                new Monoid<>(StringBuilder::new, StringBuilder::append), (a) -> new StringBuilder(a.toString())));
    }

    /***
     * Creates new list of elements matching {@code predicate} criteria
     *
     * @param threads   Number of threads. {@code 1} if the argument value is less than {@code 1}.
     * @param values    {@link List} to filter
     * @param predicate Filtering predicate. See {@link Predicate Predicate}
     * @return New list of elements for which {@code predicate} returns true.
     * @throws InterruptedException
     */

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        Monoid<List<T>> monoid = new Monoid<>(ArrayList::new, (a, b) -> {
            a.addAll(b);
            return a;
        });
        return parallelWork(threads, values, monoid, (a) -> predicate.test(a) ? Collections.singletonList(a) : Collections.emptyList());
    }

    /***
     * Makes new list of {@code f} applied to values
     *
     * @param threads Number of threads. {@code 1} if the argument value is less than {@code 1}
     * @param values  {@link List} to filter
     * @param f       Mapping function. {@link Function Function}
     * @return New List of {@code f(a)}
     * @throws InterruptedException
     */

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        Monoid<List<U>> monoid = new Monoid<>(ArrayList::new, (a, b) -> {
            a.addAll(b);
            return a;
        });
        return parallelWork(threads, values, monoid, (a) -> Collections.singletonList(f.apply(a)));
    }

    /***
     * Returns maximum in {@param values} according to comparator
     *
     * @param threads    Number of threads. {@code 1} if the argument value is less than {@code 1}
     * @param values     {@link List} where to find the maximum
     * @param comparator Specified comparator
     * @return Maximum in list for specified {@link Comparator comparator}
     * @throws InterruptedException
     * @throws IllegalArgumentException if {@param values} is empty
     */

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        if (values.size() == 0) throw new IllegalArgumentException("Empty list");
        Monoid<T> monoid = new Monoid<>(() -> values.get(0), (a, b) -> (comparator.compare(a, b) >= 0) ? a : b);
        return parallelWork(threads, values, monoid, Function.identity());
    }

    /**
     * Returns minimum in {@param values} according to {@param comparator}. Actually returns maximum for reversed {@link Comparator}. See {@link #maximum maximum}
     *
     * @throws InterruptedException
     * @throws IllegalArgumentException
     */

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    /**
     * Checks whether all of the elements in specified {@code List} meet the criteria of specified {@code predicate}
     *
     * @param threads   Number of threads. {@code 1} if the argument value is less than {@code 1}
     * @param values    List where to find
     * @param predicate Checking predicate. See {@link Predicate Predicate}
     * @return True if {@code predicate::test} returns true for all elements in {@code values}
     * @throws InterruptedException
     */

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        Monoid<Boolean> monoid = new Monoid<>(() -> Boolean.TRUE, Boolean::logicalAnd);
        return parallelWork(threads, values, monoid, predicate::test);
    }

    /**
     * Checks whether any of the elements in specified {@code List} meet the criteria of specified {@code predicate}
     *
     * @param threads   Number of threads. {@code 1} if the argument value is less than {@code 1}
     * @param values    List where to find
     * @param predicate Checking predicate. See {@link Predicate Predicate}
     * @return True if {@code predicate::test} returns true for any element in {@code values}
     * @throws InterruptedException
     */

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }

    private <T, R> R parallelWork(int threads, final List<? extends T> values, final Monoid<R> monoid, final Function<? super T, ? extends R> function) throws InterruptedException {
        threads = (int)Math.max(1,threads/1.8);
        if (values.size() == 0) {
            return monoid.get();
        }
        threads = Math.max(Math.min(threads, values.size()), 1);
        int length = values.size() / threads + 1;
        List<List<? extends T>> tasks = new ArrayList<>();
        for (int i = 0; i < values.size(); i += length) {
            tasks.add(values.subList(i, Math.min(i + length, values.size())));
        }
        final List<R> results = mapper.map(t -> {
            R accumulator = monoid.get();
            for (T element : t) {
                accumulator = monoid.operate(accumulator, function.apply(element));
            }
            return accumulator;
        }, tasks);
        R acc = monoid.get();
        for (R result : results) {
            acc = monoid.operate(acc, result);
        }
        return acc;
    }

    private static class Monoid<T> {
        private final Supplier<T> supplier;
        private final BinaryOperator<T> operator;

        public Monoid(Supplier<T> supplier, BinaryOperator<T> operator) {
            this.supplier = supplier;
            this.operator = operator;
        }

        T get() {
            return supplier.get();
        }

        T operate(T a, T b) {
            return operator.apply(a, b);
        }
    }
}