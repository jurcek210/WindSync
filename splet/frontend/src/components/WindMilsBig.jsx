import { useParams } from 'react-router-dom';
import WindEnergyGraph from './WindEnergyGraph';
import '../styles/WindMills.css';

const turbineDataBig = {
  1: { name: "Ampair 6000 Wind Turbine", price: "20000$", power: "6 kW" },
  2: { name: "Eocycle EOX S-15", price: "40000$", power: "15 kW" },
  3: { name: "Kestrel e400i", price: "12000$", power: "3.5 kW" },
};

function WindMilsBig() {
  const { id, windSpeed, lat, lng } = useParams();

  const data = turbineDataBig[id];
  if (!data) {
    return <div className="error">Napaka: Turbina z ID {id} ne obstaja.</div>;
  }

  const powerStr = data.power.replace(',', '.').replace(/[^\d.]/g, '');
  const power = parseFloat(powerStr) * 1000;
  const priceStr = data.price.replace(/[^\d]/g, '');
  const price = parseFloat(priceStr);

  const electricityCostPerKWh = 0.10;
  const powerKW = power / 1000;

  const ws = parseFloat(windSpeed);
  let efficiencyFactor = 0;

  if (ws < 3) efficiencyFactor = 0;
  else if (ws >= 3 && ws <= 12) efficiencyFactor = (ws - 3) / (12 - 3);
  else efficiencyFactor = 1;

  function formatMonthsToYearsMonths(months) {
    const years = Math.floor(months / 12);
    const remainingMonths = Math.round(months % 12);
    let result = '';

    if (years > 0) result += years === 1 ? '1 leto' : `${years} leta`;
    if (remainingMonths > 0) result += (years > 0 ? ' ' : '') + (remainingMonths === 1 ? '1 mesec' : `${remainingMonths} mescev`);

    return result || '0 mesecev';
  }

  const dailyEnergyProduction = powerKW * 24 * efficiencyFactor;
  const paybackDays = dailyEnergyProduction > 0 ? price / (dailyEnergyProduction * electricityCostPerKWh) : null;
  const paybackMonths = paybackDays !== null ? paybackDays / 30 : null;

  const annualEnergyProduction = dailyEnergyProduction * 365;
  const averageCompanyAnnualConsumption = 30000;
  const averageEmployeesPerCompany = 50;
  const companiesPowered = annualEnergyProduction / averageCompanyAnnualConsumption;
  const peoplePowered = companiesPowered * averageEmployeesPerCompany;

  return (
    <div className="container">
      <h2 className="title">{data.name}</h2>
      <img src={`/photos/image${id}.jpg`} alt={data.name} className="image" />

      <table className="data-table">
        <tbody>
          <tr>
            <td>Cena</td>
            <td>{data.price}</td>
          </tr>
          <tr>
            <td>Moč</td>
            <td>{data.power}</td>
          </tr>
          <tr>
            <td>Povprečna hitrost vetra</td>
            <td>{windSpeed} m/s</td>
          </tr>
          <tr>
            <td>Dnevna proizvodnja energije</td>
            <td>{dailyEnergyProduction.toFixed(2)} kWh</td>
          </tr>
          <tr>
            <td>Čas povrnitve investicije</td>
            <td>
              {paybackMonths !== null
                ? formatMonthsToYearsMonths(paybackMonths)
                : <em>Povprečna hitrost vetra je prenizka za proizvodnjo energije.</em>
              }
            </td>
          </tr>
        </tbody>
      </table>

      <div className="graph">
        <WindEnergyGraph lat={lat} lng={lng} powerKW={powerKW} />
      </div>

      <div className="company-stats">
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
