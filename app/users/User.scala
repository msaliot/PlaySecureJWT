package users

import play.api.libs.json.Json

case class User(id: String, login: String, password: String, roles: List[String], groups: List[String])

object User {
  implicit val format = Json.format[User]
}
