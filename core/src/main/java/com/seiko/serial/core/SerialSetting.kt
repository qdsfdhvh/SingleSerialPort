package com.seiko.serial.core

import androidx.annotation.IntDef

data class SerialSetting(
    val baudRate: Int,

    @Data val data: Int = Data.Bits8,
    @Stop val stop: Int = Stop.Bits1,
    @Parity val parity: Int = Parity.NONE,
    @FlowControl val flowControl: Int = FlowControl.OFF
) {

    @IntDef(Data.Bits8, Data.Bits7, Data.Bits6, Data.Bits5)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Data {
        companion object {
            const val Bits8 = 8
            const val Bits7 = 7
            const val Bits6 = 6
            const val Bits5 = 5
        }
    }

    @IntDef(FlowControl.OFF, FlowControl.RTS_CTS, FlowControl.DSR_DTR, FlowControl.XON_XOFF)
    @Retention(AnnotationRetention.SOURCE)
    annotation class FlowControl {
        companion object {
            const val OFF = 0
            const val RTS_CTS = 1
            const val DSR_DTR = 2
            const val XON_XOFF = 3
        }
    }

    @IntDef(Parity.NONE, Parity.ODD, Parity.EVENT, Parity.MARK, Parity.SPACE)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Parity {
        companion object {
            const val NONE = 0
            const val ODD = 1
            const val EVENT = 2
            const val MARK = 3
            const val SPACE = 4
        }
    }

    @IntDef(Stop.Bits1, Stop.Bits2, Stop.Bits3)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Stop {
        companion object {
            const val Bits1 = 1
            const val Bits2 = 2
            const val Bits3 = 3
        }
    }

}