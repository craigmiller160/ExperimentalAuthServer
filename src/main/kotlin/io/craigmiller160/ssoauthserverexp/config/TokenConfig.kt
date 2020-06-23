package io.craigmiller160.ssoauthserverexp.config

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import java.io.File
import java.io.FileNotFoundException
import java.net.URL
import java.nio.file.Paths
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import javax.annotation.PostConstruct
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

@Configuration
@ConfigurationProperties(prefix = "security.token")
class TokenConfig (
        var accessExpSecs: Int = 0,
        var refreshExpSecs: Int = 0,
        var keyStorePath: String = "",
        var keyStoreType: String = "",
        var keyStorePassword: String = "",
        var keyStoreAlias: String = ""
) {

    lateinit var publicKey: PublicKey
    lateinit var privateKey: PrivateKey
    lateinit var keyPair: KeyPair

    @PostConstruct
    fun loadKeys() {
        val keyStoreFile = evaluatePath()
        if (!keyStoreFile.exists()) {
            throw FileNotFoundException(keyStorePath)
        }

        val keyStore = KeyStore.getInstance(keyStoreType)
        keyStoreFile.inputStream()
                .use { stream -> keyStore.load(stream, keyStorePassword.toCharArray()) }
        privateKey = keyStore.getKey(keyStoreAlias, keyStorePassword.toCharArray()) as PrivateKey
        publicKey = keyStore.getCertificate(keyStoreAlias).publicKey
        keyPair = KeyPair(publicKey, privateKey)
    }

    private fun evaluatePath(): File {
        if (keyStorePath.startsWith("classpath:")) {
            val path = keyStorePath.replace(Regex("^classpath:"), "")
            val url = javaClass.classLoader.getResource(path) ?: throw FileNotFoundException(keyStorePath)
            return Paths.get(url.toURI()).toFile()
        }

        return File(keyStorePath)
    }

    fun jwkSet(): JWKSet {
        val builder = RSAKey.Builder(publicKey as RSAPublicKey)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID("oauth-jwt")
        return JWKSet(builder.build())
    }

}