import { useEffect } from "react";
import { useMap } from "react-leaflet";
import "leaflet.heat";
import L from "leaflet";

const WindHeatmapLayer = ({ data }) => {
  const map = useMap();

  useEffect(() => {
      console.log("🌍 map =", map);
  console.log("📊 data =", data);
    if (!data || data.length === 0) return;

    const heatLayer = L.heatLayer(
      data.map((p) => [p.lat, p.lng, p.value / 10]), // prilagodi intenziteto
      { radius: 25, blur: 20, maxZoom: 13 }
    ).addTo(map);

    return () => {
      map.removeLayer(heatLayer);
    };
  }, [data, map]);

  return null;
};

export default WindHeatmapLayer;
