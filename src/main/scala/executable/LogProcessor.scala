package executable

import java.io.FileWriter
import java.text.{DateFormat, SimpleDateFormat}
import java.util.Date
import java.util.concurrent.LinkedBlockingDeque

import exonode.clifton.config.ProtocolConfig
import exonode.clifton.signals.Log.{Log, LogType}
import exonode.clifton.node.SpaceCache
import exonode.clifton.node.entries.ExoEntry

/**
  * Created by #GrowinScala
  *
  * Reads all log entries and writes them in a local file
  */
object LogProcessor extends Thread {

  val LogFile: String = "log.cfv"

  setDaemon(true)

  private val IntervalTime = 1000
  private val MaxLogsCalls = 20

  private val logTemplate = ExoEntry[LogType](ProtocolConfig.LogMarker, null)
  private val dateFormat: DateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
  private val logs = new LinkedBlockingDeque[LogType]()
  private val analyseFile = new SystemAnalyser(2, logs)

  override def run(): Unit = {
    val space = SpaceCache.getSignalSpace
    println("LogProcessor Started...")

    analyseFile.start()
    while (true) {
      val res: Iterable[ExoEntry[LogType]] = space.takeMany(logTemplate, MaxLogsCalls)
      if (res.nonEmpty) {
        val file: FileWriter = new FileWriter(LogFile, true)
        val date: Date = new Date()
        for (exoEntry <- res) {
          val log = exoEntry.payload
          logs.push(log)
          file.write(dateFormat.format(date) + ProtocolConfig.LogSeparator + log.logType + ProtocolConfig.LogSeparator + log.message + "\n")
        }
        file.close()
      } else
        Thread.sleep(IntervalTime)
    }
  }

  def main(args: Array[String]): Unit = {
    if (args.length > 0)
      SpaceCache.signalHost = args(0)
    LogProcessor.start()
    LogProcessor.join()
  }
}

