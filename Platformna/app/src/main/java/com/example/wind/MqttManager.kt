package com.example.wind.mqtt

import org.eclipse.paho.client.mqttv3.*
import java.util.UUID
import kotlin.concurrent.thread

class MqttManager(
    private val serverUri: String,
    private val resultTopic: String,
    private val imageTopic: String,
    private val onResult: (String) -> Unit,
    private val onStatus: (String) -> Unit
) {

    private val clientId = UUID.randomUUID().toString()
    private val mqttClient = MqttClient(serverUri, clientId, null)

    @Volatile
    private var isConnected = false

    fun connect() {
        thread {
            try {
                mqttClient.setCallback(object : MqttCallback {

                    override fun connectionLost(cause: Throwable?) {
                        isConnected = false
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

                isConnected = true
                onStatus("✅ MQTT povezan")

            } catch (e: Exception) {
                isConnected = false
                onStatus("❌ MQTT napaka: ${e.message}")
            }
        }
    }

    fun publishImage(base64Image: String) {
        if (!isConnected) {
            onStatus("⏳ MQTT se še povezuje, poskusi znova...")
            return
        }

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
