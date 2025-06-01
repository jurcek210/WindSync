import { useState } from "react";

function DayliEnergy({ windmill }) {
  const [energy, setEnergy] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [weatherDesc, setWeatherDesc] = useState("");
  const [temperature, setTemperature] = useState(null);
  const [windSpeed, setWindSpeed] = useState(null);

  const handleClick = async () => {
    if (energy || error) {
      // Če so podatki že prikazani, jih skrijemo
      setEnergy(null);
      setError(null);
      setWeatherDesc("");
      setTemperature(null);
      setWindSpeed(null);
      return;
    }

    setLoading(true);
    setError(null);

    const { coordinates } = windmill.location;
    const [lng, lat] = coordinates;

    const API_KEY = "7f24a8b33318df879eda6cd00f4f5716";

    const url = `https://api.openweathermap.org/data/2.5/weather?lat=${lat}&lon=${lng}&appid=${API_KEY}&units=metric`;

    try {
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error("Napaka pri pridobivanju podatkov");
      }
      const data = await response.json();

      const windSpeedData = data.wind?.speed ?? 0;
      const tempData = data.main?.temp ?? null;
      const desc = data.weather && data.weather.length > 0 ? data.weather[0].description : "";

      const energyToday = (windSpeedData * 5).toFixed(2);

      setEnergy(energyToday);
      setWeatherDesc(desc);
      setTemperature(tempData);
      setWindSpeed(windSpeedData);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ marginTop: "8px" }}>
      <button
        onClick={handleClick}
        disabled={loading}
        style={{
          padding: "4px 8px",
          background: "#4caf50",
          color: "#fff",
          border: "none",
          borderRadius: "4px",
          cursor: loading ? "not-allowed" : "pointer",
        }}
      >
        {loading
          ? "Pridobivanje podatkov..."
          : energy || error
          ? "Skrij energijo in vreme"
          : "Prikaži energijo za danes"}
      </button>
      {(energy || error) && (
        <div style={{ marginTop: "8px", fontWeight: "bold" }}>
          {energy && (
            <>
              <p>Danes bi naredila: {energy} kWh</p>
              <p>Vreme: {weatherDesc.charAt(0).toUpperCase() + weatherDesc.slice(1)}</p>
              <p>Temperatura: {temperature} °C</p>
              <p>Hitrost vetra: {windSpeed} m/s</p>
            </>
          )}
          {error && <p style={{ color: "red" }}>Napaka: {error}</p>}
        </div>
      )}
    </div>
  );
}

export default DayliEnergy;
