package jp.co.tokubai.android.garage

import android.util.Log
import okhttp3.Call
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response

open class PostRequest(private val path: Path, private val requestBody: RequestBody, private val config: Config, val requestPreparing: (Request.Builder) -> Request.Builder = { it }) : GarageRequest() {

    var parameter: Parameter? = null

    override fun url(): String {
        with(config) {
            return ("${scheme.value}://${endpoint}:${customPort ?: scheme.port}/${path.to()}" + (parameter?.let { "?${it.build()}" } ?: "")).apply {
                if (config.isDebugMode) {
                    Log.d(GarageClient.TAG, "POST:$this")
                }
            }
        }
    }

    override fun newCall(requestProcessing: (Request.Builder) -> Request.Builder): Call {
        return config.client.newCall(
                requestProcessing(Request.Builder())
                        .url(url())
                        .post(requestBody)
                        .build())
    }

    override fun execute(): Response {
        val call = newCall(requestPreparing)
        try {
            return call.execute().apply{
                if (config.isDebugMode) {
                    Log.d(GarageClient.TAG, "POST ${this.code()} ${this.request().url()}")
                }
            }
        } catch(e: Exception) {
            throw GarageError(e).apply {
                this.call = call
                if (config.isDebugMode) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun requestTime() = System.currentTimeMillis()
}
