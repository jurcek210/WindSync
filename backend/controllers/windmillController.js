import User from "../models/User.js";
import Windmill from "../models/Windmill.js";

export const createWindmill = async (req, res) => {
  try {
    const windmill = await Windmill.create({
      ...req.body,
      owner: req.user.id
      
    });
    res.status(201).json(windmill);
    console.log("REQ.USER:", req.user);
  } catch (err) {
    res.status(400).json({ message: err.message });
  }
};

export const listWindmills = async (req, res) => {
  try {
    const windmills = await Windmill.find();
    res.status(200).json(windmills);
  } catch (err) {
    res.status(400).json({ message: err.message });
  }
};


export const getNearbyWindmills = async (req, res) => {
  const { lat, lon, maxDistance } = req.query;

  if (!lat || !lon) {
    return res.status(400).json({ message: "Missing required parameters" });
  }
  try {
    const windmills = await Windmill.find({
      location: {
        $near: {
          $geometry: { type: "Point", coordinates: [parseFloat(lon), parseFloat(lat)] },
          $maxDistance: parseInt(maxDistance) || 25000 
        }
      }
    });

    res.status(200).json(windmills);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
   
}