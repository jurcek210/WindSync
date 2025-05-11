import express from "express";
import { createWindmill, listWindmills } from "../controllers/windmillController.js";
import { protect } from "../middleware/auth.js";

const router = express.Router();

router.get("/", listWindmills);
router.post("/", protect, createWindmill);

export default router;