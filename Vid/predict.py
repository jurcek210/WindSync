import sys
import numpy as np
from tensorflow.keras.models import load_model
from tensorflow.keras.preprocessing import image

# preveri argument
if len(sys.argv) < 2:
    print("Uporaba: python predict.py pot_do_slike.jpg")
    sys.exit(1)

img_path = sys.argv[1]

# naloži model
model = load_model("wind_model.h5")

# naloži in pripravi sliko
img = image.load_img(img_path, target_size=(224, 224))
x = image.img_to_array(img) / 255.0
x = np.expand_dims(x, axis=0)

# napoved
pred = model.predict(x)[0]

classes = ["2_blades", "3_blades"]
idx = int(np.argmax(pred))
confidence = float(pred[idx])

# prag zaupanja
THRESHOLD = 0.5 # 60 %

if confidence < THRESHOLD:
    print("Rezultat: NI VETRNICA")
    print(f"Zanesljivost: {confidence * 100:.2f}%")
else:
    print(f"Rezultat: {classes[idx]}")
    print(f"Zanesljivost: {confidence * 100:.2f}%")
