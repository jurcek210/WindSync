import sys
import os
import json
import numpy as np
from tensorflow.keras.models import load_model
from tensorflow.keras.preprocessing import image

# ===============================
# OSNOVNE POTI (KLJUČNA POPRAVA)
# ===============================
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

DETECTOR_MODEL_PATH = os.path.join(BASE_DIR, "wind_detector_model.h5")
BLADES_MODEL_PATH = os.path.join(BASE_DIR, "wind_model.h5")

# ===============================
# KONFIGURACIJA
# ===============================
IMG_SIZE = (224, 224)
THRESHOLD_WIND = 0.6
THRESHOLD_BLADES = 0.6

# ===============================
# PREVERI ARGUMENT
# ===============================
if len(sys.argv) < 2:
    print(json.dumps({
        "error": "Manjka pot do slike"
    }))
    sys.exit(1)

img_path = sys.argv[1]

# ===============================
# PREVERI ALI SLIKA OBSTAJA
# ===============================
if not os.path.exists(img_path):
    print(json.dumps({
        "error": "Slika ne obstaja",
        "path": img_path
    }))
    sys.exit(1)

# ===============================
# NALOŽI SLIKO
# ===============================
try:
    img = image.load_img(img_path, target_size=IMG_SIZE)
    x = image.img_to_array(img) / 255.0
    x = np.expand_dims(x, axis=0)
except Exception as e:
    print(json.dumps({
        "error": "Napaka pri nalaganju slike",
        "details": str(e)
    }))
    sys.exit(1)

# ===============================
# 1️⃣ MODEL: DETEKCIJA VETRNICE
# ===============================
try:
    detector_model = load_model(DETECTOR_MODEL_PATH)
except Exception as e:
    print(json.dumps({
        "error": "Napaka pri nalaganju detector modela",
        "details": str(e)
    }))
    sys.exit(1)

detector_pred = detector_model.predict(x, verbose=0)[0]

detector_classes = ["not_wind", "wind"]
detector_idx = int(np.argmax(detector_pred))
detector_conf = float(detector_pred[detector_idx])

# ===============================
# ČE NI VETRNICA
# ===============================
if detector_classes[detector_idx] != "wind" or detector_conf < THRESHOLD_WIND:
    print(json.dumps({
        "is_wind_turbine": False,
        "confidence": detector_conf
    }))
    sys.exit(0)

# ===============================
# 2️⃣ MODEL: ŠTEVILO KRAKOV
# ===============================
try:
    blades_model = load_model(BLADES_MODEL_PATH)
except Exception as e:
    print(json.dumps({
        "error": "Napaka pri nalaganju blades modela",
        "details": str(e)
    }))
    sys.exit(1)

blades_pred = blades_model.predict(x, verbose=0)[0]

blades_classes = ["2_blades", "3_blades"]
blades_idx = int(np.argmax(blades_pred))
blades_conf = float(blades_pred[blades_idx])

# ===============================
# KONČNI REZULTAT
# ===============================
result = {
    "is_wind_turbine": True,
    "confidence": detector_conf,
    "blades": blades_classes[blades_idx] if blades_conf >= THRESHOLD_BLADES else "unknown",
    "blades_confidence": blades_conf
}

print(json.dumps(result))
