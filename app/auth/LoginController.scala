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

import play.api.Logging
import play.api.http.HeaderNames
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc._
import users.{User, UserLogin, UserService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class LoginController @Inject()(cc: ControllerComponents, userService: UserService, authService: AuthService)(
    implicit exec: ExecutionContext)
    extends AbstractController(cc)
    with Logging {

  def login: Action[JsValue] = Action.async(parse.json) { request =>
    request.body
      .validate[UserLogin]
      .fold(
        error => Future(BadRequest(Json.obj("message" -> s"Failed to login $error"))),
        data =>
          userService.isAllowed(data.login, data.password) map {
            case Failure(exception) =>
              Unauthorized(exception.toString)
            case Success(user) =>
              //generate JWT Token
              val token = authService.generateToken(user)

              //Send Cookie httpOnly
              val cookieToken = Cookie("MON-COOKIE",
                                       token,
                                       httpOnly = true,
                                       secure = false, //true if https only
                                       sameSite = Some(Cookie.SameSite.Lax),
                                       maxAge = Some(3600))

              //Send token
              Ok(jsonUser(user)).withCookies(cookieToken)
        }
      )
  }

  def logout: Action[AnyContent] = Action {
    //Remove COOKIE
    Ok.discardingCookies(DiscardingCookie("MON-COOKIE"))
  }

  def getCurrentUser(): Action[AnyContent] = Action.async { request =>
    //Get token from cookie
    val tokenOption = request.headers.get(HeaderNames.COOKIE) collect {
      case cookies: String =>
        val cookie = cookies.substring(cookies.indexOf("MON-COOKIE=") + "MON-COOKIE=".length)
        if (cookie.contains(';')) {
          cookie.substring(0, cookie.indexOf(';'))
        }
        else {
          cookie
        }
    }
    tokenOption match {
      case None => Future(Unauthorized)
      case Some(token) =>
        authService.validateJwt(token) match {
          case Success(claim) =>
            val userToken = authService.getUserAppFromToken(claim)

            userService.getUserByID(userToken.id) map {
              case None        => Unauthorized("User was not authenticate in database")
              case Some(user1) => Ok(jsonUser(user1))
            }
          case Failure(err) =>
            logger.error(s"Token is not a valid token : ${err.getMessage}")
            Future(Unauthorized("User token is not valid"))
        }
    }
  }

  private def jsonUser(user: User): JsObject = {
    Json.obj(
      "id" -> user.id,
      "login" -> user.login,
      "roles" -> user.roles,
      "group" -> user.groups
    )
  }
}
