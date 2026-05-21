package com.example.notificacionesapp.core.domain

import org.junit.Test
import org.junit.Assert.*

class ResultTest {

    @Test
    fun `Success wraps data correctly`() {
        val result = Result.Success("hello")
        assertEquals("hello", result.data)
    }

    @Test
    fun `Success with complex data`() {
        val list = listOf(1, 2, 3)
        val result = Result.Success(list)
        assertEquals(3, result.data.size)
    }

    @Test
    fun `Error wraps exception correctly`() {
        val exception = IllegalArgumentException("bad input")
        val result = Result.Error(exception)
        assertEquals("bad input", result.exception.message)
        assertTrue(result.exception is IllegalArgumentException)
    }

    @Test
    fun `Loading is a singleton`() {
        assertSame(Result.Loading, Result.Loading)
    }

    @Test
    fun `Success is pattern-matchable`() {
        val result: Result<String> = Result.Success("data")
        when (result) {
            is Result.Success -> assertEquals("data", result.data)
            is Result.Error -> fail("Should not be Error")
            is Result.Loading -> fail("Should not be Loading")
        }
    }

    @Test
    fun `Error is pattern-matchable`() {
        val result: Result<String> = Result.Error(RuntimeException("fail"))
        when (result) {
            is Result.Success -> fail("Should not be Success")
            is Result.Error -> assertEquals("fail", result.exception.message)
            is Result.Loading -> fail("Should not be Loading")
        }
    }

    @Test
    fun `Loading is pattern-matchable`() {
        val result: Result<String> = Result.Loading
        when (result) {
            is Result.Success -> fail("Should not be Success")
            is Result.Error -> fail("Should not be Error")
            is Result.Loading -> {} // expected
        }
    }

    @Test
    fun `Success with null data`() {
        val result = Result.Success(null)
        assertNull(result.data)
    }

    @Test
    fun `Success with Unit`() {
        val result = Result.Success(Unit)
        assertEquals(Unit, result.data)
    }
}
