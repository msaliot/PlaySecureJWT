/*
 * Copyright (C) 2021 Marc SALIOT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package auth

import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtJson}
import play.api.{Configuration, Logger}
import play.api.libs.json.{JsError, JsSuccess, Json}
import users.User

import java.time.{Clock, Instant}
import java.util.UUID
import javax.inject.Inject
import scala.util.Try

class AuthService @Inject()(config: Configuration) {

  private val logger: Logger = Logger(this.getClass)

  implicit val clock: Clock = Clock.systemUTC()

  private val key = config.get[String]("myapp.jwt.secret")
  private val cookieDuration = config.get[Int]("myapp.jwt.cookie.duration")
  private val algo = JwtAlgorithm.HS256
  private val issuerToken = "mycompany"

  private val ifSecureActive = {
    val ifSecureActive: Boolean = config.getOptional[Boolean]("myapp.jwt.active") match {
      case Some(bool) => bool
      case None =>
        logger.info("Value 'myapp.jwt.active' was not in application.conf file - set to default value false")
        false
    }

    if (ifSecureActive) logger.info("Security activate !!!!! HttpPermissionFilter is on")
    else logger.info("No Security activate !!!!!")

    ifSecureActive
  }

  def ifSecureActived: Boolean = ifSecureActive

  /**
    * Generate token from user data
    * @param user user
    * @return JWT token
    */
  def generateToken(user: User): String = {

    val permissions = Json.obj("permission" -> user.roles)
    val groups = Json.obj("groups" -> user.groups)
    val userID = Json.obj("userID" -> user.id)

    //Create claim
    val claim: JwtClaim = JwtClaim(
      issuer = Some(issuerToken),
      subject = Some(user.login),
      expiration = Some(Instant.now.getEpochSecond + cookieDuration), //in 1 hour
      jwtId = Some(UUID.randomUUID().toString) //JWT token id
    ) + userID.toString() + permissions.toString() + groups.toString() //private claims

    val token = JwtJson.encode(claim, key, algo)

    token
  }

  /**
    * Token validation
    * @param token JWT token to validate
    * @return claim
    */
  def validateJwt(token: String): Try[JwtClaim] = {
    JwtJson.decode(token, key, Seq(algo)) map { claim =>
      //check expiration token duration
      val now = Instant.now().getEpochSecond
      if (claim.expiration.getOrElse(now - 1) < now) {
        throw new IllegalAccessException("Token JWT is expired !")
      }
      claim
    }
  }

  def getUserAppFromToken(claim: JwtClaim): UserToken = {

    val token = Json.parse(claim.content)

    val userID = (token \ "userID").validate[String] match {
      case JsSuccess(userID, _) => userID
      case _: JsError           => ""
    }

    val permissions: List[String] = (token \ "permission").validate[List[String]] match {
      case JsSuccess(permissionsList, _) => permissionsList.map(_.toUpperCase)
      case _: JsError                    => List[String]() //TODO Logger
    }

    val authorizedGroups: List[String] = (token \ "groups").validate[List[String]] match {
      case JsSuccess(groupList, _) => groupList.map(_.toLowerCase)
      case _: JsError              => List[String]()
    }

    val isAdmin = permissions.contains("ADMIN")

    UserToken(userID, "login", isAdmin, authorizedGroups, permissions)
  }

}
