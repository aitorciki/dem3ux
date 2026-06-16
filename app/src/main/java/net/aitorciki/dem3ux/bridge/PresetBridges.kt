package net.aitorciki.dem3ux.bridge

import kotlin.String
import kotlin.collections.List
import kotlin.collections.listOf

public object PresetBridges {
    public val aethersx2: PresetBridge =
        PresetBridge(
            id = "aethersx2",
            displayName = "Aethersx2",
            aliasClassName = "net.aitorciki.dem3ux.presets.Aethersx2BridgeActivity",
            targetActivities =
                listOf(
                    "xyz.aethersx2.android/.EmulationActivity",
                ),
            inputExtraKey = "bootPath",
            esDeEmulatorName = "AETHERSX2",
        )

    public val armsx2: PresetBridge =
        PresetBridge(
            id = "armsx2",
            displayName = "Armsx2",
            aliasClassName = "net.aitorciki.dem3ux.presets.Armsx2BridgeActivity",
            targetActivities =
                listOf(
                    "come.nanodata.armsx2/kr.co.iefriends.pcsx2.MainActivity",
                    "come.nanodata.armsx2.debug/kr.co.iefriends.pcsx2.MainActivity",
                ),
            esDeEmulatorName = "ARMSX2",
        )

    public val c64Emu: PresetBridge =
        PresetBridge(
            id = "c64-emu",
            displayName = "C64 Emu",
            aliasClassName = "net.aitorciki.dem3ux.presets.C64EmuBridgeActivity",
            targetActivities =
                listOf(
                    "com.explusalpha.C64Emu/com.imagine.BaseActivity",
                ),
            esDeEmulatorName = "C64-EMU",
        )

    public val colem: PresetBridge =
        PresetBridge(
            id = "colem",
            displayName = "Colem",
            aliasClassName = "net.aitorciki.dem3ux.presets.ColemBridgeActivity",
            targetActivities =
                listOf(
                    "com.fms.colem.deluxe/com.fms.emulib.TVActivity",
                    "com.fms.colem/com.fms.emulib.TVActivity",
                ),
            esDeEmulatorName = "COLEM",
        )

    public val dolphin: PresetBridge =
        PresetBridge(
            id = "dolphin",
            displayName = "Dolphin",
            aliasClassName = "net.aitorciki.dem3ux.presets.DolphinBridgeActivity",
            targetActivities =
                listOf(
                    "org.dolphinemu.dolphinemu/.ui.main.TvMainActivity",
                ),
            inputExtraKey = "AutoStartFile",
            esDeEmulatorName = "DOLPHIN",
        )

    public val dolphinMmjr: PresetBridge =
        PresetBridge(
            id = "dolphin-mmjr",
            displayName = "Dolphin Mmjr",
            aliasClassName = "net.aitorciki.dem3ux.presets.DolphinMmjrBridgeActivity",
            targetActivities =
                listOf(
                    "org.mm.jr/org.dolphinemu.dolphinemu.ui.main.MainActivity",
                ),
            inputExtraKey = "AutoStartFile",
            esDeEmulatorName = "DOLPHIN-MMJR",
        )

    public val dolphinMmjr2: PresetBridge =
        PresetBridge(
            id = "dolphin-mmjr2",
            displayName = "Dolphin Mmjr2",
            aliasClassName = "net.aitorciki.dem3ux.presets.DolphinMmjr2BridgeActivity",
            targetActivities =
                listOf(
                    "org.dolphinemu.mmjr/org.dolphinemu.dolphinemu.ui.main.MainActivity",
                ),
            inputExtraKey = "AutoStartFile",
            esDeEmulatorName = "DOLPHIN-MMJR2",
        )

    public val duckStation: PresetBridge =
        PresetBridge(
            id = "duckstation",
            displayName = "DuckStation",
            aliasClassName = "net.aitorciki.dem3ux.presets.DuckStationBridgeActivity",
            targetActivities =
                listOf(
                    "com.github.stenzek.duckstation/.EmulationActivity",
                ),
            inputExtraKey = "bootPath",
            esDeEmulatorName = "DUCKSTATION",
        )

    public val emucorex: PresetBridge =
        PresetBridge(
            id = "emucorex",
            displayName = "Emucorex",
            aliasClassName = "net.aitorciki.dem3ux.presets.EmucorexBridgeActivity",
            targetActivities =
                listOf(
                    "com.sbro.emucorex/.MainActivity",
                ),
            esDeEmulatorName = "EMUCOREX",
        )

    public val epsxe: PresetBridge =
        PresetBridge(
            id = "epsxe",
            displayName = "Epsxe",
            aliasClassName = "net.aitorciki.dem3ux.presets.EpsxeBridgeActivity",
            targetActivities =
                listOf(
                    "com.epsxe.ePSXe/.ePSXe",
                ),
            inputExtraKey = "com.epsxe.ePSXe.isoName",
            esDeEmulatorName = "EPSXE",
        )

    public val flycast: PresetBridge =
        PresetBridge(
            id = "flycast",
            displayName = "Flycast",
            aliasClassName = "net.aitorciki.dem3ux.presets.FlycastBridgeActivity",
            targetActivities =
                listOf(
                    "com.flycast.emulator/com.flycast.emulator.MainActivity",
                    "com.flycast.emulator/com.reicast.emulator.MainActivity",
                ),
            esDeEmulatorName = "FLYCAST",
        )

    public val fmsx: PresetBridge =
        PresetBridge(
            id = "fmsx",
            displayName = "Fmsx",
            aliasClassName = "net.aitorciki.dem3ux.presets.FmsxBridgeActivity",
            targetActivities =
                listOf(
                    "com.fms.fmsx.deluxe/com.fms.emulib.TVActivity",
                    "com.fms.fmsx/com.fms.emulib.TVActivity",
                ),
            esDeEmulatorName = "FMSX",
        )

    public val fpse: PresetBridge =
        PresetBridge(
            id = "fpse",
            displayName = "Fpse",
            aliasClassName = "net.aitorciki.dem3ux.presets.FpseBridgeActivity",
            targetActivities =
                listOf(
                    "com.emulator.fpse/.Main",
                ),
            esDeEmulatorName = "FPSE",
        )

    public val fpseNg: PresetBridge =
        PresetBridge(
            id = "fpse-ng",
            displayName = "Fpse Ng",
            aliasClassName = "net.aitorciki.dem3ux.presets.FpseNgBridgeActivity",
            targetActivities =
                listOf(
                    "com.emulator.fpse64/.Main",
                ),
            esDeEmulatorName = "FPSE-NG",
        )

    public val mastergear: PresetBridge =
        PresetBridge(
            id = "mastergear",
            displayName = "Mastergear",
            aliasClassName = "net.aitorciki.dem3ux.presets.MastergearBridgeActivity",
            targetActivities =
                listOf(
                    "com.fms.mg/com.fms.emulib.TVActivity",
                ),
            esDeEmulatorName = "MASTERGEAR",
        )

    public val mdEmu: PresetBridge =
        PresetBridge(
            id = "md-emu",
            displayName = "Md Emu",
            aliasClassName = "net.aitorciki.dem3ux.presets.MdEmuBridgeActivity",
            targetActivities =
                listOf(
                    "com.explusalpha.MdEmu/com.imagine.BaseActivity",
                ),
            esDeEmulatorName = "MD-EMU",
        )

    public val msxEmu: PresetBridge =
        PresetBridge(
            id = "msx-emu",
            displayName = "Msx Emu",
            aliasClassName = "net.aitorciki.dem3ux.presets.MsxEmuBridgeActivity",
            targetActivities =
                listOf(
                    "com.explusalpha.MsxEmu/com.imagine.BaseActivity",
                ),
            esDeEmulatorName = "MSX-EMU",
        )

    public val pceEmu: PresetBridge =
        PresetBridge(
            id = "pce-emu",
            displayName = "Pce Emu",
            aliasClassName = "net.aitorciki.dem3ux.presets.PceEmuBridgeActivity",
            targetActivities =
                listOf(
                    "com.PceEmu/com.imagine.BaseActivity",
                ),
            esDeEmulatorName = "PCE-EMU",
        )

    public val pizzaBoySc: PresetBridge =
        PresetBridge(
            id = "pizza-boy-sc",
            displayName = "Pizza Boy Sc",
            aliasClassName = "net.aitorciki.dem3ux.presets.PizzaBoyScBridgeActivity",
            targetActivities =
                listOf(
                    "it.dbtecno.pizzaboyscpro/.MainActivity",
                    "it.dbtecno.pizzaboyscbasic/.MainActivity",
                ),
            inputExtraKey = "rom_uri",
            esDeEmulatorName = "PIZZA-BOY-SC",
        )

    public val play: PresetBridge =
        PresetBridge(
            id = "play",
            displayName = "Play",
            aliasClassName = "net.aitorciki.dem3ux.presets.PlayBridgeActivity",
            targetActivities =
                listOf(
                    "com.virtualapplications.play/.MainActivity",
                ),
            esDeEmulatorName = "PLAY!",
        )

    public val redream: PresetBridge =
        PresetBridge(
            id = "redream",
            displayName = "Redream",
            aliasClassName = "net.aitorciki.dem3ux.presets.RedreamBridgeActivity",
            targetActivities =
                listOf(
                    "io.recompiled.redream/.MainActivity",
                ),
            esDeEmulatorName = "REDREAM",
        )

    public val saturnEmu: PresetBridge =
        PresetBridge(
            id = "saturn-emu",
            displayName = "Saturn Emu",
            aliasClassName = "net.aitorciki.dem3ux.presets.SaturnEmuBridgeActivity",
            targetActivities =
                listOf(
                    "com.explusalpha.SaturnEmu/com.imagine.BaseActivity",
                ),
            esDeEmulatorName = "SATURN-EMU",
        )

    public val yabasanshiro2: PresetBridge =
        PresetBridge(
            id = "yabasanshiro-2",
            displayName = "Yabasanshiro 2",
            aliasClassName = "net.aitorciki.dem3ux.presets.Yabasanshiro2BridgeActivity",
            targetActivities =
                listOf(
                    "org.devmiyax.yabasanshioro2.pro/org.uoyabause.android.Yabause",
                    "org.devmiyax.yabasanshioro2/org.uoyabause.android.Yabause",
                ),
            inputExtraKey = "org.uoyabause.android.FileNameUri",
            esDeEmulatorName = "YABASANSHIRO-2",
        )

    public val all: List<PresetBridge> =
        listOf(
            aethersx2,
            armsx2,
            c64Emu,
            colem,
            dolphin,
            dolphinMmjr,
            dolphinMmjr2,
            duckStation,
            emucorex,
            epsxe,
            flycast,
            fmsx,
            fpse,
            fpseNg,
            mastergear,
            mdEmu,
            msxEmu,
            pceEmu,
            pizzaBoySc,
            play,
            redream,
            saturnEmu,
            yabasanshiro2,
        )

    public fun fromAliasClassName(className: String): PresetBridge? =
        when (className) {
            "net.aitorciki.dem3ux.presets.Aethersx2BridgeActivity" -> aethersx2
            "net.aitorciki.dem3ux.presets.Armsx2BridgeActivity" -> armsx2
            "net.aitorciki.dem3ux.presets.C64EmuBridgeActivity" -> c64Emu
            "net.aitorciki.dem3ux.presets.ColemBridgeActivity" -> colem
            "net.aitorciki.dem3ux.presets.DolphinBridgeActivity" -> dolphin
            "net.aitorciki.dem3ux.presets.DolphinMmjrBridgeActivity" -> dolphinMmjr
            "net.aitorciki.dem3ux.presets.DolphinMmjr2BridgeActivity" -> dolphinMmjr2
            "net.aitorciki.dem3ux.presets.DuckStationBridgeActivity" -> duckStation
            "net.aitorciki.dem3ux.presets.EmucorexBridgeActivity" -> emucorex
            "net.aitorciki.dem3ux.presets.EpsxeBridgeActivity" -> epsxe
            "net.aitorciki.dem3ux.presets.FlycastBridgeActivity" -> flycast
            "net.aitorciki.dem3ux.presets.FmsxBridgeActivity" -> fmsx
            "net.aitorciki.dem3ux.presets.FpseBridgeActivity" -> fpse
            "net.aitorciki.dem3ux.presets.FpseNgBridgeActivity" -> fpseNg
            "net.aitorciki.dem3ux.presets.MastergearBridgeActivity" -> mastergear
            "net.aitorciki.dem3ux.presets.MdEmuBridgeActivity" -> mdEmu
            "net.aitorciki.dem3ux.presets.MsxEmuBridgeActivity" -> msxEmu
            "net.aitorciki.dem3ux.presets.PceEmuBridgeActivity" -> pceEmu
            "net.aitorciki.dem3ux.presets.PizzaBoyScBridgeActivity" -> pizzaBoySc
            "net.aitorciki.dem3ux.presets.PlayBridgeActivity" -> play
            "net.aitorciki.dem3ux.presets.RedreamBridgeActivity" -> redream
            "net.aitorciki.dem3ux.presets.SaturnEmuBridgeActivity" -> saturnEmu
            "net.aitorciki.dem3ux.presets.Yabasanshiro2BridgeActivity" -> yabasanshiro2
            else -> null
        }
}
