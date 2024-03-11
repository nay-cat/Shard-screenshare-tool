package com.nay.shard.filters

import android.util.Log

class VersionFilter {
    private val basicCheatVersions = arrayOf(
        "Wurst", "Impact Client", "SkillClient", "LiquidBounce", "Huzuni",
        "Aristois", "Jam", "Metro Client", "Team Battle", "Grey Client",
        "Kr0w Client", "WeepCraft", "TacoClient", "Auxentity Client",
        "Reflex Client", "Cyanit Client", "Flare Client", "ThunderHack",
        "Lumina", "RusherHack", "Meteor", "Coffe", "Aoba", "DoomsDay"
    )

    fun isVersionCheating(versionsList: List<String>?): Boolean {
        if (versionsList != null) {
            for (version in versionsList) {
                if (version in basicCheatVersions) {
                    Log.d("Cheating", "true $version")
                    return true
                }
            }
        }
        return false
    }
}
