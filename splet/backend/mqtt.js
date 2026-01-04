import mqtt from "mqtt";
import fs from "fs";
import path from "path";

const client = mqtt.connect("mqtt://localhost:1883");

const imagesDir = path.join(process.cwd(), "images");

// če mapa ne obstaja → jo ustvari
if (!fs.existsSync(imagesDir)) {
    fs.mkdirSync(imagesDir);
}

client.on("connect", () => {
    console.log("✅ MQTT povezan");
    client.subscribe("windsync/image");
});

client.on("message", (topic, message) => {
    if (topic === "windsync/image") {
        console.log("📸 Slika prejeta");

        try {
            const base64 = message.toString();
            const buffer = Buffer.from(base64, "base64");

            const filename = `image_${Date.now()}.jpg`;
            const filePath = path.join(imagesDir, filename);

            fs.writeFileSync(filePath, buffer);

            console.log(`💾 Slika shranjena: ${filePath}`);
        } catch (err) {
            console.error("❌ Napaka pri shranjevanju slike:", err);
        }
    }
});
