package net.shotakosaka.wormquant

import ij.IJ
import ij.ImageJ

object WormQuantLauncher {
    @JvmStatic
    fun main(args: Array<String>) {
        // ImageJ GUIを起動
        ImageJ()

        // WormQuant プラグインを即座に実行
        IJ.runPlugIn(WormQuant::class.java.name, "")
    }
}
