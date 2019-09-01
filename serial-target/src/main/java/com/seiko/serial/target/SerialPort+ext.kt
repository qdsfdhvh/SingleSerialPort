package com.seiko.serial.target

import com.seiko.serial.core.SerialPort

@Suppress("NOTHING_TO_INLINE") // Syntactic sugar.
inline fun SerialPort.toTarget(): SerialTarget = SerialTarget(this)