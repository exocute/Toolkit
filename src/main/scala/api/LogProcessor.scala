package api

import java.io.FileWriter

import clifton.nodes.{ExoEntry, SpaceCache}
import clifton.signals.LoggingSignal
import com.zink.fly.FlyPrime

/**
  * Created by #ScalaTeam on 04/01/2017.
  */
object LogProcessor extends Thread {

  SpaceCache.signalHost="localhost"
  val space : FlyPrime = SpaceCache.getSignalSpace
  val TAKETIME = 0L
  val tmpl = new ExoEntry("LOG",null)
  val INTERVALTIME = 100


  override def run(): Unit = {

    while(true){
      val res = space.take(tmpl,TAKETIME)
      if(res!=null){
        val file : FileWriter = new FileWriter("log.txt",true)
        val log = res.payload.asInstanceOf[LoggingSignal]
        file.write(log.getLogMessage+"\n")
        file.close()
      }
      Thread.sleep(INTERVALTIME)
    }

  }

}

