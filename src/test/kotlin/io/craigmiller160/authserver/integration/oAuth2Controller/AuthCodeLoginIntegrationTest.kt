package io.craigmiller160.authserver.integration.oAuth2Controller

import io.craigmiller160.apitestprocessor.body.formOf
import io.craigmiller160.apitestprocessor.config.AuthType
import io.craigmiller160.authserver.integration.AbstractControllerIntegrationTest
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@SpringBootTest
@ExtendWith(SpringExtension::class)
class AuthCodeLoginIntegrationTest : AbstractControllerIntegrationTest() {

    private val state = "STATE"
    private val responseType = "code"
    private val errorUri = "/ui/login.html"

    private fun createLoginForm(clientId: String = validClientKey) = formOf(
            "username" to authUser.email,
            "password" to authUserPassword,
            "clientId" to clientId,
            "redirectUri" to authClient.redirectUri!!,
            "responseType" to responseType,
            "state" to state
    )

    private fun handleUrl(url: String): Pair<String,Map<String,String>> {
        val uri = url.split("?")[0]
        val query = url.split("?")[1]
                .split("&")
                .map { keyValue ->
                    val parts = keyValue.split("=")
                    Pair(parts[0], parts[1])
                }
                .toMap()
        return Pair(uri, query)
    }

    private fun validateSuccessLocation(location: String) {
        val (uri, query) = handleUrl(location)
        assertThat(uri, equalTo(authClient.redirectUri))
        assertThat(query["code"], notNullValue())
        assertThat(query["state"], equalTo(state))
    }

    private fun validateErrorLocation(location: String, form: Map<String,String>) {
        val (uri, query) = handleUrl(location)
        assertThat(uri, equalTo(errorUri))
        assertThat(query["code"], nullValue())
        assertThat(query["state"], equalTo(form["state"]))
        assertThat(query["response_type"], equalTo(form["responseType"]))
        assertThat(query["client_id"], equalTo(form["clientId"]))
        assertThat(query["redirect_uri"], equalTo(URLEncoder.encode(form["redirectUri"], StandardCharsets.UTF_8)))
        assertThat(query["fail"], equalTo("true"))
    }

    @Test
    fun test_authCodeLogin_invalidClientHeader() {
        apiProcessor.call {
            request {
                path = "/oauth/auth"
                method = HttpMethod.POST
                body = createLoginForm()
                overrideAuth {
                    type = AuthType.BASIC
                    userName = "abc"
                    password = "def"
                }
            }
            response {
                status = 401
            }
        }
    }

    @Test
    fun test_authCodeLogin() {
        val result = apiProcessor.call {
            request {
                path = "/oauth/auth"
                method = HttpMethod.POST
                body = createLoginForm()
            }
            response {
                status = 302
            }
        }
        val location = result.response.getHeader("Location")
        assertNotNull(location)
        validateSuccessLocation(location!!)
    }

    @Test
    fun test_authCodeLogin_validateAuthCodeLogin() {
        val noStateForm = createLoginForm()
        noStateForm["state"] = ""
        noStateForm["name"] = "NoStateForm"

        val badResponseTypeForm = createLoginForm()
        badResponseTypeForm["responseType"] = "bar"
        badResponseTypeForm["name"] = "BadResponseTypeForm"

        val badClientForm = createLoginForm()
        badClientForm["clientId"] = "abc"
        badClientForm["name"] = "BadClientForm"

        val badAuthCodeForm = createLoginForm()
        badAuthCodeForm["redirectUri"] = "abc"
        badAuthCodeForm["name"] = "BadAuthCodeForm"

        val badUserForm = createLoginForm()
        badUserForm["username"] = "def"
        badUserForm["name"] = "BadUserForm"

        listOf(noStateForm, badResponseTypeForm, badClientForm, badUserForm, badAuthCodeForm)
                .forEach { form ->
                    println("Testing Form: ${form["name"]}")

                    val result = apiProcessor.call {
                        request {
                            path = "/oauth/auth"
                            method = HttpMethod.POST
                            body = form
                        }
                        response {
                            status = 302
                        }
                    }

                    val location = result.response.getHeader("Location")
                    assertNotNull(location)
                    validateErrorLocation(location!!, form)
                }
    }

    @Test
    fun test_authCodeLogin_badCreds() {
        val form = createLoginForm()
        form["password"] = "abc"

        val result = apiProcessor.call {
            request {
                path = "/oauth/auth"
                method = HttpMethod.POST
                body = form
            }
            response {
                status = 302
            }
        }

        val location = result.response.getHeader("Location")
        assertNotNull(location)
        validateErrorLocation(location!!, form)
    }

    @Test
    fun `authCodeLogin() - auth_code login with disabled client`() {
        val form = createLoginForm(disabledClient.clientKey)

        apiProcessor.call {
            request {
                path = "/oauth/auth"
                method = HttpMethod.POST
                body = form
                overrideAuth {
                    type = AuthType.BASIC
                    userName = disabledClient.clientKey
                    password = validClientSecret
                }
            }
            response {
                status = 401
            }
        }
    }

    @Test
    fun `authCodeLogin() - auth_code login with disabled user`() {
        TODO("Finish this")
    }

}
