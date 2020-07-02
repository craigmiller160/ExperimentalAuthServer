package io.craigmiller160.authserver.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.craigmiller160.authserver.config.TokenConfig
import io.craigmiller160.authserver.dto.RefreshTokenData
import io.craigmiller160.authserver.entity.RefreshToken
import io.craigmiller160.authserver.entity.Role
import io.craigmiller160.authserver.entity.User
import io.craigmiller160.authserver.exception.InvalidRefreshTokenException
import io.craigmiller160.authserver.util.LegacyDateConverter
import org.springframework.stereotype.Component
import java.security.interfaces.RSAPublicKey
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID

@Component
class JwtHandler(
        private val tokenConfig: TokenConfig,
        private val legacyDateConverter: LegacyDateConverter
) {

    private fun generateExp(expSecs: Int): Date {
        val now = LocalDateTime.now()
        val exp = now.plusSeconds(expSecs.toLong())
        return legacyDateConverter.convertLocalDateTimeToDate(exp)
    }

    fun createAccessToken(clientUserDetails: ClientUserDetails, user: User? = null, roles: List<Role> = listOf()): Pair<String,String> {
        val roleNames = roles.map { it.name }

        var claimBuilder = createDefaultClaims(clientUserDetails.client.accessTokenTimeoutSecs)
                .claim("clientKey", clientUserDetails.username)
                .claim("clientName", clientUserDetails.client.name)
                .claim("roles", roleNames)

        claimBuilder = user?.let {
            claimBuilder.subject(user.email)
                    .claim("userEmail", user.email)
        } ?: claimBuilder.subject(clientUserDetails.client.name)

        val claims = claimBuilder.build()
        val token = createToken(claims)
        return Pair(token, claims.jwtid)
    }

    private fun createDefaultClaims(expSecs: Int): JWTClaimsSet.Builder {
        return JWTClaimsSet.Builder()
                .issueTime(Date())
                .expirationTime(generateExp(expSecs))
                .jwtID(UUID.randomUUID().toString())
                .notBeforeTime(Date())
    }

    private fun createToken(claims: JWTClaimsSet): String {
        val header = JWSHeader.Builder(JWSAlgorithm.RS256)
                .build()
        val jwt = SignedJWT(header, claims)
        val signer = RSASSASigner(tokenConfig.privateKey)

        jwt.sign(signer)
        return jwt.serialize()
    }

    fun createRefreshToken(clientUserDetails: ClientUserDetails, grantType: String, userId: Long = 0, tokenId: String): Pair<String,String> {
        var claimBuilder = createDefaultClaims(clientUserDetails.client.refreshTokenTimeoutSecs)
                .claim("grantType", grantType)
                .claim("clientId", clientUserDetails.client.id)
                .jwtID(tokenId)

        if (userId > 0) {
            claimBuilder = claimBuilder.claim("userId", userId)
        }

        val claims = claimBuilder.build()

        val token = createToken(claims)
        return Pair(token, claims.jwtid)
    }

    fun parseRefreshToken(refreshToken: String, clientId: Long): RefreshTokenData {
        val jwt = SignedJWT.parse(refreshToken)
        val verifier = RSASSAVerifier(tokenConfig.publicKey as RSAPublicKey)
        if (!jwt.verify(verifier)) {
            throw InvalidRefreshTokenException("Bad Signature")
        }

        val claims = jwt.jwtClaimsSet
        val now = LocalDateTime.now()
        val exp = legacyDateConverter.convertDateToLocalDateTime(claims.expirationTime)
        if(exp < now) {
            throw InvalidRefreshTokenException("Expired")
        }

        val refreshClientId = claims.getLongClaim("clientId")
        if (refreshClientId != clientId) {
            throw InvalidRefreshTokenException("Invalid Client ID")
        }

        val userId = claims.getLongClaim("userId")
        val grantType = claims.getStringClaim("grantType")
        val tokenId = claims.jwtid

        return RefreshTokenData(tokenId, grantType, refreshClientId, userId)
    }

}
