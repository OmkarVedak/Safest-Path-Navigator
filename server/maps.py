import googlemaps
import polyline
import json
import arrow
import random
from geopy import distance as geo_distance
from datetime import datetime

from .settings import MAPS_API_KEY
from .utils import get_crime_score_for_crime, get_vehicular_frequency

gmaps = googlemaps.Client(key=MAPS_API_KEY)

def get_directions(source, destination):
    return gmaps.directions(
        source,
        destination,
        mode="walking",
        departure_time=datetime.now(),
        alternatives=True
    )

def meters_to_miles(meters):
    return meters * 0.000621371

def get_path_score(path, col):
    """
    Compute safety score for a path
    """

    # First, decode the polyline to `list<lat, lng>` coordinates
    poly_points = path['overview_polyline']['points']
    decoded = polyline.decode(poly_points)
    
    # Calculate path distance and radius
    path_distance = path['legs'][0]['distance']['value']
    if len(decoded) < 10:
        sample_points = decoded
    else:
        sample_points = decoded[::len(decoded) // 10]
    radius = meters_to_miles(path_distance / 10)
    crime_ids = set()
    score = 0
    for point in sample_points:
        _around_point = col.find({
            'geo': { 
                '$geoWithin': {
                    '$center': [
                        [point[1], point[0]],
                        radius / 3959
                    ]
                }
            } 
        })
        for crime in _around_point:
            if not crime['incident_id'] in crime_ids:
                crime_ids.add(crime['incident_id'])
                score += get_crime_score_for_crime(crime)

    return score