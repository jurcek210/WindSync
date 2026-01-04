import sys
import numpy as np
from tensorflow.keras.models import load_model
from tensorflow.keras.preprocessing import image

#kako zagnati v vs code v power shell
#Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
#.\windsync_env\Scripts\Activate.ps1
#python predict.py test1.jpg

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
    print("Uporaba: python predict.py pot_do_slike.jpg")
    sys.exit(1)

img_path = sys.argv[1]

# ===============================
# NALOŽI SLIKO
# ===============================
img = image.load_img(img_path, target_size=IMG_SIZE)
x = image.img_to_array(img) / 255.0
x = np.expand_dims(x, axis=0)

# ===============================
# 1️⃣ MODEL: JE VETRNICA ALI NE
# ===============================
detector_model = load_model("wind_detector_model.h5")
detector_pred = detector_model.predict(x)[0]

detector_classes = ["not_wind", "wind"]
detector_idx = int(np.argmax(detector_pred))
detector_conf = float(detector_pred[detector_idx])

if detector_classes[detector_idx] != "wind" or detector_conf < THRESHOLD_WIND:
    print("Rezultat: NI VETRNICA ❌")
    print(f"Zanesljivost: {detector_conf * 100:.2f}%")
    sys.exit(0)

print("Rezultat: JE VETRNICA ✅")
print(f"Zanesljivost: {detector_conf * 100:.2f}%")

# ===============================
# 2️⃣ MODEL: ŠTEVILO KRAKOV
# ===============================
blades_model = load_model("wind_model.h5")
blades_pred = blades_model.predict(x)[0]

blades_classes = ["2_blades", "3_blades"]
blades_idx = int(np.argmax(blades_pred))
blades_conf = float(blades_pred[blades_idx])

if blades_conf < THRESHOLD_BLADES:
    print("Število krakov: NEZANESLJIVO")
    print(f"Zanesljivost: {blades_conf * 100:.2f}%")
else:
    print(f"Število krakov: {blades_classes[blades_idx]}")
    print(f"Zanesljivost: {blades_conf * 100:.2f}%")
