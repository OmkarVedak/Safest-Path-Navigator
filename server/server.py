from flask_sqlalchemy import SQLAlchemy
from flask import Flask, jsonify, request
from flask_pymongo import PyMongo
from pyfcm import FCMNotification
from bson.objectid import ObjectId
import uuid
import arrow

from .maps import get_directions, get_path_score
from .settings import MONGO_HOST, MONGO_PORT

app = Flask(__name__)
app.config["SQLALCHEMY_DATABASE_URI"] = 'sqlite:////tmp/safest_path.db'
app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False
app.config["MONGO_URI"] = f"mongodb://{MONGO_HOST}:{MONGO_PORT}/crimedata"
mongo = PyMongo(app)
db = SQLAlchemy(app)

class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    token = db.Column(db.String(200), unique=True, nullable=True)

    def __repr__(self):
        return '<User %r>' % self.token

"""
Get Directions - `source` to `destination`
Example: /directions?source=Sydney Town Hall&destination=Parramatta, NSW
"""
@app.route('/directions')
def directions():
    source = request.args.get('source', '')
    destination = request.args.get('destination', '')
    if source and destination:
        paths = get_directions(source, destination)
        paths_with_scores = list(
            map(lambda path: { **path, 'score': get_path_score(path, mongo.db.records_2) }, paths)
        )
        scores = [x['score'] for x in paths_with_scores]
        total = sum(scores)
        if total > 0:
            scores = [1 - (x / sum(scores)) for x in scores]
        paths_with_scores = [
            { **path, 'score': scores[i] }
            for i, path in enumerate(paths_with_scores)
        ]
        return jsonify(paths_with_scores)
    else:
        return jsonify({ 'error': 'Specify `source` and `destination` parameters' }), 400

"""
Register an app with Firebase token
Example:
    POST /register
    {
        "token": "my_firebase_token"
    }
"""
@app.route('/register', methods=['POST'])
def register():
    content = request.get_json(force=True, silent=True)
    token = content.get('token', None)
    print(token)
    if token:
        try:
            user = User(token=token)
            db.session.add(user)
            db.session.commit()
            print(f"Added token {token}")
            return jsonify({ "success": True })
        except:
            return jsonify({ "success": False }), 403
    else:
        return jsonify({ "success": False }), 403

@app.route('/add_topic', methods=['POST'])
def add_topic():
    content = request.get_json(force=True, silent=True)
    latitude = content.get('latitude', None)
    longitude = content.get('longitude', None)
    parent_incident_type = content.get('parent_incident_type', None)
    incident_id = str(uuid.uuid1())
    result = mongo.db.records_3.insert_one({
        'incident_id': incident_id,
        'latitude': latitude,
        'longitude': longitude,
        'parent_incident_type': parent_incident_type,
        'incident_datetime': arrow.now().format('MM/DD/YYYY hh:mm:ss A'),
        'geo': {
            'type': 'Point',
            'coordinates': [float(longitude), float(latitude)]
        }
    })
    print(result.inserted_id)
    return jsonify({ 'success': True })

@app.route('/nearby')
def nearby():
    lat = float(request.args.get('lat', None))
    lng = float(request.args.get('lng', None))
    radius = float(request.args.get('radius', None))
    print(radius)
    
    if lat and lng:
        crimes = []
        result = mongo.db.records_3.find({
            'geo': { 
                '$geoWithin': {
                    '$center': [
                        [lng, lat],
                        radius / 3959
                    ]
                }
            } 
        })
        for crime in result:
            crimes.append({
                "incident_datetime": crime['incident_datetime'],
                "latitude": crime['latitude'],
                "longitude": crime['longitude'],
                "parent_incident_type": crime['parent_incident_type']
            })
        return jsonify(crimes)
    else:
        return jsonify({ "success": False }), 403

@app.route('/report_unsafe', methods=['POST'])
def report_unsafe():
    content = request.get_json(force=True, silent=True)
    path = content.get('path', None)
    mongo.db.unsafe_paths.insert_one({
        'path': path
    })
    return jsonify({ "success": True })

@app.route('/notify')
def notify():
    push_service = FCMNotification(api_key="AAAAca7jNTQ:APA91bHYkOOC5WMvSKjRqErmAYHw2rFo5eF_yiSv666SI1wdFsU9tRAVJqcZsPrBNCp2uV-xgy58djMTN7Igf-MxOvUugtRH1CT75FCKE3chZ8WAzX9O5htKhyIFTuy7gN-ibBXX1m4Q")
    message_title = "Uber update"
    message_body = "Hi john, your customized news for today is ready"
    result = push_service.notify_topic_subscribers(
        topic_name="preferred",
        message_title=message_title,
        message_body=message_body
    )
    return jsonify({
        "success": True
    })

