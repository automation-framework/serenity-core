package net.serenitybdd.rest

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.serenity.test.utils.rules.TestCase
import net.serenitybdd.core.rest.RestQuery
import net.thucydides.core.steps.BaseStepListener
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import static net.serenitybdd.core.rest.RestMethod.POST
import static net.serenitybdd.rest.SerenityRest.*
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.matching
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static net.serenitybdd.rest.JsonConverter.*;
import static net.serenitybdd.rest.DecomposedContentType.*;

/**
 * User: YamStranger
 * Date: 11/29/15
 * Time: 5:58 PM
 */
class WhenRecordPostRequestsWithSerenityRest extends Specification {

    @Rule
    def WireMockRule wire = new WireMockRule(0);

    @Rule
    def TestCase<BaseStepListener> test = new TestCase({
        Mock(BaseStepListener);
    }.call());

    @Rule
    TemporaryFolder temporaryFolder

    def Gson gson = new GsonBuilder().setPrettyPrinting().
        serializeNulls().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();

    def "Should record RestAssured post() method calls"() {
        given:
            def JsonObject json = new JsonObject()
            json.addProperty("Number", "9999")
            json.addProperty("Price", "100")
            def body = gson.toJson(json)
            json.addProperty("SomeValue","value")
            def requestBody = gson.toJson(json)

            def base = "http://localhost:${wire.port()}"
            def path = "/test/number"
            def url = "$base$path"

            stubFor(WireMock.post(urlEqualTo(path))
                .withRequestBody(matching(".*"))
                .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "$APPLICATION_JSON")
                .withBody(body)));
        when:
            def result = given().contentType("$APPLICATION_JSON").body(requestBody).post(url).then()
        then: "The JSON request should be recorded in the test steps"
            1 * test.firstListener().recordRestQuery(*_) >> { RestQuery query ->
                assert "$query" == "POST $url"
                assert query.method == POST
                assert "${query.path}" == url
                assert query.statusCode == 200
                assert formatted(query.responseBody) == formatted(body)
                assert formatted(query.content) == formatted(requestBody)
            }
        and:
            result.statusCode(200)
    }

    def "Should record RestAssured post() method calls with parameters"() {
        given:
            def JsonObject json = new JsonObject()
            json.addProperty("Exists", true)
            json.addProperty("label", "UI")
            def body = gson.toJson(json)
            json.addProperty("SomeValue","value")
            def requestBody = gson.toJson(json)

            def base = "http://localhost:${wire.port()}"
            def path = "/test/label"
            def url = "$base$path"

            stubFor(WireMock.post(urlPathMatching("$path.*"))
                .withRequestBody(matching(".*"))
                .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "$APPLICATION_JSON")
                .withBody(body)));
        when:
            def result = given().contentType("$APPLICATION_JSON").body(requestBody).post("$url?status={status}", ["status": "available"]).then()
        then: "The JSON request should be recorded in the test steps"
            1 * test.firstListener().recordRestQuery(*_) >> { RestQuery query ->
                assert "$query" == "POST $url?status=available"
                assert query.method == POST
                assert query.statusCode == 200
                assert formatted(query.responseBody) == formatted(body)
                assert formatted(query.content) == formatted(requestBody)
            }
        and:
            result.statusCode(200)
    }

    def "Should record RestAssured post() method calls with parameter provided as a list"() {
        given:
            def JsonObject json = new JsonObject()
            json.addProperty("Weather", "rain")
            json.addProperty("temperature", "+2")
            def body = gson.toJson(json)
            json.addProperty("SomeValue","value")
            def requestBody = gson.toJson(json)

            def base = "http://localhost:${wire.port()}"
            def path = "/test/weather"
            def url = "$base$path"

            stubFor(WireMock.post(urlPathMatching("$path.*"))
                .withRequestBody(matching(".*"))
                .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "$APPLICATION_JSON")
                .withBody(body)));
        when:
            def result = given().contentType("$APPLICATION_JSON").body(requestBody).post("$url?status={status}", "available").then()
        then: "The JSON request should be recorded in the test steps"
            1 * test.firstListener().recordRestQuery(*_) >> { RestQuery query ->
                assert "$query" == "POST $url?status=available"
                assert query.method == POST
                assert query.statusCode == 200
                assert formatted(query.responseBody) == formatted(body)
                assert formatted(query.content) == formatted(requestBody)
            }
        and:
            result.statusCode(200)
    }

    def "Should record RestAssured post() method calls with cookies"() {
        given:
        def JsonObject json = new JsonObject()
        json.addProperty("Number", "9999")
        json.addProperty("Price", "100")
        def body = gson.toJson(json)
        json.addProperty("SomeValue","value")
        def requestBody = gson.toJson(json)

        def base = "http://localhost:${wire.port()}"
        def path = "/test/number"
        def url = "$base$path"
        def header = "Content-Type: $APPLICATION_JSON"
        def cookie = "__smToken=Nguv2UFVaztFaBguF9YF7yyF"

        stubFor(WireMock.post(urlEqualTo(path))
                .withRequestBody(matching(".*"))
                .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "$APPLICATION_JSON")
                .withHeader("Set-Cookie:", cookie)
                .withBody(body)));
        when:
        def result = given().contentType("$APPLICATION_JSON").body(requestBody).post(url).then()
        then: "The JSON request with cookies should be recorded in the test steps"
        1 * test.firstListener().recordRestQuery(*_) >> { RestQuery query ->
            assert "$query" == "POST $url"
            assert query.method == POST
            assert "${query.path}" == url
            assert query.statusCode == 200
            assert query.responseHeaders.contains(header)
            assert query.responseCookies == cookie
            assert formatted(query.responseBody) == formatted(body)
            assert formatted(query.content) == formatted(requestBody)
        }
        and:
        result.statusCode(200)
    }

    def "Should record RestAssured post() method calls with no cookies"() {
        given:
        def JsonObject json = new JsonObject()
        json.addProperty("Number", "9999")
        json.addProperty("Price", "100")
        def body = gson.toJson(json)
        json.addProperty("SomeValue","value")
        def requestBody = gson.toJson(json)

        def base = "http://localhost:${wire.port()}"
        def path = "/test/number"
        def url = "$base$path"
        def header = "Content-Type: $APPLICATION_JSON"

        stubFor(WireMock.post(urlEqualTo(path))
                .withRequestBody(matching(".*"))
                .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "$APPLICATION_JSON")
                .withBody(body)));
        when:
        def result = given().contentType("$APPLICATION_JSON").body(requestBody).post(url).then()
        then: "The JSON request with empty cookies should be recorded in the test steps"
        1 * test.firstListener().recordRestQuery(*_) >> { RestQuery query ->
            assert "$query" == "POST $url"
            assert query.method == POST
            assert "${query.path}" == url
            assert query.statusCode == 200
            assert query.responseHeaders.contains(header)
            assert query.responseCookies == ""
            assert formatted(query.responseBody) == formatted(body)
            assert formatted(query.content) == formatted(requestBody)
        }
        and:
        result.statusCode(200)
    }

    def "Should record RestAssured post() method calls with body for HTML content type"() {
        given:
        def JsonObject json = new JsonObject()
        json.addProperty("Number", "9999")
        json.addProperty("Price", "100")
        def body = gson.toJson(json)
        def requestBody = gson.toJson(json)

        def base = "http://localhost:${wire.port()}"
        def path = "/test/number"
        def url = "$base$path"

        stubFor(WireMock.post(urlEqualTo(path))
                .withRequestBody(matching(".*"))
                .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "$HTML")
                .withBody(requestBody)));
        when:
        def result = given().contentType("$HTML").body(requestBody).post(url).then()
        then: "The JSON request with body should be recorded in the test steps"
        1 * test.firstListener().recordRestQuery(*_) >> { RestQuery query ->
            assert "$query" == "POST $url"
            assert query.method == POST
            assert "${query.path}" == url
            assert query.statusCode == 200
            assert query.responseBody.contains(body)
        }
        and:
        result.statusCode(200)
    }
}
