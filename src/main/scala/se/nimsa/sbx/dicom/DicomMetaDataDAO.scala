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

package se.nimsa.sbx.dicom

import scala.slick.driver.JdbcProfile
import scala.slick.jdbc.{ GetResult, StaticQuery => Q }
import DicomProtocol._
import DicomHierarchy._
import DicomPropertyValue._
import scala.slick.jdbc.meta.MTable

class DicomMetaDataDAO(val driver: JdbcProfile) {
  import driver.simple._

  // *** Patient *** 

  protected[dicom] val toPatient = (id: Long, patientName: String, patientID: String, patientBirthDate: String, patientSex: String) =>
    Patient(id, PatientName(patientName), PatientID(patientID), PatientBirthDate(patientBirthDate), PatientSex(patientSex))

  protected[dicom] val fromPatient = (patient: Patient) => Option((patient.id, patient.patientName.value, patient.patientID.value, patient.patientBirthDate.value, patient.patientSex.value))

  protected[dicom] class Patients(tag: Tag) extends Table[Patient](tag, "Patients") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def patientName = column[String](DicomProperty.PatientName.name)
    def patientID = column[String](DicomProperty.PatientID.name)
    def patientBirthDate = column[String](DicomProperty.PatientBirthDate.name)
    def patientSex = column[String](DicomProperty.PatientSex.name)
    def * = (id, patientName, patientID, patientBirthDate, patientSex) <> (toPatient.tupled, fromPatient)
  }

  protected[dicom] val patientsQuery = TableQuery[Patients]

  protected[dicom] val fromStudy = (study: Study) => Option((study.id, study.patientId, study.studyInstanceUID.value, study.studyDescription.value, study.studyDate.value, study.studyID.value, study.accessionNumber.value, study.patientAge.value))

  // *** Study *** //

  protected[dicom] val toStudy = (id: Long, patientId: Long, studyInstanceUID: String, studyDescription: String, studyDate: String, studyID: String, accessionNumber: String, patientAge: String) =>
    Study(id, patientId, StudyInstanceUID(studyInstanceUID), StudyDescription(studyDescription), StudyDate(studyDate), StudyID(studyID), AccessionNumber(accessionNumber), PatientAge(patientAge))

  protected[dicom] class Studies(tag: Tag) extends Table[Study](tag, "Studies") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def patientId = column[Long]("patientId")
    def studyInstanceUID = column[String](DicomProperty.StudyInstanceUID.name)
    def studyDescription = column[String](DicomProperty.StudyDescription.name)
    def studyDate = column[String](DicomProperty.StudyDate.name)
    def studyID = column[String](DicomProperty.StudyID.name)
    def accessionNumber = column[String](DicomProperty.AccessionNumber.name)
    def patientAge = column[String](DicomProperty.PatientAge.name)
    def * = (id, patientId, studyInstanceUID, studyDescription, studyDate, studyID, accessionNumber, patientAge) <> (toStudy.tupled, fromStudy)

    def patientFKey = foreignKey("patientFKey", patientId, patientsQuery)(_.id, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)
    def patientIdJoin = patientsQuery.filter(_.id === patientId)
  }

  protected[dicom] val studiesQuery = TableQuery[Studies]

  // *** Equipment ***

  protected[dicom] val toEquipment = (id: Long, manufacturer: String, stationName: String) =>
    Equipment(id, Manufacturer(manufacturer), StationName(stationName))

  protected[dicom] val fromEquipment = (equipment: Equipment) => Option((equipment.id, equipment.manufacturer.value, equipment.stationName.value))

  protected[dicom] class Equipments(tag: Tag) extends Table[Equipment](tag, "Equipments") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def manufacturer = column[String](DicomProperty.Manufacturer.name)
    def stationName = column[String](DicomProperty.StationName.name)
    def * = (id, manufacturer, stationName) <> (toEquipment.tupled, fromEquipment)
  }

  protected[dicom] val equipmentsQuery = TableQuery[Equipments]

  // *** Frame of Reference ***

  protected[dicom] val toFrameOfReference = (id: Long, frameOfReferenceUID: String) =>
    FrameOfReference(id, FrameOfReferenceUID(frameOfReferenceUID))

  protected[dicom] val fromFrameOfReference = (frameOfReference: FrameOfReference) => Option((frameOfReference.id, frameOfReference.frameOfReferenceUID.value))

  protected[dicom] class FrameOfReferences(tag: Tag) extends Table[FrameOfReference](tag, "FrameOfReferences") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def frameOfReferenceUID = column[String](DicomProperty.FrameOfReferenceUID.name)
    def * = (id, frameOfReferenceUID) <> (toFrameOfReference.tupled, fromFrameOfReference)
  }

  protected[dicom] val frameOfReferencesQuery = TableQuery[FrameOfReferences]

  // *** Series ***

  protected[dicom] val toSeries = (id: Long, studyId: Long, equipmentId: Long, frameOfReferenceId: Long, seriesInstanceUID: String, seriesDescription: String, seriesDate: String, modality: String, protocolName: String, bodyPartExamined: String) =>
    Series(id, studyId, equipmentId, frameOfReferenceId, SeriesInstanceUID(seriesInstanceUID), SeriesDescription(seriesDescription), SeriesDate(seriesDate), Modality(modality), ProtocolName(protocolName), BodyPartExamined(bodyPartExamined))

  protected[dicom] val fromSeries = (series: Series) => Option((series.id, series.studyId, series.equipmentId, series.frameOfReferenceId, series.seriesInstanceUID.value, series.seriesDescription.value, series.seriesDate.value, series.modality.value, series.protocolName.value, series.bodyPartExamined.value))

  protected[dicom] class SeriesTable(tag: Tag) extends Table[Series](tag, "Series") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def studyId = column[Long]("studyId")
    def equipmentId = column[Long]("equipmentId")
    def frameOfReferenceId = column[Long]("frameOfReferenceId")
    def seriesInstanceUID = column[String](DicomProperty.SeriesInstanceUID.name)
    def seriesDescription = column[String](DicomProperty.SeriesDescription.name)
    def seriesDate = column[String](DicomProperty.SeriesDate.name)
    def modality = column[String](DicomProperty.Modality.name)
    def protocolName = column[String](DicomProperty.ProtocolName.name)
    def bodyPartExamined = column[String](DicomProperty.BodyPartExamined.name)
    def * = (id, studyId, equipmentId, frameOfReferenceId, seriesInstanceUID, seriesDescription, seriesDate, modality, protocolName, bodyPartExamined) <> (toSeries.tupled, fromSeries)

    def studyFKey = foreignKey("studyFKey", studyId, studiesQuery)(_.id, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)
    def equipmentFKey = foreignKey("equipmentFKey", equipmentId, equipmentsQuery)(_.id, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)
    def frameOfReferenceFKey = foreignKey("frameOfReferenceFKey", frameOfReferenceId, frameOfReferencesQuery)(_.id, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)
    def studyIdJoin = studiesQuery.filter(_.id === studyId)
  }

  protected[dicom] val seriesQuery = TableQuery[SeriesTable]

  // *** Image ***

  protected[dicom] val toImage = (id: Long, seriesId: Long, sopInstanceUID: String, imageType: String, instanceNumber: String) =>
    Image(id, seriesId, SOPInstanceUID(sopInstanceUID), ImageType(imageType), InstanceNumber(instanceNumber))

  protected[dicom] val fromImage = (image: Image) => Option((image.id, image.seriesId, image.sopInstanceUID.value, image.imageType.value, image.instanceNumber.value))

  protected[dicom] class Images(tag: Tag) extends Table[Image](tag, "Images") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def seriesId = column[Long]("seriesId")
    def sopInstanceUID = column[String](DicomProperty.SOPInstanceUID.name)
    def imageType = column[String](DicomProperty.ImageType.name)
    def instanceNumber = column[String](DicomProperty.InstanceNumber.name)
    def * = (id, seriesId, sopInstanceUID, imageType, instanceNumber) <> (toImage.tupled, fromImage)

    def seriesFKey = foreignKey("seriesFKey", seriesId, seriesQuery)(_.id, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)
    def seriesIdJoin = seriesQuery.filter(_.id === seriesId)
  }

  protected[dicom] val imagesQuery = TableQuery[Images]

  def create(implicit session: Session) =
    if (MTable.getTables("Patients").list.isEmpty)
      (patientsQuery.ddl ++
        studiesQuery.ddl ++
        equipmentsQuery.ddl ++
        frameOfReferencesQuery.ddl ++
        seriesQuery.ddl ++
        imagesQuery.ddl).create

  def drop(implicit session: Session) =
    if (MTable.getTables("Patients").list.size > 0)
      (patientsQuery.ddl ++
        studiesQuery.ddl ++
        equipmentsQuery.ddl ++
        frameOfReferencesQuery.ddl ++
        seriesQuery.ddl ++
        imagesQuery.ddl).drop

  def columnExists(tableName: String, columnName: String)(implicit session: Session): Boolean = {
    val tables = MTable.getTables(tableName).list
    if (tables.isEmpty)
      false
    else
      !tables(0).getColumns.list.filter(_.name == columnName).isEmpty
  }

  def checkOrderBy(orderBy: Option[String], tableNames: String*)(implicit session: Session) =
    orderBy.foreach(columnName =>
      if (!tableNames.exists(tableName =>
        columnExists(tableName, columnName)))
        throw new IllegalArgumentException(s"Property $columnName does not exist"))

  // *** Get entities by id

  def patientById(id: Long)(implicit session: Session): Option[Patient] =
    patientsQuery.filter(_.id === id).list.headOption

  def studyById(id: Long)(implicit session: Session): Option[Study] =
    studiesQuery.filter(_.id === id).list.headOption

  def seriesById(id: Long)(implicit session: Session): Option[Series] =
    seriesQuery.filter(_.id === id).list.headOption

  def equipmentById(id: Long)(implicit session: Session): Option[Equipment] =
    equipmentsQuery.filter(_.id === id).list.headOption

  def frameOfReferenceById(id: Long)(implicit session: Session): Option[FrameOfReference] =
    frameOfReferencesQuery.filter(_.id === id).list.headOption

  def imageById(id: Long)(implicit session: Session): Option[Image] =
    imagesQuery.filter(_.id === id).list.headOption

  // *** Inserts ***

  def insert(patient: Patient)(implicit session: Session): Patient = {
    val generatedId = (patientsQuery returning patientsQuery.map(_.id)) += patient
    patient.copy(id = generatedId)
  }

  def insert(study: Study)(implicit session: Session): Study = {
    val generatedId = (studiesQuery returning studiesQuery.map(_.id)) += study
    study.copy(id = generatedId)
  }

  def insert(series: Series)(implicit session: Session): Series = {
    val generatedId = (seriesQuery returning seriesQuery.map(_.id)) += series
    series.copy(id = generatedId)
  }

  def insert(frameOfReference: FrameOfReference)(implicit session: Session): FrameOfReference = {
    val generatedId = (frameOfReferencesQuery returning frameOfReferencesQuery.map(_.id)) += frameOfReference
    frameOfReference.copy(id = generatedId)
  }

  def insert(equipment: Equipment)(implicit session: Session): Equipment = {
    val generatedId = (equipmentsQuery returning equipmentsQuery.map(_.id)) += equipment
    equipment.copy(id = generatedId)
  }

  def insert(image: Image)(implicit session: Session): Image = {
    val generatedId = (imagesQuery returning imagesQuery.map(_.id)) += image
    image.copy(id = generatedId)
  }

  // *** Listing all patients, studies etc ***

  def patientsGetResult = GetResult(r =>
    Patient(r.nextLong, PatientName(r.nextString), PatientID(r.nextString), PatientBirthDate(r.nextString), PatientSex(r.nextString)))

  def patients(startIndex: Long, count: Long, orderBy: Option[String], orderAscending: Boolean, filter: Option[String])(implicit session: Session): List[Patient] = {

    checkOrderBy(orderBy, "Patients")

    implicit val getResult = patientsGetResult

    var query = """select * from "Patients""""

    filter.foreach(filterValue => {
      val filterValueLike = s"'%$filterValue%'".toLowerCase
      query += s""" where 
        lcase("PatientName") like $filterValueLike or 
          lcase("PatientID") like $filterValueLike or 
            lcase("PatientBirthDate") like $filterValueLike or 
              lcase("PatientSex") like $filterValueLike"""
    })

    orderBy.foreach(orderByValue =>
      query += s""" order by "$orderByValue" ${if (orderAscending) "asc" else "desc"}""")

    query += s""" limit $count offset $startIndex"""

    Q.queryNA(query).list
  }

  def queryPatients(startIndex: Long, count: Long, orderBy: Option[String], orderAscending: Boolean, queryProperties: Seq[QueryProperty])(implicit session: Session): List[Patient] = {

    checkOrderBy(orderBy, "Patients")

    implicit val getResult = patientsGetResult

    val querySelectPart = """select distinct("Patients"."id"),
      "Patients"."PatientName",
      "Patients"."PatientID",
      "Patients"."PatientBirthDate",
      "Patients"."PatientSex" from "Patients"
      left join "Studies" on "Studies"."patientId" = "Patients"."id"
      left join "Series" on "Series"."studyId" = "Studies"."id"
      left join "Equipments" on "Equipments"."id" = "Series"."equipmentId"
      left join "FrameOfReferences" on "FrameOfReferences"."id" = "Series"."frameOfReferenceId""""

    val query = buildMetaDataQuery(querySelectPart, startIndex, count, orderBy, orderAscending, queryProperties)

    Q.queryNA(query).list
  }

  def studies(implicit session: Session): List[Study] = studiesQuery.list

  def queryStudies(startIndex: Long, count: Long, orderBy: Option[String], orderAscending: Boolean, queryProperties: Seq[QueryProperty])(implicit session: Session): List[Study] = {

    checkOrderBy(orderBy, "Studies")

    implicit val getResult = GetResult(r =>
      Study(r.nextLong, r.nextLong, StudyInstanceUID(r.nextString), StudyDescription(r.nextString), StudyDate(r.nextString), StudyID(r.nextString), AccessionNumber(r.nextString), PatientAge(r.nextString)))

    val querySelectPart = """select distinct("Studies"."id"),
      "Studies"."patientId",
      "Studies"."StudyInstanceUID",
      "Studies"."StudyDescription",
      "Studies"."StudyDate",
      "Studies"."StudyID",
      "Studies"."AccessionNumber",
      "Studies"."PatientAge" from "Studies"
      left join "Patients" on "Patients"."id" = "Studies"."patientId"
      left join "Series" on "Series"."studyId" = "Studies"."id"
      left join "Equipments" on "Equipments"."id" = "Series"."equipmentId"
      left join "FrameOfReferences" on "FrameOfReferences"."id" = "Series"."frameOfReferenceId""""

    val query = buildMetaDataQuery(querySelectPart, startIndex, count, orderBy, orderAscending, queryProperties)

    Q.queryNA(query).list
  }

  def querySeries(startIndex: Long, count: Long, orderBy: Option[String], orderAscending: Boolean, queryProperties: Seq[QueryProperty])(implicit session: Session): List[Series] = {

    checkOrderBy(orderBy, "Series")

    implicit val getResult = GetResult(r =>
      Series(r.nextLong, r.nextLong, r.nextLong, r.nextLong, SeriesInstanceUID(r.nextString), SeriesDescription(r.nextString), SeriesDate(r.nextString), Modality(r.nextString), ProtocolName(r.nextString), BodyPartExamined(r.nextString)))

    val querySelectPart = """select distinct("Series"."id"),
      "Series"."studyId",
      "Series"."equipmentId",
      "Series"."frameOfReferenceId",
      "Series"."SeriesInstanceUID",
      "Series"."SeriesDescription",
      "Series"."SeriesDate",
      "Series"."Modality",
      "Series"."ProtocolName",
      "Series"."BodyPartExamined" from "Series"
      left join "Studies" on "Studies"."id" = "Series"."studyId"
      left join "Patients" on "Patients"."id" = "Studies"."patientId"
      left join "Equipments" on "Equipments"."id" = "Series"."equipmentId"
      left join "FrameOfReferences" on "FrameOfReferences"."id" = "Series"."frameOfReferenceId""""

    val query = buildMetaDataQuery(querySelectPart, startIndex, count, orderBy, orderAscending, queryProperties)

    Q.queryNA(query).list
  }

  def queryImages(startIndex: Long, count: Long, orderBy: Option[String], orderAscending: Boolean, queryProperties: Seq[QueryProperty])(implicit session: Session): List[Image] = {

    checkOrderBy(orderBy, "Images")

    implicit val getResult = GetResult(r =>
      Image(r.nextLong, r.nextLong, SOPInstanceUID(r.nextString), ImageType(r.nextString), InstanceNumber(r.nextString)))

    val querySelectPart = """select distinct("Images"."id"),
      "Images"."seriesId",
      "Images"."SOPInstanceUID",
      "Images"."ImageType",
      "Images"."InstanceNumber" from "Images"
      left join "Series" on "Series"."id" = "Images"."seriesId"
      left join "Studies" on "Studies"."id" = "Series"."studyId"
      left join "Patients" on "Patients"."id" = "Studies"."patientId"
      left join "Equipments" on "Equipments"."id" = "Series"."equipmentId"
      left join "FrameOfReferences" on "FrameOfReferences"."id" = "Series"."frameOfReferenceId""""

    val query = buildMetaDataQuery(querySelectPart, startIndex, count, orderBy, orderAscending, queryProperties)

    Q.queryNA(query).list
  }

  def buildMetaDataQuery(selectPart: String, startIndex: Long, count: Long, orderBy: Option[String], orderAscending: Boolean, queryProperties: Seq[QueryProperty]): String = {
    var query = selectPart

    val wherePart = queryProperties.map(queryPropertyToPart(_)).mkString(" and ")
    if (wherePart.length > 0) query += s" where $wherePart"

    orderBy.foreach(orderByValue =>
      query += s""" order by "$orderByValue" ${if (orderAscending) "asc" else "desc"}""")

    query += s""" limit $count offset $startIndex"""

    query
  }

  def queryPropertyToPart(queryProperty: QueryProperty) =
    s""""${queryProperty.propertyName}" ${queryProperty.operator.toString()} '${queryProperty.propertyValue}'"""

  def series(implicit session: Session): List[Series] = seriesQuery.list

  def equipments(implicit session: Session): List[Equipment] = equipmentsQuery.list

  def frameOfReferences(implicit session: Session): List[FrameOfReference] = frameOfReferencesQuery.list

  def images(implicit session: Session): List[Image] = imagesQuery.list

  val flatSeriesQuery = """select "Series"."id", 
      "Patients"."id", "Patients"."PatientName", "Patients"."PatientID", "Patients"."PatientBirthDate","Patients"."PatientSex", 
      "Studies"."id", "Studies"."patientId", "Studies"."StudyInstanceUID", "Studies"."StudyDescription", "Studies"."StudyDate", "Studies"."StudyID", "Studies"."AccessionNumber", "Studies"."PatientAge",
      "Equipments"."id", "Equipments"."Manufacturer", "Equipments"."StationName",
      "FrameOfReferences"."id", "FrameOfReferences"."FrameOfReferenceUID",
      "Series"."id", "Series"."studyId", "Series"."equipmentId", "Series"."frameOfReferenceId", "Series"."SeriesInstanceUID", "Series"."SeriesDescription", "Series"."SeriesDate", "Series"."Modality", "Series"."ProtocolName", "Series"."BodyPartExamined"
       from "Series" 
       inner join "Studies" on "Series"."studyId" = "Studies"."id" 
       inner join "Equipments" on "Series"."equipmentId" = "Equipments"."id"
       inner join "FrameOfReferences" on "Series"."frameOfReferenceId" = "FrameOfReferences"."id"
       inner join "Patients" on "Studies"."patientId" = "Patients"."id""""

  def flatSeriesGetResult = GetResult(r =>
    FlatSeries(r.nextLong,
      Patient(r.nextLong, PatientName(r.nextString), PatientID(r.nextString), PatientBirthDate(r.nextString), PatientSex(r.nextString)),
      Study(r.nextLong, r.nextLong, StudyInstanceUID(r.nextString), StudyDescription(r.nextString), StudyDate(r.nextString), StudyID(r.nextString), AccessionNumber(r.nextString), PatientAge(r.nextString)),
      Equipment(r.nextLong, Manufacturer(r.nextString), StationName(r.nextString)),
      FrameOfReference(r.nextLong, FrameOfReferenceUID(r.nextString)),
      Series(r.nextLong, r.nextLong, r.nextLong, r.nextLong, SeriesInstanceUID(r.nextString), SeriesDescription(r.nextString), SeriesDate(r.nextString), Modality(r.nextString), ProtocolName(r.nextString), BodyPartExamined(r.nextString))))

  def flatSeries(startIndex: Long, count: Long, orderBy: Option[String], orderAscending: Boolean, filter: Option[String])(implicit session: Session): List[FlatSeries] = {

    checkOrderBy(orderBy, "Patients", "Studies", "Equipments", "FrameOfReferences", "Series")

    implicit val getResult = flatSeriesGetResult

    var query = flatSeriesQuery

    filter.foreach(filterValue => {
      val filterValueLike = s"'%$filterValue%'".toLowerCase
      query += s""" where 
        lcase("Series"."id") like $filterValueLike or
          lcase("PatientName") like $filterValueLike or 
          lcase("PatientID") like $filterValueLike or 
          lcase("PatientBirthDate") like $filterValueLike or 
          lcase("PatientSex") like $filterValueLike or
            lcase("StudyDescription") like $filterValueLike or
            lcase("StudyDate") like $filterValueLike or
            lcase("StudyID") like $filterValueLike or
            lcase("AccessionNumber") like $filterValueLike or
            lcase("PatientAge") like $filterValueLike or
              lcase("Manufacturer") like $filterValueLike or
              lcase("StationName") like $filterValueLike or
                lcase("SeriesDescription") like $filterValueLike or
                lcase("SeriesDate") like $filterValueLike or
                lcase("Modality") like $filterValueLike or
                lcase("ProtocolName") like $filterValueLike or
                lcase("BodyPartExamined") like $filterValueLike"""
    })

    orderBy.foreach(orderByValue =>
      query += s""" order by "$orderByValue" ${if (orderAscending) "asc" else "desc"}""")

    query += s""" limit $count offset $startIndex"""

    Q.queryNA(query).list
  }

  def flatSeriesById(seriesId: Long)(implicit session: Session): Option[FlatSeries] = {

    implicit val getResult = flatSeriesGetResult
    val query = flatSeriesQuery + s""" where "Series"."id" = $seriesId"""

    Q.queryNA(query).list.headOption
  }

  // *** Grouped listings ***

  def studiesForPatient(startIndex: Long, count: Long, patientId: Long)(implicit session: Session): List[Study] =
    studiesQuery
      .filter(_.patientId === patientId)
      .drop(startIndex)
      .take(count)
      .list

  def seriesForStudy(startIndex: Long, count: Long, studyId: Long)(implicit session: Session): List[Series] =
    seriesQuery
      .filter(_.studyId === studyId)
      .drop(startIndex)
      .take(count)
      .list

  def imagesForSeries(startIndex: Long, count: Long, seriesId: Long)(implicit session: Session): List[Image] =
    imagesQuery
      .filter(_.seriesId === seriesId)
      .drop(startIndex)
      .take(count)
      .list

  def patientByNameAndID(patient: Patient)(implicit session: Session): Option[Patient] =
    patientsQuery
      .filter(_.patientName === patient.patientName.value)
      .filter(_.patientID === patient.patientID.value)
      .list.headOption

  def studyByUid(study: Study)(implicit session: Session): Option[Study] =
    studiesQuery
      .filter(_.studyInstanceUID === study.studyInstanceUID.value)
      .list.headOption

  def equipmentByManufacturerAndStationName(equipment: Equipment)(implicit session: Session): Option[Equipment] =
    equipmentsQuery
      .filter(_.manufacturer === equipment.manufacturer.value)
      .filter(_.stationName === equipment.stationName.value)
      .list.headOption

  def frameOfReferenceByUid(frameOfReference: FrameOfReference)(implicit session: Session): Option[FrameOfReference] =
    frameOfReferencesQuery
      .filter(_.frameOfReferenceUID === frameOfReference.frameOfReferenceUID.value)
      .list.headOption

  def seriesByUid(series: Series)(implicit session: Session): Option[Series] =
    seriesQuery
      .filter(_.seriesInstanceUID === series.seriesInstanceUID.value)
      .list.headOption

  def imageByUid(image: Image)(implicit session: Session): Option[Image] =
    imagesQuery
      .filter(_.sopInstanceUID === image.sopInstanceUID.value)
      .list.headOption

  // *** Deletes ***

  def deletePatient(patientId: Long)(implicit session: Session): Int = {
    patientsQuery
      .filter(_.id === patientId)
      .delete
  }

  def deleteStudy(studyId: Long)(implicit session: Session): Int = {
    studiesQuery
      .filter(_.id === studyId)
      .delete
  }

  def deleteSeries(seriesId: Long)(implicit session: Session): Int = {
    seriesQuery
      .filter(_.id === seriesId)
      .delete
  }

  def deleteFrameOfReference(frameOfReferenceId: Long)(implicit session: Session): Int = {
    frameOfReferencesQuery
      .filter(_.id === frameOfReferenceId)
      .delete
  }

  def deleteEquipment(equipmentId: Long)(implicit session: Session): Int = {
    equipmentsQuery
      .filter(_.id === equipmentId)
      .delete
  }

  def deleteImage(imageId: Long)(implicit session: Session): Int = {
    imagesQuery
      .filter(_.id === imageId)
      .delete
  }

}
