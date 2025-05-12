import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import { useEffect, useState } from "react";
import axios from "axios";
import L from "leaflet";

const Map = () => {
  const [windmills, setWindmills] = useState([]);

  const windmillIcon = new L.Icon({
    iconUrl: "/photos/windmill.png",
    iconSize: [32, 32],
    iconAnchor: [16, 32],
    popupAnchor: [0, -32],
  });

  useEffect(() => {
    const fetchWindmills = async () => {
      try {
        const { data } = await axios.get("/api/windmills");
        console.log("Vsebina podatkov:", data);
        setWindmills(data);
      } catch (err) {
        console.error("Napaka pri pridobivanju veternic:", err);
      }
    };
    fetchWindmills();
  }, []);

  return (
    <div
      style={{
        display: "flex",
        justifyContent: "center",
        gap: "24px",
        width: "100%",
        maxWidth: "66%",
        margin: "40px",
      }}
    >
      <div
        style={{
          width: "1200px",
          height: "800px",
          borderRadius: "16px",
          overflow: "hidden",
          boxShadow: "0 8px 24px rgba(0,0,0,0.08)",
          border: "1px solid var(--border)",
        }}
      >
        <MapContainer
          center={[46.1512, 14.9955]}
          zoom={9}
          style={{ width: "100%", height: "100%" }}
          scrollWheelZoom={false}
          dragging={true}
        >
          <TileLayer
            attribution='&copy; <a href="https://carto.com/">CARTO</a>'
            url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"
          />
          {windmills.map((wm) => (
            <Marker
              key={wm._id}
              position={[
                wm.location.coordinates[1],
                wm.location.coordinates[0],
              ]}
              icon={windmillIcon}
            >
              <Popup>
                <strong>{wm.name}</strong>
                <br />
                Hitrost vetra: {wm.windSpeed ?? "ni podatka"} m/s
                <br />
                Status: {wm.status ? " Aktivna" : " Neaktivna"}
              </Popup>
            </Marker>
          ))}
        </MapContainer>
      </div>

      <div
        style={{
          flex: 1,
          height: "800px",
        }}
      ></div>
    </div>
  );
};

export default Map;
