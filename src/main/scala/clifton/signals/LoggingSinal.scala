package clifton.signals

import clifton.LogLevel

/**
  * Created by #ScalaTeam on 20/12/2016.
  */
class LoggingSinal extends Serializable {

  private var logLevel: LogLevel = _
  private var logMessage: String = _

  def getLogLevel: LogLevel = logLevel

  def setLogLevel(logLevel: LogLevel) = this.logLevel = logLevel

  def getLogMessage: String = logMessage

  def setLogMessage(logMessage: String) = this.logMessage = logMessage
}
