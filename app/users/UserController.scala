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
package users

import auth.AuthAction
import com.google.inject.Inject
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class UserController @Inject()(cc: ControllerComponents, authAction: AuthAction, userService: UserService)(
    implicit exec: ExecutionContext)
    extends AbstractController(cc) {

  /**
    * Get user
    */
  def getUser(userID: String): Action[AnyContent] = authAction.async("USER") {
    userService.getUserByID(userID) map {
      case None       => BadRequest(Json.obj("message" -> s"User with $userID was not found"))
      case Some(user) => Ok(Json.toJson(user))
    }
  }

  def create(): Action[JsValue] = authAction.async("ADMIN", parse.json) { request =>
    request.body
      .validate[UserInput]
      .fold(
        error => Future(BadRequest(Json.obj("message" -> s"UserService create : Bad format of JSON : $error"))),
        userInput =>
          userService.create(userInput) map {
            case Failure(ex) =>
              BadRequest(Json.obj("message" -> ex.getMessage))
            case Success(user) =>
              Ok(Json.obj("user" -> Json.toJson(user)))
        }
      )
  }

}
