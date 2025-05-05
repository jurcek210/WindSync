import express from "express";
import {createStation, listStations} from "../controllers/stationController.js"
import { protect } from "../middleware/auth.js";

const router = express.Router()

router.get("/", listStations)
router.post("/", protect, createStation); 

export default router;