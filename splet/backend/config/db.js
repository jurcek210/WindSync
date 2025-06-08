import mongoose from 'mongoose'

const connectDB = async() => {
    try {
        await mongoose.connect(process.env.MONGO_URI);
        console.log("Connection to database successful!")
    }catch (err) {
        console.error("FAILED TO CONNECT TO DATABASE!")
        process.exit(1);
    }
};

export default connectDB