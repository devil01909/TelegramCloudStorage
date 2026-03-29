package com.yourname.telegramcloud.network

import android.util.Log
import org.drinkless.tdlib.TdApi
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class TdlibListener : org.drinkless.tdlib.Client.ResultHandler {
    
    companion object {
        private const val TAG = "TdlibListener"
    }
    
    private val callbacks = ConcurrentHashMap<Int, (TdApi.Object) -> Unit>()
    private val queryIdGenerator = AtomicInteger(0)
    
    // Update handlers
    private var updateHandler: ((TdApi.Object) -> Unit)? = null
    
    fun setUpdateHandler(handler: (TdApi.Object) -> Unit) {
        this.updateHandler = handler
    }
    
    fun sendRequest(request: TdApi.Function, callback: (TdApi.Object) -> Unit): Int {
        val queryId = queryIdGenerator.incrementAndGet()
        callbacks[queryId] = callback
        return queryId
    }
    
    override fun onResult(result: TdApi.Object) {
        // Check if this is a response to a query
        if (result is TdApi.Update) {
            // This is an update, handle via update handler
            updateHandler?.invoke(result)
        } else {
            // This is a response to a specific query
            // Find which query this belongs to (handled by client's query ID)
            // For now, we'll log it
            Log.d(TAG, "Received response: ${result::class.java.simpleName}")
        }
    }
    
    fun handleResponse(queryId: Int, result: TdApi.Object) {
        callbacks.remove(queryId)?.invoke(result)
    }
    
    fun clear() {
        callbacks.clear()
        updateHandler = null
    }
}