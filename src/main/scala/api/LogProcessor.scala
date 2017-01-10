package api

import java.io.FileWriter
import java.text.{DateFormat, SimpleDateFormat}
import java.util.Date

import com.zink.fly.FlyPrime
import exonode.clifton.Protocol
import exonode.clifton.node.{ExoEntry, SpaceCache}
import exonode.clifton.signals.LoggingSignal

/**
  * Created by #ScalaTeam on 04/01/2017.
  */
object LogProcessor extends Thread {

  private val space: FlyPrime = SpaceCache.getSignalSpace
  private val TAKETIME = 0L
  private val tmpl = new ExoEntry(Protocol.LOG_MARKER, null)
  private val INTERVALTIME = 1000
  private val dateFormat: DateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

  override def run(): Unit = {
    while (true) {
      val res = space.take(tmpl, TAKETIME)
      if (res != null) {
        val date: Date = new Date()
//        System.out.println(dateFormat.format(date))
        val file: FileWriter = new FileWriter("log.txt", true)
        val log = res.payload.asInstanceOf[LoggingSignal]
        file.write(dateFormat.format(date) + ";" + log.getLogMessage + "\n")
        file.close()
      } else
        Thread.sleep(INTERVALTIME)
    }

  }

}

