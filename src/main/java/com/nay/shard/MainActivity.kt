package com.nay.shard

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.nay.shard.filters.PackageFinder
import com.nay.shard.filters.VersionFilter
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.jar.JarFile

class MainActivity : AppCompatActivity() {

    private lateinit var outputTextView: TextView
    private lateinit var outputResult: TextView
    private lateinit var lastVersion: TextView
    // private lateinit var moreInfo: Button
    private var canScan: Boolean = false
    private var fileList: List<String> = emptyList()
    private var pojavTreeUri: Uri? = null
    private var isCheating: Boolean = false
    // private var version: Double = 1.0;
    private val openDirectoryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        handleActivityResult(result)
    }

    // @RequiresApi(Build.VERSION_CODES.Q)
    // @SuppressLint("WrongViewCast", "MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()

        val scanButton: Button = findViewById(R.id.scanButton)
        // moreInfo = findViewById(R.id.scanInfo)
        scanButton.setOnClickListener {
            startScan()
        }
    }

    private fun initializeViews() {
        outputTextView = findViewById(R.id.output)
        outputResult = findViewById(R.id.resultText)
        lastVersion = findViewById(R.id.lastVersion)
    }

    private fun startScan() {
        Toast.makeText(applicationContext, "Starting scan process", Toast.LENGTH_SHORT).show()
        if (isPojavLauncherInstalled()) {
            openDirectoryPicker()
        } else {
            Toast.makeText(applicationContext, "Can't detect Pojav Launcher", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isPojavLauncherInstalled(): Boolean {
        val packageManager = packageManager
        return try {
            packageManager.getPackageInfo("net.kdt.pojavlaunch", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
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

    private fun runScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE)
        } else {
            val versionsList: List<String>? = listVersions(pojavTreeUri)
            Log.d("Detected versions", versionsList.toString())

            checkVersion(versionsList)

            outputTextView.text = "Running autoclicker check"
            checkAutoclicker()

            outputTextView.text = "Processing information about launched version"
            processLaunchedVersion()

            outputTextView.text = "Running jar execution check"
            processJarExecution(pojavTreeUri)

            outputTextView.text = "Finished scan"
            result(isCheating)
        }

        //moreInfo.visibility = View.VISIBLE;

                        /* Next update
                        moreInfo.setOnClickListener {
                            setContentView(R.layout.basic_information)
                            val modListInfo: TextView = findViewById(R.id.modList)
                            modListInfo.text = modsList.toString();
                            val launchedModListInfo: TextView = findViewById(R.id.launchedMods)
                            launchedModListInfo.text = launchedMods.toString();
                            val versionList: TextView = findViewById(R.id.versionList)
                            versionList.text = versionList.toString()

                        }*/
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
            lastVersion.text = "Last version launched: Other (Not Forge or Fabric)"
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
        val keywords = setOf("AutoClick", "Auto Click", "Clicker")
    
        for (applicationInfo in installedApplications) {
            val appName = applicationInfo.loadLabel(packageManager).toString()
            
            if (keywords.any { keyword -> appName.contains(keyword, ignoreCase = true) }) {
                return true
            }
        }
        
        return false
    }    

    private fun result(cheating: Boolean) {
        if (cheating) {
            outputResult.setTextColor(Color.RED)
            outputResult.text = "Result: Cheating"
        } else {
            outputResult.setTextColor(Color.GREEN)
            outputResult.text = "Result: Legit"
        }
    }

    private fun openDirectoryPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        openDirectoryLauncher.launch(intent)
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
            val lastExecutedJar: TextView? = findViewById(R.id.lastExecuted)
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
            "KillAura", "Reach", "TriggerBot", "SkilledClient", "DoomsDay"
        )
    
        val contentInLowerCase = logContent.toLowerCase()
        return garbageWords.any { word -> contentInLowerCase.contains(word.toLowerCase()) }
    }    

    companion object {
        private const val PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 123
        private const val TAG = "MainActivity"
    }
}
