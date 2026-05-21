# Unit Test Suite

## Test Structure

### Core Tests
- **SessionManagerTest**: User session management (login, logout, role updates)
- **ScheduleManagerTest**: Schedule configuration, alarm management, night schedules

### Domain Model Tests
- **NotificationTest**: Notification data class defaults and copy behavior
- **ScheduleTest**: Schedule extension functions (isDayEnabled, isCurrentlyActive, getNextScheduledEvent)
- **UserTest**: User role checks (isAdmin, isEmployee)
- **ResultTest**: Sealed Result class pattern matching
- **AuthUserInfoTest**: AuthUserInfo data class

### Data Layer Tests
- **UserDtoMappingTest**: UserDto to domain User mapping, role parsing, defaults
- **NotificationDtoMappingTest**: NotificationDto to/from domain Notification, roundtrip

### Notification Processing Tests
- **NotificationProcessorRegistryTest**: Processor routing, metadata, history saving
- **NequiNotificationProcessorTest**: Nequi transfer parsing (normal, QR, metadata)
- **DaviPlataNotificationProcessorTest**: DaviPlata notification parsing and metadata

### Utility Tests
- **AmountSettingsTest**: Amount threshold, shouldReadAmount logic

## Running Tests

```bash
./gradlew test
./gradlew test --tests "com.example.notificacionesapp.AllTestsSuite"
./gradlew test --tests "com.example.notificacionesapp.SessionManagerTest"
```
