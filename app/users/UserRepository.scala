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

import com.github.t3hnar.bcrypt._

import java.util.{NoSuchElementException, UUID}
import javax.inject.{Inject, Singleton}
import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * UserRepository as a fake DB. Replace with your own implementation
  * All method return a Future to have same behavior as a DB with async driver
  */
@Singleton
class UserRepository @Inject()(implicit exec: ExecutionContext) {

  private val usersMemory: collection.concurrent.Map[String, User] = TrieMap[String, User]()

  //admin: password: "123456" ;-)
  val userAdmin: User = User(UUID.randomUUID().toString,
                             "admin",
                             "$2a$12$t72LbSGlYeO6XRoC1pL/IecBtk4Vamg28PTpaM2x/PK5F8e8u7P56".bcryptSafe(12).get,
                             List("ADMIN"),
                             List(""))
  //user1 password: "AZERTY1"
  val user1: User = User(UUID.randomUUID().toString,
                         "user1",
                         "$2a$12$hUsosq3K8zxptKtTzogWceVzi3BIqKfnRJ283yY84V5t2SzTPFas2".bcryptSafe(12).get,
                         List("USER"),
                         List("group1"))
  //user2 password: "AZERTY2"
  val user2: User = User(UUID.randomUUID().toString,
                         "user2",
                         "$2a$12$lX5qdL3xx56p1A0lkhpxSeSMjppuXJ/73UVE5sJ5PUuKbZn1VQAQG".bcryptSafe(12).get,
                         List("USER"),
                         List("group2"))

  usersMemory += (userAdmin.id -> userAdmin)
  usersMemory += (user1.id -> user1)
  usersMemory += (user2.id -> user2)

  def findByLogin(login: String): Future[Option[User]] = {
    Future(
      usersMemory.collectFirst {
        case (_, user) if user.login == login => user
      }
    )
  }

  def get(id: String): Future[Option[User]] = {
    Future(usersMemory.get(id))
  }

  def loginUser(login: String, password: String): Future[Try[User]] = {
    usersMemory.collectFirst {
      case (_, user) if user.login == login => user
    } match {
      case None => Future(Failure(new NoSuchElementException()))
      case Some(user) =>
        if (password == user.password)
          Future(Success(user))
        else
          Future(Failure(new IllegalAccessException()))
    }
  }

  def create(user: User): Future[Try[User]] = {
    usersMemory += (user.id -> user)
    Future(Success(user))
  }

  def delete(id: String): Future[Try[Unit]] = {
    Future(usersMemory.remove(id) match {
      case None    => Failure(new NoSuchElementException(s"id $id was not found"))
      case Some(_) => Success(())
    })
  }

}
