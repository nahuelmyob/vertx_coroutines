# Introduction

The objectives of this project is showcase a simple webapp using Kotlin, Vertx and its wrapper for coroutines and compare it to
the default way of using Vertx (i.e: callbacks), this way we can analyze the pros of writing async code in a sync fashion.

Although async frameworks like Vertx offer a better performance without having to deal with kernel threads they may exhibit some
complications at testing level, async frameworks relay on events/callbacks interactions causing your code to grow
horizontally and in most cases depending on objects that are only available due to closures.

```
 var myVar = 1;
 someAction({
    myVar++
    someOtherAction({
        myVar++
        yetAnotherAction({...
...
```

Kotlin offers async support natively by a feature called Coroutines, Coroutines simplify the writing of async code and Vertx provides
a wrapper to use them.

# Getting Started

Under `/src/main/kotlin` we can find 2 classes that are effectively 2 different implementations of the same webapp.
* AbstractVerticleServer implements the default callback-like structure of Vertx.
* CoroutineVerticleServer implements some of the coroutine features from Vertx.

To start AbstractVerticleServer: `./gradlew run`

To start CoroutineVerticleServer: `./gradlew run -Pcoroutine`

# Complementary reading

* [Coroutines](https://github.com/Kotlin/kotlin-coroutines/blob/master/kotlin-coroutines-informal.md#coroutines-overview)
* [Vertx](http://vertx.io/docs/vertx-core/kotlin/)
* [Vertx Coroutines](http://vertx.io/docs/vertx-lang-kotlin-coroutines/kotlin/)

## Coroutines

Coroutines are an experimental Kotlin feature to write async code in synchronous fashion, a coroutine is basically
a light-weight thread that will pause / continue its execution without having to be bound to a real kernel thread.

To start a coroutine we need to use coroutine builders, coroutine builders are provided with a lambda that may
or may not return a result when its execution ends.

When a coroutine executes a function marked as "suspend" it will be suspended until the execution of the function
finishes.

Suspend functions can only be executed inside a coroutine context defined during the declaration of the coroutine builder.

Coroutine context is a persistent set of user-defined objects that can be attached to the coroutine.

```
launch(myCoroutineContext,{
    val res = getSomethingAsync() //where this function is a 'suspend' function
})
```

### Common coroutine builders

#### Launch
The `launch{}` builder launches a coroutine and forgets about it, no result is returned, it either succeed or throws an exception,
for this reason this builder can be used as wrapper for other coroutine builders or top-level coroutine builder.
Exception handling should be performed inside its lambda.
```
launch {
    try {
        val response = getSomethingFromServerA() //this is a suspend function
        switch(response.statusCode()) {
                200 -> {
                    val anotherResponse = getSomethingFromServerB() //this is a suspend function
                    ...
                }
                else -> {

                }
            }
    }catch(e : Exception) {
        ...
    }
}
```

#### Async
The `async{}` builder will return a result wrapped in a Deferred object and the user can't forget about it. In order to obtain the
result he has to `await` for the result.

The deferred object not only contains the result but also the exception that could have happened during the execution.

```
val statusCode : Deferred<Int> = async<Int> {
    val response = getSomethingFromServerA()
    return response.statusCode //this is the Int we are expecting
}

if(statusCode.await() == 200) { //profit
...
```

#### RunBlocking
The `runBlocking{}` builder should not be used at any part of the code but tests, whatever is executed inside blocks the kernel threads.

```
runBlocking {
    val response = getSomethingFromServerA()
    assertThtat(200).toBeEqual(response.statusCode())
}
```

#### SuspendCoroutine
The `suspendCoroutine{}` builder provides the user with a way of handling when to resume the coroutine execution manually. This
builder should be used to wrap callback-like implementations

```
suspend fun getSomethingFromServer() = suspendCoroutine { continuation ->
   httpClient.get("somewhere.com",{ response ->
      if(response.statusCode() == 200) {
        continuation.resume(response)
      }else{
        continuation.resumeWithException(Exception(response))
      }
   })
}
```

## Vertx

Vertx is almost completely async, it uses an event driven model to perform the operations passed in form of handlers, handlers
are executed using a thread called `event loop`.


Most operations in Vertx look like:
```
    vertx.someOperation().withConfig(config).do({context -> doSomething())
```

Vertx generates and manages multiple `event loops` (by default one per core), any particular handler will never be executed
concurrently, and in most cases will always be called using the exact same event loop.

### Vertx and the problems with sync operations
In general we should avoid using sync operations but most libraries are synchronous, if we simply execute
sync operations without any precaution they will block the `event loops` causing performance issues.

### Vertx and its support for coroutines
Vertx comes with a reduced but powerful set of wrappers that allow the usage of coroutines to write async code in a sync way.
