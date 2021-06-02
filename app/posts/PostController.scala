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
package posts

import auth.AuthAction
import com.google.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import scala.util.{Failure, Success}
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
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
@Singleton
class PostController @Inject()(cc: ControllerComponents, authAction: AuthAction, postRepository: PostRepository)(
    implicit exec: ExecutionContext)
    extends AbstractController(cc) {

  /**
    * Get post from a specific group
    * @param group name of the group
    * @param postId post identifier
    * @return post
    */
  def getPost(group: String, postId: Int): Action[AnyContent] = authAction.async("USER") {
    postRepository.getPost(group, postId) map {
      case Failure(ex)   => BadRequest(s"${ex.getMessage}")
      case Success(post) => Ok(Json.toJson(post))
    }
  }

  /**
    * Remove post on a specific group
    * @param group Name of the group
    * @param postId post identifier
    */
  def deletePost(group: String, postId: Int): Action[AnyContent] = authAction.async("ADMIN") {
    postRepository.deletePost(group, postId) map {
      case Failure(ex) => BadRequest(ex.getMessage)
      case Success(_)  => Ok(s"Post in $group with id $postId was deleted successfully")
    }
  }

}
