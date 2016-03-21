package com.sys1yagi.android.garage

import android.os.Handler
import com.sys1yagi.android.garage.auth.Authenticator
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import java.io.IOException
import java.net.HttpURLConnection

open class GarageClient(val configuration: GarageConfiguration) {

    class CallbackDelegator(val handler: Handler?, val success: (Call, Response) -> Unit, val failed: (Call, IOException) -> Unit, val authenticator: Authenticator?) : Callback {
        override fun onFailure(call: Call, exception: IOException) {
            handler?.let { it.post { failed(call, exception) } } ?: failed(call, exception)
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED && authenticator != null) {

            } else {
                handler?.let { it.post { success(call, response) } } ?: success(call, response)
            }
        }
    }

    class Caller(val call: Call, val configuration: GarageConfiguration) {
        fun enqueue(success: (Call, Response) -> Unit, failed: (Call, IOException) -> Unit) {
            with(configuration) {
                call.enqueue(CallbackDelegator(callbackHandler, success, failed, configuration.authenticator))
            }
        }

        fun execute(): Response {
            return Observable.create<Response> { subscriber ->
                enqueue(
                        { c, r ->
                            subscriber.onNext(r)
                            subscriber.onCompleted()
                        },
                        { c, e ->
                            subscriber.onError(e)
                        }
                )
            }.toBlocking().first()
        }
    }

    fun get(path: Path): Caller {
        with(configuration) {
            System.out.println("${scheme}://${endpoint}:$port/${path.versionName}/${path.path}")
            val request = Request.Builder()
                    .url("${scheme}://${endpoint}:$port/${path.path}")
                    .build()
            return Caller(client.newCall(request), configuration)
        }
    }

}