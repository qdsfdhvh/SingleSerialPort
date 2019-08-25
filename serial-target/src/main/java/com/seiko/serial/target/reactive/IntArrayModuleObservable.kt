package com.seiko.serial.target.reactive

import com.seiko.serial.target.SerialModule
import com.seiko.serial.target.reactive.data.BoxIntArray
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.internal.disposables.DisposableHelper
import io.reactivex.internal.schedulers.TrampolineScheduler
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/***********************************************************
 * 单一的只连续读取一段地址数据的实现
 ***********************************************************/
class IntArrayModuleObservable(
    private val target: SerialModule.Target,
    private val array: BoxIntArray,
    private val period: Long,
    private val unit: TimeUnit,
    private val scheduler: Scheduler
): Observable<IntArray>() {

    override fun subscribeActual(observer: Observer<in IntArray>?) {
        val ins = IntArrayModuleObserver(observer, target, array)
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
        private class IntArrayModuleObserver(
            private val observer: Observer<in IntArray>?,
            private val target: SerialModule.Target,
            private val array: BoxIntArray
        ): AtomicReference<Disposable>(), Disposable, Runnable, SerialModule {

            override fun dispose() {
                DisposableHelper.dispose(this)
            }

            override fun isDisposed(): Boolean {
                return get() == DisposableHelper.DISPOSED
            }

            override fun run() {
                if (get() != DisposableHelper.DISPOSED) {
                    // post
                    target.send(this@IntArrayModuleObserver, array.readByte())
                }
            }

            fun setResource(d: Disposable) {
                DisposableHelper.setOnce(this, d)
            }

            override fun attach(target: SerialModule.Target?) {

            }

            override fun accept(bytes: ByteArray) {
                if (array.filter(bytes) && array.decode(bytes)) {
                    observer?.onNext(array.getDecodeArray())
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
