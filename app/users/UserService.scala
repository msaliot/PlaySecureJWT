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
import play.api.Logger

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class UserService @Inject()(userRepository: UserRepository)(implicit exec: ExecutionContext) {

  private val logger = Logger(this.getClass)

  /**
    *
    * @param login
    * @param password
    * @return
    */
  def isAllowed(login: String, password: String): Future[Try[User]] = {
    userRepository.findByLogin(login) map {
      case Some(user: User) =>
        password.isBcryptedSafe(user.password) match {
          case Success(_) =>
            logger.info(s"Authentication for login $login")
            Success(user)
          case Failure(exception) =>
            logger.error(s"Authentication failed for login $login : ${exception.getMessage}")
            Failure(new IllegalAccessException(s"User ${user.login} password is not valid"))
        }
      case None => Failure(new NoSuchElementException(s"User $login was not found"))
    }
  }

  def getUserByID(userID: String): Future[Option[User]] = {
    userRepository.get(userID)
  }

  def create(userInput: UserInput): Future[Try[User]] = {
    //Crypt password
    val cryptPasswdTry = userInput.password.bcryptSafe(12)

    cryptPasswdTry match {
      case Failure(ex) => Future(Failure(ex))
      case Success(passwdCrypted) =>
        val user = User(UUID.randomUUID().toString, userInput.login, passwdCrypted, userInput.roles, userInput.groups)
        userRepository.create(user)
    }
  }

  def delete(userID: UUID): Future[Try[Unit]] = {
    userRepository.delete(userID.toString)
  }

}
