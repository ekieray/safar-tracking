import loginKt from '../../android/app/src/main/java/com/konvoiku/touring/ui/screens/LoginScreen.kt?raw';
import roleSelectionKt from '../../android/app/src/main/java/com/konvoiku/touring/ui/screens/RoleSelectionScreen.kt?raw';
import customizeProfileKt from '../../android/app/src/main/java/com/konvoiku/touring/ui/screens/CustomizeProfileScreen.kt?raw';
import vehicleProfileKt from '../../android/app/src/main/java/com/konvoiku/touring/ui/screens/VehicleProfileScreen.kt?raw';
import tourLeaderKt from '../../android/app/src/main/java/com/konvoiku/touring/ui/screens/TourLeaderScreen.kt?raw';
import joinTourKt from '../../android/app/src/main/java/com/konvoiku/touring/ui/screens/JoinTourScreen.kt?raw';
import liveTrackingMapKt from '../../android/app/src/main/java/com/konvoiku/touring/ui/screens/LiveTrackingMapScreen.kt?raw';
import tripHistoryKt from '../../android/app/src/main/java/com/konvoiku/touring/ui/screens/TripHistoryScreen.kt?raw';
import tripDetailKt from '../../android/app/src/main/java/com/konvoiku/touring/ui/screens/TripDetailScreen.kt?raw';
import aboutAppKt from '../../android/app/src/main/java/com/konvoiku/touring/ui/screens/AboutAppScreen.kt?raw';
import adminBackendKt from '../../android/app/src/main/java/com/konvoiku/touring/ui/screens/AdminBackendScreen.kt?raw';
import tripSummaryCardRendererKt from '../../android/app/src/main/java/com/konvoiku/touring/ui/components/TripSummaryCardRenderer.kt?raw';
import touringAtmosphereKt from '../../android/app/src/main/java/com/konvoiku/touring/ui/components/TouringAtmosphereBackground.kt?raw';
import navGraphKt from '../../android/app/src/main/java/com/konvoiku/touring/ui/navigation/ConvoyNavGraph.kt?raw';
import convoyAvatarKt from '../../android/app/src/main/java/com/konvoiku/touring/ui/components/ConvoyAvatar.kt?raw';
import tomTomConvoyMapKt from '../../android/app/src/main/java/com/konvoiku/touring/ui/components/TomTomConvoyMap.kt?raw';
import viewModelKt from '../../android/app/src/main/java/com/konvoiku/touring/viewmodel/ConvoyViewModel.kt?raw';
import modelsKt from '../../android/app/src/main/java/com/konvoiku/touring/model/ConvoyModels.kt?raw';
import geoUtilsKt from '../../android/app/src/main/java/com/konvoiku/touring/util/GeoUtils.kt?raw';
import gpsLocationTrackerKt from '../../android/app/src/main/java/com/konvoiku/touring/location/GpsLocationTracker.kt?raw';
import firestoreConvoyRepositoryKt from '../../android/app/src/main/java/com/konvoiku/touring/data/FirestoreConvoyRepository.kt?raw';
import tomTomApiServiceKt from '../../android/app/src/main/java/com/konvoiku/touring/data/TomTomApiService.kt?raw';
import themeKt from '../../android/app/src/main/java/com/konvoiku/touring/ui/theme/Theme.kt?raw';
import mainActivityKt from '../../android/app/src/main/java/com/konvoiku/touring/MainActivity.kt?raw';
import settingsGradleKts from '../../android/settings.gradle.kts?raw';
import rootBuildGradleKts from '../../android/build.gradle.kts?raw';
import libsVersionsToml from '../../android/gradle/libs.versions.toml?raw';
import gradleProperties from '../../android/gradle.properties?raw';
import gradleWrapperProperties from '../../android/gradle/wrapper/gradle-wrapper.properties?raw';
import gradlewScript from '../../android/gradlew?raw';
import gradlewBatScript from '../../android/gradlew.bat?raw';
import appBuildGradleKts from '../../android/app/build.gradle.kts?raw';
import proguardRulesPro from '../../android/app/proguard-rules.pro?raw';
import manifestXml from '../../android/app/src/main/AndroidManifest.xml?raw';
import stringsXml from '../../android/app/src/main/res/values/strings.xml?raw';
import themesXml from '../../android/app/src/main/res/values/themes.xml?raw';
import filePathsXml from '../../android/app/src/main/res/xml/file_paths.xml?raw';
import { ScreenRoute } from '../types/convoy';

export interface KotlinSourceFile {
  id: string;
  name: string;
  path: string;
  category: 'Layar Compose' | 'Navigasi & State' | 'Tema & Konfigurasi';
  linkedRoute?: ScreenRoute;
  code: string;
}

export const KOTLIN_SOURCE_FILES: KotlinSourceFile[] = [
  {
    id: 'login',
    name: 'LoginScreen.kt',
    path: 'app/src/main/java/com/konvoiku/touring/ui/screens/LoginScreen.kt',
    category: 'Layar Compose',
    linkedRoute: 'login',
    code: loginKt,
  },
  {
    id: 'role_selection',
    name: 'RoleSelectionScreen.kt',
    path: 'app/src/main/java/com/konvoiku/touring/ui/screens/RoleSelectionScreen.kt',
    category: 'Layar Compose',
    linkedRoute: 'role_selection',
    code: roleSelectionKt,
  },
  {
    id: 'customize_profile',
    name: 'CustomizeProfileScreen.kt',
    path: 'app/src/main/java/com/konvoiku/touring/ui/screens/CustomizeProfileScreen.kt',
    category: 'Layar Compose',
    linkedRoute: 'customize_profile',
    code: customizeProfileKt,
  },
  {
    id: 'vehicle_profile',
    name: 'VehicleProfileScreen.kt',
    path: 'app/src/main/java/com/konvoiku/touring/ui/screens/VehicleProfileScreen.kt',
    category: 'Layar Compose',
    linkedRoute: 'vehicle_profile',
    code: vehicleProfileKt,
  },
  {
    id: 'tour_leader',
    name: 'TourLeaderScreen.kt',
    path: 'app/src/main/java/com/konvoiku/touring/ui/screens/TourLeaderScreen.kt',
    category: 'Layar Compose',
    linkedRoute: 'tour_leader',
    code: tourLeaderKt,
  },
  {
    id: 'join_tour',
    name: 'JoinTourScreen.kt',
    path: 'app/src/main/java/com/konvoiku/touring/ui/screens/JoinTourScreen.kt',
    category: 'Layar Compose',
    linkedRoute: 'join_tour',
    code: joinTourKt,
  },
  {
    id: 'live_map',
    name: 'LiveTrackingMapScreen.kt',
    path: 'app/src/main/java/com/konvoiku/touring/ui/screens/LiveTrackingMapScreen.kt',
    category: 'Layar Compose',
    linkedRoute: 'live_map',
    code: liveTrackingMapKt,
  },
  {
    id: 'trip_history',
    name: 'TripHistoryScreen.kt',
    path: 'app/src/main/java/com/konvoiku/touring/ui/screens/TripHistoryScreen.kt',
    category: 'Layar Compose',
    linkedRoute: 'trip_history',
    code: tripHistoryKt,
  },
  {
    id: 'trip_detail',
    name: 'TripDetailScreen.kt',
    path: 'app/src/main/java/com/konvoiku/touring/ui/screens/TripDetailScreen.kt',
    category: 'Layar Compose',
    linkedRoute: 'trip_detail',
    code: tripDetailKt,
  },
  {
    id: 'trip_card_renderer',
    name: 'TripSummaryCardRenderer.kt',
    path: 'app/src/main/java/com/konvoiku/touring/ui/components/TripSummaryCardRenderer.kt',
    category: 'Layar Compose',
    linkedRoute: 'trip_detail',
    code: tripSummaryCardRendererKt,
  },
  {
    id: 'about_app',
    name: 'AboutAppScreen.kt',
    path: 'app/src/main/java/com/konvoiku/touring/ui/screens/AboutAppScreen.kt',
    category: 'Layar Compose',
    linkedRoute: 'about_app',
    code: aboutAppKt,
  },
  {
    id: 'admin_backend',
    name: 'AdminBackendScreen.kt',
    path: 'app/src/main/java/com/konvoiku/touring/ui/screens/AdminBackendScreen.kt',
    category: 'Layar Compose',
    linkedRoute: 'admin_backend',
    code: adminBackendKt,
  },
  {
    id: 'convoy_avatar',
    name: 'ConvoyAvatar.kt',
    path: 'app/src/main/java/com/konvoiku/touring/ui/components/ConvoyAvatar.kt',
    category: 'Layar Compose',
    code: convoyAvatarKt,
  },
  {
    id: 'tomtom_convoy_map',
    name: 'TomTomConvoyMap.kt',
    path: 'app/src/main/java/com/konvoiku/touring/ui/components/TomTomConvoyMap.kt',
    category: 'Layar Compose',
    linkedRoute: 'live_map',
    code: tomTomConvoyMapKt,
  },
  {
    id: 'touring_atmosphere',
    name: 'TouringAtmosphereBackground.kt',
    path: 'app/src/main/java/com/konvoiku/touring/ui/components/TouringAtmosphereBackground.kt',
    category: 'Layar Compose',
    linkedRoute: 'role_selection',
    code: touringAtmosphereKt,
  },
  {
    id: 'nav_graph',
    name: 'ConvoyNavGraph.kt',
    path: 'app/src/main/java/com/konvoiku/touring/ui/navigation/ConvoyNavGraph.kt',
    category: 'Navigasi & State',
    code: navGraphKt,
  },
  {
    id: 'viewmodel',
    name: 'ConvoyViewModel.kt',
    path: 'app/src/main/java/com/konvoiku/touring/viewmodel/ConvoyViewModel.kt',
    category: 'Navigasi & State',
    code: viewModelKt,
  },
  {
    id: 'models',
    name: 'ConvoyModels.kt',
    path: 'app/src/main/java/com/konvoiku/touring/model/ConvoyModels.kt',
    category: 'Navigasi & State',
    code: modelsKt,
  },
  {
    id: 'geo_utils',
    name: 'GeoUtils.kt',
    path: 'app/src/main/java/com/konvoiku/touring/util/GeoUtils.kt',
    category: 'Navigasi & State',
    code: geoUtilsKt,
  },
  {
    id: 'gps_tracker',
    name: 'GpsLocationTracker.kt',
    path: 'app/src/main/java/com/konvoiku/touring/location/GpsLocationTracker.kt',
    category: 'Navigasi & State',
    code: gpsLocationTrackerKt,
  },
  {
    id: 'firestore_repo',
    name: 'FirestoreConvoyRepository.kt',
    path: 'app/src/main/java/com/konvoiku/touring/data/FirestoreConvoyRepository.kt',
    category: 'Navigasi & State',
    code: firestoreConvoyRepositoryKt,
  },
  {
    id: 'tomtom_api_service',
    name: 'TomTomApiService.kt',
    path: 'app/src/main/java/com/konvoiku/touring/data/TomTomApiService.kt',
    category: 'Navigasi & State',
    code: tomTomApiServiceKt,
  },
  {
    id: 'theme',
    name: 'Theme.kt',
    path: 'app/src/main/java/com/konvoiku/touring/ui/theme/Theme.kt',
    category: 'Tema & Konfigurasi',
    code: themeKt,
  },
  {
    id: 'main_activity',
    name: 'MainActivity.kt',
    path: 'app/src/main/java/com/konvoiku/touring/MainActivity.kt',
    category: 'Tema & Konfigurasi',
    code: mainActivityKt,
  },
  {
    id: 'settings_gradle',
    name: 'settings.gradle.kts',
    path: 'settings.gradle.kts',
    category: 'Tema & Konfigurasi',
    code: settingsGradleKts,
  },
  {
    id: 'root_build_gradle',
    name: 'build.gradle.kts',
    path: 'build.gradle.kts',
    category: 'Tema & Konfigurasi',
    code: rootBuildGradleKts,
  },
  {
    id: 'libs_versions',
    name: 'libs.versions.toml',
    path: 'gradle/libs.versions.toml',
    category: 'Tema & Konfigurasi',
    code: libsVersionsToml,
  },
  {
    id: 'gradle_properties',
    name: 'gradle.properties',
    path: 'gradle.properties',
    category: 'Tema & Konfigurasi',
    code: gradleProperties,
  },
  {
    id: 'gradle_wrapper',
    name: 'gradle-wrapper.properties',
    path: 'gradle/wrapper/gradle-wrapper.properties',
    category: 'Tema & Konfigurasi',
    code: gradleWrapperProperties,
  },
  {
    id: 'gradlew_sh',
    name: 'gradlew',
    path: 'gradlew',
    category: 'Tema & Konfigurasi',
    code: gradlewScript,
  },
  {
    id: 'gradlew_bat',
    name: 'gradlew.bat',
    path: 'gradlew.bat',
    category: 'Tema & Konfigurasi',
    code: gradlewBatScript,
  },
  {
    id: 'build_gradle',
    name: 'app/build.gradle.kts',
    path: 'app/build.gradle.kts',
    category: 'Tema & Konfigurasi',
    code: appBuildGradleKts,
  },
  {
    id: 'proguard_rules',
    name: 'proguard-rules.pro',
    path: 'app/proguard-rules.pro',
    category: 'Tema & Konfigurasi',
    code: proguardRulesPro,
  },
  {
    id: 'manifest',
    name: 'AndroidManifest.xml',
    path: 'app/src/main/AndroidManifest.xml',
    category: 'Tema & Konfigurasi',
    code: manifestXml,
  },
  {
    id: 'strings_xml',
    name: 'res/values/strings.xml',
    path: 'app/src/main/res/values/strings.xml',
    category: 'Tema & Konfigurasi',
    code: stringsXml,
  },
  {
    id: 'themes_xml',
    name: 'res/values/themes.xml',
    path: 'app/src/main/res/values/themes.xml',
    category: 'Tema & Konfigurasi',
    code: themesXml,
  },
  {
    id: 'file_paths',
    name: 'res/xml/file_paths.xml',
    path: 'app/src/main/res/xml/file_paths.xml',
    category: 'Tema & Konfigurasi',
    code: filePathsXml,
  },
];
