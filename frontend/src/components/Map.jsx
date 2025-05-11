import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";
import "leaflet/dist/leaflet.css";

const Map = () => {
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
          width: "1600px",
          height: "1000px",
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
          <Marker position={[46.05, 14.5]}>
            <Popup>Prva veternica 🌬️</Popup>
          </Marker>
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
