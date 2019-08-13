package com.seiko.serial.target.reactive

import com.seiko.serial.target.SerialModule
import com.seiko.serial.target.reactive.data.BoxIntArray
import io.reactivex.*
import io.reactivex.disposables.Disposable
import io.reactivex.internal.disposables.DisposableHelper
import io.reactivex.internal.disposables.EmptyDisposable
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference


/***********************************************************
 * 只会发送一次指令
 ***********************************************************/
class IntArrayModuleSingle(
    private val target: SerialModule.Target,
    private val array: BoxIntArray,
    private val scheduler: Scheduler
): Single<IntArray>() {

    override fun subscribeActual(observer: SingleObserver<in IntArray>) {
        val parent = IntArrayModuleDisposable(observer, target, array)
        observer.onSubscribe(parent)
        parent.setFuture(scheduler.scheduleDirect(parent, 500, TimeUnit.MILLISECONDS))
    }

    companion object {
        private class IntArrayModuleDisposable(private val downstream: SingleObserver<in IntArray>?,
                                               target: SerialModule.Target,
                                               private val array: BoxIntArray): AtomicReference<Disposable>(), Disposable, Runnable, SerialModule {

            init {
                target.send(this@IntArrayModuleDisposable, array.readByte())
            }

            override fun dispose() {
                DisposableHelper.dispose(this)
            }

            override fun isDisposed(): Boolean {
                return get() == DisposableHelper.DISPOSED
            }

            override fun run() {
                if (!isDisposed || get() != EmptyDisposable.INSTANCE) {
//                    downstream?.onSuccess(array.getDecodeArray())
                    downstream?.onError(TimeoutException("SerialTarget Receive Time Out."))
                }
            }

            fun setFuture(d: Disposable) {
                DisposableHelper.replace(this, d)
            }

            override fun attach(target: SerialModule.Target?) {

            }

//            override fun filter(bytes: ByteArray): Boolean {
//                return array.filter(bytes)
//            }

            override fun accept(bytes: ByteArray) {
                if (array.filter(bytes) && array.decode(bytes)) {
                    downstream?.onSuccess(array.getDecodeArray())
                    dispose()
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