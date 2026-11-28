package com.konvoiku.touring.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.konvoiku.touring.R
import com.konvoiku.touring.data.FirestoreConvoyRepository
import com.konvoiku.touring.model.AuthProviderType
import com.konvoiku.touring.model.GlobalAppConfig
import com.konvoiku.touring.ui.components.TouringDashboardCanvasBackdrop
import com.konvoiku.touring.ui.components.TouringHeroAtmosphereHeader

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    initialName: String = "",
    initialEmail: String = "",
    pendingFamilyInviteCode: String = "",
    isAuthLoading: Boolean = false,
    authError: String? = null,
    authInfoMessage: String? = null,
    showGoogleBrowserFallback: Boolean = false,
    showOwnerDevPreviewFallback: Boolean = false,
    globalConfig: GlobalAppConfig = GlobalAppConfig(),
    onClearAuthError: () -> Unit = {},
    onGoogleSignIn: (Activity?) -> Unit = {},
    onOpenGoogleBrowserSync: () -> Unit = {},
    onOpenCustomGoogleBrowserSync: (String) -> Unit = {},
    onOpenOwnerDevPreviewSync: () -> Unit = {},
    onSubmitGoogleSyncToken: (String) -> Unit = {},
    onFamilyGoogleSignIn: (Activity?, String) -> Unit = { _, _ -> },
    onFamilyGoogleBrowserSync: (String) -> Unit = {},
    onFamilyCustomGoogleBrowserSync: (String, String) -> Unit = { _, _ -> },
    onFamilyEmailAuth: (
        isSignUp: Boolean,
        fullName: String,
        email: String,
        password: String,
        inviteCodeOrLink: String
    ) -> Unit = { _, _, _, _, _ -> },
    onFacebookSignIn: (Activity?) -> Unit = {},
    onForgotPassword: (String) -> Unit = {},
    onEmailSignIn: (email: String, password: String) -> Unit = { _, _ -> },
    onEmailSignUp: (fullName: String, email: String, password: String) -> Unit = { _, _, _ -> },
    onSocialSignIn: (
        provider: AuthProviderType,
        fullName: String,
        email: String,
        instagramHandle: String
    ) -> Unit = { _, _, _, _ -> },
    onLoginSuccess: (fullName: String, email: String, isGuest: Boolean) -> Unit,
    onOpenAboutApp: () -> Unit
) {
    val context = LocalContext.current
    var isSignUpMode by remember { mutableStateOf(false) }
    var isFamilyMode by remember(pendingFamilyInviteCode) {
        mutableStateOf(pendingFamilyInviteCode.isNotBlank())
    }
    var isFamilySignUp by remember { mutableStateOf(true) }
    var familyInviteInput by remember(pendingFamilyInviteCode) {
        mutableStateOf(pendingFamilyInviteCode)
    }
    var fullName by remember(initialName) { mutableStateOf(initialName) }
    var email by remember(initialEmail) { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var showManualTokenInput by remember(showGoogleBrowserFallback) { mutableStateOf(showGoogleBrowserFallback) }
    var manualSyncToken by remember { mutableStateOf("") }
    var showCustomBridgeInput by remember { mutableStateOf(false) }
    var customBridgeUrl by remember { mutableStateOf("") }

    var pendingSocialProvider by remember { mutableStateOf<AuthProviderType?>(null) }
    var socialNameInput by remember { mutableStateOf("") }
    var socialEmailInput by remember { mutableStateOf("") }
    var socialInstagramHandle by remember { mutableStateOf("") }
    var socialDialogError by remember { mutableStateOf<String?>(null) }

    val combinedError = localError ?: authError
    val activeEmailSignUp = if (isFamilyMode) isFamilySignUp else isSignUpMode

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_safar_brand),
                            contentDescription = "Safar Tracking",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Text(
                            text = "Safar Tracking",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenAboutApp) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "Tentang Aplikasi",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TouringDashboardCanvasBackdrop()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                TouringHeroAtmosphereHeader(
                    title = when {
                        isFamilyMode -> "Pantauan Keluarga Jamaah"
                        isSignUpMode -> "Daftar Akun Safar Tracking"
                        else -> "Masuk ke Safar Tracking"
                    },
                    subtitle = if (isFamilyMode) {
                        "Masuk atau daftar khusus menggunakan kode/link undangan dari 1 anggota rombongan (mode pantau baca-saja)."
                    } else {
                        "Masuk atau daftar dengan email, Google, Facebook, atau Instagram untuk menyinkronkan profil jamaah Anda."
                    }
                )

                if (globalConfig.maintenanceMode) {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Build,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Mode Pemeliharaan Aktif",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = globalConfig.maintenanceMessage,
                                    fontSize = 11.sp,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    }
                }

                if (globalConfig.announcementActive && globalConfig.announcementMessage.isNotBlank()) {
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Campaign,
                                contentDescription = null,
                                tint = Color(0xFF047857),
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = globalConfig.announcementTitle.ifBlank { "Info Safar Tracking" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF065F46)
                                )
                                Text(
                                    text = globalConfig.announcementMessage,
                                    fontSize = 11.sp,
                                    color = Color(0xFF065F46)
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isSignUpMode) {
                                            Icons.Rounded.PersonAdd
                                        } else {
                                            Icons.AutoMirrored.Rounded.Login
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        isFamilyMode -> "Pantauan Keluarga (Read-Only)"
                                        isSignUpMode -> "Daftar Akun Baru"
                                        else -> "Masuk Akun Jamaah"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isFamilyMode) {
                                        "Hanya memantau posisi live & checklist kegiatan 1 jamaah pemberi link."
                                    } else {
                                        "Profil jamaah, rombongan & progres manasik tersinkronisasi otomatis via email."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Switcher Masuk / Daftar / Keluarga
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = !isFamilyMode && !isSignUpMode,
                                onClick = {
                                    isFamilyMode = false
                                    isSignUpMode = false
                                    localError = null
                                    onClearAuthError()
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                            ) {
                                Text("Masuk", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
                            }
                            SegmentedButton(
                                selected = !isFamilyMode && isSignUpMode,
                                onClick = {
                                    isFamilyMode = false
                                    isSignUpMode = true
                                    localError = null
                                    onClearAuthError()
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                            ) {
                                Text("Daftar", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
                            }
                            SegmentedButton(
                                selected = isFamilyMode,
                                onClick = {
                                    isFamilyMode = true
                                    localError = null
                                    onClearAuthError()
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                            ) {
                                Text("Keluarga", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
                            }
                        }

                        if (isFamilyMode) {
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Akses Pantauan Keluarga Terkunci",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF065F46)
                                    )
                                    Text(
                                        text = "Pendaftaran & login keluarga hanya dapat dilakukan melalui link atau kode khusus (FAM-XXXXXX) yang dibagikan oleh anggota rombongan.",
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        color = Color(0xFF047857)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = familyInviteInput,
                                onValueChange = {
                                    familyInviteInput = it
                                    localError = null
                                    onClearAuthError()
                                },
                                label = { Text("Kode atau Link Undangan Keluarga") },
                                placeholder = { Text("Tempel link atau kode FAM-XXXXXX") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Link, contentDescription = null)
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = !isFamilySignUp,
                                    onClick = {
                                        isFamilySignUp = false
                                        localError = null
                                        onClearAuthError()
                                    },
                                    label = { Text("Masuk Akun Keluarga", fontSize = 11.sp, maxLines = 1) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = isFamilySignUp,
                                    onClick = {
                                        isFamilySignUp = true
                                        localError = null
                                        onClearAuthError()
                                    },
                                    label = { Text("Daftar Keluarga Baru", fontSize = 11.sp, maxLines = 1) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Opsi Daftar / Masuk Cepat via Google, Facebook, Instagram
                        Text(
                            text = when {
                                isFamilyMode -> "Masuk Cepat Keluarga dengan Google"
                                isSignUpMode -> "Daftar Cepat dengan Media Sosial"
                                else -> "Masuk Cepat dengan Media Sosial"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SocialAuthProviderButton(
                                badgeText = "G",
                                badgeColor = Color(0xFFEA4335),
                                label = when {
                                    isFamilyMode -> "Lanjutkan dengan Akun Google (Keluarga)"
                                    isSignUpMode -> "Daftar dengan Akun Google"
                                    else -> "Masuk dengan Akun Google"
                                },
                                borderColor = MaterialTheme.colorScheme.outline,
                                enabled = !isAuthLoading,
                                onClick = {
                                    localError = null
                                    onClearAuthError()
                                    if (isFamilyMode) {
                                        if (familyInviteInput.isBlank()) {
                                            localError = "Masukkan link atau kode undangan keluarga (FAM-XXXXXX) terlebih dahulu."
                                            return@SocialAuthProviderButton
                                        }
                                        onFamilyGoogleSignIn(context.findActivity(), familyInviteInput.trim())
                                    } else {
                                        onGoogleSignIn(context.findActivity())
                                    }
                                }
                            )

                            OutlinedButton(
                                onClick = {
                                    localError = null
                                    onClearAuthError()
                                    if (isFamilyMode) {
                                        if (familyInviteInput.isBlank()) {
                                            localError = "Masukkan link atau kode undangan keluarga (FAM-XXXXXX) terlebih dahulu."
                                            return@OutlinedButton
                                        }
                                        if (customBridgeUrl.isNotBlank()) {
                                            onFamilyCustomGoogleBrowserSync(familyInviteInput.trim(), customBridgeUrl)
                                        } else {
                                            onFamilyGoogleBrowserSync(familyInviteInput.trim())
                                        }
                                    } else {
                                        if (customBridgeUrl.isNotBlank()) {
                                            onOpenCustomGoogleBrowserSync(customBridgeUrl)
                                        } else {
                                            onOpenGoogleBrowserSync()
                                        }
                                    }
                                },
                                enabled = !isAuthLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 46.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.OpenInBrowser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sinkronkan Google via Browser",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Text(
                                text = "Sinkronkan Google via Browser berjalan langsung lewat halaman login Google lokal di browser HP tanpa perlu deploy web.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { showCustomBridgeInput = !showCustomBridgeInput }
                                ) {
                                    Text(
                                        text = if (showCustomBridgeInput) "Tutup URL kustom" else "URL web kustom?",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(
                                    onClick = { showManualTokenInput = !showManualTokenInput }
                                ) {
                                    Text(
                                        text = if (showManualTokenInput) "Tutup tempel token" else "Sudah login di browser?",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (showCustomBridgeInput) {
                                OutlinedTextField(
                                    value = customBridgeUrl,
                                    onValueChange = { customBridgeUrl = it },
                                    label = { Text("URL Web Safar Tracking (opsional)") },
                                    placeholder = { Text("https://...run.app") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            if (showManualTokenInput) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = manualSyncToken,
                                        onValueChange = { manualSyncToken = it },
                                        label = { Text("Tempel link safartracking://auth atau token") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Button(
                                        onClick = {
                                            localError = null
                                            onClearAuthError()
                                            onSubmitGoogleSyncToken(manualSyncToken)
                                        },
                                        enabled = !isAuthLoading && manualSyncToken.isNotBlank(),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Terapkan Sinkronisasi Google", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            if (!isFamilyMode) {
                                SocialAuthProviderButton(
                                    badgeText = "f",
                                    badgeColor = Color(0xFF1877F2),
                                    label = if (isSignUpMode) "Daftar dengan Facebook" else "Masuk dengan Facebook",
                                    borderColor = Color(0xFF1877F2).copy(alpha = 0.45f),
                                    enabled = !isAuthLoading,
                                    onClick = {
                                        localError = null
                                        onClearAuthError()
                                        onFacebookSignIn(context.findActivity())
                                    }
                                )

                                SocialAuthProviderButton(
                                    badgeText = "IG",
                                    badgeColor = Color(0xFFD62976),
                                    label = if (isSignUpMode) "Daftar dengan Instagram" else "Masuk dengan Instagram",
                                    borderColor = Color(0xFFD62976).copy(alpha = 0.45f),
                                    enabled = !isAuthLoading,
                                    onClick = {
                                        socialNameInput = fullName
                                        socialEmailInput = email
                                        socialInstagramHandle = ""
                                        socialDialogError = null
                                        pendingSocialProvider = AuthProviderType.INSTAGRAM
                                    }
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f))
                            Text(
                                text = when {
                                    isFamilyMode && activeEmailSignUp -> "atau daftar keluarga pakai email"
                                    isFamilyMode -> "atau masuk keluarga pakai email"
                                    isSignUpMode -> "atau daftar pakai email"
                                    else -> "atau masuk pakai email"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f))
                        }

                        if (activeEmailSignUp) {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = {
                                    fullName = it
                                    localError = null
                                    onClearAuthError()
                                },
                                label = {
                                    Text(if (isFamilyMode) "Nama Lengkap Keluarga" else "Nama Lengkap Jamaah")
                                },
                                placeholder = { Text("Contoh: Ahmad Fauzi") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Person, contentDescription = null)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                localError = null
                                onClearAuthError()
                            },
                            label = { Text(if (isFamilyMode) "Email Keluarga" else "Email Jamaah") },
                            placeholder = { Text("nama@email.com") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Email, contentDescription = null)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                localError = null
                                onClearAuthError()
                            },
                            label = { Text("Kata Sandi") },
                            placeholder = { Text("Minimal 6 karakter") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Lock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) {
                                            Icons.Rounded.VisibilityOff
                                        } else {
                                            Icons.Rounded.Visibility
                                        },
                                        contentDescription = "Tampilkan sandi"
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = if (activeEmailSignUp) ImeAction.Next else ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        if (activeEmailSignUp) {
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    localError = null
                                    onClearAuthError()
                                },
                                label = { Text("Konfirmasi Kata Sandi") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Lock, contentDescription = null)
                                },
                                visualTransformation = if (showPassword) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }

                        if (!activeEmailSignUp) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                TextButton(
                                    onClick = {
                                        localError = null
                                        onClearAuthError()
                                        onForgotPassword(email.trim())
                                    },
                                    enabled = !isAuthLoading
                                ) {
                                    Text(
                                        text = "Lupa Kata Sandi?",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (!authInfoMessage.isNullOrBlank()) {
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF047857),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = authInfoMessage,
                                        color = Color(0xFF065F46),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        if (!combinedError.isNullOrBlank()) {
                            Surface(
                                color = Color(0xFFFEF2F2),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = combinedError,
                                    color = Color(0xFFB91C1C),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }

                        if (showOwnerDevPreviewFallback) {
                            OutlinedButton(
                                onClick = {
                                    localError = null
                                    onClearAuthError()
                                    onOpenOwnerDevPreviewSync()
                                },
                                enabled = !isAuthLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 46.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFD97706))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.OpenInBrowser,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Buka Dev Preview (Khusus Pemilik)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFB45309),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val cleanEmail = email.trim()
                                val cleanName = fullName.trim()
                                if (isFamilyMode && familyInviteInput.trim().isBlank()) {
                                    localError = "Masukkan link atau kode undangan keluarga (FAM-XXXXXX) dari anggota rombongan."
                                    return@Button
                                }
                                if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                                    localError = "Masukkan alamat email yang valid."
                                    return@Button
                                }
                                if (password.length < 6) {
                                    localError = "Kata sandi minimal 6 karakter."
                                    return@Button
                                }
                                if (activeEmailSignUp) {
                                    if (cleanName.length < 2) {
                                        localError = if (isFamilyMode) {
                                            "Masukkan nama lengkap keluarga (minimal 2 karakter)."
                                        } else {
                                            "Masukkan nama lengkap jamaah (minimal 2 karakter)."
                                        }
                                        return@Button
                                    }
                                    if (password != confirmPassword) {
                                        localError = "Konfirmasi kata sandi tidak cocok."
                                        return@Button
                                    }
                                }
                                localError = null
                                if (isFamilyMode) {
                                    onFamilyEmailAuth(
                                        isFamilySignUp,
                                        cleanName,
                                        cleanEmail,
                                        password,
                                        familyInviteInput.trim()
                                    )
                                } else if (isSignUpMode) {
                                    onEmailSignUp(cleanName, cleanEmail, password)
                                } else {
                                    onEmailSignIn(cleanEmail, password)
                                }
                            },
                            enabled = !isAuthLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = Color.White
                            )
                        ) {
                            if (isAuthLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Memproses...", fontWeight = FontWeight.SemiBold)
                            } else {
                                Text(
                                    text = when {
                                        isFamilyMode && isFamilySignUp -> "Daftar & Pantau Jamaah"
                                        isFamilyMode -> "Masuk & Pantau Jamaah"
                                        isSignUpMode -> "Daftar & Sinkronkan Profil"
                                        else -> "Masuk dengan Email"
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (!isFamilyMode) {
                            if (globalConfig.allowGuestMode) {
                                OutlinedButton(
                                    onClick = {
                                        val guestName = fullName.trim().ifBlank { "Jamaah Tamu" }
                                        val guestEmail = email.trim().ifBlank { "tamu@safartracking.id" }
                                        onLoginSuccess(guestName, guestEmail, true)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 46.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Text(
                                        text = "Masuk Cepat sebagai Tamu (Mode Lokal)",
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Text(
                                    text = "Mode Tamu dinonaktifkan sementara oleh Admin Backend.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Ringkasan Fitur Utama
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Map,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Pantau posisi seluruh jamaah & rute multi-tujuan secara live di peta.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Sinkronisasi profil jamaah, kloter & checklist manasik berdasarkan email.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = "Created by Kang Ekie",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
    }

    pendingSocialProvider?.let { provider ->
        AlertDialog(
            onDismissRequest = { pendingSocialProvider = null },
            title = {
                Text(
                    text = "${if (isSignUpMode) "Daftar" else "Masuk"} via ${provider.label}",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (provider == AuthProviderType.INSTAGRAM) {
                            "Masukkan username Instagram dan email agar profil safar Anda tersinkronisasi."
                        } else {
                            "Konfirmasi nama dan email akun ${provider.label} Anda untuk menyinkronkan profil Safar Tracking."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (provider == AuthProviderType.INSTAGRAM) {
                        OutlinedTextField(
                            value = socialInstagramHandle,
                            onValueChange = {
                                socialInstagramHandle = it
                                socialDialogError = null
                            },
                            label = { Text("Username Instagram") },
                            placeholder = { Text("contoh: safar.rombongan") },
                            leadingIcon = { Icon(Icons.Rounded.AlternateEmail, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = socialNameInput,
                        onValueChange = {
                            socialNameInput = it
                            socialDialogError = null
                        },
                        label = { Text("Nama Lengkap Jamaah") },
                        placeholder = { Text("Contoh: Ahmad Fauzi") },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = socialEmailInput,
                        onValueChange = {
                            socialEmailInput = it
                            socialDialogError = null
                        },
                        label = { Text("Email Sinkronisasi (${provider.label})") },
                        placeholder = { Text("nama@email.com") },
                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!socialDialogError.isNullOrBlank()) {
                        Text(
                            text = socialDialogError.orEmpty(),
                            color = Color(0xFFB91C1C),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanEmail = socialEmailInput.trim()
                        val cleanHandle = socialInstagramHandle.trim().removePrefix("@")
                        if (provider != AuthProviderType.INSTAGRAM && (!cleanEmail.contains("@") || !cleanEmail.contains("."))) {
                            socialDialogError = "Masukkan email ${provider.label} yang valid untuk sinkronisasi."
                            return@Button
                        }
                        if (provider == AuthProviderType.INSTAGRAM && cleanHandle.isBlank() && !cleanEmail.contains("@")) {
                            socialDialogError = "Isi username Instagram atau email Anda."
                            return@Button
                        }
                        val chosenProvider = provider
                        pendingSocialProvider = null
                        onSocialSignIn(chosenProvider, socialNameInput.trim(), cleanEmail, cleanHandle)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Lanjutkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSocialProvider = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun SocialAuthProviderButton(
    badgeText: String,
    badgeColor: Color,
    label: String,
    borderColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = badgeColor,
                shape = CircleShape,
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = if (badgeText.length > 1) 10.sp else 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
