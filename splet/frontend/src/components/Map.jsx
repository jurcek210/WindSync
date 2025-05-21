import { MapContainer, TileLayer, Marker, Popup, useMapEvents } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import { useEffect, useState } from "react";
import axios from "axios";
import L from "leaflet";
import Wind from "./Wind"; 


const MapDoubleClickHandler = ({ onDoubleClick }) => {
  useMapEvents({
    dblclick(e) {
      onDoubleClick(e.latlng);
    }
  });
  return null;
};

//funkcija za pridobivanje povprečne vrednosti vetra
const fetchAverageWindSpeed = async (lat, lng) => {
  const today = new Date();
  const oneYearAgo = new Date();
  oneYearAgo.setFullYear(today.getFullYear() - 1);

  const formatDate = (d) => d.toISOString().split("T")[0];
  const start = formatDate(oneYearAgo);
  const end = formatDate(today);

  const url = `https://archive-api.open-meteo.com/v1/archive?latitude=${lat}&longitude=${lng}&start_date=${start}&end_date=${end}&daily=windspeed_10m_mean&timezone=auto`;

  try {
    const res = await axios.get(url);
    const speeds = res.data?.daily?.windspeed_10m_mean || [];
    if (speeds.length === 0) return null;

    const sum = speeds.reduce((acc, val) => acc + val, 0);
    const average = (sum / speeds.length).toFixed(2);
    
    console.log(`📈 Povprečna hitrost vetra na (${lat}, ${lng}) med ${start} in ${end}: ${average} m/s`);
    
    return average;
  } catch (err) {
    console.error("❌ Napaka pri pridobivanju vetra:", err);
    return null;
  }
};




const Map = ({ loggedIn }) => {
  const [windmills, setWindmills] = useState([]);
  const [clickedLatLng, setClickedLatLng] = useState(null);
  const [showSidebar, setShowSidebar] = useState(false);
  const [name, setName] = useState("");
  const [windSpeed, setWindSpeed] = useState("");

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
        position: "relative",
        width: "100vw",
        height: "100vh",
      }}
    >
      <MapContainer
        center={[46.1512, 14.9955]}
        zoom={9}
        style={{ width: "100%", height: "100%" }}
        dragging={true}
        doubleClickZoom={false}
      >
        <MapDoubleClickHandler
          onDoubleClick={async (latlng) => {
            if (!loggedIn) {
              alert("Prijavi se, da lahko dodaš veternico.");
              return;
            }

            setClickedLatLng(latlng);
            setShowSidebar(true);

            const avgWind = await fetchAverageWindSpeed(latlng.lat, latlng.lng);
            if (avgWind !== null) {
              setWindSpeed(avgWind); // Samodejno nastavi vrednost
            } else {
              alert("Napaka pri pridobivanju povprečne hitrosti vetra.");
            }
          }}
        />

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

        {clickedLatLng && (
          <Marker position={clickedLatLng}>
            <Popup>Lokacija za novo veternico</Popup>
          </Marker>
        )}
      </MapContainer>

      {/* Sidebar meni */}
      {showSidebar && (
  <div
    style={{
      position: "absolute",
      top: 0,
      right: 20,
      width: "320px",
      height: "100%",
      backgroundColor: "white",
      borderLeft: "1px solid #ccc",
      boxShadow: "-4px 0 12px rgba(0,0,0,0.1)",
      padding: "20px",
      zIndex: 1000,
      overflowY: "auto", // priporočam, da dodaš scroll če bo višina prevelika
    }}
  >
    {/* ...tvoji inputi in gumbi ... */}
    
    <h2>Dodaj veternico</h2>
    <input
      placeholder="Ime"
      value={name}
      onChange={(e) => setName(e.target.value)}
      style={{ width: "100%", marginBottom: "8px" }}
    />
    <input
      type="number"
      placeholder="Hitrost vetra"
      value={windSpeed}
      onChange={(e) => setWindSpeed(e.target.value)}
      style={{ width: "100%", marginBottom: "8px" }}
    />
    <div style={{ display: "flex", justifyContent: "flex-end", gap: "8px" }}>
      <button onClick={() => setShowSidebar(false)}>Prekliči</button>
      <button
        onClick={async () => {
          // tvoj klic API za shrani
        }}
        style={{
          backgroundColor: "#4caf50",
          color: "white",
          padding: "4px 8px",
        }}
      >
        Shrani
      </button>
    </div>

    {/* TU DODAJ Wind komponento */}
    {clickedLatLng && <Wind lat={clickedLatLng.lat} lng={clickedLatLng.lng} />}
  </div>
)}

    </div>
  );
};

export default Map;
