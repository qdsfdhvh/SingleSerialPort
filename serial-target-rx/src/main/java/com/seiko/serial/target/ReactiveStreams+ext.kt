package com.seiko.serial.target

import com.seiko.serial.target.data.BoxIntArray
import com.seiko.serial.target.data.BoxIntValue
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * 连续地址持续读取
 */
fun BoxIntArray.toObservable(target: SerialModule.Target,
                             debug: Boolean = false,
                             priority: Int = 99,
                             postTime: Long = 400L,
                             unit: TimeUnit = TimeUnit.MILLISECONDS,
                             scheduler: Scheduler = Schedulers.computation()): Observable<IntArray> {
    this.debug = debug
    this.priority = priority
    return IntArrayModuleObservable(target, this, postTime, unit, scheduler)
}

/**
 * 单个地址持续读取
 */
fun BoxIntValue.toObservable(target: SerialModule.Target,
                             debug: Boolean = false,
                             priority: Int = 99,
                             postTime: Long = 400L,
                             unit: TimeUnit = TimeUnit.MILLISECONDS,
                             scheduler: Scheduler = Schedulers.computation()): Observable<Int> {
    this.debug = debug
    this.priority = priority
    return IntValueModuleObservable(target, this, postTime, unit, scheduler)
}

/**
 * 连续地址只读一次
 */
fun BoxIntArray.toSingle(target: SerialModule.Target,
                         debug: Boolean = false,
                         priority: Int = 99,
                         scheduler: Scheduler = Schedulers.computation()): Single<IntArray> {
    this.debug = debug
    this.priority = priority
    return IntArrayModuleSingle(target, this, scheduler)
}

/**
 * 单个地址只读一次
 */
fun BoxIntValue.toSingle(target: SerialModule.Target,
                         debug: Boolean = false,
                         priority: Int = 99,
                         scheduler: Scheduler = Schedulers.computation()): Single<Int> {
    this.debug = debug
    this.priority = priority
    return IntValueModuleSingle(target, this, scheduler)
}