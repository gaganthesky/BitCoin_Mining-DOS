package local

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import java.security.MessageDigest
import akka.routing.RoundRobinRouter

case class FindBitCoins(k: Int, gatorId: String, mineString:String)
case class SendBitCoinBack(bitCoinFound: String)
object Local extends App {

  implicit val system = ActorSystem("LocalSystem")
  val localActor = system.actorOf(Props[LocalActor], name = "LocalActor")  // the local actor
  localActor ! "START"                                                     // start the action

}

class LocalActor extends Actor {

  // create the remote actor (Akka 2.1 syntax)
  val remote = context.actorFor("akka://RemoteSystem@127.0.0.1:5150/user/RemoteActor")
  var counter = 0

  def receive = {
    case "START" =>{
        remote ! "I'm ready to work"
    }
    case msg: String => {
        //println(s"LocalActor received message: $msg")
        val processes = Runtime.getRuntime().availableProcessors()
        //println("Processors: " + processes)
        val actorcount=(3*processes)/2
        val actorWorker = context.actorOf(Props[RemoteWorker].withRouter(RoundRobinRouter(actorcount)))
        val k:Int = msg.charAt(0).toInt - 48
        if (k == 9){
          println("Your work is done")
          context.system.shutdown()
        }
        //println("K: "+ k)
        val gatorId:String = "umarmajeed"
        val mineString:String = msg substring(1,msg.length)
        //println("Mine String: "+mineString)
        actorWorker!FindBitCoins(k,gatorId,mineString)
        /*if (counter < 5) {
            sender ! "Hello back to you"
            counter += 1
        }*/
    }
    case SendBitCoinBack(bitCoinFound) => {
      remote ! bitCoinFound 
    } 
  }
}

class RemoteWorker extends Actor{
  //var remoteMaster = context.actorFor("akka.tcp://LocalSystem@127.0.0.1:5151/user/RemoteActor1")
  def receive = {

    case FindBitCoins(k, gatorId, mineString) => {
      //println("found worker");
      val sha = MessageDigest.getInstance("SHA-256")
      val coinArray = mineString.split(" ")
      /*
      *for each string inside the buffer, find if it is the required bitcoin
      */
      val distinctCoin = gatorId+mineString
      sha.update(distinctCoin.getBytes("UTF-8"))
      val digest = sha.digest()
      val hexValue = new StringBuffer()
      //get the 64 bit hex value
      for ( j <- 0 to digest.length-1)   
      {
          val hex = Integer.toHexString(0xff & digest(j)); 
          if(hex.length() == 1) hexValue.append('0'); 
          hexValue.append(hex);
      }
      /*
      *check the first k bits, if the first k bits
      *are zero then the string found is the bitcoin
      */
      var count:Int = 0
      for ( a <- 0 to k-1){
        if (hexValue.toString().charAt(a) == '0'){
          count += 1
        }
      }
      if(count == k){
        //echo the bitcoin back here
        var bitcoinfound = "Bitcoin: " + distinctCoin + "   " + hexValue.toString()
        sender!SendBitCoinBack(bitcoinfound)
      }
      else{
        //println(distinctCoin + " not a bitcoin")
      }
      
    } 

  }
}