import { MapContainer, TileLayer, Marker, Popup, useMapEvents } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import { useEffect, useState } from "react";
import axios from "axios";
import L from "leaflet";
import Wind from "./Wind"; 
import { GeoJSON } from "react-leaflet";
import DayliEnergy from "./DayliEnergy";

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
const getRegionColor = (speed) => {
  if (speed < 3.0) return "#ffffff"; // Bela za zelo šibek veter

  // Odtenki zelene za šibek do zmeren veter
  const greenShades = [
    "#e0f2f1", "#b2dfdb", "#80cbc4", "#4db6ac",
    "#26a69a", "#009688", "#00897b", "#00796b"
  ];

  // Odtenki rdeče za močan veter
  const redShades = [
    "#ffccbc", "#ffab91", "#ff8a65", "#ff7043",
    "#f4511e", "#e64a19", "#d84315", "#bf360c"
  ];

  if (speed < 6.0) {
    // Linearno poiščemo ustrezno zeleno barvo
    const idx = Math.floor((speed - 3.0) / (6.0 - 3.0) * greenShades.length);
    return greenShades[Math.min(idx, greenShades.length - 1)];
  } else if (speed >= 6.0 && speed < 9.0) {
    // Linearno poiščemo ustrezno rdečo barvo
    const idx = Math.floor((speed - 6.0) / (9.0 - 6.0) * redShades.length);
    return redShades[Math.min(idx, redShades.length - 1)];
  }

  // Najtemnejša rdeča za 9.0+
  return "#bf360c";
};

const Legend = () => {
  const grades = [];
  for (let i = 3.0; i <= 9.0 + 0.001; i += 0.2) {
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
  const [showRegions, setShowRegions] = useState(true);
  const [turbineCategory, setTurbineCategory] = useState("domaca");
  const [selectedSubOption, setSelectedSubOption] = useState("");
  const [currentUser, setCurrentUser] = useState(null);
 
  const windmillIcon = new L.Icon({
    iconUrl: "/photos/windmill.png",
    iconSize: [32, 32],
    iconAnchor: [16, 32],
    popupAnchor: [0, -32],
  });

  const myWindmillIcon = new L.Icon({
    iconUrl: "/photos/my_windmill.png",
    iconSize: [32, 32],
    iconAnchor: [16, 32],
    popupAnchor: [0, -32],
  });

useEffect(() => {
  const fetchData = async () => {
    try {
      const resGeo = await fetch("/gadm41_SVN_2.json");
      const geoData = await resGeo.json();
      setRegionData(geoData);

      const resSpeeds = await fetch("/municipalityWindSpeeds.json");
      const speedsData = await resSpeeds.json();

      setRegionWindSpeeds(speedsData);


    } catch (err) {
      console.error("Napaka pri nalaganju podatkov:", err);
    }
  };

  fetchData();
}, []);

  useEffect(() => {
  const fetchUser = async () => {
    const token = localStorage.getItem("token");
    if (!token) return;

    try {
      const res = await axios.get("http://localhost:3001/api/me", {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      setCurrentUser(res.data.user);
    } catch (err) {
      console.error("Napaka pri pridobivanju uporabnika", err);
    }
  };

  fetchUser();
  }, []);
  
  const deleteWindmill = async (id) => {
  const confirmed = window.confirm("Ali res želiš izbrisati veternico?");
  if (!confirmed) return;

  try {
    const res = await axios.delete(`http://localhost:3001/api/windmills/${id}`, {
      headers: {
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      }
    });

    if (res.status === 200) {
      setWindmills((prev) => prev.filter((w) => w._id !== id));
    } else {
      alert(res.data.message || "Napaka pri brisanju veternice");
    }

  } catch (err) {
    console.error("Napaka pri axios.delete:", err);
    alert("Napaka pri pošiljanju zahteve");
  }
};
  
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
        {showWindmills && windmills.map((wm) => {
          const isMine = currentUser && wm.owner === currentUser._id;

          return (
            <Marker
              key={wm._id}
              position={[wm.location.coordinates[1], wm.location.coordinates[0]]}
              icon={isMine ? myWindmillIcon : windmillIcon}
            >
              <Popup>
                <strong>{wm.name}</strong><br />
                Status: {wm.status ? "Aktivna" : "Neaktivna"}<br />
                Hitrost: {wm.windSpeed ?? "ni podatka"} m/s
                <DayliEnergy windmill={wm} />

                {isMine && (
                  <div style={{ marginTop: "8px" }}>
                    <button
                      onClick={() => deleteWindmill(wm._id)}
                      style={{
                        backgroundColor: "#ef4444",
                        color: "white",
                        border: "none",
                        padding: "6px 12px",
                        borderRadius: "6px",
                        cursor: "pointer",
                        fontSize: "14px"
                      }}
                    >
                      Izbriši
                    </button>
                  </div>
                )}
              </Popup>
            </Marker>
          );
        })}
        {regionData && showRegions && (
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
              layer.bindPopup(`<strong>${name}</strong><br/>`);
            }}
          />
        )}


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
      width: "420px",
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
    <p style={{ marginBottom: "8px" }}>
      <strong>Povprečna hitrost vetra:</strong>{" "}
      {windSpeed ? `${windSpeed} m/s` : "Ni podatka"}
    </p>

    {/* Izbira glavne kategorije */}
    <div style={{ marginBottom: "12px" }}>
      <label>
        <input
          type="radio"
          name="category"
          value="domaca"
          checked={turbineCategory === "domaca"}
          onChange={() => {
            setTurbineCategory("domaca");
            setSelectedSubOption(""); // reset podizbire
          }}
        />
        {" "}Domača raba
      </label>
      <br />
      <label>
        <input
          type="radio"
          name="category"
          value="vecja"
          checked={turbineCategory === "vecja"}
          onChange={() => {
            setTurbineCategory("vecja");
            setSelectedSubOption("");
          }}
        />
        {" "}Večja raba
      </label>
    </div>

    {/* Podmožnosti */}
{turbineCategory === "domaca" && (
  <div style={{ marginLeft: "16px", marginBottom: "12px" }}>
    <label>
      <input
        type="radio"
        name="subOption"
        value="tipA"
        checked={selectedSubOption === "tipA"}
        onChange={() => setSelectedSubOption("tipA")}
      />{" "}
      <a
        href={`/WindMils/1/${windSpeed}/${clickedLatLng.lat}/${clickedLatLng.lng} `}
        target="_blank"
        rel="noopener noreferrer"
        style={{ marginRight: "8px" }}
      >
        Poglej več
      </a>
       Bornay 1200 4000$ | 1200 W
    </label>
    <br />
    <label>
      <input
        type="radio"
        name="subOption"
        value="tipB"
        checked={selectedSubOption === "tipB"}
        onChange={() => setSelectedSubOption("tipB")}
      />{" "}
      <a
        href={`/WindMils/2/${windSpeed}/${clickedLatLng.lat}/${clickedLatLng.lng} `}
        target="_blank"
        rel="noopener noreferrer"
        style={{ marginRight: "8px" }}
      >
        Poglej več
      </a>
      Missouri Freedom II Wind Turbine 200$ | 2.000 W
    </label>
    <br />
    <label>
      <input
        type="radio"
        name="subOption"
        value="tipC"
        checked={selectedSubOption === "tipC"}
        onChange={() => setSelectedSubOption("tipC")}
      />{" "}
      <a
        href={`/WindMils/3/${windSpeed}/${clickedLatLng.lat}/${clickedLatLng.lng} `}
        target="_blank"
        rel="noopener noreferrer"
        style={{ marginRight: "8px" }}
      >
        Poglej več
      </a>
      Tumo-Int 1200W Horizontal Wind Turbine 1500$ | 1220 W
    </label>
  </div>
)}


   {turbineCategory === "vecja" && (
  <div style={{ marginLeft: "16px", marginBottom: "12px" }}>
    <label>
      <input
        type="radio"
        name="subOption"
        value="tipY"
        checked={selectedSubOption === "tipY"}
        onChange={() => setSelectedSubOption("tipY")}
      />{" "}
      <a
        href={`/WindMilsBig/1/${windSpeed}/${clickedLatLng.lat}/${clickedLatLng.lng}`}
        target="_blank"
        rel="noopener noreferrer"
        style={{ marginRight: "8px" }}
      >
        Poglej več
      </a>
       Ampair 6000 Wind Turbine 20000$ | 6 kW
    </label>
    <br />
    <label>
      <input
        type="radio"
        name="subOption"
        value="tipZ"
        checked={selectedSubOption === "tipZ"}
        onChange={() => setSelectedSubOption("tipZ")}
      />{" "}
      <a
        href={`/WindMilsBig/2/${windSpeed}/${clickedLatLng.lat}/${clickedLatLng.lng}`}
        target="_blank"
        rel="noopener noreferrer"
        style={{ marginRight: "8px" }}
      >
        Poglej več
      </a>
      Eocycle EOX S-15 40000$ | 15 kW
    </label>
    <br />
    <label>
      <input
        type="radio"
        name="subOption"
        value="tipW"
        checked={selectedSubOption === "tipW"}
        onChange={() => setSelectedSubOption("tipW")}
      />{" "}
      <a
        href={`/WindMilsBig/3/${windSpeed}/${clickedLatLng.lat}/${clickedLatLng.lng}`}
        target="_blank"
        rel="noopener noreferrer"
        style={{ marginRight: "8px" }}
      >
        Poglej več
      </a>
      Kestrel e400i 12000$ | 3,5 kW
    </label>
  </div>
)}
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
                category: turbineCategory,
                type: selectedSubOption,
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
            setTurbineCategory("domaca");
            setSelectedSubOption("");

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
          top: "0px",
          right: "90px",
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
      <button
      onClick={() => setShowRegions((prev) => !prev)}
      style={{
        position: "absolute",
        top: "0px",
        right: "240px",
        backgroundColor: "#9c27b0",
        color: "white",
        border: "none",
        padding: "10px 20px",
        fontSize: "16px",
        borderRadius: "4px",
        cursor: "pointer",
        zIndex: 1001,
      }}
      >
        {showRegions ? "Skrij mapo" : "Pokaži mapo"}
      </button>                
      {showRegions && <Legend />}
        

    </div>
  );
};

export default Map;
