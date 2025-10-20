package com.example.notificacionesapp

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.mockito.MockitoAnnotations

/**
 * Test configuration and utilities for unit tests
 */
object TestConfig {
    
    /**
     * Initialize mocks for a test class
     */
    fun initMocks(testInstance: Any) {
        MockitoAnnotations.openMocks(testInstance)
    }
    
    /**
     * Test rule for automatic mock initialization
     */
    class MockitoRule : TestRule {
        override fun apply(base: Statement, description: Description): Statement {
            return object : Statement() {
                override fun evaluate() {
                    // Mockito initialization is handled in individual test classes
                    base.evaluate()
                }
            }
        }
    }
}

/**
 * Test constants for consistent testing
 */
object TestConstants {
    const val TEST_USER_ID = "test_user_123"
    const val TEST_EMAIL = "test@example.com"
    const val TEST_ADMIN_EMAIL = "admin@example.com"
    const val TEST_EMPLOYEE_EMAIL = "employee@example.com"
    const val TEST_PASSWORD = "testPassword123"
    const val TEST_FIRST_NAME = "John"
    const val TEST_LAST_NAME = "Doe"
    const val TEST_PHONE = "1234567890"
    const val TEST_BIRTH_DATE = "1990-01-01"
    const val TEST_ROLE_ADMIN = "admin"
    const val TEST_ROLE_EMPLOYEE = "employee"
    
    // Notification test data
    const val TEST_NOTIFICATION_ID = "test_notification_123"
    const val TEST_PACKAGE_NEQUI = "com.nequi"
    const val TEST_PACKAGE_DAVIPLATA = "com.davivienda.daviplataapp"
    const val TEST_PACKAGE_WHATSAPP = "com.whatsapp"
    const val TEST_NOTIFICATION_TITLE = "Test Notification"
    const val TEST_NOTIFICATION_TEXT = "Test notification text"
    
    // Schedule test data
    const val TEST_START_HOUR = 9
    const val TEST_START_MINUTE = 0
    const val TEST_END_HOUR = 17
    const val TEST_END_MINUTE = 30
    
    // Amount test data
    const val TEST_MIN_AMOUNT = "1000"
    const val TEST_MAX_AMOUNT = "100000"
    const val TEST_AMOUNT_IN_RANGE = "5000"
    const val TEST_AMOUNT_BELOW_MIN = "500"
    const val TEST_AMOUNT_ABOVE_MAX = "150000"
}
