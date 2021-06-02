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
package filters

import akka.stream.Materializer
import auth.{AuthService, UserToken}
import play.api.http.HeaderNames
import play.api.mvc.{Filter, RequestHeader, Result, Results}
import play.api.{Configuration, Logging}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class HttpPermissionFilter @Inject()(authService: AuthService, config: Configuration)(implicit val mat: Materializer,
                                                                                      ec: ExecutionContext)
    extends Filter
    with Logging {

  //url pattern matching on witch url is filtered and user need authorisation to access this url
  private val accessNeededFilter = """/auth/(.+?)""".r
  private val accessNeededFilterGroup = """/auth/group(.+?)""".r
  private def safePath(path: String): String =
    if (path.endsWith("/")) {
      path
    }
    else {
      path + "/"
    }

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {

    val pathWithoutFilter = safePath(config.get[String]("play.http.context"))
    val isSecureActivated = authService.ifSecureActived //If security JWT Activation in application.conf
    val isPathToSecure = accessNeededFilter.findPrefixOf(requestHeader.path) match {
      case Some(_) => true
      case None    => false
    }

    if (!isSecureActivated || !isPathToSecure) {
      nextFilter(requestHeader).map { result =>
        result
      }
    }
    else {
      extractCookieToken(requestHeader) map { token =>
        authService.validateJwt(token) match {
          case Success(claim) =>
            val userToken = authService.getUserAppFromToken(claim)
            //add parameters in the Request for future Actions (AuthAction)
            val requestHeaderWithData = requestHeader
              .addAttr(UserToken.keyToken, userToken)

            val isAuthorizedGroup = checkPathGroupIfAuthorized(requestHeader.path, userToken)

            if (isAuthorizedGroup._1) {
              nextFilter(requestHeaderWithData).map { result =>
                result
              }
            }
            else {
              Future.successful(
                Results.BadRequest(s"User is not allowed to execute command on Group ${isAuthorizedGroup._2}"))
            }
          case Failure(t) => Future.successful(Results.Unauthorized(t.getMessage)) // token was invalid - return 401
        }
      } getOrElse Future.successful(Results.Unauthorized) // no token was sent - return 401
    }
  }

  /**
    * Check actions allowed: if user allowed to use group specified
    * @param url url
    * @param userToken JWT token
    * @return Tuple (if group is authorized, Group Selected)
    */
  def checkPathGroupIfAuthorized(url: String, userToken: UserToken): (Boolean, String) = {

    if (userToken.isAdmin) {
      (true, "") //Group authorized
    }
    else {
      //Test path
      val authorizedGroup: (Boolean, String) = accessNeededFilterGroup.findFirstIn(url) match {
        case Some(urlGroup) =>
          //Must control selected Group
          val selectedGroup = urlGroup.split("/")(2).toLowerCase
          (userToken.authorizedGroups.contains(selectedGroup), selectedGroup)
        case None => (true, "") //No group selection in path - request is ok
      }
      authorizedGroup
    }
  }

  // Helper for extracting the token value
  private def extractCookieToken[A](requestHeader: RequestHeader): Option[String] = {

    //Get token from cookie
    requestHeader.headers.get(HeaderNames.COOKIE) collect {
      case cookies: String =>
        //Get token cookie content if several cookies
        val cookie = cookies.substring(cookies.indexOf("MON-COOKIE=") + "MON-COOKIE=".length)
        if (cookie.contains(';')) {
          cookie.substring(0, cookie.indexOf(';'))
        }
        else {
          cookie
        }
    }
  }
}
