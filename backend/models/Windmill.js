import mongoose from "mongoose";

const windmillSchema = new mongoose.Schema({
  name: {
    type: String,
    required: true,
  },
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
  },
  windSpeed: {
    type: Number,
  },
  status: {
    type: Boolean,
    default: true,
  },
  owner: {
    type: mongoose.Schema.Types.ObjectId,
    ref: "User"
  },
   measurements: [{
    type: mongoose.Schema.Types.ObjectId,
    ref: "WindmillData" //placeholder ko se bo dalje dodajalo
  }],
  createdAt: {
    type: Date,
    default: Date.now,
  },
});
windmillSchema.index({ location: "2dsphere" });

export default mongoose.model("Windmill", windmillSchema);
