package com.seiko.serial.target.reactive

import com.seiko.serial.target.SerialModule
import com.seiko.serial.target.reactive.data.BoxIntValue
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.internal.disposables.DisposableHelper
import io.reactivex.internal.schedulers.TrampolineScheduler
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/***********************************************************
 * 单个地址读取实现
 ***********************************************************/
class IntValueModuleObservable(
    private val target: SerialModule.Target,
    private val array: BoxIntValue,
    private val period: Long,
    private val unit: TimeUnit,
    private val scheduler: Scheduler
): Observable<Int>() {

    override fun subscribeActual(observer: Observer<in Int>?) {
        val ins = IntValueModuleObserver(observer, target, array)
        observer?.onSubscribe(ins)

        val sch = scheduler
        if (sch is TrampolineScheduler) {
            val worker = sch.createWorker()
            ins.setResource(worker)
            worker.schedulePeriodically(ins, 0, period, unit)
        } else {
            val d = sch.schedulePeriodicallyDirect(ins, 0, period, unit)
            ins.setResource(d)
        }
    }

    companion object {
        private class IntValueModuleObserver(private val observer: Observer<in Int>?,
                                             private val target: SerialModule.Target,
                                             private val array: BoxIntValue): AtomicReference<Disposable>(), Disposable, Runnable, SerialModule {

            override fun dispose() {
                DisposableHelper.dispose(this)
            }

            override fun isDisposed(): Boolean {
                return get() == DisposableHelper.DISPOSED
            }

            override fun run() {
                if (get() != DisposableHelper.DISPOSED) {
                    // post
                    target.send(this@IntValueModuleObserver, array.readByte())
                }
            }

            fun setResource(d: Disposable) {
                DisposableHelper.setOnce(this, d)
            }

            override fun attach(target: SerialModule.Target?) {

            }

//            override fun filter(bytes: ByteArray): Boolean {
//                return array.filter(bytes)
//            }

            override fun accept(bytes: ByteArray) {
                if (array.filter(bytes) && array.decode(bytes)) {
                    observer?.onNext(array.getDecodeValue())
                }
            }

            override fun getPriority(): Int {
                return 99
            }

            override fun getTag(): String {
                return "Module-${array.getStartAddress()}"
            }

        }
    }

}