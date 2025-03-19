# Listen and Notify

[listen]: https://www.postgresql.org/docs/current/sql-listen.html
[notify]: https://www.postgresql.org/docs/current/sql-notify.html
[unlisten]: https://www.postgresql.org/docs/current/sql-unlisten.html

PostgreSQL ships a very simple pub-sub messaging system driven by the `LISTEN`
and `NOTIFY` commands. Briefly, a client sends messages to a certain channel,
and another client listens to that channel and receives these messages. All
together, clients may organize some sort of a message queue, process
asynchronous events, notifications, and so on.

Although it sounds promising, the "listen & notify" functionality has
limitations, namely:

- Notifications are always bound to a certain database. A client connected to a
  database A cannot notify nor listen to channels from a database B.

- Notifications can carry only text. For arbitrary data, use either JSON or any
  binary encoding library with base64 post-encoding.

- The size of a message must not exceed 8 Kbytes.

- **The most important:** when a client starts listening to a channel, he or she
  **won't receive** messages sent to the channel **before** they have started to
  listen. Only messages sent **after** this will be delivered.

- **Also important:** a listening client should regularly poll a database for
  notifications.

The list above is incomplete, and before you start crafting asynchronous message
processing on top of PostgreSQL, please refer to the official documentation and
mailing lists. The following pages describe the pub-sub framework quite well:

- [SQL LISTEN command][listen]
- [SQL NOTIFY command][notify]
- [SQL UNLISTEN command][unlisten]

## PG2 implementation

PG2 supports two ways of processing notifications:

1. to collect them into an internal storage without processing, and then you
   drain them and process as you want;
2. to process them in the background with an executor and a custom function.

Let's review both in separate chapters.

### Draining Notifications Manually

The first way doesn't require any configuration. Just connect to a database and
start listening to a certain channel:

~~~clojure
(def channel-1 "test-01")

(def conn-A (pg/connect config))

(pg/listen conn-A channel-1)
~~~

Now let's notify this channel from another connection called `conn-B`:

~~~clojure
(def conn-B (pg/connect config))

(pg/notify conn-B channel-1 "Hello!")
~~~

To check if `conn-A` has received this notification, pass it into the
`has-notifications?` function:

~~~clojure
(pg/has-notifications? conn-A)
;; false
~~~

It has not so far because there wasn't any interaction with the server. Let's
perform a trivial query from `conn-A` so it reaches the server and receives a
pending notification. Now it has notifications:

~~~clojure
(pg/query conn-A "select 1 as num")

(pg/has-notifications? conn-A)
;; true
~~~

The function `drain-notifications` clears the inner storage and returns a vector
of notifications. Afterwards, the connection doesn't have these notifications
any longer:

~~~clojure
(pg/drain-notifications conn-A)

[{:channel "test-01",
  :msg :NotificationResponse,
  :self? false,
  :pid 3630,
  :message "Hello!"}]

(pg/has-notifications? conn-A)
;; false
~~~

Every notification is a map with the following fields:

| Field      | Meaning                                                                                                                                              | Example                 |
|------------|------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------|
| `:channel` | The name of a channel this notification came from                                                                                                    | `"chat_messages"`       |
| `:msg`     | Type of a message in terms of PG Wire protocol                                                                                                       | `:NotificationResponse` |
| `:self?`   | True if a sender and a receiver are the same. Sometimes, it's important to check if a message was triggered by the current connection and ignore it. | `true` or `false`       |
| `:pid`     | The PID number of a connection that produced that message. See `pg.core/pid` function                                                                | 12345                   |
| `:message` | The payload of a notification as a string.                                                                                                           | `"Hello World!"`        |

It's up to you how to process these maps: either you send them somewhere, or use
any kind async framework, or emit new notification, or whatever else.

## Processing Notifications Automatically

Draining notifications manually is inconvenient sometimes. There is a way to
pass a function called every time a notification arrives from the server. For
this, specify the `:fn-notification` function of one argument in the config:

~~~clojure
(defn notification-handler [notification]
  (println "----------")
  (println notification)
  (println "----------"))

(def conn-A (pg/connect
             (assoc config
                    :fn-notification
                    notification-handler)))
~~~

Let's go through the pipeline again:

~~~clojure
(pg/listen conn-A channel-1)

(pg/notify conn-B channel-1 "Hello again!")

(pg/query conn-A "") ;; get pending notifications

;; will be printed in REPL
;; ----------
;; {:channel test-01, :msg :NotificationResponse, :self? false, :pid 3630, :message Hello again!}
;; ----------
~~~

The notification was successfully received and printed.

With this approach, you don't need to constantly drain connections: it is held
by the function you passed.

## Custom Executor and handling Exceptions

An important note about the `:fn-notification` function: it is always called
**in another thread** using an `Executor` object. It is never called in the
connection's thread because otherwise, one can pass a function that either fails
with an exception or takes too long to execute. Both cases ruin the connection's
pipeline.

When no a custom `Executor` object was passed, PG2 uses the builtin
`clojure.lang.Agent.soloExecutor` one. There is a way to override it with the
`:executor` parameter in a config:

~~~clojure
(def executor (Executors/newFixedThreadPool 1))

(def conn (pg/connect (assoc config :executor executor)))
~~~

If you're using Java 21 and above, consider the new `VirtualThreadPerTask`
executor that relies on virtual threads:

~~~clojure
(def executor (Executors/newVirtualThreadPerTaskExecutor))

(def conn (pg/connect (assoc config :executor executor)))
~~~

You can share the same executor object across many connections.

Above, we don't close executors that we produced. But ideally, one should close
them when stopping the program.

Since the `:fn-notification` function is executed in the background, consider
wrap its logic with try/catch to make exceptions visible. Otherwise, it's
impossible to say if processing was successful or not. A small demo:

~~~clojure
(defn notification-handler [notification]
  (let [number (-> notification :message Long/parseLong)]
    (Thread/sleep ^long (rand-int 100))
    (println "The answer is" (/ 100 number))))

(def conn-A (pg/connect
             (assoc config
                    :fn-notification
                    notification-handler)))

(pg/listen conn-A channel-1)

(pg/notify conn-B channel-1 "10")
(pg/notify conn-B channel-1 "25")
(pg/notify conn-B channel-1 "50")
(pg/notify conn-B channel-1 "0") ;; will fail

(pg/query conn-A "") ;; fetch notifications
~~~

The output will be this:

~~~
The answer is 2
The answer is 4
The answer is 10
~~~

The error triggered by division by zero stayed unseen. But with try/catch, it's
much better:

~~~clojure
(defn wrap-safe [f]
  (fn wrapped [& args]
    (try
      (apply f args)
      (catch Exception e
        (println "Error" (ex-message e))))))

...

(def conn-A (pg/connect
             (assoc config
                    :fn-notification
                    (wrap-safe notification-handler))))

...

(pg/notify conn-B channel-1 "0")

(pg/query conn-A "")
~~~

The output:

~~~clojure
The answer is 10
The answer is 4
The answer is 2
Error Divide by zero
~~~

Of course, it's better to use logging facilities rather than prints.

## Sending Notifications

To emit a notification, call the `notify` function with a channel name and a
text payload:

~~~clojure
(pg/notify conn "user_messages" "Hello!")
~~~

This function accepts not a connection only but any type of a source: a
connection pool, a Clojure map, etc. See the [Data Source
Abstraction](/docs/data-source.md) for more details.

The function `notify-json` does two things at once: it encodes arbitrary data
into a JSON string using the current `ObjectMapper`. Then it sends it into the
given channel:

~~~clojure
(pg/notify-json conn-B channel-1 {:some [:user "data" {:nested [1 2 3]}]})

;; message print in REPL
----------
{:channel test-01,
 :msg :NotificationResponse,
 :self? false,
 :pid 3630,
 :message {"some":["user","data",{"nested":[1,2,3]}]}}
----------
~~~

## Polling Notifications

Above, we polled notifications from the server by running a dummy query, for
example:

~~~clojure
(pg/query conn "select 1")
;; or
(pg/query conn "") ;; an empty query
~~~

It's better to unclude a comment into the query saying you're polling
notifications. Thus, a DBA who is watching SQL logs won't ask you "what a hell
are you doing?":

~~~clojure
(pg/query conn "-- polling notifications")
~~~

There is a special function `poll-notifications` that behaves better. First, it
doesn't send any queries to the server. Instead, it checks if there is something
available in an input stream bound to a socket and if yes, reads messages from a
stream. Second, it returns a number of notifications it has got:

~~~clojure
(pg/notify conn-B channel-1 "A")
(pg/notify conn-B channel-1 "B")
(pg/notify conn-B channel-1 "C")

(pg/poll-notifications conn-A)
;; 3
~~~

But it's still **up to you** how often to poll notifications -- no matter if you
send empty queries or call the `poll-notifications` function. The client is
passive in that terms meaning it cannot receive notifications from nowhere. You
must reach the server to get them, actually.

One possible solution is to schedule a timer task that polls notifications every
5 seconds:

~~~clojure
(def timer (new java.util.Timer "notifications"))

(def task (proxy [java.util.TimerTask] []
            (run []
              (pg/poll-notifications conn-A))))

(.scheduleAtFixedRate timer task 0 5000)
~~~

Run this code and emit some messages into the channel. You'll see prints in REPL
appearing every five seconds.

Another option is to use the `java.util.concurrent.ScheduledThreadPoolExecutor`
executor that schedules tasks as well:

~~~clojure
(def executor
  (new java.util.concurrent.ScheduledThreadPoolExecutor 4))

(.scheduleAtFixedRate executor
                      (fn []
                        (pg/poll-notifications conn-A))
                      5
                      5
                      java.util.concurrent.TimeUnit/SECONDS)
~~~

Since the `Connection` object is thread-save (it uses `ReentrantLock` under the
hood), it's OK to share the same connection across multiple threads.

## Unlistening (unsubscribing)

To stop listening a certain channel, call the `unlisten` function:

~~~clojure
(pg/unlisten conn-A channel-1)
~~~

Once called, the connection `conn-A` won't receive notifications from
`channel-1` any longer.

## Final Notes

Although I spent much time on implementing `LISTEN` and `NOTIFY` functionality
in PG2 and debugging, I haven't tried it in production. I have never used the
builtin pub-sub PostgreSQL framework for business purposes. I cannot say if it's
worth using listen/notify for a chat or a distributed queue. Please google for
real use cases, and if you have a good example or useful experience, please drop
me a line, and I'll share it here.
