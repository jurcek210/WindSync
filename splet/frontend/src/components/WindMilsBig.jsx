import { useParams } from 'react-router-dom';
import WindEnergyGraph from './WindEnergyGraph'; // uvozi graf

const turbineDataBig = {
  1: {
    name: "Ampair 6000 Wind Turbine",
    price: "20000$",
    power: "6 kW",
  },
  2: {
    name: "Eocycle EOX S-15",
    price: "40000$",
    power: "15 kW",
  },
  3: {
    name: "Kestrel e400i",
    price: "12000$",
    power: "3.5 kW",
  },
};

function WindMilsBig() {
  const { id, windSpeed, lat, lng } = useParams();

  const data = turbineDataBig[id];
  if (!data) {
    return <div>Napaka: Turbina z ID {id} ne obstaja.</div>;
  }

  const powerStr = data.power.replace(',', '.').replace(/[^\d.]/g, '');
  const power = parseFloat(powerStr) * 1000; // v W

  const priceStr = data.price.replace(/[^\d]/g, '');
  const price = parseFloat(priceStr);

  const electricityCostPerKWh = 0.10;
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
    let result = '';

    if (years > 0) {
      result += years === 1 ? '1 leto' : `${years} leta`;
    }

    if (remainingMonths > 0) {
      if (years > 0) {
        result += ' ';
      }
      result += remainingMonths === 1 ? '1 mesec' : `${remainingMonths} mescev`;
    }

    return result || '0 mesecev';
  }

  const dailyEnergyProduction = powerKW * 24 * efficiencyFactor;
  const paybackDays =
    dailyEnergyProduction > 0
      ? price / (dailyEnergyProduction * electricityCostPerKWh)
      : null;
  const paybackMonths = paybackDays !== null ? paybackDays / 30 : null;

  // Dodatni izračuni za podjetja in zaposlene
  const annualEnergyProduction = dailyEnergyProduction * 365; // kWh na leto
  const averageCompanyAnnualConsumption = 30000; // kWh
  const averageEmployeesPerCompany = 50;
  const companiesPowered = annualEnergyProduction / averageCompanyAnnualConsumption;
  const peoplePowered = companiesPowered * averageEmployeesPerCompany;

  return (
    <div style={{ padding: '20px' }}>
      <h2>{data.name}</h2>
      <img
        src={`/photos/image${id}.jpg`}
        alt={data.name}
        style={{ width: '300px', borderRadius: '12px', marginBottom: '16px' }}
      />
      <p><strong>Cena:</strong> {data.price}</p>
      <p><strong>Moč:</strong> {data.power}</p>
      <p><strong>Povprečna hitrost vetra:</strong> {windSpeed} m/s</p>
      <p><strong>Dnevna proizvodnja energije:</strong> {dailyEnergyProduction.toFixed(2)} kWh</p>
      {paybackMonths !== null ? (
        <p><strong>Čas povrnitve investicije:</strong> {formatMonthsToYearsMonths(paybackMonths)}</p>
      ) : (
        <p><em>Povprečna hitrost vetra je prenizka za proizvodnjo energije.</em></p>
      )}

      {/* Prikaz grafa */}
      <div style={{ marginTop: '40px' }}>
        <WindEnergyGraph lat={lat} lng={lng} powerKW={powerKW} />
      </div>

      {/* Informacije o podjetjih in ljudeh */}
      <div style={{ marginTop: '30px', fontWeight: '600' }}>
        <p>
          Ta turbina lahko oskrbi približno <strong>{companiesPowered.toFixed(1)}</strong> povprečnih velikih podjetij z energijo na leto.
        </p>
        <p>
          To pomeni, da bi lahko posredno napajala <strong>{Math.round(peoplePowered)}</strong> zaposlenih oseb.
        </p>
      </div>
    </div>
  );
}

export default WindMilsBig;
