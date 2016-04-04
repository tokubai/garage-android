package com.sys1yagi.android.garage

import com.sys1yagi.android.garage.core.BuildConfig
import com.sys1yagi.android.garage.testtool.milliseconds
import com.sys1yagi.android.garage.testtool.takeRequest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class GarageClientTest {

    fun createGarageClient(mockWebServer: MockWebServer, builder: GarageConfiguration.() -> Unit = {}) =
            GarageClient(GarageConfiguration.Companion.make("a", "b", mockWebServer.hostName, OkHttpClient()) {
                port = mockWebServer.port
                builder.invoke(this)
            })

    lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun notYetAuth() {
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        mockWebServer.enqueue(MockResponse().setResponseCode(200)
                .setBody("{\"access_token\":\"4bf2014681df03d9fa6ff2469d7b5594d85de2a6ca7ab15bcc5fd33d07bd1139\",\"token_type\":\"bearer\",\"expires_in\":7200,\"scope\":\"public\"}"))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        mockWebServer.start()

        val client: GarageClient = createGarageClient(mockWebServer)

        try {
            val response = client.get(Path("v1", "users")).execute()
            assertThat(response.code()).isEqualTo(200)
        } catch(e: IOException) {
            fail(e.message)
        }

        mockWebServer.takeRequest().let {
            assertThat(it.method).isEqualTo("GET")
            assertThat(it.path).isEqualTo("/v1/users")
        }
        mockWebServer.takeRequest().let {
            assertThat(it.method).isEqualTo("POST")
            assertThat(it.path).isEqualTo("/oauth/token")
        }
        mockWebServer.takeRequest().let {
            assertThat(it.method).isEqualTo("GET")
            assertThat(it.path).isEqualTo("/v1/users")
        }
        assertThat(client.configuration.accessTokenHolder.accessToken).isEqualTo("4bf2014681df03d9fa6ff2469d7b5594d85de2a6ca7ab15bcc5fd33d07bd1139")
    }

    @Test
    fun noRetry() {
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        mockWebServer.start()

        val client: GarageClient = createGarageClient(mockWebServer)

        try {
            val response = client.get(Path("v1", "users"))
                    .setMaxRetryCount(0)
                    .execute()
            assertThat(response.code()).isEqualTo(401)
        } catch(e: IOException) {
            fail(e.message)
        }

        mockWebServer.takeRequest().let {
            assertThat(it.getHeader("User-Agent")).isEqualTo("garage-android-${BuildConfig.VERSION_NAME}")
            assertThat(it.method).isEqualTo("GET")
            assertThat(it.path).isEqualTo("/v1/users")
        }
        mockWebServer.takeRequest(10.milliseconds)?.let {
            fail("should not retry")
        }
    }

    @Test
    fun customUserAgent() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        mockWebServer.start()
        val client: GarageClient = createGarageClient(mockWebServer, {
            userAgent = "custom"
        })

        client.get(Path("v1", "test"))
                .execute()
        mockWebServer.takeRequest().let {
            assertThat(it.getHeader("User-Agent")).isEqualTo("custom")
            assertThat(it.method).isEqualTo("GET")
            assertThat(it.path).isEqualTo("/v1/test")
        }
    }

    //request queue

}
