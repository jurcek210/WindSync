import express from "express";
import { createEvent, listEvents } from "../controllers/eventController.js";

const router = express.Router();

router.post("/", createEvent);
router.get("/", listEvents);

export default router;
