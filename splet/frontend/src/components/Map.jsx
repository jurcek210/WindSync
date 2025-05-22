import { MapContainer, TileLayer, Marker, Popup, useMapEvents } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import { useEffect, useState } from "react";
import axios from "axios";
import L from "leaflet";
import Wind from "./Wind"; 
import { GeoJSON } from "react-leaflet";


const MapDoubleClickHandler = ({ onDoubleClick }) => {
  useMapEvents({
    dblclick(e) {
      onDoubleClick(e.latlng);
    }
  });
  return null;
};

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

    return parseFloat(average);
  } catch (err) {
    console.error("Napaka pri pridobivanju vetra:", err);
    return null;
  }
};


const getFeatureCenter = (feature) => {
  const coords = feature.geometry.coordinates.flat(2); // flatten to [lng, lat]
  const lats = coords.map(coord => coord[1]);
  const lngs = coords.map(coord => coord[0]);
  const avgLat = lats.reduce((a, b) => a + b, 0) / lats.length;
  const avgLng = lngs.reduce((a, b) => a + b, 0) / lngs.length;
  return [avgLng, avgLat];
};


const getRegionColor = (speed) => {
  if (speed >= 6.0) return "#004529";
  if (speed >= 5.8) return "#006837";
  if (speed >= 5.6) return "#238443";
  if (speed >= 5.4) return "#41ab5d";
  if (speed >= 5.2) return "#78c679";
  if (speed >= 5.0) return "#addd8e";
  if (speed >= 4.8) return "#d9f0a3";
  if (speed >= 4.6) return "#e7f6c0";
  if (speed >= 4.4) return "#f2fadc";
  if (speed >= 4.2) return "#ffffe5";
  if (speed >= 4.0) return "#fff7bc";
  if (speed >= 3.8) return "#fee391";
  if (speed >= 3.6) return "#fec44f";
  if (speed >= 3.4) return "#fe9929";
  if (speed >= 3.2) return "#ec7014";
  if (speed >= 3.0) return "#cc4c02";
  if (speed >= 2.8) return "#d9d9d9";
  if (speed >= 2.6) return "#bdbdbd";
  if (speed >= 2.4) return "#969696";
  if (speed >= 2.2) return "#737373";
  if (speed >= 2.0) return "#525252";
  if (speed >= 1.8) return "#252525";
  if (speed >= 1.6) return "#f7f7f7";
  if (speed >= 1.4) return "#cccccc";
  if (speed >= 1.2) return "#969696";
  if (speed >= 1.0) return "#636363";
  return "#ffffcc"; // pod 1.0
};

const Legend = () => {
  // Intervali od 1.0 do 6.0 s korakom 0.2
  const grades = [];
  for (let i = 1.0; i <= 6.0 + 0.001; i += 0.2) {
    grades.push(Number(i.toFixed(1)));
  }

  return (
    <div
      style={{
        position: "absolute",
        top: "20px",
        left: "20px",
        backgroundColor: "white",
        padding: "10px",
        borderRadius: "6px",
        boxShadow: "0 0 10px rgba(0,0,0,0.3)",
        fontSize: "12px",
        maxWidth: "180px",
        zIndex: 1002,
      }}
    >
      <strong>Legenda hitrosti vetra (m/s)</strong>
      <ul style={{ listStyle: "none", padding: 0, margin: "8px 0 0 0" }}>
        {grades.map((grade) => (
          <li
            key={grade}
            style={{
              display: "flex",
              alignItems: "center",
              marginBottom: "4px",
            }}
          >
            <span
              style={{
                display: "inline-block",
                width: "20px",
                height: "14px",
                backgroundColor: getRegionColor(grade),
                marginRight: "8px",
                border: "1px solid #999",
              }}
            ></span>
            <span>{grade.toFixed(1)}+</span>
          </li>
        ))}
      </ul>
    </div>
  );
};

const Map = ({ loggedIn }) => {
  const [windmills, setWindmills] = useState([]);
  const [clickedLatLng, setClickedLatLng] = useState(null);
  const [showSidebar, setShowSidebar] = useState(false);
  const [name, setName] = useState("");
  const [windSpeed, setWindSpeed] = useState("");
  const [showWindmills, setShowWindmills] = useState(true); 
  const [regionData, setRegionData] = useState(null);
  const [regionWindSpeeds, setRegionWindSpeeds] = useState({});



  // Nova stanje za mrežo vetra
  const [windGridData, setWindGridData] = useState([]);
  const [loadingGrid, setLoadingGrid] = useState(false);

  const windmillIcon = new L.Icon({
    iconUrl: "/photos/windmill.png",
    iconSize: [32, 32],
    iconAnchor: [16, 32],
    popupAnchor: [0, -32],
  });

useEffect(() => {
  const fetchData = async () => {
    try {
      // Naloži geojson, da dobimo geometrijo za prikaz občin/regij
      const resGeo = await fetch("/gadm41_SVN_2.json");
      const geoData = await resGeo.json();
      setRegionData(geoData);

      // Naloži že obdelane povprečne hitrosti vetra za občine/regije
      const resSpeeds = await fetch("/municipalityWindSpeeds.json");
      const speedsData = await resSpeeds.json();

      setRegionWindSpeeds(speedsData);

      console.log("Povprečne hitrosti vetra za občine:");
      Object.entries(speedsData).forEach(([name, speed]) => {
        console.log(`${name}: ${speed} m/s`);
      });
    } catch (err) {
      console.error("Napaka pri nalaganju podatkov:", err);
    }
  };

  fetchData();
}, []);
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
        minZoom={8}
        maxZoom={13}
        maxBounds={[[44.8, 12.9], [47.5, 17.0]]}
        maxBoundsViscosity={1.0}
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
              setWindSpeed(avgWind);
            } else {
              alert("Napaka pri pridobivanju povprečne hitrosti vetra.");
            }
          }}
        />

        <TileLayer
          attribution='&copy; <a href="https://carto.com/">CARTO</a>'
          url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"
        />

        {/* Prikaz veternic */}
        {showWindmills && windmills.map((wm) => (
          <Marker
            key={wm._id}
            position={[wm.location.coordinates[1], wm.location.coordinates[0]]}
            icon={windmillIcon}
          >
          <Popup>
            <strong>{wm.name}</strong><br />
            Status: {wm.status ? "Aktivna" : "Neaktivna"}<br />
            Hitrost: {wm.windSpeed ?? "ni podatka"} m/s
          </Popup>
          </Marker>
        ))}
{regionData && (
  <GeoJSON
    data={regionData}
    style={(feature) => {
      const speed = regionWindSpeeds[feature.properties.NAME_2]; // prilagodi glede na ključe v tvojem JSON-u
      return {
        fillColor: speed ? getRegionColor(speed) : "#ccc", // siva za manjkajoče podatke
        fillOpacity: 0.6,
        color: "#444",
        weight: 1,
      };
    }}
    onEachFeature={(feature, layer) => {
      const name = feature.properties.NAME_2;
      const speed = regionWindSpeeds[name];
      layer.bindPopup(`<strong>${name}</strong><br/>Hitrost vetra: ${speed ?? "ni podatka"} m/s`);
    }}
  />
)}

        {/* Prikaz mreže vetra */}
        {windGridData.map(({ lat, lng, speed }, idx) => (
          <Marker
            key={`wind-grid-${idx}`}
            position={[lat, lng]}
            icon={windIcon(speed)}
          >
            <Popup>
              Hitrost vetra: {speed} m/s
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
            overflowY: "auto",
          }}
        >
          <button
            onClick={() => setShowSidebar(false)}
            style={{
              position: "absolute",
              top: "10px",
              right: "10px",
              background: "none",
              border: "none",
              fontSize: "24px",
              fontWeight: "bold",
              cursor: "pointer",
              color: "#333",
            }}
            aria-label="Zapri meni"
          >
            ×
          </button>

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
                try {
                  await axios.post(
                    "/api/windmills",
                    {
                      name,
                      windSpeed: parseFloat(windSpeed),
                      location: {
                        type: "Point",
                        coordinates: [clickedLatLng.lng, clickedLatLng.lat],
                      },
                    },
                    {
                      headers: {
                        Authorization: `Bearer ${localStorage.getItem("token")}`,
                      },
                    }
                  );

                  setShowSidebar(false);
                  setClickedLatLng(null);
                  setName("");
                  setWindSpeed("");
                  const { data } = await axios.get("/api/windmills");
                  setWindmills(data);
                } catch (err) {
                  console.error("Napaka pri shranjevanju veternice", err);
                  alert("Napaka pri shranjevanju veternice.");
                }
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

          {clickedLatLng && <Wind lat={clickedLatLng.lat} lng={clickedLatLng.lng} />}
        </div>
      )}

      {/* Gumb za preklop prikaza veternic */}
      <button
        onClick={() => setShowWindmills((prev) => !prev)}
        style={{
          position: "absolute",
          top: "20px",
          right: "20px",
          backgroundColor: "#4caf50",
          color: "white",
          border: "none",
          padding: "10px 20px",
          fontSize: "16px",
          borderRadius: "4px",
          cursor: "pointer",
          zIndex: 1001,
        }}
      >
        {showWindmills ? "Skrij veternice" : "Pokaži veternice"}
      </button>

            <Legend />


    </div>
  );
};

export default Map;
