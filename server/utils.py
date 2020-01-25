from pymongo import MongoClient
import arrow
import json
import random

client = MongoClient()
db = client['crimedata']
col = db['records_3']

with open("weights.json", "r") as f:
    weights = json.load(f)

def get_vehicular_frequency():
    """
    Simulate vehicular frequency:
    Randomly return value from 1-5
    1 - low traffic
    5 - high traffic
    """
    return random.randint(1, 5)

def get_crime_score(latitude, longitude, radius):
    _around_point = col.find({
        'geo': { 
            '$geoWithin': {
                '$center': [
                    [longitude, latitude],
                    radius / 3959
                ]
            }
        } 
    })
    score = 0
    for crime in _around_point:
        score += get_crime_score_for_crime(crime)
    return score

def get_latest_crime(latitude, longitude, radius):
    """Gets past crimes reported within last 10 minutes"""
    _around_point = col.find({
        'geo': { 
            '$geoWithin': {
                '$center': [
                    [longitude, latitude],
                    radius / 3959
                ]
            }
        } 
    })
    for crime in _around_point:
        incident_date = arrow.get(
            crime['incident_datetime'],
            'MM/DD/YYYY hh:mm:ss A'
        ).naive
        now = arrow.now().naive
        seconds = (now - incident_date).seconds
        print("Seconds", seconds)
        if seconds < 600:
            return crime
        

def get_crime_score_for_crime(crime):
    crime_type = crime['parent_incident_type']
    if crime.get('incident_datetime', None):
        incident_date = arrow.get(
            crime['incident_datetime'],
            'MM/DD/YYYY hh:mm:ss A'
        )
        now = arrow.utcnow()
        days = (now - incident_date).days
    else:
        days = 1
    
    if days == 0: 
        days += 1

    frequency = get_vehicular_frequency()
    score = weights[crime_type] / (days * frequency)
    return score