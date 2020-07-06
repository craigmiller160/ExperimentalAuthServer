package io.craigmiller160.authserver.controller

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Aspect
class ControllerLoggingAdvice {

    private val logger = LoggerFactory.getLogger(ControllerLoggingAdvice::class.java)

    private fun getRequest() = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
    private fun getResponse() = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).response

    private fun buildPath(request: HttpServletRequest): String {
        return "${handleNull(request.contextPath)}${handleNull(request.servletPath)}${handleNull(request.pathInfo)}?${handleNull(request.queryString)}"
    }

    private fun handleNull(text: String?) = text ?: ""

    @Pointcut("execution(public * io.craigmiller160.authserver.controller.*Controller.*(..))")
    fun controllerPublicMethods() { }

    private fun getResponseStatus(result: Any?, joinPoint: JoinPoint): Int {
        if (result is ResponseEntity<*>) {
            return result.statusCode.value()
        }

        val responseArg = joinPoint.args
                .find { arg -> arg is HttpServletResponse } as HttpServletResponse?
        return responseArg?.status ?: 0
    }

    @Before("controllerPublicMethods()")
    fun logRequest(joinPoint: JoinPoint) {
        val request = getRequest()
        val path = buildPath(request)
        val method = request.method
        logger.debug("Request: $method $path = ${joinPoint.signature.name}()")
    }

    @AfterReturning("controllerPublicMethods()", returning = "result")
    fun logResponseAfterReturning(joinPoint: JoinPoint, result: Any?) {
        val request = getRequest()
        val status = getResponseStatus(result, joinPoint)
        val path = buildPath(request)
        val method = request.method
        logger.debug("Response $status: $method $path = ${joinPoint.signature.name}()")
    }

    @AfterThrowing("controllerPublicMethods()", throwing = "throwing")
    fun logResponseAfterThrowing(joinPoint: JoinPoint, throwing: Throwable) {
        // TODO improve this to include error status code
        val request = getRequest()
        val path = buildPath(request)
        val method = request.method
        logger.error("Response Error: $method $path = ${joinPoint.signature.name}()", throwing)
    }

}