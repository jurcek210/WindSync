package com.example.wind.mqtt

import android.content.Context
import org.eclipse.paho.client.mqttv3.*
import java.util.UUID
import kotlin.concurrent.thread

class MqttManager(
    private val context: Context,
    private val serverUri: String,
    private val resultTopic: String,
    private val imageTopic: String,
    private val onResult: (String) -> Unit,
    private val onStatus: (String) -> Unit
) {

    private val clientId = UUID.randomUUID().toString()
    private val mqttClient = MqttClient(serverUri, clientId, null)

    fun connect() {
        thread {
            try {
                mqttClient.setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        onStatus("❌ MQTT povezava izgubljena")
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        if (topic == resultTopic && message != null) {
                            onResult(message.toString())
                        }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                })

                val options = MqttConnectOptions().apply {
                    isCleanSession = false
                    isAutomaticReconnect = true
                    connectionTimeout = 10
                    keepAliveInterval = 20
                }

                mqttClient.connect(options)
                mqttClient.subscribe(resultTopic)

                onStatus("✅ MQTT povezan")

            } catch (e: Exception) {
                onStatus("❌ MQTT napaka: ${e.message}")
            }
        }
    }

    fun publishImage(base64Image: String) {
        thread {
            try {
                mqttClient.publish(
                    imageTopic,
                    MqttMessage(base64Image.toByteArray())
                )
                onStatus("📤 Slika poslana, čakam rezultat...")
            } catch (e: Exception) {
                onStatus("❌ Napaka pošiljanja: ${e.message}")
            }
        }
    }

    fun disconnect() {
        try {
            if (mqttClient.isConnected) {
                mqttClient.disconnect()
            }
        } catch (_: Exception) {}
    }
}
