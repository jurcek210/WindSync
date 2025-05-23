import { useParams } from 'react-router-dom';
import WindEnergyGraph from './WindEnergyGraph'; // Dodamo komponento za graf

const turbineData = {
    1: {
        name: "Bornay 1200",
        price: "4000$",
        power: "1200 W"
    },
    2: {
        name: "Missouri Freedom II Wind Turbine",
        price: "1500$",
        power: "2.000 W"
    },
    3: {
        name: " Tumo-Int 1200W Horizontal Wind Turbine",
        price: "1500$",
        power: "1220 W"
    }
};

function WindMills() {
    const { id, windSpeed, lat, lng } = useParams();

    const data = turbineData[id];
    if (!data) {
        return <div>Napaka: Turbina z ID {id} ne obstaja.</div>;
    }

    const powerStr = data.power.replace(/[^\d]/g, '');
    const power = parseFloat(powerStr);

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
            result += remainingMonths === 1 ? '1 mesec' : `${remainingMonths} mescov`;
        }

        return result || '0 mesecev';
    }

    const dailyEnergyProduction = powerKW * 24 * efficiencyFactor;
    const paybackDays = dailyEnergyProduction > 0 ? (price / (dailyEnergyProduction * electricityCostPerKWh)) : null;
    const paybackMonths = paybackDays !== null ? paybackDays / 30 : null;

    return (
        <div style={{ padding: "20px" }}>
            <h2>{data.name}</h2>
            <img
                src={`/photos/Slika${id}.jpg`}
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

            {/* DODATNO: graf mesečne proizvodnje energije */}
            <div style={{ marginTop: "40px" }}>
                <WindEnergyGraph lat={lat} lng={lng} powerKW={powerKW} />
            </div>
        </div>
    );
}

export default WindMills;
