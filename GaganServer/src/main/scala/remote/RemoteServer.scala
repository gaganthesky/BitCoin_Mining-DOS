package remote

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import java.security.MessageDigest
import akka.routing.RoundRobinRouter

case class Mine() //to start the actor for mining - generate a random string	
case class Done() //to stop mining
case class Display(bitcoin: String, shaValue: String) //to Display the bitcoin found
case class FindBitCoins(k: Int, gatorId: String, mineString:String) //to determine the bitcoins

object HelloRemote extends App  {
  	println("Enter the required number of leading zeros")
    val k = readInt
    val gatorId = "umargagan" 		
	val system = ActorSystem("RemoteSystem")
	val remoteActor = system.actorOf(Props(new RemoteActor(k)), name = "RemoteActor")
	remoteActor ! "The RemoteActor is alive"
	val master = system.actorOf(Props(new Miner(gatorId,k)),name = "master")
    master ! Mine()

}

class RemoteActor(k:Int) extends Actor {
  def receive = {
    case msg: String =>
        println(s"Server received message: ")
        if(msg contains "Bitcoin"){
        	println("From Client: " + msg)
        }
        else{
        	println(msg)
        	/* 
        	*as a proof of concept, we found 6 random string which 
        	*result in bitcoins with 4 leading zeros and sent those
        	*strings with a hundred thousand other random strings
        	*/
	        var toSend = "4zGCVQo6kD 49UxKOTYhl 4fl4B4lhpb 4jOQqN60Co 4kXxXXYeQV 4zlLC4YbV0"
	        val coinArray = toSend.split(" ")
	        for(j<-0 to coinArray.length - 1){
	        	sender ! coinArray(j)
	        } 
	        //sender ! toSend
	        for (i<-0 to 100000){
	        	toSend = k+scala.util.Random.alphanumeric.take(9).mkString+""
	        	sender ! toSend
	        	if (i == 99999){
	        		toSend = "9"+scala.util.Random.alphanumeric.take(9).mkString+""
	        		sender ! toSend
	        	}	
	    	}
    	}	
  	}
}

class Miner(gatorId: String, k:Int) extends Actor{
	var runningTime:Int = 0
	def receive = {
		/*
		* The actor is responsible for starting the mining process
		*/
		case Mine() => {
    		val processes = Runtime.getRuntime().availableProcessors()
    		println("Processors: " + processes)
    		val actorcount=(3*processes)/2
    		println("Actor Count: " + actorcount)
    		val actorWorker = context.actorOf(Props[Worker].withRouter(RoundRobinRouter(actorcount)))	
			var mineString = ""
			/*
			*generate random threads and send it to the miner. 
			*/
			println("starting")
			for (i<-0 to 900000){
				mineString = scala.util.Random.alphanumeric.take(9).mkString
				actorWorker ! FindBitCoins(k,gatorId,mineString)
			}
			
		}
		//to stop mining
		case Done() => {
			runningTime += 1 
			if(runningTime == 1000000){
				println("I'm done broski")
				context.system.shutdown()
			}
		}
		case Display(bitcoin, shaValue) => {
			//print the bitcoin
			println("Bitcoin: " + bitcoin+ "	" + shaValue)
		}
		case _=>{
			println("anything")
		}
	}
}

//worker
class Worker extends Actor{
	def receive = {
		case FindBitCoins(k, gatorId, mineString) => {
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
		    for ( k <- 0 to k-1){
		    	if (hexValue.toString().charAt(k) == '0'){
		    		count += 1
		    	}
		    }
		    if(count == k){
		    	//echo the bitcoin back here
		    	sender ! Display(distinctCoin,hexValue.toString())
		    }
			sender ! Done()
		} 
	}
}