package fb

/**
 * based on: https://github.com/spray/spray/blob/release/1.2/examples/spray-can/simple-http-client
 */
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.http._
import spray.can.Http
import HttpMethods._
import spray.json._
import spray.json.DefaultJsonProtocol
import spray.json.DefaultJsonProtocol._
import spray.httpx.unmarshalling._
import spray.httpx.marshalling._
import HttpCharsets._
import MediaTypes._
import Models._
import Models.MyJsonProtocol._
import spray.httpx.SprayJsonSupport._
import spray.util._
//import spray.httpx.encoding.{ Gzip, Deflate }
import spray.client.pipelining._
import java.security.interfaces.RSAPublicKey

//TODO: Handle empty response
trait RequestLevelApi {
  private implicit val timeout: Timeout = 60.seconds

  // The request-level API is the highest-level way to access the spray-can client-side infrastructure.
  // All you have to do is to send an HttpRequest instance to `IO(Http)` and wait for the response.
  // The spray-can HTTP infrastructure looks at the URI (or the Host header if the URI is not absolute)
  // to figure out which host to send the request to. It then sets up a HostConnector for that host
  // (if it doesn't exist yet) and forwards it the request.
  def RequestLevelApiUsers(host: String, method: HttpMethod)(implicit system: ActorSystem): Future[User] = {
    val myHost = host + "/users"
    import system.dispatcher // execution context for future transformation below
    for {
      response <- IO(Http).ask(HttpRequest(method, Uri(s"http://$myHost"))).mapTo[HttpResponse]
      //      _ <- IO(Http) ? Http.CloseAll
    } yield {
      //      system.log.info(
      //        "Request-Level API: received {} response with {} bytes",
      //        response.status, response.entity.data.length
      //      )
      //      response.header[HttpHeaders.Server].get.products.head
      val body: HttpEntity = response.entity

      val user = body.as[User].e.get
      user
    }
  }
  def RequestLevelApiUser(host: String, user_id: String, method: HttpMethod)(implicit system: ActorSystem): Future[User] = {
    val myHost = host + "/users/" + user_id
    import system.dispatcher // execution context for future transformation below
    for {
      response <- IO(Http).ask(HttpRequest(method, Uri(s"http://$myHost"))).mapTo[HttpResponse]
    } yield {
      val body: HttpEntity = response.entity
      val user = body.as[User].e.get
      user
    }
  }
  def POST_User(host: String, user: User)(implicit system: ActorSystem): Future[String] = {
    import system.dispatcher
    import spray.http._
    import spray.json.DefaultJsonProtocol
    //    TODO: Look into compression/decompression options
    //    import spray.httpx.encoding.{ Gzip, Deflate }
    import spray.httpx.SprayJsonSupport._
    import spray.client.pipelining._

    /**
     * Post user to /users/{user-id}
     */
    val myHost = host + "/users/" + user.id
    val pipeline: HttpRequest => Future[String] = (
      sendReceive
      ~> unmarshal[String]
    )
    val response: Future[String] = pipeline(Post(s"http://$myHost", user))
    response
  }

  def RequestLevelApiPages(host: String, method: HttpMethod)(implicit system: ActorSystem): Future[Page] = {
    val myHost = host + "/pages"
    import system.dispatcher // execution context for future transformation below
    for {
      response <- IO(Http).ask(HttpRequest(method, Uri(s"http://$myHost"))).mapTo[HttpResponse]
    } yield {
      val body: HttpEntity = response.entity

      val page = body.as[Page].e.get
      page
    }
  }
  def RequestLevelApiPage(host: String, page_id: String, method: HttpMethod)(implicit system: ActorSystem): Future[Page] = {
    val myHost = host + "/pages/" + page_id
    import system.dispatcher // execution context for future transformation below
    for {
      response <- IO(Http).ask(HttpRequest(method, Uri(s"http://$myHost"))).mapTo[HttpResponse]
    } yield {
      val body: HttpEntity = response.entity
      val page = body.as[Page].e.get
      page
    }
  }

  def POST_Page(host: String, page: Page)(implicit system: ActorSystem): Future[String] = {
    import system.dispatcher
    import spray.http._
    import spray.json.DefaultJsonProtocol
    import spray.httpx.SprayJsonSupport._
    import spray.client.pipelining._

    /**
     * Post page to /pages/{page-id}
     */
    val myHost = host + "/pages/" + page.id
    val pipeline: HttpRequest => Future[String] = (
      sendReceive
      ~> unmarshal[String]
    )
    val response: Future[String] = pipeline(Post(s"http://$myHost", page))
    response
  }

  def RequestLevelApiPosts(host: String, method: HttpMethod)(implicit system: ActorSystem): Future[Post] = {
    val myHost = host + "/posts"
    import system.dispatcher // execution context for future transformation below
    for {
      response <- IO(Http).ask(HttpRequest(method, Uri(s"http://$myHost"))).mapTo[HttpResponse]
    } yield {
      val body: HttpEntity = response.entity

      val post = body.as[Post].e.get
      post
    }
  }
  def RequestLevelApiPost(host: String, post_id: String, method: HttpMethod)(implicit system: ActorSystem): Future[Post] = {
    val myHost = host + "/posts/" + post_id
    import system.dispatcher // execution context for future transformation below
    for {
      response <- IO(Http).ask(HttpRequest(method, Uri(s"http://$myHost"))).mapTo[HttpResponse]
    } yield {
      val body: HttpEntity = response.entity
      val post = body.as[Post].e.get
      post
    }
  }

  def POST_Post(host: String, post: Post)(implicit system: ActorSystem): Future[String] = {
    import system.dispatcher
    import spray.http._
    import spray.json.DefaultJsonProtocol
    import spray.httpx.SprayJsonSupport._
    import spray.client.pipelining._

    /**
     * Post post to /posts/{post-id}
     */
    val myHost = host + "/posts/" + post.id
    val pipeline: HttpRequest => Future[String] = (
      sendReceive
      ~> unmarshal[String]
    )
    val response: Future[String] = pipeline(Post(s"http://$myHost", post))
    response
  }

  def RequestLevelApiFriendlist(host: String, user_id: String, method: HttpMethod)(implicit system: ActorSystem): Future[List[String]] = {
    val myHost = host + "/friends/" + user_id
    import system.dispatcher // execution context for future transformation below
    import spray.json.DefaultJsonProtocol._
    for {
      response <- IO(Http).ask(HttpRequest(method, Uri(s"http://$myHost"))).mapTo[HttpResponse]
    } yield {
      val body: HttpEntity = response.entity
      val post = body.as[List[String]].e.get
      post
    }
  }
  def POST_Friend(host: String, user: User, friend_id: String, signature: String)(implicit system: ActorSystem): Future[String] = {
    import system.dispatcher
    import spray.http._
    import spray.json.DefaultJsonProtocol
    import spray.httpx.SprayJsonSupport._
    import spray.client.pipelining._

    /**
     * Add friend_id to user's friends
     */
    val myHost = host + "/friends/" + user.id
    val pipeline: HttpRequest => Future[String] = (
      sendReceive
      ~> unmarshal[String]
    )
    val response: Future[String] = pipeline(Post(s"http://$myHost", friend_id + "__" + signature))
    response
  }
  def RequestLevelApiPublicKey(host: String, user_id: String, method: HttpMethod)(implicit system: ActorSystem): Future[String] = {
    val myHost = host + "/keys/" + user_id
    import system.dispatcher // execution context for future transformation below
    for {
      response <- IO(Http).ask(HttpRequest(method, Uri(s"http://$myHost"))).mapTo[HttpResponse]
    } yield {
      val body: HttpEntity = response.entity
      val pu = body.as[String].e.get
      pu
    }
  }
  def POST_PublicKey(host: String, user_id: String, pu: RSAPublicKey)(implicit system: ActorSystem): Future[String] = {
    import system.dispatcher
    import spray.http._
    import spray.json.DefaultJsonProtocol
    import spray.httpx.SprayJsonSupport._
    import spray.client.pipelining._

    val myHost = host + "/keys/" + user_id
    val pipeline: HttpRequest => Future[String] = (
      sendReceive
      ~> unmarshal[String]
    )
    //Post public key in form of n::e with n being the modulus and e being the exponent
    val response: Future[String] = pipeline(Post(s"http://$myHost", pu.getModulus + "::" + pu.getPublicExponent))
    response
  }

}