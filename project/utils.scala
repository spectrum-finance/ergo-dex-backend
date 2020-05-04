import sbt.Keys.{moduleName, name}
import sbt.{Project, file}

object utils {

  def mkModule(id: String, description: String): Project =
    Project(id, file(s"modules/$id"))
      .settings(
        moduleName := id,
        name := description
      )
}
