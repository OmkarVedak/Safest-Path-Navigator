from pymongo import MongoClient

from .settings import MONGO_HOST, MONGO_PORT

print("Establishing Mongo connection...")
client = MongoClient(MONGO_HOST, int(MONGO_PORT))
db = client['crimedata']
col = db['records']
print("Successfully Established Mongo connection")