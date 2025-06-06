import Wind from "./Wind";
import { useEffect, useRef, useState } from "react";
import "../styles/WindmillDetailsModal.css";
import DayliEnergy from "./DayliEnergy";

const WindmillDetailsModal = ({ windmill, onClose }) => {
  const { name, location, status } = windmill;
  const [lat, lon] = [location.coordinates[1], location.coordinates[0]];
  const modalRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (modalRef.current && !modalRef.current.contains(event.target)) {
        onClose();
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [onClose]);

  return (
    <div className="modal-overlay">
      <div className="modal-content" ref={modalRef}>
        <button className="modal-close" onClick={onClose}>×</button>
        <h2>{name}</h2>
        <p><strong>Lokacija:</strong> {lat.toFixed(4)}, {lon.toFixed(4)}</p>
        <p><strong>Status:</strong> {status ? "Aktivna" : "Neaktivna"}</p>

        <DayliEnergy windmill={windmill} />

        <div style={{ marginTop: "20px" }}>
          <Wind lat={lat} lng={lon} />
        </div>
      </div>
    </div>
  );
};

export default WindmillDetailsModal;
