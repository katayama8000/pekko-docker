package com.example

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Route

import scala.util.{Failure, Success}

/**
 * Application entry point.
 *
 * The "guardian" behavior is the root of the actor hierarchy. It spawns the
 * bank-account actor and starts the HTTP server that talks to it.
 */
object Main {

  def main(args: Array[String]): Unit = {
    val guardian = Behaviors.setup[Nothing] { context =>
      // Spawn the single account actor as a child of the guardian.
      val account = context.spawn(BankAccount(), "bank-account")
      val routes  = new BankRoutes(account)(context.system).routes

      startHttpServer(routes)(context.system)
      Behaviors.empty
    }

    // Creating the ActorSystem boots the whole application.
    ActorSystem[Nothing](guardian, "BankSystem")
  }

  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext

    val host = sys.env.getOrElse("HTTP_HOST", "0.0.0.0")
    val port = sys.env.getOrElse("HTTP_PORT", "8080").toInt

    Http().newServerAt(host, port).bind(routes).onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }
}
