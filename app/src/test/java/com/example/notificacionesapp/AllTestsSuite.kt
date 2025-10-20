package com.example.notificacionesapp

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test suite that runs all unit tests
 * Run this class to execute all unit tests at once
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Core functionality tests
    SessionManagerTest::class,
    ScheduleManagerTest::class,
    
    // Utility tests
    util.AmountSettingsTest::class,
    
    // Notification processor tests
    notification.NotificationProcessorRegistryTest::class,
    notification.processors.NequiNotificationProcessorTest::class,
    notification.processors.DaviPlataNotificationProcessorTest::class,
    
    // Main activity tests
    MainActivityTest::class
)
class AllTestsSuite
