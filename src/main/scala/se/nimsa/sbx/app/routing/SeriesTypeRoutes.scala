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

import spray.http.ContentTypes
import spray.http.HttpData
import spray.http.HttpEntity
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.routing._

import se.nimsa.sbx.app.AuthInfo
import se.nimsa.sbx.app.RestApi
import se.nimsa.sbx.seriestype.SeriesTypeProtocol._
import se.nimsa.sbx.app.UserProtocol.UserRole

trait SeriesTypeRoutes { this: RestApi =>

  def seriesTypeRoutes(authInfo: AuthInfo): Route =
    pathPrefix("seriestypes") {
      pathEndOrSingleSlash {
        get {
          onSuccess(seriesTypeService.ask(GetSeriesTypes)) {
            case SeriesTypes(seriesTypes) =>
              complete(seriesTypes)
          }
        }
      } ~ authorize(authInfo.hasPermission(UserRole.ADMINISTRATOR)) {
        pathEndOrSingleSlash {
          post {
            entity(as[SeriesType]) { seriesType =>
              onSuccess(seriesTypeService.ask(AddSeriesType(seriesType))) {
                case SeriesTypeAdded(seriesType) =>
                  complete((Created, seriesType))
              }
            }
          }
        } ~ path(LongNumber) { seriesTypeId =>
          put {
            entity(as[SeriesType]) { seriesType =>
              onSuccess(seriesTypeService.ask(UpdateSeriesType(seriesType))) {
                case SeriesTypeUpdated =>
                  complete(NoContent)
              }
            }
          } ~ delete {
            onSuccess(seriesTypeService.ask(RemoveSeriesType(seriesTypeId))) {
              case SeriesTypeRemoved(seriesTypeId) =>
                complete(NoContent)
            }
          }
        }
      }
  }
}
