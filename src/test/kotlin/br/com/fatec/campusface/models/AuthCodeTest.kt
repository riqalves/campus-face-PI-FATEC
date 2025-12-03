package br.com.fatec.campusface.models

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class AuthCodeTest {

    @Test
    fun `deve instanciar AuthCode com valores padrao`() {
        val authCode = AuthCode()

        // Valida regras padrão da entidade (ex: deve nascer válida)
        assertTrue(authCode.valid, "O código deve ser iniciado como válido por padrão")
        assertEquals("", authCode.id)
    }

    @Test
    fun `deve armazenar dados corretamente`() {
        val now = Instant.now()
        val code = AuthCode(
            id = "123",
            code = "654321",
            userId = "user_01",
            organizationId = "org_01",
            expirationTime = now,
            valid = false
        )

        assertEquals("123", code.id)
        assertEquals("654321", code.code)
        assertEquals("user_01", code.userId)
        assertFalse(code.valid)
    }
}