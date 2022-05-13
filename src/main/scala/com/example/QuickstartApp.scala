package com.example

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import scala.util.Failure
import scala.util.Success

//#main-class
object QuickstartApp {
  // #start-http-server
  private def startHttpServer(
      routes: Route
  )(implicit system: ActorSystem[_]): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    import system.executionContext

    val futureBinding = Http().newServerAt("localhost", 8080).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        println(
          s"Server now online. Please navigate to http://localhost:8080/hello\nPress RETURN to stop..."
        )
      case Failure(ex) =>
        // system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        println(s"Failed to bind HTTP endpoint, terminating system")
        system.terminate()
    }
  }
  // #start-http-server
  def main(args: Array[String]): Unit = {
    // #server-bootstrapping
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val bookRegistryActor = context.spawn(BookRegistry(), "BookRegistryActor")
      context.watch(bookRegistryActor)

      val routes = new BookRoutes(bookRegistryActor)(context.system)
      startHttpServer(routes.bookRoutes)(context.system)

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
    // #server-bootstrapping
  }
}
//#main-class
