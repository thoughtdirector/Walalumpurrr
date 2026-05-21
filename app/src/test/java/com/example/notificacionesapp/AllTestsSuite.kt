package com.example.notificacionesapp

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    SessionManagerTest::class,
    ScheduleManagerTest::class,
    util.AmountSettingsTest::class,
    notification.NotificationProcessorRegistryTest::class,
    notification.processors.NequiNotificationProcessorTest::class,
    notification.processors.DaviPlataNotificationProcessorTest::class,
    core.domain.ResultTest::class,
    core.domain.AuthUserInfoTest::class,
    domain.model.NotificationTest::class,
    domain.model.ScheduleTest::class,
    domain.model.UserTest::class,
    data.repository.UserDtoMappingTest::class,
    data.repository.NotificationDtoMappingTest::class
)
class AllTestsSuite
