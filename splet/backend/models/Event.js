import mongoose from "mongoose";

const eventSchema = new mongoose.Schema(
    {
        topic: { type: String, required: true },
        message: { type: String, required: true },
        timestamp: { type: Date, default: Date.now },
        location: {
            type: {
                type: String,
                enum: ["Point"],
                required: true,
                default: () => "Point",
            },
            coordinates: {
                type: [Number],
                required: true,
            },
        }
    }
);

eventSchema.index({ location: "2dsphere" });
eventSchema.index({ topic: 1, timestamp: -1 });

export default mongoose.model("Event", eventSchema);
