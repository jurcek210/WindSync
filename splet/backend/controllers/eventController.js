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

export const deleteEvent = async (req, res) => {
  try {
    const { id } = req.params;

    const deleted = await Event.findByIdAndDelete(id);

    if (!deleted) {
      return res.status(404).json({ message: "Event not found" });
    }

    return res.status(200).json({ message: "Event deleted" });
  } catch (e) {
    return res.status(500).json({ error: e.message });
  }
};
export const updateEvent = async (req, res) => {
  try {
    const { id } = req.params;
    const { topic, message } = req.body;

    if (!topic || !message) {
      return res.status(400).json({ message: "Missing fields" });
    }

    const updated = await Event.findByIdAndUpdate(
      id,
      {
        topic,
        message,
        timestamp: new Date()
      },
      { new: true }
    );

    if (!updated) {
      return res.status(404).json({ message: "Event not found" });
    }

    return res.status(200).json(updated);
  } catch (e) {
    return res.status(500).json({ error: e.message });
  }
};
