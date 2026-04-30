/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.disareturnsstubs.controllers

object StubResourceFilename {

  private val sizeMappings: Seq[(String, String)] = Seq(
    "-2xl" -> "1000000",
    "-xl"  -> "100000",
    "-l"   -> "10000",
    "-m"   -> "5000",
    "-s"   -> "1000",
    "-xs"  -> "100"
  )

  def resolve(requestedFilename: String): String =
    sizeMappings.collectFirst {
      case (marker, fileName) if requestedFilename.contains(marker) => s"$fileName${extensionFor(requestedFilename)}"
    }.getOrElse(requestedFilename)

  private def extensionFor(requestedFilename: String): String =
    if (requestedFilename.contains("xlsx")) ".xlsx"
    else if (requestedFilename.contains("csv")) ".csv"
    else ""
}
