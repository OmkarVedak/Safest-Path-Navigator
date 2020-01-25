def preprocess(mongo):
    """
    Insert geo index to each of the records
    """
    data = mongo.db.records.find()
    for i, item in enumerate(data):
        if i % 100 == 0:
            print(f'{i}/{data.count()}')
        doc = mongo.db.records.update({
            '_id': item['_id']
        }, {
            '$set': {
                'geo': {
                    "type": "Point",
                    "coordinates": [
                        item['longitude'],
                        item['latitude']
                    ]
                }
            }
        })