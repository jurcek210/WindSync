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
