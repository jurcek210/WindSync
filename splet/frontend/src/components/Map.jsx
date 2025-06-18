import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
  useMapEvents,
} from "react-leaflet";
import "leaflet/dist/leaflet.css";
import { useEffect, useState } from "react";
import axios from "axios";
import L from "leaflet";
import Wind from "./Wind";
import { GeoJSON } from "react-leaflet";
import DayliEnergy from "./DayliEnergy";
import NearbyWindmills from "./NearbyWindmills.jsx";
import WindmillDetailsSidebar from "./WindmillDetailsSidebar";
import { io } from "socket.io-client";


const MapDoubleClickHandler = ({ onDoubleClick }) => {
  useMapEvents({
    dblclick(e) {
      onDoubleClick(e.latlng);
    },
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
const hexToRgba = (hex, alpha = 0.4) => {
  hex = hex.replace("#", "");

  const bigint = parseInt(hex, 16);
  const r = (bigint >> 16) & 255;
  const g = (bigint >> 8) & 255;
  const b = bigint & 255;

  return `rgba(${r}, ${g}, ${b}, ${alpha})`;
};

const getRegionColor = (speed) => {
  if (speed < 3.0) return hexToRgba("#ffffff", 0.4);

  const blueShades = [
    "#e3f2fd", // svetlo modra
    "#bbdefb",
    "#90caf9",
    "#64b5f6",
    "#42a5f5",
    "#2196f3",
    "#1e88e5",
    "#1976d2",
    "#1565c0",
    "#0d47a1", // temno modra
  ];

  const idx = Math.floor(
    Math.min(
      ((speed - 3.0) / (12.0 - 3.0)) * blueShades.length,
      blueShades.length - 1
    )
  );
  return hexToRgba(blueShades[idx], 0.4);
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
  const [selectedWindmillDetails, setSelectedWindmillDetails] = useState(null);

  const windmillIcon = new L.Icon({
    iconUrl: "/photos/windmill.png",
    iconSize: [32, 32],
    iconAnchor: [16, 32],
    popupAnchor: [0, -32],
  });

  const myWindmillIcon = new L.Icon({
    iconUrl: "/photos/windmillZelen.png",
    iconSize: [32, 32],
    iconAnchor: [16, 32],
    popupAnchor: [0, -32],
  });

  const redWindmillIcon = new L.Icon({
    iconUrl: "/photos/windmillRed.png",
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
      const res = await axios.delete(
        `http://localhost:3001/api/windmills/${id}`,
        {
          headers: {
            Authorization: `Bearer ${localStorage.getItem("token")}`,
          },
        }
      );

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

  useEffect(() => {
  const socket = io("http://localhost:3001");

  socket.on("connect", () => {
    console.log("Povezan na socket strežnik:", socket.id);
  });

  socket.on("windmillCreated", (windmill) => {
    console.log("Prejeta nova veternica:", windmill);
    setWindmills((prev) => [...prev, windmill]);
  });

  socket.on("windmillDeleted", ({ id }) => {
    console.log("Prejet izbris veternice:", id);
    setWindmills((prev) => prev.filter((w) => w._id !== id));
  });

  return () => {
    socket.disconnect();
  };
}, []);


  return (
    <div
      style={{
        position: "relative",
        width: "100vw",
        height: "900px",
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
        maxBounds={[
          [44.8, 12.9],
          [47.5, 17.0],
        ]}
        maxBoundsViscosity={1.0}
      >
        <MapDoubleClickHandler
          onDoubleClick={async (latlng) => {
            if (!loggedIn) {
              alert("Prijavi se, da lahko dodaš veternico.");
              return;
            }

            setClickedLatLng(latlng);
            setSelectedWindmillDetails(null);
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
        {showWindmills &&
          windmills.map((wm) => {
            const isMine = currentUser && wm.owner === currentUser._id;
            const isActive = wm.status;
            let iconToUse;
            if (!isActive) {
              iconToUse = redWindmillIcon;
            } else {
              iconToUse = isMine ? myWindmillIcon : windmillIcon;
            }

            return (
              <Marker
                key={wm._id}
                position={[
                  wm.location.coordinates[1],
                  wm.location.coordinates[0],
                ]}
                icon={iconToUse}
                eventHandlers={{
                  click: () => {
                    setSelectedWindmillDetails(wm);
                    setClickedLatLng(null);
                    setShowSidebar(true);
                  },
                }}
              ></Marker>
            );
          })}

        {regionData && showRegions && (
          <GeoJSON
            data={regionData}
            style={(feature) => {
              const speed = regionWindSpeeds[feature.properties.NAME_2];
              return {
                fillColor: speed ? getRegionColor(speed) : "#ccc",
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

      {showSidebar && selectedWindmillDetails ? (
        <WindmillDetailsSidebar
          windmill={selectedWindmillDetails}
          onClose={() => {
            setSelectedWindmillDetails(null);
            setShowSidebar(false);
          }}
        />
      ) : (
        showSidebar && (
          <div
            style={{
              position: "absolute",
              top: 0,
              right: 20,
              width: "420px",
              height: "850px",
              backgroundColor: "#f9faff",
              borderLeft: "1px solid #ddd",
              boxShadow: "-6px 0 20px rgba(0,0,0,0.1)",
              padding: "24px",
              zIndex: 1000,
              overflowY: "auto",
              color: "#222",
              fontFamily: "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif",
              borderRadius: "0 8px 8px 0",
              transition: "transform 0.3s ease",
            }}
          >
            <button
              onClick={() => setShowSidebar(false)}
              style={{
                position: "absolute",
                top: "16px",
                right: "16px",
                background: "none",
                border: "none",
                fontSize: "28px",
                fontWeight: "bold",
                cursor: "pointer",
                color: "#666",
                transition: "color 0.2s ease",
              }}
              aria-label="Zapri meni"
              onMouseEnter={(e) => (e.currentTarget.style.color = "#333")}
              onMouseLeave={(e) => (e.currentTarget.style.color = "#666")}
            >
              ×
            </button>

            <h2
              style={{
                marginBottom: "20px",
                color: "#0078d7",
                fontWeight: "700",
              }}
            >
              Dodaj veternico
            </h2>

            <input
              placeholder="Ime"
              value={name}
              onChange={(e) => setName(e.target.value)}
              style={{
                width: "100%",
                marginBottom: "12px",
                padding: "10px 12px",
                borderRadius: "6px",
                border: "1px solid #ccc",
                backgroundColor: "#fff",
                color: "#222",
                fontSize: "16px",
                outline: "none",
                transition: "border-color 0.3s ease",
              }}
              onFocus={(e) => (e.currentTarget.style.borderColor = "#0078d7")}
              onBlur={(e) => (e.currentTarget.style.borderColor = "#ccc")}
            />

            <p style={{ marginBottom: "20px", fontSize: "15px" }}>
              <strong style={{ color: "#0078d7" }}>
                Povprečna hitrost vetra:
              </strong>{" "}
              {windSpeed ? `${windSpeed} m/s` : "Ni podatka"}
            </p>
            <div style={{ marginBottom: "20px", display: "flex", gap: "16px" }}>
              {[
                { value: "domaca", label: "Domača raba" },
                { value: "vecja", label: "Večja raba" },
              ].map(({ value, label }) => {
                const isSelected = turbineCategory === value;
                return (
                  <button
                    key={value}
                    type="button"
                    onClick={() => {
                      setTurbineCategory(value);
                      setSelectedSubOption("");
                    }}
                    style={{
                      padding: "10px 20px",
                      fontSize: "16px",
                      borderRadius: "8px",
                      border: isSelected
                        ? "2px solid #0078d7"
                        : "1px solid #ccc",
                      backgroundColor: isSelected ? "#e6f0ff" : "#fff",
                      color: isSelected ? "#0078d7" : "#444",
                      cursor: "pointer",
                      transition: "all 0.3s ease",
                      boxShadow: isSelected
                        ? "0 0 8px rgba(0, 120, 215, 0.5)"
                        : "none",
                      userSelect: "none",
                    }}
                    aria-pressed={isSelected}
                  >
                    {label}
                  </button>
                );
              })}
            </div>

            {turbineCategory === "domaca" && (
              <div style={{ marginLeft: "16px", marginBottom: "24px" }}>
                {[
                  {
                    value: "tipA",
                    href: `/WindMils/1/${windSpeed}/${clickedLatLng.lat}/${clickedLatLng.lng}`,
                    label: "Bornay 1200 4000$ | 1200 W",
                  },
                  {
                    value: "tipB",
                    href: `/WindMils/2/${windSpeed}/${clickedLatLng.lat}/${clickedLatLng.lng}`,
                    label: "Missouri Freedom II Wind Turbine 200$ | 2.000 W",
                  },
                  {
                    value: "tipC",
                    href: `/WindMils/3/${windSpeed}/${clickedLatLng.lat}/${clickedLatLng.lng}`,
                    label:
                      "Tumo-Int 1200W Horizontal Wind Turbine 1500$ | 1220 W",
                  },
                ].map(({ value, href, label }) => {
                  const isSelected = selectedSubOption === value;
                  return (
                    <div
                      key={value}
                      style={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "space-between",
                        marginBottom: "12px",
                        cursor: "pointer",
                        fontSize: "14px",
                        border: isSelected
                          ? "2px solid #0078d7"
                          : "1px solid #ccc",
                        borderRadius: "8px",
                        padding: "10px 12px",
                        backgroundColor: isSelected ? "#e6f0ff" : "transparent",
                        color: isSelected ? "#0078d7" : "#222",
                        transition: "all 0.3s ease",
                        userSelect: "none",
                        gap: "12px",
                      }}
                      onClick={() => setSelectedSubOption(value)}
                      onMouseEnter={(e) => {
                        if (!isSelected)
                          e.currentTarget.style.backgroundColor = "#f0f7ff";
                      }}
                      onMouseLeave={(e) => {
                        if (!isSelected)
                          e.currentTarget.style.backgroundColor = "transparent";
                      }}
                      role="button"
                      tabIndex={0}
                      onKeyDown={(e) => {
                        if (e.key === "Enter" || e.key === " ") {
                          e.preventDefault();
                          setSelectedSubOption(value);
                        }
                      }}
                    >
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "12px",
                          flex: 1,
                        }}
                      >
                        <button
                          type="button"
                          aria-pressed={isSelected}
                          style={{
                            width: "20px",
                            height: "20px",
                            borderRadius: "50%",
                            border: isSelected
                              ? "6px solid #0078d7"
                              : "2px solid #ccc",
                            backgroundColor: isSelected
                              ? "#0078d7"
                              : "transparent",
                            cursor: "pointer",
                            padding: 0,
                            margin: 0,
                            flexShrink: 0,
                          }}
                          onClick={(e) => {
                            e.stopPropagation();
                            setSelectedSubOption(value);
                          }}
                        />
                        <span>{label}</span>
                      </div>

                      <a
                        href={href}
                        target="_blank"
                        rel="noopener noreferrer"
                        style={{
                          color: isSelected ? "#005ea1" : "#0078d7",
                          textDecoration: "none",
                          fontWeight: "600",
                          whiteSpace: "nowrap",
                          transition: "text-decoration 0.3s ease",
                        }}
                        onMouseEnter={(e) =>
                          (e.currentTarget.style.textDecoration = "underline")
                        }
                        onMouseLeave={(e) =>
                          (e.currentTarget.style.textDecoration = "none")
                        }
                        onClick={(e) => e.stopPropagation()}
                      >
                        Poglej več →
                      </a>
                    </div>
                  );
                })}
              </div>
            )}

            {turbineCategory === "vecja" && (
              <div style={{ marginLeft: "16px", marginBottom: "24px" }}>
                {[
                  {
                    value: "tipY",
                    href: `/WindMilsBig/1/${windSpeed}/${clickedLatLng.lat}/${clickedLatLng.lng}`,
                    label: "Ampair 6000 Wind Turbine 20000$ | 6 kW",
                  },
                  {
                    value: "tipZ",
                    href: `/WindMilsBig/2/${windSpeed}/${clickedLatLng.lat}/${clickedLatLng.lng}`,
                    label: "Eocycle EOX S-15 40000$ | 15 kW",
                  },
                  {
                    value: "tipW",
                    href: `/WindMilsBig/3/${windSpeed}/${clickedLatLng.lat}/${clickedLatLng.lng}`,
                    label: "Kestrel e400i 12000$ | 3,5 kW",
                  },
                ].map(({ value, href, label }) => {
                  const isSelected = selectedSubOption === value;
                  return (
                    <div
                      key={value}
                      style={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "space-between",
                        marginBottom: "12px",
                        cursor: "pointer",
                        fontSize: "14px",
                        border: isSelected
                          ? "2px solid #0078d7"
                          : "1px solid #ccc",
                        borderRadius: "8px",
                        padding: "10px 12px",
                        backgroundColor: isSelected ? "#e6f0ff" : "transparent",
                        color: isSelected ? "#0078d7" : "#222",
                        transition: "all 0.3s ease",
                        userSelect: "none",
                        gap: "12px",
                      }}
                      onClick={() => setSelectedSubOption(value)}
                      onMouseEnter={(e) => {
                        if (!isSelected)
                          e.currentTarget.style.backgroundColor = "#f0f7ff";
                      }}
                      onMouseLeave={(e) => {
                        if (!isSelected)
                          e.currentTarget.style.backgroundColor = "transparent";
                      }}
                      role="button"
                      tabIndex={0}
                      onKeyDown={(e) => {
                        if (e.key === "Enter" || e.key === " ") {
                          e.preventDefault();
                          setSelectedSubOption(value);
                        }
                      }}
                    >
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "12px",
                          flex: 1,
                        }}
                      >
                        <button
                          type="button"
                          aria-pressed={isSelected}
                          style={{
                            width: "20px",
                            height: "20px",
                            borderRadius: "50%",
                            border: isSelected
                              ? "6px solid #0078d7"
                              : "2px solid #ccc",
                            backgroundColor: isSelected
                              ? "#0078d7"
                              : "transparent",
                            cursor: "pointer",
                            padding: 0,
                            margin: 0,
                            flexShrink: 0,
                          }}
                          onClick={(e) => {
                            e.stopPropagation();
                            setSelectedSubOption(value);
                          }}
                        />
                        <span>{label}</span>
                      </div>

                      <a
                        href={href}
                        target="_blank"
                        rel="noopener noreferrer"
                        style={{
                          color: isSelected ? "#005ea1" : "#0078d7",
                          textDecoration: "none",
                          fontWeight: "600",
                          whiteSpace: "nowrap",
                          transition: "text-decoration 0.3s ease",
                        }}
                        onMouseEnter={(e) =>
                          (e.currentTarget.style.textDecoration = "underline")
                        }
                        onMouseLeave={(e) =>
                          (e.currentTarget.style.textDecoration = "none")
                        }
                        onClick={(e) => e.stopPropagation()}
                      >
                        Poglej več →
                      </a>
                    </div>
                  );
                })}
              </div>
            )}

            <div
              style={{
                display: "flex",
                justifyContent: "flex-end",
                gap: "12px",
              }}
            >
              <button
                onClick={() => setShowSidebar(false)}
                style={{
                  backgroundColor: "transparent",
                  border: "1.5px solid #0078d7",
                  borderRadius: "6px",
                  color: "#0078d7",
                  padding: "8px 16px",
                  fontWeight: "600",
                  cursor: "pointer",
                  transition: "background-color 0.3s ease, color 0.3s ease",
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.backgroundColor = "#0078d7";
                  e.currentTarget.style.color = "#fff";
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = "transparent";
                  e.currentTarget.style.color = "#0078d7";
                }}
              >
                Prekliči
              </button>
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
                        windMillType: selectedSubOption,
                      },
                      {
                        headers: {
                          Authorization: `Bearer ${localStorage.getItem(
                            "token"
                          )}`,
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
                  backgroundColor: "#0078d7",
                  border: "none",
                  borderRadius: "6px",
                  color: "white",
                  padding: "8px 16px",
                  fontWeight: "600",
                  cursor: "pointer",
                  transition: "background-color 0.3s ease",
                }}
                onMouseEnter={(e) =>
                  (e.currentTarget.style.backgroundColor = "#005ea1")
                }
                onMouseLeave={(e) =>
                  (e.currentTarget.style.backgroundColor = "#0078d7")
                }
              >
                Shrani
              </button>
            </div>

            {clickedLatLng && (
              <Wind lat={clickedLatLng.lat} lng={clickedLatLng.lng} />
            )}
          </div>
        )
      )}

      {/* Gumb za preklop prikaza veternic */}
      <div
        style={{
          position: "absolute",
          top: "10px",
          right: showSidebar ? "470px" : "20px", // potisne levo, če je sidebar
          transition: "right 0.3s ease",
          display: "flex",
          gap: "24px",
          zIndex: 1001,
        }}
      >
        <div
          onClick={() => setShowWindmills((prev) => !prev)}
          style={{
            display: "inline-block",
            cursor: "pointer",
            position: "relative",
            width: "80px",
            height: "80px",
            borderRadius: "10px",
            boxShadow: showWindmills
              ? "0 0 10px rgba(51, 153, 255, 0.6)"
              : "0 0 6px rgba(51, 153, 255, 0.4)",
            transition: "all 0.3s ease",
            transform: showWindmills ? "scale(1.04)" : "scale(1)",
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.boxShadow =
              "0 0 10px rgba(51, 153, 255, 0.6)";
            e.currentTarget.style.transform = "scale(1.04)";
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.boxShadow = showWindmills
              ? "0 0 10px rgba(51, 153, 255, 0.6)"
              : "0 0 6px rgba(51, 153, 255, 0.4)";
            e.currentTarget.style.transform = "scale(1)";
          }}
        >
          <img
            src="../../public/photos/windmill.png"
            alt="Vetrnica"
            style={{
              width: "100%",
              height: "100%",
              borderRadius: "10px",
              opacity: showWindmills ? 0.5 : 1,
              filter: showWindmills ? "grayscale(100%)" : "none",
              transition: "all 0.3s ease",
            }}
          />
          {showWindmills && (
            <div
              style={{
                position: "absolute",
                top: "50%",
                left: "0",
                width: "100%",
                height: "2px",
                background: "red",
                transform: "rotate(-45deg)",
              }}
            ></div>
          )}
        </div>
        <NearbyWindmills />

        <div
          style={{ marginTop: "8px", textAlign: "center", color: "#cceeff" }}
        >
          {showWindmills ? "" : " "}
        </div>
        {/*gumb}
      <button
        onClick={() => setShowRegions((prev) => !prev)}
        style={{
          background: "linear-gradient(135deg, #b164b1, #7a3f7a)",
          color: "#e9c8ef",
          border: "1.5px solid #bf7fcf",
          padding: "16px 32px",
          fontSize: "18px",
          fontWeight: "600",
          borderRadius: "10px",
          cursor: "pointer",
          boxShadow: "0 0 6px rgba(191, 127, 207, 0.4)",
          textShadow: "0 0 4px rgba(233, 200, 239, 0.6)",
          transition: "all 0.3s ease",
          letterSpacing: "1px",
          userSelect: "none",
        }}
        onMouseEnter={e => {
          e.currentTarget.style.background = "linear-gradient(135deg, #cc94cc, #874987)";
          e.currentTarget.style.boxShadow = "0 0 10px rgba(191, 127, 207, 0.6)";
          e.currentTarget.style.color = "#f5d6fb";
          e.currentTarget.style.textShadow = "0 0 6px rgba(233, 200, 239, 0.8)";
          e.currentTarget.style.transform = "scale(1.04)";
        }}
        onMouseLeave={e => {
          e.currentTarget.style.background = "linear-gradient(135deg, #b164b1, #7a3f7a)";
          e.currentTarget.style.boxShadow = "0 0 6px rgba(191, 127, 207, 0.4)";
          e.currentTarget.style.color = "#e9c8ef";
          e.currentTarget.style.textShadow = "0 0 4px rgba(233, 200, 239, 0.6)";
          e.currentTarget.style.transform = "scale(1)";
        }}
      >
        {showRegions ? "Skrij mapo" : "Pokaži mapo"}
      </button>
    {gumb*/}
      </div>

      {showRegions && <Legend />}
    </div>
  );
};

export default Map;
