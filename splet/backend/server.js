import dotenv from 'dotenv'
import express from 'express'
import cors from 'cors'
<<<<<<< HEAD
import connectDB from './config/db.js'
import cookieParser from "cookie-parser"
import authRoute from "./routes/authRoutes.js"
import stationRoute from "./routes/stationRoutes.js";

=======
import http from 'http'
import { Server } from 'socket.io'
import connectDB from './config/db.js'
import cookieParser from "cookie-parser"
import authRoute from "./routes/authRoutes.js"
import windmillRoute from "./routes/windmillRoutes.js"
>>>>>>> dev

dotenv.config();
connectDB();

<<<<<<< HEAD
const app = express()
app.use(express.json())

app.get('/', (req, res) => {
    res.send("API WORKS")
});

app.use(
    cors({
      origin: ["http://localhost:5173"],
      methods: ["GET", "POST", "PUT", "DELETE"],
      credentials: true,
    })
  );
  app.use(cookieParser());
  
  app.use("/api", authRoute);

  app.use("/api/stations", stationRoute);

const PORT = process.env.PORT || 3001;
app.listen(PORT, () => {
    console.log(`Server listening on port ${PORT}`)
})


=======
const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: ["http://localhost:5173"],
    methods: ["GET", "POST", "PUT", "DELETE"],
    credentials: true,
  },
});

app.set("io", io);

app.use(express.json());
app.use(cookieParser());

app.use(
  cors({
    origin: ["http://localhost:5173"],
    methods: ["GET", "POST", "PUT", "DELETE"],
    credentials: true,
  })
);

app.get('/', (req, res) => {
  res.send("API WORKS");
});

app.use("/api", authRoute);
app.use("/api/windmills", windmillRoute);

io.on("connection", (socket) => {
  console.log("Nova socket povezava:", socket.id);

  socket.on("disconnect", () => {
    console.log("Socket odklopljen:", socket.id);
  });
});

const PORT = process.env.PORT || 3001;
server.listen(PORT, () => {
  console.log(`Server listening on port ${PORT}`);
});
>>>>>>> dev
