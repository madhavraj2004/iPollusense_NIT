# iPollusense_NIT

iPollusense_NIT is an Android application that acts as a companion app for the iPollusense device—an air quality monitoring hardware. The app connects to the device via Bluetooth Low Energy (BLE) and provides real-time monitoring, visualization, and analysis of several key environmental parameters. It is designed to help users track air quality data, receive alerts, and export historical records for further analysis or reporting.

## Features

- **Real-time Sensor Monitoring**
  - Temperature, Humidity, Pressure
  - Particulate Matter (PM1, PM2.5, PM10)
  - Carbon Monoxide (CO)
  - Volatile Organic Compounds (VOC)
  - Carbon Dioxide (CO2)
- **Live and Historical Data Visualization**
  - Dynamic, live-updating graphs
  - Historical data view with customizable data limits
  - Average value and trend calculation
- **Smart Analytics**
  - Air Quality Index (AQI) calculations for dust, CO, VOC, and CO2
  - Predictive analytics for air quality trends
  - Status monitoring and critical alerts (with push notifications)
- **Device Connectivity**
  - BLE device scanning and management
  - Multi-device support with user-assigned nicknames
  - MQTT integration for remote data upload and device control
- **Data Management**
  - CSV export of sensor logs
  - Local buffer for stable graph rendering
  - Persistent device and user settings

## Repository Structure

```
iPollusense_NIT/
├── .gitignore
├── .idea/                  # IDE configuration files
├── app/                    # Main application source and resources
│   ├── src/
│   │   └── main/
│   │       ├── java/       # Java source files
│   │       └── res/        # Android resource files
│   ├── build.gradle        # App-level build configuration
│   ├── google-services.json # Firebase configuration (not tracked in repo)
│   └── proguard-rules.pro  # Code optimization rules
├── gradle/                 # Gradle wrapper files
├── build.gradle            # Project-level build file
├── gradle.properties       # Gradle configuration properties
├── gradlew                 # Gradle wrapper script (Unix)
├── gradlew.bat             # Gradle wrapper script (Windows)
└── settings.gradle         # Gradle project settings
```

## Technical Requirements

- **Android Studio Arctic Fox (or later)**
- **Minimum SDK:** Android 13 (Tiramisu) for full notification and BLE features
- **Google Play Services** (for Firebase and location)
- **A physical Android device with BLE support** (recommended for full functionality)

## Setup Instructions

1. **Clone the repository:**
   ```bash
   git clone https://github.com/madhavraj2004/iPollusense_NIT.git
   ```

2. **Open the project in Android Studio.**

3. **Configure Google Services:**
   - Place your `google-services.json` file inside `app/` for Firebase features (if used).

4. **Build and run the app:**
   - Use a physical device or compatible emulator.
   - Grant the required permissions at runtime (Bluetooth, Location, Notifications, etc.).

5. **Connect to your iPollusense device:**
   - Scan for devices in the app and pair with your iPollusense hardware.
   - Start monitoring and explore live/historical data.

## Required Permissions

- `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`: For BLE device discovery and connection.
- `ACCESS_FINE_LOCATION`: Required for BLE scanning on Android.
- `POST_NOTIFICATIONS`: For user alerts and sensor warnings (Android 13+).
- `INTERNET`: For MQTT and API communication.
- `READ_EXTERNAL_STORAGE`/`WRITE_EXTERNAL_STORAGE` (if exporting files on older Android versions).

## Contribution

We welcome contributions from the community!

- Fork this repo and create a pull request for any feature, bugfix, or improvement.
- Please follow standard Android development best practices.
- For questions, open an issue in the repo.

## License

[Add your license here, e.g., MIT, Apache 2.0, etc.]

---

**Project Link:** [https://github.com/madhavraj2004/iPollusense_NIT](https://github.com/madhavraj2004/iPollusense_NIT)

*For further details on the iPollusense device or advanced features, please refer to the device manual or contact the maintainer.*
