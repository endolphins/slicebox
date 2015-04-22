package se.vgregion.app.routing

import akka.pattern.ask

import spray.http.StatusCodes.NoContent
import spray.httpx.SprayJsonSupport._
import spray.routing._

import se.vgregion.app.RestApi
import se.vgregion.dicom.DicomHierarchy._
import se.vgregion.dicom.DicomProtocol._

trait MetadataRoutes { this: RestApi =>

  def metaDataRoutes: Route = {
    pathPrefix("metadata") {
      pathPrefix("patients") {
        pathEndOrSingleSlash {
          get {
            parameters(
              'startindex.as[Long] ? 0,
              'count.as[Long] ? 20,
              'orderby.as[String].?,
              'orderascending.as[Boolean] ? true,
              'filter.as[String].?) { (startIndex, count, orderBy, orderAscending, filter) =>

                onSuccess(dicomService.ask(GetPatients(startIndex, count, orderBy, orderAscending, filter))) {
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
              'patientid.as[Long]) { (startIndex, count, patientId) =>
                onSuccess(dicomService.ask(GetStudies(startIndex, count, patientId))) {
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
              'studyid.as[Long]) { (startIndex, count, studyId) =>
                onSuccess(dicomService.ask(GetSeries(startIndex, count, studyId))) {
                  case SeriesCollection(series) =>
                    complete(series)
                }
              }
          } ~ get {
            parameters(
              'startindex.as[Long] ? 0,
              'count.as[Long] ? 20,
              'orderby.as[String].?,
              'orderascending.as[Boolean] ? true,
              'filter.as[String].?) { (startIndex, count, orderBy, orderAscending, filter) =>
                onSuccess(dicomService.ask(GetFlatSeries(startIndex, count, orderBy, orderAscending, filter))) {
                  case FlatSeriesCollection(flatSeries) =>
                    complete(flatSeries)
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
        }
      } ~ path("images") {
        get {
          parameters('seriesid.as[Long]) { seriesId =>
            onSuccess(dicomService.ask(GetImages(seriesId))) {
              case Images(images) =>
                complete(images)
            }
          }
        }
      }
    }
  }

}