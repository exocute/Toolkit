package executable

import java.io.{File, FileWriter}
import java.text.{DateFormat, SimpleDateFormat}
import java.util.Date
import java.util.concurrent.LinkedBlockingDeque

import exonode.clifton.config.Protocol._

import exonode.clifton.node.SpaceCache
import exonode.clifton.node.entries.ExoEntry
import exonode.clifton.signals.LoggingSignal

/**
  * Created by #ScalaTeam on 04/01/2017.
  *
  * Reads all log entries and writes them in a local file
  */
object LogProcessor extends Thread {

  val LOG_FILE: String = "log.cfv"

  setDaemon(true)

  private val logTemplate = ExoEntry(LOG_MARKER, null)
  private val INTERVAL_TIME = 1000
  private val dateFormat: DateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
  private val MAX_LOGS_CALL = 20
//  private val HEADER = "Date;Level;NodeID;GraphID;ActIDFROM;ActIDTO;InjID;Message"
  private val logs = new LinkedBlockingDeque[LoggingSignal]()
  private val analyseFile = new SystemAnalyser(2,logs)


  override def run(): Unit = {
    val space = SpaceCache.getSignalSpace
    println("LogProcessor Started...")

//    if (!new File(LOG_FILE).exists()) {
    //      val file: FileWriter = new FileWriter(LOG_FILE, false)
    //      file.write(HEADER)
    //    }
    analyseFile.start()
    while (true) {
      val res: Iterable[ExoEntry] = space.takeMany(logTemplate, MAX_LOGS_CALL)
      if (res.nonEmpty) {
        val file: FileWriter = new FileWriter(LOG_FILE, true)
        val date: Date = new Date()
        for (exoEntry <- res) {
          val log = exoEntry.payload.asInstanceOf[LoggingSignal]
          logs.push(log)
          file.write(dateFormat.format(date) + LOG_SEPARATOR + log.level + LOG_SEPARATOR + log.message + "\n")
        }
        file.close()
      } else
        Thread.sleep(INTERVAL_TIME)
    }
  }

  def main(args: Array[String]): Unit = {
    if (args.length > 0)
      SpaceCache.signalHost = args(0)
    LogProcessor.start()
    LogProcessor.join()
  }
}

