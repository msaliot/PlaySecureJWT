package users

import play.api.libs.json.Json

case class UserLogin(login: String, password: String)

object UserLogin {
  implicit val reads = Json.reads[UserLogin]
}
