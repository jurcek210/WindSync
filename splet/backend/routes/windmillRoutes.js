import express from "express";
import { createWindmill, listWindmills, getNearbyWindmills } from "../controllers/windmillController.js";
import { protect } from "../middleware/auth.js";
import { get } from "mongoose";
import { getMyWindmills } from "../controllers/windmillController.js";
import { deleteWindmill } from "../controllers/windmillController.js";
import {toggleWindmillStatus} from "../controllers/windmillController.js";

const router = express.Router();

router.get("/", listWindmills);
router.post("/", createWindmill);
router.get("/nearby", getNearbyWindmills);
router.get("/my", getMyWindmills);
router.delete("/:id", deleteWindmill);
router.put("/:id/toggle-status", protect, toggleWindmillStatus);

export default router;