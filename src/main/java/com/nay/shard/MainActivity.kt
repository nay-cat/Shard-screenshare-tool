package com.nay.shard

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.nay.shard.databinding.ActivityMainBinding
import com.nay.shard.filters.VersionFilter
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.Locale
import java.util.jar.JarFile
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var outputTextView: TextView
    private lateinit var outputResult: TextView
    private lateinit var lastVersion: TextView
    private var canScan: Boolean = false
    private var fileList: List<String> = emptyList()
    private var pojavTreeUri: Uri? = null
    private var isCheating: Boolean = false
    private var keyCheating: Boolean = false

    private val openDirectoryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        handleActivityResult(result)
    }

    /*
    Thanks to @NotRequiem for optimising the project in terms of code.
     */

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("WrongViewCast", "MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set JNI text
        binding.sampleText.text = stringFromJNI()

        // Initialize views from the old code
        initializeViews()

        // Set up scan button
        val scanButton: Button = findViewById(R.id.scanButton)
        scanButton.setOnClickListener {
            startScan()
        }
    }

    private fun initializeViews() {
        outputTextView = findViewById(R.id.outputText)
        outputResult = findViewById(R.id.resultText)
        lastVersion = findViewById(R.id.lastVersionText)
    }

    private fun startScan() {
        Toast.makeText(applicationContext, "Starting scan process", Toast.LENGTH_SHORT).show()
        openDirectoryPicker();
    }

    private fun handleActivityResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val treeUri: Uri? = result.data?.data
            treeUri?.let { uri ->
                fileList = listFilesInDirectory(uri)
                canScan = true
                pojavTreeUri = uri
                if (fileList.contains(".minecraft")) {
                    Toast.makeText(applicationContext, "Detected Pojav Launcher, running scan", Toast.LENGTH_SHORT).show()
                    runOnUiThread {
                        runScan()
                    }
                } else {
                    Toast.makeText(applicationContext, "Error selecting pojav files", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun runScan() {
        val versionsList: List<String>? = listVersions(pojavTreeUri)
        Log.d("Detected versions", versionsList.toString())

        Log.d("Check", "Starting version check")
        outputTextView.text = "Checking versions"
        checkVersion(versionsList)

        Log.d("Check", "Starting autoclicker check")
        outputTextView.text = "Running autoclicker check"
        checkAutoclicker()

        Log.d("Check", "Starting mods check")
        outputTextView.text = "Running mods check"
        listMods(pojavTreeUri)

        Log.d("Check", "Starting version check")
        outputTextView.text = "Processing information about launched version"
        processLaunchedVersion()

        Log.d("Check", "Starting jar execution check")
        outputTextView.text = "Running jar execution check"
        processJarExecution(pojavTreeUri)

        Log.d("Check", "Starting controlmap check")
        outputTextView.text = "Running controlmap check"
        controlmapCheck(pojavTreeUri)

        outputTextView.text = "Finished scan"
        result(isCheating)
        Log.d("Result", isCheating.toString())
    }

    private fun checkVersion(versionsList: List<String>?) {
        val versionFilter = VersionFilter()
        if (versionFilter.isVersionCheating(versionsList)) {
            isCheating = true
        }
    }

    private fun checkAutoclicker() {
        if (checkInstalledAutoclickers()) {
            isCheating = true
        }
    }

    fun setCheating(cheating: Boolean) {
        isCheating = cheating
    }

    @SuppressLint("SetTextI18n")
    private fun processLaunchedVersion() {
        val (lastRunVersion, launchedMods) = processLatestLog(pojavTreeUri)

        if (lastRunVersion != null && (lastRunVersion == "Forge" || lastRunVersion == "Fabric")) {
            lastVersion.text = "Last version launched: $lastRunVersion"
            println("Last run version: $lastRunVersion")
            Log.d("Last run version", lastRunVersion)

            launchedMods?.let { mods ->
                Log.d("Launched Mods", "Launched Mod List")
                mods.forEach { mod ->
                    Log.d("Launched mod", mod)
                }
            }
        } else {
            lastVersion.text = "Last version launched: Other"
        }
    }

    private fun listFilesInDirectory(treeUri: Uri?): List<String> {
        val fileList = mutableListOf<String>()

        if (treeUri == null) return fileList

        val pickedDir = DocumentFile.fromTreeUri(this, treeUri) ?: return fileList

        if (!pickedDir.exists()) return fileList

        for (file in pickedDir.listFiles() ?: emptyArray()) {
            fileList.add(file.name!!)
        }

        return fileList
    }

    private fun listVersions(treeUri: Uri?): List<String>? {
        val versionsList = mutableListOf<String>()

        if (treeUri == null) return null

        val pickedDir = DocumentFile.fromTreeUri(this, treeUri)
        val minecraftDir = pickedDir?.findFile(".minecraft") ?: return null

        if (!minecraftDir.isDirectory) return null

        val versionsDir = minecraftDir.findFile("versions") ?: return null

        if (!versionsDir.isDirectory) return null

        for (versionFile in versionsDir.listFiles()) {
            if (versionFile.isDirectory) {
                versionsList.add(versionFile.name!!)
            }
        }

        return if (versionsList.isEmpty()) null else versionsList
    }

    private fun checkInstalledAutoclickers(): Boolean {
        val packageManager = packageManager
        val installedApplications = packageManager.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES)
        val keywords = setOf("AutoClick", "Auto Click", "Clicker", "Tapper")

        for (applicationInfo in installedApplications) {
            val appName = applicationInfo.loadLabel(packageManager).toString()

            if (keywords.any { keyword -> appName.contains(keyword, ignoreCase = true) }) {
                return true
            }
        }

        return false
    }

    @SuppressLint("SetTextI18n")
    private fun result(cheating: Boolean) {
        if (keyCheating) {
            outputResult.setTextColor(Color.BLUE)
            outputResult.text = "Result: Modified controlmap (Keycheating)"
        }
        if (cheating) {
            outputResult.setTextColor(Color.RED)
            outputResult.text = "Result: Cheating"
        } else {
            if (keyCheating) {
                return
            }
            outputResult.setTextColor(Color.GREEN)
            outputResult.text = "Result: Legit"
        }
    }

    private fun openDirectoryPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        openDirectoryLauncher.launch(intent)
    }

    private fun controlmapCheck(treeUri: Uri?) {
        if (treeUri == null) return

        val controlListConfig: MutableList<DocumentFile> = mutableListOf()

        val pickedDir = DocumentFile.fromTreeUri(this, treeUri)
        val controlmapDir = pickedDir?.findFile("controlmap")

        var keycodes = 0

        controlmapDir?.listFiles()?.forEach { file ->
            if (file.isFile) {
                controlListConfig.add(file)
            }
        }
        Log.d("Controlmap config files", controlListConfig.toString())
        Log.d("Controlmap config size", controlListConfig.size.toString())

        for (file in controlListConfig) {
            if (readData(file).contains("\"isSwipeable\": true")) {
                Log.d("Controlmap Check", "Swipeable key found")
                isCheating = true
                keyCheating = true
            }
        }

        // Additional keycodes check
        for (file in controlListConfig) {
            if (readData(file).contains("[-3,") || readData(file).contains("-3,")) {
                Log.d("Controlmap Check", "Found primary key, adding to keycodes")
                Log.d("File", readData(file))
                keycodes++
            }
        }

        if (keycodes >= 3 && keycodes + 2 > controlListConfig.size) {
            Log.d("Controlmap Check", "Found more than 3 primary keys, Total Keycodes Found: $keycodes")
            isCheating = true
            keyCheating = true
        }

        for (file in controlListConfig) {
            getObjects(file.uri)
        }
    }

    @SuppressLint("Recycle")
    private fun getObjects(fileUri: Uri) {
        val inputStream = this.contentResolver.openInputStream(fileUri)
        val jsonString = inputStream?.bufferedReader().use { it?.readText() }
        val jsonObject = JSONObject(jsonString)
        val controlDataList = jsonObject.getJSONArray("mControlDataList")
        var count = 0
        for (i in 0 until controlDataList.length()) {
            val controlData = controlDataList.getJSONObject(i)
            val keycodesArray = controlData.getJSONArray("keycodes")
            if (keycodesArray.length() == 4) {
                for (j in 0 until 4) {
                    if (keycodesArray.getInt(j) == -3) {
                        count++
                        if (count > 2) {
                            isCheating = false
                            keyCheating = true
                            break
                        }
                    }
                }
            }
        }

        if (count > 3) {
            isCheating = true
            keyCheating = true
        }
    }

    private fun listMods(treeUri: Uri?): List<String>? {
        val modsList = mutableListOf<String>()
        val zippedList = mutableListOf<String>()

        if (treeUri != null) {
            val pickedDir = DocumentFile.fromTreeUri(this, treeUri)
            val minecraftDir = pickedDir?.findFile(".minecraft")
            val modsDir = minecraftDir?.findFile("mods")

            if (modsDir != null && modsDir.isDirectory) {
                for (modFile in modsDir.listFiles()) {
                    if (modFile.isFile && modFile.name!!.endsWith(".jar")) {
                        Log.d("Found mod in ./minecraft/mods", modFile.name!!)
                        modsList.add(modFile.name!!)
                        val fileInputStream = contentResolver.openInputStream(modFile.uri)
                        val bufferedInputStream = BufferedInputStream(fileInputStream)
                        val zipInputStream = ZipInputStream(bufferedInputStream)
                        var entry: ZipEntry? = zipInputStream.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory) {
                                val fileName = entry.name
                                Log.d("JarEntry", fileName)
                                zippedList.add(fileName)
                            }
                            entry = zipInputStream.nextEntry
                        }
                        zipInputStream.close()
                        bufferedInputStream.close()
                        fileInputStream?.close()
                    }
                }
            }
        }

        val keywordsToCheck = setOf(
            "killaura", "reach", "autoclick", "triggerbot", "clicker", "hitboxes",
            "velocity", "aimassist", "selfdestruct", "fastplace", "safewalk",
            "fly", "blockesp", "playeresp", "storageesp", "tracers", "nuker",
            "refill", "cheststealer", "nigger", "7wjiu482ab"
        )

        val zippedListLowercase = zippedList.map { it.toLowerCase() }

        val containsKeyword = zippedListLowercase.any { zipEntry ->
            keywordsToCheck.any { keyword ->
                zipEntry.contains(keyword)
            }
        }

        if (containsKeyword) {
            isCheating = true
        }

        return modsList.ifEmpty { null }
    }

    @SuppressLint("SetTextI18n")
    private fun processJarExecution(treeUri: Uri?) {
        if (treeUri == null) return

        val pickedDir = DocumentFile.fromTreeUri(this, treeUri)
        val latestLogJar = pickedDir?.findFile("latestlog.txt") ?: return

        if (!latestLogJar.isFile) return

        val logContent = readData(latestLogJar)
        val pattern = "net\\.kdt\\.pojavlaunch/cache/(.*?)\\.jar".toRegex()
        val lines = logContent.lines()

        for (line in lines) {
            val matchResult = pattern.find(line) ?: continue
            val jarFileName = matchResult.groupValues[1] + ".jar"

            Log.d("Last executed jar", jarFileName)
            val lastExecutedJar: TextView? = findViewById(R.id.lastExecutedText)
            lastExecutedJar?.text = "Last executed Jar: $jarFileName"
        }
    }

    private fun processLatestLog(treeUri: Uri?): Pair<String?, List<String>?> {
        if (treeUri == null) return Pair(null, null)

        val pickedDir = DocumentFile.fromTreeUri(this, treeUri)
        val minecraftDir = pickedDir?.findFile(".minecraft")
        val logsDir = minecraftDir?.findFile("logs")

        if (logsDir == null || !logsDir.isDirectory) return Pair(null, null)

        val latestLog = logsDir.findFile("latest.log")
        if (latestLog == null || !latestLog.isFile) return Pair(null, null)

        val logContent = readData(latestLog)

        if (garbage(logContent)) {
            isCheating = true
            return Pair(null, null)
        }

        val lastRunVersion = when {
            logContent.contains("ModLauncher running") -> "Forge"
            logContent.contains("fabricloader") -> "Fabric"
            else -> null
        }

        val launchedMods = when (lastRunVersion) {
            "Forge" -> logContent.lines().filter { line -> line.contains("Mod file") }
            "Fabric" -> {
                val loadingModsLine = logContent.lines().find { line ->
                    line.contains("Loading") && line.contains("mods")
                }

                val numMods = loadingModsLine?.let {
                    Regex("""(\d+) mods:""").find(it)?.groups?.get(1)?.value?.toIntOrNull()
                }

                numMods?.let {
                    logContent.lines().dropWhile { line -> line != loadingModsLine }.take(it)
                }
            }
            else -> null
        }

        return Pair(lastRunVersion, launchedMods)
    }

    private fun garbage(logContent: String): Boolean {
        val garbageWords = setOf(
            "Wurst", "Impact Client", "SkillClient", "LiquidBounce", "Huzuni",
            "Aristois", "Jam", "Metro Client", "Team Battle", "Grey Client",
            "Kr0w Client", "WeepCraft", "TacoClient", "Auxentity Client",
            "Reflex Client", "Cyanit Client", "Flare Client", "ThunderHack",
            "Lumina", "RusherHack", "Meteor", "Coffe", "Aoba", "AutoClick",
            "KillAura", "Reach", "TriggerBot", "SkilledClient", "bleachhack", "skligga"
        )

        val contentLines = logContent.split("\n")
        for (line in contentLines) {
            if (line.contains("[CHAT]", ignoreCase = true)) {
                continue
            }
            val contentInLowerCase = line.toLowerCase(Locale.ROOT)
            if (garbageWords.any { word -> contentInLowerCase.contains(word.toLowerCase(Locale.ROOT)) }) {
                return true
            }
        }
        return false
    }

    private fun readData(documentFile: DocumentFile): String {
        val inputStream = contentResolver.openInputStream(documentFile.uri)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val content = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            content.append(line).append("\n")
        }
        reader.close()
        inputStream?.close()
        return content.toString()
    }

    /**
     * A native method that is implemented by the 'shard' native library,
     * which is packaged with this application.
     */
    private external fun stringFromJNI(): String

    companion object {
        private const val PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 123
        private const val TAG = "MainActivity"

        init {
            System.loadLibrary("shard")
        }
    }
}