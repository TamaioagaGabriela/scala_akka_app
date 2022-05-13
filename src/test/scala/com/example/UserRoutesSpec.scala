package com.example

//#book-routes-spec
//#test-top
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

//#set-up
class UserRoutesSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest {
  // #test-top

  // the Akka HTTP route testkit does not yet support a typed actor system (https://github.com/akka/akka-http/issues/2036)
  // so we have to adapt for now
  lazy val testKit = ActorTestKit()
  implicit def typedSystem = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  // Here we need to implement all the abstract members of BookRoutes.
  // We use the real BookRegistryActor to test it while we hit the Routes,
  // but we could "mock" it by implementing it in-place or by using a TestProbe
  // created with testKit.createTestProbe()
  val bookRegistry = testKit.spawn(BookRegistry())
  lazy val routes = new BookRoutes(bookRegistry).bookRoutes

  // use the json formats to marshal and unmarshall objects in the test
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._
  // #set-up

  // #actual-test
  "BookRoutes" should {
    "return no books if no present (GET /books)" in {
      // note that there's no need for the host part in the uri:
      val request = HttpRequest(uri = "/books")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"books":[]}""")
      }
    }
    // #actual-test

    // #testing-post
    "be able to add books (POST /books)" in {
      val book = Book("Kapi", 42, "jp")
      val bookEntity =
        Marshal(book)
          .to[MessageEntity]
          .futureValue // futureValue is from ScalaFutures

      // using the RequestBuilding DSL:
      val request = Post("/books").withEntity(bookEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and we know what message we're expecting back:
        entityAs[String] should ===("""{"description":"Book Kapi created."}""")
      }
    }
    // #testing-post

    "be able to remove books (DELETE /books)" in {
      // book the RequestBuilding DSL provided by ScalatestRouteSpec:
      val request = Delete(uri = "/books/Kapi")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and no entries should be in the list:
        entityAs[String] should ===("""{"description":"Book Kapi deleted."}""")
      }
    }
    // #actual-test
  }
  // #actual-test

  // #set-up
}
//#set-up
//#book-routes-spec
