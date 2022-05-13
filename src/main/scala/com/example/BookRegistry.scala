package com.example

//#book-registry-actor
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.collection.immutable

//#book-case-classes
final case class Book(
    id: String,
    titlu: String,
    gen: String,
    autor: String,
    editura: String,
    nrPagini: Int,
    pret: Float,
    descriere: String
)
final case class Books(books: immutable.Seq[Book])

object BookRegistry {
  // actor protocol
  sealed trait Command
  final case class GetBooks(replyTo: ActorRef[Books]) extends Command
  final case class CreateBook(book: Book, replyTo: ActorRef[ActionPerformed])
      extends Command
  final case class GetBook(id: String, replyTo: ActorRef[GetBookResponse])
      extends Command
  final case class DeleteBook(id: String, replyTo: ActorRef[ActionPerformed])
      extends Command

  // final case class UpdateBook(
  //     id: String,
  //     book: Book,
  //     replyTo: ActorRef[ActionPerformed]
  // ) extends Command

  final case class GetBookResponse(maybeBook: Option[Book])
  final case class ActionPerformed(description: String)

  def apply(): Behavior[Command] = registry(Set.empty)

  private def registry(books: Set[Book]): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetBooks(replyTo) =>
        replyTo ! Books(books.toSeq)
        Behaviors.same

      case CreateBook(book, replyTo) => {

        val carte = books.filter(_.id == book.id)
        println(carte);

        if (carte.exists(b => { b.id == book.id })) {
          replyTo ! ActionPerformed(
            s"Book with id = ${book.id} updated."
          )
          registry(books.filterNot(_.id == { book.id }) + book)
        } else {
          replyTo ! ActionPerformed(
            s"Book with id = ${book.id} created."
          )
          registry(books + book)
        }
      }

      case GetBook(id, replyTo) =>
        replyTo ! GetBookResponse(books.find(_.id == id))
        Behaviors.same

      case DeleteBook(id, replyTo) =>
        replyTo ! ActionPerformed(s"Book with id = $id deleted.")
        registry(books.filterNot(_.id == id))

      // case UpdateBook(id, book, replyTo) =>
      //   replyTo ! ActionPerformed(s"Book ${book.id} updated.")
      //   registry(books.filterNot(_.id == id) + book)
    }
}
//#book-registry-actor
