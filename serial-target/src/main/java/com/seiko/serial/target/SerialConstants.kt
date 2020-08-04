package com.seiko.serial.target

/**
 * 最多等待*ms
 */
internal const val MAX_WAIT_RECEIVE_TIME = 60L

/**
 * 超过*ms时，认为连接断开，开始清理队列，防止阻塞。
 */
internal const val MAX_CONNECT_TIME = 500L