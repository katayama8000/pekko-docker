package com.example

import org.apache.pekko.actor.typed.{ActorRef, ActorSystem}
import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.Future
import scala.concurrent.duration._

// ---- JSON payloads exchanged over HTTP -----------------------------------
final case class AmountRequest(amount: Long)
final case class BalanceResponse(balance: Long)
final case class ErrorResponse(error: String)

object JsonFormats {
  implicit val amountFormat: RootJsonFormat[AmountRequest]    = jsonFormat1(AmountRequest)
  implicit val balanceFormat: RootJsonFormat[BalanceResponse] = jsonFormat1(BalanceResponse)
  implicit val errorFormat: RootJsonFormat[ErrorResponse]     = jsonFormat1(ErrorResponse)
}

/**
 * Pekko HTTP routes that translate REST calls into actor messages.
 *
 * The bridge between the synchronous-looking HTTP world and the asynchronous
 * actor world is the `ask` pattern (`actor.ask(...)`): it sends a message and
 * returns a `Future[Response]` that completes when the actor replies.
 */
final class BankRoutes(account: ActorRef[BankAccount.Command])(implicit system: ActorSystem[_]) {
  import JsonFormats._

  // How long `ask` waits for the actor's reply before failing.
  private implicit val timeout: Timeout = 3.seconds

  private def render(response: BankAccount.Response): Route = response match {
    case BankAccount.Balance(amount)  => complete(BalanceResponse(amount))
    case BankAccount.Rejected(reason) => complete(StatusCodes.BadRequest -> ErrorResponse(reason))
  }

  val routes: Route =
    pathPrefix("account") {
      concat(
        path("balance") {
          get {
            val reply: Future[BankAccount.Response] = account.ask(BankAccount.GetBalance.apply)
            onSuccess(reply)(render)
          }
        },
        path("deposit") {
          post {
            entity(as[AmountRequest]) { req =>
              val reply: Future[BankAccount.Response] =
                account.ask(replyTo => BankAccount.Deposit(req.amount, replyTo))
              onSuccess(reply)(render)
            }
          }
        },
        path("withdraw") {
          post {
            entity(as[AmountRequest]) { req =>
              val reply: Future[BankAccount.Response] =
                account.ask(replyTo => BankAccount.Withdraw(req.amount, replyTo))
              onSuccess(reply)(render)
            }
          }
        }
      )
    }
}
