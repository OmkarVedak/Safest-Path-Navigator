import os
from dotenv import load_dotenv

load_dotenv()
MAPS_API_KEY = os.getenv("API_KEY")
MONGO_HOST = os.getenv("MONGO_HOST")
MONGO_PORT = os.getenv("MONGO_PORT")