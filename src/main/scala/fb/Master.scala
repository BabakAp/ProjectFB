package fb

import Messages._
import Models._
import Models.MyJsonProtocol._
import akka.actor.{ ActorRef, Actor, Props, PoisonPill }
import akka.event.Logging
import scala.util.{ Failure, Success }
import scala.collection.mutable.HashMap
import scala.collection.mutable.Queue
import scala.concurrent.ExecutionContext.Implicits.global
import spray.http.HttpMethods
import spray.http.HttpMethods._
import scala.concurrent.duration._
import java.security.interfaces.RSAPublicKey

class Master(host: String, listener: ActorRef) extends Actor with RequestLevelApi {

  val log = Logging(context.system, getClass)
  case object Tick
  context.system.scheduler.schedule(5 seconds, 5 seconds, self, Tick)

  case object Tick_POST_Posts
  val POST_Posts_rand = scala.util.Random
  case object Tick_POST_Friend
  val POST_Friend_rand = scala.util.Random
  case object Tick_GET_User
  val GET_User_rand = scala.util.Random
  case object Tick_GET_Page
  val GET_Page_rand = scala.util.Random
  case object Tick_GET_Post
  val GET_Post_rand = scala.util.Random
  case object Tick_GET_Friendlist
  val GET_Friendlist_rand = scala.util.Random
  /**
   * Number of users
   */
  val count = 1000

  //Keep track of all users and their ActorRef
  val usersMap = new HashMap[User, ActorRef]
  val usersQueue = new Queue[User]

  //Keep track of pages users created
  val pagesQueue = new Queue[Page]
  val postsQueue = new Queue[Post]
  var post_user_complete = 0
  var finishedGet = 0
  //Keep track of all requests
  var postCount = 0
  var getCount = 0
  var deleteCount = 0
  var firstTime = true

  var startTime: Long = 0
  def receive = {
    case Start => {
      log.info("Master started")
      for (i <- 0 until count) {
        val user = randomUser();
        val userSim = context.actorOf(Props(new UserSimulator(user, host, self)), name = "UserSim" + i)
        usersQueue += user
        usersMap.put(user, userSim)
      }
      // Putting 'count' number of Users on server
      for (i <- 0 until count) {
        my_Post_User()
      }
      //Simulation has started
      startTime = System.nanoTime
      postCount += count
    }
    case Post_User_Complete => {
      post_user_complete += 1
      if (post_user_complete == count) {
        self ! StartUserSim
      }
    }
    case StartUserSim => {
      for ((k, v) <- usersMap) {
        v ! StartUserSim
      }
      //Take two random users, tell one to POST posts to the other
      context.system.scheduler.schedule(8 seconds, 50 millis, self, Tick_POST_Posts)
      //Take two random users, tell one to POST a friend request to the other
      context.system.scheduler.schedule(10 seconds, 100 millis, self, Tick_POST_Friend)
      //Take two random users, tell one to GET the other
      context.system.scheduler.schedule(6 seconds, 1 milliseconds, self, Tick_GET_User)
      //Tell a random user to GET a random page
      context.system.scheduler.schedule(7 seconds, 5 milliseconds, self, Tick_GET_Page)
      //Tell a random user to GET two random posts, one 'to' the user and one 'from'
      context.system.scheduler.schedule(8 seconds, 100 millis, self, Tick_GET_Post)
      //Tell a random user to GET its friendlist
      context.system.scheduler.schedule(10 seconds, 500 millis, self, Tick_GET_Friendlist)
    }
    //    case UserSimComplete(method: HttpMethod, multiplier: Int) => {
    //      if (method == GET) { finishedGet += 1 }
    //      if (finishedGet == count) {
    //        println("\n\t Finished first " + count * multiplier + " " + method + " requests in " + (System.nanoTime - startTime) / 1000000 + "ms \n\t")
    //      }
    //    }
    //Take two random users, tell one to send posts to the other
    case Tick_POST_Posts => {
      if (usersQueue.size > 1) {
        val rand1 = POST_Posts_rand.nextInt(usersQueue.size)
        val rand2 = POST_Posts_rand.nextInt(usersQueue.size)
        if (rand2 != rand1) {
          val user: User = usersQueue.get(rand1) getOrElse null
          val other: User = usersQueue.get(rand2) getOrElse null
          val user_ref = (usersMap.get(user) getOrElse null)
          user_ref ! sendPosts(other)
        }
      }
    }
    //Take two random users, tell one to POST a friend request to the other
    case Tick_POST_Friend => {
      if (usersQueue.size > 1) {
        val rand1 = POST_Friend_rand.nextInt(usersQueue.size)
        val rand2 = POST_Friend_rand.nextInt(usersQueue.size)
        if (rand2 != rand1) {
          val user: User = usersQueue.get(rand1) getOrElse null
          val other: User = usersQueue.get(rand2) getOrElse null
          val user_ref = (usersMap.get(user) getOrElse null)
          user_ref ! sendFriendRequest(other)
        }
      }
    }
    //Take two random users, tell one to GET the other
    case Tick_GET_User => {
      if (usersQueue.size > 1) {
        val rand1 = GET_User_rand.nextInt(usersQueue.size)
        val rand2 = GET_User_rand.nextInt(usersQueue.size)
        if (rand2 != rand1) {
          val user: User = usersQueue.get(rand1) getOrElse null
          val other: User = usersQueue.get(rand2) getOrElse null
          val user_ref = (usersMap.get(user) getOrElse null)
          if (user == null || other == null || user_ref == null) {
            log.error("Null detected=>user::" + user + "=>other::" + other + "=>user_ref::" + user_ref)
          }
          user_ref ! getUser(other)
        }
      }
    }
    //Tell a random user to GET a random page
    case Tick_GET_Page => {
      if (usersQueue.size > 1 && pagesQueue.size > 0) {
        val rand1 = GET_Page_rand.nextInt(usersQueue.size)
        val rand2 = GET_Page_rand.nextInt(pagesQueue.size)
        val user: User = usersQueue.get(rand1) getOrElse null
        val page: Page = pagesQueue.get(rand2) getOrElse null
        val user_ref = (usersMap.get(user) getOrElse null)
        user_ref ! getPage(page)
      }
    }
    //Tell a random user to GET two random posts, one 'to' the user and one 'from'
    case Tick_GET_Post => {
      if (usersQueue.size > 1 && postsQueue.size > 0) {
        val rand1 = GET_Post_rand.nextInt(usersQueue.size)
        val user: User = usersQueue.get(rand1) getOrElse null
        val user_ref = (usersMap.get(user) getOrElse null)
        try {
          val from = postsQueue.find(e => e.from == user.id) getOrElse null
          if (from != null) {
            user_ref ! getPost(from)
          }
        } catch {
          case e: NoSuchElementException => {
            //No posts from user
          }
        }
        try {
          val to = postsQueue.find(e => e.to == user.id) getOrElse null
          if (to != null) {
            user_ref ! getPost(to)
          }
        } catch {
          case e: NoSuchElementException => {
            //No posts to user
          }
        }
      }
    }
    //Tell a random user to GET its friendlist
    case Tick_GET_Friendlist => {
      if (usersQueue.size > 1) {
        val rand1 = GET_Friendlist_rand.nextInt(usersQueue.size)
        val user: User = usersQueue.get(rand1) getOrElse null
        val user_ref = (usersMap.get(user) getOrElse null)
        user_ref ! getFriendlist()
      }
    }
    //A user has completed 'count' GET requests
    case userGET(count: Int) => {
      getCount += count
    }
    //A user is POSTing a page
    case userPOST_Page(page: Page) => {
      postCount += 1
      my_Post_Page(page, sender)
    }
    //A user is POSTing a post
    case userPOST_Post(post: Post) => {
      postCount += 1
      my_Post_Post(post, sender)
    }
    //A user is POSTing a friend request
    case userPOST_Friend(user: User, friend_id: String, signature: String) => {
      postCount += 1
      my_POST_Friend(user, friend_id, signature, sender)
    }
    //A user has created its RSA public key
    case userPOST_PU(pu: RSAPublicKey, user_id: String) => {
      postCount += 1
      my_POST_PublicKey(pu, user_id, sender)
    }

    case Tick => {
      val total = getCount + postCount
      val time = (System.nanoTime - startTime) / 1000000000
      val throughput = total / time
      log.info(
        "Average number of requests per second: {}",
        throughput
      )
      if (total > 1000000) {
        log.info(
          "{} REQUESTS HANDLED IN {} SECONDS ({}Req/s)", total, time, throughput
        )
        //Send PoisonPill to all user-simulator actors
        for ((k, v) <- usersMap) {
          v ! PoisonPill
        }
        self ! PoisonPill
        listener ! "Finished"
      }
      //Ignore warm-up phase for final output figure
      if (firstTime) {
        startTime = System.nanoTime
        firstTime = false
      }
    }
  }

  var current: Int = 0
  def my_Post_User() = {
    /**
     * Put some initial user base on server
     */
    val f = POST_User(_: String, _: User)(context.system)
    val result = for {
      result1 <- f(host, usersQueue.get(current).getOrElse(randomUser()))
    } yield Set(result1)
    current += 1
    result onComplete {
      case Success(res) => {
        //Do nothing
        //println("\n\t" + res.mkString(", "))
      }
      case Failure(error) => {
        log.warning("my_Post_User->onComplete->Failure => Error: {}", error)
      }
    }
    result onComplete { _ => self ! Post_User_Complete }
  }

  def my_Post_Page(page: Page, requester: ActorRef) = {
    val f = POST_Page(_: String, _: Page)(context.system)
    val result = for {
      result1 <- f(host, page)
    } yield Set(result1)
    result onComplete {
      case Success(res) => {
        //Do nothing
      }
      case Failure(error) => {
        log.warning("my_Post_Page->onComplete->Failure => Error: {}", error)
      }
    }
    result onComplete {
      _ =>
        {
          pagesQueue += page
          requester ! userRequestCompleted("page", "" + page.id)
        }
    }
  }
  def my_Post_Post(post: Post, requester: ActorRef) = {
    val f = POST_Post(_: String, _: Post)(context.system)
    val result = for {
      result1 <- f(host, post)
    } yield Set(result1)
    result onComplete {
      case Success(res) => {
        //Do nothing
      }
      case Failure(error) => {
        log.warning("my_Post_Post->onComplete->Failure => Error: {}", error)
      }
    }
    result onComplete {
      _ =>
        {
          postsQueue += post
          requester ! userRequestCompleted("post", "" + post.id)
        }
    }
  }
  def my_POST_Friend(user: User, friend_id: String, signature: String, requester: ActorRef) = {
    val f = POST_Friend(_: String, _: User, _: String, _: String)(context.system)
    val result = for {
      result1 <- f(host, user, friend_id, signature)
    } yield Set(result1)
    result onComplete {
      case Success(res) => {
        //Do nothing
      }
      case Failure(error) => {
//        log.warning("my_Post_Friend->onComplete->Failure => Error: {}", error)
      }
    }
    result onComplete {
      _ =>
        {
          //TODO: Keep track of friend requests in Master?
          requester ! userRequestCompleted("friend", friend_id)
        }
    }
  }
  def my_POST_PublicKey(pu: RSAPublicKey, user_id: String, requester: ActorRef) = {
    val f = POST_PublicKey(_: String, _: String, _: RSAPublicKey)(context.system)
    val result = for {
      result1 <- f(host, user_id, pu)
    } yield Set(result1)
    result onComplete {
      case Success(res) => {
        //Do nothing
      }
      case Failure(error) => {
        log.warning("my_Post_PublicKey->onComplete->Failure => Error: {}", error)
      }
    }
    result onComplete {
      _ =>
        {
          //TODO: Keep track of public keys in master?
          //          requester ! userRequestCompleted("friend", friend_id)
        }
    }
  }

}

class Listener extends Actor {
  def receive = {
    case "Finished" => {
      context.system.shutdown()
    }
  }
}
