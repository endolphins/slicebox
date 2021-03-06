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

package se.nimsa.sbx.app.routing

import akka.pattern.ask
import spray.http.StatusCodes.NoContent
import spray.httpx.SprayJsonSupport._
import spray.routing._
import se.nimsa.sbx.app.RestApi
import se.nimsa.sbx.dicom.DicomHierarchy._
import se.nimsa.sbx.dicom.DicomProtocol._
import se.nimsa.sbx.dicom.DicomProtocol.SourceType._
import se.nimsa.sbx.app.UserProtocol._
import se.nimsa.sbx.box.BoxProtocol._
import scala.concurrent.Future

trait MetadataRoutes { this: RestApi =>

  def metaDataRoutes: Route = {
    pathPrefix("metadata") {
      path("sources") {
        get {
          val futureSources =
            for {
              users <- userService.ask(GetUsers).mapTo[Users]
              boxes <- boxService.ask(GetBoxes).mapTo[Boxes]
              scps <- dicomService.ask(GetScps).mapTo[Scps]
              dirs <- dicomService.ask(GetWatchedDirectories).mapTo[WatchedDirectories]
            } yield {
              users.users.map(user => Source(SourceType.USER, user.user, user.id)) ++
                boxes.boxes.map(box => Source(SourceType.BOX, box.name, box.id)) ++
                scps.scps.map(scp => Source(SourceType.SCP, scp.name, scp.id)) ++
                dirs.directories.map(dir => Source(SourceType.DIRECTORY, dir.name, dir.id))
            }
          onSuccess(futureSources) {
            complete(_)
          }
        }
      } ~ pathPrefix("patients") {
        pathEndOrSingleSlash {
          get {
            parameters(
              'startindex.as[Long] ? 0,
              'count.as[Long] ? 20,
              'orderby.as[String].?,
              'orderascending.as[Boolean] ? true,
              'filter.as[String].?,
              'sourcetype.as[String].?,
              'sourceid.as[Long].?) { (startIndex, count, orderBy, orderAscending, filter, sourceType, sourceId) =>

                onSuccess(dicomService.ask(GetPatients(startIndex, count, orderBy, orderAscending, filter, sourceType.map(SourceType.withName(_)), sourceId))) {
                  case Patients(patients) =>
                    complete(patients)
                }
              }
          }
        } ~ path("query") {
          post {
            entity(as[Query]) { query =>
              onSuccess(dicomService.ask(QueryPatients(query))) {
                case Patients(patients) =>
                  complete(patients)
              }
            }
          }
        } ~ path(LongNumber) { patientId =>
          get {
            onSuccess(dicomService.ask(GetPatient(patientId)).mapTo[Option[Patient]]) {
              complete(_)
            }
          } ~ delete {
            onSuccess(dicomService.ask(DeletePatient(patientId))) {
              case ImageFilesDeleted(_) =>
                complete(NoContent)
            }
          }
        }
      } ~ pathPrefix("studies") {
        pathEndOrSingleSlash {
          get {
            parameters(
              'startindex.as[Long] ? 0,
              'count.as[Long] ? 20,
              'patientid.as[Long],
              'sourcetype.as[String].?,
              'sourceid.as[Long].?) { (startIndex, count, patientId, sourceType, sourceId) =>
                onSuccess(dicomService.ask(GetStudies(startIndex, count, patientId, sourceType.map(SourceType.withName(_)), sourceId))) {
                  case Studies(studies) =>
                    complete(studies)
                }
              }
          }
        } ~ path("query") {
          post {
            entity(as[Query]) { query =>
              onSuccess(dicomService.ask(QueryStudies(query))) {
                case Studies(studies) =>
                  complete(studies)
              }
            }
          }
        } ~ path(LongNumber) { studyId =>
          get {
            onSuccess(dicomService.ask(GetStudy(studyId)).mapTo[Option[Study]]) {
              complete(_)
            }
          } ~ delete {
            onSuccess(dicomService.ask(DeleteStudy(studyId))) {
              case ImageFilesDeleted(_) =>
                complete(NoContent)
            }
          }
        }
      } ~ pathPrefix("series") {
        pathEndOrSingleSlash {
          get {
            parameters(
              'startindex.as[Long] ? 0,
              'count.as[Long] ? 20,
              'studyid.as[Long],
              'sourcetype.as[String].?,
              'sourceid.as[Long].?) { (startIndex, count, studyId, sourceType, sourceId) =>
                onSuccess(dicomService.ask(GetSeries(startIndex, count, studyId, sourceType.map(SourceType.withName(_)), sourceId))) {
                  case SeriesCollection(series) =>
                    complete(series)
                }
              }
          }
        } ~ path("query") {
          post {
            entity(as[Query]) { query =>
              onSuccess(dicomService.ask(QuerySeries(query))) {
                case SeriesCollection(series) =>
                  complete(series)
              }
            }
          }
        } ~ path(LongNumber) { seriesId =>
          get {
            onSuccess(dicomService.ask(GetSingleSeries(seriesId)).mapTo[Option[Series]]) {
              complete(_)
            }
          } ~ delete {
            onSuccess(dicomService.ask(DeleteSeries(seriesId))) {
              case ImageFilesDeleted(_) =>
                complete(NoContent)
            }
          }
        } ~ path(LongNumber / "source") { seriesId =>
          get {
            onSuccess(dicomService.ask(GetSeriesSource(seriesId)).mapTo[Option[SeriesSource]]) { seriesSourceMaybe =>
              val futureSourceMaybe = seriesSourceMaybe.map(seriesSource => {
                val futureNameMaybe = seriesSource.sourceType match {
                  case USER      => userService.ask(GetUser(seriesSource.sourceId)).mapTo[Option[ApiUser]].map(_.map(_.user))
                  case BOX       => boxService.ask(GetBoxById(seriesSource.sourceId)).mapTo[Option[Box]].map(_.map(_.name))
                  case DIRECTORY => dicomService.ask(GetWatchedDirectoryById(seriesSource.sourceId)).mapTo[Option[WatchedDirectory]].map(_.map(_.name))
                  case SCP       => dicomService.ask(GetScpById(seriesSource.sourceId)).mapTo[Option[ScpData]].map(_.map(_.name))
                  case UNKNOWN   => Future(None)
                }
                val futureName: Future[String] = futureNameMaybe.map(_.getOrElse("<source removed>"))
                futureName.map(name => Some(Source(seriesSource.sourceType, name, seriesSource.sourceId)))    
              }).getOrElse(Future(None))
              onSuccess(futureSourceMaybe) {
                complete(_)
              }
            }
          }
        }
      } ~ pathPrefix("images") {
        pathEndOrSingleSlash {
          get {
            parameters(
              'startindex.as[Long] ? 0,
              'count.as[Long] ? 20,
              'seriesid.as[Long]) { (startIndex, count, seriesId) =>
                onSuccess(dicomService.ask(GetImages(startIndex, count, seriesId))) {
                  case Images(images) =>
                    complete(images)
                }
              }
          }
        } ~ path("query") {
          post {
            entity(as[Query]) { query =>
              onSuccess(dicomService.ask(QueryImages(query))) {
                case Images(images) =>
                  complete(images)
              }
            }
          }
        } ~ path(LongNumber) { imageId =>
          get {
            onSuccess(dicomService.ask(GetImage(imageId)).mapTo[Option[Image]]) {
              complete(_)
            }
          } ~ delete {
            onSuccess(dicomService.ask(DeleteImage(imageId))) {
              case ImageFilesDeleted(_) =>
                complete(NoContent)
            }
          }
        }
      } ~ path("flatseries") {
        pathEndOrSingleSlash {
          get {
            parameters(
              'startindex.as[Long] ? 0,
              'count.as[Long] ? 20,
              'orderby.as[String].?,
              'orderascending.as[Boolean] ? true,
              'filter.as[String].?,
              'sourcetype.as[String].?,
              'sourceid.as[Long].?) { (startIndex, count, orderBy, orderAscending, filter, sourceType, sourceId) =>
                onSuccess(dicomService.ask(GetFlatSeries(startIndex, count, orderBy, orderAscending, filter, sourceType.map(SourceType.withName(_)), sourceId))) {
                  case FlatSeriesCollection(flatSeries) =>
                    complete(flatSeries)
                }
              }
          }
        } ~ path(LongNumber) { seriesId =>
          get {
            onSuccess(dicomService.ask(GetSingleFlatSeries(seriesId)).mapTo[Option[FlatSeries]]) {
              complete(_)
            }
          }
        }
      }
    }
  }

}
