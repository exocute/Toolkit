package clifton.nodes

import clifton.{Info, LogLevel, exoSignal}
import clifton.signals.LoggingSignal

/**
  * Created by #ScalaTeam on 20/12/2016.
  */
object Log {

  private var logLevel: LogLevel = Info

  private var log: LoggingSignal = new LoggingSignal

  private val outChannel: OutChannel = new SignalOutChannel("LOG")

  private val local: Boolean = true

  def sendMessage(msg: String) = {
    log.setLogLevel(logLevel)
    log.setLogMessage(formatMessage(msg))
    outChannel.putObject(log)
    if (local) println(formatMessage(msg))
  }

  def info(msg: String) = logLevel match {
    case Info => sendMessage(msg)
    case _ => ()
  }

  def error(msg: String) = {
    sendMessage(msg)
  }

  def setLogLevel(logLevel: LogLevel) = {
    this.logLevel = logLevel
    info("Set logging level to " + this.logLevel)
  }

  private var sb = new StringBuilder

  private def formatMessage(msg: String) = {
    // check an exception and catch the resulting stuff
    val stack = new Throwable().getStackTrace
    sb.setLength(0)
    sb.append(stack(4).getClassName)
    sb.append(":")
    sb.append(stack(4).getMethodName)
    sb.append(":")
    sb.append(stack(4).getLineNumber)
    sb.append(":")
    sb + msg
  }

}
