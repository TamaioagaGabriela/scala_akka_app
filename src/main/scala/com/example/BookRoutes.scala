package com.example

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import scala.concurrent.Future
import com.example.BookRegistry._
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

//#import-json-formats
//#book-routes-class
class BookRoutes(bookRegistry: ActorRef[BookRegistry.Command])(implicit
    val system: ActorSystem[_]
) {

  // #book-routes-class
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._
  // #import-json-formats

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(
    system.settings.config.getDuration("my-app.routes.ask-timeout")
  )

  def getBooks(): Future[Books] =
    bookRegistry.ask(GetBooks)
  def getBook(id: String): Future[GetBookResponse] =
    bookRegistry.ask(GetBook(id, _))
  def createBook(book: Book): Future[ActionPerformed] =
    bookRegistry.ask(CreateBook(book, _))
  def deleteBook(id: String): Future[ActionPerformed] =
    bookRegistry.ask(DeleteBook(id, _))

  // def updateBook(book: String, book: Book): Future[ActionPerformed] =
  //   bookRegistry.ask(UpdateBook(id, book, _))

  // #all-routes
  // #books-get-post
  // #books-get-delete
  val bookRoutes: Route =
    pathPrefix("books") {
      concat(
        // #books-get-delete-patch
        pathEnd {
          concat(
            get {
              complete(getBooks())
            },
            post {
              entity(as[Book]) { book =>
                onSuccess(createBook(book)) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            }
          )
        },
        // #books-get-delete
        // #books-get-post
        path(Segment) { id =>
          concat(
            get {
              // #retrieve-book-info
              rejectEmptyResponse {
                onSuccess(getBook(id)) { response =>
                  complete(response.maybeBook)
                }
              }
              // #retrieve-book-info
            },
            delete {
              // #books-delete-logic
              onSuccess(deleteBook(id)) { performed =>
                complete((StatusCodes.OK, performed))
              }
              // #books-delete-logic
            }
          )
        }
      )
      // #books-get-delete
    }
  // #all-routes
}
