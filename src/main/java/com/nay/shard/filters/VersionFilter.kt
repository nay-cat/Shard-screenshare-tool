package com.nay.shard.filters

import android.util.Log
import com.nay.shard.MainActivity

class VersionFilter {


    private val basicCheatVersions = setOf(
        "Wurst", "Impact Client", "SkillClient", "LiquidBounce", "Huzuni",
        "Aristois", "Jam", "Metro Client", "Team Battle", "Grey Client",
        "Kr0w Client", "WeepCraft", "TacoClient", "Auxentity Client",
        "Reflex Client", "Cyanit Client", "Flare Client", "ThunderHack",
        "Lumina", "RusherHack", "Meteor", "Coffe", "Aoba", "DoomsDay"
    )

    fun isVersionCheating(versionsList: List<String>?): Boolean {
        for (cheatName in basicCheatVersions){
            if (versionsList.toString().contains(cheatName)){
                Log.d("Cheating", "true "+cheatName)
                return true;
            }
        }
        return false;
    }
}