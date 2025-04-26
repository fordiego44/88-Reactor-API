package com.diego.curso.springboot.reactor.springboot_reactor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.diego.curso.springboot.reactor.springboot_reactor.models.Comments;
import com.diego.curso.springboot.reactor.springboot_reactor.models.User;
import com.diego.curso.springboot.reactor.springboot_reactor.models.UserComments;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class SpringbootReactorApplication implements CommandLineRunner  {

	private static final Logger log = LoggerFactory.getLogger(SpringbootReactorApplication.class);
	
	public static void main(String[] args) {
		SpringApplication.run(SpringbootReactorApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception { 

		userCommentsZipWith2();
		 
	}

    private void backPressure() {

        Flux.range(1, 10)
                .log()
                .limitRate(5)
                .subscribe(value -> log.info(value.toString()));
//                .subscribe(new Subscriber<Integer>() {
//                    private Subscription subscription;
//                    private final int limit = 5;
//                    private int consumed = 0;
//                    @Override
//                    public void onSubscribe(Subscription subscription) {
//                        this.subscription = subscription;
//                        this.subscription.request(limit);
//                    }
//
//                    @Override
//                    public void onNext(Integer element) {
//                        log.info(element.toString());
//                        consumed++;
//                        if(consumed == limit){
//                            consumed = 0;
//                            this.subscription.request(limit);
//
//                        }
//                    }
//
//                    @Override
//                    public void onError(Throwable throwable) {
//
//                    }
//
//                    @Override
//                    public void onComplete() {
//                        log.info("hemos finalizado el flujo Flux!");
//                    }
//                }
//                );
    }

    private void intervalFromCreate() {

        Flux.create(emitter -> {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                private Integer counter = 0;

                @Override
                public void run() {
                    emitter.next(++counter);
//                    if(counter.equals(10)){
//                        timer.cancel();
//                        emitter.complete();
//                    }

                    if(counter.equals(5)){
                        timer.cancel();
                        emitter.error(new InterruptedException("Error, se ha detenido el flux en 5"));
                    }
                }
            }, 1000, 1000);
        })
                .doOnTerminate(() -> log.info("Hemos terminado"))
                .retry(2)
                .subscribe(next -> log.info(next.toString()),
                error -> log.error(error.getMessage()),
                        () -> log.info("Hemos completado correctamente!"));
    }

    private void intervalInfinite() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Flux.interval(Duration.ofSeconds(1))
                .doOnTerminate(countDownLatch::countDown)
                .flatMap(value -> {
                    if(value == 5){
                        return Flux.error(new InterruptedException("Solo hasta 5"));
                    }
                    return Flux.just(value);
                })
                .map(i -> "Hola ".concat(i.toString()))
                .retry(2)
                .subscribe(log::info, error -> log.error(error.getMessage()));

        countDownLatch.await();
    }

    private void delayElements() throws InterruptedException {
        CountDownLatch counter = new CountDownLatch(1);
        Flux<Integer> range = Flux.range(1, 12)
                .delayElements(Duration.ofSeconds(2))
                .doOnNext(value -> log.info(value.toString()))
                .doOnTerminate(counter::countDown);
        range.subscribe();

        counter.await();
        //TimeUnit.SECONDS.sleep(26);
//        Thread.sleep(26000L);
    }

    private void interval() {
        Flux<Integer> range = Flux.range(1, 12);
        Flux<Long> delay = Flux.interval(Duration.ofSeconds(1));
        range.zipWith(delay, (first, second) -> first)
                .doOnNext(value -> log.info(value.toString()))
                .blockLast();
    }

    private void zipRange() {
        Flux<Integer> range = Flux.range(0, 5);
        Flux<Integer> numbers = Flux.just(1, 2, 3, 4, 5);

        numbers
                .map(integer -> integer * 2)
                .zipWith(range, (first, second) -> String.format("Primer Flux: %d, Segundo Flux: %d", first, second))
                .subscribe(log::info);
    }
    private User createUser(){
        return new User("John", "Doe");
    }

    private void userCommentsZipWith2(){
        Mono<User> userMono = Mono.fromCallable(this::createUser);
        Mono<Comments> commentsMono = Mono.fromCallable(() -> {
            Comments comments = new Comments();
            comments.addComment("Hola pepe, que tal!");
            comments.addComment("Mañana voy a la playa!");
            comments.addComment("Estoy en la seccion de spring con reactor!");
            return comments;
        });

        Mono<UserComments> userCommentsMono = userMono
                .zipWith(commentsMono)
                .map(tuple -> {
                    User user = tuple.getT1();
                    Comments comments = tuple.getT2();
                    return new UserComments(user, comments);
                });

        userCommentsMono.subscribe(System.out::println);
    }

    private void userCommentsZipWith(){
        Mono<User> userMono = Mono.fromCallable(this::createUser);
        Mono<Comments> commentsMono = Mono.fromCallable(() -> {
            Comments comments = new Comments();
            comments.addComment("Hola pepe, que tal!");
            comments.addComment("Mañana voy a la playa!");
            comments.addComment("Estoy en la seccion de spring con reactor!");
            return comments;
        });

        Mono<UserComments> userCommentsMono = userMono.zipWith(commentsMono, UserComments::new);
        userCommentsMono.subscribe(System.out::println);
    }
    private void userCommentsFlatmap(){

        Mono<User> userMono = Mono.fromCallable(this::createUser);
        Mono<Comments> commentsMono = Mono.fromCallable(() -> {
            Comments comments = new Comments();
            comments.addComment("Hola pepe, que tal!");
            comments.addComment("Mañana voy a la playa!");
            comments.addComment("Estoy en la seccion de spring con reactor!");
            return comments;
        });

        Mono<UserComments> userCommentsMono = userMono
                .flatMap(user -> commentsMono
                        .flatMap(comments -> Mono.fromCallable(() -> new UserComments(user, comments))));

        userCommentsMono.subscribe(userComments -> log.info(userComments.toString()));
    }
	
    private void collectList(){
        List<User> userList = Arrays.asList(new User("Andres", "Guzman"),
                new User("Diego", "Fulano"),
                new User("Maria", "Fulana"),
                new User("Pedro", "Mengano"),
                new User("Bruce", "Doe"),
                new User("Juan", "Perengano"),
                new User("Bruce", "Sultano"));

        Mono<List<User>> names = Flux.fromIterable(userList)
//                .flatMap(user -> Mono.just(user.getName().concat(" ").concat(user.getLastname())))
                .collectList();

                names.subscribe(list -> list.forEach(System.out::println));


    }

    private void flatMapToString() {
        List<User> userList = new ArrayList<>();
        userList.add(new User("Andres", "Guzman"));
        userList.add(new User("Diego", "Fulano"));
        userList.add(new User("Maria", "Fulana"));
        userList.add(new User("Pedro", "Mengano"));
        userList.add(new User("Bruce", "Doe"));
        userList.add(new User("Juan", "Perengano"));
        userList.add(new User("Bruce", "Sultano"));

        Flux.fromIterable(userList)
                .flatMap(user -> Mono.just(user.getName().concat(" ").concat(user.getLastname())))
				.flatMap(user -> {
                    if(user.toLowerCase().contains("bruce")){
                        return Mono.just(user);
                    }
                    return Mono.empty();
                })
				.map(String::toUpperCase)
                .subscribe(log::info);
    }

    private void flatMap() {
        List<String> userList = new ArrayList<>();
        userList.add("Andres Guzman");
        userList.add("Diego Fulano");
        userList.add("Maria Fulana");
        userList.add("Pedro Mengano");
        userList.add("Bruce Doe");
        userList.add("Juan Perengano");
        userList.add("Bruce Sultano");

        Flux<String> names = Flux.fromIterable(userList);

		Flux<User> users = names
                .map(name ->  new User(name.split(" ")[0], name.split(" ")[1]))
				.flatMap(user -> {
                    if(user.getName().equalsIgnoreCase("bruce")){
                        return Mono.just(user);
                    }
                    return Mono.empty();
                })
				.map(user -> {
					String lastnameUpperCase = user.getLastname().toUpperCase();
					user.setLastname(lastnameUpperCase);
					user.setCreatedAt(LocalDateTime.now());
					return user;
				});

		users.subscribe(
				(user) -> log.info(user.toString()));
    }

    private void fromIterable() {
        List<String> userList = new ArrayList<>();
        userList.add("Andres Guzman");
        userList.add("Diego Fulano");
        userList.add("Maria Fulana");
        userList.add("Pedro Mengano");
        userList.add("Bruce Doe");
        userList.add("Juan Perengano");
        userList.add("Bruce Sultano");

        Flux<String> names = Flux.fromIterable(userList);

		Flux<User> users = names.map(name ->  new User(name.split(" ")[0], name.split(" ")[1]))
				.doOnNext(System.out::println)
				.filter(user -> user.getName().equalsIgnoreCase("bruce"))
				.doOnNext(user -> {
                    if (user.getName().isEmpty()) {
                        throw new RuntimeException("Nombres no pueden ser vacios!");
                    }
                    System.out.println(user.getName().length());
                })
				.map(user -> {
					String lastnameUpperCase = user.getLastname().toUpperCase();
					user.setLastname(lastnameUpperCase);
					user.setCreatedAt(LocalDateTime.now());
					return user;
				});

		users.subscribe(
				(user) -> log.info(user.toString()),
				error -> log.error(error.getMessage()),
				() -> log.info("El flujo ha finalizado correctamente del Observable"));
    }


	private void mapAndFilter3User() {
        Flux<User> names = Flux.just("Andres Guzman", "Diego Fulano", "Maria Fulana", "Pedro Mengano",  "Bruce Doe", "Juan Perengano", "Bruce Sultano")
				.map(name ->  new User(name.split(" ")[0], name.split(" ")[1]))
				.filter(user -> user.getName().length() == 5)
				.doOnNext(System.out::println)
				.filter(user -> user.getName().equalsIgnoreCase("bruce"))
				.doOnNext(user -> {
                    if (user.getName().isEmpty()) {
                        throw new RuntimeException("Nombres no pueden ser vacios!");
                    }
                    System.out.println(user.getName().length());
                })
				.map(user -> {
					String lastnameUpperCase = user.getLastname().toUpperCase();
					user.setLastname(lastnameUpperCase);
					user.setCreatedAt(LocalDateTime.now());
					return user;
				});

		names.subscribe(
				(user) -> log.info(user.toString()),
				error -> log.error(error.getMessage()),
				() -> log.info("El flujo ha finalizado correctamente del Observable"));
    }

	private void mapAndFilter2Integer() {
        Flux<Integer> names = Flux.just("Andres", "Diego", "Maria", "Pedro",  "Bruce", "Juan", "Bruce")
				.map(String::length)
                .doOnNext(length -> {
                    if (length == 0) {
                        throw new RuntimeException("Nombres no pueden ser vacios!");
                    }
                    System.out.println(length);
                });

		names.subscribe(
				(value) -> log.info(value.toString()),
				error -> log.error(error.getMessage()),
				() -> log.info("El flujo ha finalizado correctamente del Observable"));
    }

	private void mapAndFilter() {
        Flux<String> names = Flux.just("Andres", "Diego", "Maria", "Pedro", "Juan", "Bruce")
				.map(String::toUpperCase)
                .doOnNext(name -> {
                    if (name.isBlank()) {
                        throw new RuntimeException("Nombres no puedes ser vacios!");
                    }
                    System.out.println(name.toLowerCase());
                });

		names.subscribe(
				log::info,
				error -> log.error(error.getMessage()),
				() -> log.info("El flujo ha finalizado correctamente del Observable"));
    }

    private void subscribeAndOnNext() {
        Flux<String> names = Flux.just("Andres", "Diego", "Maria", "Pedro", "Juan", "Bruce")
                .doOnNext(System.out::println)
                .doOnNext(name -> System.out.println(name.toUpperCase()))
                .doOnNext(name -> {
                    if (name.isBlank()) {
                        throw new RuntimeException("Nombres no puedes ser vacios!");
                    }
                    System.out.println(name.toLowerCase());
                });

        names.subscribe(
                log::info,
                error -> log.error(error.getMessage()),
                () -> log.info("El flujo ha finalizado correctamente del Observable"));

//		names.subscribe(new Subscriber<String>() {
//			@Override
//			public void onSubscribe(Subscription subscription) {
//				subscription.request(2);
//			}
//
//			@Override
//			public void onNext(String s) {
//				log.info(s);
//			}
//
//			@Override
//			public void onError(Throwable error) {
//				log.error(error.getMessage());
//			}
//
//			@Override
//			public void onComplete() {
//				log.info("El flujo ha finalizado correctamente del Observable");
//			}
//		});
    }
}	

