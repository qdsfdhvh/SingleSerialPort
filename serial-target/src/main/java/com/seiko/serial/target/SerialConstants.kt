package com.seiko.serial.target

///**
// * 默认底层指令队列发送间隔，因为PLC有3ms的发送延时，所以设为4ms。
// */
//internal const val DEFAULT_POST_TIME = 4L

/**
 * 最多等待*ms
 */
internal const val MAX_WAIT_RECEIVE_TIME = 60L

/**
 * 超过*ms时，认为连接断开，开始清理队列，防止阻塞。
 */
internal const val MAX_CONNECT_TIME = 500L