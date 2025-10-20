# Unit Test Suite

This directory contains comprehensive unit tests for the NotificacionesApp to support future changes and ensure code quality.

## Test Structure

### Core Tests
- **SessionManagerTest**: Tests user session management functionality
- **ScheduleManagerTest**: Tests scheduled service activation/deactivation
- **MainActivityTest**: Tests main activity functions and utilities

### Utility Tests
- **AmountSettingsTest**: Tests amount filtering and validation

### Notification Tests
- **NotificationProcessorRegistryTest**: Tests notification processor selection
- **NequiNotificationProcessorTest**: Tests Nequi notification processing
- **DaviPlataNotificationProcessorTest**: Tests DaviPlata notification processing

### Integration Tests
- **MainActivityIntegrationTest**: Tests UI interactions and navigation

## Running Tests

### Run All Unit Tests
```bash
./gradlew test
```

### Run Specific Test Class
```bash
./gradlew test --tests "com.example.notificacionesapp.SessionManagerTest"
```

### Run All Tests Suite
```bash
./gradlew test --tests "com.example.notificacionesapp.AllTestsSuite"
```

### Run Android Integration Tests
```bash
./gradlew connectedAndroidTest
```

## Test Coverage

The test suite covers:

### ✅ Session Management
- User login/logout functionality
- Session data storage and retrieval
- User role management

### ✅ Schedule Management
- Schedule configuration saving/loading
- Day-of-week settings
- Time-based service activation

### ✅ Notification Processing
- Notification processor selection
- Amount extraction from notifications
- Text processing and formatting

### ✅ Utility Functions
- Amount filtering and validation
- Settings management
- Data persistence

### ✅ Main Activity Functions
- Password generation
- Employee account creation
- TTS functionality
- Clipboard operations

## Test Dependencies

The test suite uses:
- **JUnit 4**: Core testing framework
- **Mockito**: Mocking framework
- **Robolectric**: Android unit testing
- **Espresso**: UI testing framework
- **AndroidX Test**: Android testing utilities

## Adding New Tests

When adding new functionality:

1. Create corresponding test class in appropriate package
2. Follow naming convention: `[ClassName]Test`
3. Use `@RunWith(MockitoJUnitRunner::class)` for unit tests
4. Use `@RunWith(RobolectricTestRunner::class)` for Android unit tests
5. Use `@RunWith(AndroidJUnit4::class)` for integration tests
6. Add test class to `AllTestsSuite` if it's a unit test

## Test Best Practices

- **Arrange-Act-Assert**: Structure tests clearly
- **Mock External Dependencies**: Use Mockito for external services
- **Test Edge Cases**: Include null, empty, and invalid inputs
- **Descriptive Test Names**: Use clear, descriptive test method names
- **Single Responsibility**: Each test should test one specific behavior
- **Independent Tests**: Tests should not depend on each other

## Future Test Additions

Consider adding tests for:
- Firebase integration
- Fragment lifecycle
- Service lifecycle
- Broadcast receivers
- Database operations
- Network operations
- Error handling scenarios
