package com.seiko.serial.target

import com.seiko.serial.target.data.BoxIntValue
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import io.reactivex.internal.disposables.DisposableHelper
import io.reactivex.internal.disposables.EmptyDisposable
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference

/***********************************************************
 * 只会发送一次指令
 ***********************************************************/
class IntValueModuleSingle(
    private val target: SerialModule.Target,
    private val array: BoxIntValue,
    private val scheduler: Scheduler
): Single<Int>() {

    override fun subscribeActual(observer: SingleObserver<in Int>) {
        val parent = IntValueModuleDisposable(
            observer,
            target,
            array
        )
        observer.onSubscribe(parent)
        parent.setFuture(scheduler.scheduleDirect(parent, 500, TimeUnit.MILLISECONDS))
    }

    private class IntValueModuleDisposable(
        private val downstream: SingleObserver<in Int>?,
        target: SerialModule.Target,
        private val array: BoxIntValue
    ): AtomicReference<Disposable>(), Disposable, Runnable, SerialModule {

        init {
            target.send(this@IntValueModuleDisposable, array.readByte())
        }

        override fun dispose() {
            DisposableHelper.dispose(this)
        }

        override fun isDisposed(): Boolean {
            return get() == DisposableHelper.DISPOSED
        }

        override fun run() {
            if (!isDisposed || get() != EmptyDisposable.INSTANCE) {
                    downstream?.onSuccess(array.getDecodeValue())
//                downstream?.onError(TimeoutException("SerialTarget Receive Time Out."))
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

        override fun accept(bytes: ByteArray): Boolean {
            if (array.filter(bytes) && array.decode(bytes)) {
                downstream?.onSuccess(array.getDecodeValue())
                dispose()
            }
            return true
        }

        override fun getPriority(): Int = array.priority

        override fun getTag(): String = "Module-${array.getStartAddress()}"

    }

}