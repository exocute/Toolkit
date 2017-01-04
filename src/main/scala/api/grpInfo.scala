package api

import toolkit.{ActivityRep, GraphRep}

/**
  * Created by #ScalaTeam on 02/01/2017.
  */
class grpInfo(grpID: String, actID: Vector[String]) {

  def getName = grpID

  def getActID = actID
}
