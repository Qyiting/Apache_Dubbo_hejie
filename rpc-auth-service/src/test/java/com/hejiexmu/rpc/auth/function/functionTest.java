package com.hejiexmu.rpc.auth.function;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.*;

/**
 * @author 何杰
 * @version 1.0
 */
public class functionTest {

    @Test
    public void test() {
        Predicate<String> islongerThan = s -> s.length() > 3;
        System.out.println(islongerThan.test("hello"));
        System.out.println(islongerThan.test("hel"));
    }

    @Test
    public void funcTest() {
        Function<String, Integer> toLength = s -> s.length();
        System.out.println(toLength.apply("hello"));
    }
    @Test
    public void consumerTest() {
        Consumer<String> print = s -> System.out.println("内容为：" + s);
        print.accept("hello");
    }
    @Test
    public void supplierTest() {
        Supplier<Double> random = () -> Math.random();
        System.out.println(random.get());
    }
    @Test
    public void unaryTest() {
        UnaryOperator<Integer> square = x -> x * x;
        System.out.println(square.apply(5));
    }
    @Test
    public void binaryTest() {
        BinaryOperator<Integer> add = (x, y) -> x + y;
        System.out.println(add.apply(1, 2));
    }
    @Test
    public void bifuncTest() {
        BiFunction<String, Integer, String> repeat = (s, n) -> s.repeat(n);
        System.out.println(repeat.apply("hello", 3));
    }
    @Test
    public void bipredicateTest() {
        BiPredicate<String, Integer> islong = (s, n) -> s.length() == n;
        System.out.println(islong.test("hello", 3));
    }

    @Test
    public void biconsumerTest() {
        BiConsumer<String, Integer> print = (s, n) -> System.out.println(s.repeat(n));
        print.accept("hi", 3);
    }
    @Test
    public void andthen() {
        Function<String, Integer> toLength = s -> s.length();
        Function<Integer, Integer> square = x -> x * x;
        Function<String, Integer> compose = toLength.andThen(square);
        System.out.println(compose.apply("hello"));
    }
    @Test
    public void compose() {
        Function<Integer, Integer> addone = x -> x + 1;
        Function<String, Integer> toLength = s -> s.length();
        Function<String, Integer> compose = addone.compose(toLength);
        System.out.println(compose.apply("hello"));
    }
    @Test
    public void identity() {
        Function<String, String> identity = Function.identity();
        System.out.println(identity.apply("nihao"));
    }

    @Test
    public void zonghe() {
        Supplier<Set> setSupplier = HashSet::new;
        Set set = setSupplier.get();
        set.add("hello");
        System.out.println(set);
    }
}
