package clifton.nodes

/**
  * Created by #ScalaTeam on 20/12/2016.
  */
object Log {

  var logLevel: State = Info

  var log:LoggingSinal = LoggingSinal

  val outChannel:OutChannel = SignalOut

  var local:Boolean = true

  def sendMessage(msg: String) = {
    log.setLogLevel(logLevel)
    log.setLogMessage(formatMessage(msg))
    outChannel.putObject(log)
    if(local) println(formatMessage(msg))
    SpaceCache.
  }

  def info(msg: String) = logLevel match {
    case Info => sendMessage(msg)
    case _ => ()
  }

  def error(msg: String) = {
    sendMessage(msg)
  }

  def setLogLevel(s:State) = {
    logLevel = s
    info("Set logging level to "+ logLevel)
  }

  def formatMessage(msg:String) =  msg+"edited"


}

sealed trait State

case object Info extends State{
  override def toString: String = "INFO"
}

case object Error extends State{
  override def toString: String = "ERROR"
}
