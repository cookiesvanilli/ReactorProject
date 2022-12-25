package org.example;

import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Mono.empty();
        Flux.empty();

        Mono<Integer> mono = Mono.just(1);
        Flux<Integer> flux = Flux.just(1, 2, 3);

        Flux<Integer> fluxFromMono = mono.flux();
        Mono<Boolean> monoFromFlux = flux.any(s -> s.equals(1));
        Mono<Integer> integerMono = flux.elementAt(1);

        Flux.range(1, 5); //.subscribe(System.out::println)
        Flux.fromIterable(Arrays.asList(1, 2, 3)); //.subscribe(System.out::println)

        Flux.<String>generate(sink ->
                {
                    sink.next("hello");
                })
                .delayElements(Duration.ofMillis(500))
                .take(4);
        // .subscribe(System.out::println);

        Flux<Object> telegramProducer =
                Flux.generate(
                        () -> 2345,
                        (state, sink) -> {
                            if (state > 2366) {
                                sink.complete(); //break
                            } else {
                                sink.next("Step " + state);
                            }
                            return state + 3;
                        }
                );
        // .subscribe(System.out::println);


        //create - многопоточный, а push - однопоточный
        Flux.create(
                sink -> {
                    // 1 method - pull из внешнего сервера
                    sink.onRequest(r -> {
                        sink.next("DB returns " + telegramProducer.blockFirst());
                    });
                    //2 method - hooks
                    telegramProducer.subscribe(new BaseSubscriber<Object>() {
                        @Override
                        protected void hookOnNext(Object value) {
                            sink.next(value);
                        }

                        @Override
                        protected void hookOnComplete() {
                            sink.complete();
                        }
                    });
                }); //.subscribe(System.out::println);

        Flux<String> second =
                Flux
                        .just("World", "coder")
                        .repeat();

        Flux<String> sumFlux =
                Flux
                        .just("hello", "java", "Asia", "java", "Linux")
                        .zipWith(second, (f, s) -> String.format("%s %s", f, s)); //сшиваем

        Flux<String> stringFlux =
                sumFlux
                        .delayElements(Duration.ofMillis(1500))
                        .timeout(Duration.ofSeconds(1)) // отвалился по времени
                        // .retry(3) // 3 попытки
                        //.onErrorReturn("Too slow") // обрабатывает ошибку

                        .onErrorResume(throwable ->
                                Flux
                                        .interval(Duration.ofMillis(300))
                                        .map(String::valueOf)
                        )
                        .skip(2)
                        .take(5);
        // .subscribe(System.out::println);

        stringFlux.subscribe(
                v -> System.out.println(v),
                er -> System.err.println(er),
                () -> System.out.println("Finish")
        );

        Thread.sleep(4000);

    }
}