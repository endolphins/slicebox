/*
 * Copyright 2015 Lars Edenbrandt
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
 */

package se.nimsa.sbx.directory

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import scala.language.postfixOps
import akka.actor.Actor
import akka.actor.PoisonPill
import akka.actor.Props
import akka.event.Logging
import akka.event.LoggingReceive
import se.nimsa.sbx.app.DbProps
import akka.actor.Status.Failure
import se.nimsa.sbx.util.ExceptionCatching
import DirectoryWatchProtocol._

class DirectoryWatchServiceActor(dbProps: DbProps, storage: Path) extends Actor with ExceptionCatching {
  val log = Logging(context.system, this)

  val db = dbProps.db
  val dao = new DirectoryWatchDAO(dbProps.driver)

  setupDb()
  setupWatches()

  log.info("Directory watch service started")

  def receive = LoggingReceive {

    case msg: DirectoryRequest =>
      catchAndReport {

        msg match {

          case WatchDirectory(name, pathString) =>
            watchedDirectoryForPath(pathString) match {
              case Some(watchedDirectory) =>
                if (name == watchedDirectory.name)
                  sender ! watchedDirectory
                else
                  throw new IllegalArgumentException(s"Directory watch ${watchedDirectory.name} already watches directory " + pathString)

              case None =>

                val path = Paths.get(pathString)

                if (!Files.isDirectory(path))
                  throw new IllegalArgumentException("Could not create directory watch: Not a directory: " + pathString)

                if (Files.isSameFile(path, storage))
                  throw new IllegalArgumentException("The storage directory may not be watched.")

                getWatchedDirectories.map(dir => Paths.get(dir.path)).foreach(other =>
                  if (path.startsWith(other) || other.startsWith(path))
                    throw new IllegalArgumentException("Directory intersects existing directory " + other))

                val watchedDirectory = addDirectory(name, pathString)

                context.child(watchedDirectory.id.toString).getOrElse(
                  context.actorOf(DirectoryWatchActor.props(watchedDirectory), watchedDirectory.id.toString))

                sender ! watchedDirectory
            }

          case UnWatchDirectory(watchedDirectoryId) =>
            watchedDirectoryForId(watchedDirectoryId).foreach(dir => deleteDirectory(watchedDirectoryId))
            context.child(watchedDirectoryId.toString).foreach(_ ! PoisonPill)
            sender ! DirectoryUnwatched(watchedDirectoryId)

          case GetWatchedDirectories =>
            val directories = getWatchedDirectories()
            sender ! WatchedDirectories(directories)

          case GetWatchedDirectoryById(id) =>
            db.withSession { implicit session =>
              sender ! dao.watchedDirectoryForId(id)
            }
        }
      }

  }

  def setupDb() =
    db.withSession { implicit session =>
      dao.create
    }

  def setupWatches() =
    db.withTransaction { implicit session =>
      val watchedDirectories = dao.allWatchedDirectories
      watchedDirectories foreach (watchedDirectory => {
        val path = Paths.get(watchedDirectory.path)
        if (Files.isDirectory(path))
          context.actorOf(DirectoryWatchActor.props(watchedDirectory), watchedDirectory.id.toString)
        else
          deleteDirectory(watchedDirectory.id)
      })
    }

  def addDirectory(name: String, path: String): WatchedDirectory =
    db.withSession { implicit session =>
      dao.insert(WatchedDirectory(-1, name, path))
    }

  def deleteDirectory(id: Long) =
    db.withSession { implicit session =>
      dao.deleteWatchedDirectoryWithId(id)
    }

  def watchedDirectoryForId(watchedDirectoryId: Long) =
    db.withSession { implicit session =>
      dao.watchedDirectoryForId(watchedDirectoryId)
    }

  def watchedDirectoryForPath(path: String) =
    db.withSession { implicit session =>
      dao.watchedDirectoryForPath(path)
    }

  def getWatchedDirectories() =
    db.withSession { implicit session =>
      dao.allWatchedDirectories
    }

}

object DirectoryWatchServiceActor {
  def props(dbProps: DbProps, storage: Path): Props = Props(new DirectoryWatchServiceActor(dbProps, storage))
}