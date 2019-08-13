package com.seiko.serial.target.reactive

import com.seiko.serial.target.SerialModule
import com.seiko.serial.target.reactive.data.BoxIntArray
import com.seiko.serial.target.reactive.data.BoxIntValue
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * 连续地址持续读取
 */
fun BoxIntArray.observable(target: SerialModule.Target,
                           postTime: Long = 400L,
                           debug: Boolean = false,
                           unit: TimeUnit = TimeUnit.MILLISECONDS,
                           scheduler: Scheduler = Schedulers.computation()): Observable<IntArray> {
    this.isDebug = debug
    return IntArrayModuleObservable(target, this, postTime, unit, scheduler)
}

/**
 * 单个地址持续读取
 */
fun BoxIntValue.observable(target: SerialModule.Target,
                           postTime: Long = 400L,
                           debug: Boolean = false,
                           unit: TimeUnit = TimeUnit.MILLISECONDS,
                           scheduler: Scheduler = Schedulers.computation()): Observable<Int> {
    this.isDebug = debug
    return IntValueModuleObservable(target, this, postTime, unit, scheduler)
}

/**
 * 连续地址只读一次
 */
fun BoxIntArray.single(target: SerialModule.Target,
                       debug: Boolean = false,
                       scheduler: Scheduler = Schedulers.computation()): Single<IntArray> {
    this.isDebug = debug
    return IntArrayModuleSingle(target, this, scheduler)
}

/**
 * 单个地址只读一次
 */
fun BoxIntValue.single(target: SerialModule.Target,
                       debug: Boolean = false,
                       scheduler: Scheduler = Schedulers.computation()): Single<Int> {
    this.isDebug = debug
    return IntValueModuleSingle(target, this, scheduler)
}