package org.silvermoon.osm_kotlin.util

import android.os.Handler


abstract class CountDownTimer(
    private var millisInFuture: Long,
    private val countDownInterval: Long
) {
    private var cancelled = false
    abstract fun onFinish()
    abstract fun onTick()
    fun start() {
        cancelled = false
        val handler = Handler()
        val counter: Runnable = object : Runnable {
            override fun run() {
                if (millisInFuture <= 0 || cancelled) {
                    onFinish()
                } else {
                    onTick()
                    millisInFuture -= countDownInterval
                    handler.postDelayed(this, countDownInterval)
                }
            }
        }
        handler.postDelayed(counter, countDownInterval)
    }

    fun cancel() {
        cancelled = true
    }

}