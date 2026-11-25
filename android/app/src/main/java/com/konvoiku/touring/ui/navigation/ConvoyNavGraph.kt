package com.konvoiku.touring.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.konvoiku.touring.R
import com.konvoiku.touring.model.UserRole
import com.konvoiku.touring.ui.screens.AboutAppScreen
import com.konvoiku.touring.ui.screens.AdminBackendScreen
import com.konvoiku.touring.ui.screens.CustomizeProfileScreen
import com.konvoiku.touring.ui.screens.JoinTourScreen
import com.konvoiku.touring.ui.screens.LiveTrackingMapScreen
import com.konvoiku.touring.ui.screens.LoginScreen
import com.konvoiku.touring.ui.screens.RoleSelectionScreen
import com.konvoiku.touring.ui.screens.TourLeaderScreen
import com.konvoiku.touring.ui.screens.TripDetailScreen
import com.konvoiku.touring.ui.screens.TripHistoryScreen
import com.konvoiku.touring.ui.screens.VehicleProfileScreen
import com.konvoiku.touring.viewmodel.ConvoyViewModel

sealed class ConvoyRoute(val route: String) {
    data object Login : ConvoyRoute("login")
    data object RoleSelection : ConvoyRoute("role_selection")
    data object CustomizeProfile : ConvoyRoute("customize_profile")
    data object AdminBackend : ConvoyRoute("admin_backend")
    data object VehicleProfile : ConvoyRoute("vehicle_profile")
    data object TourLeader : ConvoyRoute("tour_leader")
    data object JoinTour : ConvoyRoute("join_tour")
    data object LiveMap : ConvoyRoute("live_map")
    data object TripHistory : ConvoyRoute("trip_history")
    data object TripDetail : ConvoyRoute("trip_detail")
    data object AboutApp : ConvoyRoute("about_app")
}

@Composable
fun ConvoyNavGraph(
    navController: NavHostController = rememberNavController(),
    viewModel: ConvoyViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val appContext = LocalContext.current
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentNavRoute = currentBackStackEntry?.destination?.route

    LaunchedEffect(uiState.pendingAutoFinishTripId, currentNavRoute) {
        val pendingTripId = uiState.pendingAutoFinishTripId ?: return@LaunchedEffect
        viewModel.consumePendingAutoFinishNavigation()
        if (currentNavRoute == ConvoyRoute.LiveMap.route && pendingTripId.isNotBlank()) {
            navController.navigate(ConvoyRoute.TripDetail.route) {
                popUpTo(ConvoyRoute.RoleSelection.route)
            }
        }
    }

    LaunchedEffect(uiState.isLoggedIn, uiState.pendingInviteRoomId) {
        if (uiState.isLoggedIn && uiState.pendingInviteRoomId != null) {
            viewModel.consumePendingInviteNavigation()
            navController.navigate(ConvoyRoute.JoinTour.route) {
                launchSingleTop = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.app_convoy_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isSystemInDarkTheme()) {
                        Color(0xFF0A0F16).copy(alpha = 0.80f)
                    } else {
                        Color(0xFFFFFDF8).copy(alpha = 0.42f)
                    }
                )
        )

        NavHost(
            navController = navController,
            startDestination = if (uiState.isLoggedIn) ConvoyRoute.RoleSelection.route else ConvoyRoute.Login.route,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(260)
            ) + fadeIn(animationSpec = tween(220))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(260)
            ) + fadeOut(animationSpec = tween(180))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(260)
            ) + fadeIn(animationSpec = tween(220))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(260)
            ) + fadeOut(animationSpec = tween(180))
        }
    ) {
        composable(ConvoyRoute.Login.route) {
            LaunchedEffect(uiState.isLoggedIn) {
                if (uiState.isLoggedIn) {
                    navController.navigate(ConvoyRoute.RoleSelection.route) {
                        popUpTo(ConvoyRoute.Login.route) { inclusive = true }
                    }
                }
            }
            LoginScreen(
                initialName = uiState.authUserName.ifBlank { uiState.vehicleProfile.fullName },
                initialEmail = uiState.authUserEmail,
                isAuthLoading = uiState.isAuthLoading,
                authError = uiState.authError,
                authInfoMessage = uiState.authInfoMessage,
                showGoogleBrowserFallback = uiState.showGoogleBrowserFallback,
                showOwnerDevPreviewFallback = uiState.showOwnerDevPreviewFallback,
                globalConfig = uiState.globalConfig,
                onClearAuthError = viewModel::clearAuthError,
                onGoogleSignIn = { activity ->
                    viewModel.signInWithGoogle(activity) {
                        navController.navigate(ConvoyRoute.RoleSelection.route) {
                            popUpTo(ConvoyRoute.Login.route) { inclusive = true }
                        }
                    }
                },
                onOpenGoogleBrowserSync = {
                    viewModel.openGoogleBrowserSync(appContext)
                },
                onOpenCustomGoogleBrowserSync = { customUrl ->
                    viewModel.openGoogleBrowserSync(appContext, customUrl)
                },
                onOpenOwnerDevPreviewSync = {
                    viewModel.openGoogleBrowserSync(appContext, allowDevProxyFallback = true)
                },
                onSubmitGoogleSyncToken = { raw ->
                    viewModel.importGoogleSyncInput(raw) {
                        navController.navigate(ConvoyRoute.RoleSelection.route) {
                            popUpTo(ConvoyRoute.Login.route) { inclusive = true }
                        }
                    }
                },
                onFacebookSignIn = { activity ->
                    viewModel.signInWithFacebook(activity) {
                        navController.navigate(ConvoyRoute.RoleSelection.route) {
                            popUpTo(ConvoyRoute.Login.route) { inclusive = true }
                        }
                    }
                },
                onForgotPassword = viewModel::sendPasswordReset,
                onEmailSignIn = { email, password ->
                    viewModel.signInWithEmail(email, password) {
                        navController.navigate(ConvoyRoute.RoleSelection.route) {
                            popUpTo(ConvoyRoute.Login.route) { inclusive = true }
                        }
                    }
                },
                onEmailSignUp = { name, email, password ->
                    viewModel.signUpWithEmail(name, email, password) {
                        navController.navigate(ConvoyRoute.RoleSelection.route) {
                            popUpTo(ConvoyRoute.Login.route) { inclusive = true }
                        }
                    }
                },
                onSocialSignIn = { provider, name, email, handle ->
                    viewModel.loginWithSocialProvider(provider, name, email, handle) {
                        navController.navigate(ConvoyRoute.RoleSelection.route) {
                            popUpTo(ConvoyRoute.Login.route) { inclusive = true }
                        }
                    }
                },
                onLoginSuccess = { name, email, isGuest ->
                    viewModel.loginUser(name, email, isGuest)
                    navController.navigate(ConvoyRoute.RoleSelection.route) {
                        popUpTo(ConvoyRoute.Login.route) { inclusive = true }
                    }
                },
                onOpenAboutApp = {
                    navController.navigate(ConvoyRoute.AboutApp.route)
                }
            )
        }

        composable(ConvoyRoute.RoleSelection.route) {
            RoleSelectionScreen(
                activeRoom = uiState.activeRoom,
                lastCompletedTrip = uiState.lastCompletedTrip ?: uiState.tripHistory.firstOrNull(),
                destinationName = uiState.routeConfig.destinationName,
                tripHistoryCount = uiState.tripHistory.size,
                latestTripTitle = uiState.tripHistory.firstOrNull()?.tourTitle,
                authUserName = uiState.authUserName,
                authUserEmail = uiState.authUserEmail,
                authProvider = uiState.authProvider,
                userProfile = uiState.vehicleProfile,
                isGuestMode = uiState.isGuestMode,
                globalConfig = uiState.globalConfig,
                isAdminUser = uiState.isVerifiedAdmin || viewModel.isAdminEmailSession(),
                isVerifiedAdmin = uiState.isVerifiedAdmin,
                isCloudSyncConnected = uiState.isCloudSyncConnected,
                onRoleSelected = { role ->
                    viewModel.selectRole(role)
                    navController.navigate(ConvoyRoute.VehicleProfile.route)
                },
                onResumeCurrentConvoy = {
                    if (uiState.activeRoom != null) {
                        navController.navigate(ConvoyRoute.LiveMap.route)
                    }
                },
                onRepeatTour = { trip ->
                    viewModel.repeatCompletedTrip(trip) {
                        navController.navigate(ConvoyRoute.LiveMap.route)
                    }
                },
                onOpenCustomizeProfile = {
                    viewModel.clearProfileSyncMessage()
                    navController.navigate(ConvoyRoute.CustomizeProfile.route)
                },
                onOpenAdminBackend = {
                    viewModel.clearGlobalConfigFeedback()
                    navController.navigate(ConvoyRoute.AdminBackend.route)
                },
                onOpenTripHistory = {
                    navController.navigate(ConvoyRoute.TripHistory.route)
                },
                onOpenAboutApp = {
                    navController.navigate(ConvoyRoute.AboutApp.route)
                },
                onLogout = {
                    viewModel.logoutUser()
                    navController.navigate(ConvoyRoute.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(ConvoyRoute.AdminBackend.route) {
            AdminBackendScreen(
                isVerifiedAdmin = uiState.isVerifiedAdmin,
                authUserEmail = uiState.authUserEmail,
                isCloudConnected = uiState.isCloudSyncConnected,
                isAuthLoading = uiState.isAuthLoading,
                authError = uiState.authError,
                authInfoMessage = uiState.authInfoMessage,
                showOwnerDevPreviewFallback = uiState.showOwnerDevPreviewFallback,
                isMonitoringLoading = uiState.isAdminMonitoringLoading,
                globalConfig = uiState.globalConfig,
                presenceList = uiState.adminPresenceList,
                rooms = uiState.adminRooms,
                users = uiState.adminUsers,
                monitorError = uiState.adminMonitorError,
                isSavingConfig = uiState.isSavingGlobalConfig,
                configFeedback = uiState.globalConfigFeedback,
                onGoogleSignIn = { activity -> viewModel.signInWithGoogle(activity) },
                onOpenGoogleBrowserSync = { viewModel.openGoogleBrowserSync(appContext) },
                onOpenCustomGoogleBrowserSync = { customUrl ->
                    viewModel.openGoogleBrowserSync(appContext, customUrl)
                },
                onOpenOwnerDevPreviewSync = {
                    viewModel.openGoogleBrowserSync(appContext, allowDevProxyFallback = true)
                },
                onSubmitGoogleSyncToken = { raw -> viewModel.importGoogleSyncInput(raw) },
                onStartMonitoring = viewModel::startAdminMonitoring,
                onStopMonitoring = viewModel::stopAdminMonitoring,
                onSaveGlobalConfig = viewModel::saveGlobalConfig,
                onToggleRoomStatus = viewModel::adminToggleRoomStatus,
                onResolveRoomSos = viewModel::adminResolveSos,
                onRemoveRoomMember = viewModel::adminRemoveRoomMember,
                onDeleteRoom = viewModel::adminDeleteRoom,
                onBack = { navController.popBackStack() }
            )
        }

        composable(ConvoyRoute.CustomizeProfile.route) {
            CustomizeProfileScreen(
                profile = uiState.vehicleProfile,
                authProvider = uiState.authProvider,
                isSavingProfile = uiState.isSavingProfile,
                syncStatusMessage = uiState.profileSyncMessage,
                onSaveAndSyncProfile = viewModel::saveAndSyncCustomizedProfile,
                onBack = { navController.popBackStack() }
            )
        }

        composable(ConvoyRoute.VehicleProfile.route) {
            val activeRole = uiState.selectedRole ?: UserRole.MEMBER
            VehicleProfileScreen(
                selectedRole = activeRole,
                profile = uiState.vehicleProfile,
                onUpdateProfile = { name, type, detail, plate, contact, preset, photoUri, clearPhoto ->
                    viewModel.updateVehicleProfile(
                        fullName = name,
                        vehicleType = type,
                        vehicleDetail = detail,
                        licensePlate = plate,
                        emergencyContact = contact,
                        avatarPreset = preset,
                        avatarPhotoUri = photoUri,
                        clearPhoto = clearPhoto
                    )
                },
                onBack = { navController.popBackStack() },
                onContinue = {
                    val destination = if (activeRole == UserRole.TOUR_LEADER) {
                        ConvoyRoute.TourLeader.route
                    } else {
                        ConvoyRoute.JoinTour.route
                    }
                    navController.navigate(destination)
                },
                onOpenAboutApp = {
                    navController.navigate(ConvoyRoute.AboutApp.route)
                }
            )
        }

        composable(ConvoyRoute.TourLeader.route) {
            TourLeaderScreen(
                activeRoom = uiState.activeRoom,
                routeConfig = uiState.routeConfig,
                destinationPresets = viewModel.destinationPresets,
                vehicleType = uiState.vehicleProfile.vehicleType,
                userGpsLat = uiState.userGpsLat,
                userGpsLng = uiState.userGpsLng,
                isCreatingRoom = uiState.isCreatingRoom,
                roomSetupError = uiState.roomSetupError,
                activeWebBaseUrl = uiState.activeWebBaseUrl,
                onPickOriginLatLng = { lat, lng, placeName, addressLabel ->
                    viewModel.setCustomOriginLatLng(lat, lng, placeName, addressLabel)
                },
                onUseGpsAsOrigin = viewModel::useCurrentGpsAsOrigin,
                onSelectPreset = viewModel::selectDestinationPreset,
                onPickDestinationLatLng = { lat, lng, placeName, addressLabel ->
                    viewModel.setCustomDestinationLatLng(lat, lng, placeName, addressLabel)
                },
                onSelectRoutePreference = viewModel::selectRoutePreference,
                onRouteComputed = viewModel::updateComputedTomTomRoute,
                onUpdateWaypoints = viewModel::updateCustomWaypoints,
                onResetDefaultWaypoints = viewModel::resetDefaultWaypoints,
                onAddCheckpointAtPoint = viewModel::addRestCheckpointAtPoint,
                onRemoveCheckpoint = viewModel::removeRestCheckpoint,
                onUpdateMaxStragglerDistance = viewModel::updateMaxStragglerDistance,
                onToggleSoundAlert = viewModel::toggleSoundAlert,
                onCreateRoom = { title ->
                    viewModel.createNewTourRoom(title) {
                        navController.navigate(ConvoyRoute.LiveMap.route)
                    }
                },
                onCreateLocalRoom = { title ->
                    viewModel.createLocalTourRoom(title) {
                        navController.navigate(ConvoyRoute.LiveMap.route)
                    }
                },
                onSimulateJoin = { viewModel.simulateMemberJoin() },
                onRemoveMember = viewModel::removeMember,
                onEnterMapPlaceholder = {
                    navController.navigate(ConvoyRoute.LiveMap.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(ConvoyRoute.JoinTour.route) {
            JoinTourScreen(
                joinInput = uiState.joinInput,
                joinError = uiState.joinError,
                extractedRoomId = viewModel.extractRoomId(uiState.joinInput),
                vehicleProfile = uiState.vehicleProfile,
                isJoining = uiState.isJoiningRoom,
                onInputChange = viewModel::updateJoinInput,
                onJoinSubmit = {
                    viewModel.joinConvoyRoom {
                        navController.navigate(ConvoyRoute.LiveMap.route)
                    }
                },
                onJoinDemoOffline = {
                    viewModel.joinOfflineDemoRoom()
                    navController.navigate(ConvoyRoute.LiveMap.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(ConvoyRoute.LiveMap.route) {
            val isEffectiveLeader = viewModel.isCurrentUserEffectiveLeader()
            LiveTrackingMapScreen(
                activeRoom = uiState.activeRoom,
                role = uiState.selectedRole ?: UserRole.MEMBER,
                selectedMemberId = uiState.selectedMemberId,
                isSimulationRunning = uiState.isSimulationRunning,
                routeConfig = uiState.routeConfig,
                stragglerAlert = uiState.stragglerAlert,
                offRouteAlert = uiState.offRouteAlert,
                sosState = uiState.sosState,
                messages = uiState.messages,
                isRecordingVoice = uiState.isRecordingVoice,
                recordingSeconds = uiState.recordingSeconds,
                playingMessageId = uiState.playingMessageId,
                voiceStatusMessage = uiState.voiceStatusMessage,
                isPickingDestinationOnMap = uiState.isPickingDestinationOnMap,
                isPickingCheckpointOnMap = uiState.isPickingCheckpointOnMap,
                pendingCheckpointName = uiState.pendingCheckpointName,
                isOffRouteSimActive = uiState.isOffRouteSimActive,
                isStragglerSimActive = uiState.isStragglerSimActive,
                canPingTomTomRouting = isEffectiveLeader,
                isEffectiveLeader = isEffectiveLeader,
                isLeaderTroubleSimActive = uiState.isLeaderTroubleSimActive,
                gpsStatus = uiState.gpsStatus,
                gpsAccuracyMeters = uiState.gpsAccuracyMeters,
                activeWebBaseUrl = uiState.activeWebBaseUrl,
                destinationPresets = viewModel.destinationPresets,
                onLocationPermissionResult = viewModel::onLocationPermissionResult,
                onRetryGps = viewModel::startGpsTracking,
                onSelectMember = viewModel::selectMapMember,
                onToggleSimulation = viewModel::toggleSimulation,
                onSelectPreset = viewModel::selectDestinationPreset,
                onPickCustomDestinationLatLng = { lat, lng, name, addr ->
                    viewModel.setCustomDestinationLatLng(lat, lng, name, addr)
                },
                onTogglePickMode = viewModel::setPickingDestinationMode,
                onStartPickCheckpoint = viewModel::startPickingCheckpointOnMap,
                onCancelPickCheckpoint = viewModel::cancelPickingCheckpoint,
                onConfirmAddCheckpointLatLng = viewModel::addRestCheckpointAtLatLng,
                onRemoveCheckpoint = viewModel::removeRestCheckpoint,
                onUpdateMaxStragglerDistance = viewModel::updateMaxStragglerDistance,
                onUpdateOffRouteTolerance = viewModel::updateOffRouteTolerance,
                onToggleSoundAlert = viewModel::toggleSoundAlert,
                onToggleOffRouteSim = viewModel::toggleOffRouteSimulation,
                onToggleStragglerSim = viewModel::toggleStragglerSimulation,
                onToggleLeaderTroubleSim = viewModel::toggleLeaderTroubleSimulation,
                onReclaimLeader = viewModel::reclaimLeaderAuthority,
                onTriggerSos = viewModel::triggerSosEmergency,
                onResolveSos = viewModel::resolveSosEmergency,
                onSendTextMessage = viewModel::sendTextMessage,
                onStartVoiceRecord = viewModel::startVoiceRecording,
                onStopVoiceRecord = { viewModel.stopAndSendVoiceRecording() },
                onToggleVoicePlay = viewModel::toggleVoicePlayback,
                onUpdateOwnCondition = viewModel::updateOwnConditionStatus,
                onAssignSweeper = viewModel::assignSweeper,
                onFinishAndSaveTrip = {
                    val saved = viewModel.finishAndSaveCurrentTrip()
                    if (saved != null) {
                        navController.navigate(ConvoyRoute.TripDetail.route) {
                            popUpTo(ConvoyRoute.RoleSelection.route)
                        }
                    }
                },
                onExitRoom = {
                    navController.popBackStack(
                        route = ConvoyRoute.RoleSelection.route,
                        inclusive = false
                    )
                },
                onRouteComputed = viewModel::updateComputedTomTomRoute
            )
        }

        composable(ConvoyRoute.TripHistory.route) {
            TripHistoryScreen(
                trips = uiState.tripHistory,
                onSelectTrip = { tripId ->
                    viewModel.selectTripHistory(tripId)
                    navController.navigate(ConvoyRoute.TripDetail.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(ConvoyRoute.TripDetail.route) {
            val selectedTrip = uiState.tripHistory.find { it.id == uiState.selectedTripId }
                ?: uiState.tripHistory.firstOrNull()
            TripDetailScreen(
                trip = selectedTrip,
                currentUserName = uiState.vehicleProfile.fullName.ifBlank { uiState.authUserName },
                currentUserAvatarPreset = uiState.vehicleProfile.avatarPreset,
                currentUserAvatarPhotoUri = uiState.vehicleProfile.avatarPhotoUri,
                autoFinishNotice = uiState.autoFinishedTripNotice,
                onDismissAutoFinishNotice = viewModel::clearAutoFinishNotice,
                onBack = {
                    viewModel.clearAutoFinishNotice()
                    navController.popBackStack()
                }
            )
        }

        composable(ConvoyRoute.AboutApp.route) {
            AboutAppScreen(
                onBack = { navController.popBackStack() }
            )
        }
        }
    }
}
