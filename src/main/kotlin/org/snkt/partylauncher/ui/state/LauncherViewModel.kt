package org.snkt.partylauncher.ui.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.snkt.partylauncher.auth.AccountStorage
import org.snkt.partylauncher.auth.EncryptedFileAccountStorage
import org.snkt.partylauncher.auth.MicrosoftAuthService
import org.snkt.partylauncher.auth.MinecraftSession
import org.snkt.partylauncher.auth.models.DeviceCodeResponse
import org.snkt.partylauncher.build.BuildDownloader
import org.snkt.partylauncher.build.BuildInstaller
import org.snkt.partylauncher.build.BuildManifest
import org.snkt.partylauncher.build.BuildRepository
import org.snkt.partylauncher.config.ConfigStorage
import org.snkt.partylauncher.config.LauncherConfig
import org.snkt.partylauncher.core.AppState
import org.snkt.partylauncher.core.LauncherError
import org.snkt.partylauncher.core.ProgressInfo
import org.snkt.partylauncher.instance.InstanceConfig
import org.snkt.partylauncher.instance.InstanceManager
import org.snkt.partylauncher.logging.AppLogger
import org.snkt.partylauncher.logging.LogBuffer
import org.snkt.partylauncher.logging.LogEntry
import org.snkt.partylauncher.minecraft.MinecraftDownloader
import org.snkt.partylauncher.minecraft.MinecraftLauncher
import org.snkt.partylauncher.minecraft.PlaytimeTracker
import org.snkt.partylauncher.minecraft.ServerListManager
import org.snkt.partylauncher.minecraft.ServerPinger
import org.snkt.partylauncher.minecraft.ServerStatus
import org.snkt.partylauncher.news.model.NewsItem
import org.snkt.partylauncher.news.repository.FirestoreNewsService
import java.awt.Desktop
import java.net.URI

class LauncherViewModel(
    private val configStorage: ConfigStorage = ConfigStorage(),
    private val accountStorage: AccountStorage = EncryptedFileAccountStorage(),
    private val authService: MicrosoftAuthService = MicrosoftAuthService(),
    private val buildRepository: BuildRepository = BuildRepository(),
    private val buildDownloader: BuildDownloader = BuildDownloader(),
    private val buildInstaller: BuildInstaller = BuildInstaller(),
    private val instanceManager: InstanceManager = InstanceManager(),
    private val minecraftDownloader: MinecraftDownloader = MinecraftDownloader(),
    private val minecraftLauncher: MinecraftLauncher = MinecraftLauncher(),
    private val firestoreNewsService: FirestoreNewsService = FirestoreNewsService()
) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _appState = MutableStateFlow(AppState.STARTING)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private val _config = MutableStateFlow(configStorage.loadConfig())
    val config: StateFlow<LauncherConfig> = _config.asStateFlow()

    private val _session = MutableStateFlow<MinecraftSession?>(null)
    val session: StateFlow<MinecraftSession?> = _session.asStateFlow()

    private val _deviceCode = MutableStateFlow<DeviceCodeResponse?>(null)
    val deviceCode: StateFlow<DeviceCodeResponse?> = _deviceCode.asStateFlow()

    private val _deviceCodeRemainingSeconds = MutableStateFlow(0L)
    val deviceCodeRemainingSeconds: StateFlow<Long> = _deviceCodeRemainingSeconds.asStateFlow()

    private val _remoteManifest = MutableStateFlow<BuildManifest?>(null)
    val remoteManifest: StateFlow<BuildManifest?> = _remoteManifest.asStateFlow()

    private val _installedInstanceConfig = MutableStateFlow<InstanceConfig?>(null)
    val installedInstanceConfig: StateFlow<InstanceConfig?> = _installedInstanceConfig.asStateFlow()

    private val _progress = MutableStateFlow(ProgressInfo())
    val progress: StateFlow<ProgressInfo> = _progress.asStateFlow()

    private val _currentError = MutableStateFlow<LauncherError?>(null)
    val currentError: StateFlow<LauncherError?> = _currentError.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _serverStatus = MutableStateFlow<ServerStatus?>(null)
    val serverStatus: StateFlow<ServerStatus?> = _serverStatus.asStateFlow()

    private val _formattedPlaytime = MutableStateFlow("< 1 мин")
    val formattedPlaytime: StateFlow<String> = _formattedPlaytime.asStateFlow()

    private val _newsList = MutableStateFlow<List<NewsItem>>(emptyList())
    val newsList: StateFlow<List<NewsItem>> = _newsList.asStateFlow()

    private val _isNewsLoading = MutableStateFlow(false)
    val isNewsLoading: StateFlow<Boolean> = _isNewsLoading.asStateFlow()

    private var activeJob: Job? = null
    private var serverPingJob: Job? = null
    private var newsJob: Job? = null

    init {
        // Collect logs into state for UI
        scope.launch {
            AppLogger.globalBuffer.logFlow.collect {
                _logs.value = AppLogger.globalBuffer.getAllEntries()
            }
        }

        startServerPingerLoop()
        refreshNews()
        initializeLauncher()
    }

    private fun startServerPingerLoop() {
        serverPingJob?.cancel()
        serverPingJob = scope.launch {
            while (isActive) {
                refreshServerStatus()
                delay(20_000)
            }
        }
    }

    fun refreshServerStatus() {
        scope.launch {
            val addr = _config.value.serverAddress
            if (addr.isNotBlank()) {
                val status = ServerPinger.ping(addr)
                _serverStatus.value = status
            }
        }
    }

    fun refreshNews() {
        newsJob?.cancel()
        newsJob = scope.launch {
            _isNewsLoading.value = true
            val cfg = _config.value
            val result = firestoreNewsService.fetchNews(
                projectId = cfg.firestoreProjectId,
                collection = cfg.firestoreNewsCollection,
                customUrl = cfg.customNewsUrl
            )
            if (result.isSuccess) {
                _newsList.value = result.getOrDefault(emptyList())
            } else {
                AppLogger.warn("LauncherViewModel", "Could not fetch news: ${result.exceptionOrNull()?.message}")
            }
            _isNewsLoading.value = false
        }
    }

    private fun updateFormattedPlaytime() {
        val currentUuid = _session.value?.uuid
        if (!currentUuid.isNullOrBlank()) {
            val totalSec = PlaytimeTracker.getPlaytimeSeconds(_config.value, currentUuid)
            _formattedPlaytime.value = PlaytimeTracker.formatPlaytime(totalSec)
        } else {
            _formattedPlaytime.value = "< 1 мин"
        }
    }

    fun initializeLauncher() {
        scope.launch {
            _appState.value = AppState.CHECKING_AUTH
            AppLogger.info("LauncherViewModel", "Initializing PartyLauncher...")

            _installedInstanceConfig.value = instanceManager.loadInstanceConfig(_config.value.instanceId)

            val savedSession = accountStorage.loadAccount()
            if (savedSession != null) {
                if (savedSession.isOffline) {
                    _session.value = savedSession
                    onUserAuthenticated(savedSession)
                    return@launch
                }

                if (savedSession.isExpired()) {
                    AppLogger.info("LauncherViewModel", "Saved session expired. Attempting token refresh...")
                    val refreshed = if (!savedSession.authManagerJson.isNullOrBlank()) {
                        authService.refreshSession(savedSession.authManagerJson)
                    } else null

                    if (refreshed != null && refreshed.isSuccess) {
                        val valid = refreshed.getOrThrow()
                        _session.value = valid
                        accountStorage.saveAccount(valid)
                        onUserAuthenticated(valid)
                        return@launch
                    }
                    AppLogger.warn("LauncherViewModel", "Token refresh failed. User needs to login again.")
                    accountStorage.removeAccount()
                    _appState.value = AppState.REQUIRES_LOGIN
                } else {
                    _session.value = savedSession
                    onUserAuthenticated(savedSession)
                }
            } else {
                _appState.value = AppState.REQUIRES_LOGIN
            }
        }
    }

    fun startMicrosoftLogin() {
        activeJob?.cancel()
        activeJob = scope.launch {
            _appState.value = AppState.DEVICE_CODE_WAITING
            _currentError.value = null

            val result = authService.loginWithDeviceCode { code ->
                _deviceCode.value = code
                _deviceCodeRemainingSeconds.value = code.expiresIn

                // Automatically open verification URI in user's browser
                try {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(URI(code.verificationUri))
                    }
                } catch (e: Exception) {
                    AppLogger.warn("LauncherViewModel", "Could not open browser automatically: ${e.message}")
                }
            }

            _deviceCode.value = null

            if (result.isSuccess) {
                val session = result.getOrThrow()
                _session.value = session
                accountStorage.saveAccount(session)
                onUserAuthenticated(session)
            } else {
                val err = result.exceptionOrNull()
                if (err is LauncherError) showError(err) else showError(LauncherError.AuthenticationFailed(err?.message ?: "Ошибка входа", cause = err))
                _appState.value = AppState.REQUIRES_LOGIN
            }
        }
    }

    fun cancelLogin() {
        activeJob?.cancel()
        _deviceCode.value = null
        _appState.value = AppState.REQUIRES_LOGIN
    }

    fun loginOffline(username: String) {
        activeJob?.cancel()
        val cleanName = username.trim()
        val nicknameRegex = Regex("^[a-zA-Z0-9_]{3,16}$")
        if (!nicknameRegex.matches(cleanName)) {
            showError(LauncherError.AuthenticationFailed("Никнейм должен быть от 3 до 16 символов (только латиница, цифры и _)"))
            return
        }

        _currentError.value = null
        _deviceCode.value = null
        val session = MinecraftSession.createOffline(cleanName)
        _session.value = session
        accountStorage.saveAccount(session)
        onUserAuthenticated(session)
        AppLogger.info("LauncherViewModel", "Logged in using Offline mode as '$cleanName' (UUID: ${session.formattedUuid})")
    }

    fun logout() {
        scope.launch {
            accountStorage.removeAccount()
            _session.value = null
            _appState.value = AppState.REQUIRES_LOGIN
            AppLogger.info("LauncherViewModel", "User logged out.")
        }
    }

    private fun onUserAuthenticated(session: MinecraftSession) {
        _appState.value = AppState.READY
        updateFormattedPlaytime()
        AppLogger.info("LauncherViewModel", "Logged in as ${session.username}")

        if (_config.value.autoCheckUpdates) {
            checkUpdates()
        }
    }

    fun checkUpdates() {
        activeJob?.cancel()
        activeJob = scope.launch {
            _appState.value = AppState.CHECKING_BUILD
            _progress.value = ProgressInfo(title = "Проверка обновлений сборки...")

            val manifestResult = buildRepository.fetchManifest(_config.value.buildServerUrl)
            if (manifestResult.isFailure) {
                AppLogger.warn("LauncherViewModel", "Could not fetch remote manifest: ${manifestResult.exceptionOrNull()?.message}")
                _appState.value = AppState.READY
                return@launch
            }

            val remote = manifestResult.getOrThrow()
            _remoteManifest.value = remote

            val installed = _installedInstanceConfig.value
            if (installed == null || installed.buildVersion != remote.version) {
                AppLogger.info("LauncherViewModel", "Update available! Remote: ${remote.version}, Installed: ${installed?.buildVersion ?: "none"}")
            } else {
                AppLogger.info("LauncherViewModel", "Build is up to date (${installed.buildVersion})")
            }
            _appState.value = AppState.READY
        }
    }

    fun playOrUpdate() {
        val manifest = _remoteManifest.value
        val installed = _installedInstanceConfig.value

        if (manifest != null && (installed == null || installed.buildVersion != manifest.version)) {
            startUpdateAndLaunch(manifest)
        } else {
            launchGame()
        }
    }

    private fun startUpdateAndLaunch(manifest: BuildManifest) {
        activeJob?.cancel()
        activeJob = scope.launch {
            try {
                // 1. Download Build Zip
                _appState.value = AppState.DOWNLOADING_BUILD
                val downloadUrl = if (manifest.file.startsWith("http://") || manifest.file.startsWith("https://")) {
                    manifest.file
                } else {
                    "${_config.value.buildServerUrl.trimEnd('/')}/${manifest.file}"
                }

                val downloadRes = buildDownloader.downloadBuild(
                    url = downloadUrl,
                    version = manifest.version,
                    expectedSha256 = manifest.sha256,
                    onProgress = { _progress.value = it }
                )
                if (downloadRes.isFailure) {
                    throw downloadRes.exceptionOrNull() ?: Exception("Failed to download build")
                }

                val downloadedZip = downloadRes.getOrThrow()

                // 2. Install Build Zip
                _appState.value = AppState.INSTALLING_BUILD
                _progress.value = ProgressInfo(title = "Установка файлов сборки...")
                val installRes = buildInstaller.installBuild(
                    zipFile = downloadedZip,
                    manifest = manifest,
                    instanceId = _config.value.instanceId,
                    onProgress = { _progress.value = it }
                )
                if (installRes.isFailure) {
                    throw installRes.exceptionOrNull() ?: Exception("Failed to install build")
                }

                _installedInstanceConfig.value = instanceManager.loadInstanceConfig(_config.value.instanceId)

                // 3. Continue to launch game
                launchGame()
            } catch (e: Exception) {
                AppLogger.error("LauncherViewModel", "Update flow failed: ${e.message}", e)
                if (e is LauncherError) showError(e) else showError(LauncherError.Generic(msg = e.message ?: "Update failed", cause = e))
                _appState.value = AppState.READY
            }
        }
    }

    private fun launchGame() {
        val session = _session.value ?: run {
            _appState.value = AppState.REQUIRES_LOGIN
            return
        }

        val manifest = _remoteManifest.value
        val installed = _installedInstanceConfig.value

        val mcVersion = manifest?.minecraft ?: installed?.minecraftVersion ?: "1.21.4"
        val loader = manifest?.loader ?: installed?.loader ?: "fabric"
        val loaderVersion = manifest?.loaderVersion ?: installed?.loaderVersion ?: ""

        activeJob?.cancel()
        activeJob = scope.launch {
            try {
                // Ensure server is configured in instance's servers.dat for Minecraft Multiplayer
                val instanceDir = instanceManager.getInstanceDir(_config.value.instanceId)
                ServerListManager.ensureServerInList(instanceDir.toFile(), _config.value.serverName, _config.value.serverAddress)

                _appState.value = AppState.DOWNLOADING_MINECRAFT
                _progress.value = ProgressInfo(title = "Подготовка Minecraft $mcVersion...")

                val prepRes = minecraftDownloader.prepareMinecraft(
                    minecraftVersion = mcVersion,
                    loader = loader,
                    loaderVersion = loaderVersion,
                    onProgress = { _progress.value = it }
                )

                if (prepRes.isFailure) {
                    throw prepRes.exceptionOrNull() ?: Exception("Failed to prepare Minecraft")
                }

                val installation = prepRes.getOrThrow()

                _appState.value = AppState.LAUNCHING
                _progress.value = ProgressInfo(title = "Запуск процесса Minecraft...")

                val gameStartTime = System.currentTimeMillis()
                var lastSavedTime = gameStartTime

                // Start local playtime background tracking coroutine
                val playtimeJob = scope.launch {
                    while (isActive) {
                        delay(30_000)
                        val now = System.currentTimeMillis()
                        val elapsedSec = (now - lastSavedTime) / 1000
                        if (elapsedSec > 0) {
                            lastSavedTime = now
                            val updatedConfig = PlaytimeTracker.addPlaytime(_config.value, session.uuid, elapsedSec)
                            _config.value = updatedConfig
                            configStorage.saveConfig(updatedConfig)
                            updateFormattedPlaytime()
                        }
                    }
                }

                val launchRes = minecraftLauncher.launch(
                    installation = installation,
                    session = session,
                    instanceId = _config.value.instanceId,
                    config = _config.value,
                    scope = scope,
                    onProcessExit = { exitCode ->
                        playtimeJob.cancel()
                        val now = System.currentTimeMillis()
                        val finalElapsedSec = (now - lastSavedTime) / 1000
                        if (finalElapsedSec > 0) {
                            val updatedConfig = PlaytimeTracker.addPlaytime(_config.value, session.uuid, finalElapsedSec)
                            _config.value = updatedConfig
                            configStorage.saveConfig(updatedConfig)
                            updateFormattedPlaytime()
                        }

                        if (exitCode != 0) {
                            showError(LauncherError.MinecraftLaunchFailed(exitCode))
                        }
                        _appState.value = AppState.READY
                    }
                )

                if (launchRes.isFailure) {
                    playtimeJob.cancel()
                    throw launchRes.exceptionOrNull() ?: Exception("Failed to spawn process")
                }

                _appState.value = AppState.RUNNING
                AppLogger.info("LauncherViewModel", "Game is running.")
            } catch (e: Exception) {
                AppLogger.error("LauncherViewModel", "Launch failed: ${e.message}", e)
                if (e is LauncherError) showError(e) else showError(LauncherError.MinecraftLaunchFailed(null, e.message, e))
                _appState.value = AppState.READY
            }
        }
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun saveSettings(newConfig: LauncherConfig) {
        val serverAddrChanged = _config.value.serverAddress != newConfig.serverAddress
        val newsConfigChanged = _config.value.firestoreProjectId != newConfig.firestoreProjectId ||
                _config.value.firestoreNewsCollection != newConfig.firestoreNewsCollection ||
                _config.value.customNewsUrl != newConfig.customNewsUrl

        _config.value = newConfig
        configStorage.saveConfig(newConfig)
        _isSettingsOpen.value = false
        AppLogger.info("LauncherViewModel", "Updated configuration applied.")

        if (serverAddrChanged) {
            refreshServerStatus()
        }
        if (newsConfigChanged) {
            refreshNews()
        }
    }

    fun clearLogs() {
        AppLogger.globalBuffer.clear()
        _logs.value = emptyList()
    }

    fun dismissError() {
        _currentError.value = null
    }

    private fun showError(error: LauncherError) {
        _currentError.value = error
        AppLogger.error("LauncherViewModel", "Error shown to user: ${error.userMessage}")
    }
}
