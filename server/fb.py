import firebase_admin
import time
from firebase_admin import credentials
from firebase_admin import db
from pyfcm import FCMNotification
import arrow

import utils

key = 'safest-path-firebase-adminsdk-two07-46c5f72b5d.json'
cred = credentials.Certificate(key)
firebase_admin.initialize_app(cred, {
    'databaseURL': 'https://safest-path.firebaseio.com'
})
push_service = FCMNotification(api_key="AAAAca7jNTQ:APA91bHYkOOC5WMvSKjRqErmAYHw2rFo5eF_yiSv666SI1wdFsU9tRAVJqcZsPrBNCp2uV-xgy58djMTN7Igf-MxOvUugtRH1CT75FCKE3chZ8WAzX9O5htKhyIFTuy7gN-ibBXX1m4Q")
THRESHOLD = 0

def check_locations():
    ref = db.reference('locations')
    user_ids = list(dict(ref.get()).keys())
    for user_id in user_ids:
        user = db.reference(f'users/{user_id}')
        ttl_current_loc = user.get().get('ttl_current_loc', None)
        ttl_preferred_loc = user.get().get('ttl_preferred_loc', None)
        token = user.get()['token']

        check_for_current_location(user, token, ttl_current_loc)
        check_for_preferred_location(user, token, ttl_preferred_loc)
        

def check_for_current_location(user, token, ttl):
    # Check if TTL expired, then proceed
    if ttl and arrow.utcnow() < arrow.get(ttl):
        print(user.get()['email'], "TTL alive, skipping check")
        return

    lat = user.get()['lat']
    long = user.get()['long']
    
    print(user.get()['email'], lat, long)

    # Get crime score
    score = utils.get_crime_score(lat, long, radius=0.5)
    print(score)
    if score > THRESHOLD:
        print("Sending notification")
        # Send notification
        result = push_service.notify_single_device(
            registration_id=token,
            message_title="Safety alert",
            message_body="You are near unsafe location. Kindly be alert"
        )
        
        # Set TTL so that no multiple notification is sent
        # Ex: 10 min
        user.child('ttl_current_loc').set(
            arrow.utcnow().shift(minutes=10).format()
        )

def check_for_preferred_location(user, token, ttl):
    # Check if TTL expired, then proceed
    if ttl and arrow.utcnow() < arrow.get(ttl):
        print(user.get()['email'], "TTL alive, skipping check")
        return

    lat = user.get().get('preferred', {}).get('lat', None)
    long = user.get().get('preferred', {}).get('long', None)

    print(user.get()['email'], lat, long)

    if not (lat and long): return
    crime = utils.get_latest_crime(lat, long, radius=0.5)

    if crime:
        # Send notification
        result = push_service.notify_single_device(
            registration_id=token,
            message_title="Crime alert",
            message_body=f"{crime['parent_incident_type']} reported near your home location"
        )
        
        # Set TTL so that no multiple notification is sent
        # Ex: 10 min
        user.child('ttl_preferred_loc').set(
            arrow.utcnow().shift(minutes=10).format()
        )


if __name__ == "__main__":
    while True:
        check_locations()
        time.sleep(10)