package fb

/**
 * Based on example: https://github.com/spray/spray-template/tree/on_spray-can_1.3_scala-2.11
 */
import Models._
import Data._
import akka.actor.{ Actor, ActorRef }
import spray.routing._
import spray.http._
import MediaTypes._
import spray.routing.HttpService
import spray.routing.directives.CachingDirectives._
import spray.httpx.encoding._
import spray.httpx.unmarshalling._
import spray.httpx.marshalling._
import HttpCharsets._
import spray.json._
import spray.json.DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport._
import spray.util._
import Models.MyJsonProtocol._
//import scala.collection.mutable.HashMap
import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MyServiceActor(listener: ActorRef) extends Actor with MyService
  with MyUserService with MyPageService with MyPostService with MyFriendService
  with MyKeyService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = handleTimeouts orElse runRoute(rootRoute ~ userRoute ~ pageRoute ~ postRoute ~ friendRoute ~ keyRoute)

  def handleTimeouts: Receive = {
    case Timedout(x: HttpRequest) =>
      sender() ! HttpResponse(StatusCodes.InternalServerError, "TIMED OUT")
  }
  val n = 1200
  //Shutdown the server after 'n seconds
  import scala.concurrent.ExecutionContext.Implicits.global
  context.system.scheduler.schedule(n seconds, n seconds, listener, "Finished")
}

class ServiceListener extends Actor {
  def receive = {
    case "Finished" => {
      context.system.shutdown()
    }
  }
}

// this trait defines our service behavior independently from the service actor
trait MyService extends HttpService {
  val invalidReqTitle = "Invalid Request"

  /**
   * Two types of resource: Collection and Element, based on: https://en.wikipedia.org/wiki/Representational_state_transfer
   * Operations: GET, PUT, POST, DELETE
   * collection: List, Replace, Create a new entry, Delete
   * element: Retrieve, Replace or Create if doesn't exist, Replace or Create if doesn't exist, Delete
   */
  val rootRoute: Route =
    /**
     * Root
     */
    path("") {
      get {
        respondWithMediaType(`text/html`) {
          complete {
            "<html><head><title>" + invalidReqTitle +
              "</title></head><body><h1>RECEIVED <i>GET</i> ON <i>ROOT</i></h1></body></html>"
          }
        }
      } ~
        put {
          respondWithMediaType(`text/html`) {
            complete {
              "<html><head><title>" + invalidReqTitle +
                "</title></head><body><h1>RECEIVED <i>PUT</i> ON <i>ROOT</i></h1></body></html>"
            }
          }
        } ~
        post {
          respondWithMediaType(`text/html`) {
            complete {
              "<html><head><title>" + invalidReqTitle +
                "</title></head><body><h1>RECEIVED <i>POST</i> ON <i>ROOT</i></h1></body></html>"
            }
          }
        } ~
        delete {
          respondWithMediaType(`text/html`) {
            complete {
              "<html><head><title>" + invalidReqTitle +
                "</title></head><body><h1>RECEIVED <i>DELETE</i> ON <i>ROOT</i></h1></body></html>"
            }
          }
        }
    }
}

trait MyUserService extends HttpService {
  /**
   * default user, returned when not found instead of null
   */
  val defaultUser: User = new User("", "")

  val userCollectionRoute =
    /**
     * Users collection: List, Replace, Create a new entry, Delete
     */
    path("users") {
      get {
        respondWithMediaType(`application/json`) {
          complete {
            import spray.httpx.SprayJsonSupport._
            import spray.json.DefaultJsonProtocol._
            /**
             * List of users
             */
            users.toList
          }
        }
      } ~
        (put | post | delete) {
          complete {
            /**
             * 501 Not Implemented
             */
            HttpResponse(501)
          }
        }
    }
  /**
   * User element: Retrieve, Replace or Create if doesn't exist, Replace or Create if doesn't exist, Delete
   */
  val userElementRoute =
    path("users" / IntNumber) { id =>
      //      usersElementTitle += id
      get {
        respondWithMediaType(`application/json`) {
          complete {
            val user: User = users.getOrElse("" + id, defaultUser)
            user
          }
        }
      } ~
        (put | post) {
          entity(as[User]) { user =>
            // transfer to newly spawned actor
            detach() {
              complete {
                /**
                 * If user is posted to incorrect id, return 400 Bad Request
                 */
                if (!(user.id).equalsIgnoreCase("" + id)) {
                  HttpResponse(400)
                } else {
                  users.put(user.id, user)
                  friends.put(user.id, new ListBuffer[String])
                  /**
                   * Return id
                   */
                  user.id
                }
              }
            }
          }
        } ~
        delete {
          //          respondWithMediaType(`text/html`) {
          complete {
            val tmp = users.remove("" + id)
            tmp match {
              case Some(u) =>
                /**
                 * return id if deletion was succesful
                 */
                u.id
              case None =>
                /**
                 * return -1 if deletion failed
                 */
                "-" + 1
            }
            // "<html><head><title>" + usersElementTitle +
            // "</title></head><body><h1>RECEIVED <i>DELETE</i> ON <i>USERS</i> ID:" + id + "</h1></body></html>"
          }
        }
    }

  val userRoute = userCollectionRoute ~ userElementRoute
}

trait MyPageService extends HttpService {
  /**
   * default page, returned when not found instead of null
   */
  val defaultPage: Page = new Page("", "")

  val pageCollectionRoute =
    /**
     * Pages collection: List, Replace, Create a new entry, Delete
     */
    path("pages") {
      get {
        respondWithMediaType(`application/json`) {
          complete {
            import spray.httpx.SprayJsonSupport._
            import spray.json.DefaultJsonProtocol._
            /**
             * List of pages
             */
            pages.toList
          }
        }
      } ~
        (put | post | delete) {
          complete {
            /**
             * 501 Not Implemented
             */
            HttpResponse(501)
          }
        }
    }
  /**
   * Page element: Retrieve, Replace or Create if doesn't exist, Replace or Create if doesn't exist, Delete
   */
  val pageElementRoute =
    path("pages" / IntNumber) { id =>
      get {
        respondWithMediaType(`application/json`) {
          complete {
            val page: Page = pages.getOrElse("" + id, defaultPage)
            page
          }
        }
      } ~
        (put | post) {
          entity(as[Page]) { page =>
            // transfer to newly spawned actor
            detach() {
              complete {
                /**
                 * If page_id already exists, return 403 Forbidden
                 */
                if (pages.keySet.exists(_ == page.id)) {
                  HttpResponse(403)
                } // Else insert page
                else {
                  pages.put(page.id, page)
                  /**
                   * Return id
                   */
                  page.id
                }
              }
            }
          }
        } ~
        delete {
          complete {
            val tmp = pages.remove("" + id)
            tmp match {
              case Some(p) =>
                /**
                 * return id if deletion was succesful
                 */
                p.id
              case None =>
                /**
                 * return -1 if deletion failed
                 */
                "-" + 1
            }
          }
        }
    }

  val pageRoute = pageCollectionRoute ~ pageElementRoute
}

trait MyPostService extends HttpService {
  /**
   * default post, returned when not found instead of null
   */
  val defaultPost: Post = new Post("", "", "", "", "", "")

  val postCollectionRoute =
    /**
     * Posts collection: List, Replace, Create a new entry, Delete
     */
    path("posts") {
      get {
        respondWithMediaType(`application/json`) {
          complete {
            import spray.httpx.SprayJsonSupport._
            import spray.json.DefaultJsonProtocol._
            /**
             * List of posts
             */
            posts.toList
          }
        }
      } ~
        (put | post | delete) {
          complete {
            /**
             * 501 Not Implemented
             */
            HttpResponse(501)
          }
        }
    }
  /**
   * Post element: Retrieve, Replace or Create if doesn't exist, Replace or Create if doesn't exist, Delete
   */
  val postElementRoute =
    path("posts" / IntNumber) { id =>
      get {
        respondWithMediaType(`application/json`) {
          complete {
            val post: Post = posts.getOrElse("" + id, defaultPost)
            post
          }
        }
      } ~
        (put | post) {
          entity(as[Post]) { post =>
            // transfer to newly spawned actor
            detach() {
              complete {
                posts.put(post.id, post)
                /**
                 * Return id
                 */
                post.id
              }
            }
          }
        } ~
        delete {
          complete {
            val tmp = posts.remove("" + id)
            tmp match {
              case Some(p) =>
                /**
                 * return id if deletion was succesful
                 */
                p.id
              case None =>
                /**
                 * return -1 if deletion failed
                 */
                "-" + 1
            }
          }
        }
    }

  val postRoute = postCollectionRoute ~ postElementRoute
}

trait MyFriendService extends HttpService {
  val rsa: RSA = new RSA()
  /**
   * default friend, returned when not found instead of null
   */
  val defaultFriend: User = new User("", "")

  val friendCollectionRoute =
    /**
     * Friends collection: 501 Not Implemented
     */
    path("friends") {
      (get | put | post | delete) {
        complete {
          /**
           * 501 Not Implemented
           */
          HttpResponse(501)
      }
        }
    }
  /**
   * Friend element: Retrieve, Replace or Create if doesn't exist, Replace or Create if doesn't exist, Delete
   */
  val friendElementRoute =
    path("friends" / IntNumber) { id =>
      //Returns a list of user-id's friends
      get {
        respondWithMediaType(`application/json`) {
          complete {
            import spray.json.DefaultJsonProtocol._
            import spray.httpx.SprayJsonSupport._
            val user: User = users.getOrElse("" + id, null)
            val friendList: ListBuffer[String] = friends.getOrElse("" + id, null)
            if (user == null) {
              /**
               * 404 Not Found if user doesn't exist
               */
              HttpResponse(404)
            } else {
              friendList.toList
            }
          }
        }
      } ~
        (put | post) {
          /**
           * friend's id to add
           */
          entity(as[String]) { input =>
            // transfer to newly spawned actor
            detach() {
              complete {
                val split: Array[String] = input.split("__")
                val friend_id = split(0)
                val user: User = users.getOrElse("" + id, null)
                val friendList: ListBuffer[String] = friends.getOrElse("" + id, null)
                val friend: User = users.getOrElse("" + friend_id, null)
                if (user == null) {
                  /**
                   * return 404 Not Found if user doesn't exist
                   */
                  HttpResponse(404)
                } else if (friend == null) {
                  /**
                   * Return 400 Bad Request if friend doesn't exist
                   */
                  HttpResponse(400)
                } else {
                  //Split into modulus n and exponent e
                  val ne: Array[String] = publicKeys.getOrElse(user.id, "").split("::")
                  if (ne.length < 2) {
                    HttpResponse(404)
                  } else {
                    //Create RSA Public Key object from n,e
                    val spec: RSAPublicKeySpec = new RSAPublicKeySpec(new BigInteger(ne(0)), new BigInteger(ne(1)))
                    val factor: KeyFactory = KeyFactory.getInstance("RSA")
                    val pu: RSAPublicKey = factor.generatePublic(spec).asInstanceOf[RSAPublicKey]
                    //Verify user signature
                    val verified = rsa.verifySignature(user.id + "::" + friend.id, split(1), pu)
                    if (verified) {
                      /**
                       * add friend
                       */
                      friendList += friend_id
                      /**
                       * Return id
                       */
                      friend_id
                    } else {
                    }
                    // 403 Forbidden
                    HttpResponse(404)
                  }
                }
              }
            }
          }
        } ~
        delete {
          /**
           * friend's id to remove
           */
          entity(as[String]) { friend_id =>
            complete {
              val userList: ListBuffer[String] = friends.getOrElse("" + id, null)
              if (userList == null) {
                /**
                 * return -1 if user doesn't exist
                 */
                "-" + 1
              } else {
                /**
                 * remove friend
                 */
                userList -= friend_id
                /**
                 * Return id
                 */
                friend_id
              }
            }
          }
        }
    }

  val friendRoute = friendCollectionRoute ~ friendElementRoute
}

trait MyKeyService extends HttpService {
  val keyCollectionRoute =
    path("keys") {
      get {
        respondWithMediaType(`application/json`) {
          complete {
            import spray.httpx.SprayJsonSupport._
            import spray.json.DefaultJsonProtocol._
            /**
             * List of posts
             */
            publicKeys.toList
          }
        }
      } ~
        (put | post | delete) {
          complete {
            /**
             * 501 Not Implemented
             */
            HttpResponse(501)
          }
        }
    }
  val keyElementRoute =
    path("keys" / IntNumber) { id =>
      get {
        respondWithMediaType(`application/json`) {
          complete {
            /**
             * Return public key of corresponding user
             */
            val pu: String = publicKeys.getOrElse("" + id, "")
            pu
          }
        }
      } ~
        (put | post) {
          entity(as[String]) { pu =>
            // transfer to newly spawned actor
            detach() {
              complete {
                if (users.contains("" + id) && publicKeys.getOrElse("" + id, null) == null) {
                  /**
                   * If it's the first time, keep the public key of user, return 201 Created
                   */
                  publicKeys.put("" + id, pu)
                  HttpResponse(201)
                } else {
                  /**
                   * Else return 403 Forbidden to prevent others from changing user public keys
                   */
                  HttpResponse(403)
                }
              }
            }
          }
        } ~
        delete {
          complete {
            /**
             * Deletes are not permitted, 403 Forbidden
             */
            HttpResponse(403)
          }
        }
    }

  val keyRoute = keyCollectionRoute ~ keyElementRoute
}