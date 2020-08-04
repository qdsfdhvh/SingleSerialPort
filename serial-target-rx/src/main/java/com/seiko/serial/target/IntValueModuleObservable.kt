package com.seiko.serial.target

import com.seiko.serial.target.data.BoxIntValue
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.internal.disposables.DisposableHelper
import io.reactivex.rxjava3.internal.schedulers.TrampolineScheduler
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
        val ins = IntValueModuleObserver(
            observer,
            target,
            array
        )
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

    private class IntValueModuleObserver(
        private val observer: Observer<in Int>?,
        private val target: SerialModule.Target,
        private val array: BoxIntValue
    ): AtomicReference<Disposable>(), Disposable, Runnable, SerialModule {

        override fun dispose() {
            DisposableHelper.dispose(this)
        }

        override fun isDisposed(): Boolean {
            return get() == DisposableHelper.DISPOSED
        }

        override fun run() {
            if (get() != DisposableHelper.DISPOSED) {
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

        override fun accept(bytes: ByteArray): Boolean {
            if (array.filter(bytes) && array.decode(bytes)) {
                observer?.onNext(array.getDecodeValue())
            }
            return true
        }

        override fun getPriority(): Int = array.priority

        override fun getTag(): String = "Module-${array.getStartAddress()}"

    }

}