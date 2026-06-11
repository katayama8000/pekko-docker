# Pekko on Docker — Bank Account Example

A tiny, runnable project to **understand [Apache Pekko](https://pekko.apache.org/)**: the
actor model in action, fronted by a REST API, all packaged in Docker.

> **What is Pekko?** Pekko is an open-source (Apache 2.0) toolkit for building
> concurrent, distributed, message-driven applications on the JVM. It is a
> community fork of **Akka** (taken from the last Apache-licensed release), so
> the concepts and APIs are nearly identical — the package prefix simply
> changed from `akka.*` to `org.apache.pekko.*`. It is written in Scala and
> usable from both Scala and Java.

## The core idea: the actor model

Traditional concurrency shares mutable state between threads and protects it
with locks. That is hard to get right. The **actor model** takes a different
approach:

- State lives **inside an actor** and nothing outside can touch it directly.
- You interact with an actor only by **sending it immutable messages**.
- Each actor processes its messages **one at a time**, so its state stays
  consistent **without any locks**.

This example models a single **bank account** as an actor. The balance is never
a shared variable — it is private to the actor, and deposits/withdrawals are
just messages.

## What's inside

| File | Role |
|------|------|
| [`BankAccount.scala`](src/main/scala/com/example/BankAccount.scala) | The actor. Defines its message protocol (`Deposit`, `Withdraw`, `GetBalance`) and behavior. **Start reading here.** |
| [`BankRoutes.scala`](src/main/scala/com/example/BankRoutes.scala) | Pekko HTTP routes that turn REST calls into actor messages using the `ask` pattern. |
| [`Main.scala`](src/main/scala/com/example/Main.scala) | Boots the `ActorSystem`, spawns the actor, starts the HTTP server. |
| [`Dockerfile`](Dockerfile) | Multi-stage build: compile a fat jar with sbt, then run it on a slim JRE image. |
| [`docker-compose.yml`](docker-compose.yml) | One-command run. |

### How a request flows

```
HTTP POST /account/deposit {"amount":100}
        │
        ▼
   BankRoutes ──ask(Deposit(100, replyTo))──▶  BankAccount actor
        │                                          (updates private balance,
        │                                           one message at a time)
        ◀──────────── Balance(100) ────────────────┘
        │
        ▼
   200 OK {"balance":100}
```

## Run it

You only need **Docker** — no local Scala, sbt, or JVM required.

```bash
docker compose up --build
```

The server listens on <http://localhost:8080>. The first build downloads
dependencies and may take a few minutes; later builds are cached.

To stop:

```bash
docker compose down
```

## Try the API

```bash
# Check the balance (starts at 0)
curl localhost:8080/account/balance
# {"balance":0}

# Deposit
curl -XPOST localhost:8080/account/deposit  -H 'Content-Type: application/json' -d '{"amount":100}'
# {"balance":100}

# Withdraw
curl -XPOST localhost:8080/account/withdraw -H 'Content-Type: application/json' -d '{"amount":30}'
# {"balance":70}

# Business rules are enforced by the actor:
curl -XPOST localhost:8080/account/withdraw -H 'Content-Type: application/json' -d '{"amount":999}'
# {"error":"Insufficient funds"}   (HTTP 400)
```

| Method | Path | Body | Description |
|--------|------|------|-------------|
| `GET`  | `/account/balance`  | —                  | Current balance |
| `POST` | `/account/deposit`  | `{"amount": <n>}`  | Add funds |
| `POST` | `/account/withdraw` | `{"amount": <n>}`  | Remove funds (rejected if insufficient) |

## Key Pekko concepts demonstrated

- **Typed actors** (`Behavior[Command]`) — the message protocol is part of the
  type, so the compiler rejects sending the wrong message to an actor.
- **State as behavior** — instead of mutating a field, the actor returns a *new*
  behavior carrying the new balance (`apply(newBalance)`). State is immutable
  and there are no locks.
- **The `ask` pattern** — bridges the request/response HTTP world with the
  fire-and-forget actor world by returning a `Future` that completes when the
  actor replies.
- **The actor system & guardian** — `Main` creates the root ("guardian") actor,
  which spawns the account actor as its child.

## Running without Docker (optional)

If you do have sbt installed locally:

```bash
sbt run        # start the server
sbt assembly   # build target/scala-2.13/pekko-docker-assembly-0.1.0.jar
```

## Where to go next

- Spawn **many** accounts and route by an `accountId` in the URL — one actor per
  account is the idiomatic Pekko design.
- Make state survive restarts with **Pekko Persistence** (event sourcing).
- Run across multiple nodes with **Pekko Cluster** + **Cluster Sharding**.
- Docs: <https://pekko.apache.org/docs/pekko/current/>
