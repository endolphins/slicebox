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

package se.nimsa.sbx.anonymization

import scala.slick.driver.JdbcProfile
import org.h2.jdbc.JdbcSQLException
import scala.slick.jdbc.meta.MTable
import scala.slick.jdbc.{ GetResult, StaticQuery => Q }
import AnonymizationProtocol._

class AnonymizationDAO(val driver: JdbcProfile) {
  import driver.simple._
  
  val toAnonymizationKey = (id: Long, created: Long, patientName: String, anonPatientName: String, patientID: String, anonPatientID: String, patientBirthDate: String, studyInstanceUID: String, anonStudyInstanceUID: String, studyDescription: String, studyID: String, accessionNumber: String) =>
    AnonymizationKey(id, created, patientName, anonPatientName, patientID, anonPatientID, patientBirthDate, studyInstanceUID, anonStudyInstanceUID, studyDescription, studyID, accessionNumber)
  val fromAnonymizationKey = (entry: AnonymizationKey) =>
    Option((entry.id, entry.created, entry.patientName, entry.anonPatientName, entry.patientID, entry.anonPatientID, entry.patientBirthDate, entry.studyInstanceUID, entry.anonStudyInstanceUID, entry.studyDescription, entry.studyID, entry.accessionNumber))

  class AnonymizationKeyTable(tag: Tag) extends Table[AnonymizationKey](tag, "AnonymizationKey") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def created = column[Long]("created")
    def patientName = column[String]("patientname")
    def anonPatientName = column[String]("anonpatientname")
    def patientID = column[String]("patientid")
    def anonPatientID = column[String]("anonpatientid")
    def patientBirthDate = column[String]("patientbirthdate")
    def studyInstanceUID = column[String]("studyinstanceuid")
    def anonStudyInstanceUID = column[String]("anonstudyinstanceuid")
    def studyDescription = column[String]("studydescription")
    def studyID = column[String]("studyid")
    def accessionNumber = column[String]("accessionnumber")
    def * = (id, created, patientName, anonPatientName, patientID, anonPatientID, patientBirthDate, studyInstanceUID, anonStudyInstanceUID, studyDescription, studyID, accessionNumber) <> (toAnonymizationKey.tupled, fromAnonymizationKey)
  }

  val anonymizationKeyQuery = TableQuery[AnonymizationKeyTable]

  def create(implicit session: Session): Unit =
    if (MTable.getTables("AnonymizationKey").list.isEmpty) {
      anonymizationKeyQuery.ddl.create
    }

  def drop(implicit session: Session): Unit =
    anonymizationKeyQuery.ddl.drop

  def clear(implicit session: Session): Unit = {
    anonymizationKeyQuery.delete
  }
  
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
        
  def anonymizationKeys(startIndex: Long, count: Long, orderBy: Option[String], orderAscending: Boolean, filter: Option[String])(implicit session: Session): List[AnonymizationKey] = {

    checkOrderBy(orderBy, "AnonymizationKey")

    implicit val getResult = GetResult(r =>
      AnonymizationKey(r.nextLong, r.nextLong, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString, r.nextString))

    var query = """select * from "AnonymizationKey""""

    filter.foreach(filterValue => {
      val filterValueLike = s"'%$filterValue%'".toLowerCase
      query += s""" where 
        lcase("patientname") like $filterValueLike or 
          lcase("anonpatientname") like $filterValueLike or 
            lcase("patientid") like $filterValueLike or 
              lcase("anonpatientid") like $filterValueLike or
                lcase("accessionnumber") like $filterValueLike"""
    })

    orderBy.foreach(orderByValue =>
      query += s""" order by "$orderByValue" ${if (orderAscending) "asc" else "desc"}""")

    query += s""" limit $count offset $startIndex"""

    Q.queryNA(query).list
  }

  def insertAnonymizationKey(entry: AnonymizationKey)(implicit session: Session): AnonymizationKey = {
    val generatedId = (anonymizationKeyQuery returning anonymizationKeyQuery.map(_.id)) += entry
    entry.copy(id = generatedId)
  }

  def removeAnonymizationKey(anonymizationKeyId: Long)(implicit session: Session): Unit =
    anonymizationKeyQuery.filter(_.id === anonymizationKeyId).delete

  def anonymizationKeysForAnonPatient(anonPatientName: String, anonPatientID: String)(implicit session: Session): List[AnonymizationKey] =
    anonymizationKeyQuery
      .filter(_.anonPatientName === anonPatientName)
      .filter(_.anonPatientID === anonPatientID)
      .list

  def anonymizationKeysForPatient(patientName: String, patientID: String)(implicit session: Session): List[AnonymizationKey] =
    anonymizationKeyQuery
      .filter(_.patientName === patientName)
      .filter(_.patientID === patientID)
      .list

}
