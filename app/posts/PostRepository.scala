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

import java.util.concurrent.ConcurrentHashMap
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * Example. Just to illustrate general authorisation in filter url
  */
@Singleton
class PostRepository @Inject()(implicit exec: ExecutionContext) {

  // Specify a couple of posts for our API to serve up
  private val postsGroup1: ConcurrentHashMap[Int, Post] = new ConcurrentHashMap()
  postsGroup1.put(1, Post(1, "group1", "Group1 : Post 1"))
  postsGroup1.put(2, Post(2, "group1", "Group1 : Post 2"))

  private val postsGroup2: ConcurrentHashMap[Int, Post] = new ConcurrentHashMap()
  postsGroup1.put(1, Post(1, "group2", "Group2 : Post 1"))
  postsGroup1.put(2, Post(2, "group2", "Group2 : Post 2"))

  def getGroupPost(group: String): ConcurrentHashMap[Int, Post] = {
    group match {
      case "group1" => postsGroup1
      case "group2" => postsGroup2
    }
  }

  /*
   * Returns a blog post that matches the specified id, or None if no
   * post was found (collectFirst returns None if the function is undefined for the
   * given post id)
   */
  def getPost(group: String, postId: Int): Future[Try[Post]] = {
    Future(
      Try {
        val post = getGroupPost(group).get(postId)
        if (post == null) {
          throw new NoSuchElementException(s"$postId was not found in this group")
        }
        post
      }
    )
  }

  def deletePost(group: String, id: Int): Future[Try[Unit]] = {
    Future(Try {
      getGroupPost(group).remove(id)
      ()
    })
  }

}
