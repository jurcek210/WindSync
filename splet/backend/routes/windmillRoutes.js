import express from "express";
import { createWindmill, listWindmills, getNearbyWindmills } from "../controllers/windmillController.js";
import { protect } from "../middleware/auth.js";
import { get } from "mongoose";

const router = express.Router();

router.get("/", listWindmills);
router.post("/", protect, createWindmill);
router.get("/nearby", getNearbyWindmills);

export default router;