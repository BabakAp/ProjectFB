package fb

import Models._
import java.security.interfaces.RSAPublicKey

object Messages {

  sealed trait Message
  /**
   * Control messages
   */
  case object Start extends Message
  case object StartUserSim extends Message
  //  case class UserSimComplete(method: HttpMethod, multiplier: Int) extends Message
  case class Finish(duration: Long) extends Message

  case object Post_User_Complete extends Message

  /**
   * User to Master
   */
  case class userPOST_Page(page: Page) extends Message
  case class userPOST_Post(post: Post) extends Message
  case class userPOST_Friend(user: User, friend_id: String, signature: String) extends Message
  case class userRequestCompleted(msg: String, id: String) extends Message
  case class userGET(count: Int) extends Message

  case class userPOST_PU(pu: RSAPublicKey, user_id: String) extends Message

  /**
   * Master to User
   */
  case class sendPosts(other: User) extends Message
  case class sendFriendRequest(other: User) extends Message
  case class getUser(other: User) extends Message
  case class getPage(page: Page) extends Message
  case class getPost(post: Post) extends Message
  case class getFriendlist() extends Message
}
