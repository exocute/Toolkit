package distributer

import java.io.{File, FilenameFilter};

/**
  * Created by #ScalaTeam on 21/12/2016.
  */
class ClassDistributer {
  def main(args: Array[String]): Unit = {
    val dir:File = new File(args(0))

    if(!dir.isDirectory)
      throw new Exception()
    else{
      //TODO
    }
  }
}
