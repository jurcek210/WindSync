import { useParams } from "react-router-dom";
import WindEnergyGraph from "./WindEnergyGraph";
import "../styles/WindMills.css"

const turbineData = {
  1: {
    name: "Bornay 1200",
    price: "4000$",
    power: "1200 W",
  },
  2: {
    name: "Missouri Freedom II Wind Turbine",
    price: "1500$",
    power: "2.000 W",
  },
  3: {
    name: "Tumo-Int 1200W Horizontal Wind Turbine",
    price: "1500$",
    power: "1220 W",
  },
};

function WindMills() {
  const { id, windSpeed, lat, lng } = useParams();

  const data = turbineData[id];
  if (!data) {
    return <div>Napaka: Turbina z ID {id} ne obstaja.</div>;
  }

  const powerStr = data.power.replace(/[^\d]/g, "");
  const power = parseFloat(powerStr);

  const priceStr = data.price.replace(/[^\d]/g, "");
  const price = parseFloat(priceStr);

  const electricityCostPerKWh = 0.1;
  const powerKW = power / 1000;

  const ws = parseFloat(windSpeed);
  let efficiencyFactor = 0;

  if (ws < 3) {
    efficiencyFactor = 0;
  } else if (ws >= 3 && ws <= 12) {
    efficiencyFactor = (ws - 3) / (12 - 3);
  } else {
    efficiencyFactor = 1;
  }

  function formatMonthsToYearsMonths(months) {
    const years = Math.floor(months / 12);
    const remainingMonths = Math.round(months % 12);
    let result = "";

    if (years > 0) {
      result += years === 1 ? "1 leto" : `${years} leta`;
    }

    if (remainingMonths > 0) {
      if (years > 0) {
        result += " ";
      }
      result +=
        remainingMonths === 1 ? "1 mesec" : `${remainingMonths} mescev`;
    }

    return result || "0 mesecev";
  }

  const dailyEnergyProduction = powerKW * 24 * efficiencyFactor;
  const paybackDays =
    dailyEnergyProduction > 0
      ? price / (dailyEnergyProduction * electricityCostPerKWh)
      : null;
  const paybackMonths = paybackDays !== null ? paybackDays / 30 : null;

   const averageHouseDailyConsumption = 10; // kWh, povprečna dnevna poraba hiše

  return (
    <div className="container">
      <h2 className="title">{data.name}</h2>
      <img
        src={`/photos/Slika${id}.jpg`}
        alt={data.name}
        className="image image-tooltip"
        data-tooltip={`Wind Turbine: ${data.name}`}
      />
      <div className="infoPanel">
        <div className="infoRow">
          <strong>Cena:</strong> {data.price}
        </div>
        <div className="infoRow">
          <strong>Moč:</strong> {data.power}
        </div>
        <div className="infoRow">
          <strong>Povprečna hitrost vetra:</strong> {windSpeed} m/s
        </div>

        <p className="highlight">
          Dnevna proizvodnja energije: {dailyEnergyProduction.toFixed(2)} kWh
        </p>

        <div className="infoRow" style={{ fontStyle: 'italic', color: '#ccc' }}>
          Povprečna hiša porabi približno <strong>{averageHouseDailyConsumption} kWh</strong> energije na dan.
        </div>

        {paybackMonths !== null ? (
          <p className="infoRow" style={{ fontWeight: "600" }}>
            Čas povrnitve investicije: {formatMonthsToYearsMonths(paybackMonths)}
          </p>
        ) : (
          <p className="paybackText">
            Povprečna hitrost vetra je prenizka za proizvodnjo energije.
          </p>
        )}
      </div>

      <div className="graphContainer">
        <WindEnergyGraph lat={lat} lng={lng} powerKW={powerKW} />
      </div>
    </div>
  );
}

export default WindMills;
