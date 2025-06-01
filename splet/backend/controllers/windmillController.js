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

export const getMyWindmills = async (req, res) => {
  try {
    const windmills = await Windmill.find({ owner: req.user._id });
    res.status(200).json(windmills);
  } catch (err) {
    res.status(500).json({ message: "Napaka pri pridobivanju veternic." });
  }
};

export const deleteWindmill = async (req, res) => {
  try {
    const windmill = await Windmill.findById(req.params.id);
    if (!windmill) {
      return res.status(404).json({ message: "Veternica ne obstaja" });
    }

    if (windmill.owner.toString() !== req.user._id.toString()) {
      return res.status(403).json({ message: "Ni dovoljeno" });
    }

    await windmill.deleteOne();
    res.status(200).json({ message: "Veternica izbrisana" });

  } catch (err) {
    console.error("Napaka pri brisanju veternice:", err);
    res.status(500).json({ message: "Napaka pri brisanju veternice" });
  }
};

export const toggleWindmillStatus = async (req, res) => {
  try {
    const windmill = await Windmill.findById(req.params.id);
    if (!windmill) {
      return res.status(404).json({ message: "Veternica ni bila najdena." });
    }

    if (windmill.owner.toString() !== req.user.id) {
      return res.status(403).json({ message: "Ni dovoljenja za urejanje." });
    }

    windmill.status = req.body.status;
    await windmill.save();

    res.json({ message: "Status posodobljen.", status: windmill.status });
  } catch (err) {
    console.error("Napaka pri posodobitvi statusa:", err);
    res.status(500).json({ message: "Napaka strežnika." });
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