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
import android.view.View
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
    //private lateinit var moreInfo: Button
    private var canScan: Boolean = false
    lateinit var fileList: List<String>
    var pojavTreeUri: Uri? = null
    var isCheating: Boolean = false;
    //private var version: Double = 1.0;

    /*
    Perdonad si veis comillas o incongruencias, tengo más experiencia en Java y Kotlin me resulta bastante lioso
     */

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    val openDirectoryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val treeUri: Uri? = result.data?.data
                fileList = listFilesInDirectory(treeUri)
                canScan = true;
                pojavTreeUri = treeUri;
                if (fileList.contains(".minecraft")) {
                    Toast.makeText(
                        applicationContext,
                        "Detected Pojav Launcher, running scan",
                        Toast.LENGTH_SHORT
                    ).show()
                    printText("Starting file checking")
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            123
                        )
                    } else {
                        // Version list
                        val versionsList: List<String>? = listVersions(treeUri)
                        Log.d("Detected versions", versionsList.toString())
                        // Check mod list
                        val modsList: List<String>? = listMods(treeUri)
                        Log.d("Detected mods", modsList.toString())


                        printText("Running versions check")
                        val versionFilter = VersionFilter()

                        if (versionFilter.isVersionCheating(versionsList)){
                            isCheating = true
                        }

                        // Check autoclicker packages
                        printText("Running autoclicker check")
                        PackageFinder.checkAutoclicker(this);

                        if (checkInstalledAutoclickers(this)){
                            isCheating = true
                        }

                        printText("Processing information abt launched version")
                        val result = processLatestLog(treeUri)
                        var lastRunnedVersion: String? = result.first
                        val launchedMods: List<String>? = result.second
                        if (lastRunnedVersion != null && lastRunnedVersion == "Forge" || lastRunnedVersion == "Fabric") {
                            lastVersion.text = "Last version launched: $lastRunnedVersion"
                            println("Last runned version: $lastRunnedVersion")
                            Log.d("Last runned version", lastRunnedVersion)
                            if (launchedMods != null) {
                                Log.d("Launched Mods", "Launched Mod List")
                                for (mod in launchedMods) {
                                    Log.d("Launched mod", mod)
                                }
                            }
                        } else {
                            lastVersion.text = "Last version launched: Other (Not forge or fabric)"
                        }

                        // Jar execution check
                        printText("Running jar execution check")
                        processJarExecution(treeUri)

                        // End checks
                        printText("Finished scan")
                        result(isCheating)
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
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Error selecting pojav files",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("WrongViewCast", "MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val scanButton: Button = findViewById(R.id.scanButton)
        //moreInfo = findViewById(R.id.scanInfo)
        outputTextView = findViewById(R.id.output)
        outputResult = findViewById(R.id.resultText)
        lastVersion = findViewById(R.id.lastVersion)

        scanButton.setOnClickListener {
            Toast.makeText(applicationContext, "Starting scan process", Toast.LENGTH_SHORT).show()
            if (isPojavLauncherInstalled(this)) {
                openDirectoryPicker()
            } else {
                Toast.makeText(applicationContext, "Cant detect Pojav Launcher", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isPojavLauncherInstalled(context: Context): Boolean {
        val packageManager = context.packageManager
        return try {
            val packageInfo = packageManager.getPackageInfo("net.kdt.pojavlaunch", PackageManager.GET_ACTIVITIES)
            Log.d("PojavLauncher", "$packageInfo")
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun printText(text: String) {
        outputTextView.text = text
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 123) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openDirectoryPicker()
            } else {
                printText("No permission")
            }
        }
    }

    private fun openDirectoryPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        openDirectoryLauncher.launch(intent)
    }

    private fun listFilesInDirectory(treeUri: Uri?): List<String> {
        val fileList = mutableListOf<String>()
        if (treeUri != null) {
            val pickedDir = DocumentFile.fromTreeUri(this, treeUri)
            if (pickedDir != null && pickedDir.exists()) {
                for (file in pickedDir.listFiles()) {
                    fileList.add(file.name!!)
                }
            }
        }
        return fileList
    }

    private fun listVersions(treeUri: Uri?): List<String>? {
        val versionsList = mutableListOf<String>()
        if (treeUri != null) {
            val pickedDir = DocumentFile.fromTreeUri(this, treeUri)
            val minecraftDir = pickedDir?.findFile(".minecraft")
            if (minecraftDir != null && minecraftDir.isDirectory) {
                val versionsDir = minecraftDir.findFile("versions")
                if (versionsDir != null && versionsDir.isDirectory) {
                    for (versionFile in versionsDir.listFiles()) {
                        if (versionFile.isDirectory) {
                            versionsList.add(versionFile.name!!)
                        }
                    }
                }
            }
        }
        return versionsList.ifEmpty { null }
    }

    private fun listMods(treeUri: Uri?): List<String>? {
        val modsList = mutableListOf<String>()
        val zippedList = mutableListOf<String>()

        if (treeUri != null) {
            val pickedDir = DocumentFile.fromTreeUri(this, treeUri)
            val minecraftDir = pickedDir?.findFile(".minecraft")
            if (minecraftDir != null && minecraftDir.isDirectory) {
                val modsDir = minecraftDir.findFile("mods")
                if (modsDir != null && modsDir.isDirectory) {
                    for (modFile in modsDir.listFiles()) {
                        if (modFile.isFile && modFile.name!!.endsWith(".jar")) {
                            val jarFilePath = modFile.uri.path!!
                            modsList.add(modFile.name!!)
                            if (jarFilePath.endsWith(".jar")) {
                                val jar = JarFile(File(jarFilePath))
                                val entries = jar.entries()
                                while (entries.hasMoreElements()) {
                                    val entry = entries.nextElement()
                                    if (entry.name.endsWith(".class")) {
                                        val className = entry.name.replace('/', '.').removeSuffix(".class")
                                        zippedList.add(className)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val keywordsToCheck = setOf("KillAura", "Reach", "AutoClick", "TriggerBot")

        val containsKeywords = zippedList.any { className ->
            keywordsToCheck.any { keyword ->
                className.contains(keyword, ignoreCase = true)
            }
        }

        if (containsKeywords) {
            isCheating = true;
        }

        return modsList.ifEmpty { null }
    }


    @SuppressLint("SetTextI18n")
    fun result(cheating: Boolean){
        if (cheating){
            outputResult.setTextColor(Color.RED)
            outputResult.text = "Result: Cheating"
        } else {
            outputResult.setTextColor(Color.GREEN)
            outputResult.text = "Result: Legit"
        }
    }

    private fun checkInstalledAutoclickers(context: Context): Boolean {
        val packageManager = context.packageManager
        val installedApplications = packageManager.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES)
        val keywords = setOf("AutoClick", "Auto Clicker", "Auto Click")
        for (applicationInfo in installedApplications) {
            val appName = applicationInfo.loadLabel(packageManager).toString()
            if (keywords.any { keyword -> appName.contains(keyword, ignoreCase = true) }) {
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

    @SuppressLint("SetTextI18n")
    private fun processJarExecution(treeUri: Uri?) {
        if (treeUri != null) {
            val pickedDir = DocumentFile.fromTreeUri(this, treeUri)
            val latestLogJar = pickedDir?.findFile("latestlog.txt")
            if (latestLogJar != null && latestLogJar.isFile) {
                val logContent = readData(latestLogJar)
                val lines = logContent.split("\n")
                for (line in lines) {
                    val matchResult = Regex("net\\.kdt\\.pojavlaunch/cache/(.*?)\\.jar").find(line)
                    if (matchResult != null) {
                        Log.d("Last executed jar", matchResult.groupValues[1]+".jar")
                        val lastExecutedJar: TextView = findViewById(R.id.lastExecuted)
                        lastExecutedJar.text = "Last executed Jar: "+matchResult.groupValues[1]+".jar"
                    }
                }
            }
        }
    }

    private fun processLatestLog(treeUri: Uri?): Pair<String?, List<String>?> {
        var lastRunnedVersion: String? = null
        var launchedMods: List<String>? = null

        if (treeUri != null) {
            val pickedDir = DocumentFile.fromTreeUri(this, treeUri)
            val minecraftDir = pickedDir?.findFile(".minecraft")
            val logsDir = minecraftDir?.findFile("logs")
            if (logsDir != null && logsDir.isDirectory) {
                val latestLog = logsDir.findFile("latest.log")
                if (latestLog != null && latestLog.isFile) {
                    val logContent = readData(latestLog)
                    if (garbage(logContent)){
                        isCheating = true;
                    }
                    if (logContent.contains("ModLauncher running")) {
                        lastRunnedVersion = "Forge"
                        launchedMods = logContent.lines().filter { it.contains("Mod file") }
                    } else if (logContent.contains("fabricloader")) {
                        lastRunnedVersion = "Fabric"
                        val loadingModsLine = logContent.lines().find { it.contains("Loading") && it.contains("mods") }
                        if (loadingModsLine != null) {
                            val matchResult = Regex("""(\d+) mods:""").find(loadingModsLine)
                            val numMods = matchResult?.groups?.get(1)?.value?.toIntOrNull()
                            if (numMods != null) {
                                launchedMods = logContent.lines().dropWhile { it != loadingModsLine }.take(numMods)
                            }
                        }
                    }
                }
            }
        }

        return Pair(lastRunnedVersion, launchedMods)
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
        return garbageWords.any { word -> logContent.contains(word, ignoreCase = true) }
    }

}