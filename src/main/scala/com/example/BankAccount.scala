package com.example

import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors

/**
 * A single bank-account actor.
 *
 * This is the heart of the example. Notice what is NOT here:
 *   - no `synchronized`, no locks, no `volatile`
 *   - no shared mutable variable
 *
 * The balance lives ONLY inside the actor. The outside world cannot touch it
 * directly; it can only send immutable messages (`Command`s). The actor
 * processes them ONE AT A TIME, so the state is always consistent without any
 * manual concurrency control. State changes are expressed by returning a NEW
 * behavior carrying the new balance (`apply(newBalance)`).
 */
object BankAccount {

  // ---- Protocol: the messages this actor accepts -------------------------
  sealed trait Command
  final case class Deposit(amount: Long, replyTo: ActorRef[Response])  extends Command
  final case class Withdraw(amount: Long, replyTo: ActorRef[Response]) extends Command
  final case class GetBalance(replyTo: ActorRef[Response])             extends Command

  // ---- Protocol: the replies this actor sends back -----------------------
  sealed trait Response
  final case class Balance(amount: Long)   extends Response
  final case class Rejected(reason: String) extends Response

  /** Factory for a fresh account behavior, starting from `balance`. */
  def apply(balance: Long = 0L): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      message match {
        case Deposit(amount, replyTo) =>
          if (amount <= 0) {
            replyTo ! Rejected("Deposit amount must be positive")
            Behaviors.same
          } else {
            val newBalance = balance + amount
            context.log.info("Deposited {}. New balance = {}", amount, newBalance)
            replyTo ! Balance(newBalance)
            apply(newBalance) // become a new behavior holding the new state
          }

        case Withdraw(amount, replyTo) =>
          if (amount <= 0) {
            replyTo ! Rejected("Withdraw amount must be positive")
            Behaviors.same
          } else if (amount > balance) {
            replyTo ! Rejected("Insufficient funds")
            Behaviors.same
          } else {
            val newBalance = balance - amount
            context.log.info("Withdrew {}. New balance = {}", amount, newBalance)
            replyTo ! Balance(newBalance)
            apply(newBalance)
          }

        case GetBalance(replyTo) =>
          replyTo ! Balance(balance)
          Behaviors.same
      }
    }
}
