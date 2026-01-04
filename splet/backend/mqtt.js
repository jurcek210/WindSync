import mqtt from "mqtt";
import fs from "fs";
import path from "path";
import { execFile } from "child_process";

// MQTT
const client = mqtt.connect("mqtt://localhost:1883");

// root projekta (2 nivoja gor)
const ROOT_DIR = path.resolve(process.cwd(), "../../");

// mapa za slike
const imagesDir = path.join(ROOT_DIR, "images");

// ustvari images mapo
if (!fs.existsSync(imagesDir)) {
    fs.mkdirSync(imagesDir);
}

client.on("connect", () => {
    console.log("✅ MQTT povezan");
    client.subscribe("windsync/image");
});

client.on("message", (topic, message) => {
    if (topic !== "windsync/image") return;

    console.log("📸 Slika prejeta");

    const filename = `image_${Date.now()}.jpg`;
    const filePath = path.join(imagesDir, filename);

    try {
        fs.writeFileSync(filePath, Buffer.from(message.toString(), "base64"));
        console.log(`💾 Slika shranjena: ${filePath}`);
    } catch (err) {
        console.error("❌ Napaka pri shranjevanju slike:", err);
        return;
    }

    // poti do python okolja in skripte
    const pythonPath = path.join(
        ROOT_DIR,
        "vid",
        "windsync_env",
        "Scripts",
        "python.exe"
    );

    const predictScript = path.join(
        ROOT_DIR,
        "vid",
        "predict.py"
    );

    execFile(
        pythonPath,
        [predictScript, filePath],
        (error, stdout, stderr) => {
            if (error) {
                console.error("❌ Python error:", error);
                return;
            }

            if (stderr) {
                console.error("⚠️ Python stderr:", stderr);
            }

            try {
                const result = JSON.parse(stdout);
                console.log("🧠 REZULTAT:", result);

                client.publish(
                    "windsync/result",
                    JSON.stringify(result)
                );

            } catch (err) {
                console.error("❌ JSON parse error:");
                console.error(stdout);
            }
        }
    );
});
