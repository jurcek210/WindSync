import User from "../models/User.js";
import Station from "../models/Station.js";

export const createStation = async (req, res) => {
  try {
    const station = await Station.create({
      ...req.body,
      owner: req.user.id
      
    });
    res.status(201).json(station);
    console.log("REQ.USER:", req.user);
  } catch (err) {
    res.status(400).json({ message: err.message });
  }
};

export const listStations = async (req, res) => {
  try {
    const stations = await Station.find();
    res.status(200).json(stations);
  } catch (err) {
    res.status(400).json({ message: err.message });
  }
};
