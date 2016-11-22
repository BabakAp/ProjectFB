package fb

import spray.json._

object Models {
  val n: Int = 1000000

  import spray.json.DefaultJsonProtocol

  object MyJsonProtocol extends DefaultJsonProtocol {
    implicit val userFormat = jsonFormat2(User)
    implicit val pageFormat = jsonFormat2(Page)
    implicit val postFormat = jsonFormat6(Post)
  }

  sealed trait Model

  /**
   * Control messages
   */
  //  case object Start extends Message

  /**
   * User model
   */
  case class User(id: String, about: String) extends Model
  //  case class User(id: String, about: String, bio: String, birthday: String,
  //    name: String, email: String, personal_info: String, personals_interests: String,
  //    relationship_status: String, phone: String) extends Model
  val rand = new scala.util.Random
  /**
   * Returns a randomly generated User
   */
  def randomUser(): User = {
    val id: String = "" + rand.nextInt(n)
    val about: String = "about::" + id
    return new User(id, about)
  }
  /**
   * Page model //TODO
   */
  case class Page(id: String, admin: String) extends Model

  /**
   * Post model //TODO
   */
  case class Post(id: String, from: String, to: String, key: String, msg: String, signature: String) extends Model

  /**
   * Friend list model //TODO
   */
  case class friendList(id: String) extends Model

}
