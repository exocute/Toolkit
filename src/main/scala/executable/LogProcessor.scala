package executable

import java.io.FileWriter
import java.text.{DateFormat, SimpleDateFormat}
import java.util.Date

import exonode.clifton.Protocol
import exonode.clifton.node.entries.ExoEntry
import exonode.clifton.node.SpaceCache
import exonode.clifton.signals.LoggingSignal

/**
  * Created by #ScalaTeam on 04/01/2017.
  *
  * Reads all log entries and writes them in a local file
  */
object LogProcessor extends Thread {

  setDaemon(true)

  private val space = SpaceCache.getSignalSpace
  private val logTemplate = ExoEntry(Protocol.LOG_MARKER, null)
  private val INTERVAL_TIME = 1000
  private val dateFormat: DateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
  private val MAX_LOGS_CALL = 20

  override def run(): Unit = {
    while (true) {
      val res: Iterable[ExoEntry] = space.takeMany(logTemplate, MAX_LOGS_CALL)
      if (res.nonEmpty) {
        val file: FileWriter = new FileWriter("log.txt", true)
        val date: Date = new Date()
        for (exoEntry <- res) {
          val log = exoEntry.payload.asInstanceOf[LoggingSignal]
          file.write(dateFormat.format(date) + ";" + log.logLevel + ";" + log.logMessage + "\n")
        }
        file.close()
      } else
        Thread.sleep(INTERVAL_TIME)
    }
  }

}

