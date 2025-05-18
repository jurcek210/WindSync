import "../styles/Home.css";
import Map from "./Map.jsx";
import NearbyWindmills from "./NearbyWindmills.jsx";
import { useEffect, useState } from "react";

const Home = ({loggedIn}) => {
  
  return (
  <div className="home-container">
    <Map loggedIn={loggedIn} />

    <NearbyWindmills />
    </div>
        
        
      
  );
};

export default Home;
