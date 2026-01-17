import express from "express";
import { createEvent, listEvents, deleteEvent, updateEvent } from "../controllers/eventController.js";

const router = express.Router();

router.post("/", createEvent);
router.get("/", listEvents);
router.delete("/:id", deleteEvent);
router.put("/:id", updateEvent);

export default router;
