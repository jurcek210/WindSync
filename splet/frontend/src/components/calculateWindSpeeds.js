import fs from "fs";             // za sync metode
import axios from "axios";       // če boš uporabljal axios
// Odstrani podvajanje uvoza fs/promises, ker je ne rabiš, če delaš sync fs metode

const municipalities = JSON.parse(fs.readFileSync("./public/gadm41_SVN_2.json", "utf-8"));

const fetchAverageWindSpeed = async (lat, lng) => {
  // Tukaj implementiraj ali prenesi svojo funkcijo za pridobivanje vetra
  // Primer:
  // const res = await axios.get(`someAPIurl?lat=${lat}&lng=${lng}`);
  // return res.data.averageWindSpeed;
  return Math.random() * 10; // začasna simulacija
};

function getFeatureCenter(feature) {
  const coords = feature.geometry.coordinates;
  // Za MultiPolygon vzamemo prvi polygon in prvi ring
  const polygon = coords[0][0];
  
  let lonSum = 0, latSum = 0;
  polygon.forEach(coord => {
    lonSum += coord[0];
    latSum += coord[1];
  });
  const n = polygon.length;
  return [lonSum / n, latSum / n]; // [lon, lat]
}

const main = async () => {
  const speeds = {};

  for (const feature of municipalities.features) {
    const center = getFeatureCenter(feature);
    const name = feature.properties.NAME_2 || "Nepoznano ime";

    const avgWind = await fetchAverageWindSpeed(center[1], center[0]);
    if (avgWind === null || avgWind === undefined) {
      console.log(`Ni podatka za ${name}`);
    } else {
      console.log(`Pridobljeno za ${name}: ${avgWind.toFixed(2)} m/s`);
      speeds[name] = avgWind;
    }
  }

  // Shrani rezultat v json (sinohrono, ker uvažamo 'fs')
  fs.writeFileSync("./municipalityWindSpeeds.json", JSON.stringify(speeds, null, 2));
  console.log("Shranjene povprečne hitrosti vetra za občine.");
};

main().catch(console.error);
