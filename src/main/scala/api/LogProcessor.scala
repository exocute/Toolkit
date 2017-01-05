package api

import java.io.FileWriter
import java.text.{DateFormat, SimpleDateFormat}
import java.util.Date

import clifton.nodes.{ExoEntry, Log, SpaceCache}
import clifton.signals.LoggingSignal
import com.zink.fly.FlyPrime

/**
  * Created by #ScalaTeam on 04/01/2017.
  */
object LogProcessor extends Thread {

  SpaceCache.signalHost = "localhost"
  private val space: FlyPrime = SpaceCache.getSignalSpace
  private val TAKETIME = 0L
  private val tmpl = new ExoEntry("LOG", null)
  private val INTERVALTIME = 1000
  private val dateFormat: DateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

  override def run(): Unit = {
    while (true) {
      val res = space.take(tmpl, TAKETIME)
      if (res != null) {
        val date: Date = new Date()
        System.out.println(dateFormat.format(date))
        val file: FileWriter = new FileWriter("logAux.txt", true)
        val log = res.payload.asInstanceOf[LoggingSignal]
        file.write(dateFormat.format(date) + ";" + log.getLogMessage + "\n")
        file.close()
      }
      Thread.sleep(INTERVALTIME)
    }

  }

}

