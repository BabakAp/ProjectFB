package fb

import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import Models._

object Data {
  val users: HashMap[String, User] = new HashMap
  val publicKeys: HashMap[String, String] = new HashMap
  val pages: HashMap[String, Page] = new HashMap
  val posts: HashMap[String, Post] = new HashMap
  val friends: HashMap[String, ListBuffer[String]] = new HashMap
}
