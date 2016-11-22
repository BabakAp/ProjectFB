package fb

import akka.actor.{ ActorSystem, Props }
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

object Main extends RequestLevelApi {
  def main(args: Array[String]): Unit = {

    if (args(0) == "backend") { startMySystem() }
    else if (args(0) == "frontend") { startMyFrontendSystem() }
    else { println("""Please provide "backend" or "frontend" parameter""") }

  }

  /**
   * Based on example: https://github.com/spray/spray-template/tree/on_spray-can_1.3_scala-2.11
   */
  def startMySystem() {
    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 2552).
      withFallback(ConfigFactory.load())
    // Create an Akka system
    implicit val system = ActorSystem("FaceBrookSystem", conf)

    // create the listener, which will shutdown the system
    val listener = system.actorOf(Props[ServiceListener], name = "service-listener")
    // create and start our service actor
    val service = system.actorOf(Props(new MyServiceActor(listener)), name = "service")

    import system.dispatcher
    implicit val timeout = Timeout(10.seconds)
    // start a new HTTP server on port 8080 with our service actor as the handler
    IO(Http) ? Http.Bind(service, interface = "localhost", port = 8080)
  }

  /**
   * Based on example: https://github.com/spray/spray/tree/release/1.2/examples/spray-can
   */
  def startMyFrontendSystem() {
    import Messages._

    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 2553).
      withFallback(ConfigFactory.load())
    implicit val system = ActorSystem("FaceBrookFrontend", conf)
    import system.dispatcher

    val host = "localhost:8080"

    // create the listener, which will print the result and shutdown the system
    val listener = system.actorOf(Props[Listener], name = "listener")
    // create the master
    val master = system.actorOf(
      Props(new Master(host, listener)), name = "master"
    )

    // start the master
    master ! Start

  }

}
