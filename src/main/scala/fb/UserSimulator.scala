package fb

import Messages._
import Models._
import akka.actor.{ ActorRef, Actor }
import akka.event.Logging
import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext.Implicits.global
import spray.http._
import java.math.BigInteger
import java.security._
import java.security.interfaces._
import javax.crypto.spec.SecretKeySpec
import scala.collection.mutable.HashMap
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import javax.crypto.SecretKey;

class UserSimulator(user: User, host: String, master: ActorRef) extends Actor with RequestLevelApi {
  val log = Logging(context.system, getClass)
  var isAdmin = false
  var page: Page = null
  var random = new scala.util.Random()
  //Keep track of my posts
  //val myPosts = new ListBuffer[Post]
  var firstTime = true

  val aes: AES = new AES()
  val rsa: RSA = new RSA()
  val encoder: Base64.Encoder = Base64.getEncoder();
  val decoder: Base64.Decoder = Base64.getDecoder();
  // Private Key
  private var pk: RSAPrivateKey = null
  private var post_keys: HashMap[String, SecretKey] = new HashMap
  // Public Key
  var pu: RSAPublicKey = null
  //Cache seen public keys
  private var user_fingerprints: HashMap[String, String] = new HashMap

  def receive = {

    case StartUserSim => {
      // RSA key generator
      val keyGen: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
      // Strong secure random number generator
      val random: SecureRandom = SecureRandom.getInstanceStrong();
      // Initialize key generator with 2048 bits and strong secure random seed
      keyGen.initialize(2048, random);
      // Generate public/private key pair
      val pair: KeyPair = keyGen.generateKeyPair()
      pk = pair.getPrivate().asInstanceOf[RSAPrivateKey]
      pu = pair.getPublic().asInstanceOf[RSAPublicKey]
      //Keeping track of all public keys known
      user_publicKeys.put(user.id, pu)
      //      val msg: String = rsa.encryptUsingPublicKey("This is a message", pu)
      //      println(msg)
      //      val msg2: String = rsa.decryptUsingPrivateKey(msg, pk)
      //      println(msg2)
      // Send out public key for self
      master ! userPOST_PU(pu, user.id)
    }
    //GET other user
    case getUser(other: User) => {
      /**
       * User sends 8 GET requests for the other user
       */
      sendRequest(host, other.id, HttpMethods.GET, Models.User, master)
    }
    //GET page
    case getPage(page: Page) => {
      /**
       * User sends 8 GET requests for the page
       */
      sendRequest(host, page.id, HttpMethods.GET, Models.Page, master)
    }
    //GET post
    case getPost(post: Post) => {
      /**
       * User sends 8 GET requests for the post
       */
      sendRequest(host, post.id, HttpMethods.GET, Models.Post, master)
    }
    //GET friendlist
    case getFriendlist() => {
      //First time GET friendlist is called, user might create a page, to sparsify POST_Page requests
      if (firstTime) {
        /**
         * With 10% probability user creates a page
         */
        if (random.nextDouble() < 0.1) {
          //page_id: random, admin: self
          page = new Page("" + random.nextInt(1000000), user.id)
          master ! userPOST_Page(page)
        }
        firstTime = false
      }
      val f = RequestLevelApiFriendlist(_: String, _: String, _: HttpMethod)(context.system)
      val result = for {
        result1 <- f(host, user.id, HttpMethods.GET)
      } yield Set(result1)

      result onComplete {
        case Success(res) => {
          //Do Nothing
        }
        case Failure(error) => {
          log.warning("Error in getFriendlist: {}", error)
        }
      }
      //      result onComplete { _ => master ! UserSimComplete(HttpMethods.GET, 1) }
      //Tell master 1 GET request has been made
      master ! userGET(1)
    }
    //POST posts to other user
    case sendPosts(other: User) => {
      /**
       * User sends posts to another user
       */
      val msg: String = "This is a message from " + user.id + " to " + other.id;
      //generate new strong random secret key for post
      val secret = aes.generateSecretKey()
      //Encrypt the message using newly generated strong random secret key
      val encryptedMsg: String = aes.encrypt(msg, secret);
      //Encrypt-then-Sign mechanism, use user's private key to sign the encrypted data
      val signature: String = rsa.sign(encryptedMsg, pk);
      //Ask the server for  recipient user's public key
      val f = RequestLevelApiPublicKey(_: String, _: String, _: HttpMethod)(context.system)
      val result = for {
        result1 <- f(host, other.id, HttpMethods.GET)
      } yield result1

      result onComplete {
        case Success(res) => {
          //Split response into modulus n and exponent e
          val ne: Array[String] = res.split("::")
          //Create RSA Public Key object from n,e
          val spec: RSAPublicKeySpec = new RSAPublicKeySpec(new BigInteger(ne(0)), new BigInteger(ne(1)))
          val factor: KeyFactory = KeyFactory.getInstance("RSA")
          val pu: RSAPublicKey = factor.generatePublic(spec).asInstanceOf[RSAPublicKey]
          user_publicKeys.put(other.id, pu)

          val resHash = SHA256Hasher.SHA256(res)
          //If it's the first time a message is sent, cache the SHA-256 of recipient's public key
          if (!user_fingerprints.contains(resHash)) {
            user_fingerprints.put(other.id, resHash)
          } //Else verify the received public key against the local cache
          else {
            if (!(user_fingerprints.getOrElse(other.id, "").equals(resHash))) {
              log.warning("Cannot verify user {} public key", other.id)
            }
          }
          //Encrypt secret key using recipient's public key
          val encryptedSecret: String = rsa.encryptUsingPublicKey(encoder.encodeToString(secret.getEncoded()), pu);
          //Put the encrypted key besides the post metadata and encrypted message
          //Post(id: String, from: String, to: String, key: String, msg: String)
          var post = new Post("" + random.nextInt(100000), user.id, other.id, encryptedSecret, encryptedMsg, signature)
          //Keep track of posts and their keys
          post_keys.put(post.id, secret)

          master ! userPOST_Post(post)
        }
        case Failure(error) => {
          log.warning("Error in sendRequest->PublicKey: {}", error)
        }
      }
    }

    //POST friend request to other user
    case sendFriendRequest(other: User) => {
      //Sign friend request using private key
      master ! userPOST_Friend(user, other.id, rsa.sign(user.id + "::" + other.id, pk))
    }

    case userRequestCompleted(msg, id) => {
      if (msg.equalsIgnoreCase("page")) {
        //Page successfully created, user is now an admin
        isAdmin = true
      }
    }

  }
  private var user_publicKeys: HashMap[String, RSAPublicKey] = new HashMap

  def sendRequest(host: String, id: String, method: HttpMethod, model: Any, master: ActorRef) = {
    model match {
      case Models.User => {
        val f = RequestLevelApiUser(_: String, _: String, _: HttpMethod)(context.system)
        val result = for {
          result1 <- f(host, id, method)
          result2 <- f(host, id, method)
          result3 <- f(host, id, method)
          result4 <- f(host, id, method)
          result5 <- f(host, id, method)
          result6 <- f(host, id, method)
          result7 <- f(host, id, method)
          result8 <- f(host, id, method)
        } yield Set(result1, result2, result3, result4, result5, result6, result7, result8)

        result onComplete {
          case Success(res) => {
            //Do Nothing
          }
          case Failure(error) => {
            log.warning("Error in sendRequest->User: {}", error)
          }
        }
        //        result onComplete { _ => master ! UserSimComplete(method, 5) }
      }
      case Models.Page => {
        val f = RequestLevelApiPage(_: String, _: String, _: HttpMethod)(context.system)
        val result = for {
          result1 <- f(host, id, method)
          result2 <- f(host, id, method)
          result3 <- f(host, id, method)
          result4 <- f(host, id, method)
          result5 <- f(host, id, method)
          result6 <- f(host, id, method)
          result7 <- f(host, id, method)
          result8 <- f(host, id, method)
        } yield Set(result1, result2, result3, result4, result5, result6, result7, result8)

        result onComplete {
          case Success(res) => {
            //Do Nothing
          }
          case Failure(error) => {
            log.warning("Error in sendRequest->Page: {}", error)
          }
        }
        //        result onComplete { _ => master ! UserSimComplete(method, 5) }
      }
      case Models.Post => {
        val f = RequestLevelApiPost(_: String, _: String, _: HttpMethod)(context.system)
        val result = for {
          result1 <- f(host, id, method)
          result2 <- f(host, id, method)
          result3 <- f(host, id, method)
          result4 <- f(host, id, method)
          result5 <- f(host, id, method)
          result6 <- f(host, id, method)
          result7 <- f(host, id, method)
          result8 <- f(host, id, method)
        } yield Set(result1, result2, result3, result4, result5, result6, result7, result8)

        result onComplete {
          case Success(res) => {
            for (p <- res) {
              try {
                //First verify the authenticity of message using signature
                val otherPublicKey: RSAPublicKey = user_publicKeys.getOrElse(p.from, pu)
                if (rsa.verifySignature(p.msg, p.signature, otherPublicKey)) {
                  //Decrypt secret key using private key
                  val key: String = rsa.decryptUsingPrivateKey(p.key, pk)
                  //Obtain secretKey from key string
                  val secretKey: SecretKey = new SecretKeySpec(decoder.decode(key), "AES")
                  //Decrypt message contents using secretKey
                  val msg: String = aes.decrypt(p.msg, secretKey)
                  //Print message
                  println(msg)
                } else {
                  //Signature verification failed
                }
              } catch {
                case e: Exception => {
                  //An Exception occured
                }
              }
            }
          }
          case Failure(error) => {
            log.warning("Error in sendRequest->Post: {}", error)
          }
        }
        //        result onComplete { _ => master ! UserSimComplete(method, 5) }
      }
    }
    //Tell master 8 GET requests has been made
    master ! userGET(8)
  }

}
