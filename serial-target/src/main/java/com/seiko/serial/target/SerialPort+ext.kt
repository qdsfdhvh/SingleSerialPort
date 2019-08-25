package com.seiko.serial.target

import com.seiko.serial.core.SerialPort
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.internal.operators.flowable.FlowableCreate
import io.reactivex.internal.operators.observable.ObservableCreate

fun SerialPort.toTarget(debug: Boolean = false): SerialTarget = SerialTarget(this, debug)

//fun SerialPort.toObservable(): Observable<ByteArray> {
//    return ObservableCreate { emitter ->
//        emitter.setCancellable(this::close)
//        open(object: SerialPort.Callback {
//            override fun onSuccess() {
//
//            }
//
//            override fun onResult(bytes: ByteArray) {
//                emitter.onNext(bytes)
//            }
//
//            override fun onError(e: Throwable) {
//                emitter.onError(e)
//            }
//        })
//    }
//}
//
//fun SerialPort.flowable(): Flowable<ByteArray> {
//    return FlowableCreate({ emitter ->
//        emitter.setCancellable(this::close)
//        open(object: SerialPort.Callback {
//            override fun onSuccess() {
//
//            }
//
//            override fun onResult(bytes: ByteArray) {
//                emitter.onNext(bytes)
//            }
//
//            override fun onError(e: Throwable) {
//                emitter.onError(e)
//            }
//        })
//    }, BackpressureStrategy.BUFFER)
//}