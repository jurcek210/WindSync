import Event from "../models/Event.js";

export const createEvent = async (req, res) => {
  try {
    const { topic, message, lat, lon } = req.body;

    if (!topic || !message || lat == null || lon == null) {
      return res.status(400).json({ message: "Missing fields" });
    }

    const event = await Event.create({
      topic,
      message,
      timestamp: new Date(),
      location: {
        type: "Point",
        coordinates: [Number(lon), Number(lat)]
      }
    });

    return res.status(201).json(event);
  } catch (e) {
    return res.status(500).json({ error: e.message });
  }
};

export const listEvents = async (req, res) => {
  try {
    const { topic } = req.query;

    const query = topic ? { topic } : {};

    const events = await Event.find(query)
      .sort({ timestamp: -1 });

    return res.status(200).json(events);
  } catch (e) {
    return res.status(500).json({ error: e.message });
  }
};

