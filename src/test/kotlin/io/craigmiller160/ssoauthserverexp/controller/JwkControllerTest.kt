package io.craigmiller160.ssoauthserverexp.controller

import com.nimbusds.jose.jwk.JWKSet
import io.craigmiller160.ssoauthserverexp.config.TokenConfig
import net.minidev.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class JwkControllerTest {

    @Mock
    private lateinit var tokenConfig: TokenConfig
    @Mock
    private lateinit var jwkSet: JWKSet

    @InjectMocks
    private lateinit var jwkController: JwkController

    @Test
    fun test_getKey() {
        val jsonObject = JSONObject()
        jsonObject.put("Hello", "World")
        `when`(tokenConfig.jwkSet())
                .thenReturn(jwkSet)
        `when`(jwkSet.toJSONObject())
                .thenReturn(jsonObject)

        val result = jwkController.getKey()
        assertEquals(jsonObject, result)
    }

}