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

import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

// A custom request type to hold our JWT claims, we can pass these on to the handling action
case class UserRequest[A](request: Request[A]) extends WrappedRequest[A](request)

// Our custom action implementation
class AuthAction @Inject()(bodyParser: BodyParsers.Default, authService: AuthService)(implicit ec: ExecutionContext)
    extends ActionBuilder[UserRequest, AnyContent] {

  var permissionNeeded = ""

  def async(permission: String)(block: => Future[play.api.mvc.Result]): play.api.mvc.Action[play.api.mvc.AnyContent] = {
    this.permissionNeeded = permission
    super.async(block)
  }

  def async[A](permission: String, bodyParser: BodyParser[A])(block: Request[A] => Future[Result]): Action[A] = {
    this.permissionNeeded = permission
    super.async(bodyParser)(block)
  }

  override def parser: BodyParser[AnyContent] = bodyParser
  override protected def executionContext: ExecutionContext = ec

  /**
    * Called when a request is invoked. We should validate the bearer token here
    * and allow the request to proceed if it is valid.
    */
  override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {

    val userTokenOption = request.attrs.get[UserToken](UserToken.keyToken)

    userTokenOption match {
      case None =>
        Future.successful(Results.Unauthorized(s"No token found for this user"))
      case Some(userToken: UserToken) =>
        //User is allowed here and now Check if request on a group - check if action is Authorized on this Action
        val isUserAuthorized = userToken.isAdmin ||
          (userToken.permissions contains permissionNeeded)

        if (authService.ifSecureActived && isUserAuthorized || !authService.ifSecureActived) {
          block(UserRequest(request))
        }
        else {
          Future.successful(Results.Unauthorized(s"User has not permissions to do this action"))
        }
    }
  }

}
