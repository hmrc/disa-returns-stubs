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

package uk.gov.hmrc.disareturnsstubs.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.Scenario
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SERVICE_UNAVAILABLE}
import play.api.libs.json.Json
import uk.gov.hmrc.disareturnsstubs.BaseISpec
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext

class UpscanProxyConnectorISpec extends BaseISpec {

  private val initiatePath = "/upscan/v2/initiate"

  private lazy val connector = inject[UpscanProxyConnector]

  private implicit val hc: HeaderCarrier    = HeaderCarrier()
  private implicit val ec: ExecutionContext = ExecutionContext.global

  "initiate" should {

    "retry three times when the downstream service keeps returning a server error" in {
      stubFor(
        post(urlEqualTo(initiatePath))
          .willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE).withBody("upscan unavailable"))
      )

      connector.initiate(Json.obj()).failed.futureValue shouldBe
        UpstreamErrorResponse("upscan unavailable", SERVICE_UNAVAILABLE)

      verify(4, postRequestedFor(urlEqualTo(initiatePath)))
    }

    "succeed when the downstream service recovers during retries" in {
      stubFor(
        post(urlEqualTo(initiatePath))
          .inScenario("upscan recovers")
          .whenScenarioStateIs(Scenario.STARTED)
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("first failure"))
          .willSetStateTo("second attempt")
      )
      stubFor(
        post(urlEqualTo(initiatePath))
          .inScenario("upscan recovers")
          .whenScenarioStateIs("second attempt")
          .willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE).withBody("second failure"))
          .willSetStateTo("recovered")
      )
      stubFor(
        post(urlEqualTo(initiatePath))
          .inScenario("upscan recovers")
          .whenScenarioStateIs("recovered")
          .willReturn(aResponse().withStatus(OK).withBody("success"))
      )

      val response = connector.initiate(Json.obj()).futureValue

      response.status shouldBe OK
      response.body shouldBe "success"
      verify(3, postRequestedFor(urlEqualTo(initiatePath)))
    }

    "not retry client errors" in {
      stubFor(post(urlEqualTo(initiatePath)).willReturn(aResponse().withStatus(BAD_REQUEST)))

      connector.initiate(Json.obj()).futureValue.status shouldBe BAD_REQUEST

      verify(1, postRequestedFor(urlEqualTo(initiatePath)))
    }
  }
}
