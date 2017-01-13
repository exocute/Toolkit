package api

import java.io.FileWriter
import java.text.{DateFormat, SimpleDateFormat}
import java.util.Date

import com.zink.fly.Fly
import exonode.clifton.Protocol
import exonode.clifton.node.{ExoEntry, SpaceCache}
import exonode.clifton.signals.LoggingSignal
import scala.collection.JavaConverters._

/**
  * Created by #ScalaTeam on 04/01/2017.
  */
object LogProcessor extends Thread {

  private val space: Fly = SpaceCache.getSignalSpace
  private val tmpl = new ExoEntry(Protocol.LOG_MARKER, null)
  private val INTERVALTIME = 1000
  private val dateFormat: DateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
  private val MAX_LOGS_CALL = 20

  override def run(): Unit = {
    while (true) {
      val res: Iterable[ExoEntry] = space.takeMany(tmpl, MAX_LOGS_CALL).asScala
      if (res.nonEmpty) {
        val file: FileWriter = new FileWriter("log.txt", true)
        val date: Date = new Date()
        for (exoEntry <- res) {
          val log = exoEntry.payload.asInstanceOf[LoggingSignal]
          file.write(dateFormat.format(date) + ";" + log.getLogMessage + "\n")
        }
        file.close()
      } else
        Thread.sleep(INTERVALTIME)
    }

  }

}

